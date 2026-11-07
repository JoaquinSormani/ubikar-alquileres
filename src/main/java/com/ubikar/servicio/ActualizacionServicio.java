package com.ubikar.servicio;

import com.ubikar.dao.ActualizacionValorDAO;
import com.ubikar.dao.ContratoDAO;
import com.ubikar.dao.PagoDAO;
import com.ubikar.dao.PropiedadDAO;
import com.ubikar.dao.Sql2oDAO;
import com.ubikar.modelo.ActualizacionValor;
import com.ubikar.modelo.Contrato;
import com.ubikar.modelo.EstadoPago;
import com.ubikar.modelo.IndiceActualizacion;
import com.ubikar.modelo.Inquilino;
import com.ubikar.modelo.Pago;
import com.ubikar.modelo.Propiedad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class ActualizacionServicio {

    private static final Logger log = LoggerFactory.getLogger(ActualizacionServicio.class);

    private final ContratoDAO contratoDAO = new ContratoDAO();
    private final PropiedadDAO propiedadDAO = new PropiedadDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final ActualizacionValorDAO actualizacionValorDAO = new ActualizacionValorDAO();
    private final PagoServicio pagoServicio = new PagoServicio();
    private final IndicesBcra indicesBcra;

    public record PersonaVista(String nombre, String apellido, String dni, String telefono) {
    }

    public record FilaActualizacion(
            Integer id,
            LocalDate fecha,
            Integer idContrato,
            String direccion,
            List<PersonaVista> inquilinos,
            String indice,
            Integer periodicidad,
            String periodoDesde,
            BigDecimal valorAnterior,
            BigDecimal valorNuevo,
            BigDecimal porcentaje) {
    }

    /** La próxima actualización de un contrato: todavía no se aplicó (su valor nuevo se calcula ese día). */
    public record FilaProxima(
            Integer idContrato,
            String direccion,
            List<PersonaVista> inquilinos,
            String indice,
            Integer periodicidad,
            LocalDate fechaAjuste,
            String periodoDesde,
            BigDecimal valorActual,
            String estado) {
    }

    public record PanelActualizaciones(List<FilaActualizacion> aplicadas, List<FilaProxima> proximas) {
    }

    public ActualizacionServicio() {
        this(new IndicesBcra());
    }

    public ActualizacionServicio(IndicesBcra indicesBcra) {
        this.indicesBcra = indicesBcra;
    }

    /**
     * Busca las actualizaciones pendientes de todos los contratos vigentes y las aplica en orden.
     */
    public void aplicarPendientes(LocalDate hoy) {
        for (Contrato c : contratoDAO.readAll()) {
            if (c.getFechaFin().isBefore(hoy)) {
                continue;
            }
            if (c.getFechaInicio().isAfter(hoy)) {
                continue;
            }
            if (c.getPeriodicidadActualizacion() == null || c.getPeriodicidadActualizacion() <= 0) {
                continue;
            }
            if (c.getIndiceActualizacion() == null) {
                continue;
            }
            aplicarPendientesContrato(c, hoy);
        }
    }

    private void aplicarPendientesContrato(Contrato c, LocalDate hoy) {
        while (true) {
            List<ActualizacionValor> aplicadas = actualizacionValorDAO.selectByContrato(c.getId());
            int k = aplicadas.size() + 1;
            LocalDate fechaAjuste = CalculoActualizacion.fechaAjuste(c.getFechaInicio(), c.getPeriodicidadActualizacion(), k);

            if (fechaAjuste.isAfter(hoy)) {
                // Todavía no llegó la fecha de esta actualización: pasar al siguiente contrato
                break;
            }
            if (fechaAjuste.isAfter(c.getFechaFin())) {
                break;
            }

            LocalDate periodoDesde = YearMonth.from(fechaAjuste).atDay(1);

            // Asegurar que existan los cobros hasta el mes actual
            pagoServicio.asegurarCobros(c, hoy);

            // Consultar el índice y calcular el factor
            BigDecimal factor;
            try {
                if (c.getIndiceActualizacion() == IndiceActualizacion.ICL) {
                    LocalDate fechaAnterior = (k == 1)
                            ? c.getFechaInicio()
                            : CalculoActualizacion.fechaAjuste(c.getFechaInicio(), c.getPeriodicidadActualizacion(), k - 1);
                    BigDecimal iclBase = indicesBcra.icl(fechaAnterior);
                    BigDecimal iclActual = indicesBcra.icl(fechaAjuste);
                    factor = CalculoActualizacion.factorIcl(iclActual, iclBase);
                } else { // IPC
                    int n = c.getPeriodicidadActualizacion();
                    YearMonth mesFin = YearMonth.from(fechaAjuste).minusMonths(1);
                    YearMonth mesInicio = mesFin.minusMonths(n - 1);
                    List<BigDecimal> inflaciones = indicesBcra.inflacionMensual(mesInicio, mesFin);
                    factor = CalculoActualizacion.factorIpc(inflaciones);
                }
            } catch (IllegalStateException e) {
                log.warn("No se pudo obtener el índice para la actualización k={} del contrato {}: {}. Se reintentará mañana.",
                        k, c.getId(), e.getMessage());
                break; // Se reintenta mañana
            } catch (Exception e) {
                log.error("Error inesperado consultando índices del BCRA para el contrato {}: {}", c.getId(), e.getMessage(), e);
                break;
            }

            BigDecimal valorAnterior;
            if (!aplicadas.isEmpty()) {
                valorAnterior = aplicadas.get(aplicadas.size() - 1).getValorNuevo();
            } else {
                valorAnterior = c.getValorInicial();
            }
            BigDecimal valorNuevo = CalculoActualizacion.nuevoValor(valorAnterior, factor);

            // Guardar la actualización y cambiar el monto de los cobros en una transacción
            try (Connection con = Sql2oDAO.getSql2o().beginTransaction()) {
                List<Pago> cobros = pagoDAO.selectByContrato(c.getId());
                for (Pago p : cobros) {
                    if (p.getEstado() == EstadoPago.PENDIENTE && !p.getPeriodo().isBefore(periodoDesde)) {
                        p.setMonto(valorNuevo);
                        p.setIndiceActualizacion(factor);
                        pagoDAO.update(con, p);
                    }
                }

                ActualizacionValor nueva = new ActualizacionValor(
                        c.getId(),
                        periodoDesde,
                        hoy,
                        c.getIndiceActualizacion().name(),
                        valorAnterior,
                        valorNuevo,
                        factor);
                actualizacionValorDAO.create(con, nueva);
                con.commit(false);
                log.info("Actualización k={} aplicada al contrato {}: nuevo valor {} (factor {}) desde {}",
                        k, c.getId(), valorNuevo, factor, periodoDesde);
            } catch (Exception e) {
                log.error("Error al persistir la actualización para contrato {}: {}", c.getId(), e.getMessage(), e);
                break;
            }
        }
    }

    /** "PROGRAMADA" si la fecha de ajuste es futura; "ESPERANDO_INDICE" si ya llegó pero todavía no se pudo aplicar. */
    static String estadoProxima(LocalDate fechaAjuste, LocalDate hoy) {
        return fechaAjuste.isAfter(hoy) ? "PROGRAMADA" : "ESPERANDO_INDICE";
    }

    /** La próxima actualización de cada contrato vigente (una por contrato), de la más cercana a la más lejana. */
    List<FilaProxima> proximas(LocalDate hoy) {
        List<FilaProxima> filas = new ArrayList<>();
        for (Contrato c : contratoDAO.readAll()) {
            // Un contrato que todavía no empezó también tiene su primera actualización programada.
            if (c.getFechaFin().isBefore(hoy)) {
                continue;
            }
            if (c.getPeriodicidadActualizacion() == null || c.getPeriodicidadActualizacion() <= 0 || c.getIndiceActualizacion() == null) {
                continue;
            }
            List<ActualizacionValor> aplicadas = actualizacionValorDAO.selectByContrato(c.getId());
            LocalDate fechaAjuste = CalculoActualizacion.fechaAjuste(c.getFechaInicio(), c.getPeriodicidadActualizacion(), aplicadas.size() + 1);
            if (fechaAjuste.isAfter(c.getFechaFin())) {
                continue; // el contrato termina antes de su próxima actualización
            }
            BigDecimal valorActual = aplicadas.isEmpty() ? c.getValorInicial() : aplicadas.get(aplicadas.size() - 1).getValorNuevo();
            Propiedad propiedad = propiedadDAO.read(c.getIdPropiedad());
            List<PersonaVista> inquilinos = new ArrayList<>();
            for (Inquilino inq : contratoDAO.selectInquilinos(c.getId())) {
                inquilinos.add(new PersonaVista(inq.getNombre(), inq.getApellido(), inq.getDni(), inq.getTelefono()));
            }
            filas.add(new FilaProxima(
                    c.getId(),
                    propiedad == null ? "" : propiedad.getDireccion(),
                    inquilinos,
                    c.getIndiceActualizacion().name(),
                    c.getPeriodicidadActualizacion(),
                    fechaAjuste,
                    YearMonth.from(fechaAjuste).toString(),
                    valorActual,
                    estadoProxima(fechaAjuste, hoy)));
        }
        filas.sort(Comparator.comparing(FilaProxima::fechaAjuste).thenComparing(FilaProxima::direccion, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }

    /**
     * Consulta para la pantalla de actualizaciones.
     */
    public PanelActualizaciones panel() {
        List<ActualizacionValor> todas = actualizacionValorDAO.readAll();
        List<FilaActualizacion> filas = new ArrayList<>();
        for (ActualizacionValor a : todas) {
            Contrato c = contratoDAO.read(a.getIdContrato());
            String direccion = "";
            List<PersonaVista> inquilinos = new ArrayList<>();
            if (c != null) {
                Propiedad prop = propiedadDAO.read(c.getIdPropiedad());
                if (prop != null) {
                    direccion = prop.getDireccion();
                }
                for (Inquilino inq : contratoDAO.selectInquilinos(c.getId())) {
                    inquilinos.add(new PersonaVista(inq.getNombre(), inq.getApellido(), inq.getDni(), inq.getTelefono()));
                }
            }
            String periodoDesde = YearMonth.from(a.getPeriodoDesde()).toString();
            BigDecimal porcentaje = CalculoActualizacion.porcentaje(a.getFactor());

            filas.add(new FilaActualizacion(
                    a.getId(),
                    a.getFecha(),
                    a.getIdContrato(),
                    direccion,
                    inquilinos,
                    a.getIndice(),
                    c != null ? c.getPeriodicidadActualizacion() : null,
                    periodoDesde,
                    a.getValorAnterior(),
                    a.getValorNuevo(),
                    porcentaje));
        }
        return new PanelActualizaciones(filas, proximas(LocalDate.now()));
    }
}

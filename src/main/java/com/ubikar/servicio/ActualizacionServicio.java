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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /**
     * Lo mínimo para armar la pantalla: la fecha de hoy, el mes que viene y los meses del selector (del más nuevo al más
     * viejo, formato AAAA-MM). Las filas no vienen acá: se piden de a una página con aplicadas(...) y proximas(...).
     */
    public record PanelActualizaciones(LocalDate fecha, String proximoMes, List<String> meses) {
    }

    /**
     * Una página de filas. "total" es la cantidad que cumple el filtro y "siguiente" es el cursor para pedir la página
     * que sigue (null cuando no hay más).
     */
    public record Pagina<T>(List<T> filas, int total, int tamanio, String siguiente) {
    }

    static final int TAMANIO_APLICADAS = 10;
    static final int TAMANIO_PROXIMAS = 5;
    // Cursor de las dos listas: "<fecha AAAA-MM-DD>_<id>" de la última fila mostrada (fecha de aplicación o de ajuste; id de
    // la actualización o del contrato).
    private static final Pattern CURSOR = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})_(\\d{1,9})$");

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

    /** Un contrato con su próxima actualización: cuántas lleva aplicadas decide qué fecha de ajuste toca. */
    private record Candidata(Contrato contrato, LocalDate fechaAjuste) {
    }

    /**
     * La próxima actualización de cada contrato que todavía la tiene (una por contrato), de la más cercana a la más lejana.
     * Se calcula con una sola consulta de cuántas lleva aplicadas cada contrato; los datos de las personas y de la
     * propiedad se leen después, solo de las filas que se muestran.
     */
    private List<Candidata> candidatas() {
        Map<Integer, Integer> aplicadas = actualizacionValorDAO.contarPorContrato();
        List<Candidata> lista = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (Contrato c : contratoDAO.readAll()) {
            // Un contrato que todavía no empezó también tiene su primera actualización programada.
            if (c.getFechaFin().isBefore(hoy)) {
                continue;
            }
            if (c.getPeriodicidadActualizacion() == null || c.getPeriodicidadActualizacion() <= 0 || c.getIndiceActualizacion() == null) {
                continue;
            }
            LocalDate fechaAjuste = CalculoActualizacion.fechaAjuste(c.getFechaInicio(), c.getPeriodicidadActualizacion(),
                    aplicadas.getOrDefault(c.getId(), 0) + 1);
            if (fechaAjuste.isAfter(c.getFechaFin())) {
                continue; // el contrato termina antes de su próxima actualización
            }
            lista.add(new Candidata(c, fechaAjuste));
        }
        lista.sort(Comparator.comparing(Candidata::fechaAjuste).thenComparing(x -> x.contrato().getId()));
        return lista;
    }

    private FilaProxima filaProxima(Candidata x, LocalDate hoy) {
        Contrato c = x.contrato();
        List<ActualizacionValor> aplicadas = actualizacionValorDAO.selectByContrato(c.getId());
        BigDecimal valorActual = aplicadas.isEmpty() ? c.getValorInicial() : aplicadas.get(aplicadas.size() - 1).getValorNuevo();
        return new FilaProxima(
                c.getId(),
                direccionDe(c),
                inquilinosDe(c),
                c.getIndiceActualizacion().name(),
                c.getPeriodicidadActualizacion(),
                x.fechaAjuste(),
                YearMonth.from(x.fechaAjuste()).toString(),
                valorActual,
                estadoProxima(x.fechaAjuste(), hoy));
    }

    private String direccionDe(Contrato c) {
        Propiedad propiedad = propiedadDAO.read(c.getIdPropiedad());
        return propiedad == null ? "" : propiedad.getDireccion();
    }

    private List<PersonaVista> inquilinosDe(Contrato c) {
        List<PersonaVista> inquilinos = new ArrayList<>();
        for (Inquilino inq : contratoDAO.selectInquilinos(c.getId())) {
            inquilinos.add(new PersonaVista(inq.getNombre(), inq.getApellido(), inq.getDni(), inq.getTelefono()));
        }
        return inquilinos;
    }

    /** Fecha e id de un cursor "AAAA-MM-DD_id"; null si no hay cursor. Un cursor mal formado o con una fecha imposible se rechaza. */
    private record Cursor(LocalDate fecha, int id) {
    }

    private static Cursor cursor(String despues) {
        if (despues == null || despues.isBlank()) {
            return null;
        }
        Matcher m = CURSOR.matcher(despues.trim());
        PagoServicio.requerido(m.matches(), PagoServicio.MENSAJE_CURSOR);
        try {
            return new Cursor(LocalDate.parse(m.group(1)), Integer.parseInt(m.group(2)));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(PagoServicio.MENSAJE_CURSOR);
        }
    }

    /**
     * Consulta para la pantalla: la fecha de hoy y los meses del selector. Del futuro solo se ofrece el mes que viene
     * (los más lejanos no interesan); de los meses pasados, cualquiera con una actualización aplicada o esperando el índice.
     */
    public PanelActualizaciones panel() {
        LocalDate hoy = LocalDate.now();
        YearMonth proximo = YearMonth.from(hoy).plusMonths(1);
        TreeSet<YearMonth> meses = new TreeSet<>(Comparator.reverseOrder());
        meses.add(proximo);
        for (LocalDate periodo : actualizacionValorDAO.selectPeriodos()) {
            meses.add(YearMonth.from(periodo));
        }
        for (Candidata x : candidatas()) {
            meses.add(YearMonth.from(x.fechaAjuste()));
        }
        List<String> lista = new ArrayList<>();
        for (YearMonth m : meses) {
            if (!m.isAfter(proximo)) {
                lista.add(m.toString());
            }
        }
        return new PanelActualizaciones(hoy, proximo.toString(), lista);
    }

    /**
     * Actualizaciones ya aplicadas de a una página, de la más nueva a la más vieja, con filtro opcional por mes y por texto
     * (DNI, nombre o dirección). "despues" es el cursor que devolvió la página anterior (null = la primera).
     */
    public Pagina<FilaActualizacion> aplicadas(YearMonth mes, String texto, String despues, Integer tamanio) {
        int n = PagoServicio.tamanioValido(tamanio, TAMANIO_APLICADAS);
        String busqueda = PagoServicio.busquedaValida(texto);
        Cursor cursor = cursor(despues);
        LocalDate periodo = mes == null ? null : mes.atDay(1);
        // Se piden n + 1: la fila de más solo sirve para saber si hay una página siguiente.
        List<ActualizacionValor> tanda = actualizacionValorDAO.selectPagina(periodo, busqueda,
                cursor == null ? null : cursor.fecha(), cursor == null ? null : cursor.id(), n + 1);
        boolean hayMas = tanda.size() > n;
        List<ActualizacionValor> pagina = hayMas ? tanda.subList(0, n) : tanda;
        List<FilaActualizacion> filas = new ArrayList<>();
        Map<Integer, Contrato> contratos = new HashMap<>();
        for (ActualizacionValor a : pagina) {
            Contrato c = contratos.computeIfAbsent(a.getIdContrato(), contratoDAO::read);
            filas.add(new FilaActualizacion(
                    a.getId(),
                    a.getFecha(),
                    a.getIdContrato(),
                    c == null ? "" : direccionDe(c),
                    c == null ? new ArrayList<>() : inquilinosDe(c),
                    a.getIndice(),
                    c != null ? c.getPeriodicidadActualizacion() : null,
                    YearMonth.from(a.getPeriodoDesde()).toString(),
                    a.getValorAnterior(),
                    a.getValorNuevo(),
                    CalculoActualizacion.porcentaje(a.getFactor())));
        }
        String siguiente = null;
        if (hayMas) {
            ActualizacionValor ultima = pagina.get(pagina.size() - 1);
            siguiente = ultima.getFecha() + "_" + ultima.getId();
        }
        return new Pagina<>(filas, actualizacionValorDAO.contar(periodo, busqueda), n, siguiente);
    }

    /**
     * Próximas actualizaciones de a una página, de la más cercana a la más lejana (una por contrato), con filtro opcional
     * por mes de ajuste y por texto. Una próxima todavía no existe como fila en la base (su fecha sale del contrato y de
     * cuántas lleva aplicadas), así que el cursor es "la última que ya viste" (fecha de ajuste y contrato) sobre ese orden.
     */
    public Pagina<FilaProxima> proximas(YearMonth mes, String texto, String despues, Integer tamanio) {
        int n = PagoServicio.tamanioValido(tamanio, TAMANIO_PROXIMAS);
        String busqueda = PagoServicio.busquedaValida(texto);
        Cursor cursor = cursor(despues);
        Set<Integer> coinciden = busqueda.isEmpty() ? null : actualizacionValorDAO.contratosQueCoinciden(busqueda);
        List<Candidata> filtradas = new ArrayList<>();
        for (Candidata x : candidatas()) {
            boolean delMes = mes == null || YearMonth.from(x.fechaAjuste()).equals(mes);
            if (delMes && (coinciden == null || coinciden.contains(x.contrato().getId()))) {
                filtradas.add(x);
            }
        }
        List<Candidata> pagina = new ArrayList<>();
        boolean hayMas = false;
        for (Candidata x : filtradas) {
            if (cursor != null) {
                int orden = x.fechaAjuste().compareTo(cursor.fecha());
                if (orden < 0 || (orden == 0 && x.contrato().getId() <= cursor.id())) {
                    continue; // ya se mostró en una página anterior
                }
            }
            if (pagina.size() == n) {
                hayMas = true;
                break;
            }
            pagina.add(x);
        }
        LocalDate hoy = LocalDate.now();
        List<FilaProxima> filas = new ArrayList<>();
        for (Candidata x : pagina) {
            filas.add(filaProxima(x, hoy));
        }
        String siguiente = null;
        if (hayMas) {
            Candidata ultima = pagina.get(pagina.size() - 1);
            siguiente = ultima.fechaAjuste() + "_" + ultima.contrato().getId();
        }
        return new Pagina<>(filas, filtradas.size(), n, siguiente);
    }
}

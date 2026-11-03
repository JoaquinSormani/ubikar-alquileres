package com.ubikar.servicio;

import com.ubikar.dao.ContratoDAO;
import com.ubikar.dao.PagoDAO;
import com.ubikar.dao.PropiedadDAO;
import com.ubikar.dao.PropietarioDAO;
import com.ubikar.dao.Sql2oDAO;
import com.ubikar.documento.DatosRecibo;
import com.ubikar.documento.Formato;
import com.ubikar.documento.GeneradorRecibo;
import com.ubikar.documento.ReciboPdf;
import com.ubikar.modelo.Contrato;
import com.ubikar.modelo.EstadoPago;
import com.ubikar.modelo.Inquilino;
import com.ubikar.modelo.MedioPago;
import com.ubikar.modelo.Pago;
import com.ubikar.modelo.Propiedad;
import com.ubikar.modelo.Propietario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sql2o.Connection;

/**
 * Registrar Pago. Cada contrato vigente tiene un cobro por mes (se arman a medida que el mes llega):
 * el alquiler se paga por mes adelantado y vence el día 10. Si se paga después, se suma el punitorio
 * por mora (Punitorio) y al registrar el pago se emite el recibo (GeneradorRecibo).
 */
public class PagoServicio {

    private static final int DIA_VENCIMIENTO = 10;

    private final ContratoDAO contratoDAO = new ContratoDAO();
    private final PropiedadDAO propiedadDAO = new PropiedadDAO();
    private final PropietarioDAO propietarioDAO = new PropietarioDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final GeneradorRecibo generadorRecibo = new ReciboPdf();

    // ---------- vistas que consume la pantalla ----------

    public record PersonaVista(String nombre, String apellido, String dni, String telefono) {
    }

    /** Una fila de la tabla: un cobro (pendiente o pagado) con los datos del contrato. */
    public record FilaPago(Integer idPago, Integer idContrato, String direccion, List<PersonaVista> inquilinos,
            String periodo, String periodoTexto, LocalDate fechaVencimiento, BigDecimal monto,
            int diasAtraso, BigDecimal punitorio, BigDecimal total, LocalDate fechaPago, MedioPago medioPago) {
    }

    /** Pendientes de cobro (con el punitorio a la fecha) y pagos realizados en el mes en curso. */
    public record PanelPagos(LocalDate fecha, List<FilaPago> pendientes, List<FilaPago> realizadosDelMes) {
    }

    public record Calculo(int diasAtraso, BigDecimal punitorio, BigDecimal total) {
    }

    // ---------- consultas ----------

    /** Cobros pendientes de los contratos vigentes y pagos del mes, para la pantalla de Registrar Pago. */
    public PanelPagos panel() {
        LocalDate hoy = LocalDate.now();
        YearMonth mes = YearMonth.from(hoy);
        List<FilaPago> pendientes = new ArrayList<>();
        List<FilaPago> realizados = new ArrayList<>();
        for (Contrato c : contratoDAO.readAll()) {
            if (c.getFechaInicio().isAfter(hoy) || c.getFechaFin().isBefore(hoy)) {
                continue;
            }
            asegurarCobros(c, hoy);
            Propiedad propiedad = propiedadDAO.read(c.getIdPropiedad());
            String direccion = propiedad == null ? "" : propiedad.getDireccion();
            List<PersonaVista> inquilinos = new ArrayList<>();
            for (Inquilino i : contratoDAO.selectInquilinos(c.getId())) {
                inquilinos.add(new PersonaVista(i.getNombre(), i.getApellido(), i.getDni(), i.getTelefono()));
            }
            for (Pago p : pagoDAO.selectByContrato(c.getId())) {
                if (p.getEstado() == EstadoPago.PENDIENTE) {
                    Calculo calculo = calcular(p, hoy);
                    pendientes.add(fila(p, c, direccion, inquilinos, calculo.diasAtraso(), calculo.punitorio()));
                } else if (p.getFecha() != null && YearMonth.from(p.getFecha()).equals(mes)) {
                    realizados.add(fila(p, c, direccion, inquilinos, p.getDiasAtraso(), p.getPunitorio()));
                }
            }
        }
        pendientes.sort(Comparator.comparing(FilaPago::fechaVencimiento).thenComparing(FilaPago::direccion, String.CASE_INSENSITIVE_ORDER));
        realizados.sort(Comparator.comparing(FilaPago::fechaPago).reversed().thenComparing(FilaPago::idPago, Comparator.reverseOrder()));
        return new PanelPagos(hoy, pendientes, realizados);
    }

    /** Mora y total de un cobro pendiente si se pagara en la fecha indicada (hoy si no viene). */
    public Calculo calculo(Integer idPago, LocalDate fecha) {
        Pago p = pagoDAO.read(idPago);
        requerido(p != null, "El cobro indicado no existe.");
        requerido(p.getEstado() == EstadoPago.PENDIENTE, "El período de " + textoPeriodo(YearMonth.from(p.getPeriodo())) + " ya está pagado.");
        return calcular(p, fecha == null ? LocalDate.now() : fecha);
    }

    // ---------- registrar pago ----------

    public Pago registrar(SolicitudPago s) {
        requerido(s != null && s.getIdPago() != null, "Falta elegir el cobro que se paga.");
        Pago p = pagoDAO.read(s.getIdPago());
        requerido(p != null, "El cobro indicado no existe.");
        YearMonth periodo = YearMonth.from(p.getPeriodo());
        requerido(p.getEstado() == EstadoPago.PENDIENTE, "El período de " + textoPeriodo(periodo) + " ya está pagado.");
        Contrato c = contrato(p.getIdContrato());

        LocalDate hoy = LocalDate.now();
        LocalDate fechaPago = s.getFechaPago() == null ? hoy : s.getFechaPago();
        requerido(!fechaPago.isAfter(hoy), "La fecha de pago no puede ser posterior a hoy.");
        requerido(!fechaPago.isBefore(c.getFechaInicio().minusMonths(1)), "La fecha de pago es demasiado anterior al inicio del contrato.");

        Punitorio.Mora mora = Punitorio.calcular(p.getMonto(), p.getFechaVencimiento(), fechaPago);
        p.setFecha(fechaPago);
        p.setDiasAtraso(mora.diasAtraso());
        p.setPunitorio(mora.punitorio());
        p.setEstado(EstadoPago.PAGADO);
        p.setMedioPago(s.getMedioPago() == null ? MedioPago.EFECTIVO : s.getMedioPago());
        pagoDAO.update(p);
        return p;
    }

    // ---------- recibo ----------

    public byte[] recibo(Integer idPago) {
        Pago p = pagoDAO.read(idPago);
        requerido(p != null, "El pago indicado no existe.");
        requerido(p.getEstado() == EstadoPago.PAGADO, "Ese cobro todavía no tiene un pago registrado.");
        Contrato c = contrato(p.getIdContrato());
        Propiedad propiedad = propiedadDAO.read(c.getIdPropiedad());
        Propietario propietario = propietarioDAO.read(propiedad.getIdPropietario());
        List<Inquilino> inquilinos = contratoDAO.selectInquilinos(c.getId());
        StringBuilder nombres = new StringBuilder();
        for (Inquilino i : inquilinos) {
            if (nombres.length() > 0) {
                nombres.append(" y ");
            }
            nombres.append(i.getNombre()).append(' ').append(i.getApellido()).append(" (DNI ").append(Formato.dni(i.getDni())).append(')');
        }
        BigDecimal total = p.getMonto().add(p.getPunitorio());
        DatosRecibo datos = new DatosRecibo(p.getId(), p.getFecha(), nombres.toString(), propiedad.getDireccion(),
                propietario.getNombre() + " " + propietario.getApellido(), textoPeriodo(YearMonth.from(p.getPeriodo())),
                p.getFechaVencimiento(), p.getFecha(), p.getMedioPago(), p.getMonto(), p.getDiasAtraso(), p.getPunitorio(), total);
        return generadorRecibo.generar(datos);
    }

    public String contenidoRecibo() {
        return generadorRecibo.tipoContenido();
    }

    // ---------- cobros mensuales ----------

    /**
     * Arma los cobros pendientes de cada mes del contrato hasta el mes actual (sin repetir los que ya
     * existen). El monto de cada nuevo cobro es el del último cobro conocido: cuando exista Actualizar
     * Valor de Alquiler, ese caso de uso cambia el monto de los cobros futuros.
     */
    void asegurarCobros(Contrato c, LocalDate hoy) {
        YearMonth desde = YearMonth.from(c.getFechaInicio());
        YearMonth hasta = YearMonth.from(hoy);
        YearMonth fin = YearMonth.from(c.getFechaFin());
        if (fin.isBefore(hasta)) {
            hasta = fin;
        }
        if (desde.isAfter(hasta)) {
            return;
        }
        List<Pago> existentes = pagoDAO.selectByContrato(c.getId());
        Set<YearMonth> tiene = new HashSet<>();
        for (Pago p : existentes) {
            tiene.add(YearMonth.from(p.getPeriodo()));
        }
        List<Pago> nuevos = new ArrayList<>();
        BigDecimal monto = c.getValorInicial();
        for (YearMonth m = desde; !m.isAfter(hasta); m = m.plusMonths(1)) {
            if (tiene.contains(m)) {
                final YearMonth mes = m;
                monto = existentes.stream().filter(p -> YearMonth.from(p.getPeriodo()).equals(mes)).findFirst().get().getMonto();
                continue;
            }
            Pago nuevo = new Pago();
            nuevo.setIdContrato(c.getId());
            nuevo.setPeriodo(m.atDay(1));
            nuevo.setFechaVencimiento(vencimiento(m, c.getFechaInicio()));
            nuevo.setMonto(monto);
            nuevo.setEstado(EstadoPago.PENDIENTE);
            nuevos.add(nuevo);
        }
        if (nuevos.isEmpty()) {
            return;
        }
        try (Connection con = Sql2oDAO.getSql2o().beginTransaction()) {
            for (Pago nuevo : nuevos) {
                nuevo.setId(pagoDAO.create(con, nuevo));
            }
            con.commit(false);
        }
    }

    // El alquiler vence el día 10 del mes; el primer mes, nunca antes del inicio del contrato.
    static LocalDate vencimiento(YearMonth mes, LocalDate inicioContrato) {
        LocalDate v = mes.atDay(DIA_VENCIMIENTO);
        return v.isBefore(inicioContrato) ? inicioContrato : v;
    }

    // ---------- armado de vistas ----------

    private Calculo calcular(Pago p, LocalDate fecha) {
        Punitorio.Mora mora = Punitorio.calcular(p.getMonto(), p.getFechaVencimiento(), fecha);
        return new Calculo(mora.diasAtraso(), mora.punitorio(), p.getMonto().add(mora.punitorio()));
    }

    private FilaPago fila(Pago p, Contrato c, String direccion, List<PersonaVista> inquilinos, int diasAtraso, BigDecimal punitorio) {
        YearMonth periodo = YearMonth.from(p.getPeriodo());
        return new FilaPago(p.getId(), c.getId(), direccion, inquilinos, periodo.toString(), textoPeriodo(periodo),
                p.getFechaVencimiento(), p.getMonto(), diasAtraso, punitorio, p.getMonto().add(punitorio), p.getFecha(), p.getMedioPago());
    }

    private Contrato contrato(Integer id) {
        Contrato c = contratoDAO.read(id);
        requerido(c != null, "El contrato indicado no existe.");
        return c;
    }

    static String textoPeriodo(YearMonth m) {
        return Formato.mes(m.atDay(1)) + " de " + m.getYear();
    }

    private static void requerido(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}

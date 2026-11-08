package com.ubikar.servicio;

import com.ubikar.config.ReglasDeCobro;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sql2o.Connection;

/**
 * Registrar Pago. Cada contrato vigente tiene un cobro por mes (se arman a medida que el mes llega):
 * el alquiler se paga por mes adelantado y vence el día 10. Si se paga después, se suma el punitorio
 * por mora (Punitorio) y al registrar el pago se emite el recibo (GeneradorRecibo).
 */
public class PagoServicio {

    private static final int DIA_VENCIMIENTO = ReglasDeCobro.DIA_VENCIMIENTO;
    // Un cobro a la vez: dos pagos simultáneos (por ejemplo, un doble clic) no cobran dos veces el mismo
    // período ni se pisan al numerar el recibo.
    private static final Object CERROJO_PAGOS = new Object();

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
            int diasAtraso, BigDecimal punitorio, BigDecimal total, LocalDate fechaPago, MedioPago medioPago, Integer numeroRecibo) {
    }

    /**
     * Lo mínimo que necesita la pantalla antes de pedir las páginas: la fecha de hoy y los meses que tienen cobros
     * (del más nuevo al más viejo, formato AAAA-MM), para el selector. Los pendientes y los realizados no vienen
     * acá: son muchos y se piden de a una página con pendientes(...) y realizados(...).
     */
    public record PanelPagos(LocalDate fecha, List<String> meses) {
    }

    /**
     * Una página de cobros. "total" es la cantidad que cumple el filtro y "siguiente" es el cursor para pedir la
     * página que sigue (null cuando no hay más).
     */
    public record PaginaPagos(List<FilaPago> filas, int total, int tamanio, String siguiente) {
    }

    public record Calculo(int diasAtraso, BigDecimal punitorio, BigDecimal total) {
    }

    static final int TAMANIO_PENDIENTES = 5;
    static final int TAMANIO_REALIZADOS = 10;
    static final int TAMANIO_MAXIMO = 50;
    static final int LARGO_MAXIMO_BUSQUEDA = 100;
    // Cursor de los realizados: "<número de recibo>-<id del pago>" del último pago mostrado.
    private static final Pattern CURSOR_REALIZADOS = Pattern.compile("^(\\d{1,9})-(\\d{1,9})$");
    // Cursor de los pendientes: "<vencimiento AAAA-MM-DD>_<id del cobro>" del último cobro mostrado.
    private static final Pattern CURSOR_PENDIENTES = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})_(\\d{1,9})$");

    // ---------- consultas ----------

    /**
     * Arma los cobros que faltan de cada contrato vigente (uno por mes, hasta el mes actual) y devuelve los meses para el
     * selector. La pantalla lo llama una vez al cargar; después pide las páginas, que ya no generan nada.
     */
    public PanelPagos panel() {
        LocalDate hoy = LocalDate.now();
        for (Contrato c : contratoDAO.readAll()) {
            if (c.getFechaInicio().isAfter(hoy) || c.getFechaFin().isBefore(hoy)) {
                continue;
            }
            asegurarCobros(c, hoy);
        }
        List<String> meses = new ArrayList<>();
        for (LocalDate periodo : pagoDAO.selectPeriodos()) {
            meses.add(YearMonth.from(periodo).toString());
        }
        return new PanelPagos(hoy, meses);
    }

    /**
     * Cobros pendientes de a una página, del más vencido al menos vencido, con el punitorio calculado a hoy y filtro opcional
     * por mes y por texto (DNI, nombre o dirección). "despues" es el cursor que devolvió la página anterior (null = la primera).
     */
    public PaginaPagos pendientes(YearMonth mes, String texto, String despues, Integer tamanio) {
        int n = tamanioValido(tamanio, TAMANIO_PENDIENTES);
        String busqueda = busquedaValida(texto);
        LocalDate vencimientoAnterior = null;
        Integer idAnterior = null;
        if (despues != null && !despues.isBlank()) {
            Matcher m = CURSOR_PENDIENTES.matcher(despues.trim());
            requerido(m.matches(), MENSAJE_CURSOR);
            try {
                vencimientoAnterior = LocalDate.parse(m.group(1));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(MENSAJE_CURSOR);
            }
            idAnterior = Integer.valueOf(m.group(2));
        }
        LocalDate hoy = LocalDate.now();
        LocalDate periodo = mes == null ? null : mes.atDay(1);
        List<Pago> tanda = pagoDAO.selectPendientes(hoy, periodo, busqueda, vencimientoAnterior, idAnterior, n + 1);
        boolean hayMas = tanda.size() > n;
        List<Pago> pagina = hayMas ? tanda.subList(0, n) : tanda;
        VistaDeContratos vista = new VistaDeContratos();
        List<FilaPago> filas = new ArrayList<>();
        for (Pago p : pagina) {
            Contrato c = vista.contrato(p.getIdContrato());
            Calculo calculo = calcular(p, hoy);
            filas.add(fila(p, c, vista.direccion(c), vista.inquilinos(c), calculo.diasAtraso(), calculo.punitorio()));
        }
        String siguiente = null;
        if (hayMas) {
            Pago ultimo = pagina.get(pagina.size() - 1);
            siguiente = ultimo.getFechaVencimiento() + "_" + ultimo.getId();
        }
        return new PaginaPagos(filas, pagoDAO.contarPendientes(hoy, periodo, busqueda), n, siguiente);
    }

    /**
     * Pagos realizados de a una página, del recibo más nuevo al más viejo, con filtro opcional por mes y por texto
     * (DNI, nombre o dirección). La paginación es por cursor: "despues" es el cursor que devolvió la página anterior
     * (null = la primera). Un pago nuevo siempre trae un recibo mayor, así que aparece arriba de todo y no repite ni
     * saltea filas en las páginas siguientes.
     */
    public PaginaPagos realizados(YearMonth mes, String texto, String despues, Integer tamanio) {
        int n = tamanioValido(tamanio, TAMANIO_REALIZADOS);
        String busqueda = busquedaValida(texto);
        Integer reciboAnterior = null;
        Integer idAnterior = null;
        if (despues != null && !despues.isBlank()) {
            Matcher m = CURSOR_REALIZADOS.matcher(despues.trim());
            requerido(m.matches(), MENSAJE_CURSOR);
            reciboAnterior = Integer.valueOf(m.group(1));
            idAnterior = Integer.valueOf(m.group(2));
        }
        LocalDate periodo = mes == null ? null : mes.atDay(1);
        // Se piden n + 1: la fila de más solo sirve para saber si hay una página siguiente.
        List<Pago> tanda = pagoDAO.selectRealizados(periodo, busqueda, reciboAnterior, idAnterior, n + 1);
        boolean hayMas = tanda.size() > n;
        List<Pago> pagina = hayMas ? tanda.subList(0, n) : tanda;
        VistaDeContratos vista = new VistaDeContratos();
        List<FilaPago> filas = new ArrayList<>();
        for (Pago p : pagina) {
            Contrato c = vista.contrato(p.getIdContrato());
            filas.add(fila(p, c, vista.direccion(c), vista.inquilinos(c), p.getDiasAtraso(), p.getPunitorio()));
        }
        String siguiente = null;
        if (hayMas) {
            Pago ultimo = pagina.get(pagina.size() - 1);
            siguiente = ultimo.getNumeroRecibo() + "-" + ultimo.getId();
        }
        return new PaginaPagos(filas, pagoDAO.contarRealizados(periodo, busqueda), n, siguiente);
    }

    static final String MENSAJE_CURSOR = "El punto de partida de la página no es válido.";

    static int tamanioValido(Integer tamanio, int porDefecto) {
        int n = tamanio == null ? porDefecto : tamanio;
        requerido(n >= 1 && n <= TAMANIO_MAXIMO, "El tamaño de la página tiene que estar entre 1 y " + TAMANIO_MAXIMO + ".");
        return n;
    }

    static String busquedaValida(String texto) {
        String busqueda = texto == null ? "" : texto.trim();
        requerido(busqueda.length() <= LARGO_MAXIMO_BUSQUEDA, "La búsqueda no puede superar los " + LARGO_MAXIMO_BUSQUEDA + " caracteres.");
        return busqueda;
    }

    /** Datos de los contratos de una página, leídos una sola vez por contrato aunque aparezca en varias filas. */
    private final class VistaDeContratos {

        private final Map<Integer, Contrato> contratos = new HashMap<>();
        private final Map<Integer, String> direcciones = new HashMap<>();
        private final Map<Integer, List<PersonaVista>> inquilinos = new HashMap<>();

        Contrato contrato(Integer id) {
            return contratos.computeIfAbsent(id, PagoServicio.this::contrato);
        }

        String direccion(Contrato c) {
            return direcciones.computeIfAbsent(c.getId(), k -> direccionDe(c));
        }

        List<PersonaVista> inquilinos(Contrato c) {
            return inquilinos.computeIfAbsent(c.getId(), k -> inquilinosDe(c));
        }
    }

    private String direccionDe(Contrato c) {
        Propiedad propiedad = propiedadDAO.read(c.getIdPropiedad());
        return propiedad == null ? "" : propiedad.getDireccion();
    }

    private List<PersonaVista> inquilinosDe(Contrato c) {
        List<PersonaVista> inquilinos = new ArrayList<>();
        for (Inquilino i : contratoDAO.selectInquilinos(c.getId())) {
            inquilinos.add(new PersonaVista(i.getNombre(), i.getApellido(), i.getDni(), i.getTelefono()));
        }
        return inquilinos;
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
        synchronized (CERROJO_PAGOS) {
            return registrarConCerrojo(s);
        }
    }

    private Pago registrarConCerrojo(SolicitudPago s) {
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
        // el número de recibo se asigna al cobrar, correlativo en el orden en que se paga
        try (Connection con = Sql2oDAO.getSql2o().beginTransaction()) {
            p.setNumeroRecibo(pagoDAO.siguienteNumeroRecibo(con));
            pagoDAO.update(con, p);
            con.commit(false);
        }
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
        DatosRecibo datos = new DatosRecibo(numeroDe(p), p.getFecha(), nombres.toString(), propiedad.getDireccion(),
                propietario.getNombre() + " " + propietario.getApellido(), textoPeriodo(YearMonth.from(p.getPeriodo())),
                p.getFechaVencimiento(), p.getFecha(), p.getMedioPago(), p.getMonto(), p.getDiasAtraso(), p.getPunitorio(), total);
        return generadorRecibo.generar(datos);
    }

    public String contenidoRecibo() {
        return generadorRecibo.tipoContenido();
    }

    /** Número con el que se emitió el recibo de un pago ya registrado. */
    public int numeroRecibo(Integer idPago) {
        Pago p = pagoDAO.read(idPago);
        requerido(p != null, "El pago indicado no existe.");
        return numeroDe(p);
    }

    private static int numeroDe(Pago p) {
        return p.getNumeroRecibo() != null ? p.getNumeroRecibo() : p.getId();
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
                p.getFechaVencimiento(), p.getMonto(), diasAtraso, punitorio, p.getMonto().add(punitorio), p.getFecha(), p.getMedioPago(),
                p.getNumeroRecibo());
    }

    private Contrato contrato(Integer id) {
        Contrato c = contratoDAO.read(id);
        requerido(c != null, "El contrato indicado no existe.");
        return c;
    }

    static String textoPeriodo(YearMonth m) {
        return Formato.mes(m.atDay(1)) + " de " + m.getYear();
    }

    static void requerido(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}

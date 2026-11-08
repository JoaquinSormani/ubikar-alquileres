package com.ubikar.dao;

import com.ubikar.modelo.EstadoPago;
import com.ubikar.modelo.Pago;
import java.time.LocalDate;
import java.util.List;
import org.sql2o.Connection;
import org.sql2o.Query;

public class PagoDAO {

    public List<Pago> readAll() {
        String sql = "SELECT * FROM pago";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Pago.class);
        }
    }

    public Pago read(Integer id) {
        String sql = "SELECT * FROM pago WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Pago.class);
        }
    }

    public List<Pago> selectByContrato(Integer idContrato) {
        String sql = "SELECT * FROM pago WHERE idContrato = :idContrato ORDER BY periodo";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(Pago.class);
        }
    }

    public Pago selectByContratoYPeriodo(Integer idContrato, LocalDate periodo) {
        String sql = "SELECT * FROM pago WHERE idContrato = :idContrato AND periodo = :periodo";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .addParameter("periodo", periodo)
                    .executeAndFetchFirst(Pago.class);
        }
    }

    public List<Pago> selectByEstado(EstadoPago estado) {
        String sql = "SELECT * FROM pago WHERE estado = :estado ORDER BY id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("estado", estado.name())
                    .executeAndFetch(Pago.class);
        }
    }

    public Integer create(Pago p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, p);
        }
    }

    public Integer create(Connection con, Pago p) {
        String sql = "INSERT INTO pago (idContrato, periodo, fechaVencimiento, fecha, monto, indiceActualizacion, "
                + "diasAtraso, punitorio, estado, medioPago, numeroRecibo) "
                + "VALUES (:idContrato, :periodo, :fechaVencimiento, :fecha, :monto, :indiceActualizacion, "
                + ":diasAtraso, :punitorio, :estado, :medioPago, :numeroRecibo)";
        return con.createQuery(sql, true).bind(p).executeUpdate().getKey(Integer.class);
    }

    public void update(Pago p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, p);
        }
    }

    public void update(Connection con, Pago p) {
        String sql = "UPDATE pago SET idContrato = :idContrato, periodo = :periodo, fechaVencimiento = :fechaVencimiento, "
                + "fecha = :fecha, monto = :monto, indiceActualizacion = :indiceActualizacion, diasAtraso = :diasAtraso, "
                + "punitorio = :punitorio, estado = :estado, medioPago = :medioPago, numeroRecibo = :numeroRecibo WHERE id = :id";
        con.createQuery(sql).bind(p).executeUpdate();
    }

    /**
     * Próximo número de recibo: correlativo en el orden en que se cobra. Se pide dentro de la misma
     * transacción que guarda el pago y bloquea la lectura, así dos cobros simultáneos no repiten número.
     */
    public int siguienteNumeroRecibo(Connection con) {
        String sql = "SELECT COALESCE(MAX(numeroRecibo), 0) + 1 FROM pago FOR UPDATE";
        return con.createQuery(sql).executeScalar(Integer.class);
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM pago WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }

    // ---------- pagos de a una página (paginación por cursor) ----------

    /**
     * Una tanda de pagos realizados, del recibo más nuevo al más viejo. No usa OFFSET: la página siguiente pide
     * "los que tienen un recibo menor que el último que ya viste". Así, un pago nuevo (que siempre trae un recibo
     * mayor) queda arriba de todo y nunca hace que una fila se repita ni se saltee en las páginas de abajo.
     * El índice único del recibo resuelve el orden, y el índice (periodo, numeroRecibo), el filtro por mes: sin ordenar en memoria.
     *
     * @param periodo        solo los pagos de ese mes (null = todos)
     * @param texto          DNI, nombre o dirección (null o vacío = sin búsqueda)
     * @param reciboAnterior cursor: número de recibo del último pago ya mostrado (null = desde el más nuevo)
     * @param idAnterior     cursor: id de ese último pago (desempata; el recibo ya es único)
     * @param limite         cuántos devolver
     */
    public List<Pago> selectRealizados(LocalDate periodo, String texto, Integer reciboAnterior, Integer idAnterior, int limite) {
        // Un pago realizado siempre tiene recibo (se asigna en la misma transacción que lo guarda).
        StringBuilder sql = new StringBuilder("SELECT p.* FROM pago p WHERE p.estado = 'PAGADO' AND p.numeroRecibo IS NOT NULL");
        String digitos = filtro(sql, periodo, texto);
        if (reciboAnterior != null) {
            sql.append(" AND (p.numeroRecibo < :reciboAnterior OR (p.numeroRecibo = :reciboAnterior AND p.id < :idAnterior))");
        }
        sql.append(" ORDER BY p.numeroRecibo DESC, p.id DESC LIMIT :limite");
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Query q = con.createQuery(sql.toString());
            parametrosFiltro(q, periodo, texto, digitos);
            if (reciboAnterior != null) {
                q.addParameter("reciboAnterior", reciboAnterior).addParameter("idAnterior", idAnterior == null ? Integer.MAX_VALUE : idAnterior);
            }
            return q.addParameter("limite", limite).executeAndFetch(Pago.class);
        }
    }

    /** Cuántos pagos realizados cumplen el filtro (para mostrar "N en total"). */
    public int contarRealizados(LocalDate periodo, String texto) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pago p WHERE p.estado = 'PAGADO' AND p.numeroRecibo IS NOT NULL");
        String digitos = filtro(sql, periodo, texto);
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Query q = con.createQuery(sql.toString());
            parametrosFiltro(q, periodo, texto, digitos);
            return q.executeScalar(Integer.class);
        }
    }

    // Solo cuentan los cobros de contratos vigentes a la fecha (los que ya terminaron o todavía no empezaron no se cobran).
    private static final String CONTRATO_VIGENTE = " AND EXISTS (SELECT 1 FROM contrato cv WHERE cv.id = p.idContrato"
            + " AND cv.fechaInicio <= :hoy AND cv.fechaFin >= :hoy)";

    /**
     * Una tanda de cobros pendientes de contratos vigentes, del más vencido al menos vencido. Igual que en los realizados, el
     * cursor es "el último que ya viste" (vencimiento e id) y no un OFFSET: si se cobra uno de la página anterior, o se genera
     * el cobro de un mes nuevo (que vence después), no se repite ni se saltea ningún pendiente.
     *
     * @param vencimientoAnterior cursor: vencimiento del último cobro ya mostrado (null = desde el más vencido)
     * @param idAnterior          cursor: id de ese último cobro (desempata los que vencen el mismo día)
     */
    public List<Pago> selectPendientes(LocalDate hoy, LocalDate periodo, String texto, LocalDate vencimientoAnterior, Integer idAnterior, int limite) {
        StringBuilder sql = new StringBuilder("SELECT p.* FROM pago p WHERE p.estado = 'PENDIENTE'").append(CONTRATO_VIGENTE);
        String digitos = filtro(sql, periodo, texto);
        if (vencimientoAnterior != null) {
            sql.append(" AND (p.fechaVencimiento > :vencimientoAnterior OR (p.fechaVencimiento = :vencimientoAnterior AND p.id > :idAnterior))");
        }
        sql.append(" ORDER BY p.fechaVencimiento, p.id LIMIT :limite");
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Query q = con.createQuery(sql.toString()).addParameter("hoy", hoy);
            parametrosFiltro(q, periodo, texto, digitos);
            if (vencimientoAnterior != null) {
                q.addParameter("vencimientoAnterior", vencimientoAnterior).addParameter("idAnterior", idAnterior == null ? 0 : idAnterior);
            }
            return q.addParameter("limite", limite).executeAndFetch(Pago.class);
        }
    }

    /** Cuántos cobros pendientes (de contratos vigentes) cumplen el filtro. */
    public int contarPendientes(LocalDate hoy, LocalDate periodo, String texto) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pago p WHERE p.estado = 'PENDIENTE'").append(CONTRATO_VIGENTE);
        String digitos = filtro(sql, periodo, texto);
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Query q = con.createQuery(sql.toString()).addParameter("hoy", hoy);
            parametrosFiltro(q, periodo, texto, digitos);
            return q.executeScalar(Integer.class);
        }
    }

    /** Los meses (primer día) que tienen algún cobro, del más nuevo al más viejo: alimenta el selector de meses. */
    public List<LocalDate> selectPeriodos() {
        String sql = "SELECT DISTINCT periodo FROM pago ORDER BY periodo DESC";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeScalarList(LocalDate.class);
        }
    }

    // Agrega al WHERE el filtro por mes y por texto. Devuelve los dígitos del texto (para buscar por DNI) o "" si no hay.
    private static String filtro(StringBuilder sql, LocalDate periodo, String texto) {
        if (periodo != null) {
            sql.append(" AND p.periodo = :periodo");
        }
        String digitos = "";
        if (texto != null && !texto.isBlank()) {
            digitos = texto.replaceAll("\\D", "");
            // Con EXISTS (y no con JOIN) la tabla pago sigue siendo la principal: se recorre en orden y
            // la consulta se corta apenas junta las filas de la página.
            sql.append(" AND (EXISTS (SELECT 1 FROM contrato c JOIN propiedad pr ON pr.id = c.idPropiedad")
                    .append(" WHERE c.id = p.idContrato AND pr.direccion LIKE :texto)")
                    .append(" OR EXISTS (SELECT 1 FROM contrato_inquilino ci JOIN inquilino i ON i.id = ci.idInquilino")
                    .append(" WHERE ci.idContrato = p.idContrato AND (CONCAT(i.nombre, ' ', i.apellido) LIKE :texto");
            if (!digitos.isEmpty()) {
                sql.append(" OR i.dni LIKE :dni");
            }
            sql.append(")))");
        }
        return digitos;
    }

    private static void parametrosFiltro(Query q, LocalDate periodo, String texto, String digitos) {
        if (periodo != null) {
            q.addParameter("periodo", periodo);
        }
        if (texto != null && !texto.isBlank()) {
            q.addParameter("texto", patronContiene(texto));
            if (!digitos.isEmpty()) {
                q.addParameter("dni", digitos + "%");
            }
        }
    }

    /** "%texto%" para LIKE, con %, _ y \ escapados: lo que escribe el usuario se busca tal cual, nunca como comodín. */
    static String patronContiene(String texto) {
        String limpio = texto.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + limpio + "%";
    }
}

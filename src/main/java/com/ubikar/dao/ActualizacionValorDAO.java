package com.ubikar.dao;

import com.ubikar.modelo.ActualizacionValor;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sql2o.Connection;
import org.sql2o.Query;

public class ActualizacionValorDAO {

    public List<ActualizacionValor> readAll() {
        String sql = "SELECT * FROM actualizacion_valor ORDER BY fecha DESC, id DESC";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(ActualizacionValor.class);
        }
    }

    /**
     * Una tanda de actualizaciones aplicadas, de la más nueva a la más vieja. Igual que en los pagos, la paginación es por
     * cursor ("la última que ya viste") y no por OFFSET: una actualización nueva (siempre con fecha de hoy y un id mayor)
     * entra arriba de todo y no repite ni saltea filas en las páginas siguientes.
     *
     * @param periodo        solo las que rigen desde ese mes (día 1), o null para todas
     * @param texto          DNI, nombre del inquilino o dirección de la propiedad (null o vacío = sin filtro)
     * @param fechaAnterior  cursor: fecha de la última actualización ya mostrada (null = desde la más nueva)
     * @param idAnterior     cursor: id de esa actualización (desempata las del mismo día)
     */
    public List<ActualizacionValor> selectPagina(LocalDate periodo, String texto, LocalDate fechaAnterior, Integer idAnterior, int limite) {
        StringBuilder sql = new StringBuilder("SELECT a.* FROM actualizacion_valor a WHERE 1 = 1");
        String digitos = filtro(sql, periodo, texto);
        if (fechaAnterior != null) {
            sql.append(" AND (a.fecha < :fechaAnterior OR (a.fecha = :fechaAnterior AND a.id < :idAnterior))");
        }
        sql.append(" ORDER BY a.fecha DESC, a.id DESC LIMIT :limite");
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Query q = con.createQuery(sql.toString());
            parametrosFiltro(q, periodo, texto, digitos);
            if (fechaAnterior != null) {
                q.addParameter("fechaAnterior", fechaAnterior).addParameter("idAnterior", idAnterior == null ? Integer.MAX_VALUE : idAnterior);
            }
            return q.addParameter("limite", limite).executeAndFetch(ActualizacionValor.class);
        }
    }

    /** Cuántas actualizaciones aplicadas cumplen el filtro (para mostrar "N en total"). */
    public int contar(LocalDate periodo, String texto) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM actualizacion_valor a WHERE 1 = 1");
        String digitos = filtro(sql, periodo, texto);
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Query q = con.createQuery(sql.toString());
            parametrosFiltro(q, periodo, texto, digitos);
            return q.executeScalar(Integer.class);
        }
    }

    /** Los meses (día 1) desde los que rige alguna actualización aplicada, del más nuevo al más viejo. */
    public List<LocalDate> selectPeriodos() {
        String sql = "SELECT DISTINCT periodoDesde FROM actualizacion_valor ORDER BY periodoDesde DESC";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeScalarList(LocalDate.class);
        }
    }

    /** Cuántas actualizaciones lleva aplicadas cada contrato (los que no tienen ninguna no aparecen). */
    public Map<Integer, Integer> contarPorContrato() {
        String sql = "SELECT idContrato FROM actualizacion_valor";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Map<Integer, Integer> cuentas = new HashMap<>();
            for (Integer idContrato : con.createQuery(sql).executeScalarList(Integer.class)) {
                cuentas.merge(idContrato, 1, Integer::sum);
            }
            return cuentas;
        }
    }

    /** Ids de los contratos cuyo inquilino (DNI o nombre) o propiedad (dirección) coinciden con el texto. */
    public Set<Integer> contratosQueCoinciden(String texto) {
        StringBuilder sql = new StringBuilder("SELECT c.id FROM contrato c WHERE 1 = 1");
        String digitos = filtroContrato(sql, "c.id", texto);
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Query q = con.createQuery(sql.toString());
            parametrosTexto(q, texto, digitos);
            return new HashSet<>(q.executeScalarList(Integer.class));
        }
    }

    // Mismo criterio de búsqueda que los pagos: EXISTS (y no JOIN) para que la tabla principal se recorra en orden.
    private static String filtro(StringBuilder sql, LocalDate periodo, String texto) {
        if (periodo != null) {
            sql.append(" AND a.periodoDesde = :periodo");
        }
        return filtroContrato(sql, "a.idContrato", texto);
    }

    private static String filtroContrato(StringBuilder sql, String columnaContrato, String texto) {
        String digitos = "";
        if (texto != null && !texto.isBlank()) {
            digitos = texto.replaceAll("\\D", "");
            sql.append(" AND (EXISTS (SELECT 1 FROM contrato c2 JOIN propiedad pr ON pr.id = c2.idPropiedad")
                    .append(" WHERE c2.id = ").append(columnaContrato).append(" AND pr.direccion LIKE :texto)")
                    .append(" OR EXISTS (SELECT 1 FROM contrato_inquilino ci JOIN inquilino i ON i.id = ci.idInquilino")
                    .append(" WHERE ci.idContrato = ").append(columnaContrato)
                    .append(" AND (CONCAT(i.nombre, ' ', i.apellido) LIKE :texto");
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
        parametrosTexto(q, texto, digitos);
    }

    private static void parametrosTexto(Query q, String texto, String digitos) {
        if (texto != null && !texto.isBlank()) {
            q.addParameter("texto", PagoDAO.patronContiene(texto));
            if (!digitos.isEmpty()) {
                q.addParameter("dni", digitos + "%");
            }
        }
    }

    public ActualizacionValor read(Integer id) {
        String sql = "SELECT * FROM actualizacion_valor WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(ActualizacionValor.class);
        }
    }

    public List<ActualizacionValor> selectByContrato(Integer idContrato) {
        String sql = "SELECT * FROM actualizacion_valor WHERE idContrato = :idContrato ORDER BY periodoDesde ASC";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(ActualizacionValor.class);
        }
    }

    public Integer create(ActualizacionValor a) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, a);
        }
    }

    public Integer create(Connection con, ActualizacionValor a) {
        String sql = "INSERT INTO actualizacion_valor (idContrato, periodoDesde, fecha, indice, valorAnterior, valorNuevo, factor) "
                + "VALUES (:idContrato, :periodoDesde, :fecha, :indice, :valorAnterior, :valorNuevo, :factor)";
        return con.createQuery(sql, true).bind(a).executeUpdate().getKey(Integer.class);
    }

    public void update(ActualizacionValor a) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, a);
        }
    }

    public void update(Connection con, ActualizacionValor a) {
        String sql = "UPDATE actualizacion_valor SET idContrato = :idContrato, periodoDesde = :periodoDesde, "
                + "fecha = :fecha, indice = :indice, valorAnterior = :valorAnterior, valorNuevo = :valorNuevo, "
                + "factor = :factor WHERE id = :id";
        con.createQuery(sql).bind(a).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM actualizacion_valor WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

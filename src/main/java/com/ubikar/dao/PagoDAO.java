package com.ubikar.dao;

import com.ubikar.modelo.EstadoPago;
import com.ubikar.modelo.Pago;
import java.util.List;
import org.sql2o.Connection;

public class PagoDAO {

    public List<Pago> selectAll() {
        String sql = "SELECT * FROM pago";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Pago.class);
        }
    }

    public Pago selectById(Integer id) {
        String sql = "SELECT * FROM pago WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Pago.class);
        }
    }

    public List<Pago> selectByContrato(Integer idContrato) {
        String sql = "SELECT * FROM pago WHERE idContrato = :idContrato ORDER BY id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(Pago.class);
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

    public Integer insert(Pago p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return insert(con, p);
        }
    }

    public Integer insert(Connection con, Pago p) {
        String sql = "INSERT INTO pago (idContrato, fecha, monto, indiceActualizacion, diasAtraso, punitorio, estado) "
                + "VALUES (:idContrato, :fecha, :monto, :indiceActualizacion, :diasAtraso, :punitorio, :estado)";
        return con.createQuery(sql, true).bind(p).executeUpdate().getKey(Integer.class);
    }

    public void update(Pago p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, p);
        }
    }

    public void update(Connection con, Pago p) {
        String sql = "UPDATE pago SET idContrato = :idContrato, fecha = :fecha, monto = :monto, "
                + "indiceActualizacion = :indiceActualizacion, diasAtraso = :diasAtraso, "
                + "punitorio = :punitorio, estado = :estado WHERE id = :id";
        con.createQuery(sql).bind(p).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM pago WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

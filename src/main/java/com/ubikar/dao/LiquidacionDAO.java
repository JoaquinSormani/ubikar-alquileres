package com.ubikar.dao;

import com.ubikar.modelo.Liquidacion;
import java.util.List;
import org.sql2o.Connection;

public class LiquidacionDAO {

    public List<Liquidacion> readAll() {
        String sql = "SELECT * FROM liquidacion";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Liquidacion.class);
        }
    }

    public Liquidacion read(Integer id) {
        String sql = "SELECT * FROM liquidacion WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Liquidacion.class);
        }
    }

    public Liquidacion selectByPago(Integer idPago) {
        String sql = "SELECT * FROM liquidacion WHERE idPago = :idPago";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idPago", idPago)
                    .executeAndFetchFirst(Liquidacion.class);
        }
    }

    public Integer create(Liquidacion l) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, l);
        }
    }

    public Integer create(Connection con, Liquidacion l) {
        String sql = "INSERT INTO liquidacion (idPago, fecha, montoBruto, comision, montoNeto) "
                + "VALUES (:idPago, :fecha, :montoBruto, :comision, :montoNeto)";
        return con.createQuery(sql, true).bind(l).executeUpdate().getKey(Integer.class);
    }

    public void update(Liquidacion l) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, l);
        }
    }

    public void update(Connection con, Liquidacion l) {
        String sql = "UPDATE liquidacion SET idPago = :idPago, fecha = :fecha, montoBruto = :montoBruto, "
                + "comision = :comision, montoNeto = :montoNeto WHERE id = :id";
        con.createQuery(sql).bind(l).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM liquidacion WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

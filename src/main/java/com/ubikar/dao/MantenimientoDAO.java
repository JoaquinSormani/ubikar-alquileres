package com.ubikar.dao;

import com.ubikar.modelo.Mantenimiento;
import java.util.List;
import org.sql2o.Connection;

public class MantenimientoDAO {

    public List<Mantenimiento> readAll() {
        String sql = "SELECT * FROM mantenimiento";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Mantenimiento.class);
        }
    }

    public Mantenimiento read(Integer id) {
        String sql = "SELECT * FROM mantenimiento WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Mantenimiento.class);
        }
    }

    public List<Mantenimiento> selectByContrato(Integer idContrato) {
        String sql = "SELECT * FROM mantenimiento WHERE idContrato = :idContrato ORDER BY fecha, id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(Mantenimiento.class);
        }
    }

    public Integer create(Mantenimiento m) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, m);
        }
    }

    public Integer create(Connection con, Mantenimiento m) {
        String sql = "INSERT INTO mantenimiento (idContrato, fecha, descripcion, fotos, responsablePago) "
                + "VALUES (:idContrato, :fecha, :descripcion, :fotos, :responsablePago)";
        return con.createQuery(sql, true).bind(m).executeUpdate().getKey(Integer.class);
    }

    public void update(Mantenimiento m) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, m);
        }
    }

    public void update(Connection con, Mantenimiento m) {
        String sql = "UPDATE mantenimiento SET idContrato = :idContrato, fecha = :fecha, "
                + "descripcion = :descripcion, fotos = :fotos, responsablePago = :responsablePago WHERE id = :id";
        con.createQuery(sql).bind(m).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM mantenimiento WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

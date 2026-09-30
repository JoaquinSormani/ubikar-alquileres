package com.ubikar.dao;

import com.ubikar.modelo.Inventario;
import org.sql2o.Connection;

public class InventarioDAO {

    public Inventario selectByContrato(Integer idContrato) {
        String sql = "SELECT * FROM inventario WHERE idContrato = :idContrato";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetchFirst(Inventario.class);
        }
    }

    public Integer create(Inventario i) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, i);
        }
    }

    public Integer create(Connection con, Inventario i) {
        String sql = "INSERT INTO inventario (idContrato, fechaRegistro) "
                + "VALUES (:idContrato, :fechaRegistro)";
        return con.createQuery(sql, true).bind(i).executeUpdate().getKey(Integer.class);
    }

    public Inventario read(Integer id) {
        String sql = "SELECT * FROM inventario WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Inventario.class);
        }
    }

    public void update(Inventario i) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, i);
        }
    }

    public void update(Connection con, Inventario i) {
        String sql = "UPDATE inventario SET fechaRegistro = :fechaRegistro WHERE id = :id";
        con.createQuery(sql).bind(i).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM inventario WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

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

    public Integer insert(Inventario i) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return insert(con, i);
        }
    }

    public Integer insert(Connection con, Inventario i) {
        String sql = "INSERT INTO inventario (idContrato, fechaRegistro) "
                + "VALUES (:idContrato, :fechaRegistro)";
        return con.createQuery(sql, true).bind(i).executeUpdate().getKey(Integer.class);
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM inventario WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

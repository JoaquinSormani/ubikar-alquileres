package com.ubikar.dao;

import com.ubikar.modelo.RenglonInventario;
import java.util.List;
import org.sql2o.Connection;

public class RenglonInventarioDAO {

    public List<RenglonInventario> selectByInventario(Integer idInventario) {
        String sql = "SELECT * FROM renglon_inventario WHERE idInventario = :idInventario";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idInventario", idInventario)
                    .executeAndFetch(RenglonInventario.class);
        }
    }

    public Integer insert(RenglonInventario r) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return insert(con, r);
        }
    }

    public Integer insert(Connection con, RenglonInventario r) {
        String sql = "INSERT INTO renglon_inventario (idInventario, ambiente, objeto, estado, observaciones) "
                + "VALUES (:idInventario, :ambiente, :objeto, :estado, :observaciones)";
        return con.createQuery(sql, true).bind(r).executeUpdate().getKey(Integer.class);
    }

    public void update(RenglonInventario r) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, r);
        }
    }

    public void update(Connection con, RenglonInventario r) {
        String sql = "UPDATE renglon_inventario SET ambiente = :ambiente, objeto = :objeto, estado = :estado, "
                + "observaciones = :observaciones WHERE id = :id";
        con.createQuery(sql).bind(r).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM renglon_inventario WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

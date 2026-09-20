package com.ubikar.dao;

import com.ubikar.modelo.Documentacion;
import java.util.List;
import org.sql2o.Connection;

public class DocumentacionDAO {

    public List<Documentacion> selectByContrato(Integer idContrato) {
        String sql = "SELECT * FROM documentacion WHERE idContrato = :idContrato";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(Documentacion.class);
        }
    }

    public Integer insert(Documentacion d) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return insert(con, d);
        }
    }

    public Integer insert(Connection con, Documentacion d) {
        String sql = "INSERT INTO documentacion (idContrato, tipo, archivo, fechaCarga) "
                + "VALUES (:idContrato, :tipo, :archivo, :fechaCarga)";
        return con.createQuery(sql, true).bind(d).executeUpdate().getKey(Integer.class);
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM documentacion WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

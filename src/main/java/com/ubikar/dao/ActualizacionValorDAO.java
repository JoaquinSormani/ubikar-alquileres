package com.ubikar.dao;

import com.ubikar.modelo.ActualizacionValor;
import java.util.List;
import org.sql2o.Connection;

public class ActualizacionValorDAO {

    public List<ActualizacionValor> readAll() {
        String sql = "SELECT * FROM actualizacion_valor ORDER BY fecha DESC, id DESC";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(ActualizacionValor.class);
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

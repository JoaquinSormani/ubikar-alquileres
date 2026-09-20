package com.ubikar.dao;

import com.ubikar.modelo.Propiedad;
import java.util.List;
import org.sql2o.Connection;

public class PropiedadDAO {

    public List<Propiedad> selectAll() {
        String sql = "SELECT * FROM propiedad";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Propiedad.class);
        }
    }

    public Propiedad selectById(Integer id) {
        String sql = "SELECT * FROM propiedad WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Propiedad.class);
        }
    }

    public List<Propiedad> selectByPropietario(Integer idPropietario) {
        String sql = "SELECT * FROM propiedad WHERE idPropietario = :idPropietario";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idPropietario", idPropietario)
                    .executeAndFetch(Propiedad.class);
        }
    }

    public Integer insert(Propiedad p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return insert(con, p);
        }
    }

    public Integer insert(Connection con, Propiedad p) {
        String sql = "INSERT INTO propiedad (direccion, idPropietario) "
                + "VALUES (:direccion, :idPropietario)";
        return con.createQuery(sql, true).bind(p).executeUpdate().getKey(Integer.class);
    }

    public void update(Propiedad p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, p);
        }
    }

    public void update(Connection con, Propiedad p) {
        String sql = "UPDATE propiedad SET direccion = :direccion, "
                + "idPropietario = :idPropietario WHERE id = :id";
        con.createQuery(sql).bind(p).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM propiedad WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

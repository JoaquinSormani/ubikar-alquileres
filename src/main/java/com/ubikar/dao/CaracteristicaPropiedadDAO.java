package com.ubikar.dao;

import com.ubikar.modelo.CaracteristicaPropiedad;
import java.util.List;
import org.sql2o.Connection;

public class CaracteristicaPropiedadDAO {

    public List<CaracteristicaPropiedad> selectByPropiedad(Integer idPropiedad) {
        String sql = "SELECT * FROM caracteristica_propiedad WHERE idPropiedad = :idPropiedad ORDER BY id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idPropiedad", idPropiedad)
                    .executeAndFetch(CaracteristicaPropiedad.class);
        }
    }

    public CaracteristicaPropiedad read(Integer id) {
        String sql = "SELECT * FROM caracteristica_propiedad WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(CaracteristicaPropiedad.class);
        }
    }

    public Integer create(CaracteristicaPropiedad c) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, c);
        }
    }

    public Integer create(Connection con, CaracteristicaPropiedad c) {
        String sql = "INSERT INTO caracteristica_propiedad (idPropiedad, nombre, cantidad) "
                + "VALUES (:idPropiedad, :nombre, :cantidad)";
        return con.createQuery(sql, true).bind(c).executeUpdate().getKey(Integer.class);
    }

    public void update(CaracteristicaPropiedad c) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, c);
        }
    }

    public void update(Connection con, CaracteristicaPropiedad c) {
        String sql = "UPDATE caracteristica_propiedad SET nombre = :nombre, cantidad = :cantidad WHERE id = :id";
        con.createQuery(sql).bind(c).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM caracteristica_propiedad WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

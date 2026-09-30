package com.ubikar.dao;

import com.ubikar.modelo.Garante;
import java.util.List;
import org.sql2o.Connection;

public class GaranteDAO {

    public List<Garante> readAll() {
        String sql = "SELECT * FROM garante";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Garante.class);
        }
    }

    public Garante read(Integer id) {
        String sql = "SELECT * FROM garante WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Garante.class);
        }
    }

    public Integer create(Garante g) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, g);
        }
    }

    public Integer create(Connection con, Garante g) {
        String sql = "INSERT INTO garante (nombre, apellido, dni, telefono, email, domicilio, detalle) "
                + "VALUES (:nombre, :apellido, :dni, :telefono, :email, :domicilio, :detalle)";
        return con.createQuery(sql, true).bind(g).executeUpdate().getKey(Integer.class);
    }

    public void update(Garante g) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, g);
        }
    }

    public void update(Connection con, Garante g) {
        String sql = "UPDATE garante SET nombre = :nombre, apellido = :apellido, "
                + "dni = :dni, telefono = :telefono, email = :email, domicilio = :domicilio, "
                + "detalle = :detalle WHERE id = :id";
        con.createQuery(sql).bind(g).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM garante WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

package com.ubikar.dao;

import com.ubikar.modelo.Inquilino;
import java.util.List;
import org.sql2o.Connection;

public class InquilinoDAO {

    public List<Inquilino> readAll() {
        String sql = "SELECT * FROM inquilino";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Inquilino.class);
        }
    }

    public Inquilino read(Integer id) {
        String sql = "SELECT * FROM inquilino WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Inquilino.class);
        }
    }

    public Integer create(Inquilino i) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, i);
        }
    }

    public Integer create(Connection con, Inquilino i) {
        String sql = "INSERT INTO inquilino (nombre, apellido, dni, telefono, email, domicilio) "
                + "VALUES (:nombre, :apellido, :dni, :telefono, :email, :domicilio)";
        return con.createQuery(sql, true).bind(i).executeUpdate().getKey(Integer.class);
    }

    public void update(Inquilino i) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, i);
        }
    }

    public void update(Connection con, Inquilino i) {
        String sql = "UPDATE inquilino SET nombre = :nombre, apellido = :apellido, "
                + "dni = :dni, telefono = :telefono, email = :email, domicilio = :domicilio WHERE id = :id";
        con.createQuery(sql).bind(i).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM inquilino WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

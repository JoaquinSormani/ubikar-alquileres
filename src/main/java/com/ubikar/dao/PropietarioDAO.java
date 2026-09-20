package com.ubikar.dao;

import com.ubikar.modelo.Propietario;
import java.util.List;
import org.sql2o.Connection;

public class PropietarioDAO {

    public List<Propietario> selectAll() {
        String sql = "SELECT * FROM propietario";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Propietario.class);
        }
    }

    public Propietario selectById(Integer id) {
        String sql = "SELECT * FROM propietario WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Propietario.class);
        }
    }

    public Integer insert(Propietario p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return insert(con, p);
        }
    }

    public Integer insert(Connection con, Propietario p) {
        String sql = "INSERT INTO propietario (nombre, apellido, dni, telefono, email, domicilio) "
                + "VALUES (:nombre, :apellido, :dni, :telefono, :email, :domicilio)";
        return con.createQuery(sql, true).bind(p).executeUpdate().getKey(Integer.class);
    }

    public void update(Propietario p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, p);
        }
    }

    public void update(Connection con, Propietario p) {
        String sql = "UPDATE propietario SET nombre = :nombre, apellido = :apellido, "
                + "dni = :dni, telefono = :telefono, email = :email, domicilio = :domicilio WHERE id = :id";
        con.createQuery(sql).bind(p).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM propietario WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

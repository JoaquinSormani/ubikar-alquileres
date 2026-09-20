package com.ubikar.dao;

import com.ubikar.modelo.Contrato;
import com.ubikar.modelo.Garante;
import com.ubikar.modelo.Inquilino;
import java.time.LocalDate;
import java.util.List;
import org.sql2o.Connection;

public class ContratoDAO {

    public List<Contrato> selectAll() {
        String sql = "SELECT * FROM contrato";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Contrato.class);
        }
    }

    public Contrato selectById(Integer id) {
        String sql = "SELECT * FROM contrato WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Contrato.class);
        }
    }

    public List<Contrato> selectByPropiedad(Integer idPropiedad) {
        String sql = "SELECT * FROM contrato WHERE idPropiedad = :idPropiedad ORDER BY fechaInicio";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idPropiedad", idPropiedad)
                    .executeAndFetch(Contrato.class);
        }
    }

    public List<Inquilino> selectInquilinos(Integer idContrato) {
        String sql = "SELECT i.* FROM inquilino i JOIN contrato_inquilino ci ON ci.idInquilino = i.id "
                + "WHERE ci.idContrato = :idContrato";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(Inquilino.class);
        }
    }

    public List<Garante> selectGarantes(Integer idContrato) {
        String sql = "SELECT g.* FROM garante g JOIN contrato_garante cg ON cg.idGarante = g.id "
                + "WHERE cg.idContrato = :idContrato";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(Garante.class);
        }
    }

    public Integer insert(Contrato c) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return insert(con, c);
        }
    }

    public Integer insert(Connection con, Contrato c) {
        String sql = "INSERT INTO contrato (idPropiedad, fechaInicio, fechaFin, valorInicial, periodicidadActualizacion, "
                + "indiceActualizacion, destino, depositoGarantia, tipoGarantia) "
                + "VALUES (:idPropiedad, :fechaInicio, :fechaFin, :valorInicial, :periodicidadActualizacion, "
                + ":indiceActualizacion, :destino, :depositoGarantia, :tipoGarantia)";
        return con.createQuery(sql, true).bind(c).executeUpdate().getKey(Integer.class);
    }

    public void update(Contrato c) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, c);
        }
    }

    public void update(Connection con, Contrato c) {
        String sql = "UPDATE contrato SET idPropiedad = :idPropiedad, fechaInicio = :fechaInicio, "
                + "fechaFin = :fechaFin, valorInicial = :valorInicial, "
                + "periodicidadActualizacion = :periodicidadActualizacion, indiceActualizacion = :indiceActualizacion, "
                + "destino = :destino, depositoGarantia = :depositoGarantia, tipoGarantia = :tipoGarantia WHERE id = :id";
        con.createQuery(sql).bind(c).executeUpdate();
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM contrato WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }

    public void vincularInquilino(Integer idContrato, Integer idInquilino) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            vincularInquilino(con, idContrato, idInquilino);
        }
    }

    public void vincularInquilino(Connection con, Integer idContrato, Integer idInquilino) {
        String sql = "INSERT INTO contrato_inquilino (idContrato, idInquilino) VALUES (:idContrato, :idInquilino)";
        con.createQuery(sql)
                .addParameter("idContrato", idContrato)
                .addParameter("idInquilino", idInquilino)
                .executeUpdate();
    }

    public void vincularGarante(Integer idContrato, Integer idGarante) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            vincularGarante(con, idContrato, idGarante);
        }
    }

    public void vincularGarante(Connection con, Integer idContrato, Integer idGarante) {
        String sql = "INSERT INTO contrato_garante (idContrato, idGarante) VALUES (:idContrato, :idGarante)";
        con.createQuery(sql)
                .addParameter("idContrato", idContrato)
                .addParameter("idGarante", idGarante)
                .executeUpdate();
    }

    public boolean existeContratoSuperpuesto(Integer idPropiedad, LocalDate inicio, LocalDate fin) {
        String sql = "SELECT COUNT(*) FROM contrato WHERE idPropiedad = :idPropiedad "
                + "AND fechaInicio <= :fin AND fechaFin >= :inicio";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            Integer cantidad = con.createQuery(sql)
                    .addParameter("idPropiedad", idPropiedad)
                    .addParameter("inicio", inicio)
                    .addParameter("fin", fin)
                    .executeScalar(Integer.class);
            return cantidad != null && cantidad > 0;
        }
    }
}

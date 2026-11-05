package com.ubikar.dao;

import com.ubikar.modelo.EstadoPago;
import com.ubikar.modelo.Pago;
import java.time.LocalDate;
import java.util.List;
import org.sql2o.Connection;

public class PagoDAO {

    public List<Pago> readAll() {
        String sql = "SELECT * FROM pago";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql).executeAndFetch(Pago.class);
        }
    }

    public Pago read(Integer id) {
        String sql = "SELECT * FROM pago WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("id", id)
                    .executeAndFetchFirst(Pago.class);
        }
    }

    public List<Pago> selectByContrato(Integer idContrato) {
        String sql = "SELECT * FROM pago WHERE idContrato = :idContrato ORDER BY periodo";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .executeAndFetch(Pago.class);
        }
    }

    public Pago selectByContratoYPeriodo(Integer idContrato, LocalDate periodo) {
        String sql = "SELECT * FROM pago WHERE idContrato = :idContrato AND periodo = :periodo";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("idContrato", idContrato)
                    .addParameter("periodo", periodo)
                    .executeAndFetchFirst(Pago.class);
        }
    }

    public List<Pago> selectByEstado(EstadoPago estado) {
        String sql = "SELECT * FROM pago WHERE estado = :estado ORDER BY id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return con.createQuery(sql)
                    .addParameter("estado", estado.name())
                    .executeAndFetch(Pago.class);
        }
    }

    public Integer create(Pago p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            return create(con, p);
        }
    }

    public Integer create(Connection con, Pago p) {
        String sql = "INSERT INTO pago (idContrato, periodo, fechaVencimiento, fecha, monto, indiceActualizacion, "
                + "diasAtraso, punitorio, estado, medioPago, numeroRecibo) "
                + "VALUES (:idContrato, :periodo, :fechaVencimiento, :fecha, :monto, :indiceActualizacion, "
                + ":diasAtraso, :punitorio, :estado, :medioPago, :numeroRecibo)";
        return con.createQuery(sql, true).bind(p).executeUpdate().getKey(Integer.class);
    }

    public void update(Pago p) {
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            update(con, p);
        }
    }

    public void update(Connection con, Pago p) {
        String sql = "UPDATE pago SET idContrato = :idContrato, periodo = :periodo, fechaVencimiento = :fechaVencimiento, "
                + "fecha = :fecha, monto = :monto, indiceActualizacion = :indiceActualizacion, diasAtraso = :diasAtraso, "
                + "punitorio = :punitorio, estado = :estado, medioPago = :medioPago, numeroRecibo = :numeroRecibo WHERE id = :id";
        con.createQuery(sql).bind(p).executeUpdate();
    }

    /**
     * Próximo número de recibo: correlativo en el orden en que se cobra. Se pide dentro de la misma
     * transacción que guarda el pago y bloquea la lectura, así dos cobros simultáneos no repiten número.
     */
    public int siguienteNumeroRecibo(Connection con) {
        String sql = "SELECT COALESCE(MAX(numeroRecibo), 0) + 1 FROM pago FOR UPDATE";
        return con.createQuery(sql).executeScalar(Integer.class);
    }

    public void delete(Integer id) {
        String sql = "DELETE FROM pago WHERE id = :id";
        try (Connection con = Sql2oDAO.getSql2o().open()) {
            con.createQuery(sql).addParameter("id", id).executeUpdate();
        }
    }
}

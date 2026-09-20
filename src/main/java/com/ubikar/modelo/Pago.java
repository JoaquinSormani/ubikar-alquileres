package com.ubikar.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Pago {

    private Integer id;
    private Integer idContrato;
    private LocalDate fecha;
    private BigDecimal monto;
    private BigDecimal indiceActualizacion;
    private Integer diasAtraso;
    private BigDecimal punitorio;
    private EstadoPago estado;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdContrato() {
        return idContrato;
    }

    public void setIdContrato(Integer idContrato) {
        this.idContrato = idContrato;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getIndiceActualizacion() {
        return indiceActualizacion;
    }

    public void setIndiceActualizacion(BigDecimal indiceActualizacion) {
        this.indiceActualizacion = indiceActualizacion;
    }

    public Integer getDiasAtraso() {
        return diasAtraso;
    }

    public void setDiasAtraso(Integer diasAtraso) {
        this.diasAtraso = diasAtraso;
    }

    public BigDecimal getPunitorio() {
        return punitorio;
    }

    public void setPunitorio(BigDecimal punitorio) {
        this.punitorio = punitorio;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }
}

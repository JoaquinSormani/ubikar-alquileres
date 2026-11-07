package com.ubikar.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ActualizacionValor {

    private Integer id;
    private Integer idContrato;
    private LocalDate periodoDesde;
    private LocalDate fecha;
    private String indice;
    private BigDecimal valorAnterior;
    private BigDecimal valorNuevo;
    private BigDecimal factor;

    public ActualizacionValor() {
    }

    public ActualizacionValor(Integer idContrato, LocalDate periodoDesde, LocalDate fecha, String indice,
            BigDecimal valorAnterior, BigDecimal valorNuevo, BigDecimal factor) {
        this.idContrato = idContrato;
        this.periodoDesde = periodoDesde;
        this.fecha = fecha;
        this.indice = indice;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.factor = factor;
    }

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

    public LocalDate getPeriodoDesde() {
        return periodoDesde;
    }

    public void setPeriodoDesde(LocalDate periodoDesde) {
        this.periodoDesde = periodoDesde;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getIndice() {
        return indice;
    }

    public void setIndice(String indice) {
        this.indice = indice;
    }

    public BigDecimal getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(BigDecimal valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public BigDecimal getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(BigDecimal valorNuevo) {
        this.valorNuevo = valorNuevo;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }
}

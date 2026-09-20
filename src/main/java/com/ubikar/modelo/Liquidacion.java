package com.ubikar.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Liquidacion {

    private Integer id;
    private Integer idPago;
    private LocalDate fecha;
    private BigDecimal montoBruto;
    private BigDecimal comision;
    private BigDecimal montoNeto;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdPago() {
        return idPago;
    }

    public void setIdPago(Integer idPago) {
        this.idPago = idPago;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMontoBruto() {
        return montoBruto;
    }

    public void setMontoBruto(BigDecimal montoBruto) {
        this.montoBruto = montoBruto;
    }

    public BigDecimal getComision() {
        return comision;
    }

    public void setComision(BigDecimal comision) {
        this.comision = comision;
    }

    public BigDecimal getMontoNeto() {
        return montoNeto;
    }

    public void setMontoNeto(BigDecimal montoNeto) {
        this.montoNeto = montoNeto;
    }
}

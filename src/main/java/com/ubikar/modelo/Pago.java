package com.ubikar.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Pago {

    private Integer id;
    private Integer idContrato;
    private LocalDate periodo;           // primer día del mes que se paga
    private LocalDate fechaVencimiento;
    private LocalDate fecha;             // fecha en que se pagó (null mientras está pendiente)
    private BigDecimal monto;
    private BigDecimal indiceActualizacion;
    private Integer diasAtraso = 0;
    private BigDecimal punitorio = BigDecimal.ZERO;
    private EstadoPago estado = EstadoPago.PENDIENTE;
    private MedioPago medioPago;
    private Integer numeroRecibo;        // correlativo: se asigna al registrar el pago (null mientras está pendiente)

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

    public LocalDate getPeriodo() {
        return periodo;
    }

    public void setPeriodo(LocalDate periodo) {
        this.periodo = periodo;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    public Integer getNumeroRecibo() {
        return numeroRecibo;
    }

    public void setNumeroRecibo(Integer numeroRecibo) {
        this.numeroRecibo = numeroRecibo;
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

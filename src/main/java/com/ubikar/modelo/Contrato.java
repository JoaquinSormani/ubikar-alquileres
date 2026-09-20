package com.ubikar.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Contrato {

    private Integer id;
    private Integer idPropiedad;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal valorInicial;
    private Integer periodicidadActualizacion; // en meses (1 = mensual, 3 = trimestral, ...)
    private IndiceActualizacion indiceActualizacion;
    private DestinoInmueble destino;
    private BigDecimal depositoGarantia;
    private TipoGarantia tipoGarantia;

    private List<Inquilino> inquilinos = new ArrayList<>();
    private List<Garante> garantes = new ArrayList<>();
    private Inventario inventario;
    private List<Documentacion> documentacion = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdPropiedad() {
        return idPropiedad;
    }

    public void setIdPropiedad(Integer idPropiedad) {
        this.idPropiedad = idPropiedad;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public BigDecimal getValorInicial() {
        return valorInicial;
    }

    public void setValorInicial(BigDecimal valorInicial) {
        this.valorInicial = valorInicial;
    }

    public Integer getPeriodicidadActualizacion() {
        return periodicidadActualizacion;
    }

    public void setPeriodicidadActualizacion(Integer periodicidadActualizacion) {
        this.periodicidadActualizacion = periodicidadActualizacion;
    }

    public IndiceActualizacion getIndiceActualizacion() {
        return indiceActualizacion;
    }

    public void setIndiceActualizacion(IndiceActualizacion indiceActualizacion) {
        this.indiceActualizacion = indiceActualizacion;
    }

    public DestinoInmueble getDestino() {
        return destino;
    }

    public void setDestino(DestinoInmueble destino) {
        this.destino = destino;
    }

    public BigDecimal getDepositoGarantia() {
        return depositoGarantia;
    }

    public void setDepositoGarantia(BigDecimal depositoGarantia) {
        this.depositoGarantia = depositoGarantia;
    }

    public TipoGarantia getTipoGarantia() {
        return tipoGarantia;
    }

    public void setTipoGarantia(TipoGarantia tipoGarantia) {
        this.tipoGarantia = tipoGarantia;
    }

    public List<Inquilino> getInquilinos() {
        return inquilinos;
    }

    public void setInquilinos(List<Inquilino> inquilinos) {
        this.inquilinos = inquilinos;
    }

    public List<Garante> getGarantes() {
        return garantes;
    }

    public void setGarantes(List<Garante> garantes) {
        this.garantes = garantes;
    }

    public Inventario getInventario() {
        return inventario;
    }

    public void setInventario(Inventario inventario) {
        this.inventario = inventario;
    }

    public List<Documentacion> getDocumentacion() {
        return documentacion;
    }

    public void setDocumentacion(List<Documentacion> documentacion) {
        this.documentacion = documentacion;
    }
}

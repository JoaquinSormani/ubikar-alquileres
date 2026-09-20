package com.ubikar.servicio;

import com.ubikar.modelo.DestinoInmueble;
import com.ubikar.modelo.Garante;
import com.ubikar.modelo.IndiceActualizacion;
import com.ubikar.modelo.Inquilino;
import com.ubikar.modelo.Propiedad;
import com.ubikar.modelo.Propietario;
import com.ubikar.modelo.RenglonInventario;
import com.ubikar.modelo.TipoGarantia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SolicitudContrato {

    private Propietario propietario;
    private Propiedad propiedad;
    private List<Inquilino> inquilinos = new ArrayList<>();
    private List<Garante> garantes = new ArrayList<>();
    private LocalDate fechaInicio;
    private Integer duracionMeses;
    private BigDecimal valorInicial;
    private Integer periodicidadActualizacion;
    private IndiceActualizacion indiceActualizacion;
    private DestinoInmueble destino;
    private BigDecimal depositoGarantia;
    private TipoGarantia tipoGarantia;
    private List<RenglonInventario> renglones = new ArrayList<>();
    private String plantilla;

    public Propietario getPropietario() {
        return propietario;
    }

    public void setPropietario(Propietario propietario) {
        this.propietario = propietario;
    }

    public Propiedad getPropiedad() {
        return propiedad;
    }

    public void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Integer getDuracionMeses() {
        return duracionMeses;
    }

    public void setDuracionMeses(Integer duracionMeses) {
        this.duracionMeses = duracionMeses;
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

    public List<RenglonInventario> getRenglones() {
        return renglones;
    }

    public void setRenglones(List<RenglonInventario> renglones) {
        this.renglones = renglones;
    }

    public String getPlantilla() {
        return plantilla;
    }

    public void setPlantilla(String plantilla) {
        this.plantilla = plantilla;
    }
}

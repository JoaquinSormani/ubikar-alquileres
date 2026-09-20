package com.ubikar.modelo;

import java.time.LocalDate;

public class Mantenimiento {

    private Integer id;
    private Integer idContrato;
    private LocalDate fecha;
    private String descripcion;
    private String fotos; // rutas separadas por coma
    private ResponsablePago responsablePago;

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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFotos() {
        return fotos;
    }

    public void setFotos(String fotos) {
        this.fotos = fotos;
    }

    public ResponsablePago getResponsablePago() {
        return responsablePago;
    }

    public void setResponsablePago(ResponsablePago responsablePago) {
        this.responsablePago = responsablePago;
    }
}

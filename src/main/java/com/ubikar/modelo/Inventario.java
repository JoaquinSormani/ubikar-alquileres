package com.ubikar.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Inventario {

    private Integer id;
    private Integer idContrato;
    private LocalDate fechaRegistro;

    private List<RenglonInventario> renglones = new ArrayList<>();

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

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public List<RenglonInventario> getRenglones() {
        return renglones;
    }

    public void setRenglones(List<RenglonInventario> renglones) {
        this.renglones = renglones;
    }
}

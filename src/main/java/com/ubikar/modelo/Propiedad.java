package com.ubikar.modelo;

import java.util.ArrayList;
import java.util.List;

public class Propiedad {

    private Integer id;
    private String direccion;
    private Integer idPropietario;

    private List<CaracteristicaPropiedad> caracteristicas = new ArrayList<>();

    public List<CaracteristicaPropiedad> getCaracteristicas() {
        return caracteristicas;
    }

    public void setCaracteristicas(List<CaracteristicaPropiedad> caracteristicas) {
        this.caracteristicas = caracteristicas;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Integer getIdPropietario() {
        return idPropietario;
    }

    public void setIdPropietario(Integer idPropietario) {
        this.idPropietario = idPropietario;
    }
}

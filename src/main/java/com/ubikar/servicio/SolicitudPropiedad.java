package com.ubikar.servicio;

import com.ubikar.modelo.Propietario;

public class SolicitudPropiedad {

    private Propietario propietario;
    private String direccion;

    public Propietario getPropietario() {
        return propietario;
    }

    public void setPropietario(Propietario propietario) {
        this.propietario = propietario;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}

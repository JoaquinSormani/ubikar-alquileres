package com.ubikar.servicio;

import com.ubikar.modelo.CaracteristicaPropiedad;
import com.ubikar.modelo.Propietario;
import java.util.ArrayList;
import java.util.List;

public class SolicitudPropiedad {

    private Propietario propietario;
    private String direccion;
    private List<CaracteristicaPropiedad> caracteristicas = new ArrayList<>();

    public List<CaracteristicaPropiedad> getCaracteristicas() {
        return caracteristicas;
    }

    public void setCaracteristicas(List<CaracteristicaPropiedad> caracteristicas) {
        this.caracteristicas = caracteristicas;
    }

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

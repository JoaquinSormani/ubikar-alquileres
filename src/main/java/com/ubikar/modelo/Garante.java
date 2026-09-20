package com.ubikar.modelo;

public class Garante extends Persona {

    private String detalle; // ej: empleador, CUIT, cargo y legajo del recibo de sueldo

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}

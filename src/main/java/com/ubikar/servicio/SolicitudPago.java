package com.ubikar.servicio;

import com.ubikar.modelo.MedioPago;
import java.time.LocalDate;

public class SolicitudPago {

    private Integer idPago;        // el cobro pendiente que se está pagando
    private LocalDate fechaPago;   // si no viene, se toma hoy
    private MedioPago medioPago;   // si no viene, efectivo

    public Integer getIdPago() {
        return idPago;
    }

    public void setIdPago(Integer idPago) {
        this.idPago = idPago;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }
}

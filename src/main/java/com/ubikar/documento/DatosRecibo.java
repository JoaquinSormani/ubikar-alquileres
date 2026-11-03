package com.ubikar.documento;

import com.ubikar.modelo.MedioPago;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Todo lo que lleva un recibo de pago de alquiler. */
public record DatosRecibo(
        Integer numero,
        LocalDate fechaEmision,
        String inquilinos,
        String direccion,
        String propietario,
        String periodo,
        LocalDate fechaVencimiento,
        LocalDate fechaPago,
        MedioPago medioPago,
        BigDecimal alquiler,
        int diasAtraso,
        BigDecimal punitorio,
        BigDecimal total) {
}

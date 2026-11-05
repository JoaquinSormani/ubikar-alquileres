package com.ubikar.config;

import java.math.BigDecimal;

/**
 * Parámetros de negocio del cobro de alquileres, en un solo lugar: si cambia una regla (por ejemplo el día de
 * vencimiento o la tasa de mora de la cláusula del contrato) se cambia acá y no en cada clase que la usa.
 */
public final class ReglasDeCobro {

    /** El alquiler se cobra por mes adelantado y vence este día de cada mes. */
    public static final int DIA_VENCIMIENTO = 10;

    /** Interés punitorio diario por mora: 1 % del alquiler por cada día de atraso. */
    public static final BigDecimal TASA_PUNITORIO_DIARIA = new BigDecimal("0.01");

    private ReglasDeCobro() {
    }
}

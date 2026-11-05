package com.ubikar.servicio;

import com.ubikar.config.ReglasDeCobro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calcular Punitorio por Mora (extiende a Registrar Pago).
 *
 * El contrato (cláusula de mora) fija un interés punitorio diario del 1 % sobre lo adeudado,
 * contado desde el primer día posterior al vencimiento hasta el día del pago.
 */
public final class Punitorio {

    public static final BigDecimal TASA_DIARIA = ReglasDeCobro.TASA_PUNITORIO_DIARIA;

    public record Mora(int diasAtraso, BigDecimal punitorio) {
    }

    private Punitorio() {
    }

    public static Mora calcular(BigDecimal monto, LocalDate vencimiento, LocalDate fechaDePago) {
        long dias = ChronoUnit.DAYS.between(vencimiento, fechaDePago);
        if (dias <= 0) {
            return new Mora(0, BigDecimal.ZERO.setScale(2));
        }
        BigDecimal punitorio = monto.multiply(TASA_DIARIA).multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
        return new Mora((int) dias, punitorio);
    }
}

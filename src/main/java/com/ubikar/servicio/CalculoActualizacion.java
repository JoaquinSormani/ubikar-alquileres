package com.ubikar.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public final class CalculoActualizacion {

    private CalculoActualizacion() {
    }

    /** ICL: valor del índice en la fecha del ajuste dividido el valor en la fecha base. */
    public static BigDecimal factorIcl(BigDecimal iclFinal, BigDecimal iclBase) {
        return iclFinal.divide(iclBase, 6, RoundingMode.HALF_UP);
    }

    /** IPC: se encadena la inflación mensual: (1 + m1/100) x (1 + m2/100) x ... */
    public static BigDecimal factorIpc(List<BigDecimal> inflacionMensualPorcentaje) {
        BigDecimal factor = BigDecimal.ONE;
        for (BigDecimal m : inflacionMensualPorcentaje) {
            factor = factor.multiply(BigDecimal.ONE.add(m.movePointLeft(2)));
        }
        return factor.setScale(6, RoundingMode.HALF_UP);
    }

    public static BigDecimal nuevoValor(BigDecimal valorActual, BigDecimal factor) {
        return valorActual.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Porcentaje de aumento para mostrar en pantalla, por ejemplo 10,00. */
    public static BigDecimal porcentaje(BigDecimal factor) {
        return factor.subtract(BigDecimal.ONE).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
    }

    /** Fecha de la actualización número k (1 = la primera): inicio + k x periodicidad meses. */
    public static LocalDate fechaAjuste(LocalDate inicioContrato, int periodicidadMeses, int k) {
        return inicioContrato.plusMonths((long) periodicidadMeses * k);
    }
}

package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class PunitorioTest {

    private static final BigDecimal ALQUILER = new BigDecimal("500000.00");
    private static final LocalDate VENCE = LocalDate.of(2026, 10, 10);

    @Test
    void sinAtrasoNoHayPunitorio() {
        Punitorio.Mora enFecha = Punitorio.calcular(ALQUILER, VENCE, VENCE);
        assertEquals(0, enFecha.diasAtraso());
        assertEquals(new BigDecimal("0.00"), enFecha.punitorio());

        Punitorio.Mora adelantado = Punitorio.calcular(ALQUILER, VENCE, VENCE.minusDays(5));
        assertEquals(0, adelantado.diasAtraso());
        assertEquals(new BigDecimal("0.00"), adelantado.punitorio());
    }

    @Test
    void cuentaDesdeElDiaSiguienteAlVencimiento() {
        Punitorio.Mora unDia = Punitorio.calcular(ALQUILER, VENCE, VENCE.plusDays(1));
        assertEquals(1, unDia.diasAtraso());
        assertEquals(new BigDecimal("5000.00"), unDia.punitorio());
    }

    @Test
    void esUnPorCientoDiarioSimple() {
        Punitorio.Mora catorce = Punitorio.calcular(ALQUILER, VENCE, VENCE.plusDays(14));
        assertEquals(14, catorce.diasAtraso());
        assertEquals(new BigDecimal("70000.00"), catorce.punitorio());
    }

    @Test
    void redondeaACentavos() {
        Punitorio.Mora m = Punitorio.calcular(new BigDecimal("1234.56"), VENCE, VENCE.plusDays(3));
        assertEquals(new BigDecimal("37.04"), m.punitorio());
    }

    @Test
    void elVencimientoEsElDiezPeroNuncaAntesDelInicio() {
        assertEquals(LocalDate.of(2026, 11, 10), PagoServicio.vencimiento(YearMonth.of(2026, 11), LocalDate.of(2026, 9, 19)));
        // primer mes de un contrato que empieza el 19: vence el día de inicio
        assertEquals(LocalDate.of(2026, 10, 19), PagoServicio.vencimiento(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 19)));
        // contrato que empieza antes del 10: vence el 10
        assertEquals(LocalDate.of(2026, 10, 10), PagoServicio.vencimiento(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 3)));
    }
}

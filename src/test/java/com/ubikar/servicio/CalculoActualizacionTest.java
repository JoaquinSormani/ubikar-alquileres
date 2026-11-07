package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculoActualizacionTest {

    @Test
    @DisplayName("ICL aumenta por el cociente exacto entre el índice actual y el índice base")
    void iclAumentaPorElCocienteDelIndice() {
        BigDecimal factor = CalculoActualizacion.factorIcl(new BigDecimal("33"), new BigDecimal("30"));
        assertEquals(new BigDecimal("110000.00"), CalculoActualizacion.nuevoValor(new BigDecimal("100000"), factor));
        assertEquals(new BigDecimal("10.00"), CalculoActualizacion.porcentaje(factor));
    }

    @Test
    @DisplayName("IPC encadena las tasas mensuales compuestas correctamente")
    void ipcEncadenaLosMeses() {
        BigDecimal factor = CalculoActualizacion.factorIpc(List.of(new BigDecimal("2"), new BigDecimal("3")));
        assertEquals(new BigDecimal("105060.00"), CalculoActualizacion.nuevoValor(new BigDecimal("100000"), factor));
    }

    @Test
    @DisplayName("La fecha de ajuste suma (periodicidad * k) meses a la fecha de inicio del contrato")
    void laPrimeraActualizacionEsInicioMasPeriodicidad() {
        assertEquals(LocalDate.of(2026, 7, 15), CalculoActualizacion.fechaAjuste(LocalDate.of(2026, 4, 15), 3, 1));
        assertEquals(LocalDate.of(2027, 1, 15), CalculoActualizacion.fechaAjuste(LocalDate.of(2026, 4, 15), 3, 3));
    }

    @Test
    @DisplayName("Actualización trimestral por IPC con inflaciones reales acumuladas")
    void actualizacionTrimestralIpcConInflacionAcumulada() {
        // Alquiler inicial: $350.000
        // Inflación mensual de 3 meses: 4.2%, 3.5%, 3.0%
        // Factor esperado: (1 + 0.042) * (1 + 0.035) * (1 + 0.030) = 1.042 * 1.035 * 1.030 = 1.1108241 -> 1.110824
        BigDecimal factor = CalculoActualizacion.factorIpc(List.of(
                new BigDecimal("4.2"),
                new BigDecimal("3.5"),
                new BigDecimal("3.0")
        ));
        assertEquals(new BigDecimal("1.110824"), factor);

        // Nuevo valor: 350.000 * 1.110824 = 388788.40
        BigDecimal valorActual = new BigDecimal("350000.00");
        BigDecimal nuevoValor = CalculoActualizacion.nuevoValor(valorActual, factor);
        assertEquals(new BigDecimal("388788.40"), nuevoValor);

        // Porcentaje: 11.08%
        assertEquals(new BigDecimal("11.08"), CalculoActualizacion.porcentaje(factor));
    }

    @Test
    @DisplayName("Actualización semestral por ICL calcula el incremento porcentual y nuevo valor")
    void actualizacionSemestralIcl() {
        // ICL inicial: 15.200000, ICL actual: 18.240000 (+20%)
        BigDecimal factor = CalculoActualizacion.factorIcl(new BigDecimal("18.240000"), new BigDecimal("15.200000"));
        assertEquals(new BigDecimal("1.200000"), factor);

        BigDecimal valorActual = new BigDecimal("400000.00");
        BigDecimal nuevoValor = CalculoActualizacion.nuevoValor(valorActual, factor);
        assertEquals(new BigDecimal("480000.00"), nuevoValor);
        assertEquals(new BigDecimal("20.00"), CalculoActualizacion.porcentaje(factor));
    }

    @Test
    @DisplayName("Sin variación de índice el alquiler se mantiene y el porcentaje es 0%")
    void sinVariacionDeIndiceElAlquilerSeMantiene() {
        BigDecimal factor = CalculoActualizacion.factorIcl(new BigDecimal("25.500000"), new BigDecimal("25.500000"));
        assertEquals(new BigDecimal("1.000000"), factor);

        BigDecimal valorActual = new BigDecimal("250000.00");
        assertEquals(new BigDecimal("250000.00"), CalculoActualizacion.nuevoValor(valorActual, factor));
        assertEquals(new BigDecimal("0.00"), CalculoActualizacion.porcentaje(factor));
    }

    @Test
    @DisplayName("El cálculo de nuevo valor redondea a centavos con HALF_UP")
    void redondeoDecimalesEnNuevoValor() {
        BigDecimal factor = new BigDecimal("1.123456");
        BigDecimal valorActual = new BigDecimal("12345.67");
        // 12345.67 * 1.123456 = 13869.81703552 -> redondeo a 2 decimales: 13869.82
        assertEquals(new BigDecimal("13869.82"), CalculoActualizacion.nuevoValor(valorActual, factor));
    }

    @Test
    @DisplayName("Cálculo de fechas sucesivas para periodicidad cuatrimestral y anual")
    void fechasAjusteParaDistintasPeriodicidades() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);

        // Cuatrimestral (4 meses): 1 mayo 2026, 1 septiembre 2026, 1 enero 2027
        assertEquals(LocalDate.of(2026, 5, 1), CalculoActualizacion.fechaAjuste(inicio, 4, 1));
        assertEquals(LocalDate.of(2026, 9, 1), CalculoActualizacion.fechaAjuste(inicio, 4, 2));
        assertEquals(LocalDate.of(2027, 1, 1), CalculoActualizacion.fechaAjuste(inicio, 4, 3));

        // Anual (12 meses)
        assertEquals(LocalDate.of(2027, 1, 1), CalculoActualizacion.fechaAjuste(inicio, 12, 1));
        assertEquals(LocalDate.of(2028, 1, 1), CalculoActualizacion.fechaAjuste(inicio, 12, 2));
    }
}

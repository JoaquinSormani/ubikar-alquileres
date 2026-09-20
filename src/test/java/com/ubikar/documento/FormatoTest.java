package com.ubikar.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FormatoTest {

    @Test
    void numerosEnLetras() {
        assertEquals("CERO", Formato.letras(0));
        assertEquals("DOCE", Formato.letras(12));
        assertEquals("VEINTIUNO", Formato.letras(21));
        assertEquals("CIEN", Formato.letras(100));
        assertEquals("CIENTO UNO", Formato.letras(101));
        assertEquals("MIL", Formato.letras(1000));
        assertEquals("VEINTIÚN MIL", Formato.letras(21_000));
        assertEquals("TREINTA Y UN MIL", Formato.letras(31_000));
        assertEquals("CIENTO OCHENTA Y CINCO MIL CUATROCIENTOS", Formato.letras(185_400));
        assertEquals("UN MILLÓN", Formato.letras(1_000_000));
        assertEquals("DOS MILLONES QUINIENTOS MIL", Formato.letras(2_500_000));
        assertEquals("VEINTIÚN MILLONES", Formato.letras(21_000_000));
        assertEquals("UN", Formato.letrasApocopado(1));
        assertEquals("VEINTICUATRO", Formato.letrasApocopado(24));
    }

    @Test
    void montos() {
        assertEquals("CIENTO OCHENTA Y CINCO MIL CUATROCIENTOS", Formato.montoALetras(new BigDecimal("185400")));
        assertEquals("CIENTO OCHENTA Y CINCO MIL CUATROCIENTOS CON 50/100", Formato.montoALetras(new BigDecimal("185400.50")));
        assertEquals("185.400,50", Formato.monto(new BigDecimal("185400.5")));
        assertEquals("1.250.000,00", Formato.monto(new BigDecimal("1250000")));
    }

    @Test
    void fechasYDni() {
        assertEquals("1 de octubre de 2026", Formato.fecha(LocalDate.of(2026, 10, 1)));
        assertEquals("30.111.222", Formato.dni("30111222"));
        assertEquals("30.111.222", Formato.dni("30.111.222"));
        assertEquals("5.123.456", Formato.dni("5123456"));
    }
}

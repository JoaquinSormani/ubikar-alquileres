package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ubikar.modelo.Contrato;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ContratoServicioTest {

    private static final BigDecimal ALQUILER = new BigDecimal("200000");

    @Test
    void sinDepositoSeTomaUnMesDeAlquiler() {
        assertEquals(ALQUILER, ContratoServicio.depositoEfectivo(null, ALQUILER));
    }

    @Test
    void depositoEnCeroTambienEsUnMesDeAlquiler() {
        assertEquals(ALQUILER, ContratoServicio.depositoEfectivo(BigDecimal.ZERO, ALQUILER));
        assertEquals(ALQUILER, ContratoServicio.depositoEfectivo(new BigDecimal("0.00"), ALQUILER));
    }

    @Test
    void elAvisoDeSuperposicionNombraElContrato() {
        Contrato c = new Contrato();
        c.setId(9);
        c.setFechaInicio(LocalDate.of(2027, 1, 8));
        c.setFechaFin(LocalDate.of(2028, 1, 7));
        assertEquals("La propiedad ya tiene el contrato N.º 9, vigente del 08/01/2027 al 07/01/2028.",
                ContratoServicio.mensajeContratoSuperpuesto(c));
    }

    @Test
    void conDepositoSeRespetaElMonto() {
        BigDecimal dos = new BigDecimal("400000");
        assertEquals(dos, ContratoServicio.depositoEfectivo(dos, ALQUILER));
    }
}

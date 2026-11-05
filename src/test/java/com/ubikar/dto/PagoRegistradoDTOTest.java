package com.ubikar.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ubikar.modelo.Pago;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PagoRegistradoDTOTest {

    private Pago pago() {
        Pago p = new Pago();
        p.setId(21);
        p.setNumeroRecibo(12);
        p.setIdContrato(17);
        p.setPeriodo(LocalDate.of(2026, 11, 1));
        p.setFecha(LocalDate.of(2026, 11, 4));
        p.setMonto(new BigDecimal("700000.00"));
        p.setDiasAtraso(0);
        p.setPunitorio(new BigDecimal("0.00"));
        return p;
    }

    @Test
    void armaLaRespuestaConElTotalYElEnlaceDelRecibo() {
        PagoRegistradoDTO dto = PagoRegistradoDTO.de(pago());
        assertEquals(12, dto.numeroRecibo());
        assertEquals(new BigDecimal("700000.00"), dto.total());
        assertEquals("/api/pagos/21/recibo", dto.recibo());
    }

    @Test
    void mantieneLosCamposQueLeeLaPantalla() throws Exception {
        ObjectMapper json = new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonNode nodo = json.readTree(json.writeValueAsString(PagoRegistradoDTO.de(pago())));
        for (String campo : new String[] {"id", "numeroRecibo", "idContrato", "periodo", "fechaPago", "monto", "diasAtraso", "punitorio", "total", "recibo"}) {
            assertTrue(nodo.has(campo), "falta el campo " + campo);
        }
        assertEquals("2026-11-04", nodo.get("fechaPago").asText());
    }
}

package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ActualizacionServicioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

    @Test
    void unaFechaFuturaEstaProgramada() {
        assertEquals("PROGRAMADA", ActualizacionServicio.estadoProxima(LocalDate.of(2026, 12, 1), HOY));
    }

    @Test
    void unaFechaQueYaLlegoEsperaElIndice() {
        assertEquals("ESPERANDO_INDICE", ActualizacionServicio.estadoProxima(LocalDate.of(2026, 11, 1), HOY));
        assertEquals("ESPERANDO_INDICE", ActualizacionServicio.estadoProxima(HOY, HOY));
    }
}

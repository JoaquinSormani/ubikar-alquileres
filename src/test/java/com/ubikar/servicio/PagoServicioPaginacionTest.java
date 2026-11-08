package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Validaciones de la paginación de cobros (pendientes y realizados): se rechazan antes de tocar la base de datos. */
class PagoServicioPaginacionTest {

    private final PagoServicio servicio = new PagoServicio();

    @Test
    void rechazaTamanioFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> servicio.realizados(null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> servicio.realizados(null, null, null, -3));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.realizados(null, null, null, PagoServicio.TAMANIO_MAXIMO + 1));
        assertTrue(e.getMessage().contains("entre 1 y"));
        assertThrows(IllegalArgumentException.class, () -> servicio.pendientes(null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> servicio.pendientes(null, null, null, PagoServicio.TAMANIO_MAXIMO + 1));
    }

    @Test
    void rechazaCursorDeRealizadosMalFormado() {
        for (String malo : new String[] {"abc", "12", "12-", "-5", "1-2-3", "12;DROP TABLE pago", "1e3-4", "0x10-2", "1234567890-1", "1-1234567890", "١٢-٣"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.realizados(null, null, malo, 10), "cursor: " + malo);
        }
    }

    @Test
    void rechazaCursorDePendientesMalFormado() {
        for (String malo : new String[] {"abc", "5", "2026-10-10", "2026-10-10_", "_5", "2026-10-10-5", "2026-13-45_5", "2026-02-30_5",
                "26-10-10_5", "2026-10-10_x", "2026-10-10_1;DROP TABLE pago", "2026-10-10_1234567890"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.pendientes(null, null, malo, 5), "cursor: " + malo);
        }
    }

    @Test
    void rechazaBusquedaDemasiadoLarga() {
        String larga = "a".repeat(PagoServicio.LARGO_MAXIMO_BUSQUEDA + 1);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.realizados(null, larga, null, 10));
        assertTrue(e.getMessage().contains("búsqueda"));
        assertThrows(IllegalArgumentException.class, () -> servicio.pendientes(null, larga, null, 5));
    }
}

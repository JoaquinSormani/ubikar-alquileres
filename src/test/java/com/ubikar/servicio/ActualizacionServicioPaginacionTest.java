package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Validaciones de la paginación de actualizaciones (aplicadas y próximas): se rechazan antes de tocar la base de datos. */
class ActualizacionServicioPaginacionTest {

    private final ActualizacionServicio servicio = new ActualizacionServicio();

    @Test
    void rechazaTamanioFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> servicio.aplicadas(null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> servicio.proximas(null, null, null, -1));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.aplicadas(null, null, null, PagoServicio.TAMANIO_MAXIMO + 1));
        assertTrue(e.getMessage().contains("entre 1 y"));
        assertThrows(IllegalArgumentException.class, () -> servicio.proximas(null, null, null, PagoServicio.TAMANIO_MAXIMO + 1));
    }

    @Test
    void rechazaCursorMalFormadoOConFechaImposible() {
        for (String malo : new String[] {"abc", "2026-10-15", "2026-10-15_", "_5", "2026-10-15-5", "2026-10-15_5_6", "15/10/2026_5",
                "2026-10-15_1234567890", "2026-10-15;DROP TABLE actualizacion_valor_1", "2026-13-45_1", "2026-02-30_1", "٢٠٢٦-١٠-١٥_١"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.aplicadas(null, null, malo, 10), "aplicadas, cursor: " + malo);
            assertThrows(IllegalArgumentException.class, () -> servicio.proximas(null, null, malo, 5), "proximas, cursor: " + malo);
        }
    }

    @Test
    void rechazaBusquedaDemasiadoLarga() {
        String larga = "a".repeat(PagoServicio.LARGO_MAXIMO_BUSQUEDA + 1);
        assertThrows(IllegalArgumentException.class, () -> servicio.aplicadas(null, larga, null, 10));
        assertThrows(IllegalArgumentException.class, () -> servicio.proximas(null, larga, null, 5));
    }
}

package com.ubikar.excepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ManejadorGlobalDeErroresTest {

    private final ManejadorGlobalDeErrores manejador = new ManejadorGlobalDeErrores();

    @Test
    void losDatosInvalidosRespondenConSuMensaje() {
        ResponseEntity<Map<String, String>> r = manejador.datosInvalidos(new IllegalArgumentException("El cobro indicado no existe."));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals("El cobro indicado no existe.", r.getBody().get("error"));
    }

    @Test
    void laFallaTecnicaNoExponeDetallesInternos() {
        ResponseEntity<Map<String, String>> r = manejador.fallaTecnica(new IllegalStateException("password=secreta en la conexión"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals(ManejadorGlobalDeErrores.MENSAJE_TECNICO, r.getBody().get("error"));
    }
}

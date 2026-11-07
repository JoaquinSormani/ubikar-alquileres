package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ubikar.modelo.Inquilino;
import org.junit.jupiter.api.Test;

class ValidacionPersonaTest {

    private static Inquilino valido() {
        Inquilino i = new Inquilino();
        i.setNombre("Ines");
        i.setApellido("Lopez");
        i.setDni("30.111.222");
        i.setDomicilio("Sucre 5, San Luis");
        return i;
    }

    private static String mensaje(Inquilino i) {
        return assertThrows(IllegalArgumentException.class, () -> ValidacionPersona.validar(i, "inquilino")).getMessage();
    }

    @Test
    void unaPersonaValidaNormalizaElDni() {
        Inquilino i = valido();
        ValidacionPersona.validar(i, "inquilino");
        assertEquals("30111222", i.getDni());
    }

    @Test
    void elDniNecesitaEntreSieteYQuinceDigitos() {
        Inquilino corto = valido();
        corto.setDni("12.34");
        assertTrue(mensaje(corto).contains("DNI"));
        Inquilino largo = valido();
        largo.setDni("1".repeat(16));
        assertTrue(mensaje(largo).contains("DNI"));
    }

    @Test
    void losDatosMasLargosQueLaBaseSeRechazanConMensaje() {
        Inquilino nombre = valido();
        nombre.setNombre("N".repeat(81));
        assertTrue(mensaje(nombre).contains("80"));
        Inquilino domicilio = valido();
        domicilio.setDomicilio("D".repeat(201));
        assertTrue(mensaje(domicilio).contains("200"));
        Inquilino telefono = valido();
        telefono.setTelefono("1".repeat(31));
        assertTrue(mensaje(telefono).contains("30"));
        Inquilino email = valido();
        email.setEmail("e".repeat(121));
        assertTrue(mensaje(email).contains("120"));
    }

    @Test
    void faltaElDomicilio() {
        Inquilino i = valido();
        i.setDomicilio("  ");
        assertTrue(mensaje(i).contains("domicilio"));
    }

    @Test
    void lasListasNulasDeLaSolicitudSeTomanComoVacias() {
        SolicitudContrato s = new SolicitudContrato();
        s.setInquilinos(null);
        s.setGarantes(null);
        s.setRenglones(null);
        assertTrue(s.getInquilinos().isEmpty() && s.getGarantes().isEmpty() && s.getRenglones().isEmpty());
    }
}

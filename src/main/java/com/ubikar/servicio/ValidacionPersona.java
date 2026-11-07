package com.ubikar.servicio;

import com.ubikar.modelo.Persona;

/**
 * Reglas comunes a propietarios, inquilinos y garantes: datos obligatorios y largos que entran en la base
 * (así un dato desmedido se rechaza con un mensaje claro en lugar de fallar al guardar).
 */
final class ValidacionPersona {

    private ValidacionPersona() {
    }

    static void validar(Persona p, String rol) {
        requerido(hayTexto(p.getNombre()) && hayTexto(p.getApellido()), "Faltan el nombre y el apellido del " + rol + ".");
        requerido(p.getNombre().trim().length() <= 80 && p.getApellido().trim().length() <= 80,
                "El nombre y el apellido del " + rol + " no pueden superar los 80 caracteres.");
        String dni = p.getDni() == null ? "" : p.getDni().replaceAll("\\D", "");
        requerido(dni.length() >= 7 && dni.length() <= 15, "El DNI del " + rol + " no es válido.");
        requerido(hayTexto(p.getDomicilio()), "Falta el domicilio del " + rol + " (se usa en el contrato).");
        requerido(p.getDomicilio().trim().length() <= 200, "El domicilio del " + rol + " no puede superar los 200 caracteres.");
        requerido(p.getTelefono() == null || p.getTelefono().trim().length() <= 30,
                "El teléfono del " + rol + " no puede superar los 30 caracteres.");
        requerido(p.getEmail() == null || p.getEmail().trim().length() <= 120,
                "El email del " + rol + " no puede superar los 120 caracteres.");
        p.setDni(dni);
    }

    static boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    static void requerido(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}

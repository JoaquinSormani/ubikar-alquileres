package com.ubikar.servicio;

import com.ubikar.dao.PropiedadDAO;
import com.ubikar.dao.PropietarioDAO;
import com.ubikar.dao.Sql2oDAO;
import com.ubikar.modelo.Propiedad;
import com.ubikar.modelo.Propietario;
import org.sql2o.Connection;

public class RegPropiedad {

    private final PropietarioDAO propietarioDAO = new PropietarioDAO();
    private final PropiedadDAO propiedadDAO = new PropiedadDAO();

    public Propiedad registrar(SolicitudPropiedad s) {
        validar(s);
        Propietario p = s.getPropietario();
        String direccion = s.getDireccion().trim().replaceAll("\\s+", " ");

        // Control de duplicados (evita registrar la misma dirección si ya existe en el sistema)
        Propiedad existente = propiedadDAO.readAll().stream()
                .filter(x -> x.getDireccion().trim().equalsIgnoreCase(direccion))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            if (p.getId() != null && existente.getIdPropietario().equals(p.getId())) {
                throw new IllegalArgumentException("Este propietario ya tiene una propiedad registrada con esa dirección.");
            } else {
                throw new IllegalArgumentException("Ya existe una propiedad registrada con esa dirección (pertenece a otro propietario).");
            }
        }

        try (Connection con = Sql2oDAO.getSql2o().beginTransaction()) {
            if (p.getId() == null) {
                p.setId(propietarioDAO.create(con, p));
            } else {
                propietarioDAO.update(con, p);
            }
            Propiedad nueva = new Propiedad();
            nueva.setDireccion(direccion);
            nueva.setIdPropietario(p.getId());
            nueva.setId(propiedadDAO.create(con, nueva));
            con.commit(false);
            return nueva;
        }
    }

    private void validar(SolicitudPropiedad s) {
        requerido(s != null && s.getPropietario() != null, "Falta el propietario.");
        Propietario p = s.getPropietario();
        requerido(hayTexto(p.getNombre()) && hayTexto(p.getApellido()), "Faltan el nombre y el apellido del propietario.");
        requerido(hayTexto(p.getDni()) && p.getDni().replaceAll("\\D", "").length() >= 7, "El DNI del propietario no es válido.");
        requerido(hayTexto(p.getDomicilio()), "Falta el domicilio del propietario.");
        requerido(hayTexto(s.getDireccion()) && s.getDireccion().trim().length() >= 5, "Falta la dirección de la propiedad.");
        p.setDni(p.getDni().replaceAll("\\D", ""));
        if (p.getId() == null) {
            requerido(propietarioDAO.readAll().stream().noneMatch(x -> x.getDni().equals(p.getDni())),
                    "Ya existe un propietario con ese DNI.");
        }
    }

    private static boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static void requerido(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}

package com.ubikar.controlador;

import com.ubikar.dao.PropiedadDAO;
import com.ubikar.modelo.Propiedad;
import com.ubikar.servicio.RegPropiedad;
import com.ubikar.servicio.SolicitudPropiedad;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/propiedades")
public class PropiedadControlador {

    private final PropiedadDAO propiedadDAO = new PropiedadDAO();
    private final RegPropiedad regPropiedad = new RegPropiedad();

    @GetMapping
    public List<Propiedad> getPropiedades(@RequestParam(required = false) Integer idPropietario) {
        return idPropietario == null ? propiedadDAO.selectAll() : propiedadDAO.selectByPropietario(idPropietario);
    }

    @PostMapping
    public ResponseEntity<Propiedad> registrar(@RequestBody SolicitudPropiedad solicitud) {
        Propiedad propiedad = regPropiedad.registrar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(propiedad);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}


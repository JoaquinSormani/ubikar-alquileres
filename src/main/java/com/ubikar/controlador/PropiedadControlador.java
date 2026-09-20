package com.ubikar.controlador;

import com.ubikar.dao.PropiedadDAO;
import com.ubikar.modelo.Propiedad;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/propiedades")
public class PropiedadControlador {

    private final PropiedadDAO propiedadDAO = new PropiedadDAO();

    @GetMapping
    public List<Propiedad> getPropiedades(@RequestParam(required = false) Integer idPropietario) {
        return idPropietario == null ? propiedadDAO.selectAll() : propiedadDAO.selectByPropietario(idPropietario);
    }
}

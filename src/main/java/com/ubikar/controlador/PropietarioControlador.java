package com.ubikar.controlador;

import com.ubikar.dao.PropietarioDAO;
import com.ubikar.modelo.Propietario;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/propietarios")
public class PropietarioControlador {

    private final PropietarioDAO propietarioDAO = new PropietarioDAO();

    @GetMapping
    public List<Propietario> getPropietarios() {
        return propietarioDAO.selectAll();
    }

    @PostMapping
    public Propietario insertPropietario(@RequestBody Propietario propietario) {
        Integer id = propietarioDAO.insert(propietario);
        propietario.setId(id);
        return propietario;
    }

    @DeleteMapping("/{id}")
    public void deletePropietario(@PathVariable Integer id) {
        propietarioDAO.delete(id);
    }
}

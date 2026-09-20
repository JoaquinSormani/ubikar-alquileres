package com.ubikar.controlador;

import com.ubikar.dao.GaranteDAO;
import com.ubikar.modelo.Garante;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/garantes")
public class GaranteControlador {

    private final GaranteDAO garanteDAO = new GaranteDAO();

    @GetMapping
    public List<Garante> getGarantes() {
        return garanteDAO.selectAll();
    }
}

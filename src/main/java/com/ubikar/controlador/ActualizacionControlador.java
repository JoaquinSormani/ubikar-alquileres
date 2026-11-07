package com.ubikar.controlador;

import com.ubikar.servicio.ActualizacionServicio;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/actualizaciones")
public class ActualizacionControlador {

    private final ActualizacionServicio actualizacionServicio = new ActualizacionServicio();

    @GetMapping("/panel")
    public ActualizacionServicio.PanelActualizaciones panel() {
        return actualizacionServicio.panel();
    }
}

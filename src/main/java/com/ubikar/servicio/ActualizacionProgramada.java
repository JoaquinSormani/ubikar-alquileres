package com.ubikar.servicio;

import java.time.LocalDate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ActualizacionProgramada {

    private final ActualizacionServicio servicio = new ActualizacionServicio();

    @Scheduled(cron = "0 5 6 * * *")              // todos los días a las 06:05
    public void aplicarPendientes() {
        servicio.aplicarPendientes(LocalDate.now());
    }

    @EventListener(ApplicationReadyEvent.class)   // y al arrancar, por si el sistema estuvo apagado
    public void alArrancar() {
        servicio.aplicarPendientes(LocalDate.now());
    }
}

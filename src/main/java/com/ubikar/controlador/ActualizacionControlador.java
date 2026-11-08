package com.ubikar.controlador;

import com.ubikar.servicio.ActualizacionServicio;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/actualizaciones")
public class ActualizacionControlador {

    private final ActualizacionServicio actualizacionServicio = new ActualizacionServicio();

    /** La fecha de hoy y los meses del selector. Las filas se piden aparte, de a una página. */
    @GetMapping("/panel")
    public ActualizacionServicio.PanelActualizaciones panel() {
        return actualizacionServicio.panel();
    }

    /**
     * Actualizaciones ya aplicadas, de la más nueva a la más vieja, de a una página. Parámetros (todos opcionales): mes=AAAA-MM,
     * q=texto a buscar (DNI, nombre o dirección), tamanio=cantidad por página y despues=cursor que devolvió la página anterior.
     */
    @GetMapping("/aplicadas")
    public ActualizacionServicio.Pagina<ActualizacionServicio.FilaActualizacion> aplicadas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String despues,
            @RequestParam(required = false) Integer tamanio) {
        return actualizacionServicio.aplicadas(mes, q, despues, tamanio);
    }

    /** Igual que /aplicadas pero para las próximas actualizaciones (una por contrato), de la más cercana a la más lejana. */
    @GetMapping("/proximas")
    public ActualizacionServicio.Pagina<ActualizacionServicio.FilaProxima> proximas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String despues,
            @RequestParam(required = false) Integer tamanio) {
        return actualizacionServicio.proximas(mes, q, despues, tamanio);
    }
}

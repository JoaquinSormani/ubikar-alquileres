package com.ubikar.controlador;

import com.ubikar.modelo.Pago;
import com.ubikar.servicio.PagoServicio;
import com.ubikar.servicio.SolicitudPago;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pagos")
public class PagoControlador {

    private final PagoServicio pagoServicio = new PagoServicio();

    @GetMapping("/panel")
    public PagoServicio.PanelPagos panel() {
        return pagoServicio.panel();
    }

    @GetMapping("/{id}/calculo")
    public PagoServicio.Calculo calculo(@PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return pagoServicio.calculo(id, fecha);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody SolicitudPago solicitud) {
        Pago p = pagoServicio.registrar(solicitud);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", p.getId());
        respuesta.put("idContrato", p.getIdContrato());
        respuesta.put("periodo", p.getPeriodo());
        respuesta.put("fechaPago", p.getFecha());
        respuesta.put("monto", p.getMonto());
        respuesta.put("diasAtraso", p.getDiasAtraso());
        respuesta.put("punitorio", p.getPunitorio());
        respuesta.put("total", p.getMonto().add(p.getPunitorio()));
        respuesta.put("recibo", "/api/pagos/" + p.getId() + "/recibo");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}/recibo")
    public ResponseEntity<byte[]> recibo(@PathVariable Integer id) {
        byte[] pdf = pagoServicio.recibo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pagoServicio.contenidoRecibo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recibo-" + String.format("%08d", id) + ".pdf\"")
                .body(pdf);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

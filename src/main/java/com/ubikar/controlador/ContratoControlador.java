package com.ubikar.controlador;

import com.ubikar.modelo.Contrato;
import com.ubikar.servicio.ContratoServicio;
import com.ubikar.servicio.SolicitudContrato;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/contratos")
public class ContratoControlador {

    private static final MediaType DOCX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ContratoServicio contratoServicio = new ContratoServicio();

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody SolicitudContrato solicitud) {
        Contrato c = contratoServicio.registrar(solicitud);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", c.getId());
        respuesta.put("fechaInicio", c.getFechaInicio());
        respuesta.put("fechaFin", c.getFechaFin());
        respuesta.put("documento", "/api/contratos/" + c.getId() + "/documento");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}/documento")
    public ResponseEntity<byte[]> documento(@PathVariable Integer id) throws IOException {
        Path archivo = contratoServicio.documentoDelContrato(id);
        if (archivo == null || !Files.exists(archivo)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contrato-" + id + ".docx\"")
                .body(Files.readAllBytes(archivo));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

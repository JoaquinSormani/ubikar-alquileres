package com.ubikar.controlador;

import com.ubikar.dto.PagoRegistradoDTO;
import com.ubikar.modelo.Pago;
import com.ubikar.servicio.PagoServicio;
import com.ubikar.servicio.SolicitudPago;
import java.time.LocalDate;
import java.time.YearMonth;
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

    /** Genera los cobros que faltan de cada mes y devuelve los meses que tienen cobros (para el selector). */
    @GetMapping("/panel")
    public PagoServicio.PanelPagos panel() {
        return pagoServicio.panel();
    }

    /**
     * Cobros pendientes de a una página, del más vencido al menos vencido. Parámetros (todos opcionales): mes=AAAA-MM,
     * q=texto a buscar (DNI, nombre o dirección), tamanio=cantidad por página y despues=cursor que devolvió la página anterior.
     */
    @GetMapping("/pendientes")
    public PagoServicio.PaginaPagos pendientes(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String despues,
            @RequestParam(required = false) Integer tamanio) {
        return pagoServicio.pendientes(mes, q, despues, tamanio);
    }

    /** Igual que /pendientes pero para los pagos ya realizados, del recibo más nuevo al más viejo. */
    @GetMapping("/realizados")
    public PagoServicio.PaginaPagos realizados(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String despues,
            @RequestParam(required = false) Integer tamanio) {
        return pagoServicio.realizados(mes, q, despues, tamanio);
    }

    @GetMapping("/{id}/calculo")
    public PagoServicio.Calculo calculo(@PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return pagoServicio.calculo(id, fecha);
    }

    @PostMapping
    public ResponseEntity<PagoRegistradoDTO> registrar(@RequestBody SolicitudPago solicitud) {
        Pago p = pagoServicio.registrar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(PagoRegistradoDTO.de(p));
    }

    @GetMapping("/{id}/recibo")
    public ResponseEntity<byte[]> recibo(@PathVariable Integer id) {
        byte[] pdf = pagoServicio.recibo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pagoServicio.contenidoRecibo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recibo-" + String.format("%08d", pagoServicio.numeroRecibo(id)) + ".pdf\"")
                .body(pdf);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

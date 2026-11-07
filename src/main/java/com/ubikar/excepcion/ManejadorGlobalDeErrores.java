package com.ubikar.excepcion;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Sql2oException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores a respuestas HTTP en un solo lugar. Los datos inválidos de una regla de negocio siguen
 * respondiendo 400 con su mensaje; las fallas técnicas (base de datos, generación de documentos) responden 500
 * con un mensaje genérico, sin trazas ni detalles internos, y quedan registradas en el log del servidor.
 * Los controladores que ya tienen su propio manejo de errores siguen usándolo.
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);
    static final String MENSAJE_TECNICO = "No se pudo completar la operación. Intentá de nuevo.";
    static final String MENSAJE_DATOS_ILEGIBLES = "Los datos enviados no son válidos. Revisá los campos, las fechas y los números.";
    static final String MENSAJE_CONFLICTO = "No se pudo completar la operación: choca con datos ya registrados "
            + "(por ejemplo, un registro repetido o que todavía se usa en otro lugar).";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", MENSAJE_DATOS_ILEGIBLES));
    }

    @ExceptionHandler({IllegalStateException.class, Sql2oException.class})
    public ResponseEntity<Map<String, String>> fallaTecnica(RuntimeException e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLIntegrityConstraintViolationException) {
                LOG.warn("Conflicto con datos existentes: {}", causa.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", MENSAJE_CONFLICTO));
            }
        }
        LOG.error("Falla técnica", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", MENSAJE_TECNICO));
    }
}

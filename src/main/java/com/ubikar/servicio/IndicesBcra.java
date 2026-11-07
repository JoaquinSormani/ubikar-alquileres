package com.ubikar.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta los índices en la API de Estadísticas del BCRA (pública, sin clave).
 * ICL = variable 40 (valor diario). Inflación mensual (IPC del INDEC) = variable 27 (% mensual).
 */
public class IndicesBcra {

    private static final String BASE = "https://api.bcra.gob.ar/estadisticas/v4.0/monetarias/";
    private static final int ID_ICL = 40;
    private static final int ID_INFLACION_MENSUAL = 27;

    private final HttpClient http;
    private final ObjectMapper json;

    public IndicesBcra() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), new ObjectMapper());
    }

    public IndicesBcra(HttpClient http, ObjectMapper json) {
        this.http = http;
        this.json = json;
    }

    /** Valor del ICL en la fecha. Si ese día no hay dato (feriado, fecha futura), usa el último publicado antes. */
    public BigDecimal icl(LocalDate fecha) {
        LocalDate hasta = fecha.isAfter(LocalDate.now()) ? LocalDate.now() : fecha; // el BCRA no acepta fechas futuras
        JsonNode detalle = consultar(ID_ICL, hasta.minusDays(10), hasta);
        if (detalle == null || detalle.isEmpty()) {
            throw new IllegalStateException("El BCRA no devolvió el ICL cerca del " + fecha + ".");
        }
        // la API devuelve primero la fecha más reciente
        return new BigDecimal(detalle.get(0).get("valor").asText());
    }

    /** Inflación mensual (en %) de cada mes entre desde y hasta, ambos incluidos, en orden cronológico. */
    public List<BigDecimal> inflacionMensual(YearMonth desde, YearMonth hasta) {
        JsonNode detalle = consultar(ID_INFLACION_MENSUAL, desde.atDay(1), hasta.atEndOfMonth());
        List<BigDecimal> valores = new ArrayList<>();
        if (detalle != null) {
            for (int i = detalle.size() - 1; i >= 0; i--) { // la API viene del más nuevo al más viejo
                valores.add(new BigDecimal(detalle.get(i).get("valor").asText()));
            }
        }
        long esperados = desde.until(hasta, java.time.temporal.ChronoUnit.MONTHS) + 1;
        if (valores.size() != esperados) {
            throw new IllegalStateException("Falta la inflación de algún mes entre " + desde + " y " + hasta
                    + " (el INDEC la publica con atraso). Se reintenta mañana.");
        }
        return valores;
    }

    private JsonNode consultar(int idVariable, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(LocalDate.now())) {
            return json.createArrayNode(); // todavía no hay datos de esas fechas
        }
        String url = BASE + idVariable + "?desde=" + desde + "&hasta=" + hasta + "&limit=100";
        try {
            HttpResponse<String> r = http.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                throw new IllegalStateException("El BCRA respondió " + r.statusCode() + ".");
            }
            JsonNode root = json.readTree(r.body());
            JsonNode results = root.get("results");
            if (results == null || results.isEmpty()) {
                return json.createArrayNode();
            }
            JsonNode detalle = results.get(0).get("detalle");
            return detalle != null ? detalle : json.createArrayNode();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo consultar el BCRA. Se reintenta mañana.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta al BCRA interrumpida.", e);
        }
    }
}

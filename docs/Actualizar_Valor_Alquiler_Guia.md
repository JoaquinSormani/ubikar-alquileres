# Actualizar valor de alquiler — guía para Matías

Este es el tercer caso de uso de la entrega final. Esta guía dice **qué hace**, **qué ya existe en el código** y **qué hay que escribir**, con el código de la consulta al BCRA ya probado contra la API real.

**La actualización es automática.** El Responsable de UBIKAR no carga ni confirma nada: el sistema aplica el aumento solo cuando llega la fecha de ajuste de cada contrato. El **frontend ya está hecho** (`frontend/pages/actualizacion.html|js|css`) y es una pantalla de **consulta**; falta todo el backend.

Como modelo para copiar la estructura, mirá **Registrar Pago** y **Consultar Tablero**: `PagoControlador`, `PagoServicio`, `PagoDAO` y `pago.js`. La organización de carpetas y las reglas de capas están en `ARQUITECTURA.md`.

## 1. Qué hace el caso de uso

El contrato dice (cláusula CUARTA) que el valor del alquiler *"se ajustará automáticamente cada X meses conforme al índice"* y que la corredora *"comunicará al locatario el nuevo valor"*. Entonces:

1. **El sistema aplica solo el aumento** el día de la fecha de ajuste, consultando el índice (ICL o IPC) al BCRA. Nadie tipea ni confirma nada.
2. El Responsable **consulta** en una pantalla qué se actualizó (con un filtro por mes) y **avisa al inquilino** por WhatsApp (el botón arma el mensaje; no se envía solo).

Flujo automático (lo dispara una **tarea programada** todos los días a la madrugada, y también una vez al arrancar el backend para ponerse al día):

1. Para cada contrato vigente, buscar las actualizaciones **pendientes** (fecha de ajuste `<= hoy` y todavía no aplicada).
2. Para cada una, en orden: consultar el índice al BCRA y calcular el valor nuevo.
3. Guardar la actualización y cambiar el monto de los cobros (todo en una transacción por contrato).
4. Si el BCRA no responde o el IPC de ese mes todavía no se publicó, **no se hace nada** con ese contrato: queda **"esperando el índice"** y se reintenta al día siguiente. No hay carga manual.

Como el caso lo inicia el sistema y no una persona, en el diagrama de casos de uso el actor principal es el **Sistema (temporizador)** y el **Responsable de UBIKAR** es actor secundario (consulta y avisa). Conviene confirmar con la cátedra que está bien modelarlo así.

## 2. Lo que ya existe

| Dato | Dónde está |
|---|---|
| Valor del primer mes | `Contrato.valorInicial` |
| Cada cuántos meses se ajusta | `Contrato.periodicidadActualizacion` (1, 3, 6…) |
| Qué índice usa | `Contrato.indiceActualizacion` (enum `IndiceActualizacion`: `ICL` o `IPC`) |
| Monto de cada cobro | `Pago.monto` (tabla `pago`) |
| Factor aplicado a un cobro | `Pago.indiceActualizacion` (`DECIMAL(10,6)`, hoy siempre `NULL`: usalo para guardar el factor) |
| Cobros de cada mes | `PagoServicio.asegurarCobros(contrato, hoy)` los crea hasta el mes actual |

**Cómo se mueve el valor hoy:** `asegurarCobros` crea cada cobro nuevo con el monto del **último cobro existente**. Por eso, para actualizar el valor alcanza con cambiar el `monto` de los cobros **PENDIENTES** desde el mes del ajuste en adelante: los cobros que se creen después heredan el valor nuevo solos. Los cobros **PAGADOS no se tocan nunca**.

**Valor vigente de un contrato** = `monto` del último cobro del contrato (o `valorInicial` si todavía no hay cobros).

## 3. Cuándo toca actualizar

Sea `N = periodicidadActualizacion` y `k = 1, 2, 3…` el número de actualización:

- Fecha de la actualización `k`: `fechaInicio + k × N meses` (ejemplo: inicio 15/04/2026 y N = 3, entonces k = 1 es el 15/07/2026).
- El nuevo valor rige desde el cobro del **mes de esa fecha** (`periodoDesde` = día 1 de ese mes).
- Una actualización está **pendiente** si su fecha ya llegó (`<= hoy`) y no existe todavía en la tabla de actualizaciones. **Nunca se adelanta una actualización.**
- Si hay varias pendientes de un mismo contrato (por ejemplo, el sistema estuvo apagado), se aplican **de a una, en orden**, hasta ponerse al día.

## 4. Cómo se calcula

Cada período va desde la actualización anterior (`k − 1`, o `fechaInicio` si `k = 1`) hasta la actual (`k`).

- **ICL** (valor diario): `factor = ICL(fecha de la actualización k) ÷ ICL(fecha de la actualización k−1)`.
- **IPC** (inflación mensual en %): `factor = (1 + m₁/100) × (1 + m₂/100) × …` con la inflación de los **N meses del período**, desde el mes de la actualización anterior hasta el mes anterior a la nueva. Ejemplo: inicio abril, N = 3, primera actualización en julio → se encadenan abril, mayo y junio.
- `valorNuevo = valorActual × factor`, redondeado a 2 decimales.

> **Para confirmar con Chiara:** si el contrato quiere el cociente de índices de dos meses (en vez de encadenar los N meses). Es la misma duda que ya teníamos pendiente del contrato "completo". El código está pensado para cambiar esto en un solo lugar (`CalculoActualizacion`).

Valores de ejemplo (probados): `100000 × (ICL 33 ÷ ICL 30)` = **110000,00**; IPC de 2 % y 3 % = **105060,00**.

## 5. De dónde salen los índices (API del BCRA)

Es pública, **no necesita clave** y devuelve JSON. Versión **v4.0** (la v3 está en desuso).

```
GET https://api.bcra.gob.ar/estadisticas/v4.0/monetarias/{idVariable}?desde=AAAA-MM-DD&hasta=AAAA-MM-DD&limit=100
```

| Índice | idVariable | Qué devuelve |
|---|---|---|
| ICL | **40** | Valor diario (base 30/6/20 = 1). Hoy ronda 37,3. |
| IPC | **27** | Inflación mensual del INDEC en %, con fecha de fin de mes. Último dato publicado: septiembre. |

Respuesta (el dato **más reciente viene primero**):

```json
{"status":200,"metadata":{...},"results":[{"idVariable":40,"detalle":[{"fecha":"2026-11-07","valor":37.34},{"fecha":"2026-11-06","valor":37.32}]}]}
```

Cosas que descubrimos al probarla:
- Si `desde` es una fecha **futura**, responde `400` ("la fecha desde no puede ser mayor a la actual").
- Si pedís meses de IPC que todavía no se publicaron, responde `200` con `detalle` **vacío**. El código de abajo lo convierte en una excepción: esa actualización **queda en espera y se reintenta al día siguiente** (la tarea programada corre todos los días).
- El ICL no tiene dato los días sin publicación: por eso se toma el último valor anterior a la fecha.
- No hace falta agregar ninguna dependencia: se usa `java.net.http.HttpClient` (Java 17) y Jackson, que ya viene con Spring Boot.

### `servicio/IndicesBcra.java` (probado con la API real)

```java
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

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    /** Valor del ICL en la fecha. Si ese día no hay dato (feriado, fecha futura), usa el último publicado antes. */
    public BigDecimal icl(LocalDate fecha) {
        LocalDate hasta = fecha.isAfter(LocalDate.now()) ? LocalDate.now() : fecha; // el BCRA no acepta fechas futuras
        JsonNode detalle = consultar(ID_ICL, hasta.minusDays(10), hasta);
        if (detalle.isEmpty()) {
            throw new IllegalStateException("El BCRA no devolvió el ICL cerca del " + fecha + ".");
        }
        // la API devuelve primero la fecha más reciente
        return new BigDecimal(detalle.get(0).get("valor").asText());
    }

    /** Inflación mensual (en %) de cada mes entre desde y hasta, ambos incluidos, en orden cronológico. */
    public List<BigDecimal> inflacionMensual(YearMonth desde, YearMonth hasta) {
        JsonNode detalle = consultar(ID_INFLACION_MENSUAL, desde.atDay(1), hasta.atEndOfMonth());
        List<BigDecimal> valores = new ArrayList<>();
        for (int i = detalle.size() - 1; i >= 0; i--) { // la API viene del más nuevo al más viejo
            valores.add(new BigDecimal(detalle.get(i).get("valor").asText()));
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
            return json.readTree(r.body()).get("results").get(0).get("detalle");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo consultar el BCRA. Se reintenta mañana.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta al BCRA interrumpida.", e);
        }
    }
}
```

### `servicio/CalculoActualizacion.java` (cálculo puro, sin red ni base)

```java
package com.ubikar.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public final class CalculoActualizacion {

    private CalculoActualizacion() {
    }

    /** ICL: valor del índice en la fecha del ajuste dividido el valor en la fecha base. */
    public static BigDecimal factorIcl(BigDecimal iclFinal, BigDecimal iclBase) {
        return iclFinal.divide(iclBase, 6, RoundingMode.HALF_UP);
    }

    /** IPC: se encadena la inflación mensual: (1 + m1/100) x (1 + m2/100) x ... */
    public static BigDecimal factorIpc(List<BigDecimal> inflacionMensualPorcentaje) {
        BigDecimal factor = BigDecimal.ONE;
        for (BigDecimal m : inflacionMensualPorcentaje) {
            factor = factor.multiply(BigDecimal.ONE.add(m.movePointLeft(2)));
        }
        return factor.setScale(6, RoundingMode.HALF_UP);
    }

    public static BigDecimal nuevoValor(BigDecimal valorActual, BigDecimal factor) {
        return valorActual.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Porcentaje de aumento para mostrar en pantalla, por ejemplo 10,00. */
    public static BigDecimal porcentaje(BigDecimal factor) {
        return factor.subtract(BigDecimal.ONE).movePointRight(2).setScale(2, RoundingMode.HALF_UP);
    }

    /** Fecha de la actualización número k (1 = la primera): inicio + k x periodicidad meses. */
    public static LocalDate fechaAjuste(LocalDate inicioContrato, int periodicidadMeses, int k) {
        return inicioContrato.plusMonths((long) periodicidadMeses * k);
    }
}
```

Prueba real (hecha el 7/11/2026): ICL del 1/4/2026 = 31,22 y del 1/10/2026 = 36,54, factor 1,170404 (17,04 %). IPC de julio a septiembre = 2,1 / 1,7 / 1,9, factor 1,058086.

## 6. Lo que hay que escribir

### 6.1 Base de datos

Una tabla nueva que deja el **historial** de cada actualización (sirve para no aplicar dos veces la misma y para mostrarla en la pantalla). Agregala a `bd_ubikar.sql` **y** creá `bd_migracion_actualizacion_valor.sql` para quienes ya tienen la base creada, como hicimos con `bd_migracion_numero_recibo.sql`.

```sql
CREATE TABLE `ubikar`.`actualizacion_valor` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idContrato` INT NOT NULL,
  `periodoDesde` DATE NOT NULL,            -- primer mes con el valor nuevo (día 1)
  `fecha` DATE NOT NULL,                   -- día en que el sistema la aplicó
  `indice` VARCHAR(10) NOT NULL,           -- ICL o IPC
  `valorAnterior` DECIMAL(12,2) NOT NULL,
  `valorNuevo` DECIMAL(12,2) NOT NULL,
  `factor` DECIMAL(10,6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_actualizacion` (`idContrato`, `periodoDesde`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`));
```

La clave `UNIQUE (idContrato, periodoDesde)` hace que la tarea sea **idempotente**: si corre dos veces, la segunda no duplica nada.

### 6.2 Clases (una por capa, mismos nombres para los diagramas)

| Capa | Clase | Qué hace |
|---|---|---|
| Frontend | `pages/actualizacion.html`, `.js`, `.css` | **Ya hecho.** Pantalla de consulta |
| Controlador | `ActualizacionControlador` | `GET /api/actualizaciones/panel`, con `@CrossOrigin(origins = "*")` |
| Tarea programada | `ActualizacionProgramada` | Dispara el servicio todos los días y al arrancar |
| Servicio | `ActualizacionServicio` | Reglas: buscar pendientes, calcular, aplicar (transacción) y armar el panel |
| Servicio | `IndicesBcra`, `CalculoActualizacion` | El código de la sección 5 |
| DAO | `ActualizacionValorDAO` | CRUD (`create`, `readAll`, `read`, `update`, `delete`) + `selectByContrato(idContrato)` |
| Modelo | `ActualizacionValor` | Los campos de la tabla |

Reglas de la cátedra que ya aplicamos en todo el proyecto: el DAO tiene **sí o sí el CRUD**, la conexión sale del **Singleton `Sql2oDAO`** (`Sql2oDAO.getSql2o()`), y el diagrama de secuencia muestra **todas** las clases del diagrama de clases que participan.

### 6.3 La tarea programada

Spring ya trae lo necesario, no hay que agregar dependencias:

```java
// en UbikarApplication: agregar @EnableScheduling a la clase

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
```

Los controladores del proyecto crean sus servicios con `new`, no con inyección: seguí ese mismo estilo.

**Qué hace `ActualizacionServicio.aplicarPendientes(hoy)`**, para cada contrato con `fechaFin >= hoy` y **de a una actualización por vez, en orden**:

1. Calcular `k` (la próxima actualización sin aplicar) y su fecha con `CalculoActualizacion.fechaAjuste`. Si es posterior a `hoy`, **pasar al siguiente contrato**.
2. Llamar a `PagoServicio.asegurarCobros(contrato, hoy)` para que existan los cobros hasta el mes actual.
3. Consultar el índice (`IndicesBcra`) y calcular el factor. **Si lanza `IllegalStateException`, no se aplica y se sigue con el siguiente contrato** (se reintenta mañana). Guardá el motivo en un log.
4. En **una transacción** (igual que `PagoServicio.registrar`: `Sql2oDAO.getSql2o().beginTransaction()` … `con.commit(false)`):
   - para cada cobro **PENDIENTE** del contrato con `periodo >= periodoDesde`: `monto = valorNuevo` y `indiceActualizacion = factor`;
   - insertar la fila en `actualizacion_valor`.
5. Repetir desde el paso 1 con la actualización siguiente, por si había varias atrasadas.

Los cobros **PAGADOS no se tocan nunca**. Para el cambio de monto alcanza con `PagoDAO.update(Connection, Pago)`, que ya existe.

### 6.4 El endpoint que usa la pantalla

**`GET /api/actualizaciones/panel`** (solo lectura):

```json
{
  "aplicadas": [
    {"id":3,"fecha":"2026-10-15","idContrato":101,"direccion":"Belgrano 1250, 2.º B",
     "inquilinos":[{"nombre":"Camila","apellido":"Rojas","dni":"30111222","telefono":"2664111222"}],
     "indice":"ICL","periodicidad":3,"periodoDesde":"2026-10","valorAnterior":275818.25,"valorNuevo":292818.58,"porcentaje":6.16}
  ],
  "proximas": [
    {"idContrato":3,"direccion":"Junin 700, depto 3",
     "inquilinos":[{"nombre":"Juan","apellido":"Perez","dni":"30111222","telefono":"2664001122"}],
     "indice":"ICL","periodicidad":3,"fechaAjuste":"2026-12-01","periodoDesde":"2026-12",
     "valorActual":350621.31,"estado":"PROGRAMADA"}
  ]
}
```

- `aplicadas`: todas las filas de `actualizacion_valor`, la más reciente primero. `periodicidad` es cada cuántos meses se actualiza el contrato (`Contrato.periodicidadActualizacion`): la pantalla la muestra junto al porcentaje ("+ 6,28 % · trimestral") para que se entienda de qué período es el aumento. Agregá el campo al `record FilaActualizacion` y pasá `c != null ? c.getPeriodicidadActualizacion() : null`. El **filtro por mes lo hace la pantalla** con `periodoDesde` (no hace falta ningún parámetro en el endpoint). Los datos de `inquilinos` salen del contrato (como en `PagoServicio.panel`). `porcentaje = (factor − 1) × 100`.
- `proximas`: **una por contrato vigente**, la próxima actualización que todavía no se aplicó (de la más cercana a la más lejana). `estado` es `"PROGRAMADA"` si la fecha de ajuste es futura o `"ESPERANDO_INDICE"` si ya llegó pero todavía no se pudo aplicar (por ejemplo, el IPC del mes no está publicado). `valorActual` es el valor vigente (el último `valorNuevo` aplicado o, si no hubo ninguno, `valorInicial`); el valor nuevo **no** se informa porque se calcula el día del ajuste. La pantalla las muestra al elegir un mes (con la opción "próximo mes" siempre disponible) para anticipar los avisos a los inquilinos.
- Siempre **200**. No hay endpoints de escritura: lo único que escribe es la tarea programada.

Para probar la parte automática sin esperar a la madrugada, alcanza con **reiniciar el backend** (corre al arrancar) o llamar al método desde un test. Para la demo conviene tener un contrato de hace unos meses con periodicidad corta, para que al arrancar se vea una actualización aplicada.

### 6.5 La pantalla (ya hecha)

`frontend/pages/actualizacion.html|js|css`, enlazada desde `index.html`:

- Tabla **"Actualizaciones"** (fecha, inquilino, propiedad, índice, desde cuándo rige, valor anterior y nuevo, con el porcentaje) y un botón **Avisar** que abre WhatsApp con el mensaje armado.
- **Selector de mes** (igual que en Registrar Pago) con los meses en que hubo actualizaciones, y buscador por DNI, nombre o propiedad.
- **Modo demostración:** mientras el servidor responda 404 en `/api/actualizaciones/panel`, la pantalla muestra datos de ejemplo con un aviso. Apenas exista el endpoint, pasa sola a los datos reales. Con `?demo` al final de la dirección se fuerza el modo demostración.

Si cambiás nombres de campos en el JSON, avisá para ajustar `actualizacion.js` (los datos de ejemplo están al principio del archivo y muestran la forma exacta).

### 6.6 Pruebas (sin red ni base, así `mvn package` no depende de nada)

```java
package com.ubikar.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculoActualizacionTest {

    @Test
    void iclAumentaPorElCocienteDelIndice() {
        BigDecimal factor = CalculoActualizacion.factorIcl(new BigDecimal("33"), new BigDecimal("30"));
        assertEquals(new BigDecimal("110000.00"), CalculoActualizacion.nuevoValor(new BigDecimal("100000"), factor));
        assertEquals(new BigDecimal("10.00"), CalculoActualizacion.porcentaje(factor));
    }

    @Test
    void ipcEncadenaLosMeses() {
        BigDecimal factor = CalculoActualizacion.factorIpc(List.of(new BigDecimal("2"), new BigDecimal("3")));
        assertEquals(new BigDecimal("105060.00"), CalculoActualizacion.nuevoValor(new BigDecimal("100000"), factor));
    }

    @Test
    void laPrimeraActualizacionEsInicioMasPeriodicidad() {
        assertEquals(LocalDate.of(2026, 7, 15), CalculoActualizacion.fechaAjuste(LocalDate.of(2026, 4, 15), 3, 1));
        assertEquals(LocalDate.of(2027, 1, 15), CalculoActualizacion.fechaAjuste(LocalDate.of(2026, 4, 15), 3, 3));
    }
}
```

## 7. Para la entrega (informe y presentación)

Este caso de uso va al informe como **sección 8.6**, igual que los otros: **especificación en una sola hoja**, **un diagrama de secuencia** y **un diagrama de clases**.

- **Actor principal: Sistema (temporizador).** Actor secundario: Responsable de UBIKAR (consulta el historial y avisa al inquilino). El BCRA es un sistema externo al que consulta `IndicesBcra`.
- **Secuencia:** `ActualizacionProgramada` → `ActualizacionServicio` → `ContratoDAO` / `PagoServicio` / `IndicesBcra` (BCRA) / `PagoDAO` / `ActualizacionValorDAO` → `Sql2oDAO` (Singleton). Mostrá el fragmento `alt` cuando el índice no está disponible (se reintenta mañana) y el `loop` sobre los contratos.
- **Clases:** la pantalla (`FormularioActualizacion`), `ActualizacionControlador`, `ActualizacionProgramada`, `ActualizacionServicio`, `IndicesBcra`, `CalculoActualizacion`, `ActualizacionValorDAO`, `Sql2oDAO` y el modelo `ActualizacionValor`.
- Los diagramas tienen que mostrar lo que el código hace de verdad. Cualquier cambio de nombres, avisá y se actualizan juntos.
- Para la demo en vivo: dejá un contrato con una actualización por aplicar, reiniciá el backend y mostrá en la pantalla que apareció en **"Actualizaciones aplicadas"** y que el monto nuevo se ve en **Registrar Pago**.

## 8. Checklist

- [ ] Tabla `actualizacion_valor` en `bd_ubikar.sql` y migración `bd_migracion_actualizacion_valor.sql`.
- [ ] `ActualizacionValor` y `ActualizacionValorDAO` (CRUD + `selectByContrato`).
- [ ] `IndicesBcra` y `CalculoActualizacion` (sección 5) y su prueba.
- [ ] `ActualizacionServicio` (`aplicarPendientes` y el armado del panel).
- [ ] `ActualizacionProgramada` y `@EnableScheduling` en `UbikarApplication`.
- [ ] `ActualizacionControlador` con `GET /api/actualizaciones/panel`.
- [ ] Probar el camino completo: reiniciar el backend → la pantalla muestra la actualización → el monto nuevo aparece en **Registrar Pago**.
- [ ] Especificación, secuencia y clases (sección 8.6 del informe) y diapositivas.
- [ ] `mvn test` y `mvn package` en verde antes de hacer `git push`.

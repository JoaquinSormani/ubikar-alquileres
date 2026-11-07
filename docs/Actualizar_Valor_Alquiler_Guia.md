# Actualizar valor de alquiler — guía para Matías

Este es el tercer caso de uso de la entrega final. Esta guía dice **qué hace**, **qué ya existe en el código** y **qué hay que escribir**, con el código de la consulta al BCRA ya probado contra la API real.

Como modelo para copiar la estructura, mirá **Registrar Pago**: `PagoControlador`, `PagoServicio`, `PagoDAO`, `frontend/pages/pago.html|js|css`. La organización de carpetas y las reglas de capas están en `ARQUITECTURA.md`.

## 1. Qué hace el caso de uso

El contrato dice (cláusula CUARTA) que el valor del alquiler *"se ajustará automáticamente cada X meses conforme al índice"* y que la corredora *"comunicará al locatario el nuevo valor"*. Entonces:

1. **El sistema calcula solo** el aumento, consultando el índice (ICL o IPC) al BCRA. Nadie tipea el índice.
2. **El Responsable de UBIKAR confirma** el aumento y se lo comunica al inquilino.

Flujo:

1. El Responsable abre la pantalla. Ve los contratos que **tienen una actualización pendiente** (ya llegó la fecha de ajuste y todavía no se aplicó).
2. Elige uno. El sistema muestra una **vista previa**: valor actual → valor nuevo, porcentaje de aumento, índice usado y fechas. Todavía no se guarda nada.
3. El Responsable confirma. El sistema guarda la actualización y cambia el monto de los cobros.
4. Alternativa: si la API del BCRA no responde, o el IPC del mes todavía no se publicó, el Responsable **carga el porcentaje a mano**. Esto es importante para que la demo no dependa de internet.

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
- Una actualización está **pendiente** si su fecha ya llegó (`<= hoy`) y no existe todavía en la tabla de actualizaciones. **No se permite adelantar una actualización.**
- Si hay varias pendientes de un mismo contrato, se aplican **de a una, en orden**.

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
- Si pedís meses de IPC que todavía no se publicaron, responde `200` con `detalle` **vacío**. El código de abajo lo convierte en un error con mensaje claro para que se cargue a mano.
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
                    + " (el INDEC la publica con atraso). Cargue el índice a mano.");
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
            throw new IllegalStateException("No se pudo consultar el BCRA. Cargue el índice a mano.", e);
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

Una tabla nueva que deja el **historial** de cada actualización (sirve para no aplicar dos veces la misma y para explicar el cálculo). Agregala a `bd_ubikar.sql` **y** creá `bd_migracion_actualizacion_valor.sql` para quienes ya tienen la base creada, como hicimos con `bd_migracion_numero_recibo.sql`.

```sql
CREATE TABLE `ubikar`.`actualizacion_valor` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idContrato` INT NOT NULL,
  `periodoDesde` DATE NOT NULL,            -- primer mes con el valor nuevo (día 1)
  `fecha` DATE NOT NULL,                   -- día en que se confirmó
  `indice` VARCHAR(10) NOT NULL,           -- ICL o IPC
  `valorAnterior` DECIMAL(12,2) NOT NULL,
  `valorNuevo` DECIMAL(12,2) NOT NULL,
  `factor` DECIMAL(10,6) NOT NULL,
  `origen` VARCHAR(10) NOT NULL,           -- BCRA o MANUAL
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_actualizacion` (`idContrato`, `periodoDesde`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`));
```

### 6.2 Clases (una por capa, mismos nombres para los diagramas)

| Capa | Clase | Qué hace |
|---|---|---|
| Frontend | `pages/actualizacion.html`, `.js`, `.css` | Pantalla (ver 6.4) |
| Controlador | `ActualizacionControlador` | Los 3 endpoints de abajo, con `@CrossOrigin(origins = "*")` |
| Servicio | `ActualizacionServicio` | Reglas: pendientes, vista previa, confirmar (transacción) |
| Servicio | `IndicesBcra`, `CalculoActualizacion` | El código de la sección 5 |
| DAO | `ActualizacionValorDAO` | CRUD (`create`, `readAll`, `read`, `update`, `delete`) + `selectByContrato(idContrato)` |
| Modelo | `ActualizacionValor` | Los campos de la tabla |

Reglas de la cátedra que ya aplicamos en todo el proyecto: el DAO tiene **sí o sí el CRUD**, la conexión sale del **Singleton `Sql2oDAO`** (`Sql2oDAO.getSql2o()`), y el diagrama de secuencia muestra **todas** las clases del diagrama de clases que participan.

### 6.3 Endpoints

**1) `GET /api/actualizaciones/pendientes`** → lista de contratos con una actualización pendiente:

```json
[{"idContrato":3,"direccion":"Junin 700, depto 3","inquilinos":[{"nombre":"Lucas","apellido":"Herrera"}],
  "valorActual":250000.00,"indice":"ICL","periodicidad":3,"fechaAjuste":"2026-07-15","periodoDesde":"2026-07"}]
```

**2) `GET /api/contratos/{id}/actualizacion`** (opcional `?porcentajeManual=17.5`) → vista previa, **no guarda nada**:

```json
{"idContrato":3,"indice":"ICL","origen":"BCRA","fechaBase":"2026-04-15","fechaAjuste":"2026-07-15",
 "valorIndiceBase":31.22,"valorIndiceFinal":36.54,"factor":1.170404,"porcentaje":17.04,
 "valorActual":250000.00,"valorNuevo":292601.00,"periodoDesde":"2026-07"}
```

**3) `POST /api/contratos/{id}/actualizacion`** con `{"porcentajeManual": 17.5}` (el campo es opcional; si no viene, se usa el BCRA) → confirma.
- **201** con el mismo cuerpo de la vista previa más `"id"` (el de la actualización).
- **400** `{"error": "mensaje en español"}` en estos casos: el contrato no existe, no hay actualización pendiente, el BCRA no responde o falta el dato (el mensaje tiene que decir "Cargue el índice a mano"), o el porcentaje manual es inválido.
- Códigos y forma de los errores iguales a los de `PagoControlador` (un `@ExceptionHandler(IllegalArgumentException.class)` que devuelve el 400).

**Qué hace el servicio al confirmar** (todo en **una transacción**, igual que `PagoServicio.registrar`: `Sql2oDAO.getSql2o().beginTransaction()` … `con.commit(false)`):

1. Validar que el contrato exista y que haya una actualización pendiente.
2. Llamar a `PagoServicio.asegurarCobros(contrato, hoy)` para que existan los cobros hasta el mes actual.
3. Calcular (ICL o IPC, o el porcentaje manual si vino).
4. Para cada cobro **PENDIENTE** del contrato con `periodo >= periodoDesde`: `monto = valorNuevo` y `indiceActualizacion = factor`.
5. Insertar la fila en `actualizacion_valor`.
6. `commit`. Si algo falla, se revierte todo.

Para el punto 4 conviene un método nuevo en `PagoDAO` (`update(Connection, Pago)` ya existe y alcanza).

### 6.4 Pantalla (frontend)

- Archivos en `frontend/pages/` y estilos propios; reutilizá `styles/ubk.css`, `components/ux.js` (avisos) y `services/api.js` (`API_BASE`). Copiá la estructura de `pago.html` / `pago.js`.
- Tabla con los contratos pendientes y un botón **Actualizar** por fila. El botón abre un cuadro con la vista previa (valor actual → valor nuevo, porcentaje, índice y fechas) y los botones **Confirmar** y **Cancelar**.
- Si el BCRA falla, el cuadro muestra el mensaje y habilita un campo **"Porcentaje de aumento"** para cargarlo a mano.
- Reglas que pidió la profesora y el cliente: **sin scroll** dentro de los cuadros, estilo **sobrio y empresarial** (sin efectos llamativos) y mensajes claros en español.
- Agregá el acceso a la pantalla desde `index.html`, como las demás.

### 6.5 Pruebas (sin red ni base, así `mvn package` no depende de nada)

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

Probá también a mano: con el backend en `localhost:8081`, `curl http://localhost:8081/api/actualizaciones/pendientes`.

## 7. Para la entrega (informe y presentación)

Este caso de uso va al informe como **sección 8.6**, igual que los otros: **especificación en una sola hoja**, **un diagrama de secuencia** y **un diagrama de clases** (con la pantalla, el controlador, el servicio, `IndicesBcra`, el DAO y el Singleton `Sql2oDAO`).

- Los diagramas tienen que mostrar lo que el código hace de verdad. Cualquier cambio de nombres, avisá y se actualizan juntos.
- Actor: **Responsable de UBIKAR**. El BCRA es un sistema externo que consulta el servicio (actor secundario o nota en el diagrama).
- Para la demo en vivo: tené un contrato con una actualización pendiente (por ejemplo, un contrato de hace 3 meses con periodicidad 3) y practicá también el camino del **porcentaje manual**.

## 8. Checklist

- [ ] Tabla `actualizacion_valor` en `bd_ubikar.sql` y migración `bd_migracion_actualizacion_valor.sql`.
- [ ] `ActualizacionValor` y `ActualizacionValorDAO` (CRUD + `selectByContrato`).
- [ ] `IndicesBcra` y `CalculoActualizacion` (sección 5) y su prueba.
- [ ] `ActualizacionServicio` y `ActualizacionControlador` con los 3 endpoints.
- [ ] Pantalla `pages/actualizacion.*`, con porcentaje manual de respaldo.
- [ ] Probar el camino completo: pendientes → vista previa → confirmar → ver el monto nuevo en **Registrar Pago**.
- [ ] Especificación, secuencia y clases (sección 8.6 del informe) y diapositivas.
- [ ] `mvn test` y `mvn package` en verde antes de hacer `git push`.

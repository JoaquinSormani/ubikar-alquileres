# Registrar Propiedad — guía para el backend

El frontend de este caso de uso ya está hecho (`frontend/propiedad.html`, `propiedad.css`, `propiedad.js`). Falta el backend: **un solo endpoint nuevo, `POST /api/propiedades`**. Todo lo demás que necesita el front ya existe.

## 1. Cómo probar el front

1. MySQL corriendo (Docker) con la base `ubikar` cargada.
2. Backend: `mvn spring-boot:run` (puerto 8081).
3. Frontend: en la carpeta `frontend/` correr `python -m http.server 5500`.
4. Abrir <http://localhost:5500/propiedad.html>.

Mientras el `POST` no exista, la pantalla muestra: *"El servidor todavía no tiene implementado el registro de propiedades"*. Cuando lo implementen, funciona sin tocar el front.

## 2. Cómo se comunican el front y Java

- El front (HTML + JS) corre en el **navegador**, servido en el puerto 5500. El backend Spring Boot corre en el **8081**. El JS le habla con `fetch("http://localhost:8081/api/...")`.
- Solo viaja **JSON**. El front no ve clases Java: Jackson (incluido en Spring) convierte el JSON en objetos Java **por el nombre del campo**: `"direccion"` llega a `setDireccion(...)`. Si el nombre no coincide, el campo llega `null` sin avisar.
- Como el front y el back están en puertos distintos, el navegador exige **CORS**: por eso los controladores llevan `@CrossOrigin(origins = "*")`. No lo saquen.
- Códigos de respuesta que entiende el front:
  - **201** — se registró. El cuerpo **tiene que traer `id`** (el front lo muestra como "Propiedad N.º ...").
  - **400** — datos inválidos. El cuerpo es `{"error": "mensaje en español"}` y el front **muestra ese mensaje tal cual**.
  - Cualquier otro (404, 405, 500, sin conexión) tiene su propio mensaje genérico en el front.

## 3. Lo que ya existe y usa el front (solo lectura)

| Pedido | Devuelve |
|---|---|
| `GET /api/propietarios` | `[{"id":1,"nombre":"Maria","apellido":"Gimenez","dni":"28555444","telefono":null,"email":null,"domicilio":"..."}]` |
| `GET /api/inquilinos` | igual forma que propietarios |
| `GET /api/garantes` | igual, más `"detalle"` |
| `GET /api/propiedades` | `[{"id":1,"direccion":"Junin 700, depto 3","idPropietario":9}]` |

El front los carga al abrir la página. Con eso busca por DNI en el navegador, completa los datos de quien ya existe y avisa si el propietario ya tiene una propiedad con esa dirección.

## 4. Lo que hay que implementar: `POST /api/propiedades`

**Pedido** (`Content-Type: application/json`):

```json
{
  "propietario": {
    "id": 9,
    "nombre": "Joaquin",
    "apellido": "Sormani",
    "dni": "46485903",
    "domicilio": "Pringles 1422",
    "telefono": "02664661612",
    "email": "joaquinsormani@gmail.com"
  },
  "direccion": "Junin 800, Dpto 4"
}
```

- Si el propietario **ya existe**, viene con `"id"` (y sus datos, posiblemente corregidos → hay que actualizarlos).
- Si es **nuevo**, viene **sin `"id"`** (hay que darlo de alta).
- `telefono` y `email` pueden venir como `""`.

**Respuesta OK — 201:**

```json
{ "id": 11, "direccion": "Junin 800, Dpto 4", "idPropietario": 9 }
```

**Respuesta con error — 400:**

```json
{ "error": "Este propietario ya tiene una propiedad registrada con esa dirección." }
```

## 5. Reglas de validación (el front ya las controla; repetirlas en el servidor)

- Propietario: nombre y apellido obligatorios; DNI de 7 o más dígitos (guardarlo solo con números); **domicilio obligatorio** (lo pide el contrato de alquiler más adelante).
- Propiedad: dirección obligatoria, de al menos 5 caracteres; guardarla con los espacios repetidos colapsados.
- Un propietario **no puede tener dos propiedades con la misma dirección** (sin distinguir mayúsculas ni espacios de más).
- Si el propietario viene **sin `id`**, no puede existir ya otro con ese DNI.

## 6. Guardado

Todo en **una sola transacción**: si el propietario es nuevo se inserta, si ya existe se actualiza, y después se inserta la propiedad. Si algo falla no debe quedar nada a medias. Los DAO ya tienen las variantes `insert(Connection, X)` y `update(Connection, X)` justamente para esto (mismo patrón que `ContratoServicio.registrar`).

## 7. Código sugerido

Sigue la misma estructura en capas que "Registrar Contrato": controlador → servicio → DAO. **Está probado**: compila y funciona contra la base real con todos los casos de la sección 8.

**`servicio/SolicitudPropiedad.java`** (el DTO: recibe el JSON del pedido)

```java
package com.ubikar.servicio;

import com.ubikar.modelo.Propietario;

public class SolicitudPropiedad {

    private Propietario propietario;
    private String direccion;

    public Propietario getPropietario() {
        return propietario;
    }

    public void setPropietario(Propietario propietario) {
        this.propietario = propietario;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
```

**`servicio/PropiedadServicio.java`** (reglas de negocio y transacción)

```java
package com.ubikar.servicio;

import com.ubikar.dao.PropiedadDAO;
import com.ubikar.dao.PropietarioDAO;
import com.ubikar.dao.Sql2oDAO;
import com.ubikar.modelo.Propiedad;
import com.ubikar.modelo.Propietario;
import org.sql2o.Connection;

public class PropiedadServicio {

    private final PropietarioDAO propietarioDAO = new PropietarioDAO();
    private final PropiedadDAO propiedadDAO = new PropiedadDAO();

    public Propiedad registrar(SolicitudPropiedad s) {
        validar(s);
        Propietario p = s.getPropietario();
        String direccion = s.getDireccion().trim().replaceAll("\\s+", " ");

        if (p.getId() != null) {
            boolean duplicada = propiedadDAO.selectByPropietario(p.getId()).stream()
                    .anyMatch(x -> x.getDireccion().trim().equalsIgnoreCase(direccion));
            if (duplicada) {
                throw new IllegalArgumentException("Este propietario ya tiene una propiedad registrada con esa dirección.");
            }
        }

        try (Connection con = Sql2oDAO.getSql2o().beginTransaction()) {
            if (p.getId() == null) {
                p.setId(propietarioDAO.insert(con, p));
            } else {
                propietarioDAO.update(con, p);
            }
            Propiedad nueva = new Propiedad();
            nueva.setDireccion(direccion);
            nueva.setIdPropietario(p.getId());
            nueva.setId(propiedadDAO.insert(con, nueva));
            con.commit(false);
            return nueva;
        }
    }

    private void validar(SolicitudPropiedad s) {
        requerido(s != null && s.getPropietario() != null, "Falta el propietario.");
        Propietario p = s.getPropietario();
        requerido(hayTexto(p.getNombre()) && hayTexto(p.getApellido()), "Faltan el nombre y el apellido del propietario.");
        requerido(hayTexto(p.getDni()) && p.getDni().replaceAll("\\D", "").length() >= 7, "El DNI del propietario no es válido.");
        requerido(hayTexto(p.getDomicilio()), "Falta el domicilio del propietario.");
        requerido(hayTexto(s.getDireccion()) && s.getDireccion().trim().length() >= 5, "Falta la dirección de la propiedad.");
        p.setDni(p.getDni().replaceAll("\\D", ""));
        if (p.getId() == null) {
            requerido(propietarioDAO.selectAll().stream().noneMatch(x -> x.getDni().equals(p.getDni())),
                    "Ya existe un propietario con ese DNI.");
        }
    }

    private static boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static void requerido(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
```

**`controlador/PropiedadControlador.java`** (ya existe con el `GET`; se le agrega lo marcado)

```java
package com.ubikar.controlador;

import com.ubikar.dao.PropiedadDAO;
import com.ubikar.modelo.Propiedad;
import com.ubikar.servicio.PropiedadServicio;          // NUEVO
import com.ubikar.servicio.SolicitudPropiedad;         // NUEVO
import java.util.List;
import java.util.Map;                                  // NUEVO
import org.springframework.http.HttpStatus;            // NUEVO
import org.springframework.http.ResponseEntity;        // NUEVO
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;  // NUEVO
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;       // NUEVO
import org.springframework.web.bind.annotation.RequestBody;       // NUEVO
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/propiedades")
public class PropiedadControlador {

    private final PropiedadDAO propiedadDAO = new PropiedadDAO();
    private final PropiedadServicio propiedadServicio = new PropiedadServicio();   // NUEVO

    @GetMapping
    public List<Propiedad> getPropiedades(@RequestParam(required = false) Integer idPropietario) {
        return idPropietario == null ? propiedadDAO.selectAll() : propiedadDAO.selectByPropietario(idPropietario);
    }

    @PostMapping                                                                    // NUEVO
    public ResponseEntity<Propiedad> registrar(@RequestBody SolicitudPropiedad solicitud) {
        Propiedad propiedad = propiedadServicio.registrar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(propiedad);
    }

    @ExceptionHandler(IllegalArgumentException.class)                               // NUEVO
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

No hace falta tocar los DAO ni el modelo: ya existen `PropietarioDAO.insert/update(Connection, ...)` y `PropiedadDAO.insert(Connection, ...)` / `selectByPropietario`.

## 8. Cómo probarlo sin el front

Desde Git Bash (con el backend corriendo):

```bash
curl -i -X POST http://localhost:8081/api/propiedades \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"propietario":{"nombre":"Carlos","apellido":"Suarez","dni":"31999888","domicilio":"Lavalle 55"},"direccion":"Ayacucho 1234"}'
```

Casos que tienen que responder así:

| Caso | Respuesta esperada |
|---|---|
| Propietario nuevo + dirección | `201` con `{"id":..,"direccion":..,"idPropietario":..}` |
| Propietario existente (con `id`) + otra dirección | `201` |
| Misma dirección (aunque cambien mayúsculas o espacios) | `400` "ya tiene una propiedad registrada con esa dirección" |
| Sin `domicilio` | `400` "Falta el domicilio del propietario." |
| DNI que ya existe, pero sin `id` | `400` "Ya existe un propietario con ese DNI." |

Después de las pruebas, borren las filas de prueba de `propiedad` y `propietario`.

## 9. Errores típicos

- **El front dice "sin el número de la propiedad registrada"**: la respuesta 201 no trae `id`. Devolver la `Propiedad` con su `id` ya cargado (`nueva.setId(...)`).
- **Los datos llegan `null`**: falta `@RequestBody`, o el nombre del campo Java no coincide con el del JSON.
- **El navegador dice "CORS"**: falta `@CrossOrigin(origins = "*")` en el controlador.
- **Cambiaron una clase y no se nota**: reiniciar `mvn spring-boot:run` (no hay recarga automática).
- **Acentos raros**: mandar siempre el pedido en UTF-8 (el front ya lo hace).

## 10. Si el grupo agrega campos a Propiedad

Hoy el modelo de dominio solo tiene `direccion`, y por eso el formulario tiene un solo campo. Para agregar otro (por ejemplo tipo de inmueble o ciudad) hay que tocar, en este orden: la columna en `bd_ubikar.sql` y en la base, la clase `Propiedad`, `PropiedadDAO`, `SolicitudPropiedad`, la validación del servicio y el formulario de `propiedad.html` / `propiedad.js`. Además, el Modelo de Dominio y los diagramas de diseño.

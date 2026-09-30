# UBIKAR - Sistema de Gestion de Alquileres

Proyecto Integrador de Ingenieria de Software II (2026). Caso de estudio: inmobiliaria UBIKAR, proceso de alquileres.

Documentacion (relato, requerimientos, vision, casos de uso, BPMN, modelo de dominio) en `../Etapa1_UBIKAR.md` y `../CasosDeUso_UBIKAR.drawio`.

## Stack

- **Backend**: Java 17, Spring Boot 3.5 (`@RestController`), Maven.
- **Persistencia**: SQL2o + MySQL. Patron **DAO** (`InquilinoDAO`, etc.) sobre un **Singleton** de conexion (`Sql2oDAO`).
- **Frontend**: HTML/JS plano (carpeta `frontend/`) que consume la API REST via `fetch()`, con CORS habilitado en el backend (`@CrossOrigin`).
- Arquitectura en capas: `modelo` (dominio), `dao` (persistencia), `controlador` (presentacion/REST).

## Base de datos

1. Correr `bd_ubikar.sql` en MySQL Workbench (crea la base `ubikar` y la tabla `inquilino` con un registro de ejemplo).
2. Completar usuario/contrasena reales en `src/main/java/com/ubikar/dao/Sql2oDAO.java`.

La conexion usa por defecto `jdbc:mysql://localhost:3306/ubikar`, usuario `root` y la clave de desarrollo del equipo. Cada integrante puede cambiarlos sin tocar el codigo con las variables de entorno `UBIKAR_DB_URL`, `UBIKAR_DB_USER` y `UBIKAR_DB_PASSWORD`.

## Como compilar y ejecutar el backend

```
mvn spring-boot:run
```

El backend queda en `http://localhost:8081`. Endpoints: `GET /api/inquilinos`, `POST /api/inquilinos`, `DELETE /api/inquilinos/{id}`.

Para generar el ejecutable:

```
mvn package
java -jar target/ubikar-alquileres.jar
```

## Como correr el frontend

Abrir `frontend/index.html` con un servidor local (por ejemplo la extension Live Server en Antigravity/VSCode) mientras el backend esta corriendo. El frontend apunta a `http://localhost:8081`.

Paginas:
- `index.html`: acceso del Responsable (usuario `responsable` / clave `ubikar2026`, solo del lado del front: no hay backend de autenticacion todavia) e inicio con las 2 opciones.
- `contrato.html`: asistente del Responsable para **Registrar Contrato de Alquiler**.
- `propiedad.html`: asistente del Responsable para **Registrar Propiedad**.
- `portal.html`: portal de autogestion de inquilinos y propietarios (acceso por DNI), independiente del login del Responsable.

## Caso de uso implementado: Registrar Contrato de Alquiler

Flujo: `contrato.html` (6 pasos) -> `POST /api/contratos` -> `ContratoServicio` (valida, guarda todo en **una transaccion** con SQL2o y genera el Word) -> `GET /api/contratos/{id}/documento`.

- Capas: `controlador` -> `servicio` (`ContratoServicio`) -> `dao` (SQL2o) / `documento` (generacion del .docx).
- Patrones: Singleton (`Sql2oDAO`), DAO (`*DAO`), Strategy (`GeneradorContrato`: cada plantilla de contrato es una estrategia; `GeneradorEstandar` y `GeneradorCompleto` comparten la base `GeneradorPlantilla`).
- Plantillas Word en `src/main/resources/plantillas/`: `contrato_estandar.docx` (modelo corto de UBIKAR) y `contrato_completo.docx` (modelo largo, 20 clausulas). Usan marcadores `{{campo}}` que reemplaza la libreria poi-tl. Los datos y textos derivados (montos en letras, fechas, casillas de garantia, fechas del primer ajuste) se arman en `documento/Marcadores.java`.
- El modelo completo deja renglones `__________` para lo que el sistema no conoce (horario y cuenta bancaria de cobro, marca/color de pintura, porcentajes de penalidad y sellado): se completan a mano en Word.
- Los contratos generados se guardan en `documentos/contratos/` y se registran como `Documentacion` de tipo `CONTRATO`. Un ejemplo de salida esta en `documentos/ejemplo/`.
- Para agregar otro modelo de contrato: nueva plantilla `.docx` + una clase que implemente `GeneradorContrato`, y registrarla en `ContratoServicio`.

## Trabajo en equipo: como sumar un caso de uso

Ya esta disponible para todos:
- Las 12 clases del modelo de dominio (`modelo/`) y sus tablas (`bd_ubikar.sql`).
- Un DAO con CRUD completo por entidad (`dao/`): Propietario, Inquilino, Garante, Propiedad, Contrato (con `vincularInquilino/Garante`), Inventario, RenglonInventario, Documentacion, **Pago, Liquidacion y Mantenimiento** (con `selectByContrato`, `selectByEstado`, `selectByPago`, etc.). `Sql2oDAO` es el Singleton de conexion.
- Un caso de uso completo como referencia: `ContratoControlador` -> `ContratoServicio` -> DAOs (+ `frontend/contrato.html`).

Como armar un caso de uso nuevo (misma estructura en capas):
1. `controlador/`: endpoint REST que recibe el pedido y traduce errores a HTTP (ver `ContratoControlador`).
2. `servicio/`: reglas de negocio y validaciones; lanza `IllegalArgumentException` con un mensaje claro cuando los datos no sirven.
3. `dao/`: solo SQL. Para guardar varias cosas juntas, los DAO tienen `insert(Connection, X)` y `update(Connection, X)`: abrir `Sql2oDAO.getSql2o().beginTransaction()`, usar esas variantes y cerrar con `con.commit(false)`; si algo falla, todo se revierte solo (ver `ContratoServicio.registrar`).
4. `frontend/`: pagina HTML/JS que consume la API, reutilizando `portal.css` para mantener la identidad de marca.
5. Tests que no necesiten la base en `src/test` (ver `documento/`), asi `mvn package` no depende de MySQL.

Convenciones para no pisarnos:
- Una rama por caso de uso (por ejemplo `cu/registrar-pago`) y Pull Request a `main`.
- Los DAO y el modelo son compartidos: si necesitas un metodo o campo nuevo, agregalo (no cambies los existentes) y avisa al grupo.
- Un cambio de esquema se hace en `bd_ubikar.sql` **y** se avisa: los demas deben aplicarlo con un `ALTER TABLE` en su base (el script completo borra y recrea la base `ubikar`).
- Antes de subir: `mvn test`. No subir `target/` ni los contratos generados (ya estan en `.gitignore`).

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
- `portal.html`: portal de autogestion de inquilinos y propietarios (acceso por DNI).
- `contrato.html`: asistente del Responsable para **Registrar Contrato de Alquiler**.

## Caso de uso implementado: Registrar Contrato de Alquiler

Flujo: `contrato.html` (6 pasos) -> `POST /api/contratos` -> `ContratoServicio` (valida, guarda todo en **una transaccion** con SQL2o y genera el Word) -> `GET /api/contratos/{id}/documento`.

- Capas: `controlador` -> `servicio` (`ContratoServicio`) -> `dao` (SQL2o) / `documento` (generacion del .docx).
- Patrones: Singleton (`Sql2oDAO`), DAO (`*DAO`), Strategy (`GeneradorContrato`: cada plantilla de contrato es una estrategia; `GeneradorEstandar` y `GeneradorCompleto` comparten la base `GeneradorPlantilla`).
- Plantillas Word en `src/main/resources/plantillas/`: `contrato_estandar.docx` (modelo corto de UBIKAR) y `contrato_completo.docx` (modelo largo, 20 clausulas). Usan marcadores `{{campo}}` que reemplaza la libreria poi-tl. Los datos y textos derivados (montos en letras, fechas, casillas de garantia, fechas del primer ajuste) se arman en `documento/Marcadores.java`.
- El modelo completo deja renglones `__________` para lo que el sistema no conoce (horario y cuenta bancaria de cobro, marca/color de pintura, porcentajes de penalidad y sellado): se completan a mano en Word.
- Los contratos generados se guardan en `documentos/contratos/` y se registran como `Documentacion` de tipo `CONTRATO`. Un ejemplo de salida esta en `documentos/ejemplo/`.
- Para agregar otro modelo de contrato: nueva plantilla `.docx` + una clase que implemente `GeneradorContrato`, y registrarla en `ContratoServicio`.

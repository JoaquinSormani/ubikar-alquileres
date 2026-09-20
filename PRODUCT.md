# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Spring Boot 3.5 (`@RestController`) + SQL2o + MySQL en el backend; HTML/CSS/JS plano con `fetch()` en el frontend (carpeta `frontend/`), consumiendo la API via CORS (`@CrossOrigin`). Este stack no fue delegado: viene explícitamente definido por el material de la catedra (Prof. Daniel Riesco, Ingenieria de Software II) y ya esta implementado y verificado end-to-end (entidad `Inquilino`).

## Users

- **Responsable de UBIKAR** (usuario principal / administrador): carga contratos, gestiona cobros, liquidaciones y reclamos de mantenimiento. No es una persona tecnica.
- **Inquilino**: consulta su deuda del mes, su historial de recibos y sube comprobantes de pago via un portal de autogestion (acceso por link/DNI).
- **Propietario**: consulta y descarga sus liquidaciones via el mismo portal.
- **Garante**: recibe notificaciones asociadas al contrato que garantiza (rol pasivo, no opera el sistema).

## Product Purpose

UBIKAR es una inmobiliaria real cuyo proceso de alquiler de propiedades se gestiona hoy de forma manual (planillas de Excel, calculos a mano, comunicacion por WhatsApp), generando demoras, errores y sobrecarga operativa. El sistema automatiza ese proceso: alta y seguimiento de contratos, calculo de actualizaciones del alquiler (segun indice ICL/IPC) y de punitorios por mora, cobros y liquidaciones a propietarios (con comision de UBIKAR descontada), notificaciones por WhatsApp, y autogestion remota para inquilinos y propietarios. Exito = que el responsable de UBIKAR deje de depender de Excel y de atender cada consulta manualmente.

## Positioning

A diferencia de una planilla de Excel o de un sistema generico de administracion de propiedades, UBIKAR automatiza especificamente el calculo de actualizaciones por indice (ICL/IPC) y punitorios por mora segun la normativa de alquileres, y ofrece autogestion directa a inquilinos/propietarios sin pasar por el responsable para cada tramite.

## Operating Context

- Cada propiedad pertenece a un propietario y se alquila mediante un contrato que vincula la propiedad con uno o mas inquilinos y, opcionalmente, uno o mas garantes.
- Al iniciar el contrato se registra un inventario del estado de la propiedad (ventanas, puertas, artefactos), para comparar al finalizar y determinar responsabilidad por danos.
- Mensualmente se genera un cobro por contrato (pendiente o pagado); el pago fuera de termino genera un punitorio por mora segun dias de atraso.
- UBIKAR retiene una comision sobre cada cobro y liquida el resto al propietario.
- Durante el contrato pueden surgir reclamos de mantenimiento (con fotos), donde se decide si el costo lo asume el propietario o se descuenta del alquiler del inquilino.
- Documentacion asociada al contrato (contrato firmado, garantia, comprobantes de gas/luz) se almacena por contrato.

## Capabilities and Constraints

- Implementacion en **Java** (requisito de la catedra, no negociable).
- Debe incorporar los patrones de diseno **Singleton** (ya resuelto: `Sql2oDAO`) y **DAO** (ya resuelto: `<Entidad>DAO`), mas un tercer patron a eleccion (aun no decidido).
- Debe aplicar conceptos de seguridad informatica sobre datos sensibles de inquilinos, propietarios y garantes (DNI, contacto, datos de pago).
- Arquitectura en capas obligatoria: `modelo` (dominio), `dao` (persistencia), `controlador` (presentacion/REST).
- Este es un trabajo academico (Proyecto Integrador de Ingenieria de Software II, cursada 2026) con entregas por etapas y fecha limite; no es un producto comercial en curso.
- El Modelo de Dominio (diagrama de clases completo) todavia no esta formalizado como diagrama UML — la primera entidad implementada (`Inquilino`) es un adelanto tecnico, no el modelo final cerrado.

## Evidence on Hand

- Relevamiento real con el responsable de UBIKAR: notas escritas del cliente y transcripciones de 3 audios de entrevista (en `Requerimientos UBIKAR/`).
- Documento de Etapa 1 (relato completo, requerimientos funcionales/no funcionales, vision, modelo de casos de uso) en `../Etapa1_UBIKAR.md`.
- Diagrama de Casos de Uso en `../CasosDeUso_UBIKAR.drawio`.
- No hay identidad visual, paleta ni logo definidos todavia — el frontend actual es solo una prueba tecnica de conexion, sin diseno.

## Product Principles

1. Automatizar primero lo que hoy se calcula a mano y genera errores (actualizaciones por indice, punitorios por mora), no features cosmeticas.
2. El responsable de UBIKAR no es tecnico: la interfaz de administracion debe ser simple, no requiere curva de aprendizaje.
3. Reducir la dependencia del responsable como intermediario: inquilino y propietario deben poder autogestionarse remotamente.
4. Consistencia con el modelado UML de la catedra (BPMN, diagrama de clases, casos de uso) por sobre atajos de implementacion — el codigo debe poder explicarse en una presentacion academica.

## Accessibility & Inclusion

Sin requisito de accesibilidad especifico confirmado todavia (mas alla de RNF01: usabilidad simple para un usuario no tecnico). No asumir WCAG formal sin confirmarlo.

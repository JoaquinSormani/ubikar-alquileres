# Arquitectura de UBIKAR

Monolito en capas, cliente-servidor en tres niveles físicos (navegador, servidor y base de datos). Corresponde al
**Diseño 2 de la clase de arquitectura: organización por responsabilidad (capas)**. Cuando los módulos crezcan se puede
pasar al Diseño 3 (módulos con capas adentro) de a un módulo por vez.

```
navegador (HTML, CSS y JavaScript)
   |  HTTP + JSON
controlador  ->  servicio  ->  dao  ->  MySQL
(API REST)       (reglas)      (SQL)
```

## Mapa de carpetas (Diseño 2: por responsabilidad, en capas)

```
frontend/                         Front · HTML, CSS y JavaScript plano
├── index.html                    entrada: panel del Responsable
├── pages/                        <- pantallas (cada una con su .html, .js y .css)
│   ├── contrato.html   contrato.js
│   ├── propiedad.html  propiedad.js  propiedad.css
│   ├── pago.html       pago.js       pago.css
│   ├── portal.html     portal.js
│   └── inicio.js
├── components/                   <- piezas de interfaz reutilizables
│   └── ux.js
├── services/                     <- llamadas a la API
│   └── api.js                    dirección del backend (API_BASE y API)
├── styles/                       <- estilos compartidos y tokens de la marca
└── assets/                       logos, fondo y video

src/main/java/com/ubikar/         Back · Spring Boot
├── UbikarApplication.java        levanta el servidor
├── config/                       <- parámetros (ReglasDeCobro)
├── controlador/                  <- API REST
├── servicio/                     <- reglas de negocio
├── dao/                          <- acceso a datos (SQL2o)
├── modelo/                       <- entidades
├── dto/                          <- objetos de respuesta
├── documento/                    <- contratos en Word y recibos en PDF
└── excepcion/                    <- manejo de errores
```

Correspondencia con la clase: `pages` = pantallas, `components` = piezas de UI, `services` = llamadas a la API;
`controlador` = routes + controllers, `servicio` = services, `dao` = repositories, `excepcion` = middlewares.

## Carpetas del backend (`src/main/java/com/ubikar`)

| Carpeta | Responsabilidad | Qué **no** hace |
|---|---|---|
| `controlador/` | API REST: recibe el pedido y devuelve la respuesta (JSON, PDF o Word) | No tiene reglas de negocio ni SQL |
| `servicio/` | Reglas de cada caso de uso: validaciones, cálculos, transacciones | No conoce HTTP ni escribe SQL |
| `dao/` | Acceso a datos, un DAO por entidad, con consultas parametrizadas (SQL2o). `Sql2oDAO` es el Singleton de la conexión | No decide nada: solo lee y escribe |
| `modelo/` | Entidades del dominio (contrato, pago, propiedad, personas...) | No accede a la base ni a la API |
| `documento/` | Contratos en Word y recibos en PDF (Strategy y Template Method) | No guarda datos |
| `dto/` | Objetos de transferencia: lo que viaja a la pantalla cuando no coincide con una entidad | No tiene lógica |
| `config/` | Parámetros que se ajustan sin tocar la lógica (`ReglasDeCobro`) | No tiene lógica de casos de uso |
| `excepcion/` | Traducción de errores a respuestas HTTP en un solo lugar | No expone trazas ni detalles internos |

Cada carpeta tiene su `package-info.java` con la misma descripción.

## Carpetas del frontend (`frontend/`)

| Carpeta | Responsabilidad | Qué **no** hace |
|---|---|---|
| `pages/` | Una pantalla por caso de uso: arma la interfaz, maneja la interacción y pide datos a la API | No guarda secretos ni accede a la base; no es la única validación |
| `components/` | Piezas de interfaz que se reutilizan en varias pantallas | No conoce la API |
| `services/` | Habla con el backend: la dirección y las llamadas a la API | No dibuja nada |
| `styles/` | Estilos compartidos y tokens de la marca | No tiene lógica |

Para correr el frontend se abre `frontend/index.html` con un servidor local (por ejemplo Live Server); el resto de las pantallas se alcanza desde ahí.

## Reglas

1. El pedido atraviesa las capas en orden: **controlador -> servicio -> dao**. Una capa solo habla con la de al lado.
2. Una regla de negocio se escribe **una sola vez**, en el servicio (o en `config/` si es un parámetro).
3. El SQL vive solo en los DAO y siempre con parámetros con nombre (`:id`), nunca concatenando texto.
4. Las operaciones que guardan varias cosas van en **una transacción** (todo o nada).
5. Un error de datos inválidos responde 400 con su mensaje; una falla técnica responde 500 con un mensaje genérico.
6. La pantalla nunca accede a la base: solo conoce las URLs y el JSON de la API.

## Cómo agregar un caso de uso

1. Entidad nueva o cambio en `modelo/` y en `bd_ubikar.sql` (más un `bd_migracion_*.sql` para las bases ya creadas).
2. DAO con sus consultas en `dao/`.
3. Servicio con las reglas en `servicio/`.
4. Controlador con las URLs en `controlador/` y, si hace falta, un DTO en `dto/`.
5. Pantalla en `frontend/` y pruebas en `src/test/java`.

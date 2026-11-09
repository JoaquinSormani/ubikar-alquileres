# Guion de la exposición final — UBIKAR

Máximo **20 minutos**. Este guion usa **18:30** y deja **1:30 de margen** para imprevistos de la demo.
Son **17 diapositivas** (las demás están en el apéndice, para responder preguntas).

Se habla a unas 140 palabras por minuto. Cada texto de abajo está escrito para durar lo indicado: **ensayarlo en voz alta con cronómetro**.

---

## 1. Reparto (parejo: 6:10 cada uno)

| Quién | Diapositivas | Diapositivas (tiempo) | Demo | Total |
|---|---|---|---|---|
| **Joaquín** | 1 a 6: portada, problema, Proceso Unificado, dominio, BPMN (2) | 4:30 | 1:40 (pasos 1 y 2) | **6:10** |
| **Ana** | 7 a 11: casos de uso (2), casos de uso importantes, arquitectura, componentes | 4:30 | 1:40 (pasos 3 a 5) | **6:10** |
| **Mati** | 12 a 15 y 17: patrones, secuencia (2), clases, cierre | 4:30 | 1:40 (pasos 6 a 9) | **6:10** |

La demo (diapositiva 16) la cuenta cada uno desde su parte. **Mati maneja el mouse** (es su netbook) y quien habla dice qué se está viendo. Hay que ensayarlo así, para que los pases sean naturales.

Lo que exige la cátedra y dónde está: el CE (2), el modelo de dominio (4), un proceso de negocio (5 y 6), el modelo de CU y los más importantes (7 a 9), los modelos de diseño (10 a 15) y el sistema funcionando (16).

---

## 2. Tiempos por diapositiva

| # | Diapositiva | Quién | Tiempo | Acumulado |
|---|---|---|---|---|
| 1 | Portada | Joaquín | 0:20 | 0:20 |
| 2 | El problema | Joaquín | 1:10 | 1:30 |
| 3 | Proceso Unificado | Joaquín | 0:30 | 2:00 |
| 4 | Modelo de dominio | Joaquín | 1:00 | 3:00 |
| 5 | BPMN 1/2 | Joaquín | 0:50 | 3:50 |
| 6 | BPMN 2/2 | Joaquín | 0:40 | 4:30 |
| 7 | Casos de uso 1/2 | Ana | 0:50 | 5:20 |
| 8 | Casos de uso 2/2 | Ana | 0:40 | 6:00 |
| 9 | Casos de uso más importantes | Ana | 1:00 | 7:00 |
| 10 | Arquitectura en capas | Ana | 1:15 | 8:15 |
| 11 | Componentes | Ana | 0:45 | 9:00 |
| 12 | Patrones | Mati | 1:00 | 10:00 |
| 13 | Secuencia 1/2 | Mati | 0:55 | 10:55 |
| 14 | Secuencia 2/2 | Mati | 0:55 | 11:50 |
| 15 | Clases | Mati | 1:10 | 13:00 |
| 16 | **Demo en vivo** | los tres | 5:00 | 18:00 |
| 17 | Cierre y preguntas | Mati | 0:30 | **18:30** |

**Si vamos largos:** se acorta primero la diapositiva 3 (Proceso Unificado, 30 s) y se resume la 11 (componentes). La demo no se recorta.

---

## 3. Texto sugerido

### Joaquín — bloque 1 (4:30)

**1. Portada (0:20)**
> Buenos días. Somos Joaquín Catalá Sormani, Matías Facchini y Ana Paula Ramírez Juárez, y les presentamos la exposición final de Ingeniería de Software II: **UBIKAR, un sistema de gestión de alquileres**. Vamos a recorrer el caso de estudio, los modelos de análisis y de diseño, y al final lo vamos a mostrar funcionando.

**2. El problema (1:10)**
> UBIKAR es una inmobiliaria que administra alquileres para propietarios e inquilinos, y ese proceso es el de mayor prioridad para el cliente. Hay tres cosas que gestiona. Primero, los **contratos**: unen una propiedad con uno o varios inquilinos y, a veces, con garantes, e incluyen un inventario del estado del inmueble. Segundo, los **cobros**: hay uno por contrato y por mes; si se paga fuera de término hay un punitorio por mora, y se emite un recibo en PDF. Y tercero, las **liquidaciones**: UBIKAR retiene su comisión y le liquida el resto al propietario. Relevamos todo esto con una entrevista virtual con el responsable y con las notas escritas del cliente.

**3. Proceso Unificado (0:30)**
> Trabajamos con el Proceso Unificado, que tiene tres pilares. Está **dirigido por casos de uso**: modelamos 37 e implementamos los más importantes. Está **centrado en la arquitectura**: usamos capas desde el primer caso. Y es **iterativo e incremental**: hicimos una entrega por etapa, el 10/09, el 14/10 y esta última, y cada una creció sobre la anterior.

**4. Modelo de dominio (1:00)**
> Este es el modelo de dominio: los conceptos del problema y cómo se relacionan. Propietario, inquilino y garante **heredan de una clase abstracta Persona**. El **contrato es el eje**: vincula la propiedad, los inquilinos y los garantes, y guarda cosas como el destino, el depósito, el tipo de garantía y el valor del alquiler. Alrededor del contrato están los **pagos**, las **liquidaciones**, el **inventario**, la documentación y los reclamos de mantenimiento. Un pago tiene su estado, y es lo que más vamos a ver en la demo.

**5. BPMN 1/2 (0:50)**
> Para explicar un proceso de negocio elegimos el **alta de un contrato**. Tiene tres carriles: el administrador de UBIKAR, el inquilino y el propietario. Empieza cuando el inquilino acepta la propuesta del inmueble; el administrador registra la reserva, analiza al inquilino y le comenta el perfil al propietario. Acá está la **decisión principal**: si al propietario no le gusta el perfil, el proceso termina. Si le gusta, se piden los datos del inquilino y de los garantes, se verifica el estado del inmueble, se redacta el contrato y se piden las firmas en paralelo.

**6. BPMN 2/2 (0:40)**
> Después se certifican las firmas y se le solicita al inquilino el **primer depósito**, con un plazo pactado en el contrato. La segunda decisión es si el inquilino pagó: si no pagó, se cancela la reserva; si pagó, se reparte la comisión entre UBIKAR y el propietario y se **entregan las llaves**. Este es el proceso que el sistema automatiza cuando se registra un contrato.

> **Pase a Ana:** "Con este proceso claro, Ana les muestra los casos de uso."

---

### Ana — bloque 2 (4:30)

**7. Casos de uso 1/2 (0:50)**
> Gracias, Joaquín. Este es el **modelo de casos de uso**: 37 casos de uso y cinco actores, el **Responsable de UBIKAR**, el inquilino, el propietario, el sistema de notificaciones por WhatsApp y el **tiempo**, para lo que el sistema hace solo. En esta primera mitad están la gestión de propiedades y de contratos. Fíjense en las relaciones: **Registrar contrato** incluye **Asignar propiedad**, que a su vez incluye el ingreso de inquilinos, de garantes y el registro del inventario.

**8. Casos de uso 2/2 (0:40)**
> En la segunda mitad están los cobros. **Registrar pago incluye Emitir recibo** y se extiende con **Calcular punitorio por mora**. **Consultar tablero de cobros** también extiende a Registrar pago. Del lado del actor Tiempo están los casos automáticos: **Calcular actualización según índice**, que ajusta el alquiler por ICL o IPC, **Finalizar contrato** y **Detectar pagos vencidos**, que dispara las notificaciones.

**9. Casos de uso más importantes (1:00)**
> De los 37, estos cuatro son los más importantes para el cliente y son los que **implementamos**. **Registrar propiedad**: alta del propietario y de su propiedad. **Registrar contrato**: con inquilinos, garantes e inventario, y genera el contrato en Word. **Registrar pago**: el cobro del mes, el punitorio por mora y el recibo en PDF; es el que van a ver **en vivo**. Y **Visualizar actualizaciones**: el sistema ajusta solo el alquiler por ICL o IPC, y el responsable lo ve y le avisa al inquilino por WhatsApp.

**10. Arquitectura en capas (1:15)**
> Pasamos al diseño. La arquitectura tiene **cinco capas** y cada una solo habla con la siguiente. El **navegador** usa HTML, CSS y JavaScript y se comunica por HTTP y JSON con el **controlador**, que es una API REST con Spring Boot. El controlador llama al **servicio**, donde están las reglas de cada caso de uso. El servicio accede a los datos por medio de los **DAO**, uno por entidad, con SQL2o. Y abajo está **MySQL**, en Docker. Esto responde al requerimiento no funcional de **mantenibilidad**: se pueden sumar módulos, como venta o tasación, sin cambiar la estructura.

**11. Componentes (0:45)**
> Este es el diagrama de componentes, que es el modelo de implementación. El **círculo** es una interfaz que el componente provee y el **semicírculo** una que requiere. La interfaz web requiere la API REST; los servicios web requieren la lógica de negocio; y esta requiere los **generadores de contratos y de recibos**, los **índices del BCRA** para ICL e IPC, y la persistencia, que a su vez requiere SQL de la base de datos.

> **Pase a Mati:** "Y ahora Mati nos cuenta los patrones y cómo se ve Registrar pago por dentro."

---

### Mati — bloque 3 (4:30)

**12. Patrones (1:00)**
> Usamos **cuatro patrones**, cada uno con un motivo. **DAO**: una clase de acceso a datos por entidad, para aislar el SQL; por ejemplo ContratoDAO y PagoDAO. **Singleton**: una única conexión a la base para todo el sistema, que mantiene Sql2oDAO. **Strategy**: elegimos cómo generar el contrato, estándar o completo, y el recibo, sin tocar el servicio; agregar un formato nuevo es agregar una clase. Y **Template Method**: los documentos de Word comparten los pasos comunes y cada modelo completa lo propio.

**13. Secuencia 1/2 (0:55)**
> Este es el diagrama de secuencia de **Registrar pago**. En esta primera mitad el formulario pide los cobros al controlador, que le pasa el pedido al servicio. El servicio llama al DAO, y acá se ve el **Singleton**: `getInstance()` crea la instancia de la base de datos y la conexión solo si todavía no existen. Después, por cada cobro pendiente, el servicio calcula la mora con la clase **Punitorio**.

**14. Secuencia 2/2 (0:55)**
> En la segunda mitad el responsable confirma el pago. El servicio **calcula el punitorio**, actualiza el cobro a través del DAO y, recién después, **pide el recibo**. El recibo lo genera la clase **ReciboPdf**, que es la estrategia elegida. Si algo falla antes de terminar, no se modifica ningún dato del sistema.

**15. Clases (1:10)**
> Y este es el diagrama de clases de diseño de Registrar pago, con las capas que vimos. El **formulario** se comunica con el **controlador**, que delega en **PagoServicio**. El servicio usa la clase **Punitorio** para la mora y la interfaz **GeneradorRecibo**: esa interfaz es el **Strategy**, y `ReciboPdf` es la implementación que usamos hoy. El acceso a los datos va por **PagoDAO**, que implementa el CRUD completo, y todos los DAO obtienen la conexión de **Sql2oDAO**, el Singleton. Con esto terminamos el diseño y pasamos a **mostrarlo funcionando**.

> **Pase a la demo:** "Joaquín arranca la demostración."

---

### Demo en vivo — los tres (5:00)

Tener el backend, la base y el navegador ya iniciados (ver `docs/Guia_Demo_En_Vivo.md`). Los datos de la demo ya están cargados.

**Joaquín (1:40): pasos 1 y 2**
> Vamos a ver **Registrar pago**. Entro desde el inicio. Acá están las dos tablas: los **pagos pendientes**, de a cinco por página y con los más vencidos primero, y los **pagos realizados**, de a diez. Puedo **filtrar por mes** y **buscar por DNI, nombre o propiedad**, y la búsqueda se hace en el servidor, por eso es rápida aunque haya muchos pagos. Voy a elegir un cobro **vencido** de octubre.

**Ana (1:40): pasos 3 a 5**
> Al elegirlo se abre la ventana de **cobro**. Ingreso la fecha de pago, que es hoy, y la forma de pago, por ejemplo transferencia. El sistema calcula solo la mora: muestra el **alquiler**, el **punitorio** a razón de 1 % diario por los días de atraso, y el **total a cobrar**. Si cambio la fecha a una **anterior al vencimiento**, el punitorio baja a cero y el total se recalcula. La dejo en hoy y **registro el pago**.

**Mati (1:40): pasos 6 a 9**
> Aparece **Pago registrado** con el detalle. Abro el **recibo en PDF**, con su número correlativo. Cierro, y el cobro **pasó a realizados**, arriba de todo, y bajó el contador de pendientes. Para terminar, entro a **Visualizar actualizaciones**: acá se ven las actualizaciones por ICL o IPC que el sistema **aplicó solo** y las que vienen, también paginadas, y desde acá el responsable le avisa al inquilino por WhatsApp. El cobro siguiente ya sale con el valor actualizado.

**Si algo falla:** abrir la pestaña de reserva con *Pagos realizados* y mostrar un recibo ya emitido; contarlo con naturalidad, sin disculparse.

---

### Mati — cierre (0:30)

**17. Cierre**
> Para cerrar: modelamos el problema con el Proceso Unificado, diseñamos en capas con cuatro patrones, y implementamos los casos de uso más importantes, que acaban de ver funcionando. Quedan para próximas iteraciones el tablero propio, el portal de autogestión y la liquidación al propietario. Muchas gracias. **¿Preguntas?**

---

## 4. Preguntas probables (responde quien presentó el tema)

El material de respaldo está en el **apéndice** del deck.

| Tema | Responde | Dónde está |
|---|---|---|
| Qué cubre el alcance y qué no | Joaquín | Apéndice: *Alcance del sistema* |
| Cómo se relaciona el Proceso Unificado con lo que hicimos | Joaquín | Apéndice: *Workflows y modelos* |
| Por qué cada capa hace lo que hace | Ana | Apéndice: *Qué hace y qué no hace cada capa* y *Un pedido de punta a punta* |
| Especificación de Registrar pago (flujos alternativos) | Ana | Apéndice: *Registrar pago, en una hoja* |
| Cómo se calcula el ICL y el IPC | Mati | Apéndice: *ICL e IPC* |
| Seguridad y copias de seguridad | Mati | Apéndice: *Qué hicimos y qué falta* y *Copias de seguridad* |
| Con qué tecnologías se construyó | Cualquiera | Apéndice: *Con qué se construyó* |

---

## 5. Antes de entrar (lista rápida)

- [ ] Informe en PDF **impreso** y enviado a la cátedra **durante la mañana** del día de la presentación.
- [ ] Los tres presentes (la asistencia de todos es obligatoria).
- [ ] Backend corriendo y base con los datos de la demo cargados (sin ensayos hechos encima).
- [ ] Frontend abierto en `http://localhost:5500/index.html`, con la sesión iniciada.
- [ ] Internet o hotspot.
- [ ] Deck abierto en pantalla completa y apéndice a mano.
- [ ] Una pestaña con *Pagos realizados* como plan B.
- [ ] Ensayo completo cronometrado, con los pases de cada bloque.

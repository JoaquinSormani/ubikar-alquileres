# Guion de la exposición final — UBIKAR

Máximo **20 minutos**. Este guion usa **17:40** y deja **2:20 de margen** para imprevistos de la demo.
Son **20 diapositivas**, dos de ellas de **demostración en vivo** (Registrar contrato y Registrar pago). De los modelos de diseño mostramos los de los dos casos de uso más importantes: **Registrar contrato** y **Registrar pago**. Los diagramas de los demás casos (propiedad, inventario, tablero y actualización) y el material que no es obligatorio (alcance, Proceso Unificado, especificación, reglas de capas, ICL e IPC, seguridad, backup, tecnologías) están en el **apéndice**, para responder preguntas. Todos los diagramas están, además, en el informe.

Se habla a unas 140 palabras por minuto. Cada texto está escrito para durar lo indicado: **ensayarlo en voz alta con cronómetro**. En las diapositivas de diagramas no hay que leer todo: se señala lo importante y se pasa.

---

## 1. Reparto (parejo: unos 6:00 cada uno)

| Quién | Diapositivas | Tiempo de diapositivas | Demo contrato (18) | Demo pago (19) | Total |
|---|---|---|---|---|---|
| **Ana** | 1 a 8: portada, problema, dominio, BPMN (2), casos de uso (2), casos más importantes | 4:30 | 0:50 (pasos 4 y 5) | 0:40 (pasos 1 y 2) | **6:00** |
| **Joaquín** | 9 a 14: arquitectura, componentes, patrones y el diseño de Registrar contrato (3) | 4:10 | 1:00 (pasos 1 a 3) | 0:50 (pasos 3 a 5) | **6:00** |
| **Mati** | 15 a 17: el diseño de Registrar pago (3); y el cierre (20) | 3:00 | 0:40 (pasos 6 y 7) | 2:00 (pasos 6 a 9) | **5:40** |

Las dos demos las cuenta cada uno desde su parte. **Mati maneja el mouse** (es su netbook) y quien habla dice qué se está viendo. Mati cierra el diseño con Registrar pago y le pasa la palabra a Joaquín, que abre la demo de Registrar contrato; Ana abre la de Registrar pago.

Lo que exige la cátedra y dónde está: el CE (2), el modelo de dominio (3), un proceso de negocio (4 y 5), el modelo de CU y los más importantes (6 a 8), **los modelos de diseño de los casos más importantes (9 a 17)** y el sistema funcionando (18 y 19).

---

## 2. Tiempos por diapositiva

| # | Diapositiva | Quién | Tiempo | Acumulado |
|---|---|---|---|---|
| 1 | Portada | Ana | 0:15 | 0:15 |
| 2 | El problema | Ana | 0:50 | 1:05 |
| 3 | Modelo de dominio | Ana | 0:45 | 1:50 |
| 4 | BPMN 1/2 | Ana | 0:40 | 2:30 |
| 5 | BPMN 2/2 | Ana | 0:35 | 3:05 |
| 6 | Casos de uso 1/2 | Ana | 0:25 | 3:30 |
| 7 | Casos de uso 2/2 | Ana | 0:25 | 3:55 |
| 8 | Casos de uso más importantes | Ana | 0:35 | 4:30 |
| 9 | Arquitectura en capas | Joaquín | 0:50 | 5:20 |
| 10 | Componentes | Joaquín | 0:30 | 5:50 |
| 11 | Patrones | Joaquín | 0:40 | 6:30 |
| 12 | Registrar contrato: secuencia 1/2 | Joaquín | 0:45 | 7:15 |
| 13 | Registrar contrato: secuencia 2/2 | Joaquín | 0:40 | 7:55 |
| 14 | Registrar contrato: clases | Joaquín | 0:45 | 8:40 |
| 15 | Registrar pago: secuencia 1/2 | Mati | 0:55 | 9:35 |
| 16 | Registrar pago: secuencia 2/2 | Mati | 0:50 | 10:25 |
| 17 | Registrar pago: clases | Mati | 0:55 | 11:20 |
| 18 | **Demo en vivo (1/2): Registrar contrato** | Joaquín, Ana y Mati | 2:30 | 13:50 |
| 19 | **Demo en vivo (2/2): Registrar pago** | Ana, Joaquín y Mati | 3:30 | 17:20 |
| 20 | Cierre y preguntas | Mati | 0:20 | **17:40** |

**Si vamos largos:** se resume la diapositiva 10 (componentes) y se acortan las diapositivas de BPMN. Las demos no se recortan; si hace falta, en Registrar contrato se muestra el inventario sin tocarlo.

---

## 3. Texto sugerido

### Ana — diapositivas 1 a 8 (4:30)

**1. Portada (0:15)**
> Buenos días. Somos Joaquín Catalá Sormani, Matías Facchini y Ana Paula Ramírez Juárez. Les presentamos **UBIKAR, un sistema de gestión de alquileres**: el caso de estudio, los modelos de análisis y de diseño, y al final lo mostramos funcionando.

**2. El problema (0:50)**
> UBIKAR es una inmobiliaria que administra alquileres para propietarios e inquilinos, y es el proceso de mayor prioridad para el cliente. Gestiona tres cosas. Los **contratos**, que unen una propiedad con uno o varios inquilinos y a veces con garantes, e incluyen un inventario del inmueble. Los **cobros**: uno por contrato y por mes, con punitorio por mora si se paga fuera de término y un recibo en PDF. Y las **liquidaciones**: UBIKAR retiene su comisión y le liquida el resto al propietario.

**3. Modelo de dominio (0:45)**
> Este es el modelo de dominio. Propietario, inquilino y garante **heredan de una clase abstracta Persona**. El **contrato es el eje**: vincula la propiedad, los inquilinos y los garantes, y guarda el destino, el depósito, el tipo de garantía y el valor del alquiler. Alrededor están los **pagos**, con su estado, las **liquidaciones**, el **inventario**, la documentación y los reclamos de mantenimiento.

**4. BPMN 1/2 (0:40)**
> Para explicar un proceso de negocio elegimos el **alta de un contrato**, con tres carriles: el administrador de UBIKAR, el inquilino y el propietario. El administrador registra la reserva, analiza al inquilino y le comenta el perfil al propietario. **Si al propietario no le gusta, el proceso termina**; si le gusta, se piden los datos del inquilino y de los garantes, se verifica el estado del inmueble, se redacta el contrato y se piden las firmas en paralelo.

**5. BPMN 2/2 (0:35)**
> Después se certifican las firmas y se le pide al inquilino el **primer depósito**, con un plazo pactado. Si no paga, se cancela la reserva; si paga, se reparte la comisión entre UBIKAR y el propietario y se **entregan las llaves**. Este es el proceso que el sistema automatiza cuando se registra un contrato.

**6. Casos de uso 1/2 (0:25)**
> Este es el **modelo de casos de uso**: 37 casos y cinco actores, el Responsable de UBIKAR, el inquilino, el propietario, las notificaciones por WhatsApp y el **tiempo**, para lo que el sistema hace solo. Acá están propiedades y contratos: **Registrar contrato incluye Asignar propiedad**, que incluye inquilinos, garantes e inventario.

**7. Casos de uso 2/2 (0:25)**
> En la segunda mitad están los cobros. **Registrar pago incluye Emitir recibo** y se extiende con **Calcular punitorio por mora**; **Consultar tablero** también extiende a Registrar pago. Del lado del Tiempo están los casos automáticos: **Calcular actualización según índice**, Finalizar contrato y Detectar pagos vencidos.

**8. Casos de uso más importantes (0:35)**
> De los 37, estos cuatro son los más importantes y son los que **implementamos**: **Registrar propiedad**, **Registrar contrato**, **Registrar pago** y **Visualizar actualizaciones**, donde el sistema ajusta solo el alquiler por ICL o IPC. De los cuatro, mostramos el diseño de los dos centrales, **Registrar contrato y Registrar pago**, y esos dos son los que van a ver funcionando en vivo.

> **Pase a Joaquín:** "Con los casos de uso claros, Joaquín les muestra el diseño."

---

### Joaquín — diapositivas 9 a 14 (4:10)

**9. Arquitectura en capas (0:50)**
> La arquitectura tiene **cinco capas** y cada una solo habla con la siguiente. El **navegador** se comunica por HTTP y JSON con el **controlador**, una API REST con Spring Boot. El controlador llama al **servicio**, donde están las reglas de cada caso de uso, y este accede a los datos por medio de los **DAO**, uno por entidad, con SQL2o. Abajo está **MySQL**, en Docker. Responde al requerimiento de **mantenibilidad**: se pueden sumar módulos, como venta o tasación, sin cambiar la estructura.

**10. Componentes (0:30)**
> El diagrama de componentes es el modelo de implementación. El **círculo** es una interfaz que el componente provee y el **semicírculo** una que requiere. La interfaz web requiere la API REST, los servicios web requieren la lógica de negocio, y esta requiere los **generadores de contratos y recibos**, los **índices del BCRA** y la persistencia, que requiere SQL de la base.

**11. Patrones (0:40)**
> Usamos **cuatro patrones**. **DAO**: una clase de acceso a datos por entidad, para aislar el SQL. **Singleton**: una única conexión a la base, que mantiene Sql2oDAO. **Strategy**: elegimos cómo generar el contrato o el recibo sin tocar el servicio. Y **Template Method**: los documentos de Word comparten los pasos comunes y cada modelo completa lo propio. Los vamos a ver en los diagramas de Registrar contrato y de Registrar pago.

**12. Registrar contrato: secuencia 1/2 (0:45)**
> Empezamos por **Registrar contrato**, el caso más largo. El responsable hace clic en nuevo contrato, se crea el formulario, ingresa el DNI del propietario y el sistema busca sus propiedades; después elige la propiedad. Siguen **dos bucles**, uno por cada inquilino y otro por cada garante si la garantía lo requiere. Cada búsqueda va a ContratoDAO, que obtiene la conexión con el **Singleton**: `getInstance()` crea la instancia y la conexión solo si todavía no existen.

**13. Registrar contrato: secuencia 2/2 (0:40)**
> En la segunda mitad el responsable completa las condiciones y el inventario; el formulario calcula la fecha de fin y muestra la revisión. Con **Generar contrato** se crea ContratoServicio, que genera los documentos y le pide al DAO que registre el contrato, el inventario y la documentación. El DAO devuelve el idContrato y el formulario muestra la confirmación.

**14. Registrar contrato: clases (0:45)**
> En el diagrama de clases, el formulario, hecho en HTML y JavaScript, llama a **ContratoControlador**, que delega en **ContratoServicio**. El servicio valida y registra, y usa **ContratoDAO**, que además de las operaciones básicas sabe verificar si hay contratos superpuestos y vincular inquilinos y garantes. Para generar el documento aplica el **Strategy**: la interfaz GeneradorContrato tiene dos implementaciones, GeneradorEstandar y GeneradorCompleto. La conexión viene de Sql2oDAO, el Singleton.

> **Pase a Mati:** "Mati sigue con el otro caso central: Registrar pago."

---

### Mati — diapositivas 15 a 17 (2:40)

**15. Registrar pago: secuencia 1/2 (0:55)**
> **Registrar pago** es el otro caso central. En la primera mitad, el formulario pide los cobros y el servicio llama a PagoDAO. El DAO obtiene la conexión con el **Singleton**: `getInstance()` crea la instancia de la base y la conexión solo si todavía no existen, así que todo el sistema comparte una única conexión. Con los cobros pendientes en mano, el servicio calcula la mora de cada uno con la clase **Punitorio**, que aplica un 1 % diario por los días de atraso.

**16. Registrar pago: secuencia 2/2 (0:50)**
> En la segunda mitad, el responsable elige un cobro y lo confirma. El servicio **calcula el punitorio** a la fecha de pago, actualiza el cobro a través del DAO y recién después **pide el recibo**. El recibo lo genera ReciboPdf, la estrategia elegida, con su número correlativo. Si algo falla antes de terminar, no se modifica ningún dato del sistema.

**17. Registrar pago: clases (0:55)**
> Y las clases, con el mismo esquema que Registrar contrato. El formulario llama a **PagoControlador**, que delega en **PagoServicio**. El servicio usa la clase **Punitorio** para la mora y la interfaz **GeneradorRecibo**, que es el **Strategy**: ReciboPdf es la implementación de hoy, y agregar otro formato es agregar una clase, sin tocar el servicio. El acceso a datos es **PagoDAO**, con el CRUD completo, y la conexión viene de Sql2oDAO, el Singleton. Con esto terminamos el diseño: pasamos a **mostrar los dos casos funcionando**.

> **Pase a la demo:** "Joaquín arranca la demostración con Registrar contrato."

---

### Demo en vivo (1/2): Registrar contrato — los tres (2:30), diapositiva 18

Tener el backend, la base y el navegador ya iniciados (ver `docs/Guia_Demo_En_Vivo.md`, sección 3.1). Los datos de la demo ya están cargados. Los pasos están numerados como en la guía.

**Joaquín (1:00): pasos 1 a 3**
> Vamos a ver **Registrar contrato**, el proceso de negocio que mostramos en el BPMN. En el inicio entro a **Nuevo contrato de alquiler**. Primero busco al **propietario por DNI**, Héctor Dominguez, y el sistema me muestra sus propiedades: elijo **Ayacucho 330**. Después el **inquilino**, también por DNI: como Florencia Luna ya está registrada, el sistema **completa sus datos solo**. Y la **garantía**: elijo fiador solidario y cargo al garante, Raúl Ponce.

**Ana (0:50): pasos 4 y 5**
> En las **condiciones** defino el destino, la fecha de inicio, la duración, el alquiler, cada cuántos meses se actualiza y el **índice, ICL o IPC**, y elijo el modelo de contrato. La fecha de fin, los montos en letras y las fechas de la firma los **calcula el sistema**. Siguiente, el **inventario**: ya llega armado con las características de la propiedad, y se anexa al contrato.

**Mati (0:40): pasos 6 y 7**
> En la **revisión** controlamos todo y damos **Generar contrato**: se guardan el contrato, el inventario y la documentación juntos, y se crea el contrato en **Word**. Lo descargo y lo abro: está listo para imprimir y firmar.

> **Pase:** "Ahora vemos **Registrar pago**." (Ana)

---

### Demo en vivo (2/2): Registrar pago — los tres (3:30), diapositiva 19

Sección 3.2 de la guía.

**Ana (0:40): pasos 1 y 2**
> Ahora **Registrar pago**. Acá están las dos tablas: los **pagos pendientes**, de a cinco y con los más vencidos primero, y los **pagos realizados**, de a diez. Puedo **filtrar por mes** y **buscar por DNI, nombre o propiedad**, y la búsqueda se hace en el servidor. Elijo un cobro **vencido** de octubre.

**Joaquín (0:50): pasos 3 a 5**
> Se abre la ventana de **cobro**. Ingreso la fecha y la forma de pago, por ejemplo transferencia. El sistema calcula solo la mora: el **alquiler**, el **punitorio** de 1 % diario por los días de atraso y el **total a cobrar**. Si pongo una fecha **anterior al vencimiento**, el punitorio baja a cero. La dejo en hoy y **registro el pago**.

**Mati (2:00): pasos 6 a 9**
> Aparece **Pago registrado** con el detalle. Abro el **recibo en PDF**, con su número correlativo, y lo cierro. Vuelvo a la tabla: el cobro **pasó a realizados**, arriba de todo, y bajó el contador de pendientes. Ahora entro a **Visualizar actualizaciones**: acá están las actualizaciones por ICL o IPC que el sistema **aplicó solo**, y las que vienen, paginadas igual que los pagos, y desde acá se le avisa al inquilino por WhatsApp. El cobro siguiente ya sale con el valor actualizado.

**Si algo falla:** en Registrar contrato, mostrar la pantalla *Contrato registrado* y seguir; en Registrar pago, abrir la pestaña de reserva con *Pagos realizados* y mostrar un recibo ya emitido. Contarlo con naturalidad, sin disculparse.

---

### Mati — cierre (0:20), diapositiva 20

> Para cerrar: modelamos el problema, diseñamos en capas con cuatro patrones, y implementamos los casos de uso más importantes, que acaban de ver funcionando. Quedan para próximas iteraciones el tablero propio, el portal de autogestión y la liquidación al propietario. Muchas gracias. **¿Preguntas?**

---

## 4. Preguntas probables (responde quien presentó el tema)

El material de respaldo está en el **apéndice** del deck.

| Tema | Responde | Dónde está |
|---|---|---|
| Qué cubre el alcance y qué no | Ana | Apéndice: *Alcance del sistema* |
| Cómo se relaciona el Proceso Unificado con lo que hicimos | Ana | Apéndice: *Los tres pilares* y *Workflows y modelos* |
| Por qué cada capa hace lo que hace | Joaquín | Apéndice: *Qué hace y qué no hace cada capa* y *Un pedido de punta a punta* |
| Diseño de Registrar propiedad y de Registrar inventario | Joaquín | Apéndice: diagramas de propiedad e inventario |
| Seguridad y copias de seguridad | Joaquín | Apéndice: *Qué hicimos y qué falta* y *Copias de seguridad* |
| Especificación de Registrar pago (flujos alternativos) | Mati | Apéndice: *Registrar pago, en una hoja* |
| Diseño de Consultar tablero | Mati | Apéndice: diagramas del tablero |
| Cómo se calcula el ICL y el IPC, y su diseño | Mati | Apéndice: *ICL e IPC* y diagramas de actualización |
| Con qué tecnologías se construyó | Cualquiera | Apéndice: *Con qué se construyó* |

---

## 5. Antes de entrar (lista rápida)

- [ ] Informe en PDF **impreso** y enviado a la cátedra **durante la mañana** del día de la presentación.
- [ ] Los tres presentes (la asistencia de todos es obligatoria).
- [ ] Backend corriendo y base con los datos de la demo cargados (sin ensayos hechos encima): Ayacucho 330 libre y 8 pagos pendientes.
- [ ] Frontend abierto en `http://localhost:5500/index.html`, con la sesión iniciada.
- [ ] Internet o hotspot.
- [ ] Deck abierto en pantalla completa y apéndice a mano.
- [ ] Una pestaña con *Pagos realizados* como plan B.
- [ ] Ensayo completo cronometrado, con los pases de cada bloque.

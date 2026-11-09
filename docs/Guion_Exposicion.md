# Guion de la exposición final — UBIKAR

Máximo **20 minutos**. Este guion usa **17:15** y deja **2:45 de margen** para imprevistos de la demo.
Son **27 diapositivas**: la consigna pide mostrar **todos los modelos de diseño** (clases y secuencia de los seis casos de uso), así que cada diagrama tiene su diapositiva. Lo que no es obligatorio (alcance, Proceso Unificado, especificación, reglas de capas, ICL e IPC, seguridad, backup, tecnologías) está en el **apéndice**, para responder preguntas.

Se habla a unas 140 palabras por minuto. Cada texto está escrito para durar lo indicado: **ensayarlo en voz alta con cronómetro**. En las diapositivas de diagramas no hay que leer todo: se señala lo importante y se pasa.

---

## 1. Reparto (parejo: unos 5:40 cada uno)

| Quién | Diapositivas | Tiempo de diapositivas | Demo | Total |
|---|---|---|---|---|
| **Joaquín** | 1 a 8: portada, problema, dominio, BPMN (2), casos de uso (2), casos más importantes | 4:30 | 1:30 (pasos 1 y 2) | **6:00** |
| **Ana** | 9 a 16: arquitectura, componentes, patrones, diseño de Registrar contrato (3) y de Registrar propiedad (2) | 4:05 | 1:30 (pasos 3 a 5) | **5:35** |
| **Mati** | 17 a 25: diseño de Registrar inventario (2), Registrar pago (3), Consultar tablero (2) y Calcular actualización (2); y el cierre (27) | 4:10 | 1:30 (pasos 6 a 9) | **5:40** |

La demo (diapositiva 26) la cuenta cada uno desde su parte. **Mati maneja el mouse** (es su netbook) y quien habla dice qué se está viendo. Hay que ensayarlo así, para que los pases sean naturales.

Lo que exige la cátedra y dónde está: el CE (2), el modelo de dominio (3), un proceso de negocio (4 y 5), el modelo de CU y los más importantes (6 a 8), **los modelos de diseño (9 a 25)** y el sistema funcionando (26).

---

## 2. Tiempos por diapositiva

| # | Diapositiva | Quién | Tiempo | Acumulado |
|---|---|---|---|---|
| 1 | Portada | Joaquín | 0:15 | 0:15 |
| 2 | El problema | Joaquín | 0:50 | 1:05 |
| 3 | Modelo de dominio | Joaquín | 0:50 | 1:55 |
| 4 | BPMN 1/2 | Joaquín | 0:35 | 2:30 |
| 5 | BPMN 2/2 | Joaquín | 0:30 | 3:00 |
| 6 | Casos de uso 1/2 | Joaquín | 0:25 | 3:25 |
| 7 | Casos de uso 2/2 | Joaquín | 0:25 | 3:50 |
| 8 | Casos de uso más importantes | Joaquín | 0:40 | 4:30 |
| 9 | Arquitectura en capas | Ana | 0:45 | 5:15 |
| 10 | Componentes | Ana | 0:30 | 5:45 |
| 11 | Patrones | Ana | 0:40 | 6:25 |
| 12 | Registrar contrato: secuencia 1/2 | Ana | 0:30 | 6:55 |
| 13 | Registrar contrato: secuencia 2/2 | Ana | 0:25 | 7:20 |
| 14 | Registrar contrato: clases | Ana | 0:30 | 7:50 |
| 15 | Registrar propiedad: secuencia | Ana | 0:25 | 8:15 |
| 16 | Registrar propiedad: clases | Ana | 0:20 | 8:35 |
| 17 | Registrar inventario: secuencia | Mati | 0:25 | 9:00 |
| 18 | Registrar inventario: clases | Mati | 0:20 | 9:20 |
| 19 | Registrar pago: secuencia 1/2 | Mati | 0:30 | 9:50 |
| 20 | Registrar pago: secuencia 2/2 | Mati | 0:25 | 10:15 |
| 21 | Registrar pago: clases | Mati | 0:30 | 10:45 |
| 22 | Consultar tablero: secuencia | Mati | 0:25 | 11:10 |
| 23 | Consultar tablero: clases | Mati | 0:20 | 11:30 |
| 24 | Calcular actualización: secuencia | Mati | 0:30 | 12:00 |
| 25 | Calcular actualización: clases | Mati | 0:25 | 12:25 |
| 26 | **Demo en vivo** | los tres | 4:30 | 16:55 |
| 27 | Cierre y preguntas | Mati | 0:20 | **17:15** |

**Si vamos largos:** se resume la diapositiva 10 (componentes) y se acortan los diagramas de clases de Propiedad e Inventario, que son los más simples. La demo no se recorta.

---

## 3. Texto sugerido

### Joaquín — diapositivas 1 a 8 (4:30)

**1. Portada (0:15)**
> Buenos días. Somos Joaquín Catalá Sormani, Matías Facchini y Ana Paula Ramírez Juárez. Les presentamos **UBIKAR, un sistema de gestión de alquileres**: el caso de estudio, los modelos de análisis y de diseño, y al final lo mostramos funcionando.

**2. El problema (0:50)**
> UBIKAR es una inmobiliaria que administra alquileres para propietarios e inquilinos, y es el proceso de mayor prioridad para el cliente. Gestiona tres cosas. Los **contratos**, que unen una propiedad con uno o varios inquilinos y a veces con garantes, e incluyen un inventario del inmueble. Los **cobros**: uno por contrato y por mes, con punitorio por mora si se paga fuera de término y un recibo en PDF. Y las **liquidaciones**: UBIKAR retiene su comisión y le liquida el resto al propietario.

**3. Modelo de dominio (0:50)**
> Este es el modelo de dominio. Propietario, inquilino y garante **heredan de una clase abstracta Persona**. El **contrato es el eje**: vincula la propiedad, los inquilinos y los garantes, y guarda el destino, el depósito, el tipo de garantía y el valor del alquiler. Alrededor están los **pagos**, con su estado, las **liquidaciones**, el **inventario**, la documentación y los reclamos de mantenimiento.

**4. BPMN 1/2 (0:35)**
> Para explicar un proceso de negocio elegimos el **alta de un contrato**, con tres carriles: el administrador de UBIKAR, el inquilino y el propietario. El administrador registra la reserva, analiza al inquilino y le comenta el perfil al propietario. **Si al propietario no le gusta, el proceso termina**; si le gusta, se piden los datos del inquilino y de los garantes, se redacta el contrato y se piden las firmas en paralelo.

**5. BPMN 2/2 (0:30)**
> Después se certifican las firmas y se le pide al inquilino el **primer depósito**, con un plazo pactado. Si no paga, se cancela la reserva; si paga, se reparte la comisión entre UBIKAR y el propietario y se **entregan las llaves**. Este es el proceso que el sistema automatiza al registrar un contrato.

**6. Casos de uso 1/2 (0:25)**
> Este es el **modelo de casos de uso**: 37 casos y cinco actores, el Responsable de UBIKAR, el inquilino, el propietario, las notificaciones por WhatsApp y el **tiempo**, para lo que el sistema hace solo. Acá están propiedades y contratos: **Registrar contrato incluye Asignar propiedad**, que incluye inquilinos, garantes e inventario.

**7. Casos de uso 2/2 (0:25)**
> En la segunda mitad están los cobros. **Registrar pago incluye Emitir recibo** y se extiende con **Calcular punitorio por mora**; **Consultar tablero** también extiende a Registrar pago. Del lado del Tiempo están los casos automáticos: **Calcular actualización según índice**, Finalizar contrato y Detectar pagos vencidos.

**8. Casos de uso más importantes (0:40)**
> De los 37, estos cuatro son los más importantes y son los que **implementamos**. **Registrar propiedad**, **Registrar contrato**, con inquilinos, garantes e inventario, que genera el contrato en Word. **Registrar pago**: el cobro, el punitorio y el recibo en PDF; ese lo van a ver en vivo. Y **Visualizar actualizaciones**: el sistema ajusta solo el alquiler por ICL o IPC y el responsable lo ve y avisa al inquilino por WhatsApp.

> **Pase a Ana:** "Con los casos de uso claros, Ana les muestra el diseño."

---

### Ana — diapositivas 9 a 16 (4:05)

**9. Arquitectura en capas (0:45)**
> La arquitectura tiene **cinco capas** y cada una solo habla con la siguiente. El **navegador** se comunica por HTTP y JSON con el **controlador**, una API REST con Spring Boot. El controlador llama al **servicio**, donde están las reglas de cada caso de uso, y este accede a los datos por medio de los **DAO**, uno por entidad, con SQL2o. Abajo está **MySQL**, en Docker. Responde al requerimiento de **mantenibilidad**: se pueden sumar módulos sin cambiar la estructura.

**10. Componentes (0:30)**
> El diagrama de componentes es el modelo de implementación. El **círculo** es una interfaz que el componente provee y el **semicírculo** una que requiere. La interfaz web requiere la API REST, los servicios web requieren la lógica de negocio, y esta requiere los **generadores de contratos y recibos**, los **índices del BCRA** y la persistencia, que requiere SQL de la base.

**11. Patrones (0:40)**
> Usamos **cuatro patrones**. **DAO**: una clase de acceso a datos por entidad, para aislar el SQL. **Singleton**: una única conexión a la base, que mantiene Sql2oDAO. **Strategy**: elegimos cómo generar el contrato o el recibo sin tocar el servicio. Y **Template Method**: los documentos de Word comparten los pasos comunes y cada modelo completa lo propio. Los vamos a ver en cada diagrama.

**12. Registrar contrato: secuencia 1/2 (0:30)**
> Empezamos por **Registrar contrato**, el caso más largo. El responsable busca al propietario por DNI y elige su propiedad; después hay **dos bucles**, uno por cada inquilino y otro por cada garante si la garantía lo requiere. Cada búsqueda va a ContratoDAO, que obtiene la conexión con el **Singleton**: `getInstance()` crea la instancia y la conexión solo si todavía no existen.

**13. Registrar contrato: secuencia 2/2 (0:25)**
> En la segunda mitad completa las condiciones y el inventario y hace clic en **Generar contrato**. Se crea ContratoServicio, que genera los documentos y le pide al DAO que registre el contrato, el inventario y la documentación. El DAO devuelve el idContrato y el formulario muestra la confirmación.

**14. Registrar contrato: clases (0:30)**
> En el diagrama de clases, el formulario llama a ContratoControlador, que delega en **ContratoServicio**. El servicio usa **ContratoDAO**, y para generar el documento aplica el **Strategy**: la interfaz GeneradorContrato tiene dos implementaciones, GeneradorEstandar y GeneradorCompleto. La conexión viene de Sql2oDAO, el Singleton.

**15. Registrar propiedad: secuencia (0:25)**
> **Registrar propiedad**. El responsable ingresa la dirección, hace clic en Siguiente, ingresa los datos del propietario y registra. Se crea PropiedadServicio, que **valida** y llama a PropiedadDAO; el DAO obtiene la conexión con el Singleton e inserta primero el propietario y después la propiedad. Devuelve el idPropiedad y se muestra la confirmación.

**16. Registrar propiedad: clases (0:20)**
> En clases: el formulario por pasos, PropiedadControlador, PropiedadServicio con `validar` y `registrar`, PropiedadDAO con el CRUD, y otra vez el Singleton Sql2oDAO.

> **Pase a Mati:** "Mati sigue con el inventario, los pagos, el tablero y la actualización."

---

### Mati — diapositivas 17 a 25 (3:50) y cierre (0:20)

**17. Registrar inventario: secuencia (0:25)**
> **Registrar inventario** es parte del asistente de contrato. El responsable ajusta los ambientes y los elementos que se muestran y hace clic en Siguiente. Se crea InventarioServicios, que usa InventarioDAO; el DAO obtiene la conexión con el Singleton e inserta en la tabla de inventario.

**18. Registrar inventario: clases (0:20)**
> En clases, ContratoServicio usa dos DAO: **InventarioDAO** para el inventario y **RenglonInventarioDAO** para cada renglón. Los dos toman la conexión de Sql2oDAO.

**19. Registrar pago: secuencia 1/2 (0:30)**
> **Registrar pago**. El formulario pide los cobros y el servicio llama a PagoDAO, que obtiene la conexión con el Singleton. Por cada cobro pendiente, el servicio calcula la mora con la clase **Punitorio**.

**20. Registrar pago: secuencia 2/2 (0:25)**
> En la segunda mitad, el responsable confirma. El servicio **calcula el punitorio**, actualiza el cobro a través del DAO y recién después **pide el recibo**, que genera ReciboPdf, la estrategia elegida. Si algo falla antes de terminar, no se modifica ningún dato.

**21. Registrar pago: clases (0:30)**
> Las clases: formulario, controlador y **PagoServicio**, que usa Punitorio y la interfaz **GeneradorRecibo**, el Strategy, con ReciboPdf como implementación. El acceso a datos es **PagoDAO**, con el CRUD completo, y la conexión viene de Sql2oDAO.

**22. Consultar tablero: secuencia (0:25)**
> **Consultar tablero de cobros**. El responsable elige el mes; TableroServicio pide los cobros a PagoDAO y por cada uno lo **clasifica**; si está vencido y sin pagar, calcula la mora con Punitorio. Al final suma los totales y devuelve el tablero.

**23. Consultar tablero: clases (0:20)**
> En clases, TableroServicio devuelve un **Tablero**, un DTO con los cobros pagados, pendientes y vencidos y sus totales. Usa PagoDAO, Punitorio y el Singleton. Solo los pendientes con vencimiento anterior a hoy llevan punitorio.

**24. Calcular actualización: secuencia (0:30)**
> **Calcular actualización según índice** es automático: la tarea programada llama a `aplicarPendientes` todos los días a las 06:05 y al arrancar la aplicación. El servicio lee los contratos, calcula la fecha de ajuste de cada uno, consulta al BCRA el ICL o la inflación mensual del IPC y calcula el valor nuevo. Todo se guarda en una **única transacción**.

**25. Calcular actualización: clases (0:25)**
> En clases: la tarea ActualizacionProgramada, el **ActualizacionServicio**, que usa IndicesBcra para los índices, la utilidad CalculoActualizacion para los factores y **ActualizacionValorDAO** para guardar. La conexión es, de nuevo, el Singleton.

> **Pase a la demo:** "Con el diseño completo, Joaquín arranca la demostración."

---

### Demo en vivo — los tres (4:30), diapositiva 26

Tener el backend, la base y el navegador ya iniciados (ver `docs/Guia_Demo_En_Vivo.md`). Los datos de la demo ya están cargados.

**Joaquín (1:30): pasos 1 y 2**
> Vamos a ver **Registrar pago**. Acá están las dos tablas: los **pagos pendientes**, de a cinco por página y con los más vencidos primero, y los **pagos realizados**, de a diez. Puedo **filtrar por mes** y **buscar por DNI, nombre o propiedad**; la búsqueda se hace en el servidor, por eso es rápida aunque haya muchos pagos. Elijo un cobro **vencido** de octubre.

**Ana (1:30): pasos 3 a 5**
> Se abre la ventana de **cobro**. Ingreso la fecha y la forma de pago, por ejemplo transferencia. El sistema calcula solo la mora: el **alquiler**, el **punitorio** a razón de 1 % diario por los días de atraso y el **total a cobrar**. Si cambio la fecha a una **anterior al vencimiento**, el punitorio baja a cero. La dejo en hoy y **registro el pago**.

**Mati (1:30): pasos 6 a 9**
> Aparece **Pago registrado**. Abro el **recibo en PDF**, con su número correlativo. Cierro, y el cobro **pasó a realizados**, arriba de todo. Para terminar entro a **Visualizar actualizaciones**: acá están las actualizaciones por ICL o IPC que el sistema **aplicó solo** y las que vienen, también paginadas, y desde acá se le avisa al inquilino por WhatsApp. El cobro siguiente ya sale con el valor actualizado.

**Si algo falla:** abrir la pestaña de reserva con *Pagos realizados* y mostrar un recibo ya emitido; contarlo con naturalidad, sin disculparse.

---

### Mati — cierre (0:20), diapositiva 27

> Para cerrar: modelamos el problema, diseñamos en capas con cuatro patrones y los seis casos de uso, e implementamos los más importantes, que acaban de ver funcionando. Quedan para próximas iteraciones el tablero propio, el portal de autogestión y la liquidación al propietario. Muchas gracias. **¿Preguntas?**

---

## 4. Preguntas probables (responde quien presentó el tema)

El material de respaldo está en el **apéndice** del deck.

| Tema | Responde | Dónde está |
|---|---|---|
| Qué cubre el alcance y qué no | Joaquín | Apéndice: *Alcance del sistema* |
| Cómo se relaciona el Proceso Unificado con lo que hicimos | Joaquín | Apéndice: *Los tres pilares* y *Workflows y modelos* |
| Por qué cada capa hace lo que hace | Ana | Apéndice: *Qué hace y qué no hace cada capa* y *Un pedido de punta a punta* |
| Especificación de Registrar pago (flujos alternativos) | Mati | Apéndice: *Registrar pago, en una hoja* |
| Cómo se calcula el ICL y el IPC | Mati | Apéndice: *ICL e IPC* |
| Seguridad y copias de seguridad | Ana | Apéndice: *Qué hicimos y qué falta* y *Copias de seguridad* |
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

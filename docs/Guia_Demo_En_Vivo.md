# Guía de la demostración en vivo — UBIKAR

Esta guía sirve para armar el sistema en otra computadora (la netbook de Matías) y para ensayar y hacer la demostración en vivo, que tiene **dos partes**: **Registrar contrato** (unos 2:30) y **Registrar pago** (unos 3:30). Dura unos 6 minutos en total.

---

## 1. Armar el sistema en la netbook

### Requisitos
- **MySQL Server 8.0 o superior** corriendo en la compu (el servicio de Windows) y **MySQL Workbench** para cargar los datos.
- **Java 17** (`java -version`).
- **Git** y **Python** (Python solo sirve para mostrar el frontend como archivos estáticos).
- **Internet** (o el celular como hotspot): la actualización por ICL/IPC consulta la API del BCRA. Sin internet el sistema funciona igual, pero las actualizaciones nuevas quedan "esperando el índice".

### Paso a paso

1. **Clonar el repositorio** (PowerShell):
   ```powershell
   git clone https://github.com/JoaquinSormani/ubikar-alquileres.git
   cd ubikar-alquileres
   ```

2. **Cargar la base en MySQL Workbench.**
   1. Abrir Workbench y entrar a la conexión local (con la contraseña de `root` de esa compu).
   2. Menú **File → Open SQL Script…** y elegir `bd_ubikar.sql` (está en la carpeta del repositorio).
   3. Ejecutar todo el script con el botón del **rayo** (*Execute*). Crea la base `ubikar` con sus tablas. **Borra la base `ubikar` si ya existía.**
   4. Repetir con `bd_datos_presentacion.sql`: carga los datos de la demostración (borra los datos que hubiera y deja los de ejemplo).
   5. Comprobar con una consulta nueva:
      ```sql
      SELECT (SELECT COUNT(*) FROM ubikar.contrato) AS contratos,
             (SELECT COUNT(*) FROM ubikar.pago) AS pagos,
             (SELECT COUNT(*) FROM ubikar.pago WHERE estado = 'PENDIENTE') AS pendientes;
      ```
      Resultado esperado: **6 contratos, 44 pagos y 8 pendientes** (y 36 realizados).
      Los dos archivos ya incluyen los índices que usan las tablas paginadas. Si la base `ubikar` ya existía de antes y **no** se vuelve a cargar `bd_ubikar.sql`, ejecutar una vez `bd_migracion_indice_pagos.sql` y `bd_migracion_indice_actualizaciones.sql` (se pueden repetir sin problema).

3. **Obtener el programa** (**una** de las dos opciones):
   - **Compilarlo:** `mvn -DskipTests package` (genera `target\ubikar-alquileres.jar`).
   - **Copiarlo:** pasar el archivo `ubikar-alquileres.jar` (50 MB) en un pendrive a la carpeta `target\`.

4. **Decirle al sistema cómo conectarse a la base.** Por defecto usa el usuario `root`, la contraseña `root1234` y `localhost:3306`. Si la contraseña de `root` de esta compu es otra, hay que avisarlo **antes de arrancar** el backend, en la misma ventana de PowerShell:
   ```powershell
   $env:UBIKAR_DB_PASSWORD = "la_contraseña_de_root"
   ```
   Si el usuario o el puerto también son distintos:
   ```powershell
   $env:UBIKAR_DB_USER = "root"
   $env:UBIKAR_DB_URL  = "jdbc:mysql://localhost:3306/ubikar"
   ```

5. **Levantar el backend, parado en la carpeta del proyecto** (guarda los contratos Word en `documentos\contratos`):
   ```powershell
   java -jar target\ubikar-alquileres.jar
   ```
   Debe terminar con `Started UbikarApplication` y quedar escuchando en el puerto **8081**.

6. En otra ventana de PowerShell, **servir el frontend**:
   ```powershell
   python -m http.server 5500 --directory frontend
   ```

7. Abrir en el navegador: **http://localhost:5500/index.html**
   - Usuario: `Joaquin Sormani`
   - Contraseña: `ubikar2026`

### Comprobación rápida antes de empezar
Abrir **http://localhost:8081/api/pagos/panel**: tiene que mostrar un JSON con `pendientes` (8) y `realizados` (36). Si en el sistema aparece "No hay conexión con el servidor", el backend no está corriendo. Si el backend no arranca, casi siempre es la contraseña de MySQL (paso 4).

---

## 2. Ensayar y volver al punto de partida

Cada vez que se registra un pago, ese cobro deja de estar pendiente, y cada contrato que se registra ocupa una propiedad libre y suma un archivo Word en `documentos\contratos`. Hay 8 pendientes y solo 2 vencidos, y solo 3 propiedades libres (7, 8 y 9), así que **después de cada ensayo** conviene restaurar los datos: en Workbench, abrir y ejecutar de nuevo **`bd_datos_presentacion.sql`** (con el rayo). El backend puede seguir corriendo. Después recargar el navegador (F5).

Ese archivo **borra todos los datos** y vuelve a cargar los de ejemplo (no toca la estructura). **No ejecutarlo durante la exposición.**

> Los punitorios dependen de la fecha del día: el 1 % diario se cuenta hasta hoy. Los números que se vean el día de la exposición serán distintos de los del ensayo, y está bien.

---

## 3. Guion de la demostración

Se hacen **en este orden**: primero Registrar contrato (3.1) y después Registrar pago (3.2). El contrato que se registra en 3.1 empieza el mes que viene, así que **no agrega cobros** a los de este mes y los números de 3.2 no cambian.

### 3.1 Registrar contrato (unos 2:30)

Datos ya cargados que se usan (están libres en la base de la demo): propietario **Héctor Dominguez** (DNI **17234567**) con la propiedad **Ayacucho 330, casa**; inquilina **Florencia Luna** (DNI **39444555**); garante **Raúl Ponce** (DNI **14222333**).

| # | Hacés | Decís | Se ve |
|---|---|---|---|
| 1 | En el inicio, clic en la tarjeta **Nuevo contrato de alquiler**. En el paso **Propiedad**, escribir el DNI `17234567` y buscar. Elegir **Ayacucho 330, casa**. **Siguiente**. | "Registrar contrato sigue el proceso de negocio del alta. Primero buscamos al propietario por DNI y elegimos su propiedad." | Aparecen los datos de Héctor Dominguez y el selector de propiedades. |
| 2 | Paso **Inquilinos**: DNI `39444555` y buscar. **Siguiente**. | "El inquilino también se busca por DNI: como ya está registrado, el sistema completa sus datos." | Los datos de Florencia Luna se completan solos. |
| 3 | Paso **Garantía**: elegir **Fiador solidario**. Cargar al garante con el DNI `14222333`. **Siguiente**. | "Según la garantía, el sistema pide un garante." | Aparece el bloque del garante con los datos de Raúl Ponce. |
| 4 | Paso **Condiciones**: destino **Vivienda**; fecha de inicio el **primer día del mes que viene**; duración `24`; alquiler `280000`; actualización cada `3` meses; índice **ICL**; modelo **Estándar UBIKAR**. Dejar el depósito vacío. **Siguiente**. | "La fecha de fin, los montos en letras y las fechas de la firma los calcula el sistema." | La línea de datos calculados con la fecha de fin. |
| 5 | Paso **Inventario**: llega armado con las características de la propiedad (3 dormitorios, cocina, living). Mostrarlo y, si se quiere, cambiar el estado de un elemento. **Siguiente**. | "El inventario llega armado con las características de la propiedad y se anexa al contrato." | Aviso de inventario precargado y los elementos por ambiente. |
| 6 | Paso **Revisión**: recorrer el resumen y clic en **Generar contrato**. | "Se guarda todo junto: el contrato, el inventario y la documentación, y se crea el contrato en Word." | Pantalla **Contrato registrado**. |
| 7 | Clic en el enlace de descarga del Word y abrirlo. | "Este es el contrato listo para imprimir y firmar." | El documento con las cláusulas y el inventario. |

**Después de este paso la propiedad Ayacucho 330 queda ocupada.** Si hay que repetir la demostración, usar otra propiedad libre (Lafinur 56, depto 5, de Roberto Aguirre, DNI `20345678`; o Mitre 801, local 2, de María Gimenez, DNI `28555444`) o restaurar los datos (sección 2).

### 3.2 Registrar pago (unos 3:30)

Los pendientes vencidos de octubre son **Colón 480, depto 1** y **San Martín 95, local 3**. Usar uno de ellos.

| # | Hacés | Decís | Se ve |
|---|---|---|---|
| 1 | En el inicio, clic en la tarjeta **Registrar pago**. | "Este es el caso de uso que vamos a ver funcionando. La propiedad, el contrato y la actualización ya están cargados." | Dos tablas: *Pagos pendientes* (5 por página, los más vencidos primero) y *Pagos realizados* (10 por página), con sus contadores y botones **Anterior / Siguiente**. |
| 2 | Elegir el mes **octubre** en el selector. Escribir un apellido o DNI en el buscador. | "El responsable puede filtrar por mes y buscar por DNI, nombre o propiedad." | Las tablas se filtran. |
| 3 | En *Pagos pendientes*, elegir un cobro vencido de octubre y hacer clic en su acción. | "Elegimos el cobro vencido del inquilino." | Se abre la ventana **Cobrar alquiler** con el resumen. |
| 4 | Dejar la fecha de hoy y elegir la forma de pago (por ejemplo, transferencia). | "Ingresamos la fecha y la forma de pago, y el sistema calcula solo la mora." | Tres líneas: *Alquiler*, *Punitorio* y *Total a cobrar* (1 % diario por día de atraso). |
| 5 | Hacer clic en el campo **Fecha de pago** y elegir una fecha igual o anterior al vencimiento del cobro (está en la columna *Vence*): con el ícono de calendario, o escribiéndola con el teclado (`dd/mm/aaaa`). Volver a poner la de hoy. No se puede elegir una fecha posterior a hoy. | "Si se paga en término, el punitorio baja a cero." | El punitorio y el total se recalculan. |
| 6 | Clic en **Registrar pago**. | "Confirmamos y el sistema registra el pago." | Pantalla **Pago registrado** con el detalle. |
| 7 | Clic en **Ver recibo (PDF)**. Cerrar la pestaña. | "Se emite el recibo con su número correlativo." | El PDF en una pestaña nueva. |
| 8 | Clic en **Cerrar**. | "El cobro pasó a *Pagos realizados* y bajó el contador de pendientes." | El cobro nuevo arriba de la tabla de realizados, con su número de recibo. |
| 9 | Volver al inicio y entrar a **Visualizar actualizaciones**. Mostrar un contrato con una actualización ya aplicada y, si corresponde, avisar al inquilino. | "Esto lo hace el sistema solo en la fecha de ajuste, por ICL o IPC, y el cobro siguiente ya sale con el valor nuevo." | Dos tablas paginadas: *Actualizaciones aplicadas* (10 por página) y *Próximas actualizaciones* (5 por página). |

**Antes de la exposición:** revisar en *Visualizar actualizaciones* qué contrato tiene una actualización aplicada para mostrar en el paso 9.

---

## 4. Plan B

- **Falla Registrar contrato** (por ejemplo, no descarga el Word): mostrar la pantalla **Contrato registrado** y seguir con Registrar pago; el contrato ya figura en la lista de contratos.
- **Falla el PDF o el backend:** tener abierta otra pestaña con la tabla de *Pagos realizados* y mostrar un recibo ya emitido.
- **El backend no arranca:** verificar que el servicio de MySQL esté en marcha y que la contraseña de `root` esté cargada (paso 4 de la sección 1).
- **El puerto 8081 está ocupado:** cerrar el otro programa Java que lo esté usando (el puerto del backend está fijo).
- **Pantalla vacía o con datos viejos:** F5 en el navegador.

## 5. Antes de entrar a presentar (lista rápida)

- [ ] Servicio de MySQL en marcha (se ve en Workbench) y datos de la demo cargados.
- [ ] Backend corriendo (`Started UbikarApplication`).
- [ ] Frontend en `http://localhost:5500/index.html` y sesión iniciada.
- [ ] `http://localhost:8081/api/pagos/panel` devuelve 8 pendientes y 36 realizados.
- [ ] Datos restaurados (sin ensayos hechos encima): Ayacucho 330 libre y 8 pendientes.
- [ ] Probar que el Word del contrato se descarga y se abre (Word o visor instalado en la netbook).
- [ ] Internet o hotspot conectado.
- [ ] Una pestaña con *Pagos realizados* lista como plan B.

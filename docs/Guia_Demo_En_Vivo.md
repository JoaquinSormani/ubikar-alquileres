# Guía de la demostración en vivo — UBIKAR

Esta guía sirve para armar el sistema en otra computadora (la netbook de Matías) y para ensayar y hacer la demostración de **Registrar pago**, que es el caso de uso que pidió ver la cátedra. Dura unos 5 minutos.

---

## 1. Armar el sistema en la netbook

### Requisitos
- **Docker Desktop** abierto (la base de datos corre en un contenedor MySQL).
- **Java 17** (`java -version`).
- **Git** y **Python** (Python solo sirve para mostrar el frontend como archivos estáticos).
- **Internet** (o el celular como hotspot): la actualización por ICL/IPC consulta la API del BCRA. Sin internet el sistema funciona igual, pero las actualizaciones nuevas quedan "esperando el índice".

### Paso a paso (PowerShell, desde la carpeta donde se quiere clonar)

1. Clonar el repositorio:
   ```powershell
   git clone https://github.com/JoaquinSormani/ubikar-alquileres.git
   cd ubikar-alquileres
   ```

2. Crear el contenedor de MySQL (si todavía no existe uno llamado `mysql-bd1`):
   ```powershell
   docker run --name mysql-bd1 --restart unless-stopped -e MYSQL_ROOT_PASSWORD=root1234 -p 3306:3306 -v mysql_bd1:/var/lib/mysql -d mysql:8.0
   ```
   Esperar unos 20 segundos a que arranque. Si el contenedor ya existe: `docker start mysql-bd1`.

3. Cargar la estructura y los datos de la demostración. En PowerShell el signo `<` no funciona, por eso se usa `cmd /c`:
   ```powershell
   cmd /c "docker exec -i mysql-bd1 mysql -uroot -proot1234 --default-character-set=utf8mb4 < bd_ubikar.sql"
   cmd /c "docker exec -i mysql-bd1 mysql -uroot -proot1234 --default-character-set=utf8mb4 < bd_datos_presentacion.sql"
   ```
   Resultado esperado: 6 contratos, 44 pagos (8 pendientes y 36 realizados) y 8 actualizaciones de valor.

4. Obtener el programa (**una** de las dos opciones):
   - **Compilarlo:** `mvn -DskipTests package` (genera `target\ubikar-alquileres.jar`).
   - **Copiarlo:** pasar el archivo `ubikar-alquileres.jar` (50 MB) en un pendrive a la carpeta `target\`.

5. Levantar el backend, **parado en la carpeta del proyecto** (guarda los contratos Word en `documentos\contratos`):
   ```powershell
   java -jar target\ubikar-alquileres.jar
   ```
   Debe terminar con `Started UbikarApplication` y quedar escuchando en el puerto **8081**.

6. En otra ventana de PowerShell, servir el frontend:
   ```powershell
   python -m http.server 5500 --directory frontend
   ```

7. Abrir en el navegador: **http://localhost:5500/index.html**
   - Usuario: `Joaquin Sormani`
   - Contraseña: `ubikar2026`

### Comprobación rápida antes de empezar
Abrir **http://localhost:8081/api/pagos/panel**: tiene que mostrar un JSON con `pendientes` (8) y `realizados` (36). Si aparece "No hay conexión con el servidor" en el sistema, el backend no está corriendo.

---

## 2. Ensayar y volver al punto de partida

Cada vez que se registra un pago, ese cobro deja de estar pendiente. Hay 8 pendientes y solo 2 vencidos, así que **después de cada ensayo** conviene restaurar los datos. Con el backend corriendo, ejecutar de nuevo:

```powershell
cmd /c "docker exec -i mysql-bd1 mysql -uroot -proot1234 --default-character-set=utf8mb4 < bd_datos_presentacion.sql"
```

Ese archivo **borra todos los datos** y vuelve a cargar los de ejemplo (no toca la estructura). Después de restaurar, recargar el navegador (F5). **No ejecutarlo durante la exposición.**

En la computadora de Joaquín además hay una copia de respaldo de la base (`Respaldos_BD\ubikar_demo_2026-11-08.sql`), pero el archivo del repositorio alcanza para dejar todo igual.

> Los punitorios dependen de la fecha del día: el 1 % diario se cuenta hasta hoy. Los números que se vean el día de la exposición serán distintos de los del ensayo, y está bien.

---

## 3. Guion de la demostración (Registrar pago)

Los pendientes vencidos de octubre son **Colón 480, depto 1** y **San Martín 95, local 3**. Usar uno de ellos.

| # | Hacés | Decís | Se ve |
|---|---|---|---|
| 1 | En el inicio, clic en la tarjeta **Registrar pago**. | "Este es el caso de uso que vamos a ver funcionando. La propiedad, el contrato y la actualización ya están cargados." | Dos tablas: *Pagos pendientes* y *Pagos realizados*, con sus contadores. |
| 2 | Elegir el mes **octubre** en el selector. Escribir un apellido o DNI en el buscador. | "El responsable puede filtrar por mes y buscar por DNI, nombre o propiedad." | Las tablas se filtran. |
| 3 | En *Pagos pendientes*, elegir un cobro vencido de octubre y hacer clic en su acción. | "Elegimos el cobro vencido del inquilino." | Se abre la ventana **Cobrar alquiler** con el resumen. |
| 4 | Dejar la fecha de hoy y elegir la forma de pago (por ejemplo, transferencia). | "Ingresamos la fecha y la forma de pago, y el sistema calcula solo la mora." | Tres líneas: *Alquiler*, *Punitorio* y *Total a cobrar* (1 % diario por día de atraso). |
| 5 | Hacer clic en el campo **Fecha de pago** y elegir una fecha igual o anterior al vencimiento del cobro (está en la columna *Vence*): con el ícono de calendario, o escribiéndola con el teclado (`dd/mm/aaaa`). Volver a poner la de hoy. No se puede elegir una fecha posterior a hoy. | "Si se paga en término, el punitorio baja a cero." | El punitorio y el total se recalculan. |
| 6 | Clic en **Registrar pago**. | "Confirmamos y el sistema registra el pago." | Pantalla **Pago registrado** con el detalle. |
| 7 | Clic en **Ver recibo (PDF)**. Cerrar la pestaña. | "Se emite el recibo con su número correlativo." | El PDF en una pestaña nueva. |
| 8 | Clic en **Cerrar**. | "El cobro pasó a *Pagos realizados* y bajó el contador de pendientes." | El cobro nuevo arriba de la tabla de realizados, con su número de recibo. |
| 9 | Volver al inicio y entrar a **Actualizaciones**. Mostrar un contrato con una actualización ya aplicada. | "Esto lo hace el sistema solo en la fecha de ajuste, por ICL o IPC, y el cobro siguiente ya sale con el valor nuevo." | El panel de actualizaciones aplicadas y próximas. |

**Antes de la exposición:** revisar en *Actualizaciones* qué contrato tiene una actualización aplicada para mostrar en el paso 9.

---

## 4. Plan B

- **Falla el PDF o el backend:** tener abierta otra pestaña con la tabla de *Pagos realizados* y mostrar un recibo ya emitido.
- **El backend no arranca:** verificar que Docker Desktop esté abierto y que el contenedor esté en marcha (`docker ps`).
- **El puerto 8081 está ocupado:** cerrar el otro programa Java que lo esté usando (el puerto del backend está fijo).
- **Pantalla vacía o con datos viejos:** F5 en el navegador.

## 5. Antes de entrar a presentar (lista rápida)

- [ ] Docker Desktop abierto y contenedor `mysql-bd1` en marcha.
- [ ] Backend corriendo (`Started UbikarApplication`).
- [ ] Frontend en `http://localhost:5500/index.html` y sesión iniciada.
- [ ] `http://localhost:8081/api/pagos/panel` devuelve 8 pendientes y 36 realizados.
- [ ] Datos restaurados (sin ensayos hechos encima).
- [ ] Internet o hotspot conectado.
- [ ] Una pestaña con *Pagos realizados* lista como plan B.

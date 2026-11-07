-- Datos coherentes para la presentación (noviembre de 2026).
-- ATENCIÓN: borra TODOS los datos actuales de la base `ubikar` y carga los de ejemplo. No toca la estructura.
-- Primero la estructura (bd_ubikar.sql) si la base no existe; después este archivo:
--   docker exec -i mysql-bd1 mysql -uroot -proot1234 --default-character-set=utf8mb4 < bd_datos_presentacion.sql
-- Las actualizaciones de alquiler usan los valores reales del ICL y del IPC (BCRA) a esa fecha.
USE `ubikar`;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `actualizacion_valor`;
TRUNCATE TABLE `mantenimiento`;
TRUNCATE TABLE `liquidacion`;
TRUNCATE TABLE `documentacion`;
TRUNCATE TABLE `renglon_inventario`;
TRUNCATE TABLE `inventario`;
TRUNCATE TABLE `contrato_garante`;
TRUNCATE TABLE `contrato_inquilino`;
TRUNCATE TABLE `pago`;
TRUNCATE TABLE `contrato`;
TRUNCATE TABLE `caracteristica_propiedad`;
TRUNCATE TABLE `propiedad`;
TRUNCATE TABLE `garante`;
TRUNCATE TABLE `inquilino`;
TRUNCATE TABLE `propietario`;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `propietario` (id, nombre, apellido, dni, telefono, email, domicilio) VALUES
  (1, 'Maria', 'Gimenez', '28555444', '2664007788', 'maria.gimenez@mail.com', 'Rivadavia 1080, San Luis'),
  (2, 'Hector', 'Dominguez', '17234567', '2664112233', 'hector.dominguez@mail.com', 'Pringles 950, San Luis'),
  (3, 'Lucia', 'Ferreyra', '24876543', '2664223344', 'lucia.ferreyra@mail.com', 'Colon 120, San Luis'),
  (4, 'Roberto', 'Aguirre', '20345678', '2664334455', 'roberto.aguirre@mail.com', 'Mitre 640, San Luis');

INSERT INTO `inquilino` (id, nombre, apellido, dni, telefono, email, domicilio) VALUES
  (1, 'Juan', 'Perez', '30111222', '2664001122', 'juan.perez@mail.com', 'Belgrano 12, San Luis'),
  (2, 'Camila', 'Rojas', '38123456', '2664551201', 'camila.rojas@mail.com', 'Lavalle 345, San Luis'),
  (3, 'Diego', 'Pereyra', '36987654', '2664551202', 'diego.pereyra@mail.com', 'Ejercito de los Andes 22, San Luis'),
  (4, 'Valeria', 'Sosa', '33555666', '2664551203', 'valeria.sosa@mail.com', 'Chacabuco 870, San Luis'),
  (5, 'Martin', 'Quiroga', '35222111', '2664551204', 'martin.quiroga@mail.com', 'Sucre 415, San Luis'),
  (6, 'Nicolas', 'Bustos', '34666777', '2664551205', 'nicolas.bustos@mail.com', 'San Juan 90, San Luis'),
  (7, 'Agustina', 'Herrera', '40111333', '2664551206', 'agustina.herrera@mail.com', 'San Juan 90, San Luis'),
  (8, 'Florencia', 'Luna', '39444555', '2664551207', 'florencia.luna@mail.com', 'Lafinur 301, San Luis');

INSERT INTO `garante` (id, nombre, apellido, dni, telefono, email, domicilio, detalle) VALUES
  (1, 'Raul', 'Ponce', '14222333', '2664661101', 'raul.ponce@mail.com', 'Pedernera 250, San Luis', 'Propietario del inmueble de Pedernera 250'),
  (2, 'Silvia', 'Montes', '18333444', '2664661102', 'silvia.montes@mail.com', 'Rivadavia 777, San Luis', 'Empleada publica, legajo 4521'),
  (3, 'Jorge', 'Castro', '16444555', '2664661103', 'jorge.castro@mail.com', 'Maipu 63, San Luis', 'Comerciante, local en Colon 400');

INSERT INTO `propiedad` (id, direccion, idPropietario) VALUES
  (1, 'Junin 700, depto 3', 1),
  (2, 'Belgrano 1250, 2.º B', 2),
  (3, 'Colon 480, depto 1', 3),
  (4, 'San Martin 95, local 3', 4),
  (5, 'Rivadavia 1014, 1.º A', 1),
  (6, 'Pringles 1422, depto 2', 3),
  (7, 'Ayacucho 330, casa', 2),
  (8, 'Lafinur 56, depto 5', 4),
  (9, 'Mitre 801, local 2', 1);

INSERT INTO `caracteristica_propiedad` (idPropiedad, nombre, cantidad) VALUES
  (1, 'Dormitorio', 2),
  (1, 'Cocina', 1),
  (1, 'Living', 1),
  (2, 'Dormitorio', 3),
  (2, 'Cocina', 1),
  (2, 'Living', 1),
  (3, 'Dormitorio', 1),
  (3, 'Cocina', 1),
  (3, 'Living', 1),
  (4, 'Cocina', 1),
  (5, 'Dormitorio', 2),
  (5, 'Cocina', 1),
  (5, 'Living', 1),
  (6, 'Dormitorio', 3),
  (6, 'Cocina', 1),
  (6, 'Living', 1),
  (7, 'Dormitorio', 3),
  (7, 'Cocina', 1),
  (7, 'Living', 1),
  (8, 'Dormitorio', 1),
  (8, 'Cocina', 1),
  (8, 'Living', 1),
  (9, 'Cocina', 1);

INSERT INTO `contrato` (id, idPropiedad, fechaInicio, fechaFin, valorInicial, periodicidadActualizacion, indiceActualizacion, destino, depositoGarantia, tipoGarantia) VALUES
  (1, 1, '2026-03-01', '2028-02-29', 300000, 3, 'ICL', 'VIVIENDA', 300000, 'FIADOR_SOLIDARIO'),
  (2, 2, '2026-01-01', '2027-12-31', 250000, 6, 'ICL', 'VIVIENDA', 250000, 'FIADOR_SOLIDARIO'),
  (3, 3, '2026-07-01', '2028-06-30', 180000, 3, 'IPC', 'VIVIENDA', 180000, 'SEGURO_CAUCION'),
  (4, 4, '2026-09-01', '2028-08-31', 320000, 3, 'IPC', 'COMERCIAL', 320000, 'SEGURO_CAUCION'),
  (5, 5, '2026-02-01', '2028-01-31', 220000, 3, 'ICL', 'VIVIENDA', 220000, 'RECIBO_SUELDO'),
  (6, 6, '2026-06-01', '2028-05-31', 260000, 4, 'IPC', 'VIVIENDA', 260000, 'FIADOR_SOLIDARIO');

INSERT INTO `contrato_inquilino` (idContrato, idInquilino) VALUES
  (1, 1),
  (2, 2),
  (3, 3),
  (4, 4),
  (5, 5),
  (6, 6),
  (6, 7);

INSERT INTO `contrato_garante` (idContrato, idGarante) VALUES
  (1, 1),
  (2, 2),
  (5, 2),
  (6, 3);

INSERT INTO `inventario` (id, idContrato, fechaRegistro) VALUES
  (1, 1, '2026-03-01'),
  (2, 2, '2026-01-01'),
  (3, 3, '2026-07-01'),
  (4, 4, '2026-09-01'),
  (5, 5, '2026-02-01'),
  (6, 6, '2026-06-01');

INSERT INTO `renglon_inventario` (idInventario, ambiente, objeto, estado, observaciones) VALUES
  (1, 'Cocina', 'Anafe', 'BUENO', NULL),
  (1, 'Cocina', 'Mesada', 'BUENO', NULL),
  (1, 'Cocina', 'Alacenas', 'REGULAR', 'Una puerta floja'),
  (1, 'Dormitorio', 'Placard', 'BUENO', NULL),
  (1, 'Dormitorio', 'Ventana', 'BUENO', NULL),
  (1, 'Dormitorio', 'Puerta', 'BUENO', NULL),
  (1, 'Baño', 'Inodoro', 'BUENO', NULL),
  (1, 'Baño', 'Ducha', 'REGULAR', 'Pierde un poco'),
  (2, 'Cocina', 'Anafe', 'BUENO', NULL),
  (2, 'Cocina', 'Mesada', 'BUENO', NULL),
  (2, 'Cocina', 'Alacenas', 'REGULAR', 'Una puerta floja'),
  (2, 'Dormitorio', 'Placard', 'BUENO', NULL),
  (2, 'Dormitorio', 'Ventana', 'BUENO', NULL),
  (2, 'Dormitorio', 'Puerta', 'BUENO', NULL),
  (2, 'Baño', 'Inodoro', 'BUENO', NULL),
  (2, 'Baño', 'Ducha', 'REGULAR', 'Pierde un poco'),
  (3, 'Cocina', 'Anafe', 'BUENO', NULL),
  (3, 'Cocina', 'Mesada', 'BUENO', NULL),
  (3, 'Cocina', 'Alacenas', 'REGULAR', 'Una puerta floja'),
  (3, 'Dormitorio', 'Placard', 'BUENO', NULL),
  (3, 'Dormitorio', 'Ventana', 'BUENO', NULL),
  (3, 'Dormitorio', 'Puerta', 'BUENO', NULL),
  (3, 'Baño', 'Inodoro', 'BUENO', NULL),
  (3, 'Baño', 'Ducha', 'REGULAR', 'Pierde un poco'),
  (4, 'Cocina', 'Anafe', 'BUENO', NULL),
  (4, 'Cocina', 'Mesada', 'BUENO', NULL),
  (4, 'Cocina', 'Alacenas', 'REGULAR', 'Una puerta floja'),
  (5, 'Cocina', 'Anafe', 'BUENO', NULL),
  (5, 'Cocina', 'Mesada', 'BUENO', NULL),
  (5, 'Cocina', 'Alacenas', 'REGULAR', 'Una puerta floja'),
  (5, 'Dormitorio', 'Placard', 'BUENO', NULL),
  (5, 'Dormitorio', 'Ventana', 'BUENO', NULL),
  (5, 'Dormitorio', 'Puerta', 'BUENO', NULL),
  (5, 'Baño', 'Inodoro', 'BUENO', NULL),
  (5, 'Baño', 'Ducha', 'REGULAR', 'Pierde un poco'),
  (6, 'Cocina', 'Anafe', 'BUENO', NULL),
  (6, 'Cocina', 'Mesada', 'BUENO', NULL),
  (6, 'Cocina', 'Alacenas', 'REGULAR', 'Una puerta floja'),
  (6, 'Dormitorio', 'Placard', 'BUENO', NULL),
  (6, 'Dormitorio', 'Ventana', 'BUENO', NULL),
  (6, 'Dormitorio', 'Puerta', 'BUENO', NULL),
  (6, 'Baño', 'Inodoro', 'BUENO', NULL),
  (6, 'Baño', 'Ducha', 'REGULAR', 'Pierde un poco');

INSERT INTO `actualizacion_valor` (idContrato, periodoDesde, fecha, indice, valorAnterior, valorNuevo, factor) VALUES
  (1, '2026-06-01', '2026-06-01', 'ICL', 300000, 326389.80, 1.087966),
  (1, '2026-09-01', '2026-09-01', 'ICL', 326389.80, 350621.31, 1.074241),
  (2, '2026-07-01', '2026-07-01', 'ICL', 250000, 291255.50, 1.165022),
  (3, '2026-10-01', '2026-10-01', 'IPC', 180000, 190455.48, 1.058086),
  (5, '2026-05-01', '2026-05-01', 'ICL', 220000, 235384.60, 1.069930),
  (5, '2026-08-01', '2026-08-01', 'ICL', 235384.60, 256483.53, 1.089636),
  (5, '2026-11-01', '2026-11-01', 'ICL', 256483.53, 272600.70, 1.062839),
  (6, '2026-10-01', '2026-10-01', 'IPC', 260000, 280329.14, 1.078189);

INSERT INTO `pago` (idContrato, periodo, fechaVencimiento, fecha, monto, indiceActualizacion, diasAtraso, punitorio, estado, medioPago, numeroRecibo) VALUES
  (1, '2026-03-01', '2026-03-10', '2026-03-08', 300000, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 6),
  (1, '2026-04-01', '2026-04-10', '2026-04-09', 300000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 9),
  (1, '2026-05-01', '2026-05-10', '2026-05-02', 300000, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 10),
  (1, '2026-06-01', '2026-06-10', '2026-06-03', 326389.80, 1.087966, 0, 0.00, 'PAGADO', 'EFECTIVO', 14),
  (1, '2026-07-01', '2026-07-10', '2026-07-04', 326389.80, NULL, 0, 0.00, 'PAGADO', 'TARJETA_CREDITO', 19),
  (1, '2026-08-01', '2026-08-10', '2026-08-05', 326389.80, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 24),
  (1, '2026-09-01', '2026-09-10', '2026-09-06', 350621.31, 1.074241, 0, 0.00, 'PAGADO', 'EFECTIVO', 30),
  (1, '2026-10-01', '2026-10-10', '2026-10-07', 350621.31, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 35),
  (1, '2026-11-01', '2026-11-10', NULL, 350621.31, NULL, 0, 0.00, 'PENDIENTE', NULL, NULL),
  (2, '2026-01-01', '2026-01-10', '2026-01-09', 250000, NULL, 0, 0.00, 'PAGADO', 'TARJETA_CREDITO', 1),
  (2, '2026-02-01', '2026-02-10', '2026-02-02', 250000, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 2),
  (2, '2026-03-01', '2026-03-10', '2026-03-03', 250000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 4),
  (2, '2026-04-01', '2026-04-10', '2026-04-04', 250000, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 7),
  (2, '2026-05-01', '2026-05-10', '2026-05-05', 250000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 11),
  (2, '2026-06-01', '2026-06-10', '2026-06-06', 250000, NULL, 0, 0.00, 'PAGADO', 'TARJETA_CREDITO', 15),
  (2, '2026-07-01', '2026-07-10', '2026-07-07', 291255.50, 1.165022, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 20),
  (2, '2026-08-01', '2026-08-10', '2026-08-08', 291255.50, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 25),
  (2, '2026-09-01', '2026-09-10', '2026-09-09', 291255.50, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 32),
  (2, '2026-10-01', '2026-10-10', '2026-10-16', 291255.50, NULL, 6, 17475.33, 'PAGADO', 'EFECTIVO', 36),
  (2, '2026-11-01', '2026-11-10', NULL, 291255.50, NULL, 0, 0.00, 'PENDIENTE', NULL, NULL),
  (3, '2026-07-01', '2026-07-10', '2026-07-02', 180000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 17),
  (3, '2026-08-01', '2026-08-10', '2026-08-03', 180000, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 22),
  (3, '2026-09-01', '2026-09-10', '2026-09-04', 180000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 28),
  (3, '2026-10-01', '2026-10-10', NULL, 190455.48, 1.058086, 0, 0.00, 'PENDIENTE', NULL, NULL),
  (3, '2026-11-01', '2026-11-10', NULL, 190455.48, NULL, 0, 0.00, 'PENDIENTE', NULL, NULL),
  (4, '2026-09-01', '2026-09-10', '2026-09-07', 320000, NULL, 0, 0.00, 'PAGADO', 'TARJETA_CREDITO', 31),
  (4, '2026-10-01', '2026-10-10', NULL, 320000, NULL, 0, 0.00, 'PENDIENTE', NULL, NULL),
  (4, '2026-11-01', '2026-11-10', NULL, 320000, NULL, 0, 0.00, 'PENDIENTE', NULL, NULL),
  (5, '2026-02-01', '2026-02-10', '2026-02-03', 220000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 3),
  (5, '2026-03-01', '2026-03-10', '2026-03-04', 220000, NULL, 0, 0.00, 'PAGADO', 'TARJETA_CREDITO', 5),
  (5, '2026-04-01', '2026-04-10', '2026-04-05', 220000, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 8),
  (5, '2026-05-01', '2026-05-10', '2026-05-06', 235384.60, 1.069930, 0, 0.00, 'PAGADO', 'EFECTIVO', 12),
  (5, '2026-06-01', '2026-06-10', '2026-06-07', 235384.60, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 16),
  (5, '2026-07-01', '2026-07-10', '2026-07-08', 235384.60, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 21),
  (5, '2026-08-01', '2026-08-10', '2026-08-09', 256483.53, 1.089636, 0, 0.00, 'PAGADO', 'TARJETA_CREDITO', 26),
  (5, '2026-09-01', '2026-09-10', '2026-09-02', 256483.53, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 27),
  (5, '2026-10-01', '2026-10-10', '2026-10-03', 256483.53, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 33),
  (5, '2026-11-01', '2026-11-10', NULL, 272600.70, 1.062839, 0, 0.00, 'PENDIENTE', NULL, NULL),
  (6, '2026-06-01', '2026-06-10', '2026-06-02', 260000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 13),
  (6, '2026-07-01', '2026-07-10', '2026-07-03', 260000, NULL, 0, 0.00, 'PAGADO', 'TARJETA_CREDITO', 18),
  (6, '2026-08-01', '2026-08-10', '2026-08-04', 260000, NULL, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 23),
  (6, '2026-09-01', '2026-09-10', '2026-09-05', 260000, NULL, 0, 0.00, 'PAGADO', 'EFECTIVO', 29),
  (6, '2026-10-01', '2026-10-10', '2026-10-06', 280329.14, 1.078189, 0, 0.00, 'PAGADO', 'TRANSFERENCIA', 34),
  (6, '2026-11-01', '2026-11-10', NULL, 280329.14, NULL, 0, 0.00, 'PENDIENTE', NULL, NULL);


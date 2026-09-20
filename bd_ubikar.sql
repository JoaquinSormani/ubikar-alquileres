DROP DATABASE IF EXISTS `ubikar`;
CREATE SCHEMA `ubikar`;

CREATE TABLE `ubikar`.`inquilino` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(80) NOT NULL,
  `apellido` VARCHAR(80) NOT NULL,
  `dni` VARCHAR(20) NOT NULL,
  `telefono` VARCHAR(30) NULL,
  `email` VARCHAR(120) NULL,
  `domicilio` VARCHAR(200) NULL,
  PRIMARY KEY (`id`));

INSERT INTO `ubikar`.`inquilino` (nombre, apellido, dni, telefono, email)
VALUES ('Juan', 'Perez', '30111222', '2664001122', 'juan.perez@mail.com');

CREATE TABLE `ubikar`.`propietario` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(80) NOT NULL,
  `apellido` VARCHAR(80) NOT NULL,
  `dni` VARCHAR(20) NOT NULL,
  `telefono` VARCHAR(30) NULL,
  `email` VARCHAR(120) NULL,
  `domicilio` VARCHAR(200) NULL,
  PRIMARY KEY (`id`));

INSERT INTO `ubikar`.`propietario` (nombre, apellido, dni, telefono, email)
VALUES ('Maria', 'Gimenez', '28555444', '2664007788', 'maria.gimenez@mail.com');

-- MODELO DE DOMINIO (resto de las tablas)

CREATE TABLE `ubikar`.`garante` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(80) NOT NULL,
  `apellido` VARCHAR(80) NOT NULL,
  `dni` VARCHAR(20) NOT NULL,
  `telefono` VARCHAR(30) NULL,
  `email` VARCHAR(120) NULL,
  `domicilio` VARCHAR(200) NULL,
  `detalle` VARCHAR(500) NULL,
  PRIMARY KEY (`id`));

CREATE TABLE `ubikar`.`propiedad` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `direccion` VARCHAR(200) NOT NULL,
  `idPropietario` INT NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idPropietario`) REFERENCES `ubikar`.`propietario` (`id`));

CREATE TABLE `ubikar`.`contrato` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idPropiedad` INT NOT NULL,
  `fechaInicio` DATE NOT NULL,
  `fechaFin` DATE NOT NULL,
  `valorInicial` DECIMAL(12,2) NOT NULL,
  `periodicidadActualizacion` INT NOT NULL,
  `indiceActualizacion` VARCHAR(10) NOT NULL,
  `destino` VARCHAR(20) NOT NULL,
  `depositoGarantia` DECIMAL(12,2) NOT NULL,
  `tipoGarantia` VARCHAR(30) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idPropiedad`) REFERENCES `ubikar`.`propiedad` (`id`));

CREATE TABLE `ubikar`.`contrato_inquilino` (
  `idContrato` INT NOT NULL,
  `idInquilino` INT NOT NULL,
  PRIMARY KEY (`idContrato`, `idInquilino`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`),
  FOREIGN KEY (`idInquilino`) REFERENCES `ubikar`.`inquilino` (`id`));

CREATE TABLE `ubikar`.`contrato_garante` (
  `idContrato` INT NOT NULL,
  `idGarante` INT NOT NULL,
  PRIMARY KEY (`idContrato`, `idGarante`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`),
  FOREIGN KEY (`idGarante`) REFERENCES `ubikar`.`garante` (`id`));

CREATE TABLE `ubikar`.`inventario` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idContrato` INT NOT NULL UNIQUE,
  `fechaRegistro` DATE NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`));

CREATE TABLE `ubikar`.`renglon_inventario` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idInventario` INT NOT NULL,
  `ambiente` VARCHAR(80) NULL,
  `objeto` VARCHAR(120) NOT NULL,
  `estado` VARCHAR(20) NOT NULL,
  `observaciones` VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idInventario`) REFERENCES `ubikar`.`inventario` (`id`));

CREATE TABLE `ubikar`.`documentacion` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idContrato` INT NOT NULL,
  `tipo` VARCHAR(30) NOT NULL,
  `archivo` VARCHAR(255) NOT NULL,
  `fechaCarga` DATE NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`));

CREATE TABLE `ubikar`.`mantenimiento` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idContrato` INT NOT NULL,
  `fecha` DATE NOT NULL,
  `descripcion` VARCHAR(500) NOT NULL,
  `fotos` VARCHAR(1000) NULL,
  `responsablePago` VARCHAR(20) NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`));

CREATE TABLE `ubikar`.`pago` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idContrato` INT NOT NULL,
  `fecha` DATE NULL,
  `monto` DECIMAL(12,2) NOT NULL,
  `indiceActualizacion` DECIMAL(10,6) NULL,
  `diasAtraso` INT NOT NULL DEFAULT 0,
  `punitorio` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `estado` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`));

CREATE TABLE `ubikar`.`liquidacion` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idPago` INT NOT NULL UNIQUE,
  `fecha` DATE NOT NULL,
  `montoBruto` DECIMAL(12,2) NOT NULL,
  `comision` DECIMAL(12,2) NOT NULL,
  `montoNeto` DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`idPago`) REFERENCES `ubikar`.`pago` (`id`));

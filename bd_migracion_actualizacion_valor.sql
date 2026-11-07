-- Migracion: tabla para registrar el historial de actualizaciones automaticas de valor de alquiler.
USE `ubikar`;

CREATE TABLE IF NOT EXISTS `actualizacion_valor` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `idContrato` INT NOT NULL,
  `periodoDesde` DATE NOT NULL,            -- primer mes con el valor nuevo (dia 1)
  `fecha` DATE NOT NULL,                   -- dia en que el sistema la aplico
  `indice` VARCHAR(10) NOT NULL,           -- ICL o IPC
  `valorAnterior` DECIMAL(12,2) NOT NULL,
  `valorNuevo` DECIMAL(12,2) NOT NULL,
  `factor` DECIMAL(10,6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_actualizacion` (`idContrato`, `periodoDesde`),
  FOREIGN KEY (`idContrato`) REFERENCES `ubikar`.`contrato` (`id`));

-- Migracion de la tabla `pago` para Registrar Pago (correr UNA vez sobre una base `ubikar` ya creada).
-- Agrega el periodo (mes que se paga), la fecha de vencimiento y el medio de pago.
USE `ubikar`;

ALTER TABLE `pago`
  ADD COLUMN `periodo` DATE NULL AFTER `idContrato`,
  ADD COLUMN `fechaVencimiento` DATE NULL AFTER `periodo`,
  ADD COLUMN `medioPago` VARCHAR(20) NULL;

-- La tabla no se usaba antes: si hubiera filas, se completan con valores razonables.
UPDATE `pago`
   SET `periodo` = DATE_FORMAT(COALESCE(`fecha`, CURDATE()), '%Y-%m-01'),
       `fechaVencimiento` = DATE_FORMAT(COALESCE(`fecha`, CURDATE()), '%Y-%m-10')
 WHERE `periodo` IS NULL;

ALTER TABLE `pago`
  MODIFY `periodo` DATE NOT NULL,
  MODIFY `fechaVencimiento` DATE NOT NULL,
  ADD UNIQUE KEY `uq_pago_periodo` (`idContrato`, `periodo`);

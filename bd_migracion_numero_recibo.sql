-- Migracion: el numero de recibo pasa a ser correlativo y se asigna al cobrar (correr UNA vez sobre una base `ubikar` ya creada).
-- Antes el recibo llevaba el id del cobro, que nace cuando se arma el cobro del mes y no cuando se paga.
USE `ubikar`;

ALTER TABLE `pago`
  ADD COLUMN `numeroRecibo` INT NULL,
  ADD UNIQUE KEY `uq_pago_recibo` (`numeroRecibo`);

-- Los pagos ya registrados reciben su numero en el orden en que se pagaron (por fecha de pago y, si empatan, por id).
-- Requiere MySQL 8 o superior (ROW_NUMBER).
UPDATE `pago` p
  JOIN (SELECT `id`, ROW_NUMBER() OVER (ORDER BY `fecha`, `id`) AS n
          FROM `pago` WHERE `estado` = 'PAGADO') o ON o.`id` = p.`id`
   SET p.`numeroRecibo` = o.n;

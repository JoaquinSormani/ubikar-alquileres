-- Indices para los pagos paginados (pendientes y realizados). Para bases que ya existian.
--   idx_pago_periodo_recibo      (periodo, numeroRecibo)      -> pagos realizados de un mes, del recibo mas nuevo al mas viejo
--   idx_pago_estado_vencimiento  (estado, fechaVencimiento)   -> cobros pendientes, del mas vencido al menos vencido
-- Se puede ejecutar mas de una vez: no falla ni repite un indice que ya esta.
USE `ubikar`;

SET @existe = (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'pago' AND index_name = 'idx_pago_periodo_recibo');
SET @sentencia = IF(@existe = 0,
                    'ALTER TABLE `pago` ADD INDEX `idx_pago_periodo_recibo` (`periodo`, `numeroRecibo`)',
                    'SELECT "El indice idx_pago_periodo_recibo ya existe" AS aviso');
PREPARE s FROM @sentencia;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @existe = (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'pago' AND index_name = 'idx_pago_estado_vencimiento');
SET @sentencia = IF(@existe = 0,
                    'ALTER TABLE `pago` ADD INDEX `idx_pago_estado_vencimiento` (`estado`, `fechaVencimiento`)',
                    'SELECT "El indice idx_pago_estado_vencimiento ya existe" AS aviso');
PREPARE s FROM @sentencia;
EXECUTE s;
DEALLOCATE PREPARE s;

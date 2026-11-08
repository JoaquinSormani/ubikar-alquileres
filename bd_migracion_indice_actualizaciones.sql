-- Indices para las actualizaciones paginadas (Visualizar actualizaciones). Para bases que ya existian.
--   idx_actualizacion_fecha          (fecha)                -> todas las aplicadas, de la mas nueva a la mas vieja
--   idx_actualizacion_periodo_fecha  (periodoDesde, fecha)  -> las aplicadas de un mes, de la mas nueva a la mas vieja
-- (InnoDB agrega la clave primaria a cada indice, asi que tambien cubren el desempate por id.)
-- Se puede ejecutar mas de una vez: no falla ni repite un indice que ya esta.
USE `ubikar`;

SET @existe = (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'actualizacion_valor' AND index_name = 'idx_actualizacion_fecha');
SET @sentencia = IF(@existe = 0,
                    'ALTER TABLE `actualizacion_valor` ADD INDEX `idx_actualizacion_fecha` (`fecha`)',
                    'SELECT "El indice idx_actualizacion_fecha ya existe" AS aviso');
PREPARE s FROM @sentencia;
EXECUTE s;
DEALLOCATE PREPARE s;

SET @existe = (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'actualizacion_valor' AND index_name = 'idx_actualizacion_periodo_fecha');
SET @sentencia = IF(@existe = 0,
                    'ALTER TABLE `actualizacion_valor` ADD INDEX `idx_actualizacion_periodo_fecha` (`periodoDesde`, `fecha`)',
                    'SELECT "El indice idx_actualizacion_periodo_fecha ya existe" AS aviso');
PREPARE s FROM @sentencia;
EXECUTE s;
DEALLOCATE PREPARE s;

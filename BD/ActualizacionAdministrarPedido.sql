-- =====================================================================
-- Actualizacion de la base para el caso de uso Administrar Pedidos
-- Equipo 2
--
-- Es para quien ya tiene la base taqueria_mi_buen creada con el script
-- anterior y no quiere perder sus datos. Solo agrega lo nuevo:
--   - estado PENDIENTE_PAGO en las mesas (color ambar)
--   - estado de preparacion por producto (en lugar de listo si/no)
--   - rondas, clave de envio y datos de cancelacion de cada producto
--   - quien modifico el pedido y cuando
--   - importes con dos decimales exactos (DECIMAL)
--   - la categoria Especialidades
-- Corranlo UNA sola vez. Antes saquen un respaldo, por si las moscas:
--   mysqldump -u root -p taqueria_mi_buen > respaldo.sql
-- =====================================================================
USE taqueria_mi_buen;

-- 1. Mesas: el CHECK viejo solo dejaba DISPONIBLE u OCUPADA.
--    MySQL le pone nombre automatico al CHECK; si este DROP falla, busquen el
--    nombre con SHOW CREATE TABLE mesa; y cambienlo aqui.
ALTER TABLE mesa DROP CHECK mesa_chk_1;
ALTER TABLE mesa MODIFY estado VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE';
ALTER TABLE mesa ADD CONSTRAINT chk_mesa_estado CHECK (estado IN ('DISPONIBLE','OCUPADA','PENDIENTE_PAGO'));

-- 2. Pedido: trazabilidad y clave contra duplicados
ALTER TABLE pedido
  ADD COLUMN id_usuario_modifico INT NULL AFTER fecha_apertura,
  ADD COLUMN fecha_modificacion DATETIME NULL AFTER id_usuario_modifico,
  ADD COLUMN clave_creacion VARCHAR(40) NULL UNIQUE AFTER fecha_modificacion,
  MODIFY total DECIMAL(10,2) NULL,
  ADD CONSTRAINT fk_pedido_modifico FOREIGN KEY (id_usuario_modifico) REFERENCES usuario (id_usuario),
  ADD INDEX idx_pedido_estado (estado);

-- 3. Detalle del pedido: rondas, estado de cocina y cancelacion por producto
ALTER TABLE detalle_pedido
  MODIFY precio_unitario DECIMAL(10,2) NOT NULL,
  ADD COLUMN ronda INT NOT NULL DEFAULT 1 AFTER orden_cola_pedido,
  ADD COLUMN estado_preparacion VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' AFTER ronda,
  ADD COLUMN fecha_envio DATETIME NULL AFTER estado_preparacion,
  ADD COLUMN clave_envio VARCHAR(40) NULL AFTER fecha_envio,
  ADD COLUMN id_usuario_agrego INT NULL AFTER clave_envio,
  ADD COLUMN motivo_cancelacion VARCHAR(150) NULL AFTER id_usuario_agrego,
  ADD COLUMN cancelado_por VARCHAR(100) NULL AFTER motivo_cancelacion,
  ADD COLUMN fecha_cancelacion DATETIME NULL AFTER cancelado_por;

-- Lo que ya estaba listo se pasa al estado nuevo; lo de pedidos cancelados tambien
UPDATE detalle_pedido SET estado_preparacion = 'LISTO' WHERE listo = TRUE;
UPDATE detalle_pedido d JOIN pedido p ON p.id_pedido = d.id_pedido
   SET d.estado_preparacion = 'CANCELADO'
 WHERE p.estado = 'CANCELADO' AND d.listo = FALSE;
UPDATE detalle_pedido d JOIN pedido p ON p.id_pedido = d.id_pedido
   SET d.fecha_envio = p.fecha_apertura, d.id_usuario_agrego = p.id_usuario;

ALTER TABLE detalle_pedido
  DROP COLUMN listo,
  ADD CONSTRAINT chk_detalle_estado CHECK (estado_preparacion IN ('PENDIENTE','EN_PREPARACION','LISTO','CANCELADO')),
  ADD INDEX idx_detalle_estado (estado_preparacion),
  ADD INDEX idx_detalle_envio (id_pedido, clave_envio),
  ADD CONSTRAINT fk_detalle_agrego FOREIGN KEY (id_usuario_agrego) REFERENCES usuario (id_usuario);

-- 4. Importes con dos decimales exactos
ALTER TABLE producto MODIFY precio DECIMAL(10,2) NOT NULL;
ALTER TABLE ingrediente_extra MODIFY precio DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE detalle_extra MODIFY costo_extra DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE pedido_domicilio MODIFY costo_envio DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE pago MODIFY monto DECIMAL(10,2) NOT NULL;

-- 5. Un pedido se cobra una sola vez. Si este falla es porque ya hay un pedido
--    con dos pagos: revisenlo a mano antes de volver a correr esta linea.
ALTER TABLE pago ADD CONSTRAINT uq_pago_pedido UNIQUE (id_pedido);

-- 6. Mesas que quedaron por cobrar: ahora se ven en ambar
UPDATE mesa m SET m.estado = 'PENDIENTE_PAGO'
 WHERE m.estado = 'OCUPADA'
   AND NOT EXISTS (SELECT 1 FROM pedido_local pl JOIN pedido p ON p.id_pedido = pl.id_pedido
                    WHERE pl.id_mesa = m.id_mesa AND p.estado = 'ABIERTO')
   AND EXISTS (SELECT 1 FROM pedido_local pl JOIN pedido p ON p.id_pedido = pl.id_pedido
                WHERE pl.id_mesa = m.id_mesa AND p.estado = 'POR_COBRAR');

-- 7. Categoria Especialidades (el caso de uso pide 4 categorias) y el Lirio se cambia ahi
INSERT INTO categoria (nombre, orden)
SELECT 'Especialidades', 3 FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Especialidades');
UPDATE categoria SET orden = 4 WHERE nombre = 'Bebidas';
UPDATE producto SET id_categoria = (SELECT id_categoria FROM (SELECT id_categoria FROM categoria WHERE nombre = 'Especialidades') c)
 WHERE nombre = 'Lirio';

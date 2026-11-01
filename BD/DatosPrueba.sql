-- =====================================================================
-- Datos de prueba de Taqueria Mi Buen
-- Equipo 2
--
-- Empleados de ejemplo para probar el caso de uso Administrar Pedidos sin
-- tener que dar de alta a nadie. Se corre DESPUES de ScriptBDTaqueriaMiBuen.sql.
-- Los PIN se guardan cifrados con BCrypt (igual que lo hace Cuenta.java),
-- por eso en la tabla no se ve el numero. Aqui abajo va cada PIN para que
-- sepan con cual entrar:
--
--   Nombre            Rol            PIN
--   Laura Mendez      ADMINISTRADOR  1111
--   Carlos Ruiz       CAJERO         2222
--   Jose Nunez        MESERO         3333
--   Maria Lopez       MESERO         4444
--   Pedro Garcia      COCINERO       5555
--   Luis Morales      CAJERO + MESERO + COCINERO   6666
--     (Luis es el ejemplo de alguien que hace de todo: una cuenta con varios roles)
--
-- Ojo: estos PIN son solo para pruebas, en el local cada quien pone el suyo.
-- =====================================================================
USE taqueria_mi_buen;

INSERT INTO usuario (nombre, telefono, fecha_ingreso) VALUES
  ('Laura Mendez', '6441000001', '2026-01-10'),
  ('Carlos Ruiz',  '6441000002', '2026-02-01'),
  ('José Núñez',   '6441000003', '2026-03-15'),
  ('María López',  '6441000004', '2026-03-15'),
  ('Pedro García', '6441000005', '2026-04-20'),
  ('Luis Morales', '6441000006', '2026-01-02');

-- Primero la cuenta con su PIN cifrado y luego sus roles en cuenta_rol
INSERT INTO cuenta (id_usuario, contrasena) VALUES
  ((SELECT id_usuario FROM usuario WHERE nombre = 'Laura Mendez'), '$2a$10$HdfuRsoHDJP4.wqn8QPu9OFMNqxMZynHzmQXd66tQfF9NcnnQD/da'),
  ((SELECT id_usuario FROM usuario WHERE nombre = 'Carlos Ruiz'),  '$2a$10$b17adB55Sck7Y6PSSJEjRuAHlLswMfYoAIc0BXMhHNHv2kwCWBSCu'),
  ((SELECT id_usuario FROM usuario WHERE nombre = 'José Núñez'),   '$2a$10$IB2BT1cz0XBbTXLOgdGOqe2T7ibBrEjp4nSl/A73PkmN/XAmo1Avu'),
  ((SELECT id_usuario FROM usuario WHERE nombre = 'María López'),  '$2a$10$qwofJLW2EsCPKe0ppYSmyerc1HTovUHyNaPvkiRGp9RJETfaLTF8O'),
  ((SELECT id_usuario FROM usuario WHERE nombre = 'Pedro García'), '$2a$10$F0QxMjhvAuS573K40VG8yeZnrX.3g4DSWIbAaREaBXg8u5CoCm8NG'),
  ((SELECT id_usuario FROM usuario WHERE nombre = 'Luis Morales'), '$2a$10$hc/zkPDcPCF8.Bs5bHKXv.b51RxgCXDZbibsTHqN6rnBsrNHIq.xm');

INSERT INTO cuenta_rol (id_cuenta, rol)
SELECT c.id_cuenta, r.rol
FROM cuenta c
JOIN usuario u ON u.id_usuario = c.id_usuario
JOIN (SELECT 'Laura Mendez' AS nombre, 'ADMINISTRADOR' AS rol
      UNION ALL SELECT 'Carlos Ruiz',  'CAJERO'
      UNION ALL SELECT 'José Núñez',   'MESERO'
      UNION ALL SELECT 'María López',  'MESERO'
      UNION ALL SELECT 'Pedro García', 'COCINERO'
      UNION ALL SELECT 'Luis Morales', 'CAJERO'
      UNION ALL SELECT 'Luis Morales', 'MESERO'
      UNION ALL SELECT 'Luis Morales', 'COCINERO') r ON r.nombre = u.nombre;

-- Caja abierta por el cajero, para poder probar el cobro al final del pedido
INSERT INTO caja (id_usuario, monto_inicial)
VALUES ((SELECT id_usuario FROM usuario WHERE nombre = 'Carlos Ruiz'), 500.00);

-- =====================================================================
-- Script de la base de datos de Taqueria Mi Buen (MySQL 8)
-- Equipo 2 · version 2.0 (01/Nov/2026, Jose Alfredo Guzman Moreno)
--
-- Como usarlo: abrir en MySQL Workbench y correr todo de un jalon.
-- Crea la base desde cero con las 8 mesas y un menu de ejemplo.
-- Despues se corre DatosPrueba.sql para tener empleados con su PIN.
--
-- Si ya tenian una base de antes, borrenla primero (DROP DATABASE
-- taqueria_mi_buen;): esta version cambia varias tablas.
--
-- Reglas que se respetan en todo el script:
--   - Todo importe va en DECIMAL(10,2): dos decimales exactos, como en el
--     ticket. Con DOUBLE salen centavos fantasma (requisito 3.5 del CU-02).
--   - Nada se borra: se usan estados o la columna activo (baja logica).
--   - El precio se copia al detalle del pedido al momento de pedirlo, asi un
--     cambio en el menu no altera pedidos anteriores (RN-02).
-- =====================================================================
CREATE DATABASE IF NOT EXISTS taqueria_mi_buen CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE taqueria_mi_buen;

-- ---------------------------------------------------------------------
-- Empleados y acceso (CU-00 Iniciar sesion, CU-08 Administrar Empleados)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
  id_usuario     INT AUTO_INCREMENT PRIMARY KEY,
  nombre         VARCHAR(100) NOT NULL,
  telefono       VARCHAR(15),
  fecha_ingreso  DATE,
  activo         BOOLEAN NOT NULL DEFAULT TRUE
);
-- Una cuenta por empleado. El PIN (4 digitos, unico) se guarda cifrado con BCrypt.
-- contrasena_remota solo la tiene el Administrador: es la que se pide al
-- entrar desde fuera del local (el PIN solo sirve dentro de la red de la taqueria).
CREATE TABLE cuenta (
  id_cuenta          INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario         INT NOT NULL UNIQUE,
  contrasena         VARCHAR(60) NOT NULL,
  contrasena_remota  VARCHAR(60) NULL,
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
-- Una cuenta puede tener varios roles (por ejemplo, alguien que a veces
-- esta en caja y a veces en la parrilla).
CREATE TABLE cuenta_rol (
  id_cuenta  INT NOT NULL,
  rol        VARCHAR(13) NOT NULL CHECK (rol IN ('ADMINISTRADOR','CAJERO','MESERO','COCINERO')),
  PRIMARY KEY (id_cuenta, rol),
  FOREIGN KEY (id_cuenta) REFERENCES cuenta (id_cuenta)
);

-- ---------------------------------------------------------------------
-- Mesas (CU-02)
-- ---------------------------------------------------------------------
CREATE TABLE mesa (
  id_mesa            INT AUTO_INCREMENT PRIMARY KEY,
  numero             INT NOT NULL UNIQUE,
  capacidad          INT NOT NULL DEFAULT 4,
  -- Verde, rojo o ambar en el mapa de mesas
  estado             VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE','OCUPADA','PENDIENTE_PAGO')),
  -- Si la mesa se junto con otra, aqui va la mesa principal del grupo.
  -- Los pedidos siempre viven en la principal.
  id_mesa_principal  INT NULL,
  FOREIGN KEY (id_mesa_principal) REFERENCES mesa (id_mesa)
);

-- ---------------------------------------------------------------------
-- Menu (CU-01 Administrar Menu)
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
  id_categoria  INT AUTO_INCREMENT PRIMARY KEY,
  nombre        VARCHAR(50) NOT NULL UNIQUE,
  orden         INT NOT NULL DEFAULT 0,
  activo        BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE producto (
  id_producto        INT AUTO_INCREMENT PRIMARY KEY,
  id_categoria       INT NOT NULL,
  nombre             VARCHAR(100) NOT NULL,
  descripcion        VARCHAR(255),
  precio             DECIMAL(10,2) NOT NULL,
  -- Precio para pedidos de DiDi Food y Uber Eats; NULL = el mismo precio
  precio_apps        DECIMAL(10,2) NULL,
  -- PIEZA se pide en enteros (1 a 99); KILO admite decimales (0.250, 0.5, 1...)
  unidad_venta       VARCHAR(5) NOT NULL DEFAULT 'PIEZA' CHECK (unidad_venta IN ('PIEZA','KILO')),
  -- TRUE si lo prepara la parrilla y debe salir en el monitor de cocina.
  -- Bebidas, salsas y guacamole los entrega el mesero: no pasan por el monitor.
  pasa_por_parrilla  BOOLEAN NOT NULL DEFAULT TRUE,
  -- disponible = FALSE: se agoto hoy (lo marca la parrilla o el administrador)
  disponible         BOOLEAN NOT NULL DEFAULT TRUE,
  -- activo = FALSE: se dio de baja del menu
  activo             BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (precio >= 0),
  CHECK (precio_apps IS NULL OR precio_apps >= 0),
  FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria)
);
-- Paquetes o combos: un producto que se arma con otros (ej. 3 tacos + refresco).
-- En el monitor de cocina se ven las piezas que le tocan a la parrilla.
CREATE TABLE paquete_componente (
  id_paquete     INT NOT NULL,
  id_componente  INT NOT NULL,
  cantidad       INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id_paquete, id_componente),
  CHECK (cantidad > 0),
  CHECK (id_paquete <> id_componente),
  FOREIGN KEY (id_paquete) REFERENCES producto (id_producto),
  FOREIGN KEY (id_componente) REFERENCES producto (id_producto)
);
-- Opciones de preparacion y extras. precio = 0 es una opcion sin costo
-- ("Sin cebolla"); precio > 0 es un extra con costo ("Tortilla de harina").
CREATE TABLE ingrediente_extra (
  id_extra  INT AUTO_INCREMENT PRIMARY KEY,
  nombre    VARCHAR(60) NOT NULL,
  precio    DECIMAL(10,2) NOT NULL DEFAULT 0,
  activo    BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (precio >= 0)
);
CREATE TABLE producto_extra (
  id_producto_extra INT AUTO_INCREMENT PRIMARY KEY,
  id_producto  INT NOT NULL,
  id_extra     INT NOT NULL,
  UNIQUE (id_producto, id_extra),
  FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
  FOREIGN KEY (id_extra) REFERENCES ingrediente_extra (id_extra)
);

-- ---------------------------------------------------------------------
-- Caja (CU-04 Administrar Caja). Una caja por dia, la comparten varias personas.
-- ---------------------------------------------------------------------
CREATE TABLE caja (
  id_caja         INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario      INT NOT NULL,
  fecha_apertura  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_cierre    DATETIME NULL,
  monto_inicial   DECIMAL(10,2) NOT NULL DEFAULT 0,
  abierta         BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (monto_inicial >= 0),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
-- Corte al cerrar la caja, separado por forma de pago.
-- No se permite hacer el corte si quedan cuentas sin cobrar.
CREATE TABLE corte (
  id_corte             INT AUTO_INCREMENT PRIMARY KEY,
  id_caja              INT NOT NULL UNIQUE,
  id_usuario           INT NOT NULL,
  fecha                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_ventas         DECIMAL(10,2) NOT NULL,
  total_efectivo       DECIMAL(10,2) NOT NULL,
  total_tarjeta        DECIMAL(10,2) NOT NULL,
  total_transferencia  DECIMAL(10,2) NOT NULL,
  -- Lo cobrado por DiDi Food / Uber Eats: no entra a la caja, solo se informa
  total_plataformas    DECIMAL(10,2) NOT NULL,
  total_salidas        DECIMAL(10,2) NOT NULL,
  total_descuentos     DECIMAL(10,2) NOT NULL,
  efectivo_esperado    DECIMAL(10,2) NOT NULL,
  efectivo_contado     DECIMAL(10,2) NOT NULL,
  diferencia           DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (id_caja) REFERENCES caja (id_caja),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

-- ---------------------------------------------------------------------
-- Proveedores, compras e insumos (CU-06 y CU-07)
-- ---------------------------------------------------------------------
CREATE TABLE proveedor (
  id_proveedor  INT AUTO_INCREMENT PRIMARY KEY,
  nombre        VARCHAR(100) NOT NULL,
  telefono      VARCHAR(15),
  correo        VARCHAR(100),
  que_surte     VARCHAR(200),
  activo        BOOLEAN NOT NULL DEFAULT TRUE
);
-- Insumos: solo catalogo (no se llevan existencias, decision del cliente)
CREATE TABLE ingrediente (
  id_ingrediente  INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(80) NOT NULL UNIQUE,
  unidad          VARCHAR(15) NOT NULL,
  -- Proveedor con el que normalmente se compra (puede no tener)
  id_proveedor    INT NULL,
  activo          BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor)
);
-- Compra de insumos. Sin proveedor = compra directa (mercado, super).
-- De contado queda PAGADA; a credito queda PENDIENTE hasta cubrirla con abonos.
CREATE TABLE compra (
  id_compra     INT AUTO_INCREMENT PRIMARY KEY,
  id_proveedor  INT NULL,
  id_usuario    INT NOT NULL,
  fecha         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total         DECIMAL(10,2) NOT NULL DEFAULT 0,
  a_credito     BOOLEAN NOT NULL DEFAULT FALSE,
  estado_pago   VARCHAR(9) NOT NULL DEFAULT 'PAGADA' CHECK (estado_pago IN ('PAGADA','PENDIENTE')),
  nota          VARCHAR(150),
  FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
CREATE TABLE detalle_compra (
  id_detalle_compra INT AUTO_INCREMENT PRIMARY KEY,
  id_compra       INT NOT NULL,
  id_ingrediente  INT NOT NULL,
  cantidad        DECIMAL(10,3) NOT NULL,
  costo_unitario  DECIMAL(10,2) NOT NULL,
  UNIQUE (id_compra, id_ingrediente),
  CHECK (cantidad > 0),
  CHECK (costo_unitario >= 0),
  FOREIGN KEY (id_compra) REFERENCES compra (id_compra),
  FOREIGN KEY (id_ingrediente) REFERENCES ingrediente (id_ingrediente)
);
-- Pagos parciales a una compra a credito ("Liquidar pago")
CREATE TABLE abono_compra (
  id_abono    INT AUTO_INCREMENT PRIMARY KEY,
  id_compra   INT NOT NULL,
  id_usuario  INT NOT NULL,
  monto       DECIMAL(10,2) NOT NULL,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (monto > 0),
  FOREIGN KEY (id_compra) REFERENCES compra (id_compra),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
-- Dinero que sale de la caja durante el turno (hielo, gas, pagar a un proveedor).
-- Si se le pago a una compra, queda ligada. (Por confirmar con el cliente.)
CREATE TABLE salida_caja (
  id_salida   INT AUTO_INCREMENT PRIMARY KEY,
  id_caja     INT NOT NULL,
  id_usuario  INT NOT NULL,
  monto       DECIMAL(10,2) NOT NULL,
  motivo      VARCHAR(150) NOT NULL,
  id_compra   INT NULL,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (monto > 0),
  FOREIGN KEY (id_caja) REFERENCES caja (id_caja),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  FOREIGN KEY (id_compra) REFERENCES compra (id_compra)
);

-- ---------------------------------------------------------------------
-- Configuracion y plataformas de reparto
-- ---------------------------------------------------------------------
-- Valores que el administrador puede cambiar sin tocar el sistema
CREATE TABLE configuracion (
  clave  VARCHAR(40) PRIMARY KEY,
  valor  VARCHAR(100) NOT NULL
);
-- DiDi Food y Uber Eats: su comision sirve para la venta neta del reporte
CREATE TABLE plataforma (
  clave          VARCHAR(10) PRIMARY KEY,
  nombre         VARCHAR(40) NOT NULL,
  comision_pct   DECIMAL(5,2) NOT NULL DEFAULT 0,
  CHECK (comision_pct BETWEEN 0 AND 100)
);

-- ---------------------------------------------------------------------
-- Pedidos (CU-02 Administrar Pedidos, el caso de uso priorizado)
-- ---------------------------------------------------------------------
-- Encabezado de cada pedido (una "cuenta"). Nunca se borra: si se cancela queda CANCELADO.
CREATE TABLE pedido (
  id_pedido            INT AUTO_INCREMENT PRIMARY KEY,
  folio                VARCHAR(20) NOT NULL UNIQUE,
  estado               VARCHAR(10) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','POR_COBRAR','PAGADO','CANCELADO')),
  -- Por donde llego el pedido
  canal                VARCHAR(10) NOT NULL DEFAULT 'MESA'
                       CHECK (canal IN ('MESA','MOSTRADOR','TELEFONO','WHATSAPP','DIDI_FOOD','UBER_EATS')),
  id_usuario           INT NOT NULL,
  fecha_apertura       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Quien le movio al ultimo y cuando (requisito 3.6 del caso de uso)
  id_usuario_modifico  INT NULL,
  fecha_modificacion   DATETIME NULL,
  -- Clave del primer envio; evita pedidos repetidos al usar "Reintentar"
  clave_creacion       VARCHAR(40) NULL UNIQUE,
  -- Descuento a toda la cuenta (CU-05). Hasta el limite configurado lo da el
  -- cajero; arriba del limite se guarda quien lo autorizo.
  descuento            DECIMAL(10,2) NOT NULL DEFAULT 0,
  motivo_descuento     VARCHAR(150) NULL,
  id_usuario_descuento INT NULL,
  total                DECIMAL(10,2) NULL,
  CHECK (descuento >= 0),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  FOREIGN KEY (id_usuario_modifico) REFERENCES usuario (id_usuario),
  FOREIGN KEY (id_usuario_descuento) REFERENCES usuario (id_usuario),
  INDEX idx_pedido_estado (estado)
);
CREATE TABLE pedido_local (
  id_pedido        INT PRIMARY KEY,
  id_mesa          INT NOT NULL,
  numero_personas  INT NULL,
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
  FOREIGN KEY (id_mesa) REFERENCES mesa (id_mesa)
);
CREATE TABLE pedido_para_llevar (
  id_pedido       INT PRIMARY KEY,
  nombre_cliente  VARCHAR(100) NOT NULL,
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido)
);
-- El costo de envio depende de la zona: lo captura quien toma el pedido.
-- Reparte un empleado (PROPIO) o un servicio externo como DiDi o Uber (EXTERNO).
CREATE TABLE pedido_domicilio (
  id_pedido        INT PRIMARY KEY,
  costo_envio      DECIMAL(10,2) NOT NULL DEFAULT 0,
  tipo_repartidor  VARCHAR(7) NOT NULL DEFAULT 'PROPIO' CHECK (tipo_repartidor IN ('PROPIO','EXTERNO')),
  repartidor       VARCHAR(100) NULL,
  fecha_entrega    DATETIME NULL,
  CHECK (costo_envio >= 0),
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido)
);
CREATE TABLE datos_entrega (
  id_pedido       INT PRIMARY KEY,
  nombre_cliente  VARCHAR(100) NOT NULL,
  telefono        VARCHAR(10) NOT NULL,
  direccion       VARCHAR(200) NOT NULL,
  referencias     VARCHAR(200),
  FOREIGN KEY (id_pedido) REFERENCES pedido_domicilio (id_pedido)
);
CREATE TABLE cancelacion (
  id_cancelacion  INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido       INT NOT NULL UNIQUE,
  motivo          VARCHAR(150) NOT NULL,
  autorizada_por  VARCHAR(100) NULL,
  fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido)
);
-- Bitacora de cambios de mesa: de donde a donde, quien y cuando
CREATE TABLE cambio_mesa (
  id_cambio       INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido       INT NOT NULL,
  id_mesa_origen  INT NOT NULL,
  id_mesa_destino INT NOT NULL,
  id_usuario      INT NOT NULL,
  fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
  FOREIGN KEY (id_mesa_origen) REFERENCES mesa (id_mesa),
  FOREIGN KEY (id_mesa_destino) REFERENCES mesa (id_mesa),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
-- Pagos (CU-05). Un pedido puede tener varios: pago mixto (parte efectivo y
-- parte tarjeta) o cuenta dividida por consumo (cada persona = una "parte").
CREATE TABLE pago (
  id_pago     INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido   INT NOT NULL,
  id_caja     INT NULL,
  -- Quien cobro (la caja la comparten varias personas)
  id_usuario  INT NOT NULL,
  metodo      VARCHAR(13) NOT NULL CHECK (metodo IN ('EFECTIVO','TARJETA','TRANSFERENCIA','PLATAFORMA')),
  monto       DECIMAL(10,2) NOT NULL,
  -- Solo en efectivo: con cuanto pago el cliente, para calcular el cambio
  recibido    DECIMAL(10,2) NULL,
  -- Numero de persona cuando la cuenta se divide; NULL = toda la cuenta
  parte       INT NULL,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (monto > 0),
  -- Lo de las plataformas no pasa por la caja; todo lo demas si
  CHECK ((metodo = 'PLATAFORMA') OR (id_caja IS NOT NULL)),
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
  FOREIGN KEY (id_caja) REFERENCES caja (id_caja),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  INDEX idx_pago_pedido (id_pedido)
);
-- Cada producto del pedido. Tambien es la cola del monitor de cocina.
CREATE TABLE detalle_pedido (
  id_detalle          INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido           INT NOT NULL,
  id_producto         INT NOT NULL,
  -- Piezas (enteros) o kilos (con decimales), segun la unidad del producto
  cantidad            DECIMAL(7,3) NOT NULL,
  -- Precio congelado al momento de pedirlo (RN-02); si el pedido es de una
  -- app se toma el precio de apps
  precio_unitario     DECIMAL(10,2) NOT NULL,
  nota_cocina         VARCHAR(150) NULL,
  -- Turno en la cola de cocina, unico para todos los pedidos
  orden_cola_pedido   INT NOT NULL,
  -- 1 = primer envio a cocina, 2 = lo que se pidio despues, etc.
  ronda               INT NOT NULL DEFAULT 1,
  estado_preparacion  VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado_preparacion IN ('PENDIENTE','EN_PREPARACION','LISTO','CANCELADO')),
  fecha_envio         DATETIME NULL,
  clave_envio         VARCHAR(40) NULL,
  id_usuario_agrego   INT NULL,
  motivo_cancelacion  VARCHAR(150) NULL,
  cancelado_por       VARCHAR(100) NULL,
  fecha_cancelacion   DATETIME NULL,
  -- Cortesia o comida de empleado: no se cobra pero queda registrada (CU-05)
  cortesia            BOOLEAN NOT NULL DEFAULT FALSE,
  cortesia_para       VARCHAR(100) NULL,
  -- A que persona le toca cuando la cuenta se divide por consumo
  parte               INT NULL,
  CHECK (cantidad > 0 AND cantidad <= 99),
  UNIQUE (orden_cola_pedido),
  INDEX idx_detalle_estado (estado_preparacion),
  INDEX idx_detalle_envio (id_pedido, clave_envio),
  FOREIGN KEY (id_usuario_agrego) REFERENCES usuario (id_usuario),
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
  FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
);
CREATE TABLE detalle_extra (
  id_detalle_extra INT AUTO_INCREMENT PRIMARY KEY,
  id_detalle  INT NOT NULL,
  id_extra    INT NOT NULL,
  costo_extra DECIMAL(10,2) NOT NULL DEFAULT 0,
  UNIQUE (id_detalle, id_extra),
  FOREIGN KEY (id_detalle) REFERENCES detalle_pedido (id_detalle),
  FOREIGN KEY (id_extra) REFERENCES ingrediente_extra (id_extra)
);
-- Pantallas de cocina; el latido dice si estan conectadas. Si ninguna dio
-- latido en 5 segundos se imprime el ticket de respaldo.
CREATE TABLE terminal_kds (
  id_terminal    INT AUTO_INCREMENT PRIMARY KEY,
  nombre         VARCHAR(50) NOT NULL,
  ultimo_latido  DATETIME NULL
);

-- =====================================================================
-- Datos iniciales
-- =====================================================================
INSERT INTO mesa (numero, capacidad) VALUES
  (1,4),(2,4),(3,4),(4,4),(5,4),(6,4),(7,4),(8,4);
INSERT INTO configuracion (clave, valor) VALUES ('LIMITE_DESCUENTO_PCT', '10');
-- La comision real de cada app esta por confirmar con el cliente
INSERT INTO plataforma (clave, nombre, comision_pct) VALUES
  ('DIDI_FOOD', 'DiDi Food', 0), ('UBER_EATS', 'Uber Eats', 0);
-- Las 4 categorias que pide el caso de uso (Tacos, Quesadillas, Especialidades y Bebidas)
INSERT INTO categoria (nombre, orden) VALUES ('Tacos',1),('Quesadillas',2),('Especialidades',3),('Bebidas',4);
-- Precios de ejemplo tomados de los mockups; el dueno captura los reales
INSERT INTO producto (id_categoria, nombre, descripcion, precio, precio_apps, unidad_venta, pasa_por_parrilla) VALUES
  (1,'Taco de asada','Carne al carbón',40.00,52.00,'PIEZA',TRUE),
  (1,'Asada doble carne','Doble porción de carne',75.00,86.00,'PIEZA',TRUE),
  (2,'Quesadilla asada','Queso Oaxaca y carne asada',100.00,104.00,'PIEZA',TRUE),
  (3,'Lirio','Especialidad de la casa',115.00,132.00,'PIEZA',TRUE),
  (3,'Carne asada por kilo','Para llevar, con tortillas y salsas',420.00,NULL,'KILO',TRUE),
  (4,'Coca-Cola vidrio 355 ml','Bien fría',25.00,35.00,'PIEZA',FALSE),
  (3,'Orden de guacamole','Lo prepara cocina',40.00,46.00,'PIEZA',FALSE),
  (3,'Paquete 3 tacos y refresco','3 tacos de asada y una Coca-Cola',150.00,172.00,'PIEZA',TRUE);
INSERT INTO paquete_componente (id_paquete, id_componente, cantidad) VALUES (8,1,3),(8,6,1);
INSERT INTO ingrediente_extra (nombre, precio) VALUES
  ('Tortilla de harina',5.00),('Queso extra',15.00),('Carne extra',30.00),('Aguacate',12.00),
  ('Sin cebolla',0),('Sin salsa',0),('Salsa aparte',0);
INSERT INTO producto_extra (id_producto, id_extra) VALUES
  (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(2,1),(2,2),(2,4),(2,5),(2,6),(3,2),(3,3),(3,7),(4,2);
INSERT INTO proveedor (nombre, telefono, que_surte) VALUES
  ('Carnes del Valle','6441110001','Carne de res'),('Tortillería La Esperanza','6441110002','Tortillas'),
  ('Distribuidora de refrescos','6441110003','Refrescos');
INSERT INTO ingrediente (nombre, unidad, id_proveedor) VALUES
  ('Carne asada','kg',1),('Tortilla de maíz','pza',2),('Tortilla de harina','pza',2),
  ('Queso Oaxaca','kg',NULL),('Aguacate','kg',NULL),('Coca-Cola vidrio 355 ml','pza',3);
INSERT INTO terminal_kds (nombre) VALUES ('Monitor Parrilla');

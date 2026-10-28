-- =====================================================================
-- Script de la base de datos de Taqueria Mi Buen (MySQL 8)
-- Equipo 2
--
-- Como usarlo: abrir en MySQL Workbench y correr todo de un jalon.
-- Crea la base desde cero con las 8 mesas y el menu de ejemplo.
-- Si ya tienen la base de antes con datos, NO corran este: usen
-- ActualizacionAdministrarPedido.sql, que solo agrega lo nuevo.
--
-- Los importes van en DECIMAL(10,2): dos decimales exactos, como en el
-- ticket. Con DOUBLE a veces salen centavos fantasma (requisito 3.5).
-- =====================================================================
CREATE DATABASE IF NOT EXISTS taqueria_mi_buen CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE taqueria_mi_buen;

CREATE TABLE usuario (
  id_usuario     INT AUTO_INCREMENT PRIMARY KEY,
  nombre         VARCHAR(100) NOT NULL,
  telefono       VARCHAR(15),
  fecha_ingreso  DATE,
  activo         BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE cuenta (
  id_cuenta   INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario  INT NOT NULL UNIQUE,
  contrasena  VARCHAR(60) NOT NULL,
  rol VARCHAR(13) NOT NULL CHECK (rol IN ('ADMINISTRADOR','CAJERO','MESERO','COCINERO')),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
CREATE TABLE mesa (
  id_mesa    INT AUTO_INCREMENT PRIMARY KEY,
  numero     INT NOT NULL UNIQUE,
  capacidad  INT NOT NULL DEFAULT 4,
  -- Verde, rojo o ambar en el mapa de mesas
  estado     VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE','OCUPADA','PENDIENTE_PAGO'))
);
CREATE TABLE categoria (
  id_categoria  INT AUTO_INCREMENT PRIMARY KEY,
  nombre        VARCHAR(50) NOT NULL UNIQUE,
  orden         INT NOT NULL DEFAULT 0
);
CREATE TABLE producto (
  id_producto   INT AUTO_INCREMENT PRIMARY KEY,
  id_categoria  INT NOT NULL,
  nombre        VARCHAR(100) NOT NULL,
  descripcion   VARCHAR(255),
  precio        DECIMAL(10,2) NOT NULL,
  disponible    BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (precio >= 0),
  FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria)
);
CREATE TABLE ingrediente (
  id_ingrediente  INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(80) NOT NULL UNIQUE,
  unidad          VARCHAR(15) NOT NULL,
  existencia      DOUBLE NOT NULL DEFAULT 0,
  stock_minimo    DOUBLE NOT NULL DEFAULT 0
);
CREATE TABLE producto_ingrediente (
  id_producto_ingrediente INT AUTO_INCREMENT PRIMARY KEY,
  id_producto     INT NOT NULL,
  id_ingrediente  INT NOT NULL,
  cantidad        DOUBLE NOT NULL,
  UNIQUE (id_producto, id_ingrediente),
  FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
  FOREIGN KEY (id_ingrediente) REFERENCES ingrediente (id_ingrediente)
);
CREATE TABLE ingrediente_extra (
  id_extra        INT AUTO_INCREMENT PRIMARY KEY,
  id_ingrediente  INT NULL,
  nombre          VARCHAR(60) NOT NULL,
  precio          DECIMAL(10,2) NOT NULL DEFAULT 0,
  FOREIGN KEY (id_ingrediente) REFERENCES ingrediente (id_ingrediente)
);
CREATE TABLE producto_extra (
  id_producto_extra INT AUTO_INCREMENT PRIMARY KEY,
  id_producto  INT NOT NULL,
  id_extra     INT NOT NULL,
  UNIQUE (id_producto, id_extra),
  FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
  FOREIGN KEY (id_extra) REFERENCES ingrediente_extra (id_extra)
);
CREATE TABLE caja (
  id_caja         INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario      INT NOT NULL,
  fecha_apertura  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_cierre    DATETIME NULL,
  monto_inicial   DOUBLE NOT NULL DEFAULT 0,
  abierta         BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
CREATE TABLE corte (
  id_corte           INT AUTO_INCREMENT PRIMARY KEY,
  id_caja            INT NOT NULL UNIQUE,
  fecha              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_ventas       DOUBLE NOT NULL,
  efectivo_esperado  DOUBLE NOT NULL,
  efectivo_contado   DOUBLE NOT NULL,
  diferencia         DOUBLE NOT NULL,
  FOREIGN KEY (id_caja) REFERENCES caja (id_caja)
);
-- Encabezado de cada pedido. Nunca se borra: si se cancela queda CANCELADO.
CREATE TABLE pedido (
  id_pedido            INT AUTO_INCREMENT PRIMARY KEY,
  folio                VARCHAR(20) NOT NULL UNIQUE,
  estado               VARCHAR(10) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','POR_COBRAR','PAGADO','CANCELADO')),
  id_usuario           INT NOT NULL,
  fecha_apertura       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Quien le movio al ultimo y cuando (requisito 3.6 del caso de uso)
  id_usuario_modifico  INT NULL,
  fecha_modificacion   DATETIME NULL,
  -- Clave del primer envio; evita pedidos repetidos al usar "Reintentar"
  clave_creacion       VARCHAR(40) NULL UNIQUE,
  total                DECIMAL(10,2) NULL,
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  FOREIGN KEY (id_usuario_modifico) REFERENCES usuario (id_usuario),
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
CREATE TABLE pedido_domicilio (
  id_pedido      INT PRIMARY KEY,
  costo_envio    DECIMAL(10,2) NOT NULL DEFAULT 0,
  fecha_entrega  DATETIME NULL,
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
CREATE TABLE pago (
  id_pago    INT AUTO_INCREMENT PRIMARY KEY,
  -- Un pedido se cobra una sola vez
  id_pedido  INT NOT NULL UNIQUE,
  id_caja    INT NOT NULL,
  metodo     VARCHAR(20) NOT NULL,
  monto      DECIMAL(10,2) NOT NULL,
  fecha      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (monto > 0),
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
  FOREIGN KEY (id_caja) REFERENCES caja (id_caja)
);
-- Cada producto del pedido. Tambien es la cola de la cocina.
CREATE TABLE detalle_pedido (
  id_detalle          INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido           INT NOT NULL,
  id_producto         INT NOT NULL,
  cantidad            INT NOT NULL,
  -- Precio congelado al momento de pedirlo (RN-02)
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
  CHECK (cantidad BETWEEN 1 AND 99),
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
CREATE TABLE reporte_venta (
  id_reporte      INT AUTO_INCREMENT PRIMARY KEY,
  fecha_inicio    DATETIME NOT NULL,
  fecha_fin       DATETIME NOT NULL,
  total_ventas    DOUBLE NOT NULL DEFAULT 0,
  numero_pedidos  INT NOT NULL DEFAULT 0,
  CHECK (fecha_fin >= fecha_inicio)
);
CREATE TABLE reporte_venta_pedido (
  id_reporte_venta_pedido INT AUTO_INCREMENT PRIMARY KEY,
  id_reporte  INT NOT NULL,
  id_pedido   INT NOT NULL,
  UNIQUE (id_reporte, id_pedido),
  FOREIGN KEY (id_reporte) REFERENCES reporte_venta (id_reporte),
  FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido)
);
CREATE TABLE proveedor (
  id_proveedor  INT AUTO_INCREMENT PRIMARY KEY,
  nombre        VARCHAR(100) NOT NULL,
  telefono      VARCHAR(15),
  correo        VARCHAR(100)
);
CREATE TABLE compra (
  id_compra     INT AUTO_INCREMENT PRIMARY KEY,
  id_proveedor  INT NOT NULL,
  id_usuario    INT NOT NULL,
  fecha         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total         DOUBLE NOT NULL DEFAULT 0,
  FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor),
  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
CREATE TABLE detalle_compra (
  id_detalle_compra INT AUTO_INCREMENT PRIMARY KEY,
  id_compra       INT NOT NULL,
  id_ingrediente  INT NOT NULL,
  cantidad        DOUBLE NOT NULL,
  costo_unitario  DOUBLE NOT NULL,
  UNIQUE (id_compra, id_ingrediente),
  CHECK (cantidad > 0),
  FOREIGN KEY (id_compra) REFERENCES compra (id_compra),
  FOREIGN KEY (id_ingrediente) REFERENCES ingrediente (id_ingrediente)
);
-- Pantallas de cocina; el latido dice si estan conectadas
CREATE TABLE terminal_kds (
  id_terminal    INT AUTO_INCREMENT PRIMARY KEY,
  nombre         VARCHAR(50) NOT NULL,
  ultimo_latido  DATETIME NULL
);
INSERT INTO mesa (numero, capacidad) VALUES
  (1,4),(2,4),(3,4),(4,4),(5,4),(6,4),(7,4),(8,4);
-- Las 4 categorias que pide el caso de uso (Tacos, Quesadillas, Especialidades y Bebidas)
INSERT INTO categoria (nombre, orden) VALUES ('Tacos',1),('Quesadillas',2),('Especialidades',3),('Bebidas',4);
INSERT INTO producto (id_categoria, nombre, descripcion, precio) VALUES
  (1,'Taco de asada','Carne al carbón',45.00),
  (1,'Asada doble carne','Doble porción de carne',75.00),
  (2,'Quesadilla asada','Queso Oaxaca y carne asada',90.00),
  (3,'Lirio','Especialidad de la casa',115.00),
  (4,'Coca-Cola vidrio 355 ml','Bien fría',35.00);
INSERT INTO ingrediente (nombre, unidad, existencia, stock_minimo) VALUES
  ('Carne asada','kg',10,2),('Tortilla de maíz','pza',500,100),('Tortilla de harina','pza',200,50),
  ('Queso Oaxaca','kg',5,1),('Aguacate','kg',4,1);
INSERT INTO producto_ingrediente (id_producto, id_ingrediente, cantidad) VALUES
  (1,1,0.080),(1,2,1),(2,1,0.160),(2,2,1),(3,1,0.100),(3,3,1),(3,4,0.080);
INSERT INTO ingrediente_extra (id_ingrediente, nombre, precio) VALUES
  (3,'Tortilla de harina',5.00),(4,'Queso extra',15.00),(1,'Carne extra',30.00),(5,'Aguacate',12.00);
INSERT INTO producto_extra (id_producto, id_extra) VALUES
  (1,1),(1,2),(1,3),(1,4),(2,1),(2,2),(2,4),(3,2),(3,3),(4,2);
INSERT INTO terminal_kds (nombre) VALUES ('KDS Cocina');
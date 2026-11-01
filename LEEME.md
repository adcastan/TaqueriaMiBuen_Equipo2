# Taquería Mi Buen - Caso de uso Administrar Pedidos (CU-02)

Equipo 2 · Iteración de Elaboración E1 · versión 2.0 (01/Nov/2026, José Alfredo Guzmán Moreno)

Este paquete trae el proyecto `ProyectoTaqueriaMiBuen` (Java por capas, 5 módulos Maven) con el caso de uso priorizado **CU-02 Administrar Pedidos** terminado, y la carpeta `BD` con los scripts de la base. **Solo se desarrolló CU-02**; los demás casos de uso (CU-00 a CU-10, sin el CU-11 que se eliminó) están documentados y la base de datos ya los soporta, para que el siguiente equipo los construya con el mismo patrón.

La versión 2.0 deja el código, la base y los documentos diciendo lo mismo. Las decisiones que se tomaron están en el punto 5.

---

## 1. Cómo correrlo

Se necesita: JDK 17 (o más nuevo), Apache Tomcat 10.1, MySQL 8 y NetBeans.

1. **Base de datos.** En MySQL Workbench corran, en este orden:
   - `BD/ScriptBDTaqueriaMiBuen.sql`: crea la base desde cero (30 tablas) con las 8 mesas y un menú de ejemplo.
   - `BD/DatosPrueba.sql`: crea seis empleados de prueba y deja una caja abierta.

   Si ya tenían la base de una versión anterior, bórrenla primero (`DROP DATABASE taqueria_mi_buen;`): la 2.0 cambia varias tablas. El script de actualización viejo ya no aplica y se quitó.
2. **Conexión.** En `Persistencia/src/main/resources/META-INF/persistence.xml` pongan el usuario y la contraseña de su MySQL.
3. **Compilar.** Abran en NetBeans la carpeta `ProyectoTaqueriaMiBuen` (el proyecto padre) y denle **Clean and Build** al padre. Arma los cinco módulos en orden y deja el WAR en `Presentacion/target/TaqueriaMiBuen.war`.
4. **Correr.** Denle Run al módulo Presentacion con Tomcat 10.1, o copien el WAR a `tomcat/webapps`. Se abre en `http://localhost:8080/TaqueriaMiBuen/`.
5. **Abran el monitor de cocina** (entren con el PIN del cocinero) antes de mandar pedidos. Si no hay ningún monitor conectado, el sistema avisa «Error de conexión con cocina» y manda el ticket de respaldo, como pide el caso de uso.

Empleados de prueba (los PIN se guardan cifrados con BCrypt). Una cuenta puede tener varios roles:

| Nombre | Roles | PIN |
|---|---|---|
| Laura Mendez | Administrador | 1111 |
| Carlos Ruiz | Cajero | 2222 |
| José Núñez | Mesero | 3333 |
| María López | Mesero | 4444 |
| Pedro García | Cocinero | 5555 |
| Luis Morales | Cajero, Mesero y Cocinero | 6666 |

---

## 2. Qué hace el caso de uso (Detallado CU-02 v1.2)

| Parte del caso de uso | Qué hace el sistema |
|---|---|
| Flujo básico | El mesero elige una mesa libre, indica el número de personas, agrega productos y le da «Enviar a cocina». El pedido se guarda con folio, fecha, mesa, personas y mesero. Lo de parrilla llega al monitor «En cola», la mesa se pone en rojo, sale «Comanda enviada a cocina» y la pantalla regresa al mapa de mesas. |
| Tipar pedido y canal | Comer aquí pide de 1 a 20 personas. Para llevar pide el nombre del cliente y por dónde llegó: mostrador, teléfono, WhatsApp, DiDi Food o Uber Eats. A domicilio pide nombre, teléfono de 10 dígitos, dirección, por dónde llegó (teléfono o WhatsApp), el costo de envío según la zona y quién lo lleva (empleado o externo). Se valida en la pantalla **y** en el servidor. |
| Precio de apps | Si el pedido viene de DiDi Food o Uber Eats se usa el precio de apps de cada producto. El precio se congela al agregarlo. |
| Agregar producto | Ventana con la cantidad, los extras con su costo y una nota para cocina de máximo 150 letras. Lo que se vende por pieza va de 1 a 99 (entero); lo de kilo se pide con botones de 1/4, 1/2 y 1 kg o escribiendo los kilos. Los paquetes muestran qué incluyen. |
| Productos que no son de parrilla | Bebidas, guacamole y salsas no van al monitor: quedan «Listo» desde que se envían y los entrega el mesero. |
| Modificar pedido | Ticket agrupado por rondas con la hora y el estado de cada producto. Lo que sigue en cola se puede cambiar o quitar y sale solito del monitor. Lo que ya se prepara solo lo cancela un cajero o administrador, con PIN y motivo. |
| Cambiar de mesa | Desde la cuenta, «Cambiar de mesa» deja tocar solo las mesas libres. Pasa todo (productos, rondas y lo que está en parrilla) y queda en la bitácora quién lo hizo y a qué hora. |
| Juntar mesas | «Juntar con otra mesa» pega una mesa libre a la de la cuenta. En el mapa sale del mismo color con «Junto a mesa N». Al pagar o cancelar todas las cuentas, las mesas se liberan solas. También se puede «Separar mesa». |
| Cuentas separadas | Una mesa (o un grupo de mesas) puede tener varias cuentas; la pantalla muestra botones para pasar de una a otra. |
| Cerrar pedido | Cerrar = el cliente pidió la cuenta. El pedido pasa a «Por cobrar», la mesa se pone en ámbar y ya no acepta productos. El cajero cobra y la mesa vuelve a verde. |
| Cancelar pedido | Si lo cancela un mesero pide el PIN de un cajero o administrador y el motivo. Se guardan el motivo, quién autorizó y la hora. Nunca se borra nada. Si el PIN no sirve sale «Autorizacion no valida». |
| Producto agotado | Desde el monitor de cocina, el botón «Se acabó…» marca un producto como agotado (lo puede hacer la parrilla o el administrador). El mesero lo ve gris y no lo puede agregar; si se agota mientras captura, al enviar sale «Producto no disponible». Un paquete se agota solo si se acaba una de sus piezas. |
| Salir sin enviar, pedido sin productos, datos incompletos | Pregunta antes de salir con productos nuevos; «Enviar a cocina» se apaga sin productos; los campos obligatorios se ponen en rojo con «Campo obligatorio». |
| Falla de conexión | Sale «Sin conexion. El pedido no se ha enviado» con «Reintentar»; reintentar manda la misma clave y el servidor no duplica la comanda. Si el monitor de cocina no está conectado se imprime el ticket de respaldo. |
| Concurrencia | Dos meseros no pueden abrir la misma mesa al mismo tiempo y los turnos de cocina no se repiten. |
| Importes | Todo en DECIMAL(10,2) en la base y BigDecimal en Java. |
| Roles | Una cuenta puede tener varios roles; el menú muestra todo lo que sus roles permiten. Una cajera que cubre de mesera entra con su mismo PIN: el rol Cajero ya puede tomar pedidos. |

---

## 3. Cambios de la versión 2.0

**Base de datos (`BD/ScriptBDTaqueriaMiBuen.sql`, 30 tablas)**
- Todo importe en DECIMAL(10,2).
- `cuenta_rol` (varios roles por cuenta) y `contrasena_remota` para el administrador.
- `mesa.id_mesa_principal` (mesas juntas) y `cambio_mesa` (bitácora).
- `pedido.canal`, descuento (para CU-05); `pedido_domicilio` con tipo de repartidor y repartidor.
- `producto` con `precio_apps`, `unidad_venta` (PIEZA o KILO), `pasa_por_parrilla` y `activo`; `paquete_componente` para los combos.
- Insumos solo como catálogo (sin existencias ni receta), compras con proveedor opcional y abonos, salidas de caja, corte por forma de pago, `configuracion` (límite de descuento) y `plataforma` (comisión de cada app). Estas tablas las usará el siguiente equipo.
- Se quitaron `producto_ingrediente`, `reporte_venta` y `reporte_venta_pedido`.

**Código**
- Dominio: dinero en BigDecimal (`Herramientas.Dinero`), enums `CanalPedido`, `UnidadVenta`, `TipoRepartidor` y `MetodoPago`, entidades `CambioMesa` y `PaqueteComponente`, cuenta con varios roles.
- Persistencia: `PedidoDAO.cambiarMesa` y `listarActivosPorMesa`; `MesaDAO.juntarMesas`, `separarMesa` y `recalcularEstado` (le pasa el color a las mesas unidas y deshace el grupo al quedar libre).
- Negocio: `PedidoService` con canal, precio de apps, kilos, envío y repartidor, cambiar y juntar mesas; `MenuService.obtenerProductosDeParrilla`; PIN de 4 dígitos.
- Control: rutas `POST /api/pedidos/{id}/mesa`, `GET /api/pedidos?cuentasMesa=`, `POST /api/mesas/{id}/juntar` y `/separar`, `GET /api/kds/productos` y `POST /api/kds/productos/{id}/disponible`. La sesión guarda los roles separados por comas.
- Se eliminó todo lo del reporte de rendimiento de empleados (CU-11), por indicación del profesor.
- Se quitaron del repositorio las carpetas `target` (son archivos compilados; el `.gitignore` ya las ignora).

---

## 4. Errores que traía Avance-4 y ya se corrigieron

1. **El mesero podía entrar a Cobro.** `FiltroSeguridad` calculaba si la ruta era de caja pero nunca usaba ese dato, así que un mesero abría `cobro.jsp` y `/api/cobros` escribiendo la ruta.
2. **Turnos de cocina repetidos.** El turno salía de `MAX(...) + 1` sin apartarlo. Con dos meseros enviando al mismo tiempo chocaba la llave única y uno de los envíos fallaba.
3. **Comandas duplicadas al reintentar.** No había nada que reconociera un mismo envío repetido.
4. **Acentos rotos.** `Json.leer` no indicaba UTF-8, y en Tomcat un nombre como «José Ñúñez» llegaba con letras raras.
5. **El pom padre no armaba el proyecto.** Solo listaba Dominio y Persistencia, y los módulos pedían Java 24 mientras Presentacion compilaba para Java 11.
6. **Cocina lenta.** El monitor consultaba cada 5 segundos; el requisito pide que la comanda llegue en 2 como máximo y el diseño dice cada segundo.
7. **Mapa de mesas lento.** Se actualizaba cada 10 segundos y el diseño dice 3. Ahora además no se enciman consultas si la red anda lenta.
8. **No había forma de marcar «En preparacion»** en cocina, así que la regla 2.2.10 no se podía cumplir.
9. **Folio repetible.** Usaba la hora con milisegundos y dos pedidos en el mismo milisegundo chocaban. Se le agregaron dos dígitos.
10. **Sesión.** Al iniciar sesión no se cambiaba el id de la sesión; ahora sí.
11. **Pagos dobles.** Un pedido no se puede cobrar dos veces: el cobro bloquea el pedido y solo acepta los que están POR_COBRAR. (En la versión 2.0 se quitó el UNIQUE de `pago` porque un pedido podrá tener varios pagos cuando se haga el pago mixto; se probó que tres cobros al mismo tiempo dejan un solo pago.)
12. **Persistencia sobrante.** Sobraba un `persistence.xml` vacío de la plantilla de NetBeans dentro de Presentacion; se quitó.
13. **Errores en HTML.** Si Tomcat contestaba con su página de error en HTML, la pantalla tronaba al leerla; ahora muestra un mensaje.
14. **Cobro mostraba lo cancelado.** La ventana de cobro y el ticket de pago listaban también los productos cancelados (aunque el total sí estaba bien). Ahora solo salen los que se cobran.

---

## 5. Decisiones tomadas (antes eran inconsistencias entre documentos)

Se revisaron todos los documentos contra el código y se decidió lo siguiente. Los documentos ya están corregidos con esto.

1. **Tecnología:** Java 17 + JPA (Hibernate) en 5 módulos Maven, MySQL 8 y Tomcat 10.1. La Arquitectura y el Modelo decían JDBC y un solo WAR; se corrigieron.
2. **Caso de uso que manda:** el Detallado de CU-02, ahora versión 1.2 (se recuperó su historial 1.0 y 1.1).
3. **Base de datos:** la del script de este paquete (30 tablas). Sustituye al script de 12 tablas que traía el Modelo.
4. **Flujo de la mesa:** cerrar y luego cobrar. Disponible → Ocupada → Pendiente de pago → Disponible.
5. **Cancelaciones:** las autoriza un cajero o el administrador con su PIN.
6. **Monitor de cocina caído:** ticket de respaldo; y si no hay servidor, «Reintentar».
7. **Casos de uso:** CU-00 a CU-10 con ID. Se eliminó CU-11 (rendimiento de empleados).
8. **Despliegue:** el sistema corre en una laptop en la caja (la taquería sigue operando sin internet) y el dueño lo consulta desde fuera por un túnel seguro; desde internet solo entra el administrador con contraseña. Hay respaldo automático de la base en la nube.
9. **Negocio:** monitor solo de parrilla; agotado lo marca la parrilla o el administrador; menú por pieza, kilo y paquete; precio de apps con comisión; envío según la zona; insumos solo catálogo; compras con abonos; descuentos hasta un límite configurable (inicia en 10%); cortesías registradas; no hay corte con cuentas pendientes; factura y propinas fuera del sistema.

**Por confirmar con el cliente:** si las salidas de caja se pagan con dinero de la caja, el porcentaje de comisión de cada app y los precios reales de combos y venta por kilo.

---

## 6. Qué se probó y cómo salió

Todo se probó con MySQL 8 y Tomcat 10.1 de verdad y el WAR compilado de este mismo código. Hibernate validó todas las entidades contra las 30 tablas.

- **Pruebas de aceptación de CU-02 por la API: 45 de 45 pasaron.** Cubren el flujo básico con importes exactos (incluye medio kilo y un paquete), reintento sin duplicar, bebidas que no pasan por parrilla, piezas del paquete en el monitor, mesa ocupada y cuenta separada, cantidades por pieza y por kilo, bloqueo de lo que ya se prepara, agotado desde la parrilla (y que un mesero no pueda marcarlo), paquete agotado por su pieza, juntar, separar y cambiar de mesa, pedido de DiDi con precio de apps, domicilio con envío por zona y repartidor externo, cerrar, cancelar con PIN válido e inválido, varios roles en una cuenta y dos meseros abriendo la misma mesa al mismo tiempo.
- **Ciclo completo:** pedir la cuenta → mesa en ámbar → cobrar → mesa libre. Tres cobros al mismo tiempo del mismo pedido dejan un solo pago.
- **En el navegador** (tablet 1024x768, escritorio 1440x900 y celular 390x844) se recorrieron: mapa de mesas, captura con extras y kilos, envío a cocina, juntar mesas, para llevar por DiDi, validación del domicilio y monitor de parrilla con «Se acabó». Sin errores de JavaScript.

---

## 7. Pendientes

- **Pantallas:** el diseño 2.0 (base hecha en Google Stitch) ya está aplicado en CU-02. Reglas que siguen todas las pantallas, para que el siguiente equipo las respete:
  - pocos colores y solo con significado: verde libre o acción, rojo ocupada o peligro, ámbar pidió la cuenta;
  - botones de 56 px (64 px el principal) con 12 px de separación; icono o texto claro, nada que dependa del mouse;
  - lo seleccionado en verde lleno con palomita;
  - "Deshacer" para lo que se puede revertir y confirmación solo para lo que no (cancelar, pedir la cuenta);
  - pocos botones por pantalla: lo que se usa poco va en "Opciones";
  - el monitor de parrilla agrupa por comanda, cambia de color con los minutos de espera (8 y 15, se ajustan en `kds.js`) y muestra la tira "En plancha";
  - todo es local: la letra Inter está en `assets/fuentes` (licencia OFL), no se usan CDN ni fotos de internet. Si el dueño manda fotos de sus platillos, se agregan en `assets`.
- **Para el siguiente equipo:** el resto de los casos de uso. La base ya los soporta y el patrón es el mismo (Servlet → Service → DAO):
  - CU-01 Administrar Menú (alta de productos, paquetes, extras, precio de apps).
  - CU-04 Administrar Caja (salidas y corte por forma de pago, sin corte con cuentas pendientes).
  - CU-05 Cobrar Pedido (hoy cobra un pedido completo con un método; falta pago mixto, dividir por consumo, descuentos, cortesías y pago adelantado de domicilio).
  - CU-06/07 Proveedores, compras con abonos e insumos.
  - CU-08 Empleados (con varios roles).
  - CU-09/10 Reportes en vivo con filtros y comparativas.
- **Clases de prueba con `main`** (`Pruebas/Persistencia.java` y `com/mycompany/negocio/Negocio.java`): se ajustaron y compilan, pero no se ejecutaron.
- La carpeta `taqueria-mi-buen` (el prototipo en Python de la rama `feature/practica-web`) no viene en este paquete; se conserva en su rama como prototipo de interfaz.

# Taquería Mi Buen - Caso de uso Administrar Pedidos

Equipo 2 · Iteración de Elaboración E1

Este paquete trae el proyecto `ProyectoTaqueriaMiBuen` (la versión por capas en Java que viene de la rama `Avance-4`) con el caso de uso priorizado **Administrar Pedidos** terminado, más la carpeta `BD` con los scripts de la base. Abajo va qué se hizo, cómo correrlo, qué se encontró y qué se probó.

---

## 1. Cómo correrlo

Se necesita: JDK 17 (o más nuevo), Apache Tomcat 10.1, MySQL 8 y NetBeans.

1. **Base de datos.** En MySQL Workbench corran, en este orden:
   - `BD/ScriptBDTaqueriaMiBuen.sql`: crea la base desde cero con las 8 mesas y el menú.
   - `BD/DatosPrueba.sql`: crea cinco empleados de prueba y deja una caja abierta.

   Si ya tienen la base vieja con datos y no la quieren perder, en lugar del primero corran `BD/ActualizacionAdministrarPedido.sql`: solo agrega lo nuevo y pasa los datos que ya existían. Saquen respaldo antes, por si las moscas.
2. **Conexión.** En `Persistencia/src/main/resources/META-INF/persistence.xml` pongan el usuario y la contraseña de su MySQL.
3. **Compilar.** Abran en NetBeans la carpeta `ProyectoTaqueriaMiBuen` (el proyecto padre) y denle **Clean and Build** al padre. Ahora sí arma los cinco módulos en orden y deja el WAR en `Presentacion/target/TaqueriaMiBuen.war`.
4. **Correr.** Denle Run al módulo Presentacion con Tomcat 10.1, o copien el WAR a `tomcat/webapps`. Se abre en `http://localhost:8080/TaqueriaMiBuen/`.

Empleados de prueba (los PIN se guardan cifrados con BCrypt, igual que en `Cuenta.java`):

| Nombre | Rol | PIN |
|---|---|---|
| Laura Mendez | Administrador | 1111 |
| Carlos Ruiz | Cajero | 2222 |
| José Núñez | Mesero | 3333 |
| María López | Mesero | 4444 |
| Pedro García | Cocinero | 5555 |

---

## 2. Qué quedó del caso de uso

| Parte del caso de uso | Qué hace ahora el sistema |
|---|---|
| 2.1 Flujo básico | El mesero elige una mesa libre, indica el número de personas, agrega productos y le da «Enviar a cocina». El pedido se guarda con folio, fecha, mesa, personas y mesero. Los productos llegan al monitor de cocina «En cola», la mesa se pone en rojo, sale «Comanda enviada a cocina» y la pantalla regresa al mapa de mesas. |
| 2.2.1 Tipar pedido | Comer aquí pide de 1 a 20 personas. Para llevar pide el nombre del cliente. A domicilio pide nombre, teléfono de 10 dígitos y dirección; las referencias son opcionales. Se valida en la pantalla **y** en el servidor. |
| 2.2.2 Agregar producto | Ventana con la cantidad (1 a 99), los extras con su costo y una nota para cocina de máximo 150 letras. El importe es (precio + extras) x cantidad. Las bebidas sin extras entran directo con cantidad 1. |
| 2.2.3 Para llevar / domicilio | Se crean sin mesa y aparecen en «Pedidos para llevar y a domicilio activos». En cocina se ve si es para llevar o a domicilio. |
| 2.2.4 Modificar pedido | El ticket se ve agrupado por rondas, con la hora y el estado de cada producto: NUEVO, EN COLA, EN PREPARACION, LISTO o CANCELADO. Lo que sigue en cola se puede cambiar o quitar, y sale solito del monitor de cocina. Cada envío nuevo es una ronda nueva. |
| 2.2.5 Cerrar pedido | Ver la inconsistencia 5.1. Cerrar = el cliente pidió la cuenta. La ventana de confirmación muestra los productos, el total y cuántos le faltan a cocina. |
| 2.2.6 Cancelar pedido | Si lo cancela un mesero pide el PIN de un cajero o administrador y el motivo (si eligen «Otro», el detalle es obligatorio). Se guardan el motivo, quién autorizó y la hora; lo pendiente sale de cocina y la mesa se recalcula. Nunca se borra nada. Si el PIN no sirve sale «Autorizacion no valida». |
| 2.2.7 Salir sin enviar | Si hay productos nuevos y el mesero se quiere ir, aparece «¿Desea salir sin enviar los productos a cocina?». También pregunta si cierran la pestaña. |
| 2.2.8 Pedido sin productos | «Enviar a cocina» se apaga y sale «Agregue al menos un producto». |
| 2.2.9 Datos incompletos | El campo se pone en rojo con «Campo obligatorio». |
| 2.2.10 Producto en preparación | No se deja cambiar ni quitar («Este producto ya se esta preparando y no se puede modificar»). Solo un cajero lo cancela, con PIN y motivo. |
| 2.2.11 Producto no disponible | Si al enviar algo ya se dio de baja, sale «Producto no disponible: ...», el renglón se marca en rojo y el menú se recarga. |
| 2.2.12 Falla de conexión | Sale «Sin conexion. El pedido no se ha enviado», con «Reintentar» y «Cancelar»; lo capturado se queda en pantalla. Reintentar manda la **misma clave de envío** y el servidor no duplica la comanda. |
| 3.4 Concurrencia | Dos meseros no pueden abrir la misma mesa al mismo tiempo, y los turnos de cocina nunca se repiten aunque manden al mismo tiempo. |
| 3.5 Importes | Todo con dos decimales exactos (DECIMAL en la base y redondeo en Java). El precio se congela al agregarlo. |
| 3.6 Trazabilidad | Se guarda quién abrió el pedido, quién le movió al último y a qué hora, y quién agregó cada producto. |
| Mapa de mesas | Verde = libre, rojo = ocupada, ámbar = ya pidieron la cuenta. Se actualiza cada 3 segundos. |
| Monitor de cocina | Consulta cada segundo, con los botones «En preparacion» y «Listo». |

---

## 3. Cambios por capa

**Dominio**
- `EstadoPreparacion` (nuevo): PENDIENTE, EN_PREPARACION, LISTO, CANCELADO. Sustituye al `listo` de sí/no.
- `EstadoMesa`: se agrega `PENDIENTE_PAGO` (el ámbar).
- `DetallePedido`: ronda, estado de preparación, fecha de envío, clave de envío, quién lo agregó, y motivo, quién y cuándo si se cancela. Las reglas de qué se puede modificar viven aquí.
- `Pedido`: quién lo modificó y cuándo, clave de creación y rondas. El subtotal ya no cuenta lo cancelado, y al cancelar el pedido se saca de cocina lo que no estaba listo.
- `Herramientas.Dinero` (nuevo): redondeo a dos decimales.

**Persistencia**
- `PedidoDAO`: envíos que no se duplican, turno de cocina apartado con `FOR UPDATE`, cambiar, quitar y cancelar un producto, y orden fijo de bloqueo (mesa, luego pedido, luego turno) para que dos transacciones no se queden esperando una a la otra.
- `MesaDAO.recalcularEstado`: una sola regla para el color de la mesa, que usan enviar, cerrar, cancelar y cobrar.
- `KdsDAO.cambiarEstado`: solo avanza hacia adelante y rechaza lo cancelado.
- `PagoDAO`: usa la misma regla de la mesa.

**Negocio**
- `PedidoService` revisa todo lo que llega, aunque la pantalla ya lo haya validado.
- Excepción nueva `ProductoNoDisponibleException`.
- `KdsService.cambiarEstado`.

**Control**
- Rutas nuevas en `PedidoServlet`: `.../detalles/{id}/cantidad`, `.../quitar` y `.../cancelar`.
- Ruta nueva en `KdsServlet`: `.../detalles/{id}/estado`.
- El 409 trae los ids de los productos agotados.

**Presentacion**
- `pedido.js` es casi nuevo, sobre la misma estructura y estilo que ya tenía.
- Se ajustaron `mesas.js`, `kds.js`, `datosEntrega.jsp` y `comun.js`.
- En `estilos.css` solo se agregaron unas clases con los colores que ya existían; el diseño no cambió.

**Base de datos**
- `ScriptBDTaqueriaMiBuen.sql` (completo), `ActualizacionAdministrarPedido.sql` (para una base existente) y `DatosPrueba.sql`.

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
11. **Pagos dobles.** La tabla `pago` no impedía dos pagos del mismo pedido. Se agregó UNIQUE como respaldo del bloqueo que ya existía.
12. **Persistencia sobrante.** Sobraba un `persistence.xml` vacío de la plantilla de NetBeans dentro de Presentacion; se quitó.
13. **Errores en HTML.** Si Tomcat contestaba con su página de error en HTML, la pantalla tronaba al leerla; ahora muestra un mensaje.
14. **Cobro mostraba lo cancelado.** La ventana de cobro y el ticket de pago listaban también los productos cancelados (aunque el total sí estaba bien). Ahora solo salen los que se cobran.

---

## 5. Inconsistencias entre documentos (para que el equipo decida)

Los documentos no dicen lo mismo en varios puntos. Se tomó como base el **Modelo de Análisis y Diseño**, porque es la decisión técnica más reciente y el script de la base ya lo seguía. Lo del Detallado que no lo contradice sí se implementó. Conviene que actualicen el documento que quede atrás.

1. **Estados del pedido y de la mesa.**
   - El Detallado del CU habla de pedido «Cerrado», mesa «Cobrada» y de cerrar después de cobrar.
   - El Modelo define ABIERTO, POR_COBRAR y PAGADO, y la mesa PENDIENTE_PAGO.
   - Aquí, «Cerrar pedido» = solicitar la cuenta (POR_COBRAR, mesa en ámbar), y el cobro lo deja PAGADO y la mesa en verde.
   - Si se quedan con la versión del Detallado, hay que agregar un estado CERRADO después del cobro.
2. **Autorización.** El Detallado pide «usuario y contraseña» del cajero; el Modelo y el código usan PIN. Se quedó el PIN. Ojo: el login por PIN revisa todas las cuentas con BCrypt hasta encontrar la que coincide; con muchos empleados se hace lento, y dos personas no pueden tener el mismo PIN.
3. **Acceso a datos.** Arquitectura dice JDBC + DAO y descarta JPA; el código usa JPA con Hibernate. Se dejó JPA porque todo está hecho así; hay que corregir el documento.
4. **Paquetes.** Arquitectura dice que todo cuelga de `mx.itson.taqueria`; el código usa `Entidades`, `DAO`, `Servicios`... sin prefijo.
5. **Versión de Java.** Arquitectura dice JDK 17 y los poms pedían 24. Se dejó 17, que también corre en JDK más nuevos.
6. **Tiempos de consulta.** KDS a 5 s contra 1 s, y mesas a 10 s contra 3 s. Se alineó al diseño.
7. **Categorías.** El CU menciona 4 (Tacos, Quesadillas, Especialidades y Bebidas) y el script traía 3. Se agregó Especialidades y el Lirio pasó ahí.
8. **Dinero.** El Detallado pide dos decimales exactos y la base usaba DOUBLE. Se cambió a DECIMAL(10,2).
9. **Referencias viejas.** El Modelo cita pasos como «2.2.2 Eliminar pedido» o «2.2.3 paso 2» que ya no coinciden con la numeración del Detallado v1.0.
10. **Cerrar con productos pendientes.** El Detallado no deja cerrar sin cobrar si hay productos en cola; con la idea del Modelo (cerrar = pedir la cuenta) sí se permite y se avisa cuántos faltan. Es decisión del equipo.

---

## 6. Qué se probó y cómo salió

Todo se probó en una máquina aparte, con MySQL 8.4 y Tomcat 10.1 de verdad y el WAR compilado de este mismo código.

- **Prueba completa por la API: 70 de 70 verificaciones pasaron.** Cubre:
  - inicio de sesión;
  - permisos por rol (el mesero ya no entra a Cobro y el cocinero no entra a pedidos);
  - 13 validaciones de captura;
  - flujo básico con importes;
  - reintento sin duplicar, tanto al crear el pedido como en la ronda 2;
  - dos meseros abriendo la misma mesa al mismo tiempo (uno entra y al otro le sale mesa ocupada);
  - 12 rondas enviadas al mismo tiempo sin turnos repetidos;
  - acentos;
  - cambiar y quitar lo que está en cola;
  - bloqueo de lo que está en preparación;
  - PIN incorrecto o de mesero;
  - cancelar un producto con PIN de cajero (el total baja lo correcto);
  - producto dado de baja;
  - precio congelado;
  - cerrar, cuenta separada y colores de la mesa (rojo, ámbar, verde);
  - cobro sin poder cobrar dos veces;
  - domicilio con envío;
  - cancelar el pedido completo.
- **Script de actualización:** se corrió sobre una base hecha con el script viejo y con datos viejos adentro.
  - Lo que estaba listo pasó a LISTO y lo pendiente a PENDIENTE.
  - La mesa que estaba por cobrar quedó en ámbar.
  - El Lirio pasó a Especialidades.
  - La columna vieja se quitó.
  - El sistema leyó bien esa base.
  - Encima se corrió la prueba completa: 70 de 70.
- **En el navegador** (escritorio 1366x768 y celular 390x844) se revisaron: el mapa de mesas con sus tres colores, el ticket por rondas con sus estados, «salir sin enviar», «Campo obligatorio» en rojo, «Sin conexion» con Reintentar (se cortó la red a propósito y al reintentar quedó **un solo** pedido) y el monitor de cocina. Sin errores de JavaScript.

**No se corrieron:** las clases de prueba con `main` (`Pruebas/Persistencia.java` y `com/mycompany/negocio/Negocio.java`). Se ajustaron y compilan, pero no se ejecutaron.

---

## 7. Pendientes

- Decidir las inconsistencias del punto 5 y actualizar los documentos.
- El resto de los casos de uso (Cobros completo, Administrar Menú, Empleados, Reportes) no se tocó más allá de lo necesario para que siga funcionando con lo nuevo.
- La carpeta `taqueria-mi-buen` (el prototipo en Python que también trae la rama) no viene en este paquete; esta versión ya la superó.

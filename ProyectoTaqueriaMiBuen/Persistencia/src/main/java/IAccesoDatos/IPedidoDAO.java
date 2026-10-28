package IAccesoDatos;

import Entidades.DetallePedido;
import Entidades.Pedido;
import Entidades.Usuario;
import Enums.EstadoPedido;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Operaciones de base de datos de los pedidos y sus detalles.
 * La implementa PedidoDAO.
 * Las demas capas usan esta interfaz y no la clase directamente, asi si
 * manana cambiamos la forma de guardar, Negocio ni se entera.
 *
 * @author Equipo 2
 */
public interface IPedidoDAO {

    // Guarda un pedido nuevo con sus detalles (ronda 1) y regresa su id.
    int insertarPedido(Pedido pedido);

    // Igual que el anterior, pero permite abrir otra cuenta en una mesa ocupada.
    int insertarPedido(Pedido pedido, boolean cuentaSeparada);

    // Agrega una ronda nueva a un pedido abierto y regresa el numero de ronda.
    // Si la clave de envio ya se habia usado, no inserta nada y regresa la ronda de esa vez.
    int insertarDetalles(int idPedido, List<DetallePedido> detalles, Usuario usuario, String claveEnvio);

    // Version vieja que usan las pruebas: cada detalle ya trae su pedido.
    void insertarDetalles(List<DetallePedido> detalles);

    // Ronda que se guardo con esa clave de envio, o null si nunca llego.
    Integer buscarRondaPorClave(int idPedido, String claveEnvio);

    // Pedido creado con esa clave (para no duplicar al reintentar), o null.
    Pedido buscarPorClaveCreacion(String claveCreacion);

    // Siguiente turno libre de la cola de cocina.
    int siguienteOrdenCola();

    // Pedido con sus detalles o null.
    Pedido buscarPorId(int idPedido);

    // Pedido abierto o por cobrar de la mesa, o null.
    Pedido buscarActivoPorMesa(int idMesa);

    // Cambia el estado del pedido.
    void actualizarEstado(int idPedido, EstadoPedido estado);

    // Cambia la cantidad de un producto que sigue en cola.
    Pedido cambiarCantidad(int idPedido, int idDetalle, int cantidad, Usuario usuario);

    // Quita un producto que sigue en cola (se marca cancelado, no se borra).
    Pedido quitarDetalle(int idPedido, int idDetalle, String motivo, Usuario usuario);

    // Cancela un producto que ya se esta preparando o ya esta listo (lo autoriza un cajero).
    Pedido cancelarDetalle(int idPedido, int idDetalle, String motivo, String autorizadoPor, Usuario usuario);

    // Calcula el total y deja el pedido por cobrar.
    Pedido cerrarPedido(int idPedido);

    // Igual, pero anotando quien lo cerro.
    Pedido cerrarPedido(int idPedido, Usuario usuario);

    // Cancela el pedido y guarda el motivo.
    Pedido cancelarPedido(int idPedido, String motivo, String autorizadaPor);

    // Igual, pero anotando quien hizo la cancelacion.
    Pedido cancelarPedido(int idPedido, String motivo, String autorizadaPor, Usuario usuario);

    // Pedidos abiertos o por cobrar.
    List<Pedido> listarActivos();

    // Pedidos que ya pidieron la cuenta y esperan el cobro.
    List<Pedido> listarPorCobrar();

    // Pedidos abiertos entre dos fechas, con sus detalles (para los reportes).
    List<Pedido> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fin);
}

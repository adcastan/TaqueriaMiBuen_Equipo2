package IServicios;

import DTO.DetalleDTO;
import DTO.PedidoDTO;
import DTO.ResultadoEnvio;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.Usuario;
import java.util.List;

/**
 * Lo que se puede hacer con un pedido (caso de uso Administrar Pedidos).
 * Los servlets solo conocen esta interfaz; las reglas viven en PedidoService.
 *
 * @author Equipo 2
 */
public interface IPedidoService {

    // Crea el pedido y lo manda a cocina como ronda 1.
    public ResultadoEnvio crearPedido(PedidoDTO dto, Usuario usuario);

    // Agrega productos a un pedido abierto (una ronda nueva).
    public ResultadoEnvio agregarDetalles(int idPedido, List<DetalleDTO> detallesDTO);

    // Igual, pero anotando quien lo hizo y con la clave para no duplicar al reintentar.
    public ResultadoEnvio agregarDetalles(int idPedido, List<DetalleDTO> detallesDTO, Usuario usuario, String claveEnvio);

    // Cambia la cantidad de un producto que sigue en cola.
    public Pedido cambiarCantidad(int idPedido, int idDetalle, int cantidad, Usuario usuario);

    // Quita un producto que sigue en cola.
    public Pedido quitarDetalle(int idPedido, int idDetalle, Usuario usuario);

    // Cancela un producto que ya se preparaba o estaba listo; lo autoriza un cajero.
    public Pedido cancelarDetalle(int idPedido, int idDetalle, String motivo, Usuario autoriza, Usuario usuario);

    // Cierra el pedido (el cliente pidio la cuenta).
    public Pedido cerrarPedido(int idPedido);

    // Igual, anotando quien lo cerro.
    public Pedido cerrarPedido(int idPedido, Usuario usuario);

    // Cancela el pedido, debe autorizar un cajero o administrador.
    public void cancelarPedido(int idPedido, String motivo, Usuario autoriza);

    // Igual, anotando quien hizo la cancelacion.
    public void cancelarPedido(int idPedido, String motivo, Usuario autoriza, Usuario usuario);

    // Obtiene pedido activo de una mesa o null.
    public Pedido obtenerPedidoActivo(int idMesa);

    // Obtiene pedido con sus detalles.
    public Pedido obtenerPedido(int idPedido);

    // Obtiene mesas para el mapa de mesas.
    public List<Mesa> obtenerMesas();

    // Obtiene pedidos abiertos o por cobrar.
    public List<Pedido> obtenerPedidosActivos();
}

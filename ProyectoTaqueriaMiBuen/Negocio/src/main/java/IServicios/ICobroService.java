package IServicios;

import DTO.ResultadoCobro;
import Entidades.Pedido;
import Entidades.Usuario;
import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de cobro de pedidos.
 * La implementa CobroService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface ICobroService {

    // Obtiene una lista de pedidos que ya pidieron la cuenta y esperan el cobro.
    public List<Pedido> obtenerPedidosPorCobrar();

    // Cobra el pedido completo, lo deja PAGADO, libera la mesa si toca y regresa el cambio.
    public ResultadoCobro cobrarPedido(int idPedido, String metodo, BigDecimal montoRecibido, Usuario cajero);
}

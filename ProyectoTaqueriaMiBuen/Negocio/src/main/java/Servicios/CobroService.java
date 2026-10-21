/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.PagoDAO;
import DAO.PedidoDAO;
import DTO.ResultadoCobro;
import Entidades.Caja;
import Entidades.Pago;
import Entidades.Pedido;
import Entidades.Usuario;
import Enums.EstadoPedido;
import Enums.Rol;
import Exceptions.NegocioException;
import IAccesoDatos.IPagoDAO;
import IAccesoDatos.IPedidoDAO;
import IServicios.IAutenticacionService;
import IServicios.ICajaService;
import IServicios.ICobroService;
import java.util.List;

/**
 * Servicio de Cobro.
 * @author Equipo 2
 */
public class CobroService implements ICobroService {

    public static final String METODO_EFECTIVO = "EFECTIVO";
    public static final String METODO_TARJETA = "TARJETA";

    private final IPedidoDAO pedidoDAO = new PedidoDAO();
    private final IPagoDAO pagoDAO = new PagoDAO();
    private final ICajaService cajaService = new CajaService();
    private final IAutenticacionService autenticacionService = new AutenticacionService();

    @Override
    public List<Pedido> obtenerPedidosPorCobrar() {
        return pedidoDAO.listarPorCobrar();
    }

    /**
     * Con tarjeta se cobra justo el total. En efectivo lo recibido debe
     * alcanzar para el total y se calcula el cambio.
     * @param idPedido
     * @param metodo
     * @param montoRecibido
     * @param cajero
     * @return 
     */
    @Override
    public ResultadoCobro cobrarPedido(int idPedido, String metodo, double montoRecibido, Usuario cajero) {
        boolean puedeCobrar = cajero != null
                && (autenticacionService.tieneRol(cajero, Rol.CAJERO)
                || autenticacionService.tieneRol(cajero, Rol.ADMINISTRADOR));
        if (!puedeCobrar) {
            throw new NegocioException("Solo un cajero o administrador puede cobrar");
        }
        Caja caja = cajaService.obtenerCajaAbierta();
        if (caja == null) {
            throw new NegocioException("Abre la caja antes de cobrar");
        }
        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            throw new NegocioException("No existe el pedido " + idPedido);
        }
        if (pedido.getEstado() != EstadoPedido.POR_COBRAR) {
            throw new NegocioException("Solo se puede cobrar un pedido que ya pidio la cuenta");
        }
        String metodoPago = metodo == null ? "" : metodo.trim().toUpperCase();
        if (!metodoPago.equals(METODO_EFECTIVO) && !metodoPago.equals(METODO_TARJETA)) {
            throw new NegocioException("El metodo de pago debe ser EFECTIVO o TARJETA");
        }

        double total = pedido.getTotal();
        double recibido = metodoPago.equals(METODO_TARJETA) ? total : montoRecibido;
        if (recibido < total) {
            throw new NegocioException("El efectivo recibido no alcanza para el total de $" + String.format("%.2f", total));
        }

        try {
            pagoDAO.registrarPago(new Pago(pedido, caja, metodoPago, total));
        } catch (RuntimeException e) {
            // El DAO avisa con IllegalStateException si otro cajero ya lo cobro
            if (e.getCause() instanceof IllegalStateException) {
                throw new NegocioException(e.getCause().getMessage());
            }
            throw e;
        }
        double cambio = Math.round((recibido - total) * 100) / 100.0;
        return new ResultadoCobro(idPedido, pedido.getFolio(), total, metodoPago, recibido, cambio);
    }
}

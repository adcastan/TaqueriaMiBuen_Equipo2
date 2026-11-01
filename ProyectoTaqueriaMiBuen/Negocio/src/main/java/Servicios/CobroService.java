package Servicios;

import DAO.CuentaDAO;
import DAO.PagoDAO;
import DAO.PedidoDAO;
import DTO.ResultadoCobro;
import Entidades.Caja;
import Entidades.Cuenta;
import Entidades.Pago;
import Entidades.Pedido;
import Entidades.Usuario;
import Enums.EstadoPedido;
import Enums.MetodoPago;
import Enums.Rol;
import Exceptions.NegocioException;
import Herramientas.Dinero;
import IAccesoDatos.ICuentaDAO;
import IAccesoDatos.IPagoDAO;
import IAccesoDatos.IPedidoDAO;
import IServicios.ICajaService;
import IServicios.ICobroService;
import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de Cobro (CU-05 Cobrar Pedido).
 *
 * En esta entrega el cobro es sencillo: un solo pago por pedido, por el total,
 * en efectivo (con cambio), tarjeta o transferencia. Los pedidos de DiDi Food
 * y Uber Eats se registran como PLATAFORMA y no pasan por la caja porque el
 * cliente ya pago en la app.
 *
 * Lo que sigue del CU-05 (pago mixto, dividir la cuenta por consumo,
 * descuentos y cortesias) ya tiene lugar en la base de datos, pero lo
 * programa el siguiente equipo.
 *
 * Al cobrar, el pedido queda PAGADO y la mesa se recalcula; eso es lo que
 * necesita el CU-02 para que la mesa vuelva a verde.
 *
 * @author Equipo 2
 */
public class CobroService implements ICobroService {

    private final IPedidoDAO pedidoDAO = new PedidoDAO();
    private final IPagoDAO pagoDAO = new PagoDAO();
    private final ICuentaDAO cuentaDAO = new CuentaDAO();
    private final ICajaService cajaService = new CajaService();

    // Pedidos que ya pidieron la cuenta y esperan al cajero
    @Override
    public List<Pedido> obtenerPedidosPorCobrar() {
        return pedidoDAO.listarPorCobrar();
    }

    /**
     * Cobra el pedido completo con un solo metodo.
     * - Con tarjeta o transferencia se cobra justo el total.
     * - En efectivo lo recibido debe alcanzar y se calcula el cambio.
     * - Si el pedido es de una app se registra como PLATAFORMA sin caja,
     *   aunque la pantalla mande otro metodo.
     *
     * @param idPedido pedido que se cobra (debe estar POR_COBRAR)
     * @param metodo EFECTIVO, TARJETA o TRANSFERENCIA
     * @param montoRecibido solo para efectivo: con cuanto pago el cliente
     * @param cajero quien cobra; debe tener rol de cajero o administrador
     * @return datos del ticket: total, metodo, recibido y cambio
     */
    @Override
    public ResultadoCobro cobrarPedido(int idPedido, String metodo, BigDecimal montoRecibido, Usuario cajero) {
        validarQuePuedeCobrar(cajero);
        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            throw new NegocioException("No existe el pedido " + idPedido);
        }
        if (pedido.getEstado() != EstadoPedido.POR_COBRAR) {
            throw new NegocioException("Solo se puede cobrar un pedido que ya pidio la cuenta");
        }
        // El total se congelo al cerrar el pedido; si por algo viene vacio se calcula
        BigDecimal total = pedido.getTotal() != null ? pedido.getTotal() : pedido.calcularTotal();

        MetodoPago metodoPago;
        Caja caja = null;
        BigDecimal recibido = null;
        if (pedido.getCanal() != null && pedido.getCanal().esDeApp()) {
            // DiDi Food / Uber Eats: el cliente pago en la app, no entra dinero a la caja
            metodoPago = MetodoPago.PLATAFORMA;
        } else {
            metodoPago = leerMetodo(metodo);
            caja = cajaService.obtenerCajaAbierta();
            if (caja == null) {
                throw new NegocioException("Abre la caja antes de cobrar");
            }
            if (metodoPago == MetodoPago.EFECTIVO) {
                recibido = Dinero.redondear(montoRecibido);
                if (recibido.compareTo(total) < 0) {
                    throw new NegocioException("El efectivo recibido no alcanza para el total de $" + total);
                }
            }
        }

        Pago pago = new Pago(pedido, caja, cajero, metodoPago, total, recibido);
        try {
            pagoDAO.registrarPago(pago);
        } catch (RuntimeException e) {
            // El DAO avisa con IllegalStateException si otro cajero ya lo cobro
            if (e.getCause() instanceof IllegalStateException) {
                throw new NegocioException(e.getCause().getMessage());
            }
            throw e;
        }
        BigDecimal pagoCon = recibido != null ? recibido : total;
        return new ResultadoCobro(idPedido, pedido.getFolio(), total, metodoPago.name(), pagoCon, pago.calcularCambio());
    }

    // Solo una cuenta con rol de cajero o administrador puede cobrar
    private void validarQuePuedeCobrar(Usuario cajero) {
        if (cajero == null) {
            throw new NegocioException("Inicia sesion para cobrar");
        }
        Cuenta cuenta = cuentaDAO.buscarPorUsuario(cajero.getIdUsuario());
        if (cuenta == null || !cuenta.tieneAlgunRol(Rol.CAJERO, Rol.ADMINISTRADOR)) {
            throw new NegocioException("Solo un cajero o administrador puede cobrar");
        }
    }

    /**
     * Convierte el texto que manda la pantalla al metodo de pago. PLATAFORMA
     * no se acepta aqui porque solo aplica a pedidos de app (eso lo decide el
     * canal del pedido, no el cajero).
     */
    private MetodoPago leerMetodo(String metodo) {
        String texto = metodo == null ? "" : metodo.trim().toUpperCase();
        try {
            MetodoPago m = MetodoPago.valueOf(texto);
            if (m != MetodoPago.PLATAFORMA) {
                return m;
            }
        } catch (IllegalArgumentException e) {
            // Si no es ninguno de los valores del enum cae al mensaje de abajo
        }
        throw new NegocioException("El metodo de pago debe ser EFECTIVO, TARJETA o TRANSFERENCIA");
    }
}

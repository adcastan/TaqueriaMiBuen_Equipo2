package Servicios;

import DAO.CajaDAO;
import DAO.PagoDAO;
import Entidades.Caja;
import Entidades.Corte;
import Entidades.Usuario;
import Enums.MetodoPago;
import Exceptions.NegocioException;
import Herramientas.Dinero;
import IAccesoDatos.ICajaDAO;
import IAccesoDatos.IPagoDAO;
import IServicios.ICajaService;
import java.math.BigDecimal;

/**
 * Servicio de Caja (CU-04 Administrar Caja).
 *
 * Hay una sola caja abierta al dia y la comparten varias personas. En esta
 * entrega se abre la caja con su fondo y al cerrarla se hace el corte con lo
 * cobrado en efectivo, tarjeta y transferencia. Las salidas de caja, lo de
 * plataformas en el corte y la regla de no cerrar con cuentas pendientes ya
 * tienen lugar en la base de datos, pero las programa el siguiente equipo.
 *
 * @author Equipo 2
 */
public class CajaService implements ICajaService {

    private final ICajaDAO cajaDAO = new CajaDAO();
    private final IPagoDAO pagoDAO = new PagoDAO();

    // Abre la caja del dia; no se puede abrir otra si ya hay una abierta
    @Override
    public Caja abrirCaja(Usuario usuario, BigDecimal montoInicial) {
        if (usuario == null) {
            throw new NegocioException("Inicia sesion para abrir la caja");
        }
        if (montoInicial == null || montoInicial.signum() < 0) {
            throw new NegocioException("El monto inicial no puede ser negativo");
        }
        if (cajaDAO.buscarAbierta() != null) {
            throw new NegocioException("Ya hay una caja abierta");
        }
        Caja caja = new Caja(usuario);
        caja.abrir(Dinero.redondear(montoInicial));
        cajaDAO.abrir(caja);
        return caja;
    }

    // La caja abierta ahorita, o null si nadie la ha abierto
    @Override
    public Caja obtenerCajaAbierta() {
        return cajaDAO.buscarAbierta();
    }

    // Cuanto lleva cobrado la caja con un metodo de pago
    @Override
    public BigDecimal obtenerTotalCobrado(int idCaja, MetodoPago metodo) {
        return pagoDAO.totalPorMetodo(idCaja, metodo);
    }

    /**
     * Cierra la caja y genera el corte. Al efectivo esperado solo se le suman
     * los cobros en efectivo; tarjeta y transferencia no entran al cajon pero
     * si se anotan en el corte.
     */
    @Override
    public Corte cerrarCaja(Usuario quienCierra, BigDecimal efectivoContado) {
        if (quienCierra == null) {
            throw new NegocioException("Inicia sesion para cerrar la caja");
        }
        Caja caja = cajaDAO.buscarAbierta();
        if (caja == null) {
            throw new NegocioException("No hay una caja abierta");
        }
        if (efectivoContado == null || efectivoContado.signum() < 0) {
            throw new NegocioException("El efectivo contado no puede ser negativo");
        }
        int idCaja = caja.getIdCaja();
        return cajaDAO.cerrar(idCaja, quienCierra,
                pagoDAO.totalPorMetodo(idCaja, MetodoPago.EFECTIVO),
                pagoDAO.totalPorMetodo(idCaja, MetodoPago.TARJETA),
                pagoDAO.totalPorMetodo(idCaja, MetodoPago.TRANSFERENCIA),
                Dinero.redondear(efectivoContado));
    }

    // El corte de una caja ya cerrada
    @Override
    public Corte obtenerCorte(int idCaja) {
        Corte corte = cajaDAO.buscarCorte(idCaja);
        if (corte == null) {
            throw new NegocioException("La caja " + idCaja + " todavia no tiene corte");
        }
        return corte;
    }
}

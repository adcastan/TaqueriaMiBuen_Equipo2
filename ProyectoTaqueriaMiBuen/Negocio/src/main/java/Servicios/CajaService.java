package Servicios;

import DAO.CajaDAO;
import DAO.PagoDAO;
import Entidades.Caja;
import Entidades.Corte;
import Entidades.Usuario;
import Exceptions.NegocioException;
import IAccesoDatos.ICajaDAO;
import IAccesoDatos.IPagoDAO;
import IServicios.ICajaService;

/**
 * Servicio de Caja.
 * @author Equipo 2
 */
public class CajaService implements ICajaService {

    private final ICajaDAO cajaDAO = new CajaDAO();
    private final IPagoDAO pagoDAO = new PagoDAO();

    // Abre la caja del turno; no se puede abrir otra si ya hay una abierta
    @Override
    public Caja abrirCaja(Usuario usuario, double montoInicial) {
        if (usuario == null) {
            throw new NegocioException("Inicia sesion para abrir la caja");
        }
        if (montoInicial < 0) {
            throw new NegocioException("El monto inicial no puede ser negativo");
        }
        if (cajaDAO.buscarAbierta() != null) {
            throw new NegocioException("Ya hay una caja abierta");
        }
        Caja caja = new Caja(usuario);
        caja.abrir(montoInicial);
        cajaDAO.abrir(caja);
        return caja;
    }

    // La caja abierta ahorita, o null si nadie ha abierto turno
    @Override
    public Caja obtenerCajaAbierta() {
        return cajaDAO.buscarAbierta();
    }

    // Cuanto lleva cobrado la caja con un metodo de pago
    @Override
    public double obtenerTotalCobrado(int idCaja, String metodo) {
        return pagoDAO.totalPorMetodo(idCaja, metodo);
    }

    /**
     * Al efectivo esperado solo se le suman los cobros en efectivo,
     * los de tarjeta no entran al cajon.
     */
    @Override
    public Corte cerrarCaja(double efectivoContado) {
        Caja caja = cajaDAO.buscarAbierta();
        if (caja == null) {
            throw new NegocioException("No hay una caja abierta");
        }
        if (efectivoContado < 0) {
            throw new NegocioException("El efectivo contado no puede ser negativo");
        }
        double ventasEfectivo = pagoDAO.totalPorMetodo(caja.getIdCaja(), CobroService.METODO_EFECTIVO);
        return cajaDAO.cerrar(caja.getIdCaja(), ventasEfectivo, efectivoContado);
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


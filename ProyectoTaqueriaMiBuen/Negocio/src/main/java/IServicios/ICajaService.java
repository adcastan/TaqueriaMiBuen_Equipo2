package IServicios;

import Entidades.Caja;
import Entidades.Corte;
import Entidades.Usuario;
import Enums.MetodoPago;
import java.math.BigDecimal;

/**
 * Servicio de caja.
 * La implementa CajaService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface ICajaService {

    // Abre la caja del dia con el efectivo inicial.
    public Caja abrirCaja(Usuario usuario, BigDecimal montoInicial);

    // Obtiene la Caja abierta en este momento o null.
    public Caja obtenerCajaAbierta();

    // Obtiene el total cobrado en la caja con un metodo (EFECTIVO, TARJETA o TRANSFERENCIA).
    public BigDecimal obtenerTotalCobrado(int idCaja, MetodoPago metodo);

    // Cierra la caja abierta y genera el corte con el efectivo contado.
    public Corte cerrarCaja(Usuario quienCierra, BigDecimal efectivoContado);

    // Obtiene el corte de una caja.
    public Corte obtenerCorte(int idCaja);
}

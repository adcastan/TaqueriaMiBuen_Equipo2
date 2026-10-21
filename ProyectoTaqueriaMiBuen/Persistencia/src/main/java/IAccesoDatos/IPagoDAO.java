/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.Pago;
import java.util.List;

/**
 * Operaciones de base de datos de los pagos (cobro de pedidos).
 * La implementa PagoDAO.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IPagoDAO {

    // Guarda el pago, deja el pedido PAGADO y libera la mesa si ya no tiene pedidos activos.
    public Pago registrarPago(Pago pago);

    // Pago de un pedido o null.
    public Pago buscarPorPedido(int idPedido);

    // Pagos recibidos en una caja.
    public List<Pago> listarPorCaja(int idCaja);

    // Suma de los pagos de una caja con un metodo (EFECTIVO o TARJETA).
    public double totalPorMetodo(int idCaja, String metodo);
}


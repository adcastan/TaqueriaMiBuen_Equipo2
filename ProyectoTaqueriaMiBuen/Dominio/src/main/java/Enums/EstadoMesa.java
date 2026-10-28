package Enums;

/**
 * Estados posibles de una mesa del local. Cada uno tiene su color en el mapa
 * de mesas para que el mesero vea de un vistazo como va el comedor:
 *
 * DISPONIBLE     verde, la mesa esta libre y se puede abrir un pedido.
 * OCUPADA        rojo, la mesa tiene un pedido abierto que sigue consumiendo.
 * PENDIENTE_PAGO ambar, todos los pedidos de la mesa ya pidieron la cuenta
 *                y solo falta que el cajero cobre.
 *
 * @author Equipo 2
 */
public enum EstadoMesa {
    DISPONIBLE,
    OCUPADA,
    PENDIENTE_PAGO
}

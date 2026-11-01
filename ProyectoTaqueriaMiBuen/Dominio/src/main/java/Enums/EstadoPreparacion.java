package Enums;

/**
 * En que va cada producto dentro de la cocina. Piensenlo como el camino que
 * recorre un taco desde que el mesero lo manda hasta que sale a la mesa:
 *
 * PENDIENTE       ya llego a cocina y esta esperando su turno ("En cola").
 * EN_PREPARACION  el cocinero ya lo agarro y lo esta haciendo.
 * LISTO           ya se termino y se puede entregar.
 * CANCELADO       se quito del pedido; ya no se prepara ni se cobra.
 *
 * Solo se puede avanzar hacia adelante, nunca regresar (un taco listo no
 * vuelve a estar en la plancha).
 *
 * @author Equipo 2
 */
public enum EstadoPreparacion {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    CANCELADO
}

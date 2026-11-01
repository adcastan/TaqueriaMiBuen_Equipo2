package Enums;

/**
 * Por donde llego el pedido. Sirve para saber que precio usar (las apps
 * tienen su propio precio) y para el reporte de ventas por canal.
 *
 * MESA       el cliente esta sentado en una de las 8 mesas.
 * MOSTRADOR  el cliente llego al local y pidio para llevar.
 * TELEFONO   llamaron por telefono.
 * WHATSAPP   mandaron mensaje por WhatsApp.
 * DIDI_FOOD  llego por la app de DiDi Food.
 * UBER_EATS  llego por la app de Uber Eats.
 *
 * @author Equipo 2
 */
public enum CanalPedido {
    MESA,
    MOSTRADOR,
    TELEFONO,
    WHATSAPP,
    DIDI_FOOD,
    UBER_EATS;

    /**
     * true si el pedido viene de una app de reparto. En ese caso se cobra
     * con el precio de apps y el pago lo hace la plataforma, no la caja.
     */
    public boolean esDeApp() {
        return this == DIDI_FOOD || this == UBER_EATS;
    }
}

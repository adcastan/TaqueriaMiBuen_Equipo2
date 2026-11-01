package Enums;

/**
 * Formas en que se le cobra al cliente (CU-05 Cobrar Pedido).
 *
 * EFECTIVO       en billetes y monedas; el sistema calcula el cambio.
 * TARJETA        con la terminal bancaria (voucher).
 * TRANSFERENCIA  deposito o transferencia a la cuenta del dueno.
 * PLATAFORMA     lo cobro DiDi Food o Uber Eats; no entra dinero a la caja.
 *
 * @author Equipo 2
 */
public enum MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    PLATAFORMA
}

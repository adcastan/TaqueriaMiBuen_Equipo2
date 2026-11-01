package DTO;

import java.math.BigDecimal;

/**
 * Respuesta que regresa el servicio despues de cobrar un pedido.
 * La pantalla de cobro la usa para mostrar el ticket con el cambio.
 *
 * @author Equipo 2
 */
public class ResultadoCobro {

    private int idPedido;
    private String folio;
    private BigDecimal total;
    // EFECTIVO, TARJETA, TRANSFERENCIA o PLATAFORMA
    private String metodo;
    // Con cuanto pago el cliente (en tarjeta o transferencia es igual al total)
    private BigDecimal montoRecibido;
    private BigDecimal cambio;

    public ResultadoCobro(int idPedido, String folio, BigDecimal total, String metodo, BigDecimal montoRecibido, BigDecimal cambio) {
        this.idPedido = idPedido;
        this.folio = folio;
        this.total = total;
        this.metodo = metodo;
        this.montoRecibido = montoRecibido;
        this.cambio = cambio;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public String getFolio() {
        return folio;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getMetodo() {
        return metodo;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public BigDecimal getCambio() {
        return cambio;
    }
}

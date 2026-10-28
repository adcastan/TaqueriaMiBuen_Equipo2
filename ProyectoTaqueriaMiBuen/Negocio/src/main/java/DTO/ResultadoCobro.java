package DTO;

/**
 * Respuesta que regresa el servicio despues de cobrar un pedido.
 * La pantalla de cobro la usa para mostrar el ticket con el cambio.
 *
 * @author Equipo 2
 */
public class ResultadoCobro {

    private int idPedido;
    private String folio;
    private double total;
    private String metodo;
    private double montoRecibido;
    private double cambio;

    public ResultadoCobro(int idPedido, String folio, double total, String metodo, double montoRecibido, double cambio) {
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

    public double getTotal() {
        return total;
    }

    public String getMetodo() {
        return metodo;
    }

    public double getMontoRecibido() {
        return montoRecibido;
    }

    public double getCambio() {
        return cambio;
    }
}


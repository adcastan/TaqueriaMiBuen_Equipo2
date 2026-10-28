package DTO;

/**
 * Lo que se le regresa a la pantalla despues de "Enviar a cocina": el pedido,
 * su folio, que ronda fue, si la cocina estaba conectada y si la peticion era
 * un reintento de algo que ya habia llegado (en ese caso no se guardo nada nuevo).
 *
 * @author Equipo 2
 */
public class ResultadoEnvio {

    private int idPedido;
    private String folio;
    private boolean kdsConectado;
    private int ronda;
    private boolean repetido;

    public ResultadoEnvio(int idPedido, String folio, boolean kdsConectado) {
        this(idPedido, folio, kdsConectado, 1, false);
    }

    public ResultadoEnvio(int idPedido, String folio, boolean kdsConectado, int ronda, boolean repetido) {
        this.idPedido = idPedido;
        this.folio = folio;
        this.kdsConectado = kdsConectado;
        this.ronda = ronda;
        this.repetido = repetido;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public String getFolio() {
        return folio;
    }

    public boolean isKdsConectado() {
        return kdsConectado;
    }

    public int getRonda() {
        return ronda;
    }

    public boolean isRepetido() {
        return repetido;
    }
}

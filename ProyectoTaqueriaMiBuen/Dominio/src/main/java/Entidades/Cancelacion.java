package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registro de la cancelacion de un pedido, motivo y quien la autorizo.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "cancelacion")
public class Cancelacion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cancelacion")
    private Integer idCancelacion;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "motivo", nullable = false, length = 150)
    private String motivo;

    @Column(name = "autorizada_por", length = 100)
    private String autorizadaPor;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public Cancelacion() {
    }

    public Cancelacion(Pedido pedido, String motivo, String autorizadaPor) {
        this.pedido = pedido;
        this.motivo = motivo;
        this.autorizadaPor = autorizadaPor;
    }

    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Integer getIdCancelacion() { return idCancelacion; }
    public void setIdCancelacion(Integer idCancelacion) { this.idCancelacion = idCancelacion; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getAutorizadaPor() { return autorizadaPor; }
    public void setAutorizadaPor(String autorizadaPor) { this.autorizadaPor = autorizadaPor; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}

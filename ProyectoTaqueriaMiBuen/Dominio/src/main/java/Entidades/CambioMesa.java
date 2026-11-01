package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Bitacora de cuando un pedido se paso de una mesa a otra (RN-12).
 * Sirve para saber despues "esta cuenta empezo en la 3 y la pasaron a la 5,
 * lo hizo Jose a las 9:40" (requisito 3.6 de trazabilidad).
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "cambio_mesa")
public class CambioMesa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cambio")
    private Integer idCambio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_mesa_origen", nullable = false)
    private Mesa mesaOrigen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_mesa_destino", nullable = false)
    private Mesa mesaDestino;

    // Quien hizo el cambio
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public CambioMesa() {
    }

    public CambioMesa(Pedido pedido, Mesa mesaOrigen, Mesa mesaDestino, Usuario usuario) {
        this.pedido = pedido;
        this.mesaOrigen = mesaOrigen;
        this.mesaDestino = mesaDestino;
        this.usuario = usuario;
    }

    // Si nadie puso la fecha, se toma la de este momento al guardar
    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Integer getIdCambio() {
        return idCambio;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Mesa getMesaOrigen() {
        return mesaOrigen;
    }

    public Mesa getMesaDestino() {
        return mesaDestino;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}

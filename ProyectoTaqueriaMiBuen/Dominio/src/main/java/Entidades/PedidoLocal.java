package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Pedido para comer en el local, siempre ligado a una mesa.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "pedido_local")
@PrimaryKeyJoinColumn(name = "id_pedido")
public class PedidoLocal extends Pedido {

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_mesa", nullable = false)
    private Mesa mesa;

    @Column(name = "numero_personas")
    private Integer numeroPersonas;

    public PedidoLocal() {
    }

    public PedidoLocal(Usuario usuario, Mesa mesa, Integer numeroPersonas) {
        super(usuario);
        this.mesa = mesa;
        this.numeroPersonas = numeroPersonas;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public Integer getNumeroPersonas() {
        return numeroPersonas;
    }

    public void setNumeroPersonas(Integer numeroPersonas) {
        this.numeroPersonas = numeroPersonas;
    }
}

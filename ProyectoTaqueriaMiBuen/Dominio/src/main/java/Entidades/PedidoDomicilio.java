package Entidades;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Pedido que se entrega en el domicilio del cliente.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "pedido_domicilio")
@PrimaryKeyJoinColumn(name = "id_pedido")
public class PedidoDomicilio extends Pedido {

    @Column(name = "costo_envio", nullable = false)
    private double costoEnvio;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private DatosEntrega datosEntrega;

    public PedidoDomicilio() {
    }

    public PedidoDomicilio(Usuario usuario, double costoEnvio, DatosEntrega datosEntrega) {
        super(usuario);
        this.costoEnvio = costoEnvio;
        this.datosEntrega = datosEntrega;
        if (datosEntrega != null) {
            datosEntrega.setPedido(this);
        }
    }

    /**
     * El total de un pedido a domicilio incluye el costo de envio.
     */
    @Override
    public double calcularTotal() {
        return calcularSubtotal() + costoEnvio;
    }

    public double getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(double costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public DatosEntrega getDatosEntrega() {
        return datosEntrega;
    }

    // Se liga en ambos sentidos para que JPA guarde el id_pedido en datos_entrega
    public void setDatosEntrega(DatosEntrega datosEntrega) {
        this.datosEntrega = datosEntrega;
        if (datosEntrega != null) {
            datosEntrega.setPedido(this);
        }
    }
}

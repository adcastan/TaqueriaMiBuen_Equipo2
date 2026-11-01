package Entidades;

import Enums.TipoRepartidor;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
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
 * El costo de envio no es fijo: depende de la zona y lo captura quien toma el
 * pedido. Lo puede llevar un empleado (PROPIO) o un servicio de afuera como
 * DiDi o Uber (EXTERNO); en repartidor se anota quien.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "pedido_domicilio")
@PrimaryKeyJoinColumn(name = "id_pedido")
public class PedidoDomicilio extends Pedido {

    @Column(name = "costo_envio", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_repartidor", nullable = false, length = 7)
    private TipoRepartidor tipoRepartidor = TipoRepartidor.PROPIO;

    // Nombre del empleado o del servicio que lo lleva (puede ir vacio)
    @Column(name = "repartidor", length = 100)
    private String repartidor;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private DatosEntrega datosEntrega;

    public PedidoDomicilio() {
    }

    public PedidoDomicilio(Usuario usuario, BigDecimal costoEnvio, DatosEntrega datosEntrega) {
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
    public BigDecimal calcularTotal() {
        // A lo de la comida (ya con descuento) se le suma el envio que se capturo
        return Herramientas.Dinero.sumar(super.calcularTotal(), costoEnvio);
    }

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
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

    public TipoRepartidor getTipoRepartidor() {
        return tipoRepartidor;
    }

    public void setTipoRepartidor(TipoRepartidor tipoRepartidor) {
        this.tipoRepartidor = tipoRepartidor;
    }

    public String getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(String repartidor) {
        this.repartidor = repartidor;
    }
}

package Entidades;

import Enums.MetodoPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago de un pedido (CU-05 Cobrar Pedido).
 *
 * La tabla ya permite varios pagos por pedido (pago mixto o cuenta dividida
 * con la columna parte), pero en esta entrega el cobro registra un solo pago
 * por pedido. Lo demas lo termina el siguiente equipo.
 *
 * Los pedidos de DiDi Food y Uber Eats se pagan con PLATAFORMA y no pasan por
 * la caja, por eso caja puede ir vacia en ese caso.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "pago")
public class Pago implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    // Caja donde entro el dinero; null solo si se pago por una plataforma
    @ManyToOne
    @JoinColumn(name = "id_caja")
    private Caja caja;

    // Quien cobro. La caja la comparten varias personas, por eso se guarda en cada pago
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false, length = 13)
    private MetodoPago metodo;

    // Lo que se le abona a la cuenta (sin contar el cambio)
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    // Solo en efectivo: con cuanto pago el cliente, para sacar el cambio
    @Column(name = "recibido", precision = 10, scale = 2)
    private BigDecimal recibido;

    // Numero de persona cuando la cuenta se divide; null = toda la cuenta
    @Column(name = "parte")
    private Integer parte;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public Pago() {
    }

    public Pago(Pedido pedido, Caja caja, Usuario usuario, MetodoPago metodo, BigDecimal monto, BigDecimal recibido) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        this.pedido = pedido;
        this.caja = caja;
        this.usuario = usuario;
        this.metodo = metodo;
        this.monto = monto;
        this.recibido = recibido;
    }

    // Si nadie puso la fecha, se toma la de este momento al guardar
    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    // Cambio que se le regresa al cliente; cero si no fue en efectivo
    public BigDecimal calcularCambio() {
        if (recibido == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return recibido.subtract(monto).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public Integer getIdPago() {
        return idPago;
    }

    public void setIdPago(Integer idPago) {
        this.idPago = idPago;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Caja getCaja() {
        return caja;
    }

    public void setCaja(Caja caja) {
        this.caja = caja;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getRecibido() {
        return recibido;
    }

    public void setRecibido(BigDecimal recibido) {
        this.recibido = recibido;
    }

    public Integer getParte() {
        return parte;
    }

    public void setParte(Integer parte) {
        this.parte = parte;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}

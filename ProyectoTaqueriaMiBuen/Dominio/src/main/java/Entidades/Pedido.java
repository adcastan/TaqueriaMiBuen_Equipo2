package Entidades;

import Enums.EstadoPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido general. Cada tipo (local, para llevar, a domicilio) tiene su propia
 * tabla que comparte el id_pedido con esta (herencia JOINED).
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "pedido")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pedido implements Serializable {

    private static final DateTimeFormatter FORMATO_FOLIO = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "folio", nullable = false, unique = true, length = 20)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoPedido estado = EstadoPedido.ABIERTO;

    // Usuario que capturo el pedido
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "total")
    private Double total;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private Cancelacion cancelacion;

    protected Pedido() {
    }

    protected Pedido(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * Antes de insertar se asignan la fecha y un folio si todavia no tienen.
     */
    @PrePersist
    protected void antesDeGuardar() {
        if (fechaApertura == null) {
            fechaApertura = LocalDateTime.now();
        }
        if (folio == null) {
            // L local, P para llevar, D domicilio
            String letra = "P";
            if (this instanceof PedidoLocal) {
                letra = "L";
            } else if (this instanceof PedidoDomicilio) {
                letra = "D";
            }
            folio = letra + LocalDateTime.now().format(FORMATO_FOLIO);
        }
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }

    public double calcularSubtotal() {
        double subtotal = 0;
        for (DetallePedido d : detalles) {
            subtotal += d.calcularImporte();
        }
        return subtotal;
    }

    /**
     * En el pedido general el total es igual al subtotal, el pedido a domicilio
     * lo sobreescribe para sumar el envio.
     */
    public double calcularTotal() {
        return calcularSubtotal();
    }

    /**
     * El cliente pide la cuenta: queda pendiente de cobro con su total.
     */
    public void cerrar() {
        if (estado != EstadoPedido.ABIERTO) {
            throw new IllegalStateException("Solo se puede cerrar un pedido abierto");
        }
        total = calcularTotal();
        estado = EstadoPedido.POR_COBRAR;
    }

    public void cancelar(String motivo, String autorizadaPor) {
        if (estado == EstadoPedido.PAGADO || estado == EstadoPedido.CANCELADO) {
            throw new IllegalStateException("El pedido ya no se puede cancelar");
        }
        cancelacion = new Cancelacion(this, motivo, autorizadaPor);
        estado = EstadoPedido.CANCELADO;
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }

    public Cancelacion getCancelacion() {
        return cancelacion;
    }

    public void setCancelacion(Cancelacion cancelacion) {
        this.cancelacion = cancelacion;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + idPedido + ", folio=" + folio + ", estado=" + estado + ", total=" + total + "}";
    }
}

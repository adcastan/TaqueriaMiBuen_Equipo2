package Entidades;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de un pedido. Tambien funciona como elemento de la cola de cocina:
 * ordenColaPedido marca el turno y listo indica si el cocinero ya lo termino.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    // Precio del producto al momento de pedirlo, por si despues cambia en el menu
    @Column(name = "precio_unitario", nullable = false)
    private double precioUnitario;

    @Column(name = "nota_cocina", length = 150)
    private String notaCocina;

    // Lo asigna el DAO al guardar, siguiendo el ultimo turno de la cola
    @Column(name = "orden_cola_pedido", nullable = false, unique = true)
    private Integer ordenColaPedido;

    @Column(name = "listo", nullable = false)
    private boolean listo = false;

    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL)
    private List<DetalleExtra> extras = new ArrayList<>();

    public DetallePedido() {
    }

    public DetallePedido(Producto producto, int cantidad, String notaCocina) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        this.producto = producto;
        this.cantidad = cantidad;
        this.notaCocina = notaCocina;
        this.precioUnitario = producto.getPrecio();
    }

    /**
     * Importe del renglon: cantidad por (precio del producto + extras).
     */
    public double calcularImporte() {
        double costoExtras = 0;
        for (DetalleExtra e : extras) {
            costoExtras += e.getCostoExtra();
        }
        return cantidad * (precioUnitario + costoExtras);
    }

    public void marcarListo() {
        this.listo = true;
    }

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public String getNotaCocina() {
        return notaCocina;
    }

    public void setNotaCocina(String notaCocina) {
        this.notaCocina = notaCocina;
    }

    public Integer getOrdenColaPedido() {
        return ordenColaPedido;
    }

    public void setOrdenColaPedido(Integer ordenColaPedido) {
        this.ordenColaPedido = ordenColaPedido;
    }

    public boolean isListo() {
        return listo;
    }

    public void setListo(boolean listo) {
        this.listo = listo;
    }

    public List<DetalleExtra> getExtras() {
        return extras;
    }

    public void setExtras(List<DetalleExtra> extras) {
        this.extras = extras;
    }

    @Override
    public String toString() {
        return "Detalle{turno=" + ordenColaPedido + ", " + cantidad + " x " + producto.getNombre()
                + ", importe=" + calcularImporte() + ", listo=" + listo + "}";
    }
}

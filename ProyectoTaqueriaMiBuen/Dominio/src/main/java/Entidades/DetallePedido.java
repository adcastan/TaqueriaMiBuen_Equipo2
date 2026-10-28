package Entidades;

import Enums.EstadoPreparacion;
import Herramientas.Dinero;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Un renglon del pedido: "3 tacos de asada sin cebolla". Tambien es lo que
 * ve la cocina en su monitor, por eso aqui viven el turno de la cola y el
 * estado de preparacion.
 *
 * Cada vez que el mesero le da "Enviar a cocina" se arma una ronda nueva. Asi,
 * si la mesa pide mas tacos a media comida, se sabe que eso fue la ronda 2 y
 * no se revuelve con lo que ya se habia pedido.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido implements Serializable {

    // Tope de piezas por renglon; 99 tacos de un jalon ya es pedido de fiesta
    public static final int CANTIDAD_MAXIMA = 99;
    public static final int LARGO_MAXIMO_NOTA = 150;

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

    // Precio del producto al momento de pedirlo, por si despues cambia en el menu (RN-02)
    @Column(name = "precio_unitario", nullable = false)
    private double precioUnitario;

    @Column(name = "nota_cocina", length = LARGO_MAXIMO_NOTA)
    private String notaCocina;

    // Lo asigna el DAO al guardar, siguiendo el ultimo turno de la cola
    @Column(name = "orden_cola_pedido", nullable = false, unique = true)
    private Integer ordenColaPedido;

    // Numero de envio a cocina dentro del mismo pedido (1, 2, 3...)
    @Column(name = "ronda", nullable = false)
    private int ronda = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_preparacion", nullable = false, length = 15)
    private EstadoPreparacion estadoPreparacion = EstadoPreparacion.PENDIENTE;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    // Clave que manda la pantalla en cada envio. Si el mesero le pica "Reintentar"
    // y la peticion llega dos veces, con esta clave nos damos cuenta y no se duplica
    @Column(name = "clave_envio", length = 40)
    private String claveEnvio;

    // Quien agrego el producto, para saber a quien preguntarle si algo salio mal
    @ManyToOne
    @JoinColumn(name = "id_usuario_agrego")
    private Usuario agregadoPor;

    // Solo se llenan si el producto se cancelo
    @Column(name = "motivo_cancelacion", length = 150)
    private String motivoCancelacion;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL)
    private List<DetalleExtra> extras = new ArrayList<>();

    public DetallePedido() {
    }

    public DetallePedido(Producto producto, int cantidad, String notaCocina) {
        validarCantidad(cantidad);
        this.producto = producto;
        this.cantidad = cantidad;
        this.notaCocina = notaCocina;
        this.precioUnitario = producto.getPrecio();
    }

    /**
     * Revisa que la cantidad tenga sentido: minimo una pieza y maximo 99.
     */
    public static void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        if (cantidad > CANTIDAD_MAXIMA) {
            throw new IllegalArgumentException("La cantidad maxima por producto es " + CANTIDAD_MAXIMA);
        }
    }

    /**
     * Importe del renglon: (precio unitario + costo de los extras) x cantidad.
     * Es la formula del paso 3 del subflujo "Agregar producto".
     */
    public double calcularImporte() {
        double costoExtras = 0;
        for (DetalleExtra e : extras) {
            costoExtras += e.getCostoExtra();
        }
        return Dinero.redondear(cantidad * (precioUnitario + costoExtras));
    }

    /**
     * Un producto cancelado ya no se cobra; todo lo demas si cuenta en la cuenta.
     */
    public boolean cuentaParaElTotal() {
        return estadoPreparacion != EstadoPreparacion.CANCELADO;
    }

    /**
     * El mesero solo puede cambiar la cantidad o quitar un producto mientras
     * sigue en la cola. Si el cocinero ya lo empezo, ya no (flujo 2.2.10).
     */
    public boolean sePuedeModificar() {
        return estadoPreparacion == EstadoPreparacion.PENDIENTE;
    }

    /**
     * Pendientes de cocina = lo que todavia no se termina ni se cancelo.
     */
    public boolean estaPendienteEnCocina() {
        return estadoPreparacion == EstadoPreparacion.PENDIENTE
                || estadoPreparacion == EstadoPreparacion.EN_PREPARACION;
    }

    // Cambia las piezas, pero solo si el producto sigue en la cola
    public void cambiarCantidad(int nuevaCantidad) {
        if (!sePuedeModificar()) {
            throw new IllegalStateException("Este producto ya se esta preparando y no se puede modificar");
        }
        validarCantidad(nuevaCantidad);
        this.cantidad = nuevaCantidad;
    }

    /**
     * El cocinero lo agarra. Solo se puede si estaba esperando en la cola.
     */
    public void iniciarPreparacion() {
        if (estadoPreparacion != EstadoPreparacion.PENDIENTE) {
            throw new IllegalStateException("Solo se puede empezar un producto que esta en cola");
        }
        estadoPreparacion = EstadoPreparacion.EN_PREPARACION;
    }

    /**
     * El cocinero lo termina. Se permite saltar directo de la cola a listo
     * para las cosas que no se cocinan (un refresco, por ejemplo).
     */
    public void marcarListo() {
        if (!estaPendienteEnCocina()) {
            throw new IllegalStateException("El producto ya estaba listo o fue cancelado");
        }
        estadoPreparacion = EstadoPreparacion.LISTO;
    }

    /**
     * Saca el producto del pedido sin borrarlo: queda guardado como cancelado
     * con su motivo y quien lo autorizo, para que no se pierda el rastro.
     */
    public void cancelar(String motivo, String autorizadoPor) {
        if (estadoPreparacion == EstadoPreparacion.CANCELADO) {
            throw new IllegalStateException("El producto ya estaba cancelado");
        }
        estadoPreparacion = EstadoPreparacion.CANCELADO;
        motivoCancelacion = motivo;
        canceladoPor = autorizadoPor;
        fechaCancelacion = LocalDateTime.now();
    }

    // Lo dejamos para no romper las pantallas y reportes que solo preguntan si ya esta listo
    public boolean isListo() {
        return estadoPreparacion == EstadoPreparacion.LISTO;
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

    public int getRonda() {
        return ronda;
    }

    public void setRonda(int ronda) {
        this.ronda = ronda;
    }

    public EstadoPreparacion getEstadoPreparacion() {
        return estadoPreparacion;
    }

    public void setEstadoPreparacion(EstadoPreparacion estadoPreparacion) {
        this.estadoPreparacion = estadoPreparacion;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getClaveEnvio() {
        return claveEnvio;
    }

    public void setClaveEnvio(String claveEnvio) {
        this.claveEnvio = claveEnvio;
    }

    public Usuario getAgregadoPor() {
        return agregadoPor;
    }

    public void setAgregadoPor(Usuario agregadoPor) {
        this.agregadoPor = agregadoPor;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public List<DetalleExtra> getExtras() {
        return extras;
    }

    public void setExtras(List<DetalleExtra> extras) {
        this.extras = extras;
    }

    @Override
    public String toString() {
        return "Detalle{turno=" + ordenColaPedido + ", ronda=" + ronda + ", " + cantidad + " x " + producto.getNombre()
                + ", importe=" + calcularImporte() + ", estado=" + estadoPreparacion + "}";
    }
}

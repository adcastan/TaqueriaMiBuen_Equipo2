package Entidades;

import Enums.CanalPedido;
import Enums.EstadoPedido;
import Herramientas.Dinero;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pedido general. Cada tipo (local, para llevar, a domicilio) tiene su propia
 * tabla que comparte el id_pedido con esta (herencia JOINED).
 *
 * Su vida es asi: nace ABIERTO cuando se manda la primera comanda, pasa a
 * POR_COBRAR cuando el mesero lo cierra (el cliente pidio la cuenta), y queda
 * PAGADO cuando el cajero cobra. En cualquier momento antes de pagar se puede
 * CANCELAR, pero nunca se borra: solo se marca.
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

    // Por donde llego: mesa, mostrador, telefono, WhatsApp, DiDi Food o Uber Eats.
    // Decide el precio (las apps tienen el suyo) y sale en el reporte de ventas
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 10)
    private CanalPedido canal = CanalPedido.MESA;

    // Usuario que capturo el pedido
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    // Ultima persona que le movio algo al pedido y cuando (requisito 3.6)
    @ManyToOne
    @JoinColumn(name = "id_usuario_modifico")
    private Usuario usuarioModifico;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Clave del primer envio. Si la misma peticion llega dos veces (por el boton
    // "Reintentar") se encuentra el pedido que ya existe en vez de crear otro
    @Column(name = "clave_creacion", unique = true, length = 40)
    private String claveCreacion;

    // Descuento a toda la cuenta (lo aplica el cajero en CU-05). Hasta el limite
    // configurado no pide permiso; arriba del limite guarda quien lo autorizo
    @Column(name = "descuento", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "motivo_descuento", length = 150)
    private String motivoDescuento;

    @ManyToOne
    @JoinColumn(name = "id_usuario_descuento")
    private Usuario usuarioDescuento;

    // Se congela al cerrar el pedido (cuando piden la cuenta)
    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private Cancelacion cancelacion;

    protected Pedido() {
    }

    protected Pedido(Usuario usuario) {
        this.usuario = usuario;
    }

    // JPA llama a este metodo justo antes de guardar: aqui se ponen la fecha y el folio
    @PrePersist
    protected void antesDeGuardar() {
        if (fechaApertura == null) {
            fechaApertura = LocalDateTime.now();
        }
        if (folio == null) {
            // L local, P para llevar, D domicilio. Al final van dos numeros al azar
            // por si dos meseros mandan su pedido en el mismo milisegundo
            String letra = "P";
            if (this instanceof PedidoLocal) {
                letra = "L";
            } else if (this instanceof PedidoDomicilio) {
                letra = "D";
            }
            int azar = ThreadLocalRandom.current().nextInt(10, 100);
            folio = letra + LocalDateTime.now().format(FORMATO_FOLIO) + azar;
        }
    }

    // Mete un renglon al pedido y le dice al renglon a que pedido pertenece
    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }

    /**
     * Anota quien hizo el cambio y a que hora. Se llama en cada accion que
     * modifica el pedido (agregar, quitar, cerrar, cancelar).
     */
    public void registrarCambio(Usuario quien) {
        if (quien != null) {
            usuarioModifico = quien;
        }
        fechaModificacion = LocalDateTime.now();
    }

    /**
     * Numero que le toca a la siguiente ronda: la mas alta que ya existe + 1.
     */
    public int siguienteRonda() {
        int mayor = 0;
        for (DetallePedido d : detalles) {
            mayor = Math.max(mayor, d.getRonda());
        }
        return mayor + 1;
    }

    /**
     * Productos que siguen en el pedido (todos menos los cancelados).
     */
    public List<DetallePedido> detallesActivos() {
        List<DetallePedido> activos = new ArrayList<>();
        for (DetallePedido d : detalles) {
            if (d.cuentaParaElTotal()) {
                activos.add(d);
            }
        }
        return activos;
    }

    /**
     * Cuantos productos le faltan a la cocina (en cola o preparandose).
     */
    public int productosPendientesEnCocina() {
        int pendientes = 0;
        for (DetallePedido d : detalles) {
            if (d.estaPendienteEnCocina()) {
                pendientes++;
            }
        }
        return pendientes;
    }

    // Suma de los importes de los productos que no estan cancelados
    public BigDecimal calcularSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetallePedido d : detallesActivos()) {
            subtotal = subtotal.add(d.calcularImporte());
        }
        return Dinero.redondear(subtotal);
    }

    // Lo que se cobra: subtotal menos descuento. El de domicilio le suma el envio
    public BigDecimal calcularTotal() {
        return Dinero.redondear(calcularSubtotal().subtract(descuento));
    }

    /**
     * Aplica un descuento a la cuenta. Si pasa del limite (porcentaje que el
     * dueno configura, empieza en 10%) necesita quien lo autorice: el
     * administrador con su PIN. Eso lo revisa el servicio de cobro; aqui solo
     * se guarda y se valida que no sea mas que la cuenta.
     */
    public void aplicarDescuento(BigDecimal monto, String motivo, Usuario autorizo) {
        if (monto == null || monto.signum() < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }
        if (monto.compareTo(calcularSubtotal()) > 0) {
            throw new IllegalArgumentException("El descuento no puede ser mayor que la cuenta");
        }
        this.descuento = Dinero.redondear(monto);
        this.motivoDescuento = motivo;
        this.usuarioDescuento = autorizo;
    }

    /**
     * Que porcentaje del subtotal representa un descuento. Sirve para
     * compararlo contra el limite que puede dar el cajero sin permiso.
     */
    public BigDecimal porcentajeDe(BigDecimal monto) {
        BigDecimal subtotal = calcularSubtotal();
        if (subtotal.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return monto.multiply(BigDecimal.valueOf(100)).divide(subtotal, 2, RoundingMode.HALF_UP);
    }

    // true si todavia se le pueden agregar o quitar productos
    public boolean estaAbierto() {
        return estado == EstadoPedido.ABIERTO;
    }

    /**
     * El cliente pide la cuenta: se congela el total y el pedido queda por cobrar.
     * Ya no acepta productos nuevos (RN-04).
     */
    public void cerrar() {
        if (estado != EstadoPedido.ABIERTO) {
            throw new IllegalStateException("Solo se puede cerrar un pedido abierto");
        }
        if (detallesActivos().isEmpty()) {
            throw new IllegalStateException("El pedido no tiene productos; mejor cancelalo");
        }
        total = calcularTotal();
        estado = EstadoPedido.POR_COBRAR;
    }

    /**
     * Cancela todo el pedido. Lo que la cocina no ha terminado se marca como
     * cancelado para que desaparezca del monitor; lo que ya estaba listo se
     * queda como listo porque si se preparo.
     */
    public void cancelar(String motivo, String autorizadaPor) {
        if (estado == EstadoPedido.PAGADO || estado == EstadoPedido.CANCELADO) {
            throw new IllegalStateException("El pedido ya no se puede cancelar");
        }
        for (DetallePedido d : detalles) {
            if (d.estaPendienteEnCocina()) {
                d.cancelar(motivo, autorizadaPor);
            }
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

    public Usuario getUsuarioModifico() {
        return usuarioModifico;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public String getClaveCreacion() {
        return claveCreacion;
    }

    public void setClaveCreacion(String claveCreacion) {
        this.claveCreacion = claveCreacion;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public CanalPedido getCanal() {
        return canal;
    }

    public void setCanal(CanalPedido canal) {
        this.canal = canal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public String getMotivoDescuento() {
        return motivoDescuento;
    }

    public Usuario getUsuarioDescuento() {
        return usuarioDescuento;
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

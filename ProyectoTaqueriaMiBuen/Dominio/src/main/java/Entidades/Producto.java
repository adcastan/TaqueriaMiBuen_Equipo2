package Entidades;

import Enums.CanalPedido;
import Enums.UnidadVenta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Platillo, bebida o paquete del menu (CU-01 Administrar Menu).
 *
 * Tiene dos banderas que se confunden facil, por eso van explicadas:
 * - disponible: false cuando se agoto en el dia (se acabo la carne). Lo marca
 *   la parrilla o el administrador y al dia siguiente se vuelve a prender.
 * - activo: false cuando el producto se dio de baja del menu para siempre.
 *   No se borra porque hay pedidos viejos que lo usan.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "producto")
public class Producto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    // Precio normal del menu, en pesos con dos decimales
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    // Precio para DiDi Food y Uber Eats. Si viene vacio se usa el precio normal
    @Column(name = "precio_apps", precision = 10, scale = 2)
    private BigDecimal precioApps;

    // PIEZA (enteros) o KILO (acepta 0.5, 0.250, etc.)
    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_venta", nullable = false, length = 5)
    private UnidadVenta unidadVenta = UnidadVenta.PIEZA;

    // true si lo prepara la parrilla y debe salir en el monitor de cocina.
    // Las bebidas, salsas y guacamole los entrega el mesero directo
    @Column(name = "pasa_por_parrilla", nullable = false)
    private boolean pasaPorParrilla = true;

    @Column(name = "disponible", nullable = false)
    private boolean disponible = true;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    // PERSIST: al registrar un producto nuevo tambien se guardan sus extras nuevos
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "producto_extra",
            joinColumns = @JoinColumn(name = "id_producto"),
            inverseJoinColumns = @JoinColumn(name = "id_extra"))
    private List<IngredienteExtra> extras = new ArrayList<>();

    // Si el producto es un paquete (combo), aqui van las piezas que lo forman.
    // EAGER porque el monitor de cocina necesita leerlas despues de cerrar la conexion
    @OneToMany(mappedBy = "paquete", fetch = FetchType.EAGER)
    private List<PaqueteComponente> componentes = new ArrayList<>();

    public Producto() {
    }

    public Producto(Categoria categoria, String nombre, String descripcion, BigDecimal precio) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
    }

    /**
     * Precio que se le cobra al cliente segun por donde llego el pedido:
     * si es de una app se usa el precio de apps (cuando existe), si no el normal.
     */
    public BigDecimal precioPara(CanalPedido canal) {
        if (canal != null && canal.esDeApp() && precioApps != null) {
            return precioApps;
        }
        return precio;
    }

    // true si se puede agregar a un pedido: sigue en el menu y no se ha agotado
    public boolean sePuedePedir() {
        return activo && disponible;
    }

    // true si el producto es un combo armado con otros productos
    public boolean esPaquete() {
        return componentes != null && !componentes.isEmpty();
    }

    // true si se vende por kilo (admite decimales en la cantidad)
    public boolean seVendePorKilo() {
        return unidadVenta == UnidadVenta.KILO;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getPrecioApps() {
        return precioApps;
    }

    public void setPrecioApps(BigDecimal precioApps) {
        this.precioApps = precioApps;
    }

    public UnidadVenta getUnidadVenta() {
        return unidadVenta;
    }

    public void setUnidadVenta(UnidadVenta unidadVenta) {
        this.unidadVenta = unidadVenta;
    }

    public boolean isPasaPorParrilla() {
        return pasaPorParrilla;
    }

    public void setPasaPorParrilla(boolean pasaPorParrilla) {
        this.pasaPorParrilla = pasaPorParrilla;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public List<IngredienteExtra> getExtras() {
        return extras;
    }

    public void setExtras(List<IngredienteExtra> extras) {
        this.extras = extras;
    }

    public List<PaqueteComponente> getComponentes() {
        return componentes;
    }

    public void setComponentes(List<PaqueteComponente> componentes) {
        this.componentes = componentes;
    }

    @Override
    public String toString() {
        return "Producto{id=" + idProducto + ", nombre=" + nombre + ", precio=" + precio + ", disponible=" + disponible + "}";
    }
}

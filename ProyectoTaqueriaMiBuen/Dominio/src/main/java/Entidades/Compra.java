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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Compra de insumos (CU-06).
 *
 * Puede ser a un proveedor fijo o una compra directa (mercado, super), en ese
 * caso proveedor va vacio. De contado queda PAGADA; a credito queda PENDIENTE
 * hasta que se cubre con abonos. Los abonos ya estan en la base (tabla
 * abono_compra) pero la pantalla y el servicio de abonos los hace el
 * siguiente equipo.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "compra")
public class Compra implements Serializable {

    // Valores que acepta la columna estado_pago
    public static final String PAGADA = "PAGADA";
    public static final String PENDIENTE = "PENDIENTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer idCompra;

    // null = compra directa sin proveedor fijo
    @ManyToOne
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    // Quien registro la compra
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // true si el proveedor fio y se le va a pagar despues
    @Column(name = "a_credito", nullable = false)
    private boolean aCredito = false;

    // PAGADA o PENDIENTE
    @Column(name = "estado_pago", nullable = false, length = 9)
    private String estadoPago = PAGADA;

    @Column(name = "nota", length = 150)
    private String nota;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)
    private List<DetalleCompra> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "compra")
    private List<AbonoCompra> abonos = new ArrayList<>();

    public Compra() {
    }

    public Compra(Proveedor proveedor, Usuario usuario) {
        this.proveedor = proveedor;
        this.usuario = usuario;
    }

    // Antes de guardar se pone la fecha, se calcula el total y el estado del pago
    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        total = calcularTotal();
        estadoPago = aCredito ? PENDIENTE : PAGADA;
    }

    // Suma de cantidad x costo de cada renglon de la compra
    public BigDecimal calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleCompra d : detalles) {
            suma = suma.add(d.getCantidad().multiply(d.getCostoUnitario()));
        }
        return suma.setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Integer idCompra) {
        this.idCompra = idCompra;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public boolean isACredito() {
        return aCredito;
    }

    public void setACredito(boolean aCredito) {
        this.aCredito = aCredito;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles;
    }

    public List<AbonoCompra> getAbonos() {
        return abonos;
    }

    public void setAbonos(List<AbonoCompra> abonos) {
        this.abonos = abonos;
    }
}

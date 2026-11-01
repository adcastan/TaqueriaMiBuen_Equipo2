package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Un renglon de una compra: que insumo, cuanto y a que costo.
 * La cantidad lleva 3 decimales porque hay cosas que se compran por kilo.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "detalle_compra")
public class DetalleCompra implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_compra")
    private Integer idDetalleCompra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_ingrediente", nullable = false)
    private Ingrediente ingrediente;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    // Precio por unidad (por kilo, por pieza...)
    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    public DetalleCompra() {
    }

    public DetalleCompra(Compra compra, Ingrediente ingrediente, BigDecimal cantidad, BigDecimal costoUnitario) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad comprada debe ser mayor a cero");
        }
        if (costoUnitario == null || costoUnitario.signum() < 0) {
            throw new IllegalArgumentException("El costo no puede ser negativo");
        }
        this.compra = compra;
        this.ingrediente = ingrediente;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    public Integer getIdDetalleCompra() {
        return idDetalleCompra;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public void setIngrediente(Ingrediente ingrediente) {
        this.ingrediente = ingrediente;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }
}

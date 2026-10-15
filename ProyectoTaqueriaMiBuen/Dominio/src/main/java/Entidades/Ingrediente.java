package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Insumos del inventario.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "ingrediente")
public class Ingrediente implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingrediente")
    private Integer idIngrediente;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "unidad", nullable = false, length = 15)
    private String unidad;

    @Column(name = "existencia", nullable = false)
    private double existencia;

    @Column(name = "stock_minimo", nullable = false)
    private double stockMinimo;

    public Ingrediente() {
    }

    public Ingrediente(String nombre, String unidad, double existencia, double stockMinimo) {
        this.nombre = nombre;
        this.unidad = unidad;
        this.existencia = existencia;
        this.stockMinimo = stockMinimo;
    }

    public Integer getIdIngrediente() {
        return idIngrediente;
    }

    public void setIdIngrediente(Integer idIngrediente) {
        this.idIngrediente = idIngrediente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public double getExistencia() {
        return existencia;
    }

    public void setExistencia(double existencia) {
        this.existencia = existencia;
    }

    public double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    @Override
    public String toString() {
        return "Ingrediente{nombre=" + nombre + ", existencia=" + existencia + " " + unidad + "}";
    }
}

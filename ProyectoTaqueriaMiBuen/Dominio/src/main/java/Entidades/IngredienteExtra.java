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

/**
 * Extra que el cliente puede agregar a un producto.
 * Puede estar ligado al ingrediente del inventario que consume.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "ingrediente_extra")
public class IngredienteExtra implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_extra")
    private Integer idExtra;

    @ManyToOne
    @JoinColumn(name = "id_ingrediente")
    private Ingrediente ingrediente;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "precio", nullable = false)
    private double precio;

    public IngredienteExtra() {
    }

    public IngredienteExtra(String nombre, double precio, Ingrediente ingrediente) {
        this.nombre = nombre;
        this.precio = precio;
        this.ingrediente = ingrediente;
    }

    public Integer getIdExtra() {
        return idExtra;
    }

    public void setIdExtra(Integer idExtra) {
        this.idExtra = idExtra;
    }

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public void setIngrediente(Ingrediente ingrediente) {
        this.ingrediente = ingrediente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    @Override
    public String toString() {
        return nombre + " ($" + precio + ")";
    }
}

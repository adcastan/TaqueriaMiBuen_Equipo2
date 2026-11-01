package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Opcion de preparacion o extra que se le puede poner a un producto.
 * Si el precio es cero es una opcion sin costo ("Sin cebolla", "Salsa aparte");
 * si es mayor a cero es un extra que se cobra ("Tortilla de harina" +$5).
 *
 * Ya no se liga al inventario porque los insumos solo se llevan como
 * catalogo (decision del cliente).
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

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    // Lo que se le suma al precio del producto; cero si es solo una indicacion
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    // false si ya no se ofrece (baja logica)
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public IngredienteExtra() {
    }

    public IngredienteExtra(String nombre, BigDecimal precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    // true si es una opcion que no cuesta (solo es indicacion para la parrilla)
    public boolean esSinCosto() {
        return precio == null || precio.signum() == 0;
    }

    public Integer getIdExtra() {
        return idExtra;
    }

    public void setIdExtra(Integer idExtra) {
        this.idExtra = idExtra;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombre + " ($" + precio + ")";
    }
}

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
 * Insumo de la taqueria (CU-07): carne, tortillas, refrescos, desechables...
 *
 * Solo es catalogo: el cliente decidio no llevar existencias, asi que aqui
 * no hay cantidades ni stock minimo. Se guarda el nombre, la unidad y con que
 * proveedor se compra normalmente.
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

    // kg, pza, lt...
    @Column(name = "unidad", nullable = false, length = 15)
    private String unidad;

    // Proveedor con el que normalmente se compra; puede no tener
    @ManyToOne
    @JoinColumn(name = "id_proveedor")
    private Proveedor proveedor;

    // false si ya no se usa (baja logica)
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public Ingrediente() {
    }

    public Ingrediente(String nombre, String unidad, Proveedor proveedor) {
        this.nombre = nombre;
        this.unidad = unidad;
        this.proveedor = proveedor;
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

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Ingrediente{nombre=" + nombre + ", unidad=" + unidad + "}";
    }
}

package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Categoria del menu. El campo orden indica en que posicion se muestra en la
 * pantalla de toma de pedido.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "categoria")
public class Categoria implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private int orden;

    // false si la categoria ya no se usa en el menu (baja logica)
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public Categoria() {
    }

    public Categoria(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Categoria{id=" + idCategoria + ", nombre=" + nombre + "}";
    }
}

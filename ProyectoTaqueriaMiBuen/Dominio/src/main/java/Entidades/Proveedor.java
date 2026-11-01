package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Proveedor que le surte insumos a la taqueria (CU-06).
 * No se borra: si ya no se le compra se da de baja con activo = false.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "proveedor")
public class Proveedor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "correo", length = 100)
    private String correo;

    // Que le surte a la taqueria (carne, tortillas, refrescos...)
    @Column(name = "que_surte", length = 200)
    private String queSurte;

    // false si ya no se le compra (baja logica)
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public Proveedor() {
    }

    public Proveedor(String nombre, String telefono, String correo, String queSurte) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
        this.queSurte = queSurte;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getQueSurte() {
        return queSurte;
    }

    public void setQueSurte(String queSurte) {
        this.queSurte = queSurte;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Proveedor{id=" + idProveedor + ", nombre=" + nombre + "}";
    }
}

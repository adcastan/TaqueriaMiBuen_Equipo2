package DTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos que captura el administrador para registrar o modificar un producto del menu.
 * @author Equipo 2
 */
public class ProductoDTO {

    private int idCategoria;
    private String nombre;
    private String descripcion;
    private double precio;
    private boolean disponible = true;
    private List<Integer> idsExtras = new ArrayList<>();

    public ProductoDTO() {
    }

    public ProductoDTO(int idCategoria, String nombre, String descripcion, double precio) {
        this.idCategoria = idCategoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
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

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public List<Integer> getIdsExtras() {
        return idsExtras;
    }

    public void setIdsExtras(List<Integer> idsExtras) {
        this.idsExtras = idsExtras;
    }
}

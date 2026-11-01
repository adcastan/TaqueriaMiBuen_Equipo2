package DTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Compra de insumos como llega de la pantalla: proveedor (o ninguno si es
 * compra directa), si es a credito, una nota y sus renglones.
 *
 * @author Equipo 2
 */
public class CompraDTO {

    // null = compra directa (mercado, super)
    private Integer idProveedor;
    private boolean aCredito;
    private String nota;
    private List<DetalleCompraDTO> detalles = new ArrayList<>();

    public CompraDTO() {
    }

    public CompraDTO(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public boolean isACredito() {
        return aCredito;
    }

    public void setACredito(boolean aCredito) {
        this.aCredito = aCredito;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public List<DetalleCompraDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompraDTO> detalles) {
        this.detalles = detalles;
    }
}

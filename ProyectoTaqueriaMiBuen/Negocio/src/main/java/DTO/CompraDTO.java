package DTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Compra de insumos que se captura para un proveedor con sus ingredientes.
 *
 * @author Equipo 2
 */
public class CompraDTO {

    private int idProveedor;
    private List<DetalleCompraDTO> detalles = new ArrayList<>();

    public CompraDTO() {
    }

    public CompraDTO(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public List<DetalleCompraDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompraDTO> detalles) {
        this.detalles = detalles;
    }
}

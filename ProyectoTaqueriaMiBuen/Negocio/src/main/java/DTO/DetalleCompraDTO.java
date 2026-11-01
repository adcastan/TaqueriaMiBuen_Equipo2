package DTO;

import java.math.BigDecimal;

/**
 * Un insumo de una compra: cuanto se compro y a que costo por unidad.
 *
 * @author Equipo 2
 */
public class DetalleCompraDTO {

    private int idIngrediente;
    // Puede llevar decimales (ej. 2.5 kg)
    private BigDecimal cantidad;
    private BigDecimal costoUnitario;

    public DetalleCompraDTO() {
    }

    public DetalleCompraDTO(int idIngrediente, BigDecimal cantidad, BigDecimal costoUnitario) {
        this.idIngrediente = idIngrediente;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    public int getIdIngrediente() {
        return idIngrediente;
    }

    public void setIdIngrediente(int idIngrediente) {
        this.idIngrediente = idIngrediente;
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

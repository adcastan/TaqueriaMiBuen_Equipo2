package DTO;

/**
 * Un ingrediente de una compra: cuanto se compro y a que costo.
 *
 * @author Equipo 2
 */
public class DetalleCompraDTO {

    private int idIngrediente;
    private double cantidad;
    private double costoUnitario;

    public DetalleCompraDTO() {
    }

    public DetalleCompraDTO(int idIngrediente, double cantidad, double costoUnitario) {
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

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public double getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(double costoUnitario) {
        this.costoUnitario = costoUnitario;
    }
}

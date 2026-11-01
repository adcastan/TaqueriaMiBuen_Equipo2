package DTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Un producto que el mesero agrego al pedido desde la pantalla. Solo trae ids y
 * datos capturados, el servicio busca el producto real en la base de datos para
 * tomar su precio.
 *
 * @author Equipo 2
 */
public class DetalleDTO {

    private int idProducto;
    // Piezas (entero) o kilos (con decimales) segun el producto; llega como numero en el JSON
    private BigDecimal cantidad;
    private String notaCocina;
    // Ids de los ingredientes extra que eligio el cliente
    private List<Integer> idsExtras = new ArrayList<>();

    public DetalleDTO() {
    }

    public DetalleDTO(int idProducto, BigDecimal cantidad, String notaCocina) {
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.notaCocina = notaCocina;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getNotaCocina() {
        return notaCocina;
    }

    public void setNotaCocina(String notaCocina) {
        this.notaCocina = notaCocina;
    }

    public List<Integer> getIdsExtras() {
        return idsExtras;
    }

    public void setIdsExtras(List<Integer> idsExtras) {
        this.idsExtras = idsExtras;
    }
}

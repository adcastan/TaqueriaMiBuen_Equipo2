/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DTO;

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
    private int cantidad;
    private String notaCocina;
    // Ids de los ingredientes extra que eligio el cliente
    private List<Integer> idsExtras = new ArrayList<>();

    public DetalleDTO() {
    }

    public DetalleDTO(int idProducto, int cantidad, String notaCocina) {
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

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
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

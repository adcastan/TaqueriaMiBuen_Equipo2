/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de un pedido nuevo tal como llegan de la pantalla de toma de pedido.
 * Dependiendo del tipo se usan unos campos u otros: LOCAL usa idMesa, LLEVAR
 * usa nombreCliente y DOMICILIO usa los datos de entrega.
 *
 * @author Equipo 2
 */
public class PedidoDTO {

    public static final String LOCAL = "LOCAL";
    public static final String LLEVAR = "LLEVAR";
    public static final String DOMICILIO = "DOMICILIO";

    private String tipo;
    private Integer idMesa;
    private Integer numeroPersonas;
    // true si la mesa ya esta ocupada y se abre otra cuenta en la misma mesa.
    private boolean cuentaSeparada;
    private String nombreCliente;
    private String telefono;
    private String direccion;
    private String referencias;
    private List<DetalleDTO> detalles = new ArrayList<>();

    public PedidoDTO() {
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(Integer idMesa) {
        this.idMesa = idMesa;
    }

    public Integer getNumeroPersonas() {
        return numeroPersonas;
    }

    public void setNumeroPersonas(Integer numeroPersonas) {
        this.numeroPersonas = numeroPersonas;
    }

    public boolean isCuentaSeparada() {
        return cuentaSeparada;
    }

    public void setCuentaSeparada(boolean cuentaSeparada) {
        this.cuentaSeparada = cuentaSeparada;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencias() {
        return referencias;
    }

    public void setReferencias(String referencias) {
        this.referencias = referencias;
    }

    public List<DetalleDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleDTO> detalles) {
        this.detalles = detalles;
    }
}

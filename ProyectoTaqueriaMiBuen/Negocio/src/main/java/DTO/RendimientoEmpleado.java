/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DTO;

/**
 * Rendimiento de empleados.
 * Cuantos pedidos atendio cada empleado y cuanto vendio en el periodo.
 *
 * @author Equipo 2
 */
public class RendimientoEmpleado {

    private int idUsuario;
    private String nombre;
    private int pedidosPagados;
    private int pedidosCancelados;
    private double totalVendido;

    public RendimientoEmpleado(int idUsuario, String nombre) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
    }

    // Suma un pedido pagado y su total
    public void sumarPagado(double total) {
        pedidosPagados++;
        totalVendido += total;
    }

    public void sumarCancelado() {
        pedidosCancelados++;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPedidosPagados() {
        return pedidosPagados;
    }

    public int getPedidosCancelados() {
        return pedidosCancelados;
    }

    public double getTotalVendido() {
        return totalVendido;
    }
}


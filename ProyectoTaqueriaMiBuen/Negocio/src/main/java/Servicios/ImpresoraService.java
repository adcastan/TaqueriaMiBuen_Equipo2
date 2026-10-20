/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import Entidades.DetalleExtra;
import Entidades.DetallePedido;
import IServicios.IImpresoraService;
import java.util.List;

/**
 * Ticket de respaldo para cocina cuando el monitor (KDS) no esta conectado. 
 * Por ahora no tenemos acceso a la impresora termica, asi que el ticket se imprime en la
 * consola del servidor con el mismo formato que llevaria el papel.
 * @author Equipo 2
 */
public class ImpresoraService implements IImpresoraService{

    @Override
    public void imprimirRespaldo(List<DetallePedido> detalles) {
        StringBuilder ticket = new StringBuilder();
        ticket.append("\n========== TICKET DE COCINA ==========\n");
        for (DetallePedido d : detalles) {
            ticket.append("Turno ").append(d.getOrdenColaPedido()).append(": ")
                    .append(d.getCantidad()).append(" x ").append(d.getProducto().getNombre()).append("\n");
            for (DetalleExtra extra : d.getExtras()) {
                ticket.append("     + ").append(extra.getExtra().getNombre()).append("\n");
            }
            if (d.getNotaCocina() != null && !d.getNotaCocina().isBlank()) {
                ticket.append("     Nota: ").append(d.getNotaCocina()).append("\n");
            }
        }
        ticket.append("=================================================\n");
        System.out.println(ticket);
    }
}

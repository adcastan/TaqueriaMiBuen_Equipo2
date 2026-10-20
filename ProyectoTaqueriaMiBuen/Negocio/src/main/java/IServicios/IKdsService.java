/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import Entidades.DetallePedido;
import java.util.List;

/**
 * Servicio de comunicacion con el monitor de cocina. 
 * La implementa KdsService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 * @author Equipo 2
 */
public interface IKdsService {

    // Registra el latido de la terminal y regresa la cola.
    public List<DetallePedido> obtenerCola(int idTerminal);

    // Marca un detalle como listo.
    public void marcarListo(int idDetalle);

    // true si algun monitor dio latido hace poco.
    public boolean hayKdsConectado();
}

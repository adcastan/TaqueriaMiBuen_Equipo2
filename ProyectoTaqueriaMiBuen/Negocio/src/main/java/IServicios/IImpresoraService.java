/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import Entidades.DetallePedido;
import java.util.List;

/**
 * Servicio de impresion del ticket de respaldo para cocina. 
 * La implementa ImpresoraService. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IImpresoraService {

    // Imprime los detalles cuando el KDS no esta conectado.
    public void imprimirRespaldo(List<DetallePedido> detalles);
}

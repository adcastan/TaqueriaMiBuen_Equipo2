/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.DetallePedido;
import java.util.List;

/**
 * Operaciones de base de datos del monitor de cocina. 
 * La implementa KdsDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IKdsDAO {

    // Detalles que faltan por preparar en orden de turno.
    public List<DetallePedido> consultarPendientes();

    // Marca un detalle como listo.
    public void marcarListo(int idDetalle);

    // Guarda la hora del ultimo aviso de la terminal.
    public void registrarLatido(int idTerminal);

    // Segundos desde el ultimo latido, -1 si nunca hubo.
    public int segundosDesdeUltimoLatido();
}

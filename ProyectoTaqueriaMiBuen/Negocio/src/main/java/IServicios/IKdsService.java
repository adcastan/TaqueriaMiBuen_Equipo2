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

    // Cambia el estado de un detalle: EN_PREPARACION o LISTO.
    public void cambiarEstado(int idDetalle, String estado);

    // true si algun monitor dio latido hace poco.
    public boolean hayKdsConectado();
}

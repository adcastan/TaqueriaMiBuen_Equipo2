/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.DetallePedido;
import Entidades.Pedido;
import Enums.EstadoPedido;
import java.util.List;

/**
 * Operaciones de base de datos de los pedidos y sus detalles. 
 * La implementa PedidoDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 * @author Equipo 2
 */
public interface IPedidoDAO {

    // Guarda un pedido nuevo con sus detalles y regresa su id.
    int insertarPedido(Pedido pedido);

    // Guarda un pedido nuevo con sus detalles y regresa su id.
    int insertarPedido(Pedido pedido, boolean cuentaSeparada);

    // Agrega detalles nuevos a pedidos abiertos.
    void insertarDetalles(List<DetallePedido> detalles);

    // Siguiente turno libre de la cola de cocina.
    int siguienteOrdenCola();

    // Pedido con sus detalles o null.
    Pedido buscarPorId(int idPedido);

    // Pedido abierto o por cobrar de la mesa, o null.
    Pedido buscarActivoPorMesa(int idMesa);

    // Cambia el estado del pedido.
    void actualizarEstado(int idPedido, EstadoPedido estado);

    // Calcula el total y deja el pedido por cobrar.
    Pedido cerrarPedido(int idPedido);

    // Cancela el pedido y guarda el motivo.
    Pedido cancelarPedido(int idPedido, String motivo, String autorizadaPor);

    // Pedidos abiertos o por cobrar.
    List<Pedido> listarActivos();
}

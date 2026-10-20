/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import DTO.DetalleDTO;
import DTO.PedidoDTO;
import DTO.ResultadoEnvio;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.Usuario;
import java.util.List;

/**
 * Servicio de Pedidos.
 * La implementa PedidoService. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IPedidoService {

    // Crea el pedido y lo manda a cocina.
    public ResultadoEnvio crearPedido(PedidoDTO dto, Usuario usuario);

    // Agrega productos a un pedido abierto.
    public ResultadoEnvio agregarDetalles(int idPedido, List<DetalleDTO> detallesDTO);

    // Cierra el pedido.
    public Pedido cerrarPedido(int idPedido);

    // Cancela el pedido, debe autorizar un cajero o administrador.
    public void cancelarPedido(int idPedido, String motivo, Usuario autoriza);

    // Obtiene pedido activo de una mesa o null.
    public Pedido obtenerPedidoActivo(int idMesa);

    // Obtiene pedido con sus detalles.
    public Pedido obtenerPedido(int idPedido);

    // Obtiene mesas para el mapa de mesas.
    public List<Mesa> obtenerMesas();

    // Obtiene pedidos abiertos o por cobrar.
    public List<Pedido> obtenerPedidosActivos();
}


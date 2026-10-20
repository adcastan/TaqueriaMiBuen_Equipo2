/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.MesaDAO;
import DAO.PedidoDAO;
import DTO.DetalleDTO;
import DTO.PedidoDTO;
import DTO.ResultadoEnvio;
import Entidades.DatosEntrega;
import Entidades.DetalleExtra;
import Entidades.DetallePedido;
import Entidades.IngredienteExtra;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import Entidades.PedidoParaLlevar;
import Entidades.Producto;
import Entidades.Usuario;
import Enums.EstadoPedido;
import Enums.Rol;
import Exceptions.MesaOcupadaException;
import Exceptions.NegocioException;
import IAccesoDatos.IMesaDAO;
import IAccesoDatos.IPedidoDAO;
import IServicios.IAutenticacionService;
import IServicios.IImpresoraService;
import IServicios.IKdsService;
import IServicios.IMenuService;
import IServicios.IPedidoService;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas del caso de uso CU-02 Administrar Pedidos: registrar, agregar
 * productos, cerrar y cancelar pedidos. Junta los controles Registrar Pedido,
 * Agregar Detalles, Cerrar Pedido y Cancelar Pedido del diagrama de robustez.
 *
 * @author Equipo 2
 */
public class PedidoService implements IPedidoService{

    // Costo fijo de envio para los pedidos a domicilio
    public static final double COSTO_ENVIO = 25.00;

    private final IPedidoDAO pedidoDAO = new PedidoDAO();
    private final IMesaDAO mesaDAO = new MesaDAO();
    private final IMenuService menuService = new MenuService();
    private final IKdsService kdsService = new KdsService();
    private final IImpresoraService impresoraService = new ImpresoraService();
    private final IAutenticacionService autenticacionService = new AutenticacionService();

    /**
     * Crea el pedido que viene de la pantalla y lo manda a la cola de cocina.
     * Si el monitor de cocina no esta conectado se imprime un ticket de respaldo.
     */
    @Override
    public ResultadoEnvio crearPedido(PedidoDTO dto, Usuario usuario) {
        if (dto == null || dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new NegocioException("El pedido no tiene productos");
        }
        revisarDisponibilidad(dto.getDetalles());

        Pedido pedido = armarPedido(dto, usuario);
        for (DetalleDTO detalleDTO : dto.getDetalles()) {
            pedido.agregarDetalle(armarDetalle(detalleDTO));
        }

        int idPedido;
        try {
            idPedido = pedidoDAO.insertarPedido(pedido, dto.isCuentaSeparada());
        } catch (RuntimeException e) {
            // El DAO avisa con IllegalStateException cuando la mesa ya estaba ocupada
            if (e.getCause() instanceof IllegalStateException) {
                throw new MesaOcupadaException(e.getCause().getMessage());
            }
            throw e;
        }
        return enviarACocina(idPedido, pedido.getFolio(), pedido.getDetalles());
    }

    /**
     * Agrega productos a un pedido abierto. 
     * Los nuevos quedan al final de la cola.
     */
    @Override
    public ResultadoEnvio agregarDetalles(int idPedido, List<DetalleDTO> detallesDTO) {
        if (detallesDTO == null || detallesDTO.isEmpty()) {
            throw new NegocioException("No hay productos nuevos para agregar");
        }
        Pedido pedido = obtenerPedido(idPedido);
        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new NegocioException("Solo se pueden agregar productos a un pedido abierto");
        }
        revisarDisponibilidad(detallesDTO);

        List<DetallePedido> nuevos = new ArrayList<>();
        for (DetalleDTO detalleDTO : detallesDTO) {
            DetallePedido detalle = armarDetalle(detalleDTO);
            detalle.setPedido(pedido);
            nuevos.add(detalle);
        }
        pedidoDAO.insertarDetalles(nuevos);
        return enviarACocina(idPedido, pedido.getFolio(), nuevos);
    }

    /**
     * El cliente pide la cuenta: se calcula el total y queda POR_COBRAR.
     * La mesa sigue ocupada hasta que se cobre (CU-05).
     */
    @Override
    public Pedido cerrarPedido(int idPedido) {
        try {
            return pedidoDAO.cerrarPedido(idPedido);
        } catch (RuntimeException e) {
            throw new NegocioException("No se pudo cerrar el pedido, solo se cierra un pedido abierto", e);
        }
    }

    /**
     * Cancela el pedido. Quien autoriza debe ser cajero o administrador (RN-03).
     */
    @Override
    public void cancelarPedido(int idPedido, String motivo, Usuario autoriza) {
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException("Escribe el motivo de la cancelacion");
        }
        boolean puedeAutorizar = autoriza != null
                && (autenticacionService.tieneRol(autoriza, Rol.CAJERO)
                || autenticacionService.tieneRol(autoriza, Rol.ADMINISTRADOR));
        if (!puedeAutorizar) {
            throw new NegocioException("Solo un cajero o administrador puede autorizar la cancelacion");
        }
        Pedido pedido = obtenerPedido(idPedido);
        if (pedido.getEstado() == EstadoPedido.PAGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new NegocioException("El pedido ya no se puede cancelar");
        }
        pedidoDAO.cancelarPedido(idPedido, motivo.trim(), autoriza.getNombre());
    }

    /**
     * Pedido abierto o por cobrar de una mesa, null si la mesa esta libre.
     */
    @Override
    public Pedido obtenerPedidoActivo(int idMesa) {
        return pedidoDAO.buscarActivoPorMesa(idMesa);
    }

    @Override
    public Pedido obtenerPedido(int idPedido) {
        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            throw new NegocioException("No existe el pedido " + idPedido);
        }
        return pedido;
    }

    /**
     * Mesas con su estado para el mapa de mesas (control Consultar Mesas).
     */
    @Override
    public List<Mesa> obtenerMesas() {
        return mesaDAO.listar();
    }

    /**
     * Pedidos abiertos o por cobrar de todos los tipos.
     */
    @Override
    public List<Pedido> obtenerPedidosActivos() {
        return pedidoDAO.listarActivos();
    }

    // Crea la subclase de pedido segun el tipo que eligio el mesero
    private Pedido armarPedido(PedidoDTO dto, Usuario usuario) {
        String tipo = dto.getTipo() == null ? "" : dto.getTipo();
        switch (tipo) {
            case PedidoDTO.LOCAL:
                if (dto.getIdMesa() == null) {
                    throw new NegocioException("Selecciona una mesa");
                }
                Mesa mesa = mesaDAO.buscarPorId(dto.getIdMesa());
                if (mesa == null) {
                    throw new NegocioException("La mesa no existe");
                }
                return new PedidoLocal(usuario, mesa, dto.getNumeroPersonas());
            case PedidoDTO.LLEVAR:
                if (dto.getNombreCliente() == null || dto.getNombreCliente().isBlank()) {
                    throw new NegocioException("Escribe el nombre del cliente");
                }
                return new PedidoParaLlevar(usuario, dto.getNombreCliente().trim());
            case PedidoDTO.DOMICILIO:
                DatosEntrega datos = new DatosEntrega(dto.getNombreCliente(), dto.getTelefono(),
                        dto.getDireccion(), dto.getReferencias());
                if (!datos.validar()) {
                    throw new NegocioException("Revisa los datos de entrega, el telefono debe tener 10 digitos");
                }
                return new PedidoDomicilio(usuario, COSTO_ENVIO, datos);
            default:
                throw new NegocioException("Tipo de pedido no valido: " + tipo);
        }
    }

    // Convierte lo que capturo el mesero en un DetallePedido con precio y extras reales
    private DetallePedido armarDetalle(DetalleDTO dto) {
        Producto producto = menuService.obtenerProducto(dto.getIdProducto());
        if (producto == null) {
            throw new NegocioException("No existe el producto " + dto.getIdProducto());
        }
        if (dto.getCantidad() <= 0) {
            throw new NegocioException("La cantidad de " + producto.getNombre() + " debe ser mayor a cero");
        }
        String nota = dto.getNotaCocina() == null || dto.getNotaCocina().isBlank() ? null : dto.getNotaCocina().trim();
        DetallePedido detalle = new DetallePedido(producto, dto.getCantidad(), nota);

        if (dto.getIdsExtras() != null) {
            for (Integer idExtra : dto.getIdsExtras()) {
                IngredienteExtra extra = buscarExtra(producto, idExtra);
                detalle.getExtras().add(new DetalleExtra(detalle, extra, extra.getPrecio()));
            }
        }
        return detalle;
    }

    // Solo se aceptan los extras que el producto admite
    private IngredienteExtra buscarExtra(Producto producto, Integer idExtra) {
        for (IngredienteExtra extra : producto.getExtras()) {
            if (extra.getIdExtra().equals(idExtra)) {
                return extra;
            }
        }
        throw new NegocioException(producto.getNombre() + " no admite ese ingrediente extra");
    }

    private void revisarDisponibilidad(List<DetalleDTO> detalles) {
        List<Integer> ids = new ArrayList<>();
        for (DetalleDTO d : detalles) {
            ids.add(d.getIdProducto());
        }
        if (!menuService.validarDisponibilidad(ids)) {
            throw new NegocioException("Hay productos agotados en el pedido, actualiza el menu");
        }
    }

    // Revisa si cocina esta conectada; si no, manda el ticket de respaldo
    private ResultadoEnvio enviarACocina(int idPedido, String folio, List<DetallePedido> detalles) {
        boolean kdsConectado = kdsService.hayKdsConectado();
        if (!kdsConectado) {
            impresoraService.imprimirRespaldo(detalles);
        }
        return new ResultadoEnvio(idPedido, folio, kdsConectado);
    }
}


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
import Exceptions.ProductoNoDisponibleException;
import IAccesoDatos.IMesaDAO;
import IAccesoDatos.IPedidoDAO;
import IServicios.IAutenticacionService;
import IServicios.IImpresoraService;
import IServicios.IKdsService;
import IServicios.IMenuService;
import IServicios.IPedidoService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas del caso de uso CU-02 Administrar Pedidos: registrar, agregar,
 * modificar, cerrar y cancelar pedidos. Junta los controles Registrar Pedido,
 * Agregar Detalles, Cerrar Pedido y Cancelar Pedido del diagrama de robustez.
 *
 * La idea de esta clase es ser el "portero": todo lo que manda la pantalla se
 * revisa aqui antes de tocar la base de datos. Aunque el navegador ya valide,
 * aqui se vuelve a validar, porque cualquiera puede mandar una peticion a mano
 * saltandose la pantalla.
 *
 * @author Equipo 2
 */
public class PedidoService implements IPedidoService {

    // Costo fijo de envio para los pedidos a domicilio
    public static final double COSTO_ENVIO = 25.00;
    public static final int PERSONAS_MINIMO = 1;
    public static final int PERSONAS_MAXIMO = 20;
    public static final int LARGO_NOMBRE = 100;
    public static final int LARGO_DIRECCION = 200;
    public static final int LARGO_MOTIVO = 150;
    // Mas de 50 renglones en un solo envio ya huele a error de captura
    public static final int RENGLONES_POR_ENVIO = 50;
    public static final String MOTIVO_QUITADO = "Quitado por el mesero antes de prepararse";

    private final IPedidoDAO pedidoDAO = new PedidoDAO();
    private final IMesaDAO mesaDAO = new MesaDAO();
    private final IMenuService menuService = new MenuService();
    private final IKdsService kdsService = new KdsService();
    private final IImpresoraService impresoraService = new ImpresoraService();
    private final IAutenticacionService autenticacionService = new AutenticacionService();

    /**
     * Crea el pedido que viene de la pantalla y lo manda a la cola de cocina.
     * Si el monitor de cocina no esta conectado se imprime un ticket de respaldo.
     *
     * Si la misma clave de envio ya habia creado un pedido (porque el mesero le
     * pico "Reintentar" y la primera vez si habia llegado), se regresa ese mismo
     * pedido y no se crea otro.
     */
    @Override
    public ResultadoEnvio crearPedido(PedidoDTO dto, Usuario usuario) {
        if (dto == null) {
            throw new NegocioException("La peticion no trae el pedido");
        }
        String clave = validarClave(dto.getClaveEnvio());
        Pedido yaCreado = pedidoDAO.buscarPorClaveCreacion(clave);
        if (yaCreado != null) {
            return new ResultadoEnvio(yaCreado.getIdPedido(), yaCreado.getFolio(), kdsService.hayKdsConectado(), 1, true);
        }
        validarRenglones(dto.getDetalles());
        revisarDisponibilidad(dto.getDetalles());
        Pedido pedido = armarPedido(dto, usuario);
        pedido.setClaveCreacion(clave);
        for (DetalleDTO detalleDTO : dto.getDetalles()) {
            DetallePedido detalle = armarDetalle(detalleDTO);
            detalle.setClaveEnvio(clave);
            detalle.setAgregadoPor(usuario);
            pedido.agregarDetalle(detalle);
        }
        int idPedido;
        try {
            idPedido = pedidoDAO.insertarPedido(pedido, dto.isCuentaSeparada());
        } catch (RuntimeException e) {
            // Dos peticiones con la misma clave al mismo tiempo: la segunda choca con
            // la llave unica de clave_creacion. No es error, es el mismo pedido.
            Pedido otro = pedidoDAO.buscarPorClaveCreacion(clave);
            if (otro != null) {
                return new ResultadoEnvio(otro.getIdPedido(), otro.getFolio(), kdsService.hayKdsConectado(), 1, true);
            }
            // El DAO avisa con IllegalStateException cuando la mesa ya estaba ocupada
            if (e.getCause() instanceof IllegalStateException) {
                throw new MesaOcupadaException(e.getCause().getMessage());
            }
            throw traducir(e);
        }
        return enviarACocina(idPedido, pedido.getFolio(), pedido.getDetalles(), 1);
    }

    // Version corta que usan las pruebas: sin usuario y sin clave de envio
    @Override
    public ResultadoEnvio agregarDetalles(int idPedido, List<DetalleDTO> detallesDTO) {
        return agregarDetalles(idPedido, detallesDTO, null, null);
    }

    /**
     * Agrega una ronda nueva a un pedido abierto. Los productos nuevos quedan al
     * final de la cola de cocina, despues de lo que ya estaba esperando.
     */
    @Override
    public ResultadoEnvio agregarDetalles(int idPedido, List<DetalleDTO> detallesDTO, Usuario usuario, String claveEnvio) {
        String clave = validarClave(claveEnvio);
        Pedido pedido = obtenerPedido(idPedido);
        // Reintento de algo que ya llego: se contesta igual que la primera vez
        if (clave != null) {
            Integer ronda = pedidoDAO.buscarRondaPorClave(idPedido, clave);
            if (ronda != null) {
                return new ResultadoEnvio(idPedido, pedido.getFolio(), kdsService.hayKdsConectado(), ronda, true);
            }
        }
        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new NegocioException("Solo se pueden agregar productos a un pedido abierto");
        }
        validarRenglones(detallesDTO);
        revisarDisponibilidad(detallesDTO);
        List<DetallePedido> nuevos = new ArrayList<>();
        for (DetalleDTO detalleDTO : detallesDTO) {
            DetallePedido detalle = armarDetalle(detalleDTO);
            detalle.setPedido(pedido);
            nuevos.add(detalle);
        }
        int ronda;
        try {
            ronda = pedidoDAO.insertarDetalles(idPedido, nuevos, usuario, clave);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
        return enviarACocina(idPedido, pedido.getFolio(), nuevos, ronda);
    }

    /**
     * Cambia la cantidad de un producto. Solo mientras sigue en la cola: si el
     * cocinero ya lo empezo, el DAO lo rechaza con el mensaje del flujo 2.2.10.
     */
    @Override
    public Pedido cambiarCantidad(int idPedido, int idDetalle, int cantidad, Usuario usuario) {
        try {
            DetallePedido.validarCantidad(cantidad);
            return pedidoDAO.cambiarCantidad(idPedido, idDetalle, cantidad, usuario);
        } catch (IllegalArgumentException e) {
            throw new NegocioException(e.getMessage());
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    /**
     * Quita un producto que sigue en la cola; desaparece del monitor de cocina.
     */
    @Override
    public Pedido quitarDetalle(int idPedido, int idDetalle, Usuario usuario) {
        try {
            return pedidoDAO.quitarDetalle(idPedido, idDetalle, MOTIVO_QUITADO, usuario);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    /**
     * Cancela un producto que ya se esta preparando o ya esta listo. Como la
     * cocina ya gasto ingredientes, esto solo lo autoriza un cajero o el
     * administrador, y siempre con motivo.
     */
    @Override
    public Pedido cancelarDetalle(int idPedido, int idDetalle, String motivo, Usuario autoriza, Usuario usuario) {
        String motivoLimpio = validarMotivo(motivo);
        validarQuienAutoriza(autoriza);
        try {
            return pedidoDAO.cancelarDetalle(idPedido, idDetalle, motivoLimpio, autoriza.getNombre(), usuario);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    // Version corta sin usuario, la usan las pruebas del equipo
    @Override
    public Pedido cerrarPedido(int idPedido) {
        return cerrarPedido(idPedido, null);
    }

    /**
     * El cliente pide la cuenta: se calcula el total y queda POR_COBRAR.
     * La mesa queda en ambar (pendiente de pago) hasta que el cajero cobre.
     */
    @Override
    public Pedido cerrarPedido(int idPedido, Usuario usuario) {
        try {
            return pedidoDAO.cerrarPedido(idPedido, usuario);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    // Version corta: quien autoriza es el mismo que queda anotado
    @Override
    public void cancelarPedido(int idPedido, String motivo, Usuario autoriza) {
        cancelarPedido(idPedido, motivo, autoriza, autoriza);
    }

    /**
     * Cancela el pedido. Quien autoriza debe ser cajero o administrador (RN-03).
     */
    @Override
    public void cancelarPedido(int idPedido, String motivo, Usuario autoriza, Usuario usuario) {
        String motivoLimpio = validarMotivo(motivo);
        validarQuienAutoriza(autoriza);
        Pedido pedido = obtenerPedido(idPedido);
        if (pedido.getEstado() == EstadoPedido.PAGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new NegocioException("El pedido ya no se puede cancelar");
        }
        try {
            pedidoDAO.cancelarPedido(idPedido, motivoLimpio, autoriza.getNombre(), usuario);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    /**
     * Pedido abierto o por cobrar de una mesa, null si la mesa esta libre.
     */
    @Override
    public Pedido obtenerPedidoActivo(int idMesa) {
        return pedidoDAO.buscarActivoPorMesa(idMesa);
    }

    // Trae el pedido o avisa con un mensaje si no existe
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

    // Crea la subclase de pedido segun el tipo que eligio el mesero (subflujo "Tipar pedido")
    private Pedido armarPedido(PedidoDTO dto, Usuario usuario) {
        if (usuario == null) {
            throw new NegocioException("Inicia sesion para registrar pedidos");
        }
        String tipo = dto.getTipo() == null ? "" : dto.getTipo();
        switch (tipo) {
            case PedidoDTO.LOCAL:
                if (dto.getIdMesa() == null) {
                    throw new NegocioException("Selecciona una mesa");
                }
                Integer personas = dto.getNumeroPersonas();
                if (personas == null || personas < PERSONAS_MINIMO || personas > PERSONAS_MAXIMO) {
                    throw new NegocioException("El numero de personas debe ser de "
                            + PERSONAS_MINIMO + " a " + PERSONAS_MAXIMO);
                }
                Mesa mesa = mesaDAO.buscarPorId(dto.getIdMesa());
                if (mesa == null) {
                    throw new NegocioException("La mesa no existe");
                }
                return new PedidoLocal(usuario, mesa, personas);
            case PedidoDTO.LLEVAR:
                String nombre = limpiar(dto.getNombreCliente());
                if (nombre == null) {
                    throw new NegocioException("Escribe el nombre del cliente");
                }
                revisarLargo(nombre, LARGO_NOMBRE, "El nombre del cliente");
                return new PedidoParaLlevar(usuario, nombre);
            case PedidoDTO.DOMICILIO:
                String nombreEntrega = limpiar(dto.getNombreCliente());
                String telefono = limpiar(dto.getTelefono());
                String direccion = limpiar(dto.getDireccion());
                String referencias = limpiar(dto.getReferencias());
                DatosEntrega datos = new DatosEntrega(nombreEntrega, telefono, direccion, referencias);
                if (!datos.validar()) {
                    throw new NegocioException("Revisa los datos de entrega: nombre, direccion y telefono de 10 digitos");
                }
                revisarLargo(nombreEntrega, LARGO_NOMBRE, "El nombre del cliente");
                revisarLargo(direccion, LARGO_DIRECCION, "La direccion");
                revisarLargo(referencias, LARGO_DIRECCION, "Las referencias");
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
        if (dto.getCantidad() <= 0 || dto.getCantidad() > DetallePedido.CANTIDAD_MAXIMA) {
            throw new NegocioException("La cantidad de " + producto.getNombre() + " debe ser de 1 a "
                    + DetallePedido.CANTIDAD_MAXIMA);
        }
        String nota = limpiar(dto.getNotaCocina());
        revisarLargo(nota, DetallePedido.LARGO_MAXIMO_NOTA, "La nota para cocina");
        DetallePedido detalle = new DetallePedido(producto, dto.getCantidad(), nota);
        if (dto.getIdsExtras() != null) {
            // Un mismo extra dos veces seria cobrarle doble el queso al cliente
            Set<Integer> vistos = new HashSet<>();
            for (Integer idExtra : dto.getIdsExtras()) {
                if (!vistos.add(idExtra)) {
                    throw new NegocioException("Un ingrediente extra viene repetido en " + producto.getNombre());
                }
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

    // Flujo 2.2.8: sin productos no hay nada que mandar a cocina
    private void validarRenglones(List<DetalleDTO> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new NegocioException("Agregue al menos un producto");
        }
        if (detalles.size() > RENGLONES_POR_ENVIO) {
            throw new NegocioException("Son demasiados renglones en un solo envio (maximo " + RENGLONES_POR_ENVIO + ")");
        }
    }

    /**
     * Flujo 2.2.11: si un producto se dio de baja del menu mientras el mesero
     * capturaba, se avisa con el nombre de cada uno para que lo quite o cambie.
     */
    private void revisarDisponibilidad(List<DetalleDTO> detalles) {
        List<Integer> agotados = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        for (DetalleDTO d : detalles) {
            Producto producto = menuService.obtenerProducto(d.getIdProducto());
            if (producto == null) {
                throw new NegocioException("No existe el producto " + d.getIdProducto());
            }
            if (!producto.isDisponible() && !agotados.contains(producto.getIdProducto())) {
                agotados.add(producto.getIdProducto());
                nombres.add(producto.getNombre());
            }
        }
        if (!agotados.isEmpty()) {
            throw new ProductoNoDisponibleException("Producto no disponible: " + String.join(", ", nombres), agotados);
        }
    }

    // Solo un cajero o el administrador pueden autorizar cancelaciones (RN-03)
    private void validarQuienAutoriza(Usuario autoriza) {
        boolean puedeAutorizar = autoriza != null
                && (autenticacionService.tieneRol(autoriza, Rol.CAJERO)
                || autenticacionService.tieneRol(autoriza, Rol.ADMINISTRADOR));
        if (!puedeAutorizar) {
            throw new NegocioException("Solo un cajero o administrador puede autorizar la cancelacion");
        }
    }

    // El motivo es obligatorio y no puede pasar de 150 letras
    private String validarMotivo(String motivo) {
        String limpio = limpiar(motivo);
        if (limpio == null) {
            throw new NegocioException("Escribe el motivo de la cancelacion");
        }
        revisarLargo(limpio, LARGO_MOTIVO, "El motivo");
        return limpio;
    }

    // La clave la inventa la pantalla; solo aceptamos letras, numeros y guiones
    private String validarClave(String clave) {
        String limpia = limpiar(clave);
        if (limpia == null) {
            return null;
        }
        if (!limpia.matches("[A-Za-z0-9-]{8,40}")) {
            throw new NegocioException("La clave de envio no es valida");
        }
        return limpia;
    }

    // Quita espacios de sobra; un texto vacio cuenta como si no lo hubieran escrito
    private String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    // Revisa que un texto no se pase del largo que aguanta la columna de la base
    private void revisarLargo(String texto, int maximo, String campo) {
        if (texto != null && texto.length() > maximo) {
            throw new NegocioException(campo + " no puede pasar de " + maximo + " caracteres");
        }
    }

    // Convierte los avisos del DAO en mensajes para el usuario; lo demas se deja pasar como error del sistema
    private RuntimeException traducir(RuntimeException e) {
        Throwable causa = e.getCause();
        if (causa instanceof IllegalStateException || causa instanceof IllegalArgumentException) {
            return new NegocioException(causa.getMessage(), e);
        }
        return e;
    }

    // Revisa si cocina esta conectada; si no, manda el ticket de respaldo
    private ResultadoEnvio enviarACocina(int idPedido, String folio, List<DetallePedido> detalles, int ronda) {
        boolean kdsConectado = kdsService.hayKdsConectado();
        if (!kdsConectado) {
            impresoraService.imprimirRespaldo(detalles);
        }
        return new ResultadoEnvio(idPedido, folio, kdsConectado, ronda, false);
    }
}

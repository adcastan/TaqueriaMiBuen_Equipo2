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
import Entidades.PaqueteComponente;
import Enums.CanalPedido;
import Enums.TipoRepartidor;
import Herramientas.Dinero;
import java.math.BigDecimal;
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

    // El envio ya no es fijo: depende de la zona y lo captura quien toma el pedido.
    // Este tope solo evita errores de dedo (un 2500 en vez de 25)
    public static final BigDecimal ENVIO_MAXIMO = new BigDecimal("500.00");
    public static final int LARGO_REPARTIDOR = 100;
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
            // El canal decide el precio: los pedidos de apps usan el precio de apps
            DetallePedido detalle = armarDetalle(detalleDTO, pedido.getCanal());
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
            // Las rondas nuevas usan el mismo canal (y precio) con el que se abrio el pedido
            DetallePedido detalle = armarDetalle(detalleDTO, pedido.getCanal());
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
    public Pedido cambiarCantidad(int idPedido, int idDetalle, BigDecimal cantidad, Usuario usuario) {
        try {
            // La regla de pieza entera o kilo con decimales la revisa DetallePedido
            // dentro de la transaccion, porque ahi ya se sabe que producto es
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

    /**
     * Cuentas vivas de una mesa. Con esto la pantalla puede mostrar las
     * cuentas separadas de una mesa o de un grupo de mesas juntas (RN-14).
     */
    @Override
    public List<Pedido> obtenerCuentasDeMesa(int idMesa) {
        return pedidoDAO.listarActivosPorMesa(idMesa);
    }

    /**
     * Pasa la cuenta a otra mesa libre: se mueve todo y queda en la bitacora
     * quien lo hizo (RN-12). Las reglas de que mesa se puede usar estan en el DAO
     * porque se revisan con las mesas bloqueadas.
     */
    @Override
    public Pedido cambiarMesa(int idPedido, int idMesaDestino, Usuario usuario) {
        if (usuario == null) {
            throw new NegocioException("Inicia sesion para cambiar de mesa");
        }
        try {
            return pedidoDAO.cambiarMesa(idPedido, idMesaDestino, usuario);
        } catch (IllegalArgumentException e) {
            throw new NegocioException(e.getMessage());
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    /**
     * Junta una mesa libre con la mesa principal del grupo (RN-13). Ejemplo:
     * un grupo de 10 ocupa la 3 y la 4; la principal es la 3.
     */
    @Override
    public void juntarMesas(int idPrincipal, int idMesa) {
        try {
            mesaDAO.juntarMesas(idPrincipal, idMesa);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    // Separa una mesa de su grupo; las cuentas se quedan en la principal
    @Override
    public void separarMesa(int idMesa) {
        try {
            mesaDAO.separarMesa(idMesa);
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    // Crea la subclase de pedido segun el tipo que eligio el mesero (subflujo "Tipar pedido")
    private Pedido armarPedido(PedidoDTO dto, Usuario usuario) {
        if (usuario == null) {
            throw new NegocioException("Inicia sesion para registrar pedidos");
        }
        String tipo = dto.getTipo() == null ? "" : dto.getTipo();
        switch (tipo) {
            case PedidoDTO.LOCAL:
                // En el local el canal siempre es MESA, diga lo que diga la pantalla
                dto.setCanal(CanalPedido.MESA.name());
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
                PedidoLocal local = new PedidoLocal(usuario, mesa, personas);
                local.setCanal(CanalPedido.MESA);
                return local;
            case PedidoDTO.LLEVAR:
                String nombre = limpiar(dto.getNombreCliente());
                if (nombre == null) {
                    throw new NegocioException("Escribe el nombre del cliente");
                }
                revisarLargo(nombre, LARGO_NOMBRE, "El nombre del cliente");
                PedidoParaLlevar llevar = new PedidoParaLlevar(usuario, nombre);
                // Para llevar puede llegar por mostrador, telefono, WhatsApp o una app
                llevar.setCanal(leerCanal(dto.getCanal(), CanalPedido.MOSTRADOR,
                        CanalPedido.MOSTRADOR, CanalPedido.TELEFONO, CanalPedido.WHATSAPP,
                        CanalPedido.DIDI_FOOD, CanalPedido.UBER_EATS));
                return llevar;
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
                BigDecimal envio = validarEnvio(dto.getCostoEnvio());
                PedidoDomicilio domicilio = new PedidoDomicilio(usuario, envio, datos);
                // A domicilio llega por telefono o WhatsApp (lo de apps lo reparte la app: es "para llevar")
                domicilio.setCanal(leerCanal(dto.getCanal(), CanalPedido.TELEFONO,
                        CanalPedido.TELEFONO, CanalPedido.WHATSAPP));
                domicilio.setTipoRepartidor(leerRepartidor(dto.getTipoRepartidor()));
                String repartidor = limpiar(dto.getRepartidor());
                revisarLargo(repartidor, LARGO_REPARTIDOR, "El repartidor");
                domicilio.setRepartidor(repartidor);
                return domicilio;
            default:
                throw new NegocioException("Tipo de pedido no valido: " + tipo);
        }
    }

    /**
     * Convierte lo que capturo el mesero en un DetallePedido con precio y extras
     * reales. El precio sale del menu (nunca de la pantalla) y depende del canal.
     */
    private DetallePedido armarDetalle(DetalleDTO dto, CanalPedido canal) {
        Producto producto = menuService.obtenerProducto(dto.getIdProducto());
        if (producto == null) {
            throw new NegocioException("No existe el producto " + dto.getIdProducto());
        }
        try {
            // Pieza: entero de 1 a 99. Kilo: con decimales, mayor a cero
            DetallePedido.validarCantidad(producto, dto.getCantidad());
        } catch (IllegalArgumentException e) {
            throw new NegocioException(e.getMessage());
        }
        String nota = limpiar(dto.getNotaCocina());
        revisarLargo(nota, DetallePedido.LARGO_MAXIMO_NOTA, "La nota para cocina");
        DetallePedido detalle = new DetallePedido(producto, dto.getCantidad(), nota, canal);
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
            if (!sePuedePedir(producto) && !agotados.contains(producto.getIdProducto())) {
                agotados.add(producto.getIdProducto());
                nombres.add(producto.getNombre());
            }
        }
        if (!agotados.isEmpty()) {
            throw new ProductoNoDisponibleException("Producto no disponible: " + String.join(", ", nombres), agotados);
        }
    }

    /**
     * Un producto se puede pedir si sigue en el menu y no se agoto. Si es un
     * paquete, tambien tienen que estar todas sus piezas (sin carne no hay
     * "paquete 3 tacos").
     */
    private boolean sePuedePedir(Producto producto) {
        if (!producto.sePuedePedir()) {
            return false;
        }
        for (PaqueteComponente pieza : producto.getComponentes()) {
            if (!pieza.getComponente().sePuedePedir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lee el canal que manda la pantalla. Si no viene se usa el de siempre para
     * ese tipo de pedido; si viene uno que no aplica (ej. DiDi para domicilio)
     * se rechaza.
     */
    private CanalPedido leerCanal(String texto, CanalPedido porDefecto, CanalPedido... permitidos) {
        String limpio = limpiar(texto);
        if (limpio == null) {
            return porDefecto;
        }
        for (CanalPedido c : permitidos) {
            if (c.name().equals(limpio)) {
                return c;
            }
        }
        throw new NegocioException("El canal " + limpio + " no aplica para este tipo de pedido");
    }

    // PROPIO si no dicen nada; EXTERNO si lo lleva DiDi, Uber u otro servicio
    private TipoRepartidor leerRepartidor(String texto) {
        String limpio = limpiar(texto);
        if (limpio == null) {
            return TipoRepartidor.PROPIO;
        }
        try {
            return TipoRepartidor.valueOf(limpio);
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Tipo de repartidor no valido: " + limpio);
        }
    }

    // El envio es obligatorio en domicilio (puede ser 0) y no puede pasar del tope
    private BigDecimal validarEnvio(BigDecimal envio) {
        if (envio == null) {
            throw new NegocioException("Captura el costo de envio (puede ser 0)");
        }
        if (envio.signum() < 0 || envio.compareTo(ENVIO_MAXIMO) > 0) {
            throw new NegocioException("El costo de envio debe ser de $0 a $" + ENVIO_MAXIMO);
        }
        return Dinero.redondear(envio);
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

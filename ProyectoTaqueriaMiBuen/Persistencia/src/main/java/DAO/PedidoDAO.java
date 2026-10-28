package DAO;

import Conexion.JPAUtil;
import Entidades.DatosEntrega;
import Entidades.DetallePedido;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import Entidades.Usuario;
import Enums.EstadoMesa;
import Enums.EstadoPedido;
import IAccesoDatos.IPedidoDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a datos de los pedidos y sus detalles.
 *
 * Aqui pasa lo delicado del caso de uso: varios meseros trabajan al mismo
 * tiempo sobre las mismas mesas y la misma cola de cocina. Para que nadie se
 * pise, cada operacion que cambia algo hace su trabajo dentro de UNA
 * transaccion y bloquea las filas que va a tocar siempre en el mismo orden:
 *
 *   1) la mesa,  2) el pedido,  3) el contador de turnos de la cocina.
 *
 * Es como la fila de las tortillas: si todos se forman igual, nadie se cuela
 * y nadie se queda atorado esperando al otro.
 *
 * Los errores de regla (mesa ocupada, producto ya en preparacion, etc.) se
 * avisan con IllegalStateException o IllegalArgumentException; la capa de
 * Negocio las convierte en mensajes para el usuario.
 *
 * @author Equipo 2
 */
public class PedidoDAO implements IPedidoDAO {

    private final MesaDAO mesaDAO = new MesaDAO();

    // Version corta: pedido normal, sin cuenta separada
    @Override
    public int insertarPedido(Pedido pedido) {
        return insertarPedido(pedido, false);
    }

    /**
     * Inserta un pedido nuevo con sus detalles (ronda 1) y regresa su id.
     * Si es pedido local y la mesa ya esta ocupada, se rechaza, salvo que el
     * mesero haya pedido abrir una cuenta separada (RN-01).
     */
    @Override
    public int insertarPedido(Pedido pedido, boolean cuentaSeparada) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Mesa mesa = null;
            if (pedido instanceof PedidoLocal) {
                PedidoLocal local = (PedidoLocal) pedido;
                // Paso 1 del orden de bloqueo: la mesa. Si dos meseros pican la misma
                // mesa libre al mismo tiempo, el segundo espera aqui y luego ve que ya esta ocupada
                mesa = mesaDAO.bloquearParaActualizar(em, local.getMesa().getIdMesa());
                if (mesa.getEstado() != EstadoMesa.DISPONIBLE && !cuentaSeparada) {
                    throw new IllegalStateException("La mesa " + mesa.getNumero() + " ya esta ocupada");
                }
                local.setMesa(mesa);
            }
            LocalDateTime ahora = LocalDateTime.now();
            int turno = siguienteOrdenCola(em);
            for (DetallePedido detalle : pedido.getDetalles()) {
                detalle.setOrdenColaPedido(turno);
                detalle.setRonda(1);
                detalle.setFechaEnvio(ahora);
                turno++;
            }
            pedido.registrarCambio(pedido.getUsuario());
            if (pedido instanceof PedidoDomicilio) {
                // Primero se guarda el pedido para que exista el id_pedido que usa datos_entrega
                PedidoDomicilio domicilio = (PedidoDomicilio) pedido;
                DatosEntrega datos = domicilio.getDatosEntrega();
                domicilio.setDatosEntrega(null);
                em.persist(domicilio);
                em.flush();
                domicilio.setDatosEntrega(datos);
                em.persist(datos);
            } else {
                em.persist(pedido);
            }
            if (mesa != null) {
                mesaDAO.recalcularEstado(em, mesa);
            }
            em.getTransaction().commit();
            return pedido.getIdPedido();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo insertar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Agrega una ronda nueva a un pedido abierto. Primero revisa, ya con el
     * pedido bloqueado, si esa misma clave de envio ya habia llegado: si si,
     * regresa la ronda de aquella vez sin volver a insertar (asi "Reintentar"
     * nunca manda dos comandas iguales a cocina).
     */
    @Override
    public int insertarDetalles(int idPedido, List<DetallePedido> detalles, Usuario usuario, String claveEnvio) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
            if (pedido == null) {
                throw new IllegalArgumentException("No existe el pedido " + idPedido);
            }
            if (claveEnvio != null) {
                Integer repetida = rondaPorClave(em, idPedido, claveEnvio);
                if (repetida != null) {
                    em.getTransaction().commit();
                    return repetida;
                }
            }
            if (pedido.getEstado() != EstadoPedido.ABIERTO) {
                throw new IllegalStateException("Solo se pueden agregar productos a un pedido abierto");
            }
            int ronda = pedido.siguienteRonda();
            int turno = siguienteOrdenCola(em);
            LocalDateTime ahora = LocalDateTime.now();
            Usuario quien = usuario == null ? null : em.find(Usuario.class, usuario.getIdUsuario());
            for (DetallePedido detalle : detalles) {
                detalle.setOrdenColaPedido(turno);
                detalle.setRonda(ronda);
                detalle.setFechaEnvio(ahora);
                detalle.setClaveEnvio(claveEnvio);
                if (quien != null) {
                    detalle.setAgregadoPor(quien);
                }
                turno++;
                pedido.agregarDetalle(detalle);
                em.persist(detalle);
            }
            pedido.registrarCambio(quien);
            em.getTransaction().commit();
            return ronda;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudieron insertar los detalles: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Version que ya traian las pruebas del equipo: cada detalle sabe a que
     * pedido va. Se agrupan como una sola ronda por pedido.
     */
    @Override
    public void insertarDetalles(List<DetallePedido> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        int idPedido = detalles.get(0).getPedido().getIdPedido();
        insertarDetalles(idPedido, detalles, null, null);
    }

    // Pregunta si una clave de envio ya llego antes y que ronda le toco
    @Override
    public Integer buscarRondaPorClave(int idPedido, String claveEnvio) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return rondaPorClave(em, idPedido, claveEnvio);
        } finally {
            em.close();
        }
    }

    // Busca el pedido que se creo con esa clave; si existe, el envio era un reintento
    @Override
    public Pedido buscarPorClaveCreacion(String claveCreacion) {
        if (claveCreacion == null) {
            return null;
        }
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Pedido> lista = em.createQuery(
                    "SELECT p FROM Pedido p WHERE p.claveCreacion = :clave", Pedido.class)
                    .setParameter("clave", claveCreacion)
                    .getResultList();
            if (lista.isEmpty()) {
                return null;
            }
            cargarDetalles(lista.get(0));
            return lista.get(0);
        } finally {
            em.close();
        }
    }

    // Turno que le tocaria al siguiente producto (solo para consultar, no aparta nada)
    @Override
    public int siguienteOrdenCola() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return siguienteOrdenCola(em);
        } finally {
            em.close();
        }
    }

    // Pedido con sus detalles y extras ya cargados; null si no existe
    @Override
    public Pedido buscarPorId(int idPedido) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            Pedido pedido = em.find(Pedido.class, idPedido);
            if (pedido != null) {
                cargarDetalles(pedido);
            }
            return pedido;
        } finally {
            em.close();
        }
    }

    /**
     * Pedido activo de una mesa. Si la mesa tiene cuentas separadas se regresa
     * la mas nueva; las demas se ven en "Pedidos activos".
     */
    @Override
    public Pedido buscarActivoPorMesa(int idMesa) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<PedidoLocal> lista = em.createQuery(
                    "SELECT p FROM PedidoLocal p WHERE p.mesa.idMesa = :idMesa "
                    + "AND p.estado IN (:abierto, :porCobrar) ORDER BY p.fechaApertura DESC", PedidoLocal.class)
                    .setParameter("idMesa", idMesa)
                    .setParameter("abierto", EstadoPedido.ABIERTO)
                    .setParameter("porCobrar", EstadoPedido.POR_COBRAR)
                    .getResultList();
            if (lista.isEmpty()) {
                return null;
            }
            PedidoLocal pedido = lista.get(0);
            cargarDetalles(pedido);
            return pedido;
        } finally {
            em.close();
        }
    }

    // Cambia el estado del pedido a mano, con el pedido bloqueado mientras tanto
    @Override
    public void actualizarEstado(int idPedido, EstadoPedido estado) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
            pedido.setEstado(estado);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Cambia cuantas piezas lleva un producto. Solo si sigue en cola y el
     * pedido sigue abierto; si el cocinero ya lo empezo, la entidad lo rechaza.
     */
    @Override
    public Pedido cambiarCantidad(int idPedido, int idDetalle, int cantidad, Usuario usuario) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = pedidoAbiertoBloqueado(em, idPedido);
            DetallePedido detalle = detalleDelPedido(em, pedido, idDetalle);
            detalle.cambiarCantidad(cantidad);
            pedido.registrarCambio(usuarioEn(em, usuario));
            em.getTransaction().commit();
            cargarDetalles(pedido);
            return pedido;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cambiar la cantidad: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Quita un producto que sigue en cola. No se borra de la base: queda como
     * CANCELADO con su motivo, y como el monitor de cocina solo muestra los
     * pendientes, desaparece de la pantalla de cocina en el siguiente refresco.
     */
    @Override
    public Pedido quitarDetalle(int idPedido, int idDetalle, String motivo, Usuario usuario) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = pedidoAbiertoBloqueado(em, idPedido);
            DetallePedido detalle = detalleDelPedido(em, pedido, idDetalle);
            if (!detalle.sePuedeModificar()) {
                throw new IllegalStateException("Este producto ya se esta preparando y no se puede modificar");
            }
            Usuario quien = usuarioEn(em, usuario);
            detalle.cancelar(motivo, quien == null ? null : quien.getNombre());
            pedido.registrarCambio(quien);
            em.getTransaction().commit();
            cargarDetalles(pedido);
            return pedido;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo quitar el producto: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Cancela un producto que ya se esta preparando o ya esta listo. Esto solo
     * lo puede autorizar un cajero (eso lo revisa Negocio antes de llamar aqui).
     */
    @Override
    public Pedido cancelarDetalle(int idPedido, int idDetalle, String motivo, String autorizadoPor, Usuario usuario) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Pedido pedido = pedidoAbiertoBloqueado(em, idPedido);
            DetallePedido detalle = detalleDelPedido(em, pedido, idDetalle);
            detalle.cancelar(motivo, autorizadoPor);
            pedido.registrarCambio(usuarioEn(em, usuario));
            em.getTransaction().commit();
            cargarDetalles(pedido);
            return pedido;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cancelar el producto: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Version corta sin usuario, la usan las pruebas del equipo
    @Override
    public Pedido cerrarPedido(int idPedido) {
        return cerrarPedido(idPedido, null);
    }

    /**
     * El cliente pidio la cuenta: se congela el total, el pedido queda POR_COBRAR
     * y la mesa se recalcula (si ya no le queda nada abierto se pone en ambar).
     */
    @Override
    public Pedido cerrarPedido(int idPedido, Usuario usuario) {
        Integer idMesa = mesaDAO.idMesaDelPedido(idPedido);
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Mesa mesa = idMesa == null ? null : mesaDAO.bloquearParaActualizar(em, idMesa);
            Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
            if (pedido == null) {
                throw new IllegalArgumentException("No existe el pedido " + idPedido);
            }
            pedido.cerrar();
            pedido.registrarCambio(usuarioEn(em, usuario));
            if (mesa != null) {
                mesaDAO.recalcularEstado(em, mesa);
            }
            em.getTransaction().commit();
            cargarDetalles(pedido);
            return pedido;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cerrar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Version corta sin usuario, la usan las pruebas del equipo
    @Override
    public Pedido cancelarPedido(int idPedido, String motivo, String autorizadaPor) {
        return cancelarPedido(idPedido, motivo, autorizadaPor, null);
    }

    /**
     * Cancela el pedido completo: se guarda el motivo y quien autorizo, se sacan
     * de cocina los productos que no estaban listos y la mesa se recalcula.
     * Nunca se borra nada.
     */
    @Override
    public Pedido cancelarPedido(int idPedido, String motivo, String autorizadaPor, Usuario usuario) {
        Integer idMesa = mesaDAO.idMesaDelPedido(idPedido);
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Mesa mesa = idMesa == null ? null : mesaDAO.bloquearParaActualizar(em, idMesa);
            Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
            if (pedido == null) {
                throw new IllegalArgumentException("No existe el pedido " + idPedido);
            }
            pedido.cancelar(motivo, autorizadaPor);
            pedido.registrarCambio(usuarioEn(em, usuario));
            em.persist(pedido.getCancelacion());
            if (mesa != null) {
                mesaDAO.recalcularEstado(em, mesa);
            }
            em.getTransaction().commit();
            cargarDetalles(pedido);
            return pedido;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cancelar el pedido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Pedidos que siguen vivos (abiertos o por cobrar), del mas viejo al mas nuevo
    @Override
    public List<Pedido> listarActivos() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Pedido> pedidos = em.createQuery(
                    "SELECT p FROM Pedido p WHERE p.estado IN (:abierto, :porCobrar) ORDER BY p.fechaApertura",
                    Pedido.class)
                    .setParameter("abierto", EstadoPedido.ABIERTO)
                    .setParameter("porCobrar", EstadoPedido.POR_COBRAR)
                    .getResultList();
            for (Pedido pedido : pedidos) {
                cargarDetalles(pedido);
            }
            return pedidos;
        } finally {
            em.close();
        }
    }

    // Pedidos que ya pidieron la cuenta; es la lista que ve el cajero
    @Override
    public List<Pedido> listarPorCobrar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Pedido> pedidos = em.createQuery(
                    "SELECT p FROM Pedido p WHERE p.estado = :porCobrar ORDER BY p.fechaApertura", Pedido.class)
                    .setParameter("porCobrar", EstadoPedido.POR_COBRAR)
                    .getResultList();
            for (Pedido pedido : pedidos) {
                cargarDetalles(pedido);
            }
            return pedidos;
        } finally {
            em.close();
        }
    }

    // Pedidos abiertos entre dos fechas, con todo cargado, para los reportes
    @Override
    public List<Pedido> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Pedido> pedidos = em.createQuery(
                    "SELECT p FROM Pedido p WHERE p.fechaApertura BETWEEN :inicio AND :fin ORDER BY p.fechaApertura", Pedido.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getResultList();
            for (Pedido pedido : pedidos) {
                cargarDetalles(pedido);
            }
            return pedidos;
        } finally {
            em.close();
        }
    }

    /**
     * Siguiente turno de la cocina, dentro de la transaccion que ya esta abierta.
     * El "FOR UPDATE" es el truco: aparta el ultimo turno hasta que termine la
     * transaccion, asi si dos meseros mandan al mismo tiempo, el segundo espera
     * su turno (literal) y nunca salen dos comandas con el mismo numero.
     */
    private int siguienteOrdenCola(EntityManager em) {
        Object ultimo = em.createNativeQuery(
                "SELECT COALESCE(MAX(orden_cola_pedido), 0) FROM detalle_pedido FOR UPDATE")
                .getSingleResult();
        return ((Number) ultimo).intValue() + 1;
    }

    // Busca, dentro de la transaccion abierta, la ronda que se guardo con esa clave
    private Integer rondaPorClave(EntityManager em, int idPedido, String claveEnvio) {
        List<Integer> rondas = em.createQuery(
                "SELECT d.ronda FROM DetallePedido d WHERE d.pedido.idPedido = :id AND d.claveEnvio = :clave",
                Integer.class)
                .setParameter("id", idPedido)
                .setParameter("clave", claveEnvio)
                .setMaxResults(1)
                .getResultList();
        return rondas.isEmpty() ? null : rondas.get(0);
    }

    // Bloquea el pedido y revisa que siga abierto; un pedido por cobrar ya no se toca (RN-04)
    private Pedido pedidoAbiertoBloqueado(EntityManager em, int idPedido) {
        Pedido pedido = em.find(Pedido.class, idPedido, LockModeType.PESSIMISTIC_WRITE);
        if (pedido == null) {
            throw new IllegalArgumentException("No existe el pedido " + idPedido);
        }
        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new IllegalStateException("El pedido ya no esta abierto, no se puede modificar");
        }
        return pedido;
    }

    // Busca el producto dentro de ESE pedido; asi nadie puede mover un producto de otra mesa
    private DetallePedido detalleDelPedido(EntityManager em, Pedido pedido, int idDetalle) {
        DetallePedido detalle = em.find(DetallePedido.class, idDetalle, LockModeType.PESSIMISTIC_WRITE);
        if (detalle == null || !detalle.getPedido().getIdPedido().equals(pedido.getIdPedido())) {
            throw new IllegalArgumentException("Ese producto no pertenece al pedido");
        }
        return detalle;
    }

    // El usuario de la sesion viene "suelto"; aqui lo traemos a esta transaccion
    private Usuario usuarioEn(EntityManager em, Usuario usuario) {
        if (usuario == null || usuario.getIdUsuario() == null) {
            return null;
        }
        return em.find(Usuario.class, usuario.getIdUsuario());
    }

    // Lee detalles y extras antes de cerrar el EntityManager para usarlos despues
    private void cargarDetalles(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            detalle.getExtras().size();
            if (detalle.getAgregadoPor() != null) {
                detalle.getAgregadoPor().getNombre();
            }
        }
        if (pedido.getUsuarioModifico() != null) {
            pedido.getUsuarioModifico().getNombre();
        }
    }
}

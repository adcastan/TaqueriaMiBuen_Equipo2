package DAO;

import Conexion.JPAUtil;
import Enums.EstadoMesa;
import Enums.EstadoPedido;
import Entidades.Mesa;
import Entidades.PedidoLocal;
import IAccesoDatos.IMesaDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de las mesas.
 *
 * @author Equipo 2
 */
public class MesaDAO implements IMesaDAO{

    // Guarda una mesa nueva
    @Override
    public void registrar(Mesa mesa) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(mesa);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar la mesa: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Busca una mesa por su id; null si no existe
    @Override
    public Mesa buscarPorId(int idMesa) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Mesa.class, idMesa);
        } finally {
            em.close();
        }
    }

    // Busca una mesa por el numero que trae pintado (1 al 8)
    @Override
    public Mesa buscarPorNumero(int numero) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Mesa> lista = em.createQuery("SELECT m FROM Mesa m WHERE m.numero = :numero", Mesa.class)
                    .setParameter("numero", numero)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    // Mesas en verde, listas para recibir clientes
    @Override
    public List<Mesa> listarDisponibles() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT m FROM Mesa m WHERE m.estado = :estado ORDER BY m.numero", Mesa.class)
                    .setParameter("estado", EstadoMesa.DISPONIBLE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Cambia el estado de una mesa a mano. Para los pedidos mejor se usa recalcularEstado
    @Override
    public void actualizarEstado(int idMesa, EstadoMesa estado) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Mesa mesa = em.find(Mesa.class, idMesa);
            mesa.setEstado(estado);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar la mesa: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Todas las mesas ordenadas por numero.
     */
    @Override
    public List<Mesa> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT m FROM Mesa m ORDER BY m.numero", Mesa.class).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Lee la mesa y la bloquea hasta que termine la transaccion que recibe,
     * asi nadie mas la puede tomar al mismo tiempo. Se usa dentro de
     * PedidoDAO.insertarPedido.
     */
    @Override
    public Mesa bloquearParaActualizar(EntityManager em, int idMesa) {
        return em.find(Mesa.class, idMesa, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Decide el color de la mesa viendo los pedidos que siguen vivos en ella.
     * Es la misma regla para todos (enviar, cerrar, cancelar y cobrar), asi no
     * hay forma de que una pantalla diga "libre" y otra "ocupada":
     *
     * - si hay al menos un pedido ABIERTO, la mesa esta OCUPADA;
     * - si no hay abiertos pero si alguno POR_COBRAR, queda PENDIENTE_PAGO;
     * - si ya no queda nada (todo pagado o cancelado), vuelve a DISPONIBLE.
     *
     * Se usa con la transaccion del que la llama para que el cambio del pedido
     * y el de la mesa se guarden juntos o no se guarde ninguno.
     */
    @Override
    public void recalcularEstado(EntityManager em, Mesa mesa) {
        // flush para que la consulta vea los cambios de esta misma transaccion
        em.flush();
        // Lectura con candado (FOR SHARE): asi leemos lo ultimo que se guardo y no
        // una foto vieja de la base, aunque otro mesero haya cobrado hace un segundo
        List<PedidoLocal> vivos = em.createQuery(
                "SELECT p FROM PedidoLocal p WHERE p.mesa.idMesa = :idMesa "
                + "AND p.estado IN (:abierto, :porCobrar)", PedidoLocal.class)
                .setParameter("idMesa", mesa.getIdMesa())
                .setParameter("abierto", EstadoPedido.ABIERTO)
                .setParameter("porCobrar", EstadoPedido.POR_COBRAR)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .getResultList();
        List<EstadoPedido> estados = new ArrayList<>();
        for (PedidoLocal p : vivos) {
            estados.add(p.getEstado());
        }
        if (estados.contains(EstadoPedido.ABIERTO)) {
            mesa.setEstado(EstadoMesa.OCUPADA);
        } else if (estados.contains(EstadoPedido.POR_COBRAR)) {
            mesa.setEstado(EstadoMesa.PENDIENTE_PAGO);
        } else {
            mesa.setEstado(EstadoMesa.DISPONIBLE);
        }
    }

    /**
     * Regresa el id de la mesa de un pedido local (o null si es para llevar o a
     * domicilio). Se lee en una conexion aparte ANTES de empezar la transaccion
     * grande, para poder bloquear primero la mesa y despues el pedido. Si todos
     * bloquean en el mismo orden (mesa, pedido, turno) nunca se quedan esperando
     * uno al otro para siempre.
     */
    @Override
    public Integer idMesaDelPedido(int idPedido) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Integer> ids = em.createQuery(
                    "SELECT p.mesa.idMesa FROM PedidoLocal p WHERE p.idPedido = :id", Integer.class)
                    .setParameter("id", idPedido)
                    .getResultList();
            return ids.isEmpty() ? null : ids.get(0);
        } finally {
            em.close();
        }
    }
}

package DAO;

import Conexion.JPAUtil;
import Entidades.Caja;
import Entidades.Mesa;
import Entidades.Pago;
import Entidades.Pedido;
import Entidades.Usuario;
import Enums.EstadoPedido;
import Enums.MetodoPago;
import IAccesoDatos.IPagoDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Acceso a datos de los pagos.
 *
 * @author Equipo 2
 */
public class PagoDAO implements IPagoDAO {

    private final MesaDAO mesaDAO = new MesaDAO();

    /**
     * Todo va en una transaccion: si algo falla no queda un pago sin pedido
     * pagado ni una mesa liberada sin cobrar. 
     * El pedido se bloquea para que dos cajeros no lo cobren al mismo tiempo.
     */
    @Override
    public Pago registrarPago(Pago pago) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            // Mismo orden de bloqueo que en PedidoDAO: primero la mesa, luego el pedido
            Integer idMesa = mesaDAO.idMesaDelPedido(pago.getPedido().getIdPedido());
            em.getTransaction().begin();
            Mesa mesa = idMesa == null ? null : mesaDAO.bloquearParaActualizar(em, idMesa);
            Pedido pedido = em.find(Pedido.class, pago.getPedido().getIdPedido(), LockModeType.PESSIMISTIC_WRITE);
            if (pedido.getEstado() != EstadoPedido.POR_COBRAR) {
                throw new IllegalStateException("Solo se puede cobrar un pedido por cobrar");
            }
            pedido.setEstado(EstadoPedido.PAGADO);
            pago.setPedido(pedido);
            // Lo de las apps no pasa por caja, por eso la caja puede venir vacia
            if (pago.getCaja() != null) {
                pago.setCaja(em.find(Caja.class, pago.getCaja().getIdCaja()));
            }
            pago.setUsuario(em.find(Usuario.class, pago.getUsuario().getIdUsuario()));
            em.persist(pago);

            // La mesa se recalcula con la misma regla de siempre: si le quedan
            // otras cuentas sigue ocupada o en ambar, si no, vuelve a verde
            if (mesa != null) {
                mesaDAO.recalcularEstado(em, mesa);
            }
            em.getTransaction().commit();
            return pago;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el pago: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // El pago de un pedido, o null si todavia no se cobra
    @Override
    public Pago buscarPorPedido(int idPedido) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Pago> lista = em.createQuery(
                    "SELECT p FROM Pago p WHERE p.pedido.idPedido = :idPedido", Pago.class)
                    .setParameter("idPedido", idPedido)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    // Todos los pagos que entraron a una caja, en orden de hora
    @Override
    public List<Pago> listarPorCaja(int idCaja) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Pago p WHERE p.caja.idCaja = :idCaja ORDER BY p.fecha", Pago.class)
                    .setParameter("idCaja", idCaja)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Cuanto se cobro en una caja con un metodo (efectivo, tarjeta o transferencia); sirve para el corte
    @Override
    public BigDecimal totalPorMetodo(int idCaja, MetodoPago metodo) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            BigDecimal total = em.createQuery(
                    "SELECT SUM(p.monto) FROM Pago p WHERE p.caja.idCaja = :idCaja AND p.metodo = :metodo", BigDecimal.class)
                    .setParameter("idCaja", idCaja)
                    .setParameter("metodo", metodo)
                    .getSingleResult();
            // SUM regresa null si no hay pagos; en ese caso son cero pesos
            return total == null ? new BigDecimal("0.00") : total;
        } finally {
            em.close();
        }
    }
}

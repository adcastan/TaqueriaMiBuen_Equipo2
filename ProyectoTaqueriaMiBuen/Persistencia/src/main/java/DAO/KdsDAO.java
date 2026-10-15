package DAO;

import Conexion.JPAUtil;
import Enums.EstadoPedido;
import Entidades.DetallePedido;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Consultas del monitor de cocina sobre la cola de detalles.
 *
 * @author Equipo 2
 */
public class KdsDAO {

    /**
     * Detalles que faltan por preparar en orden de llegada.
     * No incluye los de pedidos cancelados.
     */
    public List<DetallePedido> consultarPendientes() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<DetallePedido> pendientes = em.createQuery(
                    "SELECT d FROM DetallePedido d WHERE d.listo = false AND d.pedido.estado <> :cancelado "
                    + "ORDER BY d.ordenColaPedido", DetallePedido.class)
                    .setParameter("cancelado", EstadoPedido.CANCELADO)
                    .getResultList();
            for (DetallePedido detalle : pendientes) {
                detalle.getExtras().size();
            }
            return pendientes;
        } finally {
            em.close();
        }
    }

    public void marcarListo(int idDetalle) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            DetallePedido detalle = em.find(DetallePedido.class, idDetalle);
            detalle.marcarListo();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo marcar el detalle como listo: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * La terminal de cocina avisa que sigue conectada.
     */
    public void registrarLatido(int idTerminal) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("UPDATE terminal_kds SET ultimo_latido = NOW() WHERE id_terminal = ?1")
                    .setParameter(1, idTerminal)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el latido: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Segundos desde la ultima conexión de cualquier terminal.
     * Regresa -1 si ninguna terminal se ha conectado.
     */
    public int segundosDesdeUltimoLatido() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            Object resultado = em.createNativeQuery(
                    "SELECT TIMESTAMPDIFF(SECOND, MAX(ultimo_latido), NOW()) FROM terminal_kds")
                    .getSingleResult();
            if (resultado == null) {
                return -1;
            }
            return ((Number) resultado).intValue();
        } finally {
            em.close();
        }
    }
}

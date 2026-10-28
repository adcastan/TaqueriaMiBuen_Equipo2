package DAO;

import Conexion.JPAUtil;
import Enums.EstadoPedido;
import Enums.EstadoPreparacion;
import jakarta.persistence.LockModeType;
import Entidades.DetallePedido;
import IAccesoDatos.IKdsDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Consultas del monitor de cocina sobre la cola de detalles.
 *
 * @author Equipo 2
 */
public class KdsDAO implements IKdsDAO{

    /**
     * Detalles que faltan por preparar en orden de llegada: los que estan en
     * cola y los que ya se estan haciendo. Lo listo y lo cancelado ya no se
     * muestra, por eso cuando el mesero quita algo desaparece solito de cocina.
     */
    @Override
    public List<DetallePedido> consultarPendientes() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<DetallePedido> pendientes = em.createQuery(
                    "SELECT d FROM DetallePedido d WHERE d.estadoPreparacion IN (:pendiente, :enPreparacion) "
                    + "AND d.pedido.estado <> :cancelado ORDER BY d.ordenColaPedido", DetallePedido.class)
                    .setParameter("pendiente", EstadoPreparacion.PENDIENTE)
                    .setParameter("enPreparacion", EstadoPreparacion.EN_PREPARACION)
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

    // Atajo que ya usaban las pantallas: es lo mismo que cambiar el estado a LISTO
    @Override
    public void marcarListo(int idDetalle) {
        cambiarEstado(idDetalle, EstadoPreparacion.LISTO);
    }

    /**
     * Avanza un producto en la cocina: de la cola a "en preparacion", o a
     * "listo". El renglon se bloquea mientras se cambia, por si el mesero lo
     * esta quitando justo en ese segundo; el que llegue segundo se entera de
     * que el producto ya cambio.
     */
    @Override
    public void cambiarEstado(int idDetalle, EstadoPreparacion nuevo) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            DetallePedido detalle = em.find(DetallePedido.class, idDetalle, LockModeType.PESSIMISTIC_WRITE);
            if (detalle == null) {
                throw new IllegalArgumentException("No existe el producto " + idDetalle + " en la cola");
            }
            if (detalle.getPedido().getEstado() == EstadoPedido.CANCELADO
                    || detalle.getEstadoPreparacion() == EstadoPreparacion.CANCELADO) {
                throw new IllegalStateException("Ese producto fue cancelado, ya no se prepara");
            }
            if (nuevo == EstadoPreparacion.EN_PREPARACION) {
                detalle.iniciarPreparacion();
            } else if (nuevo == EstadoPreparacion.LISTO) {
                detalle.marcarListo();
            } else {
                throw new IllegalArgumentException("Desde cocina solo se marca en preparacion o listo");
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cambiar el estado: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Anota la hora en que la pantalla de cocina pidio su cola; asi sabemos que sigue prendida
    @Override
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
    @Override
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

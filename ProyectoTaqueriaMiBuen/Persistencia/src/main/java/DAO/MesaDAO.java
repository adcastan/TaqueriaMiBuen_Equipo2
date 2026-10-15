package DAO;

import Conexion.JPAUtil;
import Enums.EstadoMesa;
import Entidades.Mesa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;

/**
 * Acceso a datos de las mesas.
 *
 * @author Equipo 2
 */
public class MesaDAO {

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

    public Mesa buscarPorId(int idMesa) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Mesa.class, idMesa);
        } finally {
            em.close();
        }
    }

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
    public Mesa bloquearParaActualizar(EntityManager em, int idMesa) {
        return em.find(Mesa.class, idMesa, LockModeType.PESSIMISTIC_WRITE);
    }
}

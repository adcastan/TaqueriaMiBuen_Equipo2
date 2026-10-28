package DAO;

import Conexion.JPAUtil;
import Entidades.Caja;
import Entidades.Corte;
import IAccesoDatos.ICajaDAO;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a datos de la caja y del corte de caja.
 *
 * @author Equipo 2
 */
public class CajaDAO implements ICajaDAO {

    // Guarda la caja nueva que abrio el cajero al iniciar su turno
    @Override
    public void abrir(Caja caja) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(caja);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo abrir la caja: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Regresa la caja que esta abierta ahorita, o null si nadie ha abierto turno
    @Override
    public Caja buscarAbierta() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Caja> lista = em.createQuery(
                    "SELECT c FROM Caja c WHERE c.abierta = true ORDER BY c.fechaApertura DESC", Caja.class)
                    .setMaxResults(1)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    // Busca una caja por su id; null si no existe
    @Override
    public Caja buscarPorId(int idCaja) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Caja.class, idCaja);
        } finally {
            em.close();
        }
    }

    /**
     * Calcula el efectivo esperado y regresa el corte. 
     * Aqui solo se guardan los dos cambios juntos.
     */
    @Override
    public Corte cerrar(int idCaja, double totalVentas, double efectivoContado) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Caja caja = em.find(Caja.class, idCaja);
            Corte corte = caja.cerrar(totalVentas, efectivoContado);
            em.persist(corte);
            em.getTransaction().commit();
            return corte;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cerrar la caja: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Regresa el corte que se hizo al cerrar esa caja, o null si sigue abierta
    @Override
    public Corte buscarCorte(int idCaja) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Corte> lista = em.createQuery(
                    "SELECT c FROM Corte c WHERE c.caja.idCaja = :idCaja", Corte.class)
                    .setParameter("idCaja", idCaja)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    // Cortes hechos entre dos fechas, para revisar como fueron los turnos
    @Override
    public List<Corte> listarCortes(LocalDateTime inicio, LocalDateTime fin) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT c FROM Corte c WHERE c.fecha BETWEEN :inicio AND :fin ORDER BY c.fecha", Corte.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

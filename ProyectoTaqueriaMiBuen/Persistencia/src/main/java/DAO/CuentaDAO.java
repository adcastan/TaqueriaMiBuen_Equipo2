package DAO;

import Conexion.JPAUtil;
import Entidades.Cuenta;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de las cuentas de usuario.
 *
 * @author Equipo 2
 */
public class CuentaDAO {

    /**
     * Guarda la cuenta. Si su usuario es nuevo tambien se guarda.
     */
    public void registrar(Cuenta cuenta) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cuenta);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar la cuenta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public Cuenta buscarPorUsuario(int idUsuario) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Cuenta> lista = em.createQuery(
                    "SELECT c FROM Cuenta c WHERE c.usuario.idUsuario = :id", Cuenta.class)
                    .setParameter("id", idUsuario)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    /**
     * Cuentas de usuarios activos. Como el PIN esta guardado con hash,
     * para iniciar sesion se recorren estas cuentas y se usa validarPin().
     */
    public List<Cuenta> buscarActivas() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cuenta c WHERE c.usuario.activo = true", Cuenta.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

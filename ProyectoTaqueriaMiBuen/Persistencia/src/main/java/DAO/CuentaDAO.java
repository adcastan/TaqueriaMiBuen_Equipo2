package DAO;

import Conexion.JPAUtil;
import Entidades.Cuenta;
import Enums.Rol;
import IAccesoDatos.ICuentaDAO;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;

/**
 * Acceso a datos de las cuentas de usuario.
 *
 * @author Equipo 2
 */
public class CuentaDAO implements ICuentaDAO {

    /**
     * Registra la cuenta. 
     */
    @Override
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

    // Cuenta (PIN y rol) de un empleado; null si no tiene
    @Override
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
     * Cuentas de usuarios activos. 
     * Como el PIN esta guardado con hash, para iniciar sesion se recorren estas cuentas y 
     * se usa validarPin().
     */
    @Override
    public List<Cuenta> buscarActivas() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cuenta c WHERE c.usuario.activo = true", Cuenta.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Todas las cuentas, activas o no, para la pantalla de empleados
    @Override
    public List<Cuenta> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cuenta c ORDER BY c.usuario.nombre", Cuenta.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Cambia los roles de un empleado (por ejemplo, que tambien pueda estar en caja)
    @Override
    public void actualizarRoles(int idUsuario, Set<Rol> roles) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Cuenta cuenta = em.createQuery(
                    "SELECT c FROM Cuenta c WHERE c.usuario.idUsuario = :id", Cuenta.class)
                    .setParameter("id", idUsuario)
                    .getSingleResult();
            // Se vacia la lista y se llena con los nuevos; JPA actualiza la tabla cuenta_rol
            cuenta.getRoles().clear();
            cuenta.getRoles().addAll(roles);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cambiar el rol: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}

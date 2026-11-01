package DAO;

import Conexion.JPAUtil;
import Entidades.Usuario;
import IAccesoDatos.IUsuarioDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de los usuarios.
 * @author Equipo 2
 */
public class UsuarioDAO implements IUsuarioDAO {

    // Busca un empleado por su id; null si no existe
    @Override
    public Usuario buscarPorId(int idUsuario) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Usuario.class, idUsuario);
        } finally {
            em.close();
        }
    }

    // Todos los empleados, activos y dados de baja
    @Override
    public List<Usuario> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT u FROM Usuario u ORDER BY u.nombre", Usuario.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Guarda los cambios de nombre o telefono de un empleado
    @Override
    public void actualizar(Usuario usuario) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(usuario);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar el usuario: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * No se borra al usuario porque sus pedidos y compras lo siguen usando,
     * solo se marca como inactivo y ya no puede iniciar sesion.
     */
    @Override
    public void cambiarActivo(int idUsuario, boolean activo) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.find(Usuario.class, idUsuario);
            usuario.setActivo(activo);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cambiar el estado del usuario: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}

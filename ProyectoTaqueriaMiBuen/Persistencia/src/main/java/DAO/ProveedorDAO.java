package DAO;

import Conexion.JPAUtil;
import Entidades.Proveedor;
import IAccesoDatos.IProveedorDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de los proveedores. No se borran: la baja es logica
 * (activo = false) y se guarda con actualizar().
 * @author Equipo 2
 */
public class ProveedorDAO implements IProveedorDAO {

    // Guarda un proveedor nuevo
    @Override
    public void registrar(Proveedor proveedor) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(proveedor);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el proveedor: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Guarda los cambios de datos de un proveedor
    @Override
    public void actualizar(Proveedor proveedor) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(proveedor);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar el proveedor: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Busca un proveedor por su id; null si no existe
    @Override
    public Proveedor buscarPorId(int idProveedor) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Proveedor.class, idProveedor);
        } finally {
            em.close();
        }
    }

    // Busca un proveedor por su nombre; sirve para no repetirlo
    @Override
    public Proveedor buscarPorNombre(String nombre) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Proveedor> lista = em.createQuery(
                    "SELECT p FROM Proveedor p WHERE p.nombre = :nombre", Proveedor.class)
                    .setParameter("nombre", nombre)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    // Todos los proveedores ordenados por nombre (tambien los dados de baja)
    @Override
    public List<Proveedor> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT p FROM Proveedor p ORDER BY p.nombre", Proveedor.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

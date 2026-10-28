package DAO;

import Conexion.JPAUtil;
import Entidades.Categoria;
import IAccesoDatos.ICategoriaDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de las categorias del menu.
 *
 * @author Equipo 2
 */
public class CategoriaDAO implements ICategoriaDAO {

    // Guarda una categoria nueva del menu (Tacos, Bebidas...)
    @Override
    public void registrar(Categoria categoria) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(categoria);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar la categoria: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Busca una categoria por su nombre exacto; sirve para no repetirlas
    @Override
    public Categoria buscarPorNombre(String nombre) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Categoria> lista = em.createQuery(
                    "SELECT c FROM Categoria c WHERE c.nombre = :nombre", Categoria.class)
                    .setParameter("nombre", nombre)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    // Todas las categorias en el orden en que se muestran en el menu
    @Override
    public List<Categoria> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT c FROM Categoria c ORDER BY c.orden", Categoria.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Busca una categoria por su id; null si no existe
    @Override
    public Categoria buscarPorId(int idCategoria) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Categoria.class, idCategoria);
        } finally {
            em.close();
        }
    }

    // Guarda los cambios de nombre u orden de una categoria
    @Override
    public void actualizar(Categoria categoria) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(categoria);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar la categoria: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}

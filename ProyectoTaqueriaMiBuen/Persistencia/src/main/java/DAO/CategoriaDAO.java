package DAO;

import Conexion.JPAUtil;
import Entidades.Categoria;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de las categorias del menu.
 *
 * @author Equipo 2
 */
public class CategoriaDAO {

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

    public List<Categoria> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT c FROM Categoria c ORDER BY c.orden", Categoria.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

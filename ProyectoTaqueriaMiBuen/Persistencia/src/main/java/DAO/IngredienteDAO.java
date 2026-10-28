package DAO;

import Conexion.JPAUtil;
import Entidades.Ingrediente;
import Entidades.ProductoIngrediente;
import IAccesoDatos.IIngredienteDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de los ingredientes.
 * @author Equipo 2
 */
public class IngredienteDAO implements IIngredienteDAO {

    // Guarda un ingrediente nuevo del almacen
    @Override
    public void registrar(Ingrediente ingrediente) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(ingrediente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el ingrediente: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Guarda los cambios de un ingrediente (nombre, unidad, existencia, minimo)
    @Override
    public void actualizar(Ingrediente ingrediente) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(ingrediente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar el ingrediente: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // Busca un ingrediente por su id; null si no existe
    @Override
    public Ingrediente buscarPorId(int idIngrediente) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Ingrediente.class, idIngrediente);
        } finally {
            em.close();
        }
    }

    // Busca un ingrediente por su nombre; sirve para no darlo de alta dos veces
    @Override
    public Ingrediente buscarPorNombre(String nombre) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Ingrediente> lista = em.createQuery(
                    "SELECT i FROM Ingrediente i WHERE i.nombre = :nombre", Ingrediente.class)
                    .setParameter("nombre", nombre)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    // Todos los ingredientes ordenados por nombre
    @Override
    public List<Ingrediente> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT i FROM Ingrediente i ORDER BY i.nombre", Ingrediente.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Ingredientes que ya llegaron o bajaron de su minimo: hay que comprar
    @Override
    public List<Ingrediente> listarBajoStock() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT i FROM Ingrediente i WHERE i.existencia <= i.stockMinimo ORDER BY i.nombre", Ingrediente.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Guarda cuanto de un ingrediente lleva un producto (la receta)
    @Override
    public void registrarReceta(ProductoIngrediente receta) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(receta);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar la receta: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // La receta de un producto: que ingredientes lleva y cuanto de cada uno
    @Override
    public List<ProductoIngrediente> listarReceta(int idProducto) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT r FROM ProductoIngrediente r WHERE r.producto.idProducto = :idProducto "
                    + "ORDER BY r.ingrediente.nombre", ProductoIngrediente.class)
                    .setParameter("idProducto", idProducto)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

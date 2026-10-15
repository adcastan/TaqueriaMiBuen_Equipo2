package DAO;

import Conexion.JPAUtil;
import Entidades.Producto;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los productos del menu.
 *
 * @author Equipo 2
 */
public class ProductoDAO {

    public void registrar(Producto producto) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(producto);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el producto: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public Producto buscarPorNombre(String nombre) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Producto> lista = em.createQuery(
                    "SELECT p FROM Producto p WHERE p.nombre = :nombre", Producto.class)
                    .setParameter("nombre", nombre)
                    .getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } finally {
            em.close();
        }
    }

    /**
     * Busca el producto y carga tambien los extras que admite.
     */
    public Producto buscarConExtras(String nombre) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Producto> lista = em.createQuery(
                    "SELECT p FROM Producto p WHERE p.nombre = :nombre", Producto.class)
                    .setParameter("nombre", nombre)
                    .getResultList();
            if (lista.isEmpty()) {
                return null;
            }
            Producto producto = lista.get(0);
            producto.getExtras().size(); // se leen antes de cerrar el EntityManager
            return producto;
        } finally {
            em.close();
        }
    }

    /**
     * Productos que se pueden vender, en el orden del menu.
     */
    public List<Producto> listarDisponibles() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Producto p WHERE p.disponible = true ORDER BY p.categoria.orden, p.nombre",
                    Producto.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * De la lista de ids, regresa los productos que ya no estan disponibles.
     */
    public List<Integer> buscarNoDisponibles(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT p.idProducto FROM Producto p WHERE p.idProducto IN :ids AND p.disponible = false",
                    Integer.class)
                    .setParameter("ids", ids)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

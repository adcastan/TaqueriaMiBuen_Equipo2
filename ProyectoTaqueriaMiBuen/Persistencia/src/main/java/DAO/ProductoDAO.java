package DAO;

import Conexion.JPAUtil;
import Entidades.Producto;
import IAccesoDatos.IProductoDAO;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los productos del menu.
 *
 * @author Equipo 2
 */
public class ProductoDAO implements IProductoDAO {

    @Override
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

    @Override
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
    @Override
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
    @Override
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
    @Override
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

    /**
     * Busca un producto por su id y carga sus extras. Lo usa el servicio de
     * pedidos para armar cada detalle.
     *
     * @param idProducto
     * @return
     */
    @Override
    public Producto buscarPorId(int idProducto) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            Producto producto = em.find(Producto.class, idProducto);
            if (producto != null) {
                producto.getExtras().size(); // Antes de cerrar el EntityManager, se leen.
            }
            return producto;
        } finally {
            em.close();
        }
    }

    /**
     * Todo el menu con sus extras, ordenado por categoria. 
     * Los agotados se muestran deshabilitados en la pantalla.
     * @return
     */
    @Override
    public List<Producto> listarConExtras() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Producto> productos = em.createQuery(
                    "SELECT p FROM Producto p ORDER BY p.categoria.orden, p.nombre", Producto.class)
                    .getResultList();
            for (Producto p : productos) {
                p.getExtras().size();
            }
            return productos;
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(Producto producto) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(producto);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar el producto: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public void cambiarDisponible(int idProducto, boolean disponible) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Producto producto = em.find(Producto.class, idProducto);
            producto.setDisponible(disponible);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo cambiar la disponibilidad: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}

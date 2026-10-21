/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

    @Override
    public Ingrediente buscarPorId(int idIngrediente) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Ingrediente.class, idIngrediente);
        } finally {
            em.close();
        }
    }

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

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO;

import Conexion.JPAUtil;
import Entidades.Proveedor;
import IAccesoDatos.IProveedorDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de los proveedores.
 * @author Equipo 2
 */
public class ProveedorDAO implements IProveedorDAO {

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

    /**
     * Si el proveedor ya tiene compras la base de datos no deja borrarlo.
     */
    @Override
    public void eliminar(int idProveedor) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            Proveedor proveedor = em.find(Proveedor.class, idProveedor);
            if (proveedor != null) {
                em.remove(proveedor);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo eliminar el proveedor: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public Proveedor buscarPorId(int idProveedor) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Proveedor.class, idProveedor);
        } finally {
            em.close();
        }
    }

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

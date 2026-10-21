/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO;

import Conexion.JPAUtil;
import Entidades.IngredienteExtra;
import IAccesoDatos.IIngredienteExtraDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de los ingredientes extra que se pueden agregar a un producto.
 *
 * @author Equipo 2
 */
public class IngredienteExtraDAO implements IIngredienteExtraDAO {

    @Override
    public void registrar(IngredienteExtra extra) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(extra);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el extra: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(IngredienteExtra extra) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(extra);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo actualizar el extra: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public IngredienteExtra buscarPorId(int idExtra) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(IngredienteExtra.class, idExtra);
        } finally {
            em.close();
        }
    }

    @Override
    public List<IngredienteExtra> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT e FROM IngredienteExtra e ORDER BY e.nombre", IngredienteExtra.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

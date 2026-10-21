/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

    @Override
    public Usuario buscarPorId(int idUsuario) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.find(Usuario.class, idUsuario);
        } finally {
            em.close();
        }
    }

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

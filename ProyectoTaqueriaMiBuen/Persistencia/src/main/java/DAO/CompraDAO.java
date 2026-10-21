/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO;

import Conexion.JPAUtil;
import Entidades.Compra;
import Entidades.DetalleCompra;
import Entidades.Ingrediente;
import Entidades.Proveedor;
import IAccesoDatos.ICompraDAO;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a datos de las compras de insumos a los proveedores.
 *
 * @author Equipo 2
 */
public class CompraDAO implements ICompraDAO {

    @Override
    public void registrar(Compra compra) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            compra.setProveedor(em.find(Proveedor.class, compra.getProveedor().getIdProveedor()));
            for (DetalleCompra detalle : compra.getDetalles()) {
                Ingrediente ingrediente = em.find(Ingrediente.class, detalle.getIngrediente().getIdIngrediente());
                ingrediente.setExistencia(ingrediente.getExistencia() + detalle.getCantidad());
                detalle.setIngrediente(ingrediente);
                detalle.setCompra(compra);
            }
            em.persist(compra);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar la compra: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public Compra buscarPorId(int idCompra) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            Compra compra = em.find(Compra.class, idCompra);
            if (compra != null) {
                compra.getDetalles().size();
            }
            return compra;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Compra> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            List<Compra> compras = em.createQuery(
                    "SELECT c FROM Compra c WHERE c.fecha BETWEEN :inicio AND :fin ORDER BY c.fecha", Compra.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getResultList();
            // Se leen los detalles antes de cerrar el EntityManager
            for (Compra compra : compras) {
                compra.getDetalles().size();
            }
            return compras;
        } finally {
            em.close();
        }
    }

    @Override
    public long contarPorProveedor(int idProveedor) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(c) FROM Compra c WHERE c.proveedor.idProveedor = :id", Long.class)
                    .setParameter("id", idProveedor)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}

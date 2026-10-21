/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO;

import Conexion.JPAUtil;
import Entidades.ReporteVenta;
import IAccesoDatos.IReporteVentaDAO;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Acceso a datos de los reportes de ventas.
 * @author Equipo 2
 */
public class ReporteVentaDAO implements IReporteVentaDAO {

    @Override
    public void registrar(ReporteVenta reporte) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(reporte);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("No se pudo registrar el reporte: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public ReporteVenta buscarPorId(int idReporte) {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            ReporteVenta reporte = em.find(ReporteVenta.class, idReporte);
            if (reporte != null) {
                reporte.getPedidos().size();
            }
            return reporte;
        } finally {
            em.close();
        }
    }

    @Override
    public List<ReporteVenta> listar() {
        EntityManager em = JPAUtil.obtenerEntityManager();
        try {
            return em.createQuery("SELECT r FROM ReporteVenta r ORDER BY r.fechaFin DESC", ReporteVenta.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

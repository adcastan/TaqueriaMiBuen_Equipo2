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

    // Guarda un reporte de ventas ya calculado
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

    // Busca un reporte con sus pedidos; null si no existe
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

    // Todos los reportes de ventas que se han generado
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

package IAccesoDatos;

import Entidades.ReporteVenta;
import java.util.List;

/**
 * Operaciones de base de datos de los reportes de ventas.
 * La implementa ReporteVentaDAO.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IReporteVentaDAO {

    // Guarda un reporte ya generado con sus pedidos.
    public void registrar(ReporteVenta reporte);

    // Reporte por su id o null.
    public ReporteVenta buscarPorId(int idReporte);

    // Todos los reportes, del mas reciente al mas viejo.
    public List<ReporteVenta> listar();
    
    
}

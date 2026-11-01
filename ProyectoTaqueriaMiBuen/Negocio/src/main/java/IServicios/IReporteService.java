package IServicios;

import DTO.ReporteCompras;
import java.time.LocalDateTime;

/**
 * Servicio de reportes.
 * La implementa ReporteService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 * El reporte de ventas en vivo lo agrega el siguiente equipo.
 *
 * @author Equipo 2
 */
public interface IReporteService {

    // Genera el reporte de compras de insumos de un periodo con su total.
    public ReporteCompras generarReporteCompras(LocalDateTime inicio, LocalDateTime fin);
}

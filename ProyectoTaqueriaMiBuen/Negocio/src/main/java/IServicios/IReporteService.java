package IServicios;

import DTO.ReporteCompras;
import DTO.ReporteVentas;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Servicio de reportes.
 * La implementa ReporteService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IReporteService {

    // Genera el reporte de compras de insumos de un periodo con su total.
    public ReporteCompras generarReporteCompras(LocalDateTime inicio, LocalDateTime fin);

    // Genera el reporte de ventas (CU-09) con los pedidos de un periodo.
    public ReporteVentas generarReporteVentas(LocalDate inicio, LocalDate fin);

    // Reporte de ventas del periodo anterior de la misma duracion, para comparar.
    public ReporteVentas generarReporteAnterior(LocalDate inicio, LocalDate fin);
}
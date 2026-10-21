/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import DTO.RendimientoEmpleado;
import DTO.ReporteCompras;
import Entidades.ReporteVenta;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de reportes. 
 * La implementa ReporteService. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 * @author Equipo 2
 */
public interface IReporteService {

    // Genera y guarda el reporte de ventas de un periodo.
    public ReporteVenta generarReporteVentas(LocalDateTime inicio, LocalDateTime fin);

    // Obtiene reportes de ventas guardados.
    public List<ReporteVenta> obtenerReportesVentas();

    // Genera el reporte de compras de insumos de un periodo con su total.
    public ReporteCompras generarReporteCompras(LocalDateTime inicio, LocalDateTime fin);

    // Genera el reporte de rendimiento en base a pedidos pagados, cancelados y total vendido por empleado en un periodo.
    public List<RendimientoEmpleado> generarRendimientoEmpleados(LocalDateTime inicio, LocalDateTime fin);
}

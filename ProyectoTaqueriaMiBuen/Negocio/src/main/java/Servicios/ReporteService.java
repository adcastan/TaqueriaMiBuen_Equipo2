package Servicios;

import DAO.CompraDAO;
import DTO.ReporteCompras;
import Exceptions.NegocioException;
import IAccesoDatos.ICompraDAO;
import IServicios.IReporteService;
import java.time.LocalDateTime;

/**
 * Servicio de Reportes (CU-09 y CU-10).
 *
 * Por ahora solo queda el reporte de compras de un periodo. El reporte de
 * ventas se va a calcular en vivo (sin tablas de reportes guardados) con
 * filtros, comparativas y grafica; lo programa el siguiente equipo.
 *
 * El reporte de rendimiento de empleados se quito porque el profesor indico
 * que no va (ya no existe el CU-11).
 *
 * @author Equipo 2
 */
public class ReporteService implements IReporteService {

    private final ICompraDAO compraDAO = new CompraDAO();

    // Junta las compras de un periodo con su total
    @Override
    public ReporteCompras generarReporteCompras(LocalDateTime inicio, LocalDateTime fin) {
        validarPeriodo(inicio, fin);
        return new ReporteCompras(inicio, fin, compraDAO.listarPorPeriodo(inicio, fin));
    }

    // Las dos fechas son obligatorias y la final no puede ser antes que la inicial
    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            throw new NegocioException("Selecciona la fecha de inicio y la fecha fin");
        }
        if (fin.isBefore(inicio)) {
            throw new NegocioException("La fecha fin no puede ser anterior a la fecha inicio");
        }
    }
}

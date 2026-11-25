package Servicios;

import DAO.CompraDAO;
import DAO.PedidoDAO;
import DTO.ReporteCompras;
import DTO.ReporteVentas;
import Exceptions.NegocioException;
import IAccesoDatos.ICompraDAO;
import IAccesoDatos.IPedidoDAO;
import IServicios.IReporteService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Servicio de Reportes.
 *
 * El reporte de ventas se calcula en tiempo real (sin tablas de reportes guardados)
 * con los pedidos del periodo que ya regresa PedidoDAO.listarPorPeriodo.
 * @author Equipo 2
 */
public class ReporteService implements IReporteService {

    private final ICompraDAO compraDAO = new CompraDAO();
    private final IPedidoDAO pedidoDAO = new PedidoDAO();

    // Junta las compras de un periodo con su total
    @Override
    public ReporteCompras generarReporteCompras(LocalDateTime inicio, LocalDateTime fin) {
        validarPeriodo(inicio, fin);
        return new ReporteCompras(inicio, fin, compraDAO.listarPorPeriodo(inicio, fin));
    }

    // Junta los pedidos de un periodo; el dia final se toma completo (hasta las 23:59:59)
    @Override
    public ReporteVentas generarReporteVentas(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        return new ReporteVentas(inicio, fin, pedidoDAO.listarPorPeriodo(inicio.atStartOfDay(), fin.atTime(LocalTime.MAX)));
    }

    // El periodo anterior dura lo mismo y termina un dia antes del inicio.
    @Override
    public ReporteVentas generarReporteAnterior(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
        LocalDate finAnterior = inicio.minusDays(1);
        LocalDate inicioAnterior = finAnterior.minusDays(dias - 1);
        return generarReporteVentas(inicioAnterior, finAnterior);
    }

    // Fechas, la inicial no mayor que la final y ninguna despues de hoy.
    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new NegocioException("Selecciona la fecha inicial y la fecha final");
        }
        if (inicio.isAfter(fin) || fin.isAfter(LocalDate.now())) {
            throw new NegocioException("El rango de fechas no es valido");
        }
    }

    // Las dos fechas son obligatorias y la final no puede ser antes que la inicial.
    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            throw new NegocioException("Selecciona la fecha de inicio y la fecha fin");
        }
        if (fin.isBefore(inicio)) {
            throw new NegocioException("La fecha fin no puede ser anterior a la fecha inicio");
        }
    }
}
package Servicios;

import DAO.CompraDAO;
import DAO.PedidoDAO;
import DAO.ReporteVentaDAO;
import DTO.RendimientoEmpleado;
import DTO.ReporteCompras;
import Entidades.Pedido;
import Entidades.ReporteVenta;
import Entidades.Usuario;
import Enums.EstadoPedido;
import Exceptions.NegocioException;
import IAccesoDatos.ICompraDAO;
import IAccesoDatos.IPedidoDAO;
import IAccesoDatos.IReporteVentaDAO;
import IServicios.IReporteService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de Reportes.
 * @author Equipo 2
 */
public class ReporteService implements IReporteService {

    private final IPedidoDAO pedidoDAO = new PedidoDAO();
    private final IReporteVentaDAO reporteVentaDAO = new ReporteVentaDAO();
    private final ICompraDAO compraDAO = new CompraDAO();
    
    // Calcula y guarda el reporte de ventas de un periodo
    @Override
    public ReporteVenta generarReporteVentas(LocalDateTime inicio, LocalDateTime fin) {
        validarPeriodo(inicio, fin);
        ReporteVenta reporte = new ReporteVenta(inicio, fin);
        reporte.generar(pedidoDAO.listarPorPeriodo(inicio, fin));
        reporteVentaDAO.registrar(reporte);
        return reporte;
    }

    // Los reportes de ventas que ya se han generado
    @Override
    public List<ReporteVenta> obtenerReportesVentas() {
        return reporteVentaDAO.listar();
    }

    // Junta las compras de un periodo con su total
    @Override
    public ReporteCompras generarReporteCompras(LocalDateTime inicio, LocalDateTime fin) {
        validarPeriodo(inicio, fin);
        return new ReporteCompras(inicio, fin, compraDAO.listarPorPeriodo(inicio, fin));
    }
    
    // Cuanto vendio y cuanto le cancelaron a cada mesero en un periodo
    @Override
    public List<RendimientoEmpleado> generarRendimientoEmpleados(LocalDateTime inicio, LocalDateTime fin) {
        validarPeriodo(inicio, fin);
        Map<Integer, RendimientoEmpleado> porEmpleado = new LinkedHashMap<>();
        for (Pedido pedido : pedidoDAO.listarPorPeriodo(inicio, fin)) {
            Usuario usuario = pedido.getUsuario();
            RendimientoEmpleado renglon = porEmpleado.get(usuario.getIdUsuario());
            if (renglon == null) {
                renglon = new RendimientoEmpleado(usuario.getIdUsuario(), usuario.getNombre());
                porEmpleado.put(usuario.getIdUsuario(), renglon);
            }
            if (pedido.getEstado() == EstadoPedido.PAGADO) {
                renglon.sumarPagado(pedido.getTotal() != null ? pedido.getTotal() : 0);
            } else if (pedido.getEstado() == EstadoPedido.CANCELADO) {
                renglon.sumarCancelado();
            }
        }
        List<RendimientoEmpleado> lista = new ArrayList<>(porEmpleado.values());
        lista.sort((a, b) -> Double.compare(b.getTotalVendido(), a.getTotalVendido()));
        return lista;
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

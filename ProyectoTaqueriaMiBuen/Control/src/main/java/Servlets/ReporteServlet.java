/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servlets;

import DTO.ReporteVentas;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import Enums.CanalPedido;
import Exceptions.NegocioException;
import IServicios.IReporteService;
import Servicios.ReporteService;
import Utilerias.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atiende la pantalla del reporte de ventas. 
 * Solo entra el administrador (lo revisa FiltroSeguridad).
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/api/reportes/ventas", "/api/reportes/ventas/*"})
public class ReporteServlet extends HttpServlet {

    // Cuantos productos se muestran en "Productos mas vendidos".
    private static final int PRODUCTOS_EN_REPORTE = 6;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IReporteService reporteService = new ReporteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo();
        try {
            LocalDate inicio = fecha(req.getParameter("inicio"));
            LocalDate fin = fecha(req.getParameter("fin"));
            if (ruta == null || "/".equals(ruta)) {
                String agrupar = req.getParameter("agrupar");
                boolean comparar = "true".equals(req.getParameter("comparar"));
                Json.enviar(resp, 200, armarReporte(inicio, fin, agrupar, comparar));
            } else if ("/pedidos".equals(ruta)) {
                Json.enviar(resp, 200, armarPedidos(reporteService.generarReporteVentas(inicio, fin).getPedidos()));
            } else {
                Json.error(resp, 404, "Ruta no encontrada");
            }
        } catch (DateTimeParseException e) {
            Json.error(resp, 400, "El rango de fechas no es valido");
        } catch (NegocioException e) {
            Json.error(resp, 400, e.getMessage());
        } catch (RuntimeException e) {
            //  La pantalla muestra el mensaje con el boton Reintentar.
            Json.error(resp, 500, "No se pudo generar el reporte. Intenta de nuevo.");
        }
    }

    // Lo que necesita la pantalla: resumen, barras, productos y, si se pidio, el periodo anterior.
    private Map<String, Object> armarReporte(LocalDate inicio, LocalDate fin, String agrupar, boolean comparar) {
        ReporteVentas reporte = reporteService.generarReporteVentas(inicio, fin);
        List<Map<String, Object>> barras = reporte.ventasPor(agrupar);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fechaInicio", reporte.getFechaInicio().toString());
        m.put("fechaFin", reporte.getFechaFin().toString());
        m.put("agrupar", agrupar);
        m.put("totalVendido", reporte.getTotalVendido());
        m.put("pedidosCobrados", reporte.getPedidosCobrados());
        m.put("pedidosSinCobrar", reporte.getPedidosSinCobrar());
        m.put("productos", reporte.productosMasVendidos(PRODUCTOS_EN_REPORTE));

        if (comparar) {
            ReporteVentas anterior = reporteService.generarReporteAnterior(inicio, fin);
            List<Map<String, Object>> barrasAnteriores = anterior.ventasPor(agrupar);
            // Cada barra se compara con la que ocupa el mismo lugar en el periodo anterior.
            for (int i = 0; i < barras.size(); i++) {
                Object totalAnterior = i < barrasAnteriores.size() ? barrasAnteriores.get(i).get("total") : BigDecimal.ZERO;
                barras.get(i).put("totalAnterior", totalAnterior);
            }
            m.put("inicioAnterior", anterior.getFechaInicio().toString());
            m.put("finAnterior", anterior.getFechaFin().toString());
            m.put("totalAnterior", anterior.getTotalVendido());
            m.put("pedidosAnterior", anterior.getPedidosCobrados());
        }
        m.put("periodos", barras);
        return m;
    }

    // Lista de pedidos de una barra.
    private List<Map<String, Object>> armarPedidos(List<Pedido> pedidos) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idPedido", pedido.getIdPedido());
            m.put("folio", pedido.getFolio());
            m.put("fecha", pedido.getFechaApertura().format(FECHA_HORA));
            m.put("tipo", tipo(pedido));
            m.put("total", pedido.calcularTotal());
            lista.add(m);
        }
        return lista;
    }

    // "Mesa 4", "Para llevar" o "A domicilio", con la app si vino de DiDi o Uber.
    private String tipo(Pedido pedido) {
        String tipo;
        if (pedido instanceof PedidoLocal) {
            tipo = "Mesa " + ((PedidoLocal) pedido).getMesa().getNumero();
        } else if (pedido instanceof PedidoDomicilio) {
            tipo = "A domicilio";
        } else {
            tipo = "Para llevar";
        }
        if (pedido.getCanal() == CanalPedido.DIDI_FOOD) {
            tipo += " (DiDi Food)";
        } else if (pedido.getCanal() == CanalPedido.UBER_EATS) {
            tipo += " (Uber Eats)";
        }
        return tipo;
    }

    // fecha
    private LocalDate fecha(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
    }
}

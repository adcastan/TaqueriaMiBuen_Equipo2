/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DTO;

import DTO.ReporteVentas;
import Entidades.DetallePedido;
import Entidades.Pedido;
import Enums.EstadoPedido;
import Exceptions.NegocioException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporte de ventas de un periodo.
 * Recibe los pedidos del periodo y en el constructor calcula el total vendido y
 * cuantos pedidos se cobraron.
 *
 * Las barras de la grafica y los productos mas vendidos se sacan con los
 * metodos de esta clase, usando lo que ya calcula las entidades
 * (Pedido.calcularTotal y DetallePedido.calcularImporte).
 *
 * Solo cuentan los pedidos PAGADO. Los ABIERTO o POR_COBRAR se cuentan aparte
 * para avisar que faltan por cobrar, y los CANCELADO no se toman en cuenta.
 *
 * @author Equipo 2
 */
public class ReporteVentas {

    private static final String[] DIAS = {"Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do"};
    private static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int pedidosCobrados;
    private BigDecimal totalVendido = new BigDecimal("0.00");
    // Pedidos del periodo que siguen abiertos o por cobrar (flujo 2.2.6); no entran al total
    private int pedidosSinCobrar;
    // Solo los pedidos PAGADO del periodo
    private List<Pedido> pedidos = new ArrayList<>();

    public ReporteVentas(LocalDate fechaInicio, LocalDate fechaFin, List<Pedido> pedidosDelPeriodo) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        for (Pedido pedido : pedidosDelPeriodo) {
            if (pedido.getEstado() == EstadoPedido.PAGADO) {
                pedidos.add(pedido);
                totalVendido = totalVendido.add(pedido.calcularTotal());
            } else if (pedido.getEstado() == EstadoPedido.ABIERTO || pedido.getEstado() == EstadoPedido.POR_COBRAR) {
                pedidosSinCobrar++;
            }
        }
        pedidosCobrados = pedidos.size();
    }

    /**
     * Una barra por cada dia, semana o mes del periodo, aunque no se haya
     * vendido nada (esa sale en $0.00 para que la grafica no se salte dias).
     * Cada barra trae: etiqueta, inicio, fin, pedidos y total.
     *
     * @param agrupar DIA, SEMANA o MES
     * @return 
     */
    public List<Map<String, Object>> ventasPor(String agrupar) {
        if (agrupar == null || !List.of("DIA", "SEMANA", "MES").contains(agrupar)) {
            throw new NegocioException("Elige si las ventas se agrupan por dia, semana o mes");
        }
        List<Map<String, Object>> barras = new ArrayList<>();
        LocalDate inicioBarra = inicioDePeriodo(fechaInicio, agrupar);
        while (!inicioBarra.isAfter(fechaFin)) {
            LocalDate siguiente = siguientePeriodo(inicioBarra, agrupar);
            // La primera y la ultima barra se recortan al rango que se eligio
            LocalDate desde = inicioBarra.isBefore(fechaInicio) ? fechaInicio : inicioBarra;
            LocalDate hasta = siguiente.minusDays(1).isAfter(fechaFin) ? fechaFin : siguiente.minusDays(1);

            List<Pedido> pedidosBarra = pedidosEntre(desde, hasta);
            BigDecimal total = new BigDecimal("0.00");
            for (Pedido pedido : pedidosBarra) {
                total = total.add(pedido.calcularTotal());
            }
            Map<String, Object> barra = new LinkedHashMap<>();
            barra.put("etiqueta", etiqueta(desde, hasta, agrupar));
            barra.put("inicio", desde.toString());
            barra.put("fin", hasta.toString());
            barra.put("pedidos", pedidosBarra.size());
            barra.put("total", total);
            barras.add(barra);
            inicioBarra = siguiente;
        }
        return barras;
    }

    /**
     * Productos mas vendidos por importe. 
     * No cuenta los productos cancelados ni las cortesias. 
     * Cada renglon trae: nombre, unidad, cantidad e importe.
     * @param limite
     * @return 
     */
    public List<Map<String, Object>> productosMasVendidos(int limite) {
        // Se juntan por producto: el mismo producto en varios pedidos se suma
        Map<Integer, Map<String, Object>> porProducto = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            for (DetallePedido detalle : pedido.detallesActivos()) {
                if (detalle.isCortesia()) {
                    continue;
                }
                Map<String, Object> renglon = porProducto.get(detalle.getProducto().getIdProducto());
                if (renglon == null) {
                    renglon = new LinkedHashMap<>();
                    renglon.put("nombre", detalle.getProducto().getNombre());
                    renglon.put("unidad", detalle.getProducto().getUnidadVenta().name());
                    renglon.put("cantidad", BigDecimal.ZERO);
                    renglon.put("importe", BigDecimal.ZERO);
                    porProducto.put(detalle.getProducto().getIdProducto(), renglon);
                }
                renglon.put("cantidad", ((BigDecimal) renglon.get("cantidad")).add(detalle.getCantidad()));
                renglon.put("importe", ((BigDecimal) renglon.get("importe")).add(detalle.calcularImporte()));
            }
        }
        // De mayor a menor importe y solo los primeros
        List<Map<String, Object>> lista = new ArrayList<>(porProducto.values());
        lista.sort((a, b) -> ((BigDecimal) b.get("importe")).compareTo((BigDecimal) a.get("importe")));
        return lista.size() > limite ? lista.subList(0, limite) : lista;
    }

    // Pedidos cobrados que se abrieron entre dos dias (incluye los dos)
    public List<Pedido> pedidosEntre(LocalDate desde, LocalDate hasta) {
        List<Pedido> lista = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            LocalDate dia = pedido.getFechaApertura().toLocalDate();
            if (!dia.isBefore(desde) && !dia.isAfter(hasta)) {
                lista.add(pedido);
            }
        }
        return lista;
    }

    // Lunes de la semana o dia 1 del mes; por dia es la misma fecha.
    private LocalDate inicioDePeriodo(LocalDate fecha, String agrupar) {
        switch (agrupar) {
            case "SEMANA":
                return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "MES":
                return fecha.withDayOfMonth(1);
            default:
                return fecha;
        }
    }

    private LocalDate siguientePeriodo(LocalDate fecha, String agrupar) {
        switch (agrupar) {
            case "SEMANA":
                return fecha.plusWeeks(1);
            case "MES":
                return fecha.plusMonths(1);
            default:
                return fecha.plusDays(1);
        }
    }

    private String etiqueta(LocalDate desde, LocalDate hasta, String agrupar) {
        switch (agrupar) {
            case "SEMANA":
                return desde.format(DIA_MES) + " - " + hasta.format(DIA_MES);
            case "MES":
                return MESES[desde.getMonthValue() - 1] + " " + desde.getYear();
            default:
                return DIAS[desde.getDayOfWeek().getValue() - 1] + " " + desde.getDayOfMonth();
        }
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public int getPedidosCobrados() {
        return pedidosCobrados;
    }

    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    public int getPedidosSinCobrar() {
        return pedidosSinCobrar;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }
}

package Utilerias;

import Entidades.Caja;
import Entidades.Categoria;
import Entidades.DatosEntrega;
import Entidades.DetalleExtra;
import Entidades.DetallePedido;
import Entidades.IngredienteExtra;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import Entidades.PedidoParaLlevar;
import Entidades.Producto;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pasa las entidades a mapas sencillos para mandarlas como JSON.
 * No mandamos las entidades directo porque tienen relaciones de ida y
 * vuelta (pedido - detalle - pedido) y Gson se ciclaria.
 *
 * @author Equipo 2
 */
public class Convertidor {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

     /**
     * Caja abierta para la pantalla de cobro, con lo cobrado en efectivo y con tarjeta.
     */
    public static Map<String, Object> caja(Caja caja, double cobradoEfectivo, double cobradoTarjeta) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCaja", caja.getIdCaja());
        m.put("abiertaPor", caja.getUsuario().getNombre());
        m.put("hora", caja.getFechaApertura() == null ? "" : caja.getFechaApertura().format(HORA));
        m.put("montoInicial", caja.getMontoInicial());
        m.put("cobradoEfectivo", cobradoEfectivo);
        m.put("cobradoTarjeta", cobradoTarjeta);
        return m;
    }
    
    // Mesa para el mapa: numero, capacidad, color y, si tiene, su pedido y la cuenta
    public static Map<String, Object> mesa(Mesa mesa, Pedido pedidoActivo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idMesa", mesa.getIdMesa());
        m.put("numero", mesa.getNumero());
        m.put("capacidad", mesa.getCapacidad());
        m.put("estado", mesa.getEstado().name());
        // Si la mesa tiene pedido se manda un resumen para el mapa de mesas
        if (pedidoActivo != null) {
            m.put("idPedido", pedidoActivo.getIdPedido());
            m.put("estadoPedido", pedidoActivo.getEstado().name());
            m.put("total", pedidoActivo.calcularTotal());
        }
        return m;
    }

    // Categoria con todos sus productos
    public static Map<String, Object> categoria(Categoria categoria, List<Producto> productos) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCategoria", categoria.getIdCategoria());
        m.put("nombre", categoria.getNombre());
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Producto p : productos) {
            lista.add(producto(p));
        }
        m.put("productos", lista);
        return m;
    }

    // Producto con precio, si esta disponible y sus extras
    public static Map<String, Object> producto(Producto producto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idProducto", producto.getIdProducto());
        m.put("nombre", producto.getNombre());
        m.put("descripcion", producto.getDescripcion());
        m.put("precio", producto.getPrecio());
        m.put("disponible", producto.isDisponible());
        List<Map<String, Object>> extras = new ArrayList<>();
        for (IngredienteExtra extra : producto.getExtras()) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("idExtra", extra.getIdExtra());
            e.put("nombre", extra.getNombre());
            e.put("precio", extra.getPrecio());
            extras.add(e);
        }
        m.put("extras", extras);
        return m;
    }

    // Pedido completo para la pantalla: datos segun el tipo, renglones, totales y quien lo movio
    public static Map<String, Object> pedido(Pedido pedido) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idPedido", pedido.getIdPedido());
        m.put("folio", pedido.getFolio());
        m.put("estado", pedido.getEstado().name());
        m.put("tipo", tipo(pedido));
        m.put("destino", destino(pedido));
        m.put("hora", pedido.getFechaApertura() == null ? "" : pedido.getFechaApertura().format(HORA));
        if (pedido instanceof PedidoLocal) {
            PedidoLocal local = (PedidoLocal) pedido;
            m.put("idMesa", local.getMesa().getIdMesa());
            m.put("numeroMesa", local.getMesa().getNumero());
            m.put("numeroPersonas", local.getNumeroPersonas());
        }
        if (pedido instanceof PedidoParaLlevar) {
            m.put("nombreCliente", ((PedidoParaLlevar) pedido).getNombreCliente());
        }
        if (pedido instanceof PedidoDomicilio) {
            PedidoDomicilio domicilio = (PedidoDomicilio) pedido;
            m.put("costoEnvio", domicilio.getCostoEnvio());
            DatosEntrega datos = domicilio.getDatosEntrega();
            if (datos != null) {
                m.put("nombreCliente", datos.getNombreCliente());
                m.put("telefono", datos.getTelefono());
                m.put("direccion", datos.getDireccion());
                m.put("referencias", datos.getReferencias());
            }
        }
        List<Map<String, Object>> detalles = new ArrayList<>();
        for (DetallePedido d : pedido.getDetalles()) {
            detalles.add(detalle(d));
        }
        m.put("detalles", detalles);
        m.put("subtotal", pedido.calcularSubtotal());
        m.put("total", pedido.calcularTotal());
        m.put("pendientesCocina", pedido.productosPendientesEnCocina());
        m.put("rondas", pedido.siguienteRonda() - 1);
        // Trazabilidad (requisito 3.6): quien lo abrio y quien le movio al ultimo
        m.put("abiertoPor", pedido.getUsuario() == null ? null : pedido.getUsuario().getNombre());
        m.put("modificadoPor", pedido.getUsuarioModifico() == null ? null : pedido.getUsuarioModifico().getNombre());
        m.put("horaModificacion", pedido.getFechaModificacion() == null ? "" : pedido.getFechaModificacion().format(HORA));
        return m;
    }

    // Un renglon del pedido con su estado en cocina, ronda e importe
    public static Map<String, Object> detalle(DetallePedido detalle) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idDetalle", detalle.getIdDetalle());
        m.put("turno", detalle.getOrdenColaPedido());
        m.put("producto", detalle.getProducto().getNombre());
        m.put("cantidad", detalle.getCantidad());
        m.put("precioUnitario", detalle.getPrecioUnitario());
        m.put("notaCocina", detalle.getNotaCocina());
        m.put("listo", detalle.isListo());
        m.put("estado", detalle.getEstadoPreparacion().name());
        m.put("ronda", detalle.getRonda());
        m.put("hora", detalle.getFechaEnvio() == null ? "" : detalle.getFechaEnvio().format(HORA));
        m.put("agregadoPor", detalle.getAgregadoPor() == null ? null : detalle.getAgregadoPor().getNombre());
        // Si se cancelo, que la pantalla pueda decir por que y quien lo autorizo
        m.put("motivoCancelacion", detalle.getMotivoCancelacion());
        m.put("canceladoPor", detalle.getCanceladoPor());
        List<String> extras = new ArrayList<>();
        for (DetalleExtra extra : detalle.getExtras()) {
            extras.add(extra.getExtra().getNombre());
        }
        m.put("extras", extras);
        m.put("importe", detalle.calcularImporte());
        return m;
    }

    /**
     * Detalle para el monitor de cocina, con el destino del pedido.
     */
    public static Map<String, Object> detalleCocina(DetallePedido detalle) {
        Map<String, Object> m = detalle(detalle);
        m.put("folio", detalle.getPedido().getFolio());
        m.put("tipo", tipo(detalle.getPedido()));
        m.put("destino", destino(detalle.getPedido()));
        return m;
    }

    // LOCAL, LLEVAR o DOMICILIO, igual que en PedidoDTO
    private static String tipo(Pedido pedido) {
        if (pedido instanceof PedidoLocal) {
            return "LOCAL";
        }
        if (pedido instanceof PedidoDomicilio) {
            return "DOMICILIO";
        }
        return "LLEVAR";
    }

    // Texto corto para identificar el pedido en pantalla y en cocina
    private static String destino(Pedido pedido) {
        if (pedido instanceof PedidoLocal) {
            return "Mesa " + ((PedidoLocal) pedido).getMesa().getNumero();
        }
        if (pedido instanceof PedidoParaLlevar) {
            return "Para llevar: " + ((PedidoParaLlevar) pedido).getNombreCliente();
        }
        PedidoDomicilio domicilio = (PedidoDomicilio) pedido;
        String cliente = domicilio.getDatosEntrega() == null ? "" : domicilio.getDatosEntrega().getNombreCliente();
        return "A domicilio: " + cliente;
    }
}

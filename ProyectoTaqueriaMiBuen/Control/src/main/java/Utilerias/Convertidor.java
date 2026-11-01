package Utilerias;

import Entidades.Caja;
import Entidades.Categoria;
import Entidades.DatosEntrega;
import Entidades.DetalleExtra;
import Entidades.DetallePedido;
import Entidades.IngredienteExtra;
import Entidades.Mesa;
import Entidades.PaqueteComponente;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import Entidades.PedidoParaLlevar;
import Entidades.Producto;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
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
    public static Map<String, Object> caja(Caja caja, BigDecimal cobradoEfectivo, BigDecimal cobradoTarjeta) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCaja", caja.getIdCaja());
        m.put("abiertaPor", caja.getUsuario().getNombre());
        m.put("hora", caja.getFechaApertura() == null ? "" : caja.getFechaApertura().format(HORA));
        m.put("montoInicial", caja.getMontoInicial());
        m.put("cobradoEfectivo", cobradoEfectivo);
        m.put("cobradoTarjeta", cobradoTarjeta);
        return m;
    }
    
    /**
     * Mesa para el mapa: numero, capacidad, color y, si tiene, sus cuentas.
     * Si la mesa esta junto a otra se manda tambien cual es su mesa principal,
     * para que el mapa diga "Junto a mesa N".
     */
    public static Map<String, Object> mesa(Mesa mesa, List<Pedido> cuentas) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idMesa", mesa.getIdMesa());
        m.put("numero", mesa.getNumero());
        m.put("capacidad", mesa.getCapacidad());
        m.put("estado", mesa.getEstado().name());
        if (mesa.estaUnida()) {
            m.put("idMesaPrincipal", mesa.getMesaPrincipal().getIdMesa());
            m.put("juntoAMesa", mesa.getMesaPrincipal().getNumero());
        }
        // Resumen de las cuentas vivas para el mapa (puede haber cuentas separadas)
        if (cuentas != null && !cuentas.isEmpty()) {
            Pedido ultima = cuentas.get(cuentas.size() - 1);
            BigDecimal total = BigDecimal.ZERO;
            for (Pedido p : cuentas) {
                total = total.add(p.calcularTotal());
            }
            m.put("idPedido", ultima.getIdPedido());
            m.put("estadoPedido", ultima.getEstado().name());
            m.put("cuentas", cuentas.size());
            m.put("total", total);
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
        // Precio para DiDi/Uber; si no tiene se usa el normal
        m.put("precioApps", producto.getPrecioApps() == null ? producto.getPrecio() : producto.getPrecioApps());
        // PIEZA o KILO: con KILO la pantalla deja capturar 0.25, 0.5, etc.
        m.put("unidadVenta", producto.getUnidadVenta().name());
        m.put("pasaPorParrilla", producto.isPasaPorParrilla());
        m.put("disponible", producto.sePuedePedir());
        m.put("piezas", piezasDePaquete(producto, false));
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
        m.put("canal", pedido.getCanal().name());
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
            m.put("tipoRepartidor", domicilio.getTipoRepartidor().name());
            m.put("repartidor", domicilio.getRepartidor());
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
        m.put("descuento", pedido.getDescuento());
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
        m.put("cortesia", detalle.isCortesia());
        m.put("unidadVenta", detalle.getProducto().getUnidadVenta().name());
        return m;
    }

    /**
     * Detalle para el monitor de cocina, con el destino del pedido.
     */
    public static Map<String, Object> detalleCocina(DetallePedido detalle) {
        Map<String, Object> m = detalle(detalle);
        // Con esto el monitor junta los productos de la misma comanda (pedido + ronda)
        m.put("idPedido", detalle.getPedido().getIdPedido());
        m.put("canal", detalle.getPedido().getCanal().name());
        // Minutos desde que se mando a cocina; el monitor cambia de color con esto
        m.put("minutosEspera", detalle.getFechaEnvio() == null ? 0
                : Duration.between(detalle.getFechaEnvio(), LocalDateTime.now()).toMinutes());
        m.put("folio", detalle.getPedido().getFolio());
        m.put("tipo", tipo(detalle.getPedido()));
        m.put("destino", destino(detalle.getPedido()));
        // Si es un paquete, la parrilla necesita ver que piezas le tocan
        // (ej. "3 x Taco de asada"); el refresco del paquete no le toca
        m.put("piezas", piezasDePaquete(detalle.getProducto(), true));
        return m;
    }

    /**
     * Piezas de un paquete como texto ("3 x Taco de asada"). Con soloParrilla
     * se quitan las que no prepara la parrilla (bebidas, guacamole).
     */
    private static List<String> piezasDePaquete(Producto producto, boolean soloParrilla) {
        List<String> piezas = new ArrayList<>();
        for (PaqueteComponente pieza : producto.getComponentes()) {
            if (soloParrilla && !pieza.getComponente().isPasaPorParrilla()) {
                continue;
            }
            piezas.add(pieza.getCantidad() + " x " + pieza.getComponente().getNombre());
        }
        return piezas;
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

package Utilerias;

import Entidades.Caja;
import Entidades.DetallePedido;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import Entidades.PedidoParaLlevar;
import Enums.EstadoPreparacion;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte a mapas (que luego Gson pasa a JSON) lo que necesita la pantalla
 * de cobro. Se hizo aparte de Convertidor para que la parte de cobro y la de
 * pedidos no se estorben cuando cada una cambia.
 *
 * @author Equipo 2
 */
public class ConvertidorCobro {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private ConvertidorCobro() {
        // Solo tiene metodos estaticos
    }

    /**
     * Caja abierta para la pantalla de cobro, con lo cobrado por cada metodo.
     */
    public static Map<String, Object> caja(Caja caja, BigDecimal cobradoEfectivo, BigDecimal cobradoTarjeta,
            BigDecimal cobradoTransferencia) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCaja", caja.getIdCaja());
        m.put("abiertaPor", caja.getUsuario().getNombre());
        m.put("hora", caja.getFechaApertura() == null ? "" : caja.getFechaApertura().format(HORA));
        m.put("montoInicial", caja.getMontoInicial());
        m.put("cobradoEfectivo", cobradoEfectivo);
        m.put("cobradoTarjeta", cobradoTarjeta);
        m.put("cobradoTransferencia", cobradoTransferencia);
        return m;
    }

    /**
     * Pedido por cobrar: a quien es (mesa o cliente), su total y sus renglones.
     * Lo cancelado no se manda porque no se cobra ni sale en el ticket.
     */
    public static Map<String, Object> pedido(Pedido pedido) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idPedido", pedido.getIdPedido());
        m.put("folio", pedido.getFolio());
        m.put("destino", destino(pedido));
        m.put("canal", pedido.getCanal() == null ? null : pedido.getCanal().name());
        // Si es de app la pantalla no pide metodo: se registra como PLATAFORMA
        m.put("esDeApp", pedido.getCanal() != null && pedido.getCanal().esDeApp());
        m.put("hora", pedido.getFechaApertura() == null ? "" : pedido.getFechaApertura().format(HORA));
        m.put("total", pedido.getTotal() != null ? pedido.getTotal() : pedido.calcularTotal());
        m.put("descuento", pedido.getDescuento());
        if (pedido instanceof PedidoDomicilio) {
            m.put("costoEnvio", ((PedidoDomicilio) pedido).getCostoEnvio());
        }
        List<Map<String, Object>> renglones = new ArrayList<>();
        for (DetallePedido d : pedido.getDetalles()) {
            if (d.getEstadoPreparacion() == EstadoPreparacion.CANCELADO) {
                continue;
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("idDetalle", d.getIdDetalle());
            // stripTrailingZeros para que salga "2" y no "2.000" en piezas
            r.put("cantidad", d.getCantidad().stripTrailingZeros().toPlainString());
            r.put("producto", d.getProducto().getNombre());
            r.put("importe", d.calcularImporte());
            r.put("cortesia", d.isCortesia());
            renglones.add(r);
        }
        m.put("detalles", renglones);
        return m;
    }

    // Texto corto para saber de quien es la cuenta: "Mesa 3", "Para llevar - Ana"...
    private static String destino(Pedido pedido) {
        if (pedido instanceof PedidoLocal) {
            return "Mesa " + ((PedidoLocal) pedido).getMesa().getNumero();
        }
        if (pedido instanceof PedidoParaLlevar) {
            return "Para llevar - " + ((PedidoParaLlevar) pedido).getNombreCliente();
        }
        if (pedido instanceof PedidoDomicilio) {
            PedidoDomicilio domicilio = (PedidoDomicilio) pedido;
            String cliente = domicilio.getDatosEntrega() == null ? "" : domicilio.getDatosEntrega().getNombreCliente();
            return "Domicilio - " + cliente;
        }
        return "Pedido " + pedido.getFolio();
    }
}

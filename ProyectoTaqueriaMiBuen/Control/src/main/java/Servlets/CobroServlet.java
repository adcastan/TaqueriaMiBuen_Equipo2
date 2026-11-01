package Servlets;

import DTO.ResultadoCobro;
import Entidades.Caja;
import Entidades.Pedido;
import Entidades.Usuario;
import Enums.MetodoPago;
import Exceptions.NegocioException;
import IServicios.ICajaService;
import IServicios.ICobroService;
import Servicios.CajaService;
import Servicios.CobroService;
import Utilerias.ConvertidorCobro;
import Utilerias.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atiende la pantalla de cobro (CU-05). Solo entran cajero y administrador
 * (lo revisa FiltroSeguridad).
 *
 * GET  /api/cobros            caja abierta y pedidos por cobrar
 * POST /api/cobros/caja       abre la caja con su fondo inicial
 * POST /api/cobros/{idPedido} cobra ese pedido completo
 *
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/api/cobros", "/api/cobros/*"})
public class CobroServlet extends HttpServlet {

    private final ICobroService cobroService = new CobroService();
    private final ICajaService cajaService = new CajaService();

    // Lo que manda la pantalla al abrir la caja
    private static class DatosCaja {

        BigDecimal montoInicial;
    }

    // Lo que manda la pantalla al cobrar: metodo y, si es efectivo, con cuanto pagaron
    private static class DatosCobro {

        String metodo;
        BigDecimal montoRecibido;
    }

    // GET /api/cobros: la caja abierta con lo cobrado y la lista de pedidos por cobrar
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> respuesta = new LinkedHashMap<>();
            Caja caja = cajaService.obtenerCajaAbierta();
            if (caja == null) {
                respuesta.put("caja", null);
            } else {
                int id = caja.getIdCaja();
                respuesta.put("caja", ConvertidorCobro.caja(caja,
                        cajaService.obtenerTotalCobrado(id, MetodoPago.EFECTIVO),
                        cajaService.obtenerTotalCobrado(id, MetodoPago.TARJETA),
                        cajaService.obtenerTotalCobrado(id, MetodoPago.TRANSFERENCIA)));
            }
            List<Map<String, Object>> pendientes = new ArrayList<>();
            for (Pedido pedido : cobroService.obtenerPedidosPorCobrar()) {
                pendientes.add(ConvertidorCobro.pedido(pedido));
            }
            respuesta.put("pendientes", pendientes);
            Json.enviar(resp, 200, respuesta);
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo cargar el cobro: " + e.getMessage());
        }
    }

    // POST /api/cobros/caja abre la caja; POST /api/cobros/{idPedido} cobra ese pedido
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario usuario = (Usuario) req.getSession().getAttribute("usuario");
        String ruta = req.getPathInfo();
        try {
            if ("/caja".equals(ruta)) {
                DatosCaja datos = Json.leer(req, DatosCaja.class);
                Caja caja = cajaService.abrirCaja(usuario, datos.montoInicial);
                BigDecimal cero = new BigDecimal("0.00");
                Json.enviar(resp, 201, ConvertidorCobro.caja(caja, cero, cero, cero));
                return;
            }
            String[] partes = ruta == null ? new String[0] : ruta.split("/"); // ["", "5"]
            if (partes.length != 2) {
                Json.error(resp, 404, "Ruta no encontrada");
                return;
            }
            int idPedido = Integer.parseInt(partes[1]);
            DatosCobro datos = Json.leer(req, DatosCobro.class);
            ResultadoCobro resultado = cobroService.cobrarPedido(idPedido, datos.metodo, datos.montoRecibido, usuario);
            Json.enviar(resp, 200, resultado);
        } catch (NumberFormatException e) {
            Json.error(resp, 400, "El id del pedido no es un numero");
        } catch (NegocioException | IllegalArgumentException e) {
            Json.error(resp, 400, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo completar el cobro: " + e.getMessage());
        }
    }
}

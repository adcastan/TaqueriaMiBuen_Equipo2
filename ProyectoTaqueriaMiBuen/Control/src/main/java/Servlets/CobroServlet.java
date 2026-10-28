package Servlets;

import DTO.ResultadoCobro;
import Entidades.Caja;
import Entidades.Pedido;
import Entidades.Usuario;
import Exceptions.NegocioException;
import IServicios.ICajaService;
import IServicios.ICobroService;
import Servicios.CajaService;
import Servicios.CobroService;
import Utilerias.Convertidor;
import Utilerias.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/api/cobros", "/api/cobros/*"})
public class CobroServlet extends HttpServlet {

    private final ICobroService cobroService = new CobroService();
    private final ICajaService cajaService = new CajaService();

    private static class DatosCaja {

        double montoInicial;
    }

    private static class DatosCobro {

        String metodo;
        double montoRecibido;
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
                respuesta.put("caja", Convertidor.caja(caja,
                        cajaService.obtenerTotalCobrado(caja.getIdCaja(), CobroService.METODO_EFECTIVO),
                        cajaService.obtenerTotalCobrado(caja.getIdCaja(), CobroService.METODO_TARJETA)));
            }
            List<Map<String, Object>> pendientes = new ArrayList<>();
            for (Pedido pedido : cobroService.obtenerPedidosPorCobrar()) {
                pendientes.add(Convertidor.pedido(pedido));
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
                Json.enviar(resp, 201, Convertidor.caja(caja, 0, 0));
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

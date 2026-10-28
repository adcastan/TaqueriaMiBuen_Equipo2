package Servlets;

import Exceptions.NegocioException;
import Entidades.DetallePedido;
import Servicios.KdsService;
import Utilerias.Convertidor;
import Utilerias.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Peticiones del monitor de cocina (kds.js):
 *   GET  /api/kds?terminal=1: cola de detalles pendientes (tambien registra el latido)
 *   POST /api/kds/detalles/{idDetalle}/listo: el cocinero marca un detalle como listo
 *
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/api/kds", "/api/kds/*"})
public class KdsServlet extends HttpServlet {

    private final KdsService kdsService = new KdsService();

    // GET /api/kds?terminal=1: la cola de cocina; de paso cuenta como latido de la pantalla
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String terminal = req.getParameter("terminal");
            int idTerminal = terminal == null ? 1 : Integer.parseInt(terminal);
            List<Map<String, Object>> cola = new ArrayList<>();
            for (DetallePedido detalle : kdsService.obtenerCola(idTerminal)) {
                cola.add(Convertidor.detalleCocina(detalle));
            }
            Json.enviar(resp, 200, cola);
        } catch (NumberFormatException e) {
            Json.error(resp, 400, "La terminal debe ser un numero");
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo cargar la cola: " + e.getMessage());
        }
    }

    // Cuerpo de POST /api/kds/detalles/{id}/estado
    private static class NuevoEstado {
        String estado;
    }

    /**
     * POST /api/kds/detalles/{id}/listo           lo marca listo (se queda por compatibilidad)
     * POST /api/kds/detalles/{id}/estado {estado} lo pasa a EN_PREPARACION o LISTO
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo();
        String[] partes = ruta == null ? new String[0] : ruta.split("/");
        if (partes.length != 4 || !partes[1].equals("detalles")
                || !(partes[3].equals("listo") || partes[3].equals("estado"))) {
            Json.error(resp, 404, "Ruta no encontrada");
            return;
        }
        try {
            int idDetalle = Integer.parseInt(partes[2]);
            if (partes[3].equals("listo")) {
                kdsService.marcarListo(idDetalle);
            } else {
                NuevoEstado datos = Json.leer(req, NuevoEstado.class);
                kdsService.cambiarEstado(idDetalle, datos.estado);
            }
            resp.setStatus(204);
        } catch (NumberFormatException e) {
            Json.error(resp, 400, "El id del detalle no es un numero");
        } catch (NegocioException | IllegalArgumentException e) {
            // Por ejemplo: el mesero lo quito justo antes de que el cocinero le picara
            Json.error(resp, 409, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo cambiar el estado: " + e.getMessage());
        }
    }
}

package Servlets;

import Exceptions.NegocioException;
import Entidades.DetallePedido;
import Servicios.MenuService;
import Entidades.Producto;
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
 *   GET  /api/kds/productos: productos de parrilla con su disponibilidad
 *   POST /api/kds/productos/{idProducto}/disponible {disponible}: se agoto o ya hay (RN-05)
 *
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/api/kds", "/api/kds/*"})
public class KdsServlet extends HttpServlet {

    private final KdsService kdsService = new KdsService();
    private final MenuService menuService = new MenuService();

    // Cuerpo de POST /api/kds/productos/{id}/disponible
    private static class Disponible {
        Boolean disponible;
    }

    // GET /api/kds?terminal=1: la cola de cocina; de paso cuenta como latido de la pantalla
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if ("/productos".equals(req.getPathInfo())) {
                // Lista corta para el boton "Se acabo": solo lo que prepara la parrilla
                List<Map<String, Object>> productos = new ArrayList<>();
                for (Producto p : menuService.obtenerProductosDeParrilla()) {
                    productos.add(Convertidor.producto(p));
                }
                Json.enviar(resp, 200, productos);
                return;
            }
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
        // "/productos/3/disponible": la parrilla avisa que se acabo (o ya hay) un producto
        if (partes.length == 4 && partes[1].equals("productos") && partes[3].equals("disponible")) {
            cambiarDisponible(req, resp, partes[2]);
            return;
        }
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

    /**
     * Marca un producto como agotado o disponible. Lo puede hacer la parrilla
     * (decision del cliente: son los primeros en saber que se acabo la carne)
     * o el administrador; el filtro de seguridad solo deja entrar a esos roles.
     */
    private void cambiarDisponible(HttpServletRequest req, HttpServletResponse resp, String idTexto) throws IOException {
        try {
            int idProducto = Integer.parseInt(idTexto);
            Disponible datos = Json.leer(req, Disponible.class);
            if (datos == null || datos.disponible == null) {
                throw new NegocioException("Indica si el producto esta disponible");
            }
            menuService.cambiarDisponibilidad(idProducto, datos.disponible);
            resp.setStatus(204);
        } catch (NumberFormatException e) {
            Json.error(resp, 400, "El id del producto no es un numero");
        } catch (NegocioException e) {
            Json.error(resp, 404, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo cambiar el producto: " + e.getMessage());
        }
    }
}

package Servlets;

import Entidades.Mesa;
import Exceptions.NegocioException;
import Entidades.Pedido;
import Enums.EstadoMesa;
import Servicios.PedidoService;
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
 * Peticiones del mapa de mesas (mesas.js):
 *   GET  /api/mesas: las mesas con su estado; a las ocupadas se les agrega
 *        el resumen de sus cuentas, y a las juntas su mesa principal.
 *   POST /api/mesas/{idMesa}/juntar {idPrincipal}: pega la mesa a la principal (RN-13)
 *   POST /api/mesas/{idMesa}/separar: la despega del grupo y la deja libre
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/api/mesas", "/api/mesas/*"})
public class MesaServlet extends HttpServlet {

    private final PedidoService pedidoService = new PedidoService();

    // GET /api/mesas: las 8 mesas con su color y, si tienen, la cuenta que llevan
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Map<String, Object>> mesas = new ArrayList<>();
            for (Mesa mesa : pedidoService.obtenerMesas()) {
                List<Pedido> cuentas = null;
                // Ocupada o pendiente de pago: se mandan sus cuentas para mostrar el total.
                // Si es una mesa unida, las cuentas salen de su mesa principal
                if (mesa.getEstado() != EstadoMesa.DISPONIBLE) {
                    cuentas = pedidoService.obtenerCuentasDeMesa(mesa.getIdMesa());
                }
                mesas.add(Convertidor.mesa(mesa, cuentas));
            }
            Json.enviar(resp, 200, mesas);
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudieron cargar las mesas: " + e.getMessage());
        }
    }

    // Cuerpo de POST /api/mesas/{id}/juntar
    private static class DatosJuntar {
        Integer idPrincipal;
    }

    // Juntar o separar mesas; contesta la lista de mesas ya actualizada
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo();
        // "/4/juntar" se parte en ["", "4", "juntar"]
        String[] partes = ruta == null ? new String[0] : ruta.split("/");
        if (partes.length != 3) {
            Json.error(resp, 404, "Ruta no encontrada");
            return;
        }
        try {
            int idMesa = Integer.parseInt(partes[1]);
            if (partes[2].equals("juntar")) {
                DatosJuntar datos = Json.leer(req, DatosJuntar.class);
                if (datos == null || datos.idPrincipal == null) {
                    throw new NegocioException("Indica con que mesa se junta");
                }
                pedidoService.juntarMesas(datos.idPrincipal, idMesa);
            } else if (partes[2].equals("separar")) {
                pedidoService.separarMesa(idMesa);
            } else {
                Json.error(resp, 404, "Ruta no encontrada");
                return;
            }
            // Se regresa el mapa completo para que la pantalla se pinte de una vez
            doGet(req, resp);
        } catch (NumberFormatException e) {
            Json.error(resp, 400, "El id de la mesa no es un numero");
        } catch (NegocioException e) {
            // 409: otra persona ya ocupo la mesa o ya estaba junta
            Json.error(resp, 409, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo cambiar la mesa: " + e.getMessage());
        }
    }
}

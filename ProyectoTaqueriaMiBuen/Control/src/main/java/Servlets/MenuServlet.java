package Servlets;

import Entidades.Categoria;
import Entidades.Producto;
import Servicios.MenuService;
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
 * GET /api/menu: regresa las categorias con sus productos y extras.
 * Lo usa pedido.js para dibujar el menu en la toma de pedido.
 *
 * @author Equipo 2
 */
@WebServlet("/api/menu")
public class MenuServlet extends HttpServlet {

    private final MenuService menuService = new MenuService();

    // GET /api/menu: categorias con sus productos y extras para la pantalla de pedido
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Map<String, Object>> categorias = new ArrayList<>();
            for (Map.Entry<Categoria, List<Producto>> entrada : menuService.obtenerMenu().entrySet()) {
                categorias.add(Convertidor.categoria(entrada.getKey(), entrada.getValue()));
            }
            Json.enviar(resp, 200, categorias);
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo cargar el menu: " + e.getMessage());
        }
    }
}


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servlets;

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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo(); // "/detalles/12/listo"
        String[] partes = ruta == null ? new String[0] : ruta.split("/");
        if (partes.length != 4 || !partes[1].equals("detalles") || !partes[3].equals("listo")) {
            Json.error(resp, 404, "Ruta no encontrada");
            return;
        }
        try {
            kdsService.marcarListo(Integer.parseInt(partes[2]));
            resp.setStatus(204); // 204 No Content, como en la secuencia KDS
        } catch (NumberFormatException e) {
            Json.error(resp, 400, "El id del detalle no es un numero");
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo marcar como listo: " + e.getMessage());
        }
    }
}

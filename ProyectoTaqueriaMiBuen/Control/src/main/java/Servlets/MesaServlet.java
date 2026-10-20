/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servlets;

import Entidades.Mesa;
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
 * GET /api/mesas: regresa las mesas con su estado para el mapa de mesas. 
 * A las mesas ocupadas se les agrega el total de su pedido activo.
 * @author Equipo 2
 */
@WebServlet("/api/mesas")
public class MesaServlet extends HttpServlet {

    private final PedidoService pedidoService = new PedidoService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Map<String, Object>> mesas = new ArrayList<>();
            for (Mesa mesa : pedidoService.obtenerMesas()) {
                Pedido activo = null;
                if (mesa.getEstado() == EstadoMesa.OCUPADA) {
                    activo = pedidoService.obtenerPedidoActivo(mesa.getIdMesa());
                }
                mesas.add(Convertidor.mesa(mesa, activo));
            }
            Json.enviar(resp, 200, mesas);
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudieron cargar las mesas: " + e.getMessage());
        }
    }
}

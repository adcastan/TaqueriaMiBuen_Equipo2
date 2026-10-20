/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servlets;

import DTO.DetalleDTO;
import DTO.PedidoDTO;
import DTO.ResultadoEnvio;
import Entidades.Pedido;
import Entidades.Usuario;
import Exceptions.MesaOcupadaException;
import Exceptions.NegocioException;
import Servicios.AutenticacionService;
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
 * Peticiones de pedidos que hace pedido.js:
 *   GET  /api/pedidos: pedidos activos (para llevar y domicilio en el mapa)
 *   GET  /api/pedidos?id={idPedido}: un pedido con sus detalles
 *   GET  /api/pedidos?idMesa={idMesa}: pedido activo de una mesa (null si esta libre)
 *   POST /api/pedidos: crea el pedido y lo manda a cocina (PedidoDTO)
 *   POST /api/pedidos/{idPedido}/detalles: agrega productos al pedido
 *   POST /api/pedidos/{idPedido}/cerrar: el cliente pide la cuenta
 *   POST /api/pedidos/{idPedido}/cancelar: cancela con motivo (y PIN si es mesero)
 *
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/api/pedidos", "/api/pedidos/*"})
public class PedidoServlet extends HttpServlet {

    private final PedidoService pedidoService = new PedidoService();
    private final AutenticacionService autenticacionService = new AutenticacionService();

    // Cuerpo de POST /api/pedidos/{id}/detalles
    private static class NuevosDetalles {
        List<DetalleDTO> detalles;
    }

    // Cuerpo de POST /api/pedidos/{id}/cancelar
    private static class DatosCancelacion {
        String motivo;
        String pin;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String id = req.getParameter("id");
            String idMesa = req.getParameter("idMesa");
            if (id != null) {
                Json.enviar(resp, 200, Convertidor.pedido(pedidoService.obtenerPedido(Integer.parseInt(id))));
            } else if (idMesa != null) {
                // Si la mesa esta libre se regresa null (no es un error)
                Pedido activo = pedidoService.obtenerPedidoActivo(Integer.parseInt(idMesa));
                Json.enviar(resp, 200, activo == null ? null : Convertidor.pedido(activo));
            } else {
                List<Map<String, Object>> lista = new ArrayList<>();
                for (Pedido pedido : pedidoService.obtenerPedidosActivos()) {
                    lista.add(Convertidor.pedido(pedido));
                }
                Json.enviar(resp, 200, lista);
            }
        } catch (NumberFormatException e) {
            Json.error(resp, 400, "El id no es un numero");
        } catch (NegocioException e) {
            Json.error(resp, 404, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo consultar el pedido: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario usuario = (Usuario) req.getSession().getAttribute("usuario");
        String ruta = req.getPathInfo(); 
        try {
            if (ruta == null || ruta.equals("/")) {
                // Convierte el JSON a PedidoDTO y lo manda al servicio.
                PedidoDTO dto = Json.leer(req, PedidoDTO.class);
                ResultadoEnvio resultado = pedidoService.crearPedido(dto, usuario);
                Json.enviar(resp, 201, resultado);
                return;
            }

            String[] partes = ruta.split("/"); 
            if (partes.length != 3) {
                Json.error(resp, 404, "Ruta no encontrada");
                return;
            }
            int idPedido = Integer.parseInt(partes[1]);
            switch (partes[2]) {
                case "detalles":
                    NuevosDetalles nuevos = Json.leer(req, NuevosDetalles.class);
                    Json.enviar(resp, 200, pedidoService.agregarDetalles(idPedido, nuevos.detalles));
                    break;
                case "cerrar":
                    Pedido cerrado = pedidoService.cerrarPedido(idPedido);
                    Json.enviar(resp, 200, Convertidor.pedido(pedidoService.obtenerPedido(cerrado.getIdPedido())));
                    break;
                case "cancelar":
                    DatosCancelacion datos = Json.leer(req, DatosCancelacion.class);
                    Usuario autoriza = quienAutoriza(req, usuario, datos.pin);
                    pedidoService.cancelarPedido(idPedido, datos.motivo, autoriza);
                    Json.enviar(resp, 200, Map.of("ok", true));
                    break;
                default:
                    Json.error(resp, 404, "Ruta no encontrada");
            }
        } catch (MesaOcupadaException e) {
            Json.error(resp, 409, e.getMessage());
        } catch (NegocioException | IllegalArgumentException e) {
            Json.error(resp, 400, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo completar la operacion: " + e.getMessage());
        }
    }

    // Si quien cancela es cajero o administrador se autoriza solo, si es mesero se pide PIN.
    private Usuario quienAutoriza(HttpServletRequest req, Usuario usuario, String pin) {
        String rol = (String) req.getSession().getAttribute("rol");
        if ("CAJERO".equals(rol) || "ADMINISTRADOR".equals(rol)) {
            return usuario;
        }
        return autenticacionService.validarAutorizacion(pin);
    }
}

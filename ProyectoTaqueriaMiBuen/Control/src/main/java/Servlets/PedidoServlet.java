package Servlets;

import DTO.DetalleDTO;
import DTO.PedidoDTO;
import DTO.ResultadoEnvio;
import Entidades.Pedido;
import Entidades.Usuario;
import Exceptions.MesaOcupadaException;
import Exceptions.NegocioException;
import Exceptions.ProductoNoDisponibleException;
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
 *   POST /api/pedidos/{idPedido}/detalles: agrega una ronda de productos
 *   POST /api/pedidos/{idPedido}/detalles/{idDetalle}/cantidad: cambia la cantidad (solo en cola)
 *   POST /api/pedidos/{idPedido}/detalles/{idDetalle}/quitar: quita un producto (solo en cola)
 *   POST /api/pedidos/{idPedido}/detalles/{idDetalle}/cancelar: cancela un producto ya en preparacion (PIN de cajero)
 *   POST /api/pedidos/{idPedido}/cerrar: el cliente pide la cuenta
 *   POST /api/pedidos/{idPedido}/cancelar: cancela con motivo (y PIN si es mesero)
 *
 * Codigos que contesta, para que la pantalla sepa que hacer:
 *   200/201 todo bien, 400 dato mal capturado, 404 no existe,
 *   409 choque con algo que paso al mismo tiempo (mesa ya ocupada o producto agotado),
 *   500 fallo del sistema.
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
        String claveEnvio;
    }

    // Cuerpo de POST .../cancelar (pedido o producto)
    private static class DatosCancelacion {
        String motivo;
        String pin;
    }

    // Cuerpo de POST .../cantidad
    private static class NuevaCantidad {
        Integer cantidad;
    }

    // Consultas de pedidos (ver las rutas GET de arriba)
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

    // Todas las acciones que cambian algo (ver las rutas POST de arriba)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Usuario usuario = (Usuario) req.getSession().getAttribute("usuario");
        String ruta = req.getPathInfo();
        try {
            if (ruta == null || ruta.equals("/")) {
                // Convierte el JSON a PedidoDTO y lo manda al servicio.
                PedidoDTO dto = Json.leer(req, PedidoDTO.class);
                ResultadoEnvio resultado = pedidoService.crearPedido(dto, usuario);
                // 201 = se creo algo nuevo; si era un reintento se contesta 200 con el mismo pedido
                Json.enviar(resp, resultado.isRepetido() ? 200 : 201, resultado);
                return;
            }
            // "/8/cerrar" se parte en ["", "8", "cerrar"]
            // "/8/detalles/15/quitar" se parte en ["", "8", "detalles", "15", "quitar"]
            String[] partes = ruta.split("/");
            int idPedido = Integer.parseInt(partes[1]);
            if (partes.length == 3) {
                accionDePedido(req, resp, usuario, idPedido, partes[2]);
            } else if (partes.length == 5 && partes[2].equals("detalles")) {
                accionDeProducto(req, resp, usuario, idPedido, Integer.parseInt(partes[3]), partes[4]);
            } else {
                Json.error(resp, 404, "Ruta no encontrada");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Json.error(resp, 400, "La ruta trae un id que no es numero");
        } catch (MesaOcupadaException e) {
            Json.error(resp, 409, e.getMessage());
        } catch (ProductoNoDisponibleException e) {
            Json.error(resp, 409, e.getMessage(), "productosNoDisponibles", e.getIdsProductos());
        } catch (NegocioException | IllegalArgumentException e) {
            Json.error(resp, 400, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "No se pudo completar la operacion: " + e.getMessage());
        }
    }

    // Acciones sobre el pedido completo: detalles (nueva ronda), cerrar o cancelar
    private void accionDePedido(HttpServletRequest req, HttpServletResponse resp, Usuario usuario,
            int idPedido, String accion) throws IOException {
        switch (accion) {
            case "detalles":
                NuevosDetalles nuevos = Json.leer(req, NuevosDetalles.class);
                Json.enviar(resp, 200, pedidoService.agregarDetalles(idPedido, nuevos.detalles, usuario, nuevos.claveEnvio));
                break;
            case "cerrar":
                Pedido cerrado = pedidoService.cerrarPedido(idPedido, usuario);
                Json.enviar(resp, 200, Convertidor.pedido(pedidoService.obtenerPedido(cerrado.getIdPedido())));
                break;
            case "cancelar":
                DatosCancelacion datos = Json.leer(req, DatosCancelacion.class);
                Usuario autoriza = quienAutoriza(req, usuario, datos.pin);
                pedidoService.cancelarPedido(idPedido, datos.motivo, autoriza, usuario);
                Json.enviar(resp, 200, Map.of("ok", true));
                break;
            default:
                Json.error(resp, 404, "Ruta no encontrada");
        }
    }

    // Acciones sobre un solo producto del pedido; todas regresan el pedido ya actualizado
    private void accionDeProducto(HttpServletRequest req, HttpServletResponse resp, Usuario usuario,
            int idPedido, int idDetalle, String accion) throws IOException {
        Pedido pedido;
        switch (accion) {
            case "cantidad":
                NuevaCantidad nueva = Json.leer(req, NuevaCantidad.class);
                if (nueva.cantidad == null) {
                    throw new NegocioException("Falta la cantidad");
                }
                pedido = pedidoService.cambiarCantidad(idPedido, idDetalle, nueva.cantidad, usuario);
                break;
            case "quitar":
                pedido = pedidoService.quitarDetalle(idPedido, idDetalle, usuario);
                break;
            case "cancelar":
                DatosCancelacion datos = Json.leer(req, DatosCancelacion.class);
                Usuario autoriza = quienAutoriza(req, usuario, datos.pin);
                pedido = pedidoService.cancelarDetalle(idPedido, idDetalle, datos.motivo, autoriza, usuario);
                break;
            default:
                Json.error(resp, 404, "Ruta no encontrada");
                return;
        }
        Json.enviar(resp, 200, Convertidor.pedido(pedidoService.obtenerPedido(pedido.getIdPedido())));
    }

    // Si quien cancela es cajero o administrador se autoriza solo, si es mesero se pide PIN.
    private Usuario quienAutoriza(HttpServletRequest req, Usuario usuario, String pin) {
        String rol = (String) req.getSession().getAttribute("rol");
        if ("CAJERO".equals(rol) || "ADMINISTRADOR".equals(rol)) {
            return usuario;
        }
        try {
            return autenticacionService.validarAutorizacion(pin);
        } catch (NegocioException e) {
            // Mismo mensaje que pide el caso de uso (flujo 2.2.6)
            throw new NegocioException("Autorizacion no valida");
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servlets;

import Entidades.Usuario;
import Enums.Rol;
import Exceptions.NegocioException;
import Servicios.AutenticacionService;
import Utilerias.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POST /login: inicia sesion.
 * POST /logout: cierra la sesion.
 * En la sesion se guarda el usuario y su rol para que FiltroSeguridad los revise.
 *
 * @author Equipo 2
 */
@WebServlet(urlPatterns = {"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private final AutenticacionService autenticacionService = new AutenticacionService();

    // Clase para leer el JSON que manda login.jsp
    private static class DatosLogin {
        String pin;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/logout".equals(req.getServletPath())) {
            HttpSession sesion = req.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            Json.enviar(resp, 200, Map.of("ok", true));
            return;
        }

        try {
            DatosLogin datos = Json.leer(req, DatosLogin.class);
            Usuario usuario = autenticacionService.iniciarSesion(datos.pin);
            Rol rol = autenticacionService.obtenerRol(usuario);

            HttpSession sesion = req.getSession(true);
            sesion.setAttribute("usuario", usuario);
            sesion.setAttribute("rol", rol.name());

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("rol", rol.name());
            // El cocinero entra directo al monitor de cocina, los demas al mapa de mesas
            respuesta.put("inicio", rol == Rol.COCINERO ? "kds.jsp" : "mesas.jsp");
            Json.enviar(resp, 200, respuesta);
        } catch (NegocioException | IllegalArgumentException e) {
            Json.error(resp, 401, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "Error al iniciar sesion: " + e.getMessage());
        }
    }
}

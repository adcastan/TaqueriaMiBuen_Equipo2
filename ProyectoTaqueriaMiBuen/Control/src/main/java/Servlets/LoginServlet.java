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
import java.util.Set;
import java.util.stream.Collectors;

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

    // POST /login entra con el PIN; POST /logout cierra la sesion
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
            Set<Rol> roles = autenticacionService.obtenerRoles(usuario);
            // Los roles viajan como texto separado por comas ("CAJERO,MESERO"),
            // asi los JSP y el JavaScript los leen sin complicarse
            String textoRoles = roles.stream().map(Rol::name).sorted().collect(Collectors.joining(","));

            HttpSession sesion = req.getSession(true);
            // Se cambia el id de la sesion al entrar; asi nadie que haya visto el id
            // viejo (por ejemplo en una compu compartida) se puede colar con nuestra sesion
            req.changeSessionId();
            sesion.setAttribute("usuario", usuario);
            sesion.setAttribute("roles", textoRoles);

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("roles", textoRoles);
            // Quien SOLO es cocinero entra directo al monitor; si tambien atiende mesas
            // (o es cajero/administrador) entra al mapa de mesas
            boolean soloCocina = roles.size() == 1 && roles.contains(Rol.COCINERO);
            respuesta.put("inicio", soloCocina ? "kds.jsp" : "mesas.jsp");
            Json.enviar(resp, 200, respuesta);
        } catch (NegocioException | IllegalArgumentException e) {
            Json.error(resp, 401, e.getMessage());
        } catch (RuntimeException e) {
            Json.error(resp, 500, "Error al iniciar sesion: " + e.getMessage());
        }
    }
}

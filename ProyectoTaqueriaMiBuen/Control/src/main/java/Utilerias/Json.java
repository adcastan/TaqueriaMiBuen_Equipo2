package Utilerias;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Metodos de ayuda para que los servlets lean y respondan JSON.
 * Se utiliza la libreria Gson de Google.
 *
 * @author Equipo 2
 */
public class Json {

    private static final Gson GSON = new Gson();

    /**
     * Convierte el cuerpo de la peticion (JSON) en un objeto de la clase indicada.
     */
    public static <T> T leer(HttpServletRequest req, Class<T> clase) throws IOException {
        try {
            // Sin esto Tomcat lee el cuerpo como ISO-8859-1 y "Jose Nunez" con acentos llega con letras raras
            if (req.getCharacterEncoding() == null) {
                req.setCharacterEncoding("UTF-8");
            }
            T objeto = GSON.fromJson(req.getReader(), clase);
            if (objeto == null) {
                throw new IllegalArgumentException("La peticion no trae datos");
            }
            return objeto;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("El JSON de la peticion no es valido");
        }
    }

    /**
     * Manda cualquier objeto (mapa, lista, DTO) como respuesta JSON.
     */
    public static void enviar(HttpServletResponse resp, int estado, Object datos) throws IOException {
        resp.setStatus(estado);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(datos));
    }

    /**
     * Respuesta de error con la forma {"error": "mensaje"} que lee JavaScript.
     */
    public static void error(HttpServletResponse resp, int estado, String mensaje) throws IOException {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("error", mensaje);
        enviar(resp, estado, cuerpo);
    }

    /**
     * Igual que error(), pero con un dato extra para la pantalla. Por ejemplo,
     * los ids de los productos que se agotaron para marcarlos en rojo.
     */
    public static void error(HttpServletResponse resp, int estado, String mensaje, String campo, Object valor)
            throws IOException {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("error", mensaje);
        cuerpo.put(campo, valor);
        enviar(resp, estado, cuerpo);
    }
}
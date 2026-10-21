/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Filtros;

import Utilerias.Json;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * Revisa cada peticion antes de que llegue a un servlet o a una pagina.
 * 1. Sin sesion solo se puede ver el login, los estilos y las imagenes.
 * 2. El monitor de cocina es para COCINERO y ADMINISTRADOR.
 * 3. Mesas y pedidos son para MESERO, CAJERO y ADMINISTRADOR.
 * 4. El cobro y la caja son para CAJERO y ADMINISTRADOR.
 * @author Equipo 2
 */
@WebFilter("/*")
public class FiltroSeguridad implements Filter {

    // Rutas que se pueden abrir sin haber iniciado sesion
    private static final List<String> LIBRES = List.of("/login", "/login.jsp", "/estilos.css");
    private static final List<String> ROLES_COCINA = List.of("COCINERO", "ADMINISTRADOR");
    private static final List<String> ROLES_PEDIDOS = List.of("MESERO", "CAJERO", "ADMINISTRADOR");
    private static final List<String> ROLES_CAJA = List.of("CAJERO", "ADMINISTRADOR");
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String ruta = req.getRequestURI().substring(req.getContextPath().length());

        if (ruta.isEmpty() || ruta.equals("/") || LIBRES.contains(ruta)
                || ruta.startsWith("/assets/") || ruta.startsWith("/js/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession sesion = req.getSession(false);
        String rol = sesion == null ? null : (String) sesion.getAttribute("rol");
        if (rol == null) {
            negar(req, resp, 401, "Inicia sesion para continuar");
            return;
        }

        boolean esCocina = ruta.startsWith("/api/kds") || ruta.equals("/kds.jsp");
        boolean esCaja = ruta.startsWith("/api/cobros") || ruta.equals("/cobro.jsp");
        List<String> permitidos = esCocina ? ROLES_COCINA : ROLES_PEDIDOS;
        if (!ruta.equals("/logout") && !permitidos.contains(rol)) {
            negar(req, resp, 403, "Tu rol no tiene permiso para esta pantalla");
            return;
        }
        chain.doFilter(request, response);
    }

    // A la API se le contesta JSON. 
    // Si una pagina pide sesion se manda al login,
    // y si le falta permiso se muestra el error 403 de Tomcat.
    private void negar(HttpServletRequest req, HttpServletResponse resp, int estado, String mensaje)
            throws IOException {
        String ruta = req.getRequestURI().substring(req.getContextPath().length());
        if (ruta.startsWith("/api/")) {
            Json.error(resp, estado, mensaje);
        } else if (estado == 401) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
        } else {
            resp.sendError(estado, mensaje);
        }
    }
}
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
Monitor de cocina.
Muestra los detalles pendientes en el orden de la cola (ordenColaPedido).
Solo pueden entrar COCINERO y ADMINISTRADOR (lo revisa FiltroSeguridad).
--%>
<% request.setAttribute("seccion", "cocina");%>
<!DOCTYPE html>
<html lang="es-MX">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Monitor de cocina - Mi Buen</title>
        <link rel="icon" href="assets/brand.svg" type="image/svg+xml">
        <link rel="stylesheet" href="estilos.css">
    </head>
    <body data-terminal="1">
        <%@include file="WEB-INF/partes/encabezado.jspf" %>

        <main class="workspace" id="main">
            <div class="page-title">
                <div class="title-copy">
                    <span class="title-icon"><img src="assets/asada.svg" alt="" width="30" height="30"></span>
                    <div>
                        <div class="eyebrow">Monitor de cocina</div>
                        <h1>Cola de preparacion</h1>
                        <p>Se prepara en orden de turno. Toca LISTO cuando el producto salga.</p>
                    </div>
                </div>
                <div class="title-actions">
                    <span class="pill amber" id="totalPendientes">0 pendientes</span>
                    <span class="pill green" id="ultimaActualizacion">Sin actualizar</span>
                </div>
            </div>

            <section class="cola-cocina" id="cola" aria-label="Detalles pendientes">
                <div class="empty">Cargando cola...</div>
            </section>
        </main>

        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script src="js/kds.js"></script>
    </body>
</html>

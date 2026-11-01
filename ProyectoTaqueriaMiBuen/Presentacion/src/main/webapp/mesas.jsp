<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
Mapa de mesas.
Las mesas las carga mesas.js desde GET /api/mesas.
Verde = libre, rojo = ocupada, ambar = ya pidieron la cuenta y falta cobrar.
Abajo se ven los pedidos para llevar y a domicilio activos.
--%>
<% request.setAttribute("seccion", "mesas");%>
<!DOCTYPE html>
<html lang="es-MX">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Mesas - Mi Buen</title>
        <link rel="icon" href="assets/logo-mi-buen.png" type="image/png">
        <link rel="stylesheet" href="estilos.css">
    </head>
    <body>
        <%@include file="WEB-INF/partes/encabezado.jspf" %>

        <main class="workspace" id="main">
            <div class="page-title">
                <div class="title-copy">
                    <span class="title-icon"><img src="assets/brand.svg" alt="" width="26" height="26"></span>
                    <div>
                        <div class="eyebrow">Comedor / mesas</div>
                        <h1>Elige una mesa</h1>
                        <p>Toca una mesa libre para abrir un pedido o una ocupada para ver su cuenta.</p>
                    </div>
                </div>
                <div class="title-actions">
                    <button class="btn secondary compact" onclick="nuevoPedido('LLEVAR')">Nuevo para llevar</button>
                    <button class="btn secondary compact" onclick="nuevoPedido('DOMICILIO')">Nuevo a domicilio</button>
                </div>
            </div>

            <%-- Leyenda en una sola linea: el color de la franja de cada mesa dice su estado --%>
            <div class="table-legend">
                <span class="legend">Libre</span>
                <span class="legend red">Ocupada</span>
                <span class="legend amber">Pidio la cuenta</span>
            </div>

            <section class="table-grid" id="mapaMesas" aria-label="Mapa de mesas">
                <div class="empty">Cargando mesas...</div>
            </section>

            <section class="card recent-panel otros-pedidos">
                <div class="section-heading">
                    <h2>Pedidos para llevar y a domicilio activos</h2>
                </div>
                <div class="recent-grid" id="otrosPedidos"></div>
            </section>
        </main>

        <%-- En celular las dos acciones mas usadas quedan abajo, al alcance del pulgar --%>
        <nav class="barra-inferior" aria-label="Pedidos sin mesa">
            <button class="btn secondary" onclick="nuevoPedido('LLEVAR')">Para llevar</button>
            <button class="btn secondary" onclick="nuevoPedido('DOMICILIO')">A domicilio</button>
        </nav>
        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script src="js/mesas.js"></script>
    </body>
</html>

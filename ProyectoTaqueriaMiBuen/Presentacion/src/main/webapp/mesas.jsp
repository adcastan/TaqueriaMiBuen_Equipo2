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
        <link rel="icon" href="assets/brand.svg" type="image/svg+xml">
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

            <div class="table-legend">
                <div class="legend">VERDE = LIBRE<small>TOCA PARA ABRIR PEDIDO</small></div>
                <div class="legend red">ROJO = OCUPADA<small>TOCA PARA VER LA CUENTA</small></div>
                <div class="legend amber">AMBAR = POR COBRAR<small>YA PIDIERON LA CUENTA</small></div>
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

        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script src="js/mesas.js"></script>
    </body>
</html>

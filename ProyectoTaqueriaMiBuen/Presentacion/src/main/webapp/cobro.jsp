<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
Cobro en caja (CU-05).
Muestra la caja abierta y los pedidos que ya pidieron la cuenta.
Se puede abrir con cobro.jsp?pedido=8 para empezar a cobrar ese pedido.
Solo pueden entrar CAJERO y ADMINISTRADOR (lo revisa FiltroSeguridad).
Toda la logica de la pantalla esta en cobro.js.
--%>
<% request.setAttribute("seccion", "cobro");%>
<!DOCTYPE html>
<html lang="es-MX">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Cobro - Mi Buen</title>
        <link rel="icon" href="assets/brand.svg" type="image/svg+xml">
        <link rel="stylesheet" href="estilos.css">
    </head>
    <body data-rol="${sessionScope.rol}">
        <%@include file="WEB-INF/partes/encabezado.jspf" %>

        <main class="workspace" id="main">
            <div class="page-title">
                <div class="title-copy">
                    <span class="title-icon"><img src="assets/brand.svg" alt="" width="30" height="30"></span>
                    <div>
                        <div class="eyebrow">Caja</div>
                        <h1>Cobro de pedidos</h1>
                        <p>Elige un pedido por cobrar, captura el pago y entrega el ticket.</p>
                    </div>
                </div>
                <div class="title-actions">
                    <span class="pill amber" id="totalPendientes">0 por cobrar</span>
                    <span class="pill" id="estadoCaja">Caja cerrada</span>
                </div>
            </div>

            <section class="card resumen-caja" id="resumenCaja" aria-label="Caja">
                <div class="empty">Cargando caja...</div>
            </section>

            <section class="cola-cobro" id="pendientes" aria-label="Pedidos por cobrar"></section>
        </main>

        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script src="js/cobro.js"></script>
    </body>
</html>

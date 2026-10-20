<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
Toma de pedido.
Se abre de varias formas:
      tomarPedido.jsp?mesa=3              pedido local (si la mesa esta ocupada carga su pedido)
      tomarPedido.jsp?mesa=3&separada=1   otra cuenta en una mesa ocupada
      tomarPedido.jsp?tipo=LLEVAR         pedido para llevar
      tomarPedido.jsp?tipo=DOMICILIO      pedido a domicilio (despues de datosEntrega.jsp)
      tomarPedido.jsp?pedido=8            abrir un pedido que ya existe
Toda la logica de la pantalla esta en pedido.js.
--%>
<%
    String tipoPagina = request.getParameter("tipo");
    if ("LLEVAR".equals(tipoPagina)) {
        request.setAttribute("seccion", "llevar");
    } else if ("DOMICILIO".equals(tipoPagina)) {
        request.setAttribute("seccion", "domicilio");
    } else {
        request.setAttribute("seccion", "mesas");
    }
%>
<!DOCTYPE html>
<html lang="es-MX">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Tomar pedido - Mi Buen</title>
        <link rel="icon" href="assets/brand.svg" type="image/svg+xml">
        <link rel="stylesheet" href="estilos.css">
    </head>
    <body data-rol="${sessionScope.rol}">
        <%@include file="WEB-INF/partes/encabezado.jspf" %>

        <main class="workspace" id="main">
            <div class="page-title">
                <div class="title-copy">
                    <span class="title-icon"><img src="assets/taco.svg" alt="" width="30" height="30"></span>
                    <div>
                        <div class="eyebrow" id="subtitulo">Toma de pedido</div>
                        <h1 id="titulo">Cargando...</h1>
                        <p>Agrega los productos, revisa el ticket y envia a cocina.</p>
                    </div>
                </div>
                <div class="title-actions">
                    <a class="btn secondary compact" href="mesas.jsp">Volver a mesas</a>
                </div>
            </div>

            <div class="order-steps">
                <span><b>1</b> Agrega productos</span>
                <span><b>2</b> Revisa el pedido</span>
                <span><b>3</b> Envia a cocina</span>
            </div>

            <div class="order-layout">
                <section aria-label="Menu">
                    <div class="order-heading">
                        <strong id="destino">ORDENANDO</strong>
                        <small id="resumenPendientes">0 por enviar</small>
                    </div>

                    <!-- Solo se muestran segun el tipo de pedido -->
                    <div class="guest-count" id="bloquePersonas" hidden>
                        <label for="numeroPersonas">Comensales</label>
                        <input id="numeroPersonas" type="number" min="1" max="20" value="2">
                    </div>
                    <div class="card customer-form" id="bloqueCliente" hidden>
                        <div class="field">
                            <label for="nombreCliente">Nombre del cliente (para llamarlo cuando este listo)</label>
                            <input id="nombreCliente" maxlength="100">
                        </div>
                    </div>
                    <div class="warning-bar" id="bloqueEntrega" hidden></div>

                    <div class="product-toolbar">
                        <div class="category-tabs" id="categorias" aria-label="Categorias"></div>
                        <input id="buscador" class="search-field" type="search" placeholder="Buscar producto..." oninput="filtrarProductos(this.value)">
                    </div>
                    <div class="products-grid" id="productos"></div>
                </section>

                <aside class="ticket" aria-label="Ticket del pedido">
                    <div class="ticket-header">
                        <div>
                            <h2 id="tituloTicket">TICKET</h2>
                            <small id="estadoTicket">Pendiente de enviar a cocina</small>
                        </div>
                        <span class="ticket-folio" id="folio">NUEVO</span>
                    </div>
                    <div class="ticket-items" id="lineasTicket"></div>
                    <div class="ticket-summary">
                        <div class="subtotal"><span id="textoSubtotal">0 productos</span><span id="subtotal">$0.00</span></div>
                        <div class="subtotal" id="lineaEnvio" hidden><span>Costo de envio</span><span>$25.00</span></div>
                        <div class="ticket-total"><span>TOTAL<small>MXN</small></span><strong id="total">$0.00</strong></div>
                        <div class="ticket-actions" id="accionesTicket"></div>
                        <p class="ticket-note" id="notaTicket"></p>
                    </div>
                </aside>
            </div>
        </main>

        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script src="js/pedido.js"></script>
    </body>
</html>

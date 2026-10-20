<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
Captura de los datos de entrega de un pedido a domicilio.
Si los datos son validos se guardan en el navegador (sessionStorage) y se pasa a la toma de pedido.
--%>
<% request.setAttribute("seccion", "domicilio");%>
<!DOCTYPE html>
<html lang="es-MX">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Datos de entrega - Mi Buen</title>
        <link rel="icon" href="assets/brand.svg" type="image/svg+xml">
        <link rel="stylesheet" href="estilos.css">
    </head>
    <body>
        <%@include file="WEB-INF/partes/encabezado.jspf" %>

        <main class="workspace" id="main">
            <div class="page-title">
                <div class="title-copy">
                    <span class="title-icon"><img src="assets/reparto.svg" alt="" width="30" height="30"></span>
                    <div>
                        <div class="eyebrow">Pedido a domicilio</div>
                        <h1>Datos de entrega</h1>
                        <p>Captura los datos del cliente antes de tomar el pedido.</p>
                    </div>
                </div>
                <div class="title-actions">
                    <a class="btn secondary compact" href="mesas.jsp">Volver a mesas</a>
                </div>
            </div>

            <form class="card customer-form datos-entrega" onsubmit="continuar(); return false;" novalidate>
                <div class="form-grid">
                    <div class="field">
                        <label for="nombreCliente">Nombre del cliente *</label>
                        <input id="nombreCliente" maxlength="100">
                        <small class="form-error" id="errorNombre"></small>
                    </div>
                    <div class="field">
                        <label for="telefono">Telefono (10 digitos) *</label>
                        <input id="telefono" inputmode="numeric" maxlength="10">
                        <small class="form-error" id="errorTelefono"></small>
                    </div>
                    <div class="field wide">
                        <label for="direccion">Direccion *</label>
                        <input id="direccion" maxlength="200">
                        <small class="form-error" id="errorDireccion"></small>
                    </div>
                    <div class="field wide">
                        <label for="referencias">Referencias</label>
                        <textarea id="referencias" maxlength="200" placeholder="Ej. casa azul, porton negro"></textarea>
                    </div>
                </div>
                <p class="muted">El costo de envio es de $25.00 y se suma al total.</p>
                <button class="btn primary" type="submit">CONTINUAR A LA TOMA DE PEDIDO</button>
            </form>
        </main>

        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script>
                // Si ya se habian capturado (por ejemplo al regresar) se vuelven a llenar
                const guardados = JSON.parse(sessionStorage.getItem('datosEntrega') || 'null');
                if (guardados) {
                    for (const campo of ['nombreCliente', 'telefono', 'direccion', 'referencias']) {
                        document.getElementById(campo).value = guardados[campo] || '';
                    }
                }

                function valor(id) {
                    return document.getElementById(id).value.trim();
                }

                /**
                 * Revisa los campos obligatorios y que el telefono tenga 10 digitos.
                 * Es la misma regla que DatosEntrega.validar() en Java.
                 */
                function validar() {
                    let valido = true;
                    document.getElementById('errorNombre').textContent = '';
                    document.getElementById('errorTelefono').textContent = '';
                    document.getElementById('errorDireccion').textContent = '';
                    if (valor('nombreCliente') === '') {
                        document.getElementById('errorNombre').textContent = 'Escribe el nombre';
                        valido = false;
                    }
                    if (!/^\d{10}$/.test(valor('telefono'))) {
                        document.getElementById('errorTelefono').textContent = 'Deben ser 10 numeros';
                        valido = false;
                    }
                    if (valor('direccion') === '') {
                        document.getElementById('errorDireccion').textContent = 'Escribe la direccion';
                        valido = false;
                    }
                    return valido;
                }

                function continuar() {
                    if (!validar()) {
                        mostrarAviso('Revisa los campos marcados', true);
                        return;
                    }
                    sessionStorage.setItem('datosEntrega', JSON.stringify({
                        nombreCliente: valor('nombreCliente'),
                        telefono: valor('telefono'),
                        direccion: valor('direccion'),
                        referencias: valor('referencias')
                    }));
                    window.location.href = 'tomarPedido.jsp?tipo=DOMICILIO';
                }
        </script>
    </body>
</html>

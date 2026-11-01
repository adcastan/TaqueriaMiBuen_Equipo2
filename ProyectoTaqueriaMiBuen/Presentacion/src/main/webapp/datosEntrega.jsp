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
        <link rel="icon" href="assets/logo-mi-buen.png" type="image/png">
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
                <div class="form-grid">
                    <%-- Por donde pidieron (lo de apps se captura como "para llevar") --%>
                    <div class="field">
                        <label>Por donde llego *</label>
                        <div class="custom-options">
                            <button type="button" class="custom-option" data-canal="TELEFONO" onclick="elegirCanal('TELEFONO')">TELEFONO</button>
                            <button type="button" class="custom-option" data-canal="WHATSAPP" onclick="elegirCanal('WHATSAPP')">WHATSAPP</button>
                        </div>
                    </div>
                    <%-- El envio no es fijo: depende de la zona. Botones rapidos para no teclear --%>
                    <div class="field">
                        <label for="costoEnvio">Costo de envio (segun la zona) *</label>
                        <input id="costoEnvio" type="number" inputmode="decimal" min="0" max="500" step="1">
                        <div class="custom-options">
                            <button type="button" class="custom-option" onclick="ponerEnvio(20)">$20</button>
                            <button type="button" class="custom-option" onclick="ponerEnvio(30)">$30</button>
                            <button type="button" class="custom-option" onclick="ponerEnvio(40)">$40</button>
                            <button type="button" class="custom-option" onclick="ponerEnvio(50)">$50</button>
                        </div>
                        <small class="form-error" id="errorEnvio"></small>
                    </div>
                    <%-- Quien lo lleva: un empleado o un servicio de afuera (DiDi, Uber) --%>
                    <div class="field">
                        <label>Quien lo lleva *</label>
                        <div class="custom-options">
                            <button type="button" class="custom-option" data-repartidor="PROPIO" onclick="elegirRepartidor('PROPIO')">EMPLEADO</button>
                            <button type="button" class="custom-option" data-repartidor="EXTERNO" onclick="elegirRepartidor('EXTERNO')">EXTERNO (DIDI, UBER)</button>
                        </div>
                    </div>
                    <div class="field">
                        <label for="repartidor">Nombre del repartidor o servicio</label>
                        <input id="repartidor" maxlength="100" placeholder="Ej. Juan, Uber Flash">
                    </div>
                </div>
                <button class="btn primary" type="submit">CONTINUAR A LA TOMA DE PEDIDO</button>
            </form>
        </main>

        <%@include file="WEB-INF/partes/pie.jspf" %>
        <script src="js/comun.js"></script>
        <script>
                // Si ya se habian capturado (por ejemplo al regresar) se vuelven a llenar
                const guardados = JSON.parse(sessionStorage.getItem('datosEntrega') || 'null');
                // Canal y repartidor elegidos (por defecto telefono y un empleado)
                let canal = 'TELEFONO';
                let tipoRepartidor = 'PROPIO';
                if (guardados) {
                    for (const campo of ['nombreCliente', 'telefono', 'direccion', 'referencias', 'costoEnvio', 'repartidor']) {
                        document.getElementById(campo).value = guardados[campo] ?? '';
                    }
                    canal = guardados.canal || canal;
                    tipoRepartidor = guardados.tipoRepartidor || tipoRepartidor;
                }
                marcarElegidos();

                // Prende el boton del canal y del repartidor que estan elegidos
                function marcarElegidos() {
                    document.querySelectorAll('[data-canal]').forEach(b => b.classList.toggle('active', b.dataset.canal === canal));
                    document.querySelectorAll('[data-repartidor]').forEach(b => b.classList.toggle('active', b.dataset.repartidor === tipoRepartidor));
                }

                function elegirCanal(nuevo) {
                    canal = nuevo;
                    marcarElegidos();
                }

                function elegirRepartidor(nuevo) {
                    tipoRepartidor = nuevo;
                    marcarElegidos();
                }

                // Botones rapidos del envio
                function ponerEnvio(monto) {
                    document.getElementById('costoEnvio').value = monto;
                    marcar('costoEnvio', 'errorEnvio', '');
                }

                // Lo que escribio el usuario en un campo, sin espacios de sobra
                function valor(id) {
                    return document.getElementById(id).value.trim();
                }

                /**
                 * Revisa los campos obligatorios y que el telefono tenga 10 digitos.
                 * Es la misma regla que DatosEntrega.validar() en Java.
                 */
                // Pone en rojo el campo y su mensaje, o lo limpia si el mensaje viene vacio
                function marcar(idCampo, idError, mensaje) {
                    document.getElementById(idError).textContent = mensaje;
                    document.getElementById(idCampo).closest('.field').classList.toggle('invalid', mensaje !== '');
                }

                /**
                 * Revisa los campos obligatorios y que el telefono tenga 10 digitos.
                 * Es la misma regla que DatosEntrega.validar() en Java (RN-06).
                 */
                function validar() {
                    let valido = true;
                    marcar('nombreCliente', 'errorNombre', '');
                    marcar('telefono', 'errorTelefono', '');
                    marcar('direccion', 'errorDireccion', '');
                    if (valor('nombreCliente') === '') {
                        marcar('nombreCliente', 'errorNombre', 'Campo obligatorio');
                        valido = false;
                    }
                    if (valor('telefono') === '') {
                        marcar('telefono', 'errorTelefono', 'Campo obligatorio');
                        valido = false;
                    } else if (!/^\d{10}$/.test(valor('telefono'))) {
                        marcar('telefono', 'errorTelefono', 'Deben ser 10 numeros, sin espacios ni guiones');
                        valido = false;
                    }
                    if (valor('direccion') === '') {
                        marcar('direccion', 'errorDireccion', 'Campo obligatorio');
                        valido = false;
                    }
                    // El envio es obligatorio (puede ser 0) y no mas de $500, igual que en Java
                    marcar('costoEnvio', 'errorEnvio', '');
                    const envio = Number(valor('costoEnvio'));
                    if (valor('costoEnvio') === '') {
                        marcar('costoEnvio', 'errorEnvio', 'Campo obligatorio');
                        valido = false;
                    } else if (!(envio >= 0 && envio <= 500)) {
                        marcar('costoEnvio', 'errorEnvio', 'De $0 a $500');
                        valido = false;
                    }
                    return valido;
                }

                // Si todo esta bien, guarda los datos en el navegador y pasa a la toma de pedido
                function continuar() {
                    if (!validar()) {
                        mostrarAviso('Revisa los campos marcados', true);
                        return;
                    }
                    sessionStorage.setItem('datosEntrega', JSON.stringify({
                        nombreCliente: valor('nombreCliente'),
                        telefono: valor('telefono'),
                        direccion: valor('direccion'),
                        referencias: valor('referencias'),
                        canal: canal,
                        costoEnvio: Number(valor('costoEnvio')),
                        tipoRepartidor: tipoRepartidor,
                        repartidor: valor('repartidor')
                    }));
                    window.location.href = 'tomarPedido.jsp?tipo=DOMICILIO';
                }
        </script>
    </body>
</html>

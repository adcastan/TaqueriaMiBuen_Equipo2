<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%--
Inicio de sesion con PIN.
Manda POST /login al LoginServlet. 
Si el PIN es correcto el servlet guarda el usuario en la sesion 
y nos dice a que pantalla entrar segun el rol.
--%>
<!DOCTYPE html>
<html lang="es-MX">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Iniciar sesion - Mi Buen</title>
        <link rel="icon" href="assets/brand.svg" type="image/svg+xml">
        <link rel="stylesheet" href="estilos.css">
    </head>
    <body>
        <main class="login-pagina">
            <section class="card login-tarjeta">
                <div class="login-marca">
                    <span class="brand-mark">A</span>
                    <div>
                        <strong>MI BUEN <span class="muted">/ ASADA POS</span></strong>
                        <small>Sistema de Gestion Taqueria Mi Buen</small>
                    </div>
                </div>
                <h1>Iniciar sesion</h1>
                <p class="muted">Escribe tu PIN de 4 digitos.</p>

                <form id="formLogin" onsubmit="iniciarSesion(); return false;">
                    <div class="field">
                        <label for="pin">PIN</label>
                        <input id="pin" type="password" inputmode="numeric" maxlength="6" autocomplete="off" autofocus>
                    </div>
                    <div class="login-teclado" id="teclado"></div>
                    <button class="btn primary full" type="submit" id="btnEntrar">ENTRAR</button>
                </form>
            </section>
        </main>
        <div id="toast" role="status" aria-live="polite"></div>

        <script src="js/comun.js"></script>
        <script>
                    // Teclado numerico en pantalla para usarlo desde el dispositivo.
                    const teclas = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'Borrar', '0', 'Limpiar'];
                    const campoPin = document.getElementById('pin');
                    document.getElementById('teclado').innerHTML = teclas
                            .map(t => '<button type="button" class="btn secondary" data-tecla="' + t + '">' + t + '</button>')
                            .join('');
                    document.getElementById('teclado').addEventListener('click', evento => {
                        const tecla = evento.target.dataset.tecla;
                        if (!tecla)
                            return;
                        if (tecla === 'Borrar')
                            campoPin.value = campoPin.value.slice(0, -1);
                        else if (tecla === 'Limpiar')
                            campoPin.value = '';
                        else if (campoPin.value.length < 6)
                            campoPin.value += tecla;
                        campoPin.focus();
                    });

                    // Manda el PIN al LoginServlet
                    async function iniciarSesion() {
                        const pin = campoPin.value.trim();
                        if (pin.length < 4) {
                            mostrarAviso('El PIN debe tener al menos 4 digitos', true);
                            return;
                        }
                        const boton = document.getElementById('btnEntrar');
                        boton.disabled = true;
                        try {
                            const respuesta = await fetch('login', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({pin: pin})
                            });
                            const datos = await respuesta.json();
                            if (!respuesta.ok) {
                                throw new Error(datos.error || 'No se pudo iniciar sesion');
                            }
                            window.location.href = datos.inicio;
                        } catch (e) {
                            mostrarAviso(e.message, true);
                            campoPin.value = '';
                            boton.disabled = false;
                        }
                    }
        </script>
    </body>
</html>

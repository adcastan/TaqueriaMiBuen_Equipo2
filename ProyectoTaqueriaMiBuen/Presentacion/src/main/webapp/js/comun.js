/*
 * Funciones que usan todas las pantallas: llamar a los servlets,
 * dar formato al dinero, mostrar avisos y cerrar sesion.
 * Equipo 2
 */

/**
 * Hace una peticion a un servlet y regresa la respuesta ya convertida de JSON.
 * Si el servidor contesta con error se lanza un Error con el mensaje que mando.
 */
async function api(url, metodo = 'GET', datos = null) {
    const opciones = { method: metodo, headers: {} };
    if (datos !== null) {
        opciones.headers['Content-Type'] = 'application/json';
        opciones.body = JSON.stringify(datos);
    }

    let respuesta;
    try {
        respuesta = await fetch(url, opciones);
    } catch (e) {
        // fetch solo truena asi cuando no hubo respuesta (sin red o servidor apagado).
        // Lo marcamos para que la pantalla ofrezca "Reintentar" sin perder lo capturado.
        const error = new Error('Sin conexion. El pedido no se ha enviado');
        error.sinConexion = true;
        throw error;
    }

    // Si la sesion se termino el filtro contesta 401 y regresamos al login
    if (respuesta.status === 401) {
        window.location.href = 'login.jsp';
        throw new Error('La sesion termino');
    }
    // 204 = No Content (por ejemplo al marcar listo en cocina)
    if (respuesta.status === 204) {
        return null;
    }

    // Si Tomcat contesta con su pagina de error en HTML, json() truena; mejor avisar bonito
    let cuerpo = null;
    try {
        cuerpo = await respuesta.json();
    } catch (e) {
        cuerpo = {error: 'El servidor contesto algo inesperado (' + respuesta.status + ')'};
    }
    if (!respuesta.ok) {
        const error = new Error((cuerpo && cuerpo.error) || 'No se pudo completar la operacion');
        error.estado = respuesta.status;
        error.datos = cuerpo;
        throw error;
    }
    return cuerpo;
}

/**
 * Clave al azar para un envio. Si la red falla y se reintenta, se manda la
 * misma clave y el servidor se da cuenta de que es el mismo envio.
 */
function nuevaClaveEnvio() {
    if (window.crypto && crypto.randomUUID) {
        return crypto.randomUUID();
    }
    // Navegadores viejitos o paginas sin https: se arma una a mano
    return 'env-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 12);
}

// 45 -> "$45.00"
function dinero(cantidad) {
    return '$' + Number(cantidad || 0).toFixed(2);
}

// Evita que un texto escrito por el usuario se meta como HTML
function escapar(texto) {
    return String(texto ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;');
}

// Muestra un aviso abajo de la pantalla por unos segundos
let temporizadorAviso = null;
// Muestra un aviso abajo de la pantalla por unos segundos (rojo si es error)
function mostrarAviso(mensaje, esError = false) {
    const aviso = document.getElementById('toast');
    aviso.textContent = mensaje;
    aviso.className = esError ? 'show error' : 'show';
    clearTimeout(temporizadorAviso);
    temporizadorAviso = setTimeout(() => aviso.className = '', 4500);
}

// Abre una ventana (modal) con el HTML que se le pase
function abrirModal(html) {
    document.getElementById('modal-root').innerHTML =
        '<div class="modal-backdrop"><section class="modal" role="dialog" aria-modal="true">' + html + '</section></div>';
}

// Cierra la ventana que este abierta
function cerrarModal() {
    document.getElementById('modal-root').innerHTML = '';
}

// Cierra la sesion en el servidor y regresa al login pase lo que pase
async function cerrarSesion() {
    try {
        await api('logout', 'POST', {});
    } finally {
        window.location.href = 'login.jsp';
    }
}

// Imagen del producto segun su categoria (las imagenes las hizo el equipo)
function imagenProducto(categoria) {
    const nombre = (categoria || '').toLowerCase();
    if (nombre.includes('taco')) return 'assets/taco.svg';
    if (nombre.includes('quesadilla')) return 'assets/quesadilla.svg';
    if (nombre.includes('bebida')) return 'assets/bebida.svg';
    return 'assets/asada.svg';
}

// Reloj del encabezado
function actualizarReloj() {
    const reloj = document.getElementById('reloj');
    if (reloj) {
        reloj.textContent = new Date().toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });
    }
}
actualizarReloj();
setInterval(actualizarReloj, 30000);

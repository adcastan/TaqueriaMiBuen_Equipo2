/*
 * kds.js - Script de kds.jsp (secuencia KDS).
 * Cada pocos segundos pide la cola a KdsServlet. Al pedirla el servidor
 * registra el latido de la terminal, asi el sistema sabe que cocina esta conectada.
 * Equipo 2
 */

const idTerminal = Number(document.body.dataset.terminal || 1);
// Cada segundo, para que la comanda aparezca en menos de 2 (requisito 3.1)
const intervaloRefresco = 1000;
let cargando = false;
let cola = [];

/**
 * GET /api/kds?terminal=1 y dibuja los detalles en orden de turno.
 */
async function cargarCola() {
    // Si la consulta anterior no ha regresado no se manda otra encima
    if (cargando) {
        return;
    }
    cargando = true;
    try {
        cola = await api('api/kds?terminal=' + idTerminal);
        dibujarCola();
        document.getElementById('ultimaActualizacion').textContent =
                'Actualizado ' + new Date().toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch (e) {
        mostrarAviso('Sin conexion con el servidor: ' + e.message, true);
    } finally {
        cargando = false;
    }
}

// Pinta una tarjeta por producto en el orden de su turno, con sus botones
function dibujarCola() {
    document.getElementById('totalPendientes').textContent = cola.length + ' pendientes';
    const contenedor = document.getElementById('cola');
    if (cola.length === 0) {
        contenedor.innerHTML = '<div class="empty"><strong>Sin pendientes</strong>Cuando llegue un pedido aparecera aqui.</div>';
        return;
    }
    contenedor.innerHTML = cola.map(d => {
        const extras = d.extras.length ? '<p><strong>Extras:</strong> ' + escapar(d.extras.join(', ')) + '</p>' : '';
        const nota = d.notaCocina ? '<p class="nota-cocina"><strong>Nota:</strong> ' + escapar(d.notaCocina) + '</p>' : '';
        const preparando = d.estado === 'EN_PREPARACION';
        // Primero se marca "en preparacion" (asi el mesero ya no lo puede quitar) y luego "listo"
        const botones = preparando
                ? '<button class="btn primary full" onclick="cambiarEstado(' + d.idDetalle + ', \'LISTO\')">LISTO</button>'
                : '<div class="kitchen-actions">'
                + '<button class="btn secondary" onclick="cambiarEstado(' + d.idDetalle + ', \'EN_PREPARACION\')">EN PREPARACION</button>'
                + '<button class="btn primary" onclick="cambiarEstado(' + d.idDetalle + ', \'LISTO\')">LISTO</button></div>';
        return '<article class="delivery-order kitchen' + (preparando ? ' preparing' : '') + '">'
                + '<div class="delivery-order-top"><strong>Turno ' + d.turno + '</strong>'
                + '<span class="folio-chip">' + escapar(d.destino) + '</span></div>'
                + (preparando ? '<span class="pill amber">EN PREPARACION</span>' : '<span class="pill">EN COLA - RONDA ' + d.ronda + '</span>')
                + '<h3>' + d.cantidad + ' x ' + escapar(d.producto) + '</h3>'
                + extras + nota
                + botones
                + '</article>';
    }).join('');
}

/**
 * POST /api/kds/detalles/{id}/estado con EN_PREPARACION o LISTO.
 * El servidor contesta 204 sin contenido. Si el mesero lo acababa de quitar,
 * contesta 409 y se avisa en pantalla.
 */
async function cambiarEstado(idDetalle, estado) {
    try {
        await api('api/kds/detalles/' + idDetalle + '/estado', 'POST', {estado: estado});
    } catch (e) {
        mostrarAviso(e.message, true);
    }
    cargarCola();
}

/**
 * El latido se manda cada vez que se pide la cola (KdsService.obtenerCola).
 */
function enviarLatido() {
    cargarCola();
}

cargarCola();
setInterval(enviarLatido, intervaloRefresco);

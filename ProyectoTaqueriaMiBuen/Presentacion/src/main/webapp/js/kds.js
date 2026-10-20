/*
 * kds.js - Script de kds.jsp (secuencia KDS).
 * Cada pocos segundos pide la cola a KdsServlet. Al pedirla el servidor
 * registra el latido de la terminal, asi el sistema sabe que cocina esta conectada.
 * Equipo 2
 */

const idTerminal = Number(document.body.dataset.terminal || 1);
const intervaloRefresco = 5000;
let cola = [];

/**
 * GET /api/kds?terminal=1 y dibuja los detalles en orden de turno.
 */
async function cargarCola() {
    try {
        cola = await api('api/kds?terminal=' + idTerminal);
        dibujarCola();
        document.getElementById('ultimaActualizacion').textContent =
                'Actualizado ' + new Date().toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch (e) {
        mostrarAviso('Sin conexion con el servidor: ' + e.message, true);
    }
}

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
        return '<article class="delivery-order kitchen">'
                + '<div class="delivery-order-top"><strong>Turno ' + d.turno + '</strong>'
                + '<span class="folio-chip">' + escapar(d.destino) + '</span></div>'
                + '<h3>' + d.cantidad + ' x ' + escapar(d.producto) + '</h3>'
                + extras + nota
                + '<button class="btn primary full" onclick="marcarListo(' + d.idDetalle + ')">LISTO</button>'
                + '</article>';
    }).join('');
}

/**
 * POST /api/kds/detalles/{id}/listo. El servidor contesta 204 sin contenido.
 */
async function marcarListo(idDetalle) {
    try {
        await api('api/kds/detalles/' + idDetalle + '/listo', 'POST', {});
        cola = cola.filter(d => d.idDetalle !== idDetalle);
        dibujarCola();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

/**
 * El latido se manda cada vez que se pide la cola (KdsService.obtenerCola).
 */
function enviarLatido() {
    cargarCola();
}

cargarCola();
setInterval(enviarLatido, intervaloRefresco);

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

// Minutos de espera para cambiar el color de la comanda (se pueden ajustar aqui).
// A tiempo = verde, atencion = ambar, tarde = rojo. Se mide desde que se mando a cocina.
const MINUTOS_ATENCION = 8;
const MINUTOS_TARDE = 15;

// Nombres cortos de los canales para la cabeza de la comanda
const CANALES = {MESA: '', MOSTRADOR: 'Mostrador', TELEFONO: 'Telefono', WHATSAPP: 'WhatsApp',
    DIDI_FOOD: 'DiDi Food', UBER_EATS: 'Uber Eats'};

/**
 * Junta los productos de la cola por comanda (mismo pedido y misma ronda),
 * porque la parrilla trabaja por mesa, no producto por producto. Las comandas
 * quedan de la mas vieja a la mas nueva (orden de turno).
 */
function agruparPorComanda(detalles) {
    const comandas = [];
    const porClave = {};
    for (const d of detalles) {
        const clave = d.idPedido + '-' + d.ronda;
        if (!porClave[clave]) {
            porClave[clave] = {idPedido: d.idPedido, destino: d.destino, canal: d.canal, ronda: d.ronda, minutos: 0, productos: []};
            comandas.push(porClave[clave]);
        }
        porClave[clave].productos.push(d);
        // La comanda espera lo que espera su producto mas viejo
        porClave[clave].minutos = Math.max(porClave[clave].minutos, d.minutosEspera || 0);
    }
    return comandas;
}

/**
 * Tira "En plancha": cuantos de cada producto faltan en TODAS las comandas.
 * Asi el parrillero echa a la plancha toda la carne de una vez. Lo de los
 * paquetes se cuenta por sus piezas (3 x Taco de asada).
 */
function dibujarEnPlancha() {
    const totales = {};
    const sumar = (nombre, cantidad) => totales[nombre] = (totales[nombre] || 0) + cantidad;
    for (const d of cola) {
        const cantidad = Number(d.cantidad);
        if (d.piezas && d.piezas.length) {
            for (const pieza of d.piezas) {
                const partes = pieza.split(' x ');
                sumar(partes[1], Number(partes[0]) * cantidad);
            }
        } else {
            sumar(d.producto + (d.unidadVenta === 'KILO' ? ' (kg)' : ''), cantidad);
        }
    }
    const nombres = Object.keys(totales);
    document.getElementById('enPlancha').innerHTML = nombres.length
            ? 'EN PLANCHA:' + nombres.map(n => '<span>' + Number(totales[n].toFixed(3)) + ' x ' + escapar(n) + '</span>').join('')
            : '';
}

// Pinta una tarjeta por comanda con un solo boton grande por producto
function dibujarCola() {
    document.getElementById('totalPendientes').textContent = cola.length + ' pendientes';
    dibujarEnPlancha();
    const contenedor = document.getElementById('cola');
    if (cola.length === 0) {
        contenedor.innerHTML = '<div class="empty"><strong>Sin pendientes</strong>Cuando llegue una comanda aparecera aqui.</div>';
        return;
    }
    contenedor.innerHTML = agruparPorComanda(cola).map(c => {
        const urgencia = c.minutos >= MINUTOS_TARDE ? ' tarde' : (c.minutos >= MINUTOS_ATENCION ? ' atencion' : '');
        const canal = CANALES[c.canal] ? ' · ' + CANALES[c.canal] : '';
        // Un solo boton para toda la comanda: si algo sigue en cola se empieza todo;
        // si ya se esta preparando, se marca toda lista. Son 2 toques por mesa.
        const hayEnCola = c.productos.some(d => d.estado === 'PENDIENTE');
        const botonComanda = hayEnCola
                ? '<button class="btn secondary full boton-comanda" onclick="cambiarComanda(' + c.idPedido + ', ' + c.ronda + ', \'EN_PREPARACION\')">EMPEZAR COMANDA</button>'
                : '<button class="btn primary full boton-comanda" onclick="cambiarComanda(' + c.idPedido + ', ' + c.ronda + ', \'LISTO\')">COMANDA LISTA</button>';
        return '<article class="comanda' + urgencia + '">'
                + '<header class="comanda-cabeza"><div><h3>' + escapar(c.destino) + '</h3>'
                + '<small>Ronda ' + c.ronda + canal + '</small></div>'
                + '<span class="minutos">' + c.minutos + ' min</span></header>'
                + c.productos.map(renglonCocina).join('')
                + '<div class="pie-comanda">' + botonComanda + '</div>'
                + '</article>';
    }).join('');
}

/**
 * Un producto de la comanda. No hace falta tocar cada uno: la comanda se
 * empieza y se termina completa. El boton chico "Ya salio" es opcional, para
 * cuando algo sale antes que lo demas (entrega parcial).
 */
function renglonCocina(d) {
    const cantidad = d.unidadVenta === 'KILO' ? Number(d.cantidad) + ' kg' : Number(d.cantidad);
    const preparando = d.estado === 'EN_PREPARACION';
    let resaltados = '';
    if (d.piezas && d.piezas.length) resaltados += '<span class="resaltado">' + escapar(d.piezas.join(', ')) + '</span> ';
    if (d.extras.length) resaltados += '<span class="resaltado">' + escapar(d.extras.join(', ')) + '</span> ';
    if (d.notaCocina) resaltados += '<span class="resaltado">' + escapar(d.notaCocina) + '</span>';
    const boton = '<button class="btn secondary compact" onclick="cambiarEstado(' + d.idDetalle + ', \'LISTO\')">Ya salio</button>';
    return '<div class="renglon-cocina' + (preparando ? ' preparando' : '') + '">'
            + '<div><h4><b>' + cantidad + '</b>' + escapar(d.producto) + '</h4>' + (resaltados ? '<p>' + resaltados + '</p>' : '') + '</div>'
            + boton + '</div>';
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
 * "Empezar comanda" o "Comanda lista": un solo toque cambia todos los
 * productos de esa mesa y ronda (POST /api/kds/comandas/{idPedido}/{ronda}/estado).
 */
async function cambiarComanda(idPedido, ronda, estado) {
    try {
        await api('api/kds/comandas/' + idPedido + '/' + ronda + '/estado', 'POST', {estado: estado});
    } catch (e) {
        mostrarAviso(e.message, true);
    }
    cargarCola();
}

/**
 * Ventana "Productos agotados": cada producto de parrilla con su estado
 * (chip Disponible / Agotado) y un boton que dice exactamente lo que hace
 * ("Marcar agotado" o "Marcar disponible"), para que no haya duda (RN-05).
 * Se marcan productos y no insumos porque es lo que el mesero deja de poder
 * pedir; los insumos solo se llevan como catalogo (decision del cliente).
 */
async function abrirAgotados() {
    let productos;
    try {
        productos = await api('api/kds/productos');
    } catch (e) {
        mostrarAviso(e.message, true);
        return;
    }
    abrirModal('<div class="modal-basic ventana-agotados"><h2>Productos agotados</h2>'
            + '<p>Si se acaba algo, marcalo como agotado. El mesero ya no podra pedirlo hasta que lo marques disponible.</p>'
            + productos.map(p =>
                '<div class="renglon-agotado">'
                + '<strong>' + escapar(p.nombre) + '</strong>'
                + '<span class="pill ' + (p.disponible ? 'green' : 'red') + '">' + (p.disponible ? 'Disponible' : 'Agotado') + '</span>'
                + (p.disponible
                    ? '<button class="btn outline" onclick="cambiarDisponible(' + p.idProducto + ', false)">Marcar agotado</button>'
                    : '<button class="btn primary" onclick="cambiarDisponible(' + p.idProducto + ', true)">Marcar disponible</button>')
                + '</div>').join('')
            + '<button class="btn secondary full" onclick="cerrarModal()">Cerrar</button></div>');
}

// Marca el producto y vuelve a pintar la ventana con el cambio
async function cambiarDisponible(idProducto, disponible) {
    try {
        await api('api/kds/productos/' + idProducto + '/disponible', 'POST', {disponible: disponible});
        await abrirAgotados();
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

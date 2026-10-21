/*
 * cobro.js - Script de cobro.jsp (secuencia Cobrar pedido, CU-05).
 * Pide a CobroServlet la caja abierta y los pedidos por cobrar,
 * abre la caja si hace falta y registra el pago de un pedido.
 * Equipo 2
 */

const parametrosCobro = new URLSearchParams(window.location.search);
let caja = null;            // caja abierta o null
let pendientes = [];        // pedidos en POR_COBRAR
let pedidoCobro = null;     // pedido que se esta cobrando
let metodoElegido = 'EFECTIVO';

/**
 * GET /api/cobros y dibuja la caja y la lista de pedidos.
 */
async function cargarCobro() {
    try {
        const datos = await api('api/cobros');
        caja = datos.caja;
        pendientes = datos.pendientes;
        dibujarCaja();
        dibujarPendientes();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

// ---------- Caja ----------

function dibujarCaja() {
    const resumen = document.getElementById('resumenCaja');
    const estado = document.getElementById('estadoCaja');
    if (!caja) {
        estado.textContent = 'Caja cerrada';
        estado.className = 'pill red';
        resumen.innerHTML = '<div class="section-heading"><h2>Abrir caja</h2><small>Antes de cobrar se abre la caja con el fondo inicial</small></div>'
                + '<div class="form-grid">'
                + '<div class="field"><label for="montoInicial">Monto inicial en efectivo</label>'
                + '<input id="montoInicial" type="number" min="0" step="0.50" value="500"></div>'
                + '</div>'
                + '<button class="btn primary" onclick="abrirCaja()">ABRIR CAJA</button>';
        return;
    }
    estado.textContent = 'Caja abierta';
    estado.className = 'pill green';
    resumen.innerHTML = '<div class="section-heading"><h2>Caja ' + caja.idCaja + '</h2>'
            + '<small>Abierta por ' + escapar(caja.abiertaPor) + ' a las ' + escapar(caja.hora) + '</small></div>'
            + '<div class="mini-stats">'
            + '<div class="mini-stat"><span>Fondo inicial</span><strong>' + dinero(caja.montoInicial) + '</strong></div>'
            + '<div class="mini-stat"><span>Cobrado en efectivo</span><strong>' + dinero(caja.cobradoEfectivo) + '</strong></div>'
            + '<div class="mini-stat"><span>Cobrado con tarjeta</span><strong>' + dinero(caja.cobradoTarjeta) + '</strong></div>'
            + '</div>';
}

/**
 * POST /api/cobros/caja con el monto inicial.
 */
async function abrirCaja() {
    const monto = Number(document.getElementById('montoInicial').value);
    try {
        await api('api/cobros/caja', 'POST', { montoInicial: monto });
    } catch (e) {
        mostrarAviso(e.message, true);
        return;
    }
    mostrarAviso('Caja abierta');
    await cargarCobro();
}

// ---------- Pedidos por cobrar ----------

function dibujarPendientes() {
    document.getElementById('totalPendientes').textContent = pendientes.length + ' por cobrar';
    const contenedor = document.getElementById('pendientes');
    if (pendientes.length === 0) {
        contenedor.innerHTML = '<div class="empty"><strong>Sin pedidos por cobrar</strong>'
                + 'Cuando un mesero solicite la cuenta el pedido aparecera aqui.</div>';
        return;
    }
    contenedor.innerHTML = pendientes.map(p =>
        '<article class="delivery-order">'
        + '<div class="delivery-order-top"><strong>' + escapar(p.destino) + '</strong>'
        + '<span class="folio-chip">' + escapar(p.folio) + '</span></div>'
        + '<p class="muted">Abierto a las ' + escapar(p.hora) + ' - ' + p.detalles.length + ' productos</p>'
        + '<h3>' + dinero(p.total) + '</h3>'
        + '<button class="btn primary full" onclick="abrirCobro(' + p.idPedido + ')" ' + (caja ? '' : 'disabled') + '>COBRAR</button>'
        + '</article>'
    ).join('');
}

// ---------- Ventana de cobro ----------

function abrirCobro(idPedido) {
    if (!caja) {
        mostrarAviso('Abre la caja antes de cobrar', true);
        return;
    }
    pedidoCobro = pendientes.find(p => p.idPedido === idPedido);
    if (!pedidoCobro) {
        mostrarAviso('Ese pedido ya no esta por cobrar', true);
        return;
    }
    metodoElegido = 'EFECTIVO';
    dibujarVentanaCobro();
}

function dibujarVentanaCobro() {
    const p = pedidoCobro;
    const lineas = p.detalles.map(d =>
        '<div class="receipt-line"><span>' + d.cantidad + 'x ' + escapar(d.producto) + '</span><span>' + dinero(d.importe) + '</span></div>'
    ).join('');
    const envio = p.costoEnvio
            ? '<div class="receipt-line"><span>Envio</span><span>' + dinero(p.costoEnvio) + '</span></div>' : '';
    const campoEfectivo = metodoElegido === 'EFECTIVO'
            ? '<div class="field"><label for="montoRecibido">Efectivo recibido</label>'
              + '<input id="montoRecibido" type="number" min="0" step="0.50" value="' + p.total.toFixed(2) + '" oninput="calcularCambio()"></div>'
              + '<div class="receipt-line receipt-total"><strong>CAMBIO</strong><strong id="cambio">$0.00</strong></div>'
            : '<p class="muted">El cobro con tarjeta se hace por el total exacto.</p>';

    abrirModal(
        '<div class="modal-basic"><h2>Cobrar - ' + escapar(p.destino) + '</h2>'
        + '<p class="muted">Folio ' + escapar(p.folio) + '</p>' + lineas + envio
        + '<div class="receipt-line receipt-total"><strong>TOTAL</strong><strong>' + dinero(p.total) + '</strong></div>'
        + '<div class="metodos-pago">'
        + '<button class="category-tab ' + (metodoElegido === 'EFECTIVO' ? 'active' : '') + '" onclick="elegirMetodo(\'EFECTIVO\')">EFECTIVO</button>'
        + '<button class="category-tab ' + (metodoElegido === 'TARJETA' ? 'active' : '') + '" onclick="elegirMetodo(\'TARJETA\')">TARJETA</button>'
        + '</div>'
        + campoEfectivo
        + '<button class="btn primary full" id="botonCobrar" onclick="cobrarPedido()">CONFIRMAR COBRO</button>'
        + '<button class="btn secondary full" onclick="cerrarModal()">REGRESAR</button></div>');
    if (metodoElegido === 'EFECTIVO') {
        calcularCambio();
    }
}

function elegirMetodo(metodo) {
    metodoElegido = metodo;
    dibujarVentanaCobro();
}

// Muestra el cambio mientras se escribe lo recibido
function calcularCambio() {
    const recibido = Number(document.getElementById('montoRecibido').value) || 0;
    const cambio = recibido - pedidoCobro.total;
    document.getElementById('cambio').textContent = cambio >= 0 ? dinero(cambio) : 'Falta ' + dinero(-cambio);
    document.getElementById('botonCobrar').disabled = cambio < 0;
}

/**
 * POST /api/cobros/{idPedido} con el metodo y lo recibido.
 * Si sale bien muestra el ticket de pago.
 */
async function cobrarPedido() {
    const campo = document.getElementById('montoRecibido');
    const recibido = campo ? Number(campo.value) : pedidoCobro.total;
    let resultado;
    try {
        resultado = await api('api/cobros/' + pedidoCobro.idPedido, 'POST', {
            metodo: metodoElegido,
            montoRecibido: recibido
        });
    } catch (e) {
        mostrarAviso(e.message, true);
        return;
    }
    mostrarTicketPago(resultado);
    await cargarCobro();
}

function mostrarTicketPago(resultado) {
    const p = pedidoCobro;
    const lineas = p.detalles.map(d =>
        '<div class="receipt-line"><span>' + d.cantidad + 'x ' + escapar(d.producto) + '</span><span>' + dinero(d.importe) + '</span></div>'
    ).join('');
    const envio = p.costoEnvio
            ? '<div class="receipt-line"><span>Envio</span><span>' + dinero(p.costoEnvio) + '</span></div>' : '';
    abrirModal(
        '<div class="modal-basic"><h2>Ticket de pago - ' + escapar(p.destino) + '</h2>'
        + '<p class="muted">Folio ' + escapar(resultado.folio) + ' - ' + new Date().toLocaleString('es-MX') + '</p>'
        + lineas + envio
        + '<div class="receipt-line receipt-total"><strong>TOTAL</strong><strong>' + dinero(resultado.total) + '</strong></div>'
        + '<div class="receipt-line"><span>Pago con ' + escapar(resultado.metodo.toLowerCase()) + '</span><span>' + dinero(resultado.montoRecibido) + '</span></div>'
        + '<div class="receipt-line"><span>Cambio</span><span>' + dinero(resultado.cambio) + '</span></div>'
        + '<p class="muted">Gracias por su visita</p>'
        + '<button class="btn primary full" onclick="window.print()">IMPRIMIR TICKET</button>'
        + '<button class="btn secondary full" onclick="cerrarModal()">SIGUIENTE COBRO</button></div>');
    mostrarAviso('Pedido ' + resultado.folio + ' pagado');
}

// ---------- Inicio ----------

async function iniciar() {
    await cargarCobro();
    // cobro.jsp?pedido=8 abre directo la ventana de ese pedido
    const idPedido = Number(parametrosCobro.get('pedido'));
    if (idPedido && caja && pendientes.some(p => p.idPedido === idPedido)) {
        abrirCobro(idPedido);
    }
}

iniciar();

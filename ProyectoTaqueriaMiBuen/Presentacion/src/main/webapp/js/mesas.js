/*
 * mesas.js - Script de mesas.jsp (diagrama de clases de presentacion).
 * Carga el mapa de mesas y lo vuelve a pedir cada cierto tiempo para ver
 * los cambios que hacen otros meseros.
 * Equipo 2
 */

// Cada cuantos milisegundos se actualiza el mapa (el diseno pide 3 segundos)
const intervaloRefresco = 3000;
// Para no encimar consultas si la red anda lenta
let cargando = false;

/**
 * Pide las mesas a MesaServlet (GET /api/mesas) y las dibuja.
 * Tambien pide los pedidos activos para mostrar los de llevar y domicilio.
 */
async function cargarMesas() {
    if (cargando) {
        return;
    }
    cargando = true;
    try {
        const mesas = await api('api/mesas');
        dibujarMesas(mesas);

        const pedidos = await api('api/pedidos');
        dibujarOtrosPedidos(pedidos.filter(p => p.tipo !== 'LOCAL'));
    } catch (e) {
        mostrarAviso(e.message, true);
    } finally {
        cargando = false;
    }
}

// Pinta las 8 mesas con su color segun su estado
function dibujarMesas(mesas) {
    const contenedor = document.getElementById('mapaMesas');
    if (mesas.length === 0) {
        contenedor.innerHTML = '<div class="empty">No hay mesas registradas</div>';
        return;
    }
    // Cada estado tiene su color: verde libre, rojo ocupada, ambar falta cobrar
    const vistas = {
        DISPONIBLE: {clase: '', texto: 'LIBRE'},
        OCUPADA: {clase: 'occupied', texto: 'OCUPADA'},
        PENDIENTE_PAGO: {clase: 'pending', texto: 'POR COBRAR'}
    };
    contenedor.innerHTML = mesas.map(mesa => {
        const vista = vistas[mesa.estado] || vistas.OCUPADA;
        const numero = String(mesa.numero).padStart(2, '0');
        let detalle = 'TOCAR PARA ABRIR';
        if (mesa.estado !== 'DISPONIBLE') {
            detalle = 'CUENTA ' + dinero(mesa.total);
        }
        return '<button class="table-tile ' + vista.clase + '" onclick="seleccionarMesa(' + mesa.idMesa + ')">'
                + '<span class="table-number">' + numero + '</span>'
                + '<strong>MESA ' + mesa.numero + '</strong>'
                + '<span class="table-state">' + vista.texto + '</span>'
                + '<small>' + detalle + ' - ' + mesa.capacidad + ' PERS.</small>'
                + '</button>';
    }).join('');
}

// Pinta abajo los pedidos para llevar y a domicilio que siguen activos
function dibujarOtrosPedidos(pedidos) {
    const contenedor = document.getElementById('otrosPedidos');
    if (pedidos.length === 0) {
        contenedor.innerHTML = '<p class="muted">No hay pedidos para llevar o a domicilio activos.</p>';
        return;
    }
    contenedor.innerHTML = pedidos.map(p =>
        '<button class="recent-card" onclick="abrirPedido(' + p.idPedido + ')">'
        + '<strong>' + escapar(p.destino) + '</strong>'
        + '<p>Folio ' + escapar(p.folio) + ' - ' + p.hora + ' - ' + (p.estado === 'POR_COBRAR' ? 'Por cobrar' : 'Abierto') + '</p>'
        + '<b>' + dinero(p.total) + '</b>'
        + '</button>'
    ).join('');
}

/**
 * Libre: abre la toma de pedido para esa mesa.
 * Ocupada: la toma de pedido carga el pedido activo de la mesa.
 */
function seleccionarMesa(idMesa) {
    window.location.href = 'tomarPedido.jsp?mesa=' + idMesa;
}

// Abre la toma de pedido de un pedido que ya existe
function abrirPedido(idPedido) {
    window.location.href = 'tomarPedido.jsp?pedido=' + idPedido;
}

/**
 * Para llevar va directo a la toma de pedido; a domicilio primero pide
 * los datos de entrega (secuencia Pedido a domicilio).
 */
function nuevoPedido(tipo) {
    if (tipo === 'DOMICILIO') {
        window.location.href = 'datosEntrega.jsp';
    } else {
        window.location.href = 'tomarPedido.jsp?tipo=LLEVAR';
    }
}

cargarMesas();
setInterval(cargarMesas, intervaloRefresco);

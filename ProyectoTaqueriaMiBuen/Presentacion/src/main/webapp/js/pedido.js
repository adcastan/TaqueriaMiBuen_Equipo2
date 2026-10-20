/*
 * pedido.js - Script de tomarPedido.jsp (diagrama de clases de presentacion).
 * Maneja el menu, la lista temporal de detalles y las acciones del pedido:
 * enviar a cocina, cerrar (pedir la cuenta) y cancelar.
 * Equipo 2
 */

const COSTO_ENVIO = 25.00;
const parametros = new URLSearchParams(window.location.search);
const rolUsuario = document.body.dataset.rol;

// Datos de la pantalla
let tipo = parametros.get('tipo') || 'LOCAL';      // LOCAL, LLEVAR o DOMICILIO
let idMesa = parametros.get('mesa') ? Number(parametros.get('mesa')) : null;
const cuentaSeparada = parametros.get('separada') === '1';
let menu = [];                 // categorias con productos (GET /api/menu)
let pedidoActual = null;       // pedido que ya esta en la BD (si existe)
let detalles = [];             // detalles temporales que aun no se envian a cocina
let datosEntrega = null;       // solo para domicilio
let categoriaActiva = 'TODAS';
let textoBusqueda = '';

// Datos de la ventana para personalizar un producto
let productoElegido = null;
let cantidadElegida = 1;
let extrasElegidos = [];

/**
 * Pide el menu a MenuServlet y dibuja las categorias y productos.
 */
async function cargarMenu() {
    menu = await api('api/menu');
    dibujarCategorias();
    dibujarProductos();
}

// Revisa si hay un pedido abierto que cargar (pedido existente o mesa ocupada)
async function cargarPedido() {
    if (parametros.get('pedido')) {
        pedidoActual = await api('api/pedidos?id=' + parametros.get('pedido'));
    } else if (idMesa !== null && !cuentaSeparada) {
        // Si la mesa esta libre el servlet regresa null
        pedidoActual = await api('api/pedidos?idMesa=' + idMesa);
    }
    if (pedidoActual) {
        tipo = pedidoActual.tipo;
        idMesa = pedidoActual.idMesa || null;
    }
    if (tipo === 'DOMICILIO' && !pedidoActual) {
        datosEntrega = JSON.parse(sessionStorage.getItem('datosEntrega') || 'null');
        if (!datosEntrega) {
            window.location.href = 'datosEntrega.jsp';
        }
    }
}

// ---------- Menu ----------

function todosLosProductos() {
    const lista = [];
    for (const categoria of menu) {
        for (const producto of categoria.productos) {
            lista.push({ ...producto, categoria: categoria.nombre });
        }
    }
    return lista;
}

function buscarProducto(idProducto) {
    return todosLosProductos().find(p => p.idProducto === idProducto);
}

function dibujarCategorias() {
    const botones = ['TODAS', ...menu.map(c => c.nombre)];
    document.getElementById('categorias').innerHTML = botones.map(nombre =>
        '<button class="category-tab ' + (nombre === categoriaActiva ? 'active' : '') + '" '
        + 'onclick="elegirCategoria(\'' + escapar(nombre) + '\')">' + escapar(nombre.toUpperCase()) + '</button>'
    ).join('');
}

function elegirCategoria(nombre) {
    categoriaActiva = nombre;
    dibujarCategorias();
    dibujarProductos();
}

function filtrarProductos(texto) {
    textoBusqueda = texto.trim().toLowerCase();
    dibujarProductos();
}

function dibujarProductos() {
    const productos = todosLosProductos().filter(p =>
        (categoriaActiva === 'TODAS' || p.categoria === categoriaActiva)
        && p.nombre.toLowerCase().includes(textoBusqueda));

    const contenedor = document.getElementById('productos');
    if (productos.length === 0) {
        contenedor.innerHTML = '<div class="empty">No hay productos con ese nombre</div>';
        return;
    }
    // Los agotados se muestran pero no se pueden elegir
    contenedor.innerHTML = productos.map(p =>
        '<button class="product-card" ' + (p.disponible ? '' : 'disabled') + ' onclick="abrirPersonalizar(' + p.idProducto + ')">'
        + '<span class="product-badge">' + (p.disponible ? escapar(p.categoria) : 'AGOTADO') + '</span>'
        + '<strong class="product-price">' + dinero(p.precio) + '</strong>'
        + '<span class="product-body">'
        + '<img class="product-photo" src="' + imagenProducto(p.categoria) + '" alt="" width="65" height="65">'
        + '<strong class="product-name">' + escapar(p.nombre.toUpperCase()) + '</strong>'
        + '<span class="product-description">' + escapar(p.descripcion || '') + '</span>'
        + '</span>'
        + '<span class="product-add">' + (p.disponible ? 'TOCAR PARA AGREGAR' : 'NO DISPONIBLE') + '</span>'
        + '</button>'
    ).join('');
}

// ---------- Ventana para elegir cantidad, extras y nota ----------

function abrirPersonalizar(idProducto) {
    if (!pedidoAbierto()) {
        mostrarAviso('Este pedido ya no acepta productos', true);
        return;
    }
    productoElegido = buscarProducto(idProducto);
    cantidadElegida = 1;
    extrasElegidos = [];
    dibujarPersonalizar();
}

function dibujarPersonalizar() {
    const p = productoElegido;
    let extrasHtml = '<p class="muted">Este producto no tiene ingredientes extra.</p>';
    if (p.extras.length > 0) {
        extrasHtml = '<div class="custom-options">' + p.extras.map(e =>
            '<button type="button" class="custom-option ' + (extrasElegidos.includes(e.idExtra) ? 'active' : '') + '" '
            + 'onclick="alternarExtra(' + e.idExtra + ')">' + escapar(e.nombre.toUpperCase())
            + '<span class="tag">+ ' + dinero(e.precio) + '</span></button>'
        ).join('') + '</div>';
    }
    const notaAnterior = document.getElementById('notaCocina') ? document.getElementById('notaCocina').value : '';

    abrirModal(
        '<header class="modal-header">'
        + '<span class="title-icon"><img src="' + imagenProducto(p.categoria) + '" alt="" width="30" height="30"></span>'
        + '<div><h2>' + escapar(p.nombre.toUpperCase()) + '</h2><p><strong>' + dinero(p.precio) + ' c/u</strong></p></div>'
        + '<button class="icon-btn" onclick="cerrarModal()" aria-label="Cerrar">X</button>'
        + '</header>'
        + '<div class="modal-body">'
        + '<div class="quantity-panel"><div><strong>CANTIDAD</strong><small>Cuantas piezas se preparan</small></div>'
        + '<div class="large-qty">'
        + '<button onclick="cambiarCantidadElegida(-1)" ' + (cantidadElegida <= 1 ? 'disabled' : '') + '>-</button>'
        + '<span>' + cantidadElegida + '<small>PIEZAS</small></span>'
        + '<button onclick="cambiarCantidadElegida(1)">+</button>'
        + '</div></div>'
        + '<section class="modal-section"><div class="modal-section-title"><strong>INGREDIENTES EXTRA</strong>'
        + '<small>Toca para agregar o quitar</small></div>' + extrasHtml + '</section>'
        + '<div class="field"><label for="notaCocina">Nota para cocina (opcional)</label>'
        + '<input id="notaCocina" maxlength="150" placeholder="Ej. sin cebolla, bien dorado" value="' + escapar(notaAnterior) + '"></div>'
        + '</div>'
        + '<footer class="modal-footer">'
        + '<button class="btn secondary" onclick="cerrarModal()">CANCELAR</button>'
        + '<button class="btn primary" onclick="confirmarProducto()">AGREGAR AL PEDIDO <strong>' + dinero(importeElegido()) + '</strong></button>'
        + '</footer>');
}

function cambiarCantidadElegida(cambio) {
    cantidadElegida = Math.max(1, Math.min(99, cantidadElegida + cambio));
    dibujarPersonalizar();
}

function alternarExtra(idExtra) {
    if (extrasElegidos.includes(idExtra)) {
        extrasElegidos = extrasElegidos.filter(id => id !== idExtra);
    } else {
        extrasElegidos.push(idExtra);
    }
    dibujarPersonalizar();
}

function importeElegido() {
    let precio = productoElegido.precio;
    for (const extra of productoElegido.extras) {
        if (extrasElegidos.includes(extra.idExtra)) precio += extra.precio;
    }
    return precio * cantidadElegida;
}

// Pasa lo que se eligio en la ventana a la lista temporal del pedido
function confirmarProducto() {
    const nota = document.getElementById('notaCocina').value.trim();
    const i = agregarDetalle(productoElegido.idProducto, cantidadElegida);
    for (const idExtra of extrasElegidos) {
        agregarExtra(i, idExtra);
    }
    detalles[i].notaCocina = nota;
    cerrarModal();
    dibujarTicket();
    mostrarAviso(cantidadElegida + ' x ' + productoElegido.nombre + ' agregado');
}

// ---------- Lista temporal de detalles (metodos del diagrama) ----------

/**
 * Agrega un detalle temporal y regresa su posicion en la lista.
 */
function agregarDetalle(idProducto, cantidad) {
    const producto = buscarProducto(idProducto);
    detalles.push({
        idProducto: producto.idProducto,
        nombre: producto.nombre,
        precio: producto.precio,
        cantidad: cantidad,
        notaCocina: '',
        extras: []
    });
    return detalles.length - 1;
}

function quitarDetalle(i) {
    detalles.splice(i, 1);
    dibujarTicket();
}

function agregarExtra(i, idExtra) {
    const extra = buscarProducto(detalles[i].idProducto).extras.find(e => e.idExtra === idExtra);
    detalles[i].extras.push(extra);
}

function cambiarCantidad(i, cambio) {
    detalles[i].cantidad += cambio;
    if (detalles[i].cantidad <= 0) {
        quitarDetalle(i);
        return;
    }
    dibujarTicket();
}

function importeDetalle(d) {
    const costoExtras = d.extras.reduce((suma, e) => suma + e.precio, 0);
    return d.cantidad * (d.precio + costoExtras);
}

/**
 * Total = lo que ya se envio + lo nuevo + envio si es a domicilio.
 */
function calcularTotal() {
    const enviado = pedidoActual ? pedidoActual.subtotal : 0;
    const nuevo = detalles.reduce((suma, d) => suma + importeDetalle(d), 0);
    const envio = tipo === 'DOMICILIO' ? COSTO_ENVIO : 0;
    return enviado + nuevo + envio;
}

// ---------- Ticket ----------

function pedidoAbierto() {
    return !pedidoActual || pedidoActual.estado === 'ABIERTO';
}

function textoDestino() {
    if (pedidoActual) return pedidoActual.destino;
    if (tipo === 'LOCAL') return 'Mesa ' + idMesa;
    if (tipo === 'LLEVAR') return 'Para llevar';
    return 'A domicilio: ' + (datosEntrega ? datosEntrega.nombreCliente : '');
}

function dibujarEncabezado() {
    const destino = textoDestino();
    document.getElementById('titulo').textContent = pedidoActual ? destino : 'Nuevo pedido: ' + destino;
    document.getElementById('subtitulo').textContent = pedidoActual
            ? 'Folio ' + pedidoActual.folio + (cuentaSeparada ? ' - cuenta separada' : '')
            : (cuentaSeparada ? 'Cuenta separada' : 'Nuevo pedido');
    document.getElementById('destino').textContent = 'ORDENANDO: ' + destino.toUpperCase();
    document.getElementById('tituloTicket').textContent = 'TICKET - ' + destino.toUpperCase();
    document.getElementById('folio').textContent = pedidoActual ? pedidoActual.folio : 'NUEVO';

    // Campos extra segun el tipo, solo cuando el pedido es nuevo
    document.getElementById('bloquePersonas').hidden = !(tipo === 'LOCAL' && !pedidoActual);
    document.getElementById('bloqueCliente').hidden = !(tipo === 'LLEVAR' && !pedidoActual);
    const entrega = pedidoActual && tipo === 'DOMICILIO' ? pedidoActual : datosEntrega;
    const bloqueEntrega = document.getElementById('bloqueEntrega');
    bloqueEntrega.hidden = tipo !== 'DOMICILIO' || !entrega;
    if (entrega) {
        bloqueEntrega.innerHTML = '<strong>Entregar a:</strong> ' + escapar(entrega.nombreCliente)
                + ' - Tel. ' + escapar(entrega.telefono) + '<br>' + escapar(entrega.direccion)
                + (entrega.referencias ? ' (' + escapar(entrega.referencias) + ')' : '');
    }
}

function lineaTicket(d, enviado, i) {
    const extras = d.extras.map(e => enviado ? e : e.nombre);
    let descripcion = extras.length ? 'Extras: ' + extras.join(', ') : '';
    if (d.notaCocina) descripcion += (descripcion ? ' - ' : '') + 'Nota: ' + d.notaCocina;
    const nombre = enviado ? d.producto : d.nombre;
    const importe = enviado ? d.importe : importeDetalle(d);

    let etiqueta = '<span class="pill amber">POR ENVIAR</span>';
    if (enviado) etiqueta = d.listo ? '<span class="pill green">LISTO</span>' : '<span class="pill">ENVIADO</span>';

    let botones = '';
    if (!enviado) {
        botones = '<div class="qty-control">'
                + '<button onclick="cambiarCantidad(' + i + ', -1)">-</button><span>' + d.cantidad + '</span>'
                + '<button onclick="cambiarCantidad(' + i + ', 1)">+</button>'
                + '<button class="remove" onclick="quitarDetalle(' + i + ')" aria-label="Quitar">x</button></div>';
    }
    return '<div class="ticket-line">'
            + '<div class="ticket-line-top"><strong>' + d.cantidad + 'x ' + escapar(nombre.toUpperCase()) + '</strong>' + etiqueta + '</div>'
            + (descripcion ? '<small>' + escapar(descripcion) + '</small>' : '')
            + '<div class="ticket-line-top"><strong>' + dinero(importe) + '</strong>' + botones + '</div>'
            + '</div>';
}

function dibujarTicket() {
    dibujarEncabezado();
    const enviados = pedidoActual ? pedidoActual.detalles : [];
    const lineas = enviados.map(d => lineaTicket(d, true)).concat(detalles.map((d, i) => lineaTicket(d, false, i)));

    document.getElementById('lineasTicket').innerHTML = lineas.length
            ? lineas.join('')
            : '<div class="empty"><strong>El pedido empieza aqui</strong>Toca un producto del menu para agregarlo.</div>';

    const piezas = enviados.reduce((s, d) => s + d.cantidad, 0) + detalles.reduce((s, d) => s + d.cantidad, 0);
    document.getElementById('textoSubtotal').textContent = piezas + ' productos';
    document.getElementById('subtotal').textContent = dinero(calcularTotal() - (tipo === 'DOMICILIO' ? COSTO_ENVIO : 0));
    document.getElementById('lineaEnvio').hidden = tipo !== 'DOMICILIO';
    document.getElementById('total').textContent = dinero(calcularTotal());
    document.getElementById('resumenPendientes').textContent = detalles.length + ' por enviar';

    const estados = { ABIERTO: 'Abierto - en cocina', POR_COBRAR: 'Por cobrar', PAGADO: 'Pagado', CANCELADO: 'Cancelado' };
    document.getElementById('estadoTicket').textContent = pedidoActual ? estados[pedidoActual.estado] : 'Pendiente de enviar a cocina';

    dibujarAcciones();
}

function dibujarAcciones() {
    let html = '';
    if (pedidoAbierto()) {
        html += '<button class="btn primary full" onclick="enviarACocina()" ' + (detalles.length ? '' : 'disabled') + '>ENVIAR A COCINA</button>';
    }
    if (pedidoActual && pedidoActual.estado === 'ABIERTO') {
        html += '<button class="btn secondary full" onclick="confirmarCerrar()" ' + (detalles.length ? 'disabled' : '') + '>CERRAR PEDIDO / SOLICITAR CUENTA</button>';
    }
    if (pedidoActual && (pedidoActual.estado === 'ABIERTO' || pedidoActual.estado === 'POR_COBRAR')) {
        html += '<button class="btn danger full" onclick="abrirCancelar()">CANCELAR PEDIDO</button>';
    }
    if (pedidoActual && tipo === 'LOCAL' && pedidoActual.estado !== 'CANCELADO') {
        html += '<a class="btn secondary full" href="tomarPedido.jsp?mesa=' + idMesa + '&separada=1">NUEVA CUENTA SEPARADA EN ESTA MESA</a>';
    }
    document.getElementById('accionesTicket').innerHTML = html;

    let nota = 'Los productos nuevos se mandan a la cola de cocina al enviar.';
    if (pedidoActual && pedidoActual.estado === 'POR_COBRAR') nota = 'El pedido ya pidio la cuenta. El cobro se hace en caja (CU-05).';
    if (detalles.length && pedidoActual) nota = 'Envia los productos nuevos antes de cerrar el pedido.';
    document.getElementById('notaTicket').textContent = nota;
}

// ---------- Acciones con el servidor ----------

// Convierte la lista temporal a la forma de DetalleDTO que espera Java
function detallesParaEnviar() {
    return detalles.map(d => ({
        idProducto: d.idProducto,
        cantidad: d.cantidad,
        notaCocina: d.notaCocina,
        idsExtras: d.extras.map(e => e.idExtra)
    }));
}

/**
 * Si el pedido es nuevo hace POST /api/pedidos con el PedidoDTO.
 * Si ya existe hace POST /api/pedidos/{id}/detalles solo con lo nuevo.
 */
async function enviarACocina() {
    if (detalles.length === 0) return;
    let resultado;
    try {
        if (pedidoActual) {
            resultado = await api('api/pedidos/' + pedidoActual.idPedido + '/detalles', 'POST', { detalles: detallesParaEnviar() });
        } else {
            const pedidoDTO = { tipo: tipo, cuentaSeparada: cuentaSeparada, detalles: detallesParaEnviar() };
            if (tipo === 'LOCAL') {
                pedidoDTO.idMesa = idMesa;
                pedidoDTO.numeroPersonas = Number(document.getElementById('numeroPersonas').value) || null;
            } else if (tipo === 'LLEVAR') {
                pedidoDTO.nombreCliente = document.getElementById('nombreCliente').value.trim();
                if (!pedidoDTO.nombreCliente) {
                    mostrarAviso('Escribe el nombre del cliente', true);
                    return;
                }
            } else {
                Object.assign(pedidoDTO, datosEntrega);
            }
            resultado = await api('api/pedidos', 'POST', pedidoDTO);
            sessionStorage.removeItem('datosEntrega');
            // Se cambia la direccion para que al recargar se abra el pedido creado
            history.replaceState(null, '', 'tomarPedido.jsp?pedido=' + resultado.idPedido);
        }
    } catch (e) {
        if (e.estado === 409) {
            mostrarAviso('La mesa ya fue ocupada por otro mesero. Regresa al mapa de mesas o abre cuenta separada.', true);
        } else {
            mostrarAviso(e.message, true);
        }
        return;
    }

    detalles = [];
    pedidoActual = await api('api/pedidos?id=' + resultado.idPedido);
    dibujarTicket();
    if (resultado.kdsConectado) {
        mostrarAviso('Pedido enviado a cocina. Folio ' + resultado.folio);
    } else {
        // Flujo alterno: el monitor de cocina no responde
        mostrarAviso('Error de conexion con cocina: se imprimio un ticket de respaldo. Folio ' + resultado.folio, true);
    }
}

function confirmarCerrar() {
    abrirModal(
        '<div class="modal-basic"><h2>Cerrar pedido</h2>'
        + '<p>El pedido queda <strong>por cobrar</strong> con un total de <strong>' + dinero(calcularTotal()) + '</strong>. '
        + 'Ya no se le pueden agregar productos.</p>'
        + '<button class="btn primary full" onclick="cerrarPedido()">SI, SOLICITAR CUENTA</button>'
        + '<button class="btn secondary full" onclick="cerrarModal()">REGRESAR</button></div>');
}

/**
 * POST /api/pedidos/{id}/cerrar y muestra el ticket con el total a pagar.
 */
async function cerrarPedido() {
    try {
        pedidoActual = await api('api/pedidos/' + pedidoActual.idPedido + '/cerrar', 'POST', {});
    } catch (e) {
        mostrarAviso(e.message, true);
        return;
    }
    dibujarTicket();
    const lineas = pedidoActual.detalles.map(d =>
        '<div class="receipt-line"><span>' + d.cantidad + 'x ' + escapar(d.producto) + '</span><span>' + dinero(d.importe) + '</span></div>'
    ).join('');
    const envio = pedidoActual.costoEnvio
            ? '<div class="receipt-line"><span>Envio</span><span>' + dinero(pedidoActual.costoEnvio) + '</span></div>' : '';
    abrirModal(
        '<div class="modal-basic"><h2>Ticket - ' + escapar(pedidoActual.destino) + '</h2>'
        + '<p class="muted">Folio ' + escapar(pedidoActual.folio) + '</p>' + lineas + envio
        + '<div class="receipt-line receipt-total"><strong>TOTAL A PAGAR</strong><strong>' + dinero(pedidoActual.total) + '</strong></div>'
        + '<button class="btn primary full" onclick="window.print()">IMPRIMIR TICKET</button>'
        + '<a class="btn secondary full" href="mesas.jsp">VOLVER A MESAS</a></div>');
}

function abrirCancelar() {
    // El mesero necesita el PIN de un cajero o administrador (RN-03)
    const pidePin = rolUsuario === 'MESERO';
    abrirModal(
        '<div class="modal-basic"><h2>Cancelar pedido</h2>'
        + '<p>Los productos que no esten listos se quitan de la cola de cocina.</p>'
        + '<div class="field"><label for="motivo">Motivo</label>'
        + '<select id="motivo"><option>El cliente se retiro</option><option>Error en la captura</option>'
        + '<option>Producto agotado</option><option>Otro</option></select></div>'
        + '<div class="field"><label for="motivoOtro">Detalle del motivo (opcional)</label><input id="motivoOtro" maxlength="100"></div>'
        + (pidePin ? '<div class="field"><label for="pinAutoriza">PIN de cajero o administrador</label>'
            + '<input id="pinAutoriza" type="password" inputmode="numeric" maxlength="6"></div>' : '')
        + '<button class="btn primary full" onclick="confirmarCancelacion()">CONFIRMAR CANCELACION</button>'
        + '<button class="btn secondary full" onclick="cerrarModal()">REGRESAR</button></div>');
}

function confirmarCancelacion() {
    const otro = document.getElementById('motivoOtro').value.trim();
    const motivo = document.getElementById('motivo').value + (otro ? ': ' + otro : '');
    cancelarPedido(motivo);
}

/**
 * POST /api/pedidos/{id}/cancelar con el motivo y, si hace falta, el PIN.
 */
async function cancelarPedido(motivo) {
    const campoPin = document.getElementById('pinAutoriza');
    try {
        await api('api/pedidos/' + pedidoActual.idPedido + '/cancelar', 'POST', {
            motivo: motivo,
            pin: campoPin ? campoPin.value.trim() : null
        });
    } catch (e) {
        mostrarAviso(e.message, true);
        return;
    }
    cerrarModal();
    mostrarAviso('Pedido cancelado');
    setTimeout(() => window.location.href = 'mesas.jsp', 1200);
}

// ---------- Inicio ----------

async function iniciar() {
    try {
        await cargarMenu();
        await cargarPedido();
        dibujarTicket();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

iniciar();

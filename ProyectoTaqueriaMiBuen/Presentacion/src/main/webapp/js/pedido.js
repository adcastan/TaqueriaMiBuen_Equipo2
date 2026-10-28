/*
 * pedido.js - Script de tomarPedido.jsp (diagrama de clases de presentacion).
 * Maneja el menu, la lista temporal de detalles y las acciones del pedido:
 * enviar a cocina, cambiar o quitar lo que sigue en cola, cerrar (pedir la
 * cuenta) y cancelar.
 *
 * Una idea para entender todo el archivo: hay DOS listas.
 *   - pedidoActual.detalles: lo que ya esta guardado en el servidor y ya se mando
 *     a cocina, con su estado (en cola, en preparacion, listo o cancelado).
 *   - detalles: lo "Nuevo", lo que el mesero esta capturando y todavia no manda.
 * Al darle "Enviar a cocina" lo nuevo se manda como una ronda y pasa a la primera lista.
 *
 * Equipo 2
 */

const COSTO_ENVIO = 25.00;
// Cada cuanto se vuelve a pedir el pedido para ver como va la cocina
const INTERVALO_REFRESCO = 3000;
const parametros = new URLSearchParams(window.location.search);
const rolUsuario = document.body.dataset.rol;
const esCajero = rolUsuario === 'CAJERO' || rolUsuario === 'ADMINISTRADOR';

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

// Clave del envio que esta en camino. Se conserva si falla la red para que
// "Reintentar" mande la misma y el servidor no duplique la comanda.
let claveEnvioPendiente = null;
let enviando = false;          // evita el doble clic en "Enviar a cocina"

// Datos de la ventana para personalizar un producto
let productoElegido = null;
let cantidadElegida = 1;
let extrasElegidos = [];

// Textos y colores de cada estado de cocina
const ESTADOS_COCINA = {
    PENDIENTE: {texto: 'EN COLA', clase: ''},
    EN_PREPARACION: {texto: 'EN PREPARACION', clase: 'amber'},
    LISTO: {texto: 'LISTO', clase: 'green'},
    CANCELADO: {texto: 'CANCELADO', clase: 'red'}
};

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

/**
 * Cada pocos segundos se vuelve a pedir el pedido para que el mesero vea
 * cuando la cocina empieza o termina algo, sin tener que recargar.
 * No toca lo que esta capturando (la lista "detalles").
 */
async function refrescarPedido() {
    if (!pedidoActual || enviando || document.hidden) {
        return;
    }
    try {
        pedidoActual = await api('api/pedidos?id=' + pedidoActual.idPedido);
        dibujarTicket();
    } catch (e) {
        // Si se cae la red un ratito no pasa nada, en el siguiente intento se actualiza
    }
}

// ---------- Menu ----------

// Junta los productos de todas las categorias en una sola lista
function todosLosProductos() {
    const lista = [];
    for (const categoria of menu) {
        for (const producto of categoria.productos) {
            lista.push({...producto, categoria: categoria.nombre});
        }
    }
    return lista;
}

// Busca un producto del menu por su id
function buscarProducto(idProducto) {
    return todosLosProductos().find(p => p.idProducto === idProducto);
}

// Pinta los botones de categorias arriba de los productos
function dibujarCategorias() {
    const botones = ['TODAS', ...menu.map(c => c.nombre)];
    document.getElementById('categorias').innerHTML = botones.map(nombre =>
        '<button class="category-tab ' + (nombre === categoriaActiva ? 'active' : '') + '" '
                + 'onclick="elegirCategoria(\'' + escapar(nombre) + '\')">' + escapar(nombre.toUpperCase()) + '</button>'
    ).join('');
}

// Cambia la categoria que se esta viendo
function elegirCategoria(nombre) {
    categoriaActiva = nombre;
    dibujarCategorias();
    dibujarProductos();
}

// Se llama mientras el mesero escribe en el buscador
function filtrarProductos(texto) {
    textoBusqueda = texto.trim().toLowerCase();
    dibujarProductos();
}

// Pinta las tarjetas de productos segun la categoria y lo que se busco
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

// Al tocar un producto: si se puede personalizar abre la ventana, si no lo agrega directo
function abrirPersonalizar(idProducto) {
    if (!pedidoAbierto()) {
        mostrarAviso('Este pedido ya no acepta productos', true);
        return;
    }
    productoElegido = buscarProducto(idProducto);
    cantidadElegida = 1;
    extrasElegidos = [];
    // Flujo 2.2.2 paso 6: lo que no tiene nada que personalizar (un refresco) entra directo
    if (productoElegido.extras.length === 0 && !esProductoConNota(productoElegido)) {
        agregarDetalle(productoElegido.idProducto, 1);
        dibujarTicket();
        bajarAlFinalDelTicket();
        mostrarAviso('1 x ' + productoElegido.nombre + ' agregado');
        return;
    }
    dibujarPersonalizar();
}

// Las bebidas no llevan nota de cocina; lo demas si (sin cebolla, bien dorado...)
function esProductoConNota(producto) {
    return !(producto.categoria || '').toLowerCase().includes('bebida');
}

// Pinta la ventana de cantidad, extras y nota para cocina
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
            + '<button onclick="cambiarCantidadElegida(1)" ' + (cantidadElegida >= 99 ? 'disabled' : '') + '>+</button>'
            + '</div></div>'
            + '<section class="modal-section"><div class="modal-section-title"><strong>INGREDIENTES EXTRA</strong>'
            + '<small>Toca para agregar o quitar</small></div>' + extrasHtml + '</section>'
            + '<div class="field"><label for="notaCocina">Nota para cocina (opcional)</label>'
            + '<input id="notaCocina" maxlength="150" placeholder="Ej. sin cebolla, bien dorado" value="' + escapar(notaAnterior) + '">'
            + '<small>Maximo 150 letras</small></div>'
            + '</div>'
            + '<footer class="modal-footer">'
            + '<button class="btn secondary" onclick="cerrarModal()">CANCELAR</button>'
            + '<button class="btn primary" onclick="confirmarProducto()">AGREGAR AL PEDIDO <strong>' + dinero(importeElegido()) + '</strong></button>'
            + '</footer>');
}

// Botones + y - de la ventana; la cantidad se queda entre 1 y 99
function cambiarCantidadElegida(cambio) {
    cantidadElegida = Math.max(1, Math.min(99, cantidadElegida + cambio));
    dibujarPersonalizar();
}

// Prende o apaga un extra en la ventana
function alternarExtra(idExtra) {
    if (extrasElegidos.includes(idExtra)) {
        extrasElegidos = extrasElegidos.filter(id => id !== idExtra);
    } else {
        extrasElegidos.push(idExtra);
    }
    dibujarPersonalizar();
}

// (precio unitario + extras) x cantidad, la misma formula que usa Java
function importeElegido() {
    let precio = productoElegido.precio;
    for (const extra of productoElegido.extras) {
        if (extrasElegidos.includes(extra.idExtra))
            precio += extra.precio;
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
    bajarAlFinalDelTicket();
    mostrarAviso(cantidadElegida + ' x ' + productoElegido.nombre + ' agregado');
}

// El ticket tiene su propia barrita para desplazarse; al agregar algo se baja
// hasta el final para que el mesero vea lo que acaba de poner
function bajarAlFinalDelTicket() {
    const lista = document.getElementById('lineasTicket');
    lista.scrollTop = lista.scrollHeight;
}

// ---------- Lista temporal de detalles (lo "Nuevo") ----------

/**
 * Agrega un detalle temporal y regresa su posicion en la lista.
 * Cualquier cambio a lo nuevo invalida la clave del envio anterior, porque ya
 * no es el mismo envio.
 */
function agregarDetalle(idProducto, cantidad) {
    const producto = buscarProducto(idProducto);
    detalles.push({
        idProducto: producto.idProducto,
        nombre: producto.nombre,
        precio: producto.precio,
        cantidad: cantidad,
        notaCocina: '',
        extras: [],
        agotado: false
    });
    claveEnvioPendiente = null;
    return detalles.length - 1;
}

// Quita un renglon de lo nuevo (todavia no se habia mandado)
function quitarDetalle(i) {
    detalles.splice(i, 1);
    claveEnvioPendiente = null;
    dibujarTicket();
}

// Le pone un extra a un renglon de lo nuevo
function agregarExtra(i, idExtra) {
    const extra = buscarProducto(detalles[i].idProducto).extras.find(e => e.idExtra === idExtra);
    detalles[i].extras.push(extra);
}

// Botones + y - de un renglon nuevo; si llega a cero se quita
function cambiarCantidad(i, cambio) {
    const nueva = detalles[i].cantidad + cambio;
    if (nueva <= 0) {
        quitarDetalle(i);
        return;
    }
    if (nueva > 99) {
        mostrarAviso('La cantidad maxima por producto es 99', true);
        return;
    }
    detalles[i].cantidad = nueva;
    claveEnvioPendiente = null;
    dibujarTicket();
}

// (precio + extras) x cantidad de un renglon nuevo
function importeDetalle(d) {
    const costoExtras = d.extras.reduce((suma, e) => suma + e.precio, 0);
    return d.cantidad * (d.precio + costoExtras);
}

/**
 * Total = lo que ya se envio (sin cancelados, eso lo calcula el servidor)
 * + lo nuevo + envio si es a domicilio.
 */
function calcularTotal() {
    const enviado = pedidoActual ? pedidoActual.subtotal : 0;
    const nuevo = detalles.reduce((suma, d) => suma + importeDetalle(d), 0);
    const envio = tipo === 'DOMICILIO' ? COSTO_ENVIO : 0;
    return enviado + nuevo + envio;
}

// ---------- Ticket ----------

// true si el pedido todavia acepta productos (o si ni siquiera existe aun)
function pedidoAbierto() {
    return !pedidoActual || pedidoActual.estado === 'ABIERTO';
}

// Texto corto de a donde va el pedido: la mesa, para llevar o el domicilio
function textoDestino() {
    if (pedidoActual)
        return pedidoActual.destino;
    if (tipo === 'LOCAL')
        return 'Mesa ' + idMesa;
    if (tipo === 'LLEVAR')
        return 'Para llevar';
    return 'A domicilio: ' + (datosEntrega ? datosEntrega.nombreCliente : '');
}

// Pinta el titulo, el folio y los campos que pide cada tipo de pedido
function dibujarEncabezado() {
    const destino = textoDestino();
    document.getElementById('titulo').textContent = pedidoActual ? destino : 'Nuevo pedido: ' + destino;
    let subtitulo = cuentaSeparada ? 'Cuenta separada' : 'Nuevo pedido';
    if (pedidoActual) {
        subtitulo = 'Folio ' + pedidoActual.folio + (cuentaSeparada ? ' - cuenta separada' : '')
                + ' - abrio ' + (pedidoActual.abiertoPor || '')
                + (pedidoActual.modificadoPor ? ' - ultimo cambio: ' + pedidoActual.modificadoPor + ' ' + pedidoActual.horaModificacion : '');
    }
    document.getElementById('subtitulo').textContent = subtitulo;
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

// Renglon de algo que ya esta en el servidor (ya se mando a cocina)
function lineaEnviada(d) {
    let descripcion = d.extras.length ? 'Extras: ' + d.extras.join(', ') : '';
    if (d.notaCocina)
        descripcion += (descripcion ? ' - ' : '') + 'Nota: ' + d.notaCocina;
    if (d.estado === 'CANCELADO' && d.motivoCancelacion)
        descripcion += (descripcion ? ' - ' : '') + 'Cancelado: ' + d.motivoCancelacion
                + (d.canceladoPor ? ' (' + d.canceladoPor + ')' : '');

    const estado = ESTADOS_COCINA[d.estado] || ESTADOS_COCINA.PENDIENTE;
    const etiqueta = '<span class="pill ' + estado.clase + '">' + estado.texto + '</span>';

    let botones = '';
    if (pedidoAbierto() && d.estado === 'PENDIENTE') {
        // Flujo 2.2.4: lo que sigue en cola se puede cambiar o quitar
        botones = '<div class="qty-control">'
                + '<button onclick="cambiarCantidadEnviada(' + d.idDetalle + ', ' + (d.cantidad - 1) + ')" aria-label="Uno menos">-</button>'
                + '<span>' + d.cantidad + '</span>'
                + '<button onclick="cambiarCantidadEnviada(' + d.idDetalle + ', ' + (d.cantidad + 1) + ')" aria-label="Uno mas">+</button>'
                + '<button class="remove" onclick="confirmarQuitarEnviado(' + d.idDetalle + ')" aria-label="Quitar">x</button></div>';
    } else if (pedidoAbierto() && (d.estado === 'EN_PREPARACION' || d.estado === 'LISTO')) {
        // Flujo 2.2.10: ya se esta preparando; solo un cajero lo quita
        botones = '<button class="btn outline compact" onclick="abrirCancelarProducto(' + d.idDetalle + ')">CANCELAR</button>';
    }
    return '<div class="ticket-line' + (d.estado === 'CANCELADO' ? ' cancelled' : '') + '">'
            + '<div class="ticket-line-top"><strong>' + d.cantidad + 'x ' + escapar(d.producto.toUpperCase()) + '</strong>' + etiqueta + '</div>'
            + (descripcion ? '<small>' + escapar(descripcion) + '</small>' : '')
            + '<div class="ticket-line-top"><strong>' + dinero(d.importe) + '</strong>' + botones + '</div>'
            + '</div>';
}

// Renglon de lo "Nuevo" que todavia no se manda
function lineaNueva(d, i) {
    let descripcion = d.extras.length ? 'Extras: ' + d.extras.map(e => e.nombre).join(', ') : '';
    if (d.notaCocina)
        descripcion += (descripcion ? ' - ' : '') + 'Nota: ' + d.notaCocina;
    const etiqueta = d.agotado
            ? '<span class="pill red">NO DISPONIBLE</span>'
            : '<span class="pill amber">NUEVO</span>';
    const botones = '<div class="qty-control">'
            + '<button onclick="cambiarCantidad(' + i + ', -1)" aria-label="Uno menos">-</button><span>' + d.cantidad + '</span>'
            + '<button onclick="cambiarCantidad(' + i + ', 1)" aria-label="Uno mas">+</button>'
            + '<button class="remove" onclick="quitarDetalle(' + i + ')" aria-label="Quitar">x</button></div>';
    return '<div class="ticket-line">'
            + '<div class="ticket-line-top"><strong>' + d.cantidad + 'x ' + escapar(d.nombre.toUpperCase()) + '</strong>' + etiqueta + '</div>'
            + (descripcion ? '<small>' + escapar(descripcion) + '</small>' : '')
            + '<div class="ticket-line-top"><strong>' + dinero(importeDetalle(d)) + '</strong>' + botones + '</div>'
            + '</div>';
}

/**
 * Arma el ticket agrupado por rondas (flujo 2.2.4 paso 2): primero lo que ya
 * se mando, ronda por ronda con la hora, y hasta abajo lo nuevo.
 */
function dibujarTicket() {
    dibujarEncabezado();
    const enviados = pedidoActual ? pedidoActual.detalles : [];
    const rondas = {};
    for (const d of enviados) {
        (rondas[d.ronda] = rondas[d.ronda] || []).push(d);
    }
    let html = '';
    for (const numero of Object.keys(rondas).map(Number).sort((a, b) => a - b)) {
        html += '<div class="ticket-round">RONDA ' + numero + ' - ' + escapar(rondas[numero][0].hora || '') + '</div>'
                + rondas[numero].map(lineaEnviada).join('');
    }
    if (detalles.length) {
        html += '<div class="ticket-round">POR ENVIAR</div>' + detalles.map(lineaNueva).join('');
    }
    // Al volver a pintar, la barrita del ticket se regresaria hasta arriba; se guarda donde iba
    const lista = document.getElementById('lineasTicket');
    const posicion = lista.scrollTop;
    lista.innerHTML = html
            || '<div class="empty"><strong>El pedido empieza aqui</strong>Toca un producto del menu para agregarlo.</div>';
    lista.scrollTop = posicion;

    const piezasEnviadas = enviados.filter(d => d.estado !== 'CANCELADO').reduce((s, d) => s + d.cantidad, 0);
    const piezas = piezasEnviadas + detalles.reduce((s, d) => s + d.cantidad, 0);
    document.getElementById('textoSubtotal').textContent = piezas + ' productos';
    document.getElementById('subtotal').textContent = dinero(calcularTotal() - (tipo === 'DOMICILIO' ? COSTO_ENVIO : 0));
    document.getElementById('lineaEnvio').hidden = tipo !== 'DOMICILIO';
    document.getElementById('total').textContent = dinero(calcularTotal());
    document.getElementById('resumenPendientes').textContent = detalles.length + ' por enviar';

    const estados = {ABIERTO: 'Abierto - en cocina', POR_COBRAR: 'Por cobrar', PAGADO: 'Pagado', CANCELADO: 'Cancelado'};
    document.getElementById('estadoTicket').textContent = pedidoActual ? estados[pedidoActual.estado] : 'Pendiente de enviar a cocina';

    dibujarAcciones();
}

// Pinta los botones de abajo del ticket segun el estado del pedido y el rol
function dibujarAcciones() {
    let html = '';
    if (pedidoAbierto()) {
        // Flujo 2.2.8: sin productos nuevos el boton queda apagado
        html += '<button class="btn primary full" id="botonEnviar" onclick="enviarACocina()" '
                + (detalles.length && !enviando ? '' : 'disabled') + '>'
                + (enviando ? 'ENVIANDO...' : 'ENVIAR A COCINA') + '</button>';
    }
    if (pedidoActual && pedidoActual.estado === 'ABIERTO') {
        html += '<button class="btn secondary full" onclick="confirmarCerrar()" ' + (detalles.length ? 'disabled' : '') + '>CERRAR PEDIDO / SOLICITAR CUENTA</button>';
    }
    // El cajero o el administrador pueden ir directo a cobrar
    if (pedidoActual && pedidoActual.estado === 'POR_COBRAR' && esCajero) {
        html += '<a class="btn primary full" href="cobro.jsp?pedido=' + pedidoActual.idPedido + '">COBRAR PEDIDO</a>';
    }
    if (pedidoActual && (pedidoActual.estado === 'ABIERTO' || pedidoActual.estado === 'POR_COBRAR')) {
        html += '<button class="btn danger full" onclick="abrirCancelar()">CANCELAR PEDIDO</button>';
    }
    if (pedidoActual && tipo === 'LOCAL' && pedidoActual.estado !== 'CANCELADO' && pedidoActual.estado !== 'PAGADO') {
        html += '<a class="btn secondary full" href="tomarPedido.jsp?mesa=' + idMesa + '&separada=1">NUEVA CUENTA SEPARADA EN ESTA MESA</a>';
    }

    document.getElementById('accionesTicket').innerHTML = html;

    let nota = 'Los productos nuevos se mandan a la cola de cocina al enviar.';
    if (pedidoAbierto() && detalles.length === 0) nota = 'Agregue al menos un producto';
    if (pedidoActual && pedidoActual.estado === 'POR_COBRAR') nota = 'El pedido ya pidio la cuenta. El cobro se hace en caja.';
    if (pedidoActual && pedidoActual.estado === 'PAGADO') nota = 'El pedido ya fue pagado.';
    if (pedidoActual && pedidoActual.estado === 'CANCELADO') nota = 'El pedido fue cancelado.';
    if (detalles.length && pedidoActual) nota = 'Envia los productos nuevos antes de cerrar el pedido.';
    if (detalles.some(d => d.agotado)) nota = 'Quita o cambia los productos marcados como no disponibles.';
    document.getElementById('notaTicket').textContent = nota;
}

// ---------- Campos obligatorios (flujo 2.2.9) ----------

// Pone el campo en rojo con su mensaje; con mensaje vacio lo regresa a la normalidad
function marcarCampo(idCampo, mensaje) {
    const campo = document.getElementById(idCampo);
    const contenedor = campo.closest('.field, .guest-count');
    let aviso = contenedor.querySelector('.field-error');
    if (!mensaje) {
        contenedor.classList.remove('invalid');
        if (aviso) aviso.remove();
        return;
    }
    contenedor.classList.add('invalid');
    if (!aviso) {
        aviso = document.createElement('small');
        aviso.className = 'field-error';
        contenedor.appendChild(aviso);
    }
    aviso.textContent = mensaje;
    campo.focus();
}

// Revisa lo que pide el subflujo "Tipar pedido" antes de mandar algo
function validarTipo() {
    if (pedidoActual) {
        return true;
    }
    if (tipo === 'LOCAL') {
        const texto = document.getElementById('numeroPersonas').value.trim();
        const personas = Number(texto);
        if (texto === '') {
            marcarCampo('numeroPersonas', 'Campo obligatorio');
            return false;
        }
        if (!Number.isInteger(personas) || personas < 1 || personas > 20) {
            marcarCampo('numeroPersonas', 'De 1 a 20 personas');
            return false;
        }
        marcarCampo('numeroPersonas', '');
    }
    if (tipo === 'LLEVAR') {
        if (document.getElementById('nombreCliente').value.trim() === '') {
            marcarCampo('nombreCliente', 'Campo obligatorio');
            return false;
        }
        marcarCampo('nombreCliente', '');
    }
    return true;
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
 * En los dos casos se manda la claveEnvio: si la red falla y se reintenta,
 * va la misma clave y el servidor no duplica nada.
 */
async function enviarACocina() {
    if (detalles.length === 0) {
        mostrarAviso('Agregue al menos un producto', true);
        return;
    }
    if (enviando || !validarTipo()) {
        return;
    }
    if (!claveEnvioPendiente) {
        claveEnvioPendiente = nuevaClaveEnvio();
    }
    enviando = true;
    dibujarAcciones();
    let resultado;
    try {
        if (pedidoActual) {
            resultado = await api('api/pedidos/' + pedidoActual.idPedido + '/detalles', 'POST',
                    {detalles: detallesParaEnviar(), claveEnvio: claveEnvioPendiente});
        } else {
            const pedidoDTO = {tipo: tipo, cuentaSeparada: cuentaSeparada, detalles: detallesParaEnviar(),
                claveEnvio: claveEnvioPendiente};
            if (tipo === 'LOCAL') {
                pedidoDTO.idMesa = idMesa;
                pedidoDTO.numeroPersonas = Number(document.getElementById('numeroPersonas').value);
            } else if (tipo === 'LLEVAR') {
                pedidoDTO.nombreCliente = document.getElementById('nombreCliente').value.trim();
            } else {
                Object.assign(pedidoDTO, datosEntrega);
            }
            resultado = await api('api/pedidos', 'POST', pedidoDTO);
            sessionStorage.removeItem('datosEntrega');
            // Se cambia la direccion para que al recargar se abra el pedido creado
            history.replaceState(null, '', 'tomarPedido.jsp?pedido=' + resultado.idPedido);
        }
    } catch (e) {
        enviando = false;
        manejarErrorDeEnvio(e);
        return;
    }

    enviando = false;
    claveEnvioPendiente = null;
    detalles = [];
    pedidoActual = await api('api/pedidos?id=' + resultado.idPedido);
    dibujarTicket();
    if (!resultado.kdsConectado) {
        // Flujo alterno: el monitor de cocina no responde
        mostrarAviso('Error de conexion con cocina: se imprimio un ticket de respaldo. Folio ' + resultado.folio, true);
    } else if (pedidoActual.tipo === 'LOCAL' && !resultado.repetido) {
        // Paso 13 del flujo basico: mensaje y de regreso al mapa de mesas
        mostrarAviso('Comanda enviada a cocina');
        setTimeout(() => window.location.href = 'mesas.jsp', 1200);
    } else {
        mostrarAviso('Comanda enviada a cocina. Folio ' + resultado.folio);
    }
}

// Decide que hacer si el envio fallo: sin red, producto agotado, mesa ocupada u otro error
function manejarErrorDeEnvio(e) {
    if (e.sinConexion) {
        // Flujo 2.2.12: se conserva todo en pantalla y se ofrece reintentar con la misma clave
        dibujarAcciones();
        abrirModal('<div class="modal-basic"><h2>Sin conexion</h2>'
                + '<p>Sin conexion. El pedido no se ha enviado. Lo que capturaste sigue aqui.</p>'
                + '<button class="btn primary full" onclick="cerrarModal(); enviarACocina()">REINTENTAR</button>'
                + '<button class="btn secondary full" onclick="cerrarModal()">CANCELAR</button></div>');
        return;
    }
    if (e.estado === 409 && e.datos && e.datos.productosNoDisponibles) {
        // Flujo 2.2.11: se marcan los agotados y se recarga el menu para que ya no aparezcan
        const agotados = e.datos.productosNoDisponibles;
        for (const d of detalles) {
            d.agotado = agotados.includes(d.idProducto);
        }
        claveEnvioPendiente = null;
        cargarMenu().catch(() => {});
        dibujarTicket();
        mostrarAviso(e.message, true);
        return;
    }
    if (e.estado === 409) {
        mostrarAviso('La mesa ya fue ocupada por otro mesero. Regresa al mapa de mesas o abre cuenta separada.', true);
    } else {
        mostrarAviso(e.message, true);
    }
    dibujarAcciones();
}

// ---------- Modificar lo que ya se envio (flujo 2.2.4) ----------

// Cambia en el servidor la cantidad de algo que sigue en cola
async function cambiarCantidadEnviada(idDetalle, cantidad) {
    if (cantidad <= 0) {
        confirmarQuitarEnviado(idDetalle);
        return;
    }
    try {
        pedidoActual = await api('api/pedidos/' + pedidoActual.idPedido + '/detalles/' + idDetalle + '/cantidad',
                'POST', {cantidad: cantidad});
        dibujarTicket();
    } catch (e) {
        mostrarAviso(e.message, true);
        refrescarPedido();
    }
}

// Pregunta antes de quitar algo que ya estaba en la cola de cocina
function confirmarQuitarEnviado(idDetalle) {
    const d = pedidoActual.detalles.find(x => x.idDetalle === idDetalle);
    abrirModal('<div class="modal-basic"><h2>Quitar producto</h2>'
            + '<p>Se quita <strong>' + d.cantidad + 'x ' + escapar(d.producto) + '</strong> y se saca de la cola de cocina.</p>'
            + '<button class="btn danger full" onclick="quitarEnviado(' + idDetalle + ')">SI, QUITAR</button>'
            + '<button class="btn secondary full" onclick="cerrarModal()">REGRESAR</button></div>');
}

// Quita en el servidor algo que seguia en cola
async function quitarEnviado(idDetalle) {
    try {
        pedidoActual = await api('api/pedidos/' + pedidoActual.idPedido + '/detalles/' + idDetalle + '/quitar', 'POST', {});
        cerrarModal();
        dibujarTicket();
        mostrarAviso('Producto quitado de la cocina');
    } catch (e) {
        cerrarModal();
        mostrarAviso(e.message, true);
        refrescarPedido();
    }
}

// Campo de PIN que se agrega a las ventanas cuando quien esta en sesion es mesero
function campoPin() {
    if (esCajero) {
        return '';
    }
    return '<div class="field"><label for="pinAutoriza">PIN de cajero o administrador</label>'
            + '<input id="pinAutoriza" type="password" inputmode="numeric" maxlength="6" autocomplete="off"></div>';
}

// Lo que se escribio en el campo de PIN, o null si no hay campo
function leerPin() {
    const campo = document.getElementById('pinAutoriza');
    return campo ? campo.value.trim() : null;
}

// Ventana para cancelar un producto que ya se esta preparando
function abrirCancelarProducto(idDetalle) {
    const d = pedidoActual.detalles.find(x => x.idDetalle === idDetalle);
    abrirModal('<div class="modal-basic"><h2>Cancelar producto</h2>'
            + '<p><strong>' + d.cantidad + 'x ' + escapar(d.producto) + '</strong> ya se esta preparando. '
            + 'Solo un cajero puede quitarlo y queda anotado el motivo.</p>'
            + '<div class="field"><label for="motivoProducto">Motivo</label>'
            + '<input id="motivoProducto" maxlength="150" placeholder="Ej. el cliente ya no lo quiso"></div>'
            + campoPin()
            + '<button class="btn danger full" onclick="cancelarProducto(' + idDetalle + ')">CANCELAR PRODUCTO</button>'
            + '<button class="btn secondary full" onclick="cerrarModal()">REGRESAR</button></div>');
}

// Manda la cancelacion del producto con su motivo y el PIN
async function cancelarProducto(idDetalle) {
    const motivo = document.getElementById('motivoProducto').value.trim();
    if (!motivo) {
        marcarCampo('motivoProducto', 'Campo obligatorio');
        return;
    }
    if (!esCajero && !leerPin()) {
        marcarCampo('pinAutoriza', 'Campo obligatorio');
        return;
    }
    try {
        pedidoActual = await api('api/pedidos/' + pedidoActual.idPedido + '/detalles/' + idDetalle + '/cancelar',
                'POST', {motivo: motivo, pin: leerPin()});
        cerrarModal();
        dibujarTicket();
        mostrarAviso('Producto cancelado');
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

// ---------- Cerrar y cancelar el pedido ----------

// Ventana de confirmacion antes de pedir la cuenta
function confirmarCerrar() {
    const activos = pedidoActual.detalles.filter(d => d.estado !== 'CANCELADO');
    const piezas = activos.reduce((s, d) => s + d.cantidad, 0);
    const pendientes = pedidoActual.pendientesCocina || 0;
    abrirModal(
            '<div class="modal-basic"><h2>Cerrar pedido</h2>'
            + '<p>' + piezas + ' productos - total <strong>' + dinero(calcularTotal()) + '</strong>. '
            + 'El pedido queda <strong>por cobrar</strong> y ya no se le pueden agregar productos.</p>'
            + (pendientes ? '<p class="warning-bar">Ojo: la cocina todavia tiene ' + pendientes + ' producto(s) sin terminar.</p>' : '')
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
    const lineas = pedidoActual.detalles.filter(d => d.estado !== 'CANCELADO').map(d =>
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

// Ventana para cancelar el pedido completo
function abrirCancelar() {
    // El mesero necesita el PIN de un cajero o administrador (RN-03)
    abrirModal(
            '<div class="modal-basic"><h2>Cancelar pedido</h2>'
            + '<p>Los productos que no esten listos se quitan de la cola de cocina. El pedido no se borra, queda como cancelado.</p>'
            + '<div class="field"><label for="motivo">Motivo</label>'
            + '<select id="motivo" onchange="marcarCampo(\'motivoOtro\', \'\')"><option>El cliente se retiro</option><option>Error en la captura</option>'
            + '<option>Producto agotado</option><option>Otro</option></select></div>'
            + '<div class="field"><label for="motivoOtro">Detalle del motivo (obligatorio si elegiste Otro)</label><input id="motivoOtro" maxlength="100"></div>'
            + campoPin()
            + '<button class="btn primary full" onclick="confirmarCancelacion()">CONFIRMAR CANCELACION</button>'
            + '<button class="btn secondary full" onclick="cerrarModal()">REGRESAR</button></div>');
}

// Revisa el motivo y el PIN antes de mandar la cancelacion
function confirmarCancelacion() {
    const elegido = document.getElementById('motivo').value;
    const otro = document.getElementById('motivoOtro').value.trim();
    // "Otro" a secas no le dice nada a nadie; se pide que expliquen
    if (elegido === 'Otro' && !otro) {
        marcarCampo('motivoOtro', 'Campo obligatorio');
        return;
    }
    if (!esCajero && !leerPin()) {
        marcarCampo('pinAutoriza', 'Campo obligatorio');
        return;
    }
    cancelarPedido(elegido + (otro ? ': ' + otro : ''));
}

/**
 * POST /api/pedidos/{id}/cancelar con el motivo y, si hace falta, el PIN.
 */
async function cancelarPedido(motivo) {
    try {
        await api('api/pedidos/' + pedidoActual.idPedido + '/cancelar', 'POST', {
            motivo: motivo,
            pin: leerPin()
        });
    } catch (e) {
        mostrarAviso(e.message, true);
        return;
    }
    cerrarModal();
    detalles = [];
    mostrarAviso('Pedido cancelado');
    setTimeout(() => window.location.href = 'mesas.jsp', 1200);
}

// ---------- Salir sin enviar (flujo 2.2.7) ----------

/**
 * Si hay productos nuevos sin mandar y el mesero se quiere ir, primero se le
 * pregunta. Si dice que si, se descartan; como un pedido nuevo no existe en la
 * base hasta que se envia, la mesa se queda libre sola.
 */
function intentarSalir(destino) {
    if (detalles.length === 0) {
        window.location.href = destino;
        return;
    }
    abrirModal('<div class="modal-basic"><h2>Productos sin enviar</h2>'
            + '<p>¿Desea salir sin enviar los productos a cocina?</p>'
            + '<button class="btn danger full" onclick="salirDescartando(\'' + escapar(destino) + '\')">SI, SALIR SIN ENVIAR</button>'
            + '<button class="btn secondary full" onclick="cerrarModal()">REGRESAR AL PEDIDO</button></div>');
}

// El mesero acepto salir: se olvida lo nuevo y se va
function salirDescartando(destino) {
    detalles = [];
    window.location.href = destino;
}

// Todos los enlaces del encabezado y "Volver a mesas" pasan por la pregunta
function protegerEnlaces() {
    document.querySelectorAll('.header-nav a, .brand, .title-actions a').forEach(enlace => {
        enlace.addEventListener('click', evento => {
            if (detalles.length > 0) {
                evento.preventDefault();
                intentarSalir(enlace.getAttribute('href'));
            }
        });
    });
    // Si cierran la pestana o recargan, el navegador hace su propia pregunta
    window.addEventListener('beforeunload', evento => {
        if (detalles.length > 0) {
            evento.preventDefault();
            evento.returnValue = '';
        }
    });
}

// ---------- Inicio ----------

// Arranque de la pantalla: menu, pedido y el refresco cada pocos segundos
async function iniciar() {
    protegerEnlaces();
    try {
        await cargarMenu();
        await cargarPedido();
        dibujarTicket();
    } catch (e) {
        mostrarAviso(e.message, true);
    }
    setInterval(refrescarPedido, INTERVALO_REFRESCO);
}

iniciar();

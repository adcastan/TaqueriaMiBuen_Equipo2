/*
 * reportes.js Script de reportes.jsp.
 * Pide a ReporteServlet las ventas del periodo y dibuja el resumen,
 * la grafica de barras y los productos mas vendidos.
 * La grafica se hace con div y CSS.
 * Equipo 2
 */

let reporte = null;        // ultimo reporte generado
let periodoAbierto = null;  // barra que se toco, para regresar a su lista de pedidos

// ---------- Fechas ----------

// Fecha local en aaaa-mm-dd.
function textoFecha(fecha) {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return fecha.getFullYear() + '-' + mes + '-' + dia;
}

function fechaCorta(texto) {
    const p = texto.split('-');
    return p[2] + '/' + p[1] + '/' + p[0];
}

// Botones rapidos: Hoy, Esta semana (desde el lunes) y Este mes (desde el dia 1).
function elegirRapido(rapido) {
    const hoy = new Date();
    let inicio = new Date(hoy);
    if (rapido === 'SEMANA') {
        const dia = (hoy.getDay() + 6) % 7; // lunes = 0
        inicio.setDate(hoy.getDate() - dia);
    } else if (rapido === 'MES') {
        inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    }
    document.getElementById('fechaInicio').value = textoFecha(inicio);
    document.getElementById('fechaFin').value = textoFecha(hoy);
    document.querySelectorAll('.rapidos .chip').forEach(b => b.classList.toggle('on', b.dataset.rapido === rapido));
    document.getElementById('campoFechas').classList.remove('invalid');
}

// Si se escribe una fecha a mano ya no aplica el boton rapido.
function quitarRapido() {
    document.querySelectorAll('.rapidos .chip').forEach(b => b.classList.remove('on'));
    document.getElementById('campoFechas').classList.remove('invalid');
}

// ---------- Generar ----------

async function generarReporte() {
    const inicio = document.getElementById('fechaInicio').value;
    const fin = document.getElementById('fechaFin').value;
    const agrupar = document.getElementById('agrupar').value;
    const comparar = document.getElementById('comparar').checked;
    const hoy = textoFecha(new Date());
    if (!inicio || !fin || inicio > fin || fin > hoy) {
        document.getElementById('campoFechas').classList.add('invalid');
        mostrarAviso('El rango de fechas no es válido', true);
        return;
    }
    const boton = document.getElementById('btnGenerar');
    boton.disabled = true;
    try {
        reporte = await api('api/reportes/ventas?inicio=' + inicio + '&fin=' + fin
                + '&agrupar=' + agrupar + '&comparar=' + comparar);
        // Si quedo un aviso de error de antes, ya no aplica
        document.getElementById('toast').className = '';
        dibujarReporte();
    } catch (e) {
        if (e.estado === 400) {
            document.getElementById('campoFechas').classList.add('invalid');
            mostrarAviso(e.message, true);
        } else {
            // Flujo 2.2.8: error al consultar, se ofrece reintentar
            document.getElementById('reporte').innerHTML = '<div class="card empty"><strong>No se pudo generar el reporte. Intenta de nuevo.</strong>'
                    + '<button class="btn primary" type="button" onclick="generarReporte()">REINTENTAR</button></div>';
        }
    } finally {
        boton.disabled = false;
    }
}

function dibujarReporte() {
    const r = reporte;
    // Flujo 2.2.6: pedidos que no se han cobrado no cuentan en el total
    document.getElementById('avisoReporte').innerHTML = r.pedidosSinCobrar > 0
            ? '<div class="aviso-reporte">Hay ' + r.pedidosSinCobrar + (r.pedidosSinCobrar === 1 ? ' pedido' : ' pedidos')
              + ' sin cobrar en este periodo; no se cuentan en el total.</div>'
            : '';

    const sinVentas = r.pedidosCobrados === 0;
    const vsTexto = r.totalAnterior == null ? '' : ' vs. ' + fechaCorta(r.inicioAnterior) + ' a ' + fechaCorta(r.finAnterior);
    let html = '<div class="reporte-izq">'
            + '<div class="tarjetas-reporte">'
            + tarjeta('Total vendido', dinero(r.totalVendido), cambioDinero(r.totalVendido, r.totalAnterior, vsTexto))
            + tarjeta('Pedidos cobrados', String(r.pedidosCobrados), cambioPedidos(r.pedidosCobrados, r.pedidosAnterior, vsTexto))
            + '<div class="card tarjeta-reporte centrada"><button class="btn secondary" type="button" onclick="imprimirReporte()">IMPRIMIR REPORTE</button></div>'
            + '</div>';

    if (sinVentas) {
        // sin ventas en el periodo
        html += '<div class="card empty"><strong>No hay ventas registradas en el periodo seleccionado</strong>Elige otro rango de fechas.</div>';
    } else {
        html += graficaHtml(r);
    }
    html += '</div>' + productosHtml(r.productos);
    document.getElementById('reporte').innerHTML = html;
}

function tarjeta(titulo, valor, cambio) {
    return '<div class="card tarjeta-reporte"><span>' + titulo + '</span><strong>' + valor + '</strong>' + cambio + '</div>';
}

// Cuanto subio o bajo contra el periodo anterior
function cambioDinero(actual, anterior, vsTexto) {
    if (anterior == null) return '';
    const diferencia = Number(actual) - Number(anterior);
    const porcentaje = Number(anterior) > 0 ? ' (' + Math.abs(diferencia / Number(anterior) * 100).toFixed(1) + '%)' : '';
    return flecha(diferencia) + dinero(Math.abs(diferencia)) + porcentaje + vsTexto + '</small>';
}

function cambioPedidos(actual, anterior, vsTexto) {
    if (anterior == null) return '';
    const diferencia = actual - anterior;
    return flecha(diferencia) + Math.abs(diferencia) + (Math.abs(diferencia) === 1 ? ' pedido' : ' pedidos') + vsTexto + '</small>';
}

function flecha(diferencia) {
    if (diferencia > 0) return '<small class="sube">▲ ';
    if (diferencia < 0) return '<small class="baja">▼ ';
    return '<small>= ';
}

// Grafica de barras: la altura de cada barra es su venta entre la venta mas alta.
function graficaHtml(r) {
    const comparar = r.totalAnterior != null;
    let maximo = 0;
    r.periodos.forEach(p => {
        maximo = Math.max(maximo, Number(p.total), comparar ? Number(p.totalAnterior) : 0);
    });
    const titulo = {DIA: 'Ventas por día', SEMANA: 'Ventas por semana', MES: 'Ventas por mes'}[r.agrupar];
    let barras = '';
    let etiquetas = '';
    r.periodos.forEach((p, i) => {
        const alto = maximo > 0 ? Math.round(Number(p.total) / maximo * 100) : 0;
        let columna = '';
        if (comparar) {
            const altoAnterior = maximo > 0 ? Math.round(Number(p.totalAnterior) / maximo * 100) : 0;
            columna += '<div class="barra anterior" style="height:' + altoAnterior + '%" title="Periodo anterior: ' + dinero(p.totalAnterior) + '"></div>';
        }
        columna += '<div class="barra" style="height:' + alto + '%"></div>';
        barras += '<button class="columna" type="button" onclick="verPedidos(' + i + ')" title="' + escapar(p.etiqueta) + ': ' + dinero(p.total) + '">'
                + '<span class="monto">' + dineroCorto(p.total) + '</span><div class="barras">' + columna + '</div></button>';
        etiquetas += '<span>' + escapar(p.etiqueta) + '</span>';
    });
    const leyenda = comparar
            ? '<span class="cuadro anterior"></span> Periodo anterior <span class="cuadro"></span> Periodo elegido · '
            : '';
    return '<section class="card grafica-reporte"><h2>' + titulo + '</h2>'
            + '<div class="grafica">' + barras + '</div>'
            + '<div class="etiquetas">' + etiquetas + '</div>'
            + '<small class="leyenda">' + leyenda + 'Toca una barra para ver sus pedidos.</small></section>';
}

// numeros sin centavos para que quepa arriba de la barra.
function dineroCorto(cantidad) {
    return '$' + Math.round(Number(cantidad)).toLocaleString('es-MX');
}

function productosHtml(productos) {
    let filas = '';
    productos.forEach((p, i) => {
        filas += '<li><span>' + (i + 1) + '. ' + escapar(p.nombre) + '</span><strong>' + cantidadTexto(p) + '</strong></li>';
    });
    if (!filas) {
        filas = '<li class="vacio">Sin productos vendidos</li>';
    }
    return '<section class="card productos-reporte"><h2>Productos más vendidos <small>(por importe)</small></h2><ol>' + filas + '</ol></section>';
}

// pzs o kgs
function cantidadTexto(p) {
    if (p.unidad === 'KILO') {
        return Number(p.cantidad).toLocaleString('es-MX', {maximumFractionDigits: 3}) + ' kg';
    }
    const piezas = Math.round(Number(p.cantidad));
    return piezas + (piezas === 1 ? ' pza' : ' pzas');
}

// ---------- Detalle de un periodo ----------

async function verPedidos(indice) {
    periodoAbierto = reporte.periodos[indice];
    try {
        const pedidos = await api('api/reportes/ventas/pedidos?inicio=' + periodoAbierto.inicio + '&fin=' + periodoAbierto.fin);
        let filas = '';
        pedidos.forEach(p => {
            filas += '<button class="renglon-reporte" type="button" onclick="verPedido(' + p.idPedido + ', \'' + escapar(p.folio) + '\')">'
                    + '<span><strong>' + escapar(p.folio) + '</strong><small>' + escapar(p.fecha) + ' · ' + escapar(p.tipo) + '</small></span>'
                    + '<strong>' + dinero(p.total) + '</strong></button>';
        });
        if (!filas) {
            filas = '<div class="empty">No hubo pedidos cobrados en este periodo</div>';
        }
        abrirModal('<div class="modal-header"><div><h2>Pedidos cobrados</h2><p>' + escapar(periodoAbierto.etiqueta) + ' · ' + dinero(periodoAbierto.total) + '</p></div></div>'
                + '<div class="modal-body lista-reporte">' + filas + '</div>'
                + '<div class="modal-footer"><button class="btn secondary full" type="button" onclick="cerrarModal()">REGRESAR</button></div>');
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

// Productos de un pedido: se usa la ruta de pedidos que ya existe.
async function verPedido(idPedido, folio) {
    try {
        const pedido = await api('api/pedidos?id=' + idPedido);
        let filas = '';
        pedido.detalles.forEach(d => {
            // Los productos cancelados no se cobraron, no se muestran.
            if (d.estado === 'CANCELADO') return;
            filas += '<div class="renglon-reporte"><span>' + cantidadTexto({cantidad: d.cantidad, unidad: d.unidadVenta}) + ' ' + escapar(d.producto)
                    + (d.cortesia ? ' <small>(cortesía)</small>' : '') + '</span><strong>' + dinero(d.importe) + '</strong></div>';
        });
        const indice = reporte.periodos.indexOf(periodoAbierto);
        abrirModal('<div class="modal-header"><div><h2>Pedido ' + escapar(folio) + '</h2><p>' + escapar(pedido.destino || '') + ' · ' + dinero(pedido.total) + '</p></div></div>'
                + '<div class="modal-body lista-reporte">' + filas + '</div>'
                + '<div class="modal-footer"><button class="btn secondary full" type="button" onclick="verPedidos(' + indice + ')">REGRESAR</button></div>');
    } catch (e) {
        mostrarAviso(e.message, true);
    }
}

// ---------- Imprimir ----------

function imprimirReporte() {
    const ahora = new Date();
    const hora = String(ahora.getHours()).padStart(2, '0') + ':' + String(ahora.getMinutes()).padStart(2, '0');
    document.getElementById('encabezadoImpresion').innerHTML = '<h1>Taquería Mi Buen</h1>'
            + '<p>Reporte de ventas del ' + fechaCorta(reporte.fechaInicio) + ' al ' + fechaCorta(reporte.fechaFin) + '</p>'
            + '<p>Generado el ' + fechaCorta(textoFecha(ahora)) + ' a las ' + hora
            + ' por ' + escapar(document.body.dataset.usuario) + '</p>';
    window.print();
}

// Al entrar: "Hoy" seleccionado y agrupado por dia
document.getElementById('fechaInicio').max = textoFecha(new Date());
document.getElementById('fechaFin').max = textoFecha(new Date());
elegirRapido('HOY');

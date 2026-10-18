/* Mi Buen · interfaz de práctica. Todos los importes son centavos enteros. */
'use strict';
const $ = (s, root = document) => root.querySelector(s);
const money = n => new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format((n || 0) / 100);
const shortMoney = n => money(n).replace(/\.00$/, '');
const esc = value => String(value ?? '').replace(/[&<>"']/g, x => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[x]));
const icons = {
  home:'M3 10 12 3l9 7v11H3z M9 21v-8h6v8', table:'M4 9h16M6 9V5h12v4M6 9v12m12-12v12M4 15h16',
  bag:'M5 7h14l1 14H4L5 7z M8 8V6a4 4 0 0 1 8 0v2', bike:'M5 21a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M19 21a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M5 17l6-9 8 9M9 8h6l-3 9H5M15 4h3l1 4',
  receipt:'M5 3h14v19l-3-2-4 2-4-2-3 2V3z M9 7h6M9 11h6M9 15h4', clock:'M12 8v5l3 2 M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0',
  plus:'M12 5v14M5 12h14', minus:'M5 12h14', close:'m6 6 12 12M18 6 6 18', check:'m5 12 4 4L19 6',
  arrow:'M19 12H5m6-6-6 6 6 6', food:'M5 3v6m3-6v6M3 6h7M6 9v12M17 3c-4 4-4 9 1 9V3h-1zm1 9v9',
  drink:'M5 6h14l-2 15H7L5 6z M13 6l2-4h5M6 11h12', fire:'M12 2c2 5 7 7 7 13a7 7 0 0 1-14 0c0-3 2-5 4-7 0 4 1 5 3 6 2-3 2-7 0-12z',
  print:'M6 8V3h12v5M6 17H3V8h18v9h-3M6 14h12v8H6z M17 11h1', card:'M3 5h18v14H3z M3 9h18M7 15h3',
  cash:'M2 6h20v13H2z M8 6c0 3-3 3-6 3m20 0c-3 0-6 0-6-3M10 12a2 2 0 1 0 4 0 2 2 0 0 0-4 0',
  transfer:'M4 7h15m-4-4 4 4-4 4M20 17H5m4-4-4 4 4 4', phone:'M7 3 3 6c0 8 7 15 15 15l3-4-5-4-3 3-5-5 3-3-4-5z',
  shield:'M12 2 3 6v6c0 5 9 10 9 10s9-5 9-10V6l-9-4z m-5 10 3 3 6-6', people:'M16 21v-3c0-4-12-4-12 0v3 M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M20 21v-4c0-2-2-3-3-3M17 3a4 4 0 0 1 0 8',
  box:'m3 7 9-5 9 5v10l-9 5-9-5V7zm0 0 9 5 9-5M12 12v10', search:'M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16m6-2 6 6', info:'M12 11v6m0-10v1 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
  trash:'M3 6h18M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7m4-7v7', exit:'M9 3H3v18h6m6-14 5 5-5 5m-8-5h13'
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="${icons[name] || icons.food}"/></svg>`;
const defaultOptions = () => ({ tortilla: 'maiz', onion: true, cilantro: true, salsa: true, salsaAside: false });
const statuses = { draft: 'Por enviar', kitchen: 'En preparación', ready: 'Listo para entregar', dispatched: 'En reparto', delivered: 'Entregado · pago pendiente', paid: 'Liquidado', cancelled: 'Cancelado' };
const channels = { salon: 'Salón', llevar: 'Para llevar', domicilio: 'A domicilio' };
const state = { data: null, page: 'home', channel: 'salon', tableId: null, orderId: null, cart: [], form: {}, category: 'all', search: '', busy: false, online: true, checkout: null, modal: null, restored: false };
let modalTrigger = null, toastTimer, activeRequest = null;

function toast(message, error = false) {
  const el = $('#toast'); el.textContent = message; el.className = `show${error ? ' error' : ''}`;
  clearTimeout(toastTimer); toastTimer = setTimeout(() => el.className = '', 4800);
}
async function api(path, method = 'GET', body) {
  let response;
  try { response = await fetch(path, { method, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body ? JSON.stringify(body) : undefined }); }
  catch { state.online = false; throw new Error('Sin conexión con la estación. Tu borrador sigue en este navegador; vuelve a intentar cuando regrese la conexión.'); }
  const data = await response.json();
  if (!response.ok) { const error = new Error(data.error || data.message || 'No se pudo completar la operación.'); error.details = data; error.status = response.status; throw error; }
  state.online = true; return data;
}
function orderById(id) { return state.data?.orders.find(o => o.id === Number(id)); }
function currentOrder() { return orderById(state.orderId); }
function openOrders(channel) { return state.data.orders.filter(o => !o.closed && (!channel || o.channel === channel)); }
function tableOrder(id) { return openOrders('salon').find(o => o.tableId === Number(id)); }
function age(order) { const minutes = Math.max(0, Math.floor((Date.now() - Date.parse(order.createdAt)) / 60000)); return `${minutes} min`; }
function product(id) { return state.data.products.find(p => p.id === Number(id)); }
function unitPrice(line) { const p = product(line.productId); return p.price + (p.customizable && line.options?.tortilla === 'harina' ? 500 : 0); }
function cartTotal() { return state.cart.reduce((sum, line) => sum + unitPrice(line) * line.quantity, 0); }
function optionsText(options, id) {
  if (!product(id)?.customizable) return '';
  const o = options || defaultOptions(), parts = [o.tortilla === 'harina' ? 'Tortilla harina (+$5/pza)' : 'Tortilla maíz'];
  if (!o.onion) parts.push('Sin cebolla'); if (!o.cilantro) parts.push('Sin cilantro'); if (!o.salsa) parts.push('Sin salsa');
  if (o.onion && o.cilantro && o.salsa) parts.push('Con todo'); if (o.salsaAside) parts.push('Salsa aparte'); return parts.join(' · ');
}
function saveDraft() {
  try { localStorage.setItem('mibuen-draft-v1', JSON.stringify({ channel: state.channel, tableId: state.tableId, orderId: state.orderId, cart: state.cart, form: state.form, requestId: activeRequest })); }
  catch { toast('El navegador no pudo guardar el borrador. Envía el pedido antes de cerrar.', true); }
}
function restoreDraft() {
  try {
    const draft = JSON.parse(localStorage.getItem('mibuen-draft-v1') || 'null');
    if (draft && Array.isArray(draft.cart) && draft.cart.length && ['salon', 'llevar', 'domicilio'].includes(draft.channel)) {
      state.channel = draft.channel; state.tableId = draft.tableId; state.orderId = draft.orderId; state.form = draft.form || {};
      state.cart = draft.cart.filter(l => product(l.productId) && Number.isInteger(l.quantity) && l.quantity >= 1 && l.quantity <= 99).map(l => ({ ...l, options: { ...defaultOptions(), ...l.options } }));
      state.restored = state.cart.length > 0;
      activeRequest = typeof draft.requestId === 'string' ? draft.requestId : null;
    }
  } catch { /* Un borrador inválido nunca impide abrir la estación. */ }
}
function syncOrder(order) { const index = state.data.orders.findIndex(o => o.id === order.id); if (index < 0) state.data.orders.unshift(order); else state.data.orders[index] = order; }
async function refresh(renderPage = false) { state.data = await api('/api/bootstrap'); if (renderPage) render(); }
function navigate(page, extras = {}) {
  Object.assign(state, extras, { page }); state.modal = null; closeModal();
  const params = new URLSearchParams(); if (state.orderId && ['order', 'checkout'].includes(page)) params.set('id', state.orderId);
  const hash = `#${page}${params.size ? `?${params}` : ''}`;
  if (location.hash !== hash) history.pushState(null, '', hash);
  render(); window.scrollTo(0, 0);
}
function openOrder(id) {
  const order = orderById(id); if (!order) return toast('Pedido no encontrado.', true);
  if (state.cart.length && state.orderId !== order.id) return confirmDiscard(() => openOrder(id));
  Object.assign(state, { orderId: order.id, channel: order.channel, tableId: order.tableId, form: { customer: order.customer, phone: order.phone, address: order.address, reference: order.reference, notes: order.notes, guests: order.guests }, category: 'all', search: '' });
  navigate('order'); saveDraft();
}
function newOrder(channel, tableId = null) {
  const existing = channel === 'salon' && tableOrder(tableId);
  if (existing) return openOrder(existing.id);
  if (state.cart.length) return confirmDiscard(() => newOrder(channel, tableId));
  Object.assign(state, { channel, tableId, orderId: null, cart: [], form: { customer: '', phone: '', address: '', reference: '', notes: '', guests: 2 }, category: 'all', search: '', restored: false });
  navigate('order'); saveDraft();
}
function confirmDiscard(callback) {
  openBasicModal('Hay productos por enviar', '<p>Este borrador aún no se ha enviado a cocina. Puedes seguir capturándolo o descartarlo para abrir otra comanda.</p><button class="btn primary full" data-action="resume-draft">Continuar borrador</button><button class="btn secondary full" id="discard-confirm">Descartar borrador y continuar</button>');
  $('#discard-confirm').onclick = () => { state.cart = []; activeRequest = null; saveDraft(); closeModal(); callback(); };
}
function header() {
  const active = state.page === 'tables' || (['order', 'checkout'].includes(state.page) && state.channel === 'salon') ? 'salon' : state.page === 'delivery' || (['order', 'checkout'].includes(state.page) && state.channel === 'domicilio') ? 'domicilio' : ['order', 'checkout'].includes(state.page) && state.channel === 'llevar' ? 'llevar' : state.page;
  return `<header class="app-header"><a class="brand" href="#home" data-action="home"><span class="brand-mark">A</span><div><strong>MI BUEN <span class="muted">/ ASADA POS</span></strong><small>Tu turno. Tu ritmo. Tu estación.</small></div></a><nav class="header-nav" aria-label="Navegación principal"><button data-action="tables" class="${active === 'salon' ? 'active' : ''}">Comedor / Mesas</button><button data-action="new-llevar" class="${active === 'llevar' ? 'active' : ''}">Para llevar</button><button data-action="delivery" class="${active === 'domicilio' ? 'active' : ''}">A domicilio</button><button data-action="kitchen" class="${active === 'kitchen' ? 'active' : ''}">Comandas</button></nav><div class="header-side"><span class="clock">${icon('clock')}<span id="clock">${new Date().toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })}</span></span><div class="user"><div><strong>Estación de práctica</strong><small>Operación local</small></div><span class="avatar" title="Modo de práctica">M</span></div></div></header>`;
}
function footer() { return `<footer class="footer"><span>Estación: <strong>${esc(state.data.settings.station)}</strong> &nbsp; <span class="connection ${state.online ? '' : 'offline'}">${state.online ? 'Servidor local conectado' : 'Sin conexión'}</span></span><span>DEMO EDUCATIVA · Pagos de prueba · Sin conexión bancaria</span><button data-action="about" style="background:transparent;color:inherit;font-size:10px;text-decoration:underline">Ayuda / Acerca de</button></footer>`; }
function title(eyebrow, text, subtitle = '', name = 'receipt', actions = '') {
  return `<div class="page-title"><div class="title-copy"><span class="title-icon">${icon(name)}</span><div><div class="eyebrow">${eyebrow}</div><h1>${text}</h1>${subtitle ? `<p>${subtitle}</p>` : ''}</div></div>${actions ? `<div class="title-actions">${actions}</div>` : ''}</div>`;
}
function render() {
  if (!state.data) return;
  const views = { home: homeView, tables: tablesView, order: orderView, delivery: () => boardView('domicilio'), kitchen: () => boardView(), checkout: checkoutView, history: historyView };
  $('#app').innerHTML = `${header()}${!state.online ? '<div class="offline-banner" role="alert">No hay conexión con el servidor. Las acciones enviadas necesitan confirmación antes de continuar.</div>' : ''}<main class="workspace" id="main" tabindex="-1">${(views[state.page] || homeView)()}</main>${footer()}${state.page === 'order' ? `<div class="mobile-order-dock"><button class="btn secondary" data-action="show-menu">${icon('food')} Menú</button><button class="btn primary" data-action="show-ticket">${icon('receipt')} Ver pedido <strong>${money((currentOrder()?.remaining || 0)+cartTotal())}</strong></button></div>` : ''}`;
  document.title = `${{ home: 'Nueva orden', tables: 'Mesas', order: 'Tomar pedido', delivery: 'Domicilios', kitchen: 'Comandas', checkout: 'Cobrar', history: 'Historial' }[state.page] || 'Inicio'} · Mi Buen`;
  updatePaymentDisplay();
}
function homeView() {
  const salon = openOrders('salon'), llevar = openOrders('llevar'), dom = openOrders('domicilio'), all = openOrders();
  return `${title('Nueva orden · servicio de taquería', 'Selecciona el destino del pedido', 'Todo listo para una buena comanda.', 'receipt', `<span class="pill">${all.length} activas</span><span class="pill amber">${all.filter(o => o.status === 'kitchen').length} en cocina</span><span class="pill green">${all.filter(o => o.status === 'ready').length} listas</span>`)}
  ${state.restored && state.cart.length ? `<div class="warning-bar">Recuperamos un borrador con ${state.cart.length} productos por enviar. <button class="btn secondary compact" data-action="resume-draft">Continuar captura</button></div>` : ''}
  <section class="destination-grid" aria-label="Destinos de pedido">
    ${destinationCard('salon', 'SALÓN PRINCIPAL', '12 mesas · atención en mesa', 'comedor.svg', 'food', 'COMEDOR', 'COMER EN MESA', 'Servicio a mesas activas en salón interior y terraza. La cuenta permanece abierta.', 'Disponibles', 12 - salon.length, 'Mesas libres', 'Ocupadas', salon.length, 'En consumo', 'SELECCIONAR MESA', 'tables')}
    ${destinationCard('takeaway', 'PICKUP / BARRA', 'Sin entrega a domicilio', 'mostrador.svg', 'bag', 'PARA LLEVAR', 'MOSTRADOR / BARRA', 'Recibe y prepara pedidos para recoger. Identifica cada comanda con el nombre del cliente.', 'En preparación', llevar.filter(o => ['draft', 'kitchen'].includes(o.status)).length, 'En cocina', 'Listas para recoger', llevar.filter(o => o.status === 'ready').length, 'En mostrador', 'INICIAR PEDIDO LLEVAR', 'new-llevar')}
    ${destinationCard('delivery', 'REPARTO PROPIO', 'Seguimiento de entregas', 'reparto.svg', 'bike', 'DOMICILIO', 'ENVÍO A DOMICILIO', 'Captura cliente, dirección y referencias. Organiza el despacho y confirma cada entrega.', 'En ruta', dom.filter(o => o.status === 'dispatched').length, 'Repartidores', 'Por despachar', dom.filter(o => o.status === 'ready').length, 'Pedidos listos', 'CAPTURAR DOMICILIO', 'new-domicilio')}
  </section><section class="home-bottom"><div class="card recent-panel"><div class="section-heading"><h2>${icon('receipt')} Últimas comandas abiertas</h2><button data-action="kitchen" class="btn secondary compact">Ver todas →</button></div><div class="recent-grid">${all.slice(0, 3).map(o => `<button class="recent-card" data-action="open-order" data-id="${o.id}"><strong>${o.channel === 'salon' ? `Mesa ${String(o.tableId).padStart(2, '0')}` : esc(o.folio)} <span class="status-mini">${age(o)}</span></strong><p>${esc(o.items.map(i => `${i.quantity} ${i.name}`).join(', ').slice(0, 58))}</p><b>${money(o.total)}</b> <span class="muted" style="font-size:9px">${statuses[o.status]}</span></button>`).join('') || '<p class="muted">No hay comandas abiertas.</p>'}</div></div><div class="card quick-panel"><div class="section-heading"><h2>${icon('cash')} Tu operación, a la vista</h2></div><p>Consulta ventas registradas, pagos y tickets. Cada comanda conserva su historial al recargar.</p><div class="quick-actions"><button class="btn secondary compact" data-action="history">${icon('receipt')} Historial / caja</button><button class="btn secondary compact" data-action="about">${icon('info')} Guía rápida</button></div></div></section>`;
}
function destinationCard(cls, tag, meta, image, ico, label, heading, description, stat1, num1, note1, stat2, num2, note2, cta, action) {
  return `<article class="card destination-card ${cls}"><div class="destination-top"><span class="pill ${cls === 'salon' ? 'green' : cls === 'delivery' ? 'red' : 'amber'}">● &nbsp; ${tag}</span><small>${meta}</small></div><div class="destination-image"><img src="/assets/${image}" alt="" width="600" height="300"><strong>${icon(ico)} ${label}</strong></div><h2>${heading}</h2><p>${description}</p><div class="mini-stats"><div class="mini-stat"><span>${stat1}</span><strong>${num1}</strong><small>${note1}</small></div><div class="mini-stat ${cls === 'salon' || cls === 'delivery' ? 'red' : ''}"><span>${stat2}</span><strong>${num2}</strong><small>${note2}</small></div></div><button class="btn primary full" data-action="${action}">${icon(ico)} ${cta}</button></article>`;
}
function tablesView() {
  return `${title('Comedor / mesas', 'Elige una mesa y empieza', 'Consulta la cuenta de las mesas ocupadas o abre una nueva comanda.', 'table', '<button class="btn secondary compact" data-action="home">'+icon('arrow')+' Volver al inicio</button>')}<div class="table-legend"><div class="legend">✓ VERDE = LIBRE<small>TOCA PARA ABRIR ORDEN</small></div><div class="legend red">▣ ROJO = OCUPADA<small>TOCA PARA VER LA CUENTA</small></div></div><section class="table-grid" aria-label="Mapa de las doce mesas">${state.data.tables.map(t => { const o = tableOrder(t.id); return `<button class="table-tile ${o ? 'occupied' : ''}" data-action="table" data-id="${t.id}" aria-label="${esc(t.name)}, ${o ? `ocupada, saldo ${money(o.remaining)}` : 'libre'}"><span class="table-number">${String(t.id).padStart(2, '0')}</span>${icon(o ? 'receipt' : 'plus')}<strong>MESA ${t.id}</strong><span class="table-state">${o ? 'OCUPADA' : 'LIBRE'}</span><small>${o ? `${money(o.remaining)} MXN · ${o.guests} PERS.` : 'TOCAR PARA ABRIR'}</small></button>`; }).join('')}</section>`;
}
function orderView() {
  const o = currentOrder(), closed = o?.closed, label = state.channel === 'salon' ? `Mesa ${String(state.tableId).padStart(2, '0')}` : channels[state.channel];
  if (state.channel === 'salon' && !state.tableId) return tablesView();
  return `${title(o ? `Comanda ${esc(o.folio)} · ${statuses[o.status]}` : 'Nueva comanda', `${o ? 'Pedido' : 'Nuevo pedido'}: ${label}`, state.channel === 'llevar' ? 'Mostrador express · captura rápida' : state.channel === 'domicilio' ? 'Cliente, ubicación y pedido en un solo lugar.' : 'Asada al carbón. Una orden bien tomada.', state.channel === 'salon' ? 'food' : state.channel === 'llevar' ? 'bag' : 'bike', `<button class="btn secondary compact" data-action="${state.channel === 'salon' ? 'tables' : state.channel === 'domicilio' ? 'delivery' : 'kitchen'}">${icon('arrow')} ${state.channel === 'salon' ? 'Cambiar mesa' : 'Ver comandas'}</button>${o && o.remaining > 0 && !closed ? `<button class="btn primary compact" data-action="checkout" data-id="${o.id}">${icon('cash')} Cobrar cuenta</button>` : ''}`)}
    ${closed ? `<div class="warning-bar">Esta comanda está ${o.status === 'paid' ? 'liquidada' : 'cancelada'}. Puedes consultar su ticket en el historial.</div>` : ''}
    <div class="order-steps"><span><b>1</b> Agrega productos</span><span><b>2</b> Revisa el pedido</span><span><b>3</b> Envía a cocina</span></div><div class="order-layout"><section aria-label="Captura del pedido" id="order-menu"><div class="order-heading"><strong>${icon(state.channel === 'salon' ? 'table' : 'bag')} ORDENANDO: ${label.toUpperCase()}</strong><small>${o ? esc(o.folio) : 'Folio al confirmar'} · ${state.cart.length} por enviar</small></div>${state.channel === 'salon' ? `<div class="guest-count"><label for="guests">${icon('people')} Comensales</label><input id="guests" data-form="guests" type="number" min="1" max="20" value="${esc(state.form.guests || 2)}" ${o ? 'disabled' : ''}></div>` : customerForm()}
    <div class="product-toolbar"><div class="category-tabs" aria-label="Categorías">${[['all', 'Todo', 'food'], ['tacos', 'Tacos', 'food'], ['quesadillas', 'Quesadillas', 'food'], ['bebidas', 'Bebidas', 'drink'], ['paquetes', 'Paquetes', 'box']].map(([key, name, ico]) => `<button class="category-tab ${state.category === key ? 'active' : ''}" data-action="category" data-id="${key}" aria-pressed="${state.category === key}">${icon(ico)} ${name.toUpperCase()}</button>`).join('')}</div><input id="product-search" class="search-field" type="search" placeholder="Buscar producto…" aria-label="Buscar productos" value="${esc(state.search)}"></div><div class="products-grid" id="products-grid">${productsView()}</div><div class="capacity">${icon('shield')} <span>Precios de muestra en MXN. Las opciones y los importes se verifican al enviar.</span></div></section>${ticketView(o)}</div>`;
}
function customerForm() {
  const dom = state.channel === 'domicilio', fields = [['customer', 'Nombre del cliente'+(dom ? ' *' : ''), 'text', 'Ej. Juan Roberto'], ['phone', 'Teléfono'+(dom ? ' *' : ' (opcional)'), 'tel', 'Ej. 555 123 4567']];
  const locked = currentOrder() ? 'readonly aria-readonly="true"' : '';
  return `<section class="card customer-form"><div class="section-heading"><h2>${icon(dom ? 'bike' : 'bag')} ${dom ? 'Datos de entrega' : 'Datos rápidos de entrega'}</h2><small>${currentOrder() ? 'Datos guardados' : dom ? 'Campos * obligatorios' : 'Mostrador / recogida'}</small>${currentOrder() && !currentOrder().closed ? '<button class="btn secondary compact" data-action="edit-customer">Editar datos</button>' : ''}</div><div class="form-grid">${fields.map(([key, label, type, placeholder]) => `<div class="field"><label for="f-${key}">${label}</label><input id="f-${key}" data-form="${key}" type="${type}" value="${esc(state.form[key] || '')}" placeholder="${placeholder}" maxlength="150" ${dom ? 'required' : ''} ${locked}></div>`).join('')}${dom ? `<div class="field wide"><label for="f-address">Dirección completa *</label><input id="f-address" data-form="address" value="${esc(state.form.address || '')}" placeholder="Calle, número y colonia" maxlength="500" required ${locked}></div><div class="field wide"><label for="f-reference">Referencias de ubicación</label><input id="f-reference" data-form="reference" value="${esc(state.form.reference || '')}" placeholder="Color de casa, entre calles, referencias…" maxlength="300" ${locked}></div>` : ''}<div class="field wide"><label for="f-notes">Notas de la comanda</label><input id="f-notes" data-form="notes" value="${esc(state.form.notes || '')}" placeholder="Indicaciones para el equipo (opcional)" maxlength="300" ${locked}></div></div></section>`;
}
function productsView() {
  const matches = state.data.products.filter(p => p.available && (state.category === 'all' || p.category === state.category) && `${p.name} ${p.description}`.toLocaleLowerCase().includes(state.search.toLocaleLowerCase()));
  return matches.map(p => `<button class="product-card" data-action="product" data-id="${p.id}" ${currentOrder()?.closed || currentOrder()?.paid > 0 ? 'disabled' : ''}><span class="product-badge">${esc(p.badge || { tacos: 'AL CARBÓN', quesadillas: 'CON QUESO', bebidas: 'BIEN FRÍA', paquetes: 'PARA COMPARTIR' }[p.category])}</span><strong class="product-price">${shortMoney(p.price)}</strong><span class="product-body"><img class="product-photo" src="/assets/${p.category === 'bebidas' ? 'bebida' : p.category === 'quesadillas' ? 'quesadilla' : p.category === 'paquetes' ? 'asada' : 'taco'}.svg" alt="" width="65" height="65"><strong class="product-name">${esc(p.name.toUpperCase())}</strong><span class="product-description">${esc(p.description)}</span></span><span class="product-add">TOCAR PARA AGREGAR ${icon('plus')}</span></button>`).join('') || '<div class="card empty no-results">No hay productos con esa búsqueda.</div>';
}
function ticketView(o) {
  const saved = o?.items || [], total = (o?.total || 0) + cartTotal(), quantity = saved.concat(state.cart).reduce((s, i) => s + i.quantity, 0);
  return `<aside class="ticket" id="order-ticket" tabindex="-1" aria-label="Ticket de la comanda"><div class="ticket-header"><div><h2>${icon('receipt')} ${state.channel === 'salon' ? `TICKET · MESA ${String(state.tableId).padStart(2, '0')}` : `COMANDA ${state.channel === 'llevar' ? 'LLEVAR' : 'DOMICILIO'}`}</h2><small>${o ? `${statuses[o.status]} · ${age(o)}` : 'Pendiente de enviar a cocina'}</small></div><span class="ticket-folio">${o ? '#'+String(o.id).padStart(3, '0') : 'NUEVA'}</span></div>
  ${state.channel !== 'salon' && o ? `<div class="ticket-checks"><strong class="field-label">Checklist de empaque y despacho</strong>${[['label', 'Nombre, folio y destino revisados'], ['extras', 'Salsas, cebollitas y limones'], ['napkins', 'Servilletas, palillos y cubiertos']].map(([key, text]) => `<label class="check-label"><input type="checkbox" data-check="${key}" data-order="${o.id}" ${o.checks[key] ? 'checked' : ''} ${o.closed ? 'disabled' : ''}>${text}</label>`).join('')}</div>` : ''}
  <div class="ticket-items">${saved.map(i => `<div class="ticket-line"><div class="ticket-line-top"><strong>${i.quantity}× ${esc(i.name.toUpperCase())}</strong><span class="pill green">${o.status === 'draft' ? 'GUARDADO' : 'ENVIADO'}</span></div><small>${esc(optionsText(i.options, i.productId))}${i.notes ? ' · '+esc(i.notes) : ''}</small><div class="line-bottom"><b>${money(i.total)}</b>${o.status === 'draft' && !o.paid ? `<button class="icon-btn danger" data-action="delete-item" data-id="${i.id}" aria-label="Quitar ${esc(i.name)}">${icon('trash')}</button>` : ''}</div></div>`).join('')}${state.cart.map((i, index) => `<div class="ticket-line"><div class="ticket-line-top"><strong>${i.quantity}× ${esc(product(i.productId).name.toUpperCase())}</strong><span class="pill amber">NUEVO</span></div><small>${esc(optionsText(i.options, i.productId))}${i.notes ? ' · '+esc(i.notes) : ''}</small><div class="line-bottom"><b>${money(unitPrice(i) * i.quantity)}</b><div class="qty-control"><button class="remove" data-action="cart-minus" data-id="${index}" aria-label="Quitar una unidad">−</button><span>${i.quantity}</span><button data-action="cart-plus" data-id="${index}" aria-label="Agregar una unidad">+</button></div></div></div>`).join('')}${!saved.length && !state.cart.length ? `<div class="empty">${icon('bag')}<strong>La buena orden empieza aquí</strong>Agrega productos del menú.<br>Personaliza cada taco a su gusto.</div>` : ''}</div><div class="ticket-summary"><div class="subtotal"><span>${quantity} productos · sin cargos adicionales</span><span>${money(total)}</span></div>${o?.paid ? `<div class="subtotal"><span>Abonado</span><span>−${money(o.paid)}</span></div>` : ''}<div class="ticket-total"><span>${o?.paid ? 'SALDO' : 'TOTAL A PAGAR'}<small>MXN</small></span><strong>${money(total - (o?.paid || 0))}</strong></div><div class="ticket-actions"><button class="btn primary" data-action="send-order" ${state.busy || (!state.cart.length && o?.status !== 'draft') || o?.closed ? 'disabled' : ''}>${icon('fire')}<span>${state.busy ? 'GUARDANDO…' : 'ENVIAR A COCINA'}<small>${state.cart.length ? 'Confirmar los productos nuevos' : 'Conservar comanda abierta'}</small></span></button></div>${state.channel !== 'salon' && !o?.closed ? `<button style="margin-top:8px" class="btn secondary full" data-action="send-pay" ${!state.cart.length && !o ? 'disabled' : ''}>${icon('cash')} ${o ? 'COBRAR / REGISTRAR PAGO' : 'GUARDAR Y COBRAR'}</button>` : ''}<div class="ticket-actions">${o ? `<button class="btn secondary compact" data-action="precheck" data-id="${o.id}">${icon('print')} Precuenta</button>` : ''}${state.cart.length ? '<button class="btn secondary compact" data-action="clear-cart">Descartar nuevos</button>' : ''}</div>${o && !o.paid && ['draft','kitchen','ready'].includes(o.status) ? '<button class="btn secondary full compact" style="margin-top:12px;color:var(--red)" data-action="cancel-order">Cancelar comanda</button>' : ''}<p class="ticket-note">${state.cart.length ? 'Borrador guardado en este navegador. Cocina lo recibe al confirmar el envío.' : 'Los pedidos confirmados se guardan en esta estación. Impresión mediante el navegador.'}</p></div></aside>`;
}
function boardView(channel) {
  const orders = openOrders(channel), kitchen = orders.filter(o => ['draft', 'kitchen'].includes(o.status)), ready = orders.filter(o => o.status === 'ready'), route = orders.filter(o => ['dispatched', 'delivered'].includes(o.status));
  return `${title(channel ? 'Reparto propio · control de pedidos' : 'Todas las comandas', channel ? 'Cada pedido, hasta su puerta' : 'Una cocina bien coordinada', channel ? 'Prepara, revisa y despacha desde un mismo tablero.' : 'Salón, mostrador y domicilio en una sola vista.', channel ? 'bike' : 'fire', `<button class="btn secondary compact" data-action="history">${icon('receipt')} Historial</button>`)}<div class="delivery-stats"><div class="stat-card"><span>● EN COCINA</span><strong>${kitchen.length}</strong><small>Preparación pendiente</small></div><div class="stat-card"><span>● LISTOS / DESPACHO</span><strong>${ready.length}</strong><small>Revisar antes de entregar</small></div><div class="stat-card"><span>● EN RUTA / ENTREGADOS</span><strong>${route.length}</strong><small>Entrega o pago pendiente</small></div><button class="btn primary" data-action="${channel ? 'new-domicilio' : 'home'}">${icon('plus')} NUEVO PEDIDO<small>${channel ? 'Capturar cliente y dirección' : 'Elegir destino de la comanda'}</small></button></div><div class="board">${boardColumn('COCINA PREPARANDO', 'kitchen', kitchen)}${boardColumn('LISTO PARA DESPACHO', 'ready', ready)}${boardColumn('EN RUTA / POR CERRAR', 'dispatched', route)}</div>`;
}
function boardColumn(label, cls, orders) {
  return `<section class="board-column"><div class="column-heading ${cls}"><span><i class="dot"></i>${label}</span><b>${orders.length}</b></div>${orders.map(boardCard).join('') || `<div class="empty">${icon(cls === 'kitchen' ? 'fire' : cls === 'ready' ? 'box' : 'bike')}<strong>Todo en orden</strong>Sin comandas en esta etapa.</div>`}</section>`;
}
function boardCard(o) {
  const canReady = o.status === 'kitchen', canDeliver = o.status === 'ready' && o.channel === 'llevar', pending = o.remaining > 0;
  return `<article class="delivery-order ${o.status}"><div class="delivery-order-top"><span class="folio-chip">${esc(o.folio)}</span><div><strong>${money(o.total)}</strong><small class="status-mini" style="display:block;margin-top:5px">${pending ? `Saldo ${money(o.remaining)}` : 'PAGADO'}</small></div></div><div class="delivery-meta"><span>${o.channel === 'salon' ? `Mesa ${String(o.tableId).padStart(2,'0')}` : channels[o.channel]}</span><span>${age(o)}</span></div><p class="order-person">${esc(o.customer || (o.channel === 'salon' ? `${o.guests} comensales` : 'Cliente de mostrador'))}</p>${o.address ? `<p class="order-address">${esc(o.address)}${o.reference ? '<br>'+esc(o.reference) : ''}</p>` : ''}<div class="order-list">${o.items.map(i => `<div>${i.quantity}× ${esc(i.name)}<small>${esc(optionsText(i.options, i.productId))}${i.notes ? '<br><strong>Indicación: '+esc(i.notes)+'</strong>' : ''}</small></div>`).join('')}${o.notes ? `<strong>Nota: ${esc(o.notes)}</strong>` : ''}</div>
  ${o.status === 'draft' ? `<button class="btn secondary full compact" data-action="send-saved" data-id="${o.id}">${icon('fire')} ENVIAR A COCINA</button>` : ''}${canReady ? `<button class="btn outline full compact" data-action="status" data-id="${o.id}" data-status="ready">${icon('check')} MARCAR COMO LISTO</button>` : ''}
  ${o.status === 'ready' && o.channel === 'domicilio' ? `<div class="ticket-checks" style="padding:6px 0 10px">${[['label', 'Nombre y dirección verificados'], ['extras', 'Salsas y complementos listos'], ['napkins', 'Servilletas y cubiertos listos']].map(([key,text]) => `<label class="check-label"><input type="checkbox" data-check="${key}" data-order="${o.id}" ${o.checks[key] ? 'checked' : ''}>${text}</label>`).join('')}</div><div class="field"><label for="driver-${o.id}">Asignar repartidor</label><select id="driver-${o.id}" data-driver="${o.id}"><option value="">Selecciona un repartidor</option>${['Miguel López','Mariana Gómez','Luis Ortega','Reparto externo'].map(name => `<option ${o.driver === name ? 'selected' : ''}>${name}</option>`).join('')}</select></div><button class="btn primary full compact" data-action="dispatch" data-id="${o.id}">${icon('bike')} DESPACHAR PEDIDO</button>` : ''}
  ${canDeliver ? `<button class="btn primary full compact" data-action="status" data-id="${o.id}" data-status="delivered">${icon('check')} ${o.channel === 'salon' ? 'ENTREGADO EN MESA' : 'CONFIRMAR RECOGIDA'}</button>` : ''}${o.status === 'dispatched' ? `<div class="pill green" style="margin-bottom:5px">${icon('bike')} ${esc(o.driver)}</div><button class="btn primary full compact" data-action="status" data-id="${o.id}" data-status="delivered">${icon('check')} CONFIRMAR ENTREGA</button>` : ''}${o.phone ? `<a class="btn secondary full compact" href="tel:${esc(o.phone.replace(/[^+\d]/g,''))}">${icon('phone')} Llamar al cliente</a>` : ''}<div style="display:flex;gap:6px;margin-top:7px"><button class="btn secondary compact" style="flex:1" data-action="open-order" data-id="${o.id}">Ver pedido</button>${pending ? `<button class="btn outline compact" style="flex:1" data-action="checkout" data-id="${o.id}">${icon('cash')} Cobrar</button>` : ''}</div></article>`;
}
function startCheckout(id) {
  if (localStorage.getItem('mibuen-payment-pending')) { openBasicModal('Verificar el pago anterior', '<p>Hay un pago enviado cuya respuesta no se confirmó. Primero verifica ese pago para evitar registrar un segundo abono.</p><button class="btn primary full" data-action="recover-payment">Verificar pago pendiente</button>'); return; }
  if (state.cart.length) { toast('Envía o descarta los productos nuevos antes de cobrar.', true); return; }
  const o = orderById(id); if (!o || o.closed || !o.remaining) return toast('Esta comanda ya no tiene saldo pendiente.', true);
  state.orderId = o.id; state.channel = o.channel; state.checkout = { method: 'cash', split: 'full', parts: 2, customAmount: '', tipPercent: 0, received: '', requestId: crypto.randomUUID(), externalConfirmed: false };
  navigate('checkout');
}
function amountDue() {
  const o = currentOrder(), c = state.checkout; if (!o || !c) return 0;
  if (c.split === 'equal') return Math.min(o.remaining, Math.ceil(o.total / Math.max(2, Math.min(20, Number(c.parts) || 2))));
  if (c.split === 'custom') return parseMoney(c.customAmount);
  return o.remaining;
}
function parseMoney(text) { const s = String(text || '').replace(',', '.'); if (!/^\d{1,8}(\.\d{0,2})?$/.test(s)) return 0; const [a,b=''] = s.split('.'); return Number(a) * 100 + Number(b.padEnd(2,'0')); }
function tipAmount() { return Math.round(amountDue() * (state.checkout?.tipPercent || 0) / 100); }
function checkoutView() {
  const o = currentOrder(); if (!o || !state.checkout) return '<div class="empty">Selecciona una comanda para cobrar.<button class="btn primary" data-action="tables">Ver mesas</button></div>';
  const c = state.checkout, label = o.channel === 'salon' ? `MESA ${String(o.tableId).padStart(2,'0')} / SALÓN` : `${channels[o.channel].toUpperCase()} · ${esc(o.customer || 'MOSTRADOR')}`;
  return `${title(`Cuenta abierta · ${esc(o.folio)}`, `COBRAR ${label}`, 'Revisa el importe y registra el pago de prueba.', 'cash', `<div class="checkout-summary"><div><span>SALDO A PAGAR (SIN PROPINA)</span><strong>${money(o.remaining)}</strong></div><small>Total<br>${money(o.total)}</small></div>`)}<div class="checkout-layout"><section class="card checkout-card"><div class="section-heading"><h2>${icon('people')} DIVIDIR / ABONAR A LA CUENTA</h2><small>${o.channel === 'salon' ? `Mesa ${o.tableId} · ${o.guests} comensales` : esc(o.folio)}</small></div><div class="choice-row">${[['full','CUENTA COMPLETA','Pagar el saldo pendiente'],['equal','PARTES IGUALES','Abono de una parte'],['custom','OTRO IMPORTE','Abono personalizado']].map(([key,label,sub]) => `<button class="choice dark ${c.split === key ? 'active' : ''}" data-action="split" data-id="${key}">${label}<small>${sub}</small></button>`).join('')}</div>${c.split === 'equal' ? `<div class="split-row"><label for="split-parts">Dividir la cuenta entre</label><input id="split-parts" type="number" min="2" max="20" value="${c.parts}"><strong>partes</strong></div><p class="tip-note">Este pago abona una parte de la cuenta original. El último pago ajusta el saldo restante y el redondeo.</p>` : c.split === 'custom' ? `<div class="field"><label for="custom-amount">Importe a abonar (MXN)</label><input id="custom-amount" type="text" inputmode="decimal" placeholder="0.00" value="${esc(c.customAmount)}"></div>` : ''}<div class="tip-section"><div class="section-heading"><h2>${icon('shield')} PROPINA / SUGERIDA</h2><small>Voluntaria · se registra por separado</small></div><div class="tip-options">${[0,10,15,20].map(n => `<button class="tip-option ${c.tipPercent === n ? 'active' : ''}" data-action="tip" data-id="${n}">${n}%<small>${n ? `Sobre este abono` : 'Sin propina'}</small></button>`).join('')}</div><p class="tip-note">La propina no cambia el precio de los productos. No se añade por defecto.</p></div><div class="checkout-items">${o.items.map(i => `<div class="checkout-item"><span>${i.quantity}× ${esc(i.name)}</span><strong>${money(i.total)}</strong></div>`).join('')}</div>${o.payments.length ? `<div class="payment-history"><strong>Pagos anteriores · ${money(o.paid)}</strong>${o.payments.map(p => `<div>${esc(p.method)} · ${money(p.amount)}${p.tip ? ' + propina '+money(p.tip) : ''}</div>`).join('')}</div>` : ''}</section><section class="card checkout-card"><div class="section-heading"><h2>MÉTODO DE PAGO</h2><small>Registro manual de práctica</small></div><div class="choice-row">${[['cash','EFECTIVO','cash'],['card','TARJETA','card'],['transfer','TRANSFERENCIA','transfer']].map(([key,name,ico]) => `<button class="choice ${c.method === key ? 'active' : ''}" data-action="method" data-id="${key}">${icon(ico)} ${name}</button>`).join('')}</div><div id="payment-breakdown"></div>${c.method === 'cash' ? `<div class="field-label">Billetes / montos recibidos</div><div class="denominations">${[100000,50000,20000].map(n => `<button data-action="received" data-id="${n}">${shortMoney(n)}</button>`).join('')}<button data-action="exact">EXACTO</button></div><div class="received-input"><label for="received">Efectivo recibido (MXN)</label><input id="received" type="text" inputmode="decimal" autocomplete="off" value="${esc(c.received)}" placeholder="0.00"></div><div class="keypad" aria-label="Teclado para capturar el efectivo">${['1','2','3','+100','4','5','6','+50','7','8','9','C','.','0','00','⌫'].map(key => `<button data-action="keypad" data-id="${key}" ${key === 'C' ? 'class="clear"' : ''} aria-label="${key === '⌫' ? 'Borrar último dígito' : key === 'C' ? 'Limpiar importe' : key}">${key}</button>`).join('')}</div>` : `<div class="warning-bar">Solo registra una operación de prueba. Esta aplicación no procesa tarjetas ni verifica transferencias bancarias.</div><label class="check-label"><input id="external-confirmed" type="checkbox" ${c.externalConfirmed ? 'checked' : ''}>Confirmo el registro manual de este pago de prueba.</label>`}<div class="payment-confirm"><button class="btn secondary" data-action="precheck" data-id="${o.id}">${icon('print')} Precuenta</button><button class="btn primary" id="confirm-payment" data-action="pay" ${state.busy ? 'disabled' : ''}>${icon('cash')} ${state.busy ? 'REGISTRANDO…' : 'CONFIRMAR PAGO'}</button></div><button class="btn secondary full compact" style="margin-top:10px" data-action="open-order" data-id="${o.id}">${icon('arrow')} Regresar a la comanda</button><p class="tip-note" id="payment-hint"></p></section></div>`;
}
function updatePaymentDisplay() {
  if (state.page !== 'checkout' || !state.checkout || !currentOrder()) return;
  const amount = amountDue(), tip = tipAmount(), total = amount+tip, received = parseMoney(state.checkout.received), cash = state.checkout.method === 'cash';
  const valid = amount > 0 && amount <= currentOrder().remaining && (cash ? received >= total : state.checkout.externalConfirmed);
  const root = $('#payment-breakdown'); if (!root) return;
  root.innerHTML = `<div class="payment-change ${cash && received < total ? 'insufficient' : ''}"><div><span>${cash ? received >= total ? 'CAMBIO A ENTREGAR' : 'EFECTIVO FALTANTE' : 'TOTAL DE ESTE PAGO'}</span><strong>${money(cash ? Math.abs(received - total) : total)}</strong></div><small>Cuenta: ${money(amount)}<br>Propina: ${money(tip)}<br><b>A recibir: ${money(total)}</b></small></div>`;
  $('#confirm-payment').disabled = !valid || state.busy;
  $('#payment-hint').textContent = amount <= 0 || amount > currentOrder().remaining ? 'Indica un abono mayor a cero y no superior al saldo.' : cash && received < total ? `Faltan ${money(total-received)} para confirmar el pago.` : !cash && !state.checkout.externalConfirmed ? 'Confirma el registro manual antes de continuar.' : `Después de este pago quedará un saldo de ${money(currentOrder().remaining-amount)}.`;
}
function historyView() {
  const orders = state.data.orders, payments = orders.flatMap(o => o.payments), sales = payments.reduce((s,p) => s+p.amount,0), tips=payments.reduce((s,p)=>s+p.tip,0);
  return `${title('Historial / caja · práctica local', 'Cada cuenta tiene su historia', 'Consulta pagos registrados y vuelve a imprimir tickets.', 'receipt', '<button class="btn secondary compact" data-action="home">'+icon('arrow')+' Nueva orden</button>')}<div class="delivery-stats" style="grid-template-columns:repeat(3,1fr)"><div class="stat-card"><span>VENTAS COBRADAS</span><strong>${money(sales)}</strong><small>Abonos y liquidaciones · sin propinas</small></div><div class="stat-card"><span>PROPINAS REGISTRADAS</span><strong>${money(tips)}</strong><small>Separadas del ingreso por productos</small></div><div class="stat-card"><span>COMANDAS CERRADAS</span><strong>${orders.filter(o=>o.closed).length}</strong><small>${orders.length} comandas en esta estación</small></div></div><section class="card history-wrap"><table class="history-table"><thead><tr><th>Folio</th><th>Destino / cliente</th><th>Estado</th><th>Cuenta</th><th>Saldo</th><th>Acciones</th></tr></thead><tbody>${orders.map(o=>`<tr><td><strong>${esc(o.folio)}</strong></td><td>${o.channel === 'salon' ? `Mesa ${o.tableId}` : channels[o.channel]}<br><small class="muted">${esc(o.customer)}</small></td><td><span class="pill ${o.closed?'green':'amber'}">${statuses[o.status]}</span></td><td>${money(o.total)}</td><td>${money(o.remaining)}</td><td><button class="btn secondary compact" data-action="receipt" data-id="${o.id}">${icon('print')} Ticket</button> ${!o.closed ? `<button class="btn outline compact" data-action="open-order" data-id="${o.id}">Abrir</button>`:''}</td></tr>`).join('')}</tbody></table></section><p class="tip-note">Resumen de todos los registros de la demo. No es un corte fiscal ni un arqueo de efectivo; no se configura saldo inicial ni retiros.</p>`;
}
function openModal(content) {
  modalTrigger = document.activeElement;
  $('#modal-root').innerHTML = `<div class="modal-backdrop"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="dialog-title" tabindex="-1">${content}</section></div>`;
  document.body.style.overflow = 'hidden';
  requestAnimationFrame(()=> { const el = $('.modal input, .modal button, .modal'); el?.focus(); });
}
function openBasicModal(heading, content) { state.modal = {type:'basic'}; openModal(`<div class="modal-basic"><div class="section-heading"><h2 id="dialog-title">${heading}</h2><button class="icon-btn no-print" data-action="close-modal" aria-label="Cerrar diálogo">${icon('close')}</button></div>${content}</div>`); }
function closeModal() { $('#modal-root').innerHTML=''; document.body.style.overflow=''; state.modal=null; if(modalTrigger?.isConnected) modalTrigger.focus(); modalTrigger=null; }
function customize(id) {
  const p=product(id); if(!p) return;
  if(!p.customizable){ addCart({productId:p.id,quantity:1,options:defaultOptions(),notes:''}); return; }
  state.modal={type:'product',productId:p.id,quantity:1,options:defaultOptions(),notes:''};
  openModal(customizeHTML());
}
function customizeHTML() {
  const m=state.modal,p=product(m.productId),o=m.options,total=(p.price+(o.tortilla==='harina'?500:0))*m.quantity;
  return `<header class="modal-header"><span class="title-icon">${icon('food')}</span><div><h2 id="dialog-title">PERSONALIZAR: ${esc(p.name.toUpperCase())}</h2><p><strong>${money(p.price)} c/u</strong> · Recién hecho, a tu gusto</p></div><button class="icon-btn" data-action="close-modal" aria-label="Cerrar personalización">${icon('close')}</button></header><div class="modal-body"><div class="quantity-panel"><div><strong>CANTIDAD DE PRODUCTOS</strong><small>Elige cuántas piezas deseas preparar</small></div><div class="large-qty"><button data-action="modal-qty" data-id="-1" aria-label="Reducir cantidad" ${m.quantity===1?'disabled':''}>−</button><span aria-live="polite">${m.quantity}<small>PIEZAS</small></span><button data-action="modal-qty" data-id="1" aria-label="Aumentar cantidad" ${m.quantity===99?'disabled':''}>+</button></div></div><section class="modal-section"><div class="modal-section-title"><strong>PREPARACIÓN & VERDURA</strong><small>Toca las opciones para cambiar</small></div><div class="custom-options"><button class="custom-option ${o.onion&&o.cilantro&&o.salsa?'active':''}" data-action="option" data-id="all" aria-pressed="${o.onion&&o.cilantro&&o.salsa}">${icon('check')} CON TODO<span class="tag">Cebolla, cilantro<br>y salsa</span></button>${[['onion','SIN CEBOLLA'],['salsa','SIN SALSA']].map(([key,label])=>`<button class="custom-option ${!o[key]?'active':''}" data-action="option" data-id="${key}" aria-pressed="${!o[key]}">${icon(!o[key]?'check':'minus')} ${label}<span class="tag">${!o[key]?'SÍ':'NO'}</span></button>`).join('')}</div><label class="check-label" style="margin-top:12px"><input type="checkbox" id="no-cilantro" ${!o.cilantro?'checked':''}>Sin cilantro</label></section><section class="modal-section"><div class="modal-section-title"><strong>TIPO DE TORTILLA</strong><small>Selecciona una opción obligatoria</small></div><div class="custom-options" role="group" aria-label="Tipo de tortilla">${[['maiz','TORTILLA MAÍZ','INCLUIDO'],['harina','TORTILLA HARINA','+ $5.00 / pza']].map(([key,label,tag])=>`<button class="custom-option ${o.tortilla===key?'active':''}" data-action="tortilla" data-id="${key}" aria-pressed="${o.tortilla===key}">${icon(o.tortilla===key?'check':'food')} ${label}<span class="tag">${tag}</span></button>`).join('')}</div></section><div class="salsa-row"><div><strong>SALSA ROJA / VERDE APARTE</strong><small>Servir en contenedor separado, sin costo extra.</small></div><label class="check-label"><input id="salsa-aside" type="checkbox" ${o.salsaAside?'checked':''}>SALSA APARTE</label></div><div class="field" style="margin-top:17px"><label for="product-notes">Indicación especial (opcional)</label><input id="product-notes" maxlength="300" placeholder="Ej. bien dorado" value="${esc(m.notes)}"></div></div><footer class="modal-footer"><button class="btn secondary" data-action="close-modal">${icon('close')} CANCELAR</button><button class="btn primary" data-action="add-custom">${icon('bag')}<span>AGREGAR A LA ORDEN<small>${m.quantity}× ${esc(p.name)}</small></span><strong>${money(total)}</strong></button></footer>`;
}
function rerenderCustomization() {
  const action=document.activeElement?.dataset.action,id=document.activeElement?.dataset.id,elementId=document.activeElement?.id;
  $('.modal').innerHTML=customizeHTML();
  if(action) Array.from($('.modal').querySelectorAll('button')).find(el=>el.dataset.action===action&&el.dataset.id===id)?.focus();
  else if(elementId) document.getElementById(elementId)?.focus();
}
function addCart(line) {
  const match=state.cart.find(l=>l.productId===line.productId&&JSON.stringify(l.options)===JSON.stringify(line.options)&&l.notes===line.notes);
  if(match){ if(match.quantity+line.quantity>99)return toast('Máximo 99 piezas por línea.',true);match.quantity+=line.quantity; }
  else state.cart.push(line);
  activeRequest=null;saveDraft();closeModal();render();toast('Producto agregado a la comanda.');
}
async function sendOrder(goPay=false) {
  if(state.busy)return;
  let pending;
  try { pending=JSON.parse(localStorage.getItem('mibuen-order-pending')||'null'); } catch {toast('No se pudo leer el envío pendiente.',true);return;}
  if(!pending&&!state.cart.length&&currentOrder()?.status!=='draft'){ if(goPay&&currentOrder())return startCheckout(state.orderId);return; }
  if(!pending&&state.channel==='domicilio'){
    for(const key of ['customer','phone','address'])if(!state.form[key]?.trim()){ toast('Completa nombre, teléfono y dirección antes de enviar.',true);$(`#f-${key}`)?.focus();return; }
  }
  if(!pending&&state.channel==='salon'&&(!Number.isInteger(Number(state.form.guests))||state.form.guests<1||state.form.guests>20)){toast('Indica de 1 a 20 comensales.',true);$('#guests')?.focus();return;}
  if(!pending){
    activeRequest ||= crypto.randomUUID();saveDraft();
    const existing=currentOrder(),body=state.cart.length?(existing?{items:state.cart,requestId:activeRequest}:{...state.form,channel:state.channel,tableId:state.tableId,guests:Number(state.form.guests||1),items:state.cart,requestId:activeRequest}):{requestId:activeRequest};
    pending={path:state.cart.length?(existing?`/api/orders/${existing.id}/items`:'/api/orders'):`/api/orders/${existing.id}/send`,body};
    try{localStorage.setItem('mibuen-order-pending',JSON.stringify(pending));}catch{toast('No se pudo proteger el envío en este navegador. Revisa sus permisos de almacenamiento.',true);return;}
  }
  state.busy=true; render();
  let confirmed=false;
  try{
    let o=(await api(pending.path,'POST',pending.body)).order;confirmed=true;
    if(o.status==='draft')o=(await api(`/api/orders/${o.id}/send`,'POST',{requestId:'send-'+pending.body.requestId})).order;
    syncOrder(o);state.orderId=o.id;state.channel=o.channel;state.tableId=o.tableId;state.form={customer:o.customer,phone:o.phone,address:o.address,reference:o.reference,notes:o.notes,guests:o.guests};state.cart=[];activeRequest=null;localStorage.removeItem('mibuen-order-pending');saveDraft();
    state.restored=false;toast('Comanda guardada y enviada a cocina.');
    if(goPay){state.busy=false;startCheckout(o.id);}else navigate('order');
  }catch(error){
    if(error.status&&!confirmed)localStorage.removeItem('mibuen-order-pending');
    toast(error.message,true);
    if(localStorage.getItem('mibuen-order-pending'))openBasicModal('Verifica el envío a cocina','<p>La conexión no confirmó el envío. Conservamos exactamente los productos y su folio. Primero verifica esta comanda antes de cambiar productos o abrir otra cuenta.</p><button class="btn primary full" data-action="recover-order">Verificar envío sin duplicarlo</button>');
    else if(error.details?.orderId){await refresh();toast('La mesa ya tiene una cuenta activa. Conservamos tu borrador; abre su cuenta para revisarla.',true);}
  }
  finally{state.busy=false;render();}
}
async function mutate(path,method,body,message){
  if(state.busy)return null;state.busy=true;
  document.querySelectorAll('button[data-action="pay"],button[data-action="dispatch"],button[data-action="status"]').forEach(b=>b.disabled=true);
  try{const result=await api(path,method,body);if(result.order)syncOrder(result.order);toast(message);return result;}
  catch(error){toast(error.message,true);return null;}finally{state.busy=false;render();}
}
async function pay(){
  if(state.busy||$('#confirm-payment')?.disabled)return;
  if(localStorage.getItem('mibuen-payment-pending')){await recoverPayment();return;}
  const o=currentOrder(),c=state.checkout,amount=amountDue(),tip=tipAmount();
  const payload={amount,tip,method:c.method,received:c.method==='cash'?parseMoney(c.received):amount+tip,requestId:c.requestId};
  try { localStorage.setItem('mibuen-payment-pending',JSON.stringify({orderId:o.id,payload})); }
  catch { toast('No se pudo proteger el registro de pago en este navegador. Revisa sus permisos de almacenamiento.',true);return; }
  await recoverPayment();
}
async function recoverPayment(){
  if(state.busy)return;
  let pending;
  try { pending=JSON.parse(localStorage.getItem('mibuen-payment-pending')||'null'); } catch { toast('No se pudo leer el pago pendiente.',true);return; }
  if(!pending)return;
  state.busy=true;render();
  try{
    const result=await api(`/api/orders/${Number(pending.orderId)}/payments`,'POST',pending.payload);
    syncOrder(result.order);localStorage.removeItem('mibuen-payment-pending');state.checkout=null;closeModal();
    navigate(result.order.closed?'tables':'history');showReceipt(result.order.id,result.payment);toast('Pago confirmado y guardado.');
  }catch(error){
    if(error.status){localStorage.removeItem('mibuen-payment-pending');toast(error.message,true);}
    else {toast('Pago sin respuesta confirmada. Conservamos su folio para verificarlo sin duplicarlo.',true);openBasicModal('Pago pendiente de confirmar','<p>No registres otro abono. Cuando vuelva la conexión, verifica este mismo pago; el sistema conservará su identificador para evitar duplicados.</p><button class="btn primary full" data-action="recover-payment">Verificar este pago</button>');}
  }finally{state.busy=false;render();}
}
function receiptHTML(o,payment,precheck=false){
  const tips=o.payments.reduce((s,p)=>s+p.tip,0);
  return `<div class="receipt"><h2>MI BUEN<br>ASADA POS</h2><p>Comprobante de práctica<br>${precheck?'PRECUENTA · SIN VALOR DE PAGO':'DEMO · SIN VALOR FISCAL'}<br>${esc(o.folio)} · ${o.channel==='salon'?`Mesa ${o.tableId}`:channels[o.channel]}<br>${new Date(o.updatedAt).toLocaleString('es-MX')}</p><hr>${o.items.map(i=>`<div class="receipt-line"><span>${i.quantity}× ${esc(i.name)}</span><span>${money(i.total)}</span></div><small>${esc(optionsText(i.options,i.productId))}${i.notes?' · '+esc(i.notes):''}</small>`).join('')}<hr><div class="receipt-line receipt-total"><span>CUENTA</span><span>${money(o.total)}</span></div><div class="receipt-line"><span>Abonado</span><span>${money(o.paid)}</span></div><div class="receipt-line"><span>Saldo</span><span>${money(o.remaining)}</span></div><div class="receipt-line"><span>Propinas acumuladas</span><span>${money(tips)}</span></div>${payment?`<hr><div class="receipt-line"><span>Este pago (${esc(payment.method)})</span><span>${money(payment.amount)}</span></div><div class="receipt-line"><span>Propina de este pago</span><span>${money(payment.tip)}</span></div>${payment.method==='cash'?`<div class="receipt-line"><span>Efectivo recibido</span><span>${money(payment.received)}</span></div><div class="receipt-line"><span>Cambio</span><span>${money(payment.change)}</span></div>`:''}`:''}<hr><p>Gracias por tu visita.<br>¡Nos vemos en la siguiente orden!</p></div>`;
}
function showReceipt(id,payment,precheck=false){const o=orderById(id);if(!o)return;openBasicModal(precheck?'Precuenta':'Comprobante de práctica',`${receiptHTML(o,payment,precheck)}<button class="btn primary full no-print" data-action="print">${icon('print')} IMPRIMIR / GUARDAR PDF</button><button class="btn secondary full no-print" data-action="close-modal">Cerrar</button>`);}
function about(){openBasicModal('Mi Buen · guía de práctica',`<p><strong>1.</strong> Elige mesa, mostrador o domicilio. Agrega productos y personalízalos antes de enviar a cocina.</p><p><strong>2.</strong> En Comandas puedes marcar preparación, revisar empaque y confirmar entregas. Domicilio requiere datos de entrega y repartidor.</p><p><strong>3.</strong> Cobra la cuenta completa o registra abonos. La mesa permanece ocupada hasta liquidarse. El ticket se imprime con el navegador.</p><p>Los pedidos se guardan en SQLite en esta estación; los productos sin enviar son un borrador de este navegador. Los pagos son registros de prueba, sin terminal bancaria ni WhatsApp integrado. Usa datos ficticios.</p><button class="btn primary full" data-action="close-modal">ENTENDIDO</button><button class="btn secondary full" data-action="reset-demo">Reiniciar datos de muestra</button>`);}
function editCustomer(){
  const o=currentOrder();if(!o||o.closed)return;
  const fields=[['customer','Nombre del cliente'],['phone','Teléfono'],...(o.channel==='domicilio'?[['address','Dirección completa'],['reference','Referencias de ubicación']]:[]),['notes','Notas de la comanda']];
  openBasicModal('Corregir datos del pedido',`<p>Actualiza los datos y confirma para que el equipo vea la corrección.</p><div class="form-grid">${fields.map(([key,label])=>`<div class="field ${key==='address'||key==='notes'?'wide':''}"><label for="edit-${key}">${label}</label><input id="edit-${key}" data-edit="${key}" maxlength="${key==='address'?500:300}" value="${esc(o[key])}" ${key==='phone'?'type="tel"':''}></div>`).join('')}</div><button class="btn primary full" data-action="save-customer">Guardar corrección</button><button class="btn secondary full" data-action="close-modal">Conservar datos anteriores</button>`);
}

document.addEventListener('click',async event=>{
  const button=event.target.closest('[data-action]');if(!button||button.disabled)return;
  event.preventDefault();const action=button.dataset.action,id=button.dataset.id;
  if(state.busy&&!['close-modal','print'].includes(action))return;
  if(['product','add-custom','cart-plus','cart-minus','clear-cart','new-llevar','new-domicilio','table','open-order','checkout','reset-demo','cancel-order','edit-customer'].includes(action)&&localStorage.getItem('mibuen-order-pending')){openBasicModal('Primero verifica el envío','<p>Hay una comanda esperando confirmación. Verifica su envío para continuar sin duplicar productos.</p><button class="btn primary full" data-action="recover-order">Verificar envío pendiente</button>');return;}
  if(['method','split','tip','received','exact','keypad'].includes(action)&&localStorage.getItem('mibuen-payment-pending')){openBasicModal('Primero verifica el pago anterior','<p>El pago anterior todavía espera confirmación. Su importe y folio se conservan para evitar un cobro duplicado.</p><button class="btn primary full" data-action="recover-payment">Verificar pago pendiente</button>');return;}
  switch(action){
    case 'home':navigate('home');break;
    case 'show-ticket':$('#order-ticket')?.scrollIntoView({behavior:'smooth',block:'start'});$('#order-ticket')?.focus({preventScroll:true});break;
    case 'show-menu':$('#order-menu')?.scrollIntoView({behavior:'smooth',block:'start'});break;
    case 'tables':navigate('tables');break;
    case 'new-llevar':newOrder('llevar');break;
    case 'new-domicilio':newOrder('domicilio');break;
    case 'delivery':navigate('delivery');break;
    case 'kitchen':navigate('kitchen');break;
    case 'history':navigate('history');break;
    case 'table':newOrder('salon',Number(id));break;
    case 'open-order':openOrder(id);break;
    case 'resume-draft':closeModal();navigate('order');break;
    case 'category':state.category=id;render();break;
    case 'product':customize(Number(id));break;
    case 'close-modal':closeModal();break;
    case 'modal-qty':state.modal.quantity=Math.max(1,Math.min(99,state.modal.quantity+Number(id)));rerenderCustomization();break;
    case 'option':if(id==='all')Object.assign(state.modal.options,{onion:true,cilantro:true,salsa:true});else state.modal.options[id]=!state.modal.options[id];rerenderCustomization();break;
    case 'tortilla':state.modal.options.tortilla=id;rerenderCustomization();break;
    case 'add-custom':addCart({productId:state.modal.productId,quantity:state.modal.quantity,options:{...state.modal.options},notes:state.modal.notes});break;
    case 'cart-plus':if(state.cart[id].quantity<99)state.cart[id].quantity++;saveDraft();activeRequest=null;render();break;
    case 'cart-minus':if(--state.cart[id].quantity<1)state.cart.splice(Number(id),1);saveDraft();activeRequest=null;render();break;
    case 'clear-cart':openBasicModal('Descartar productos nuevos',`<p>Se quitarán ${state.cart.length} líneas del borrador. Los productos ya enviados a cocina se conservan.</p><button class="btn danger full" id="confirm-clear">Descartar nuevos</button><button class="btn secondary full" data-action="close-modal">Volver al pedido</button>`);$('#confirm-clear').onclick=()=>{state.cart=[];activeRequest=null;saveDraft();closeModal();render();};break;
    case 'edit-customer':editCustomer();break;
    case 'save-customer':{const body=Object.fromEntries(Array.from(document.querySelectorAll('[data-edit]')).map(el=>[el.dataset.edit,el.value]));const result=await mutate(`/api/orders/${state.orderId}`,'PATCH',body,'Datos del pedido corregidos.');if(result){Object.assign(state.form,body);saveDraft();closeModal();render();}break;}
    case 'cancel-order':{const o=currentOrder();openBasicModal('¿Cancelar esta comanda?',`<p>Se cancelará <strong>${esc(o.folio)}</strong> y se liberará la mesa si corresponde. La comanda conservará su registro en el historial. No se permite cancelar cuentas con pagos.</p><button class="btn danger full" data-action="confirm-cancel">Sí, cancelar comanda</button><button class="btn secondary full" data-action="close-modal">Conservar comanda</button>`);break;}
    case 'confirm-cancel':{const result=await mutate(`/api/orders/${state.orderId}`,'PATCH',{status:'cancelled'},'Comanda cancelada.');if(result){state.cart=[];saveDraft();closeModal();navigate('history');}break;}
    case 'send-order':await sendOrder(false);break;
    case 'recover-order':closeModal();await sendOrder(false);break;
    case 'send-pay':if(state.cart.length||currentOrder()?.status==='draft')await sendOrder(true);else startCheckout(state.orderId);break;
    case 'send-saved':await mutate(`/api/orders/${id}/send`,'POST',{},'Pedido enviado a cocina.');break;
    case 'delete-item':if(currentOrder().items.length===1){toast('El pedido debe conservar al menos un producto.',true);break;}await mutate(`/api/orders/${state.orderId}/items/${id}`,'DELETE',{},'Producto retirado del borrador.');break;
    case 'status':await mutate(`/api/orders/${id}`,'PATCH',{status:button.dataset.status},'Estado de la comanda actualizado.');break;
    case 'dispatch':{const o=orderById(id),driver=$(`#driver-${id}`)?.value;if(!driver){toast('Selecciona un repartidor antes de despachar.',true);$(`#driver-${id}`)?.focus();break;}if(!Object.values(o.checks).every(Boolean)){toast('Completa las tres revisiones de empaque antes de despachar.',true);break;}await mutate(`/api/orders/${id}`,'PATCH',{status:'dispatched',driver},'Pedido despachado con su repartidor.');break;}
    case 'checkout':startCheckout(id);break;
    case 'method':state.checkout.method=id;state.checkout.externalConfirmed=false;state.checkout.requestId=crypto.randomUUID();render();break;
    case 'split':state.checkout.split=id;state.checkout.received='';state.checkout.requestId=crypto.randomUUID();render();break;
    case 'tip':state.checkout.tipPercent=Number(id);state.checkout.requestId=crypto.randomUUID();render();break;
    case 'received':state.checkout.received=(Number(id)/100).toFixed(2);$('#received').value=state.checkout.received;updatePaymentDisplay();break;
    case 'exact':state.checkout.received=((amountDue()+tipAmount())/100).toFixed(2);$('#received').value=state.checkout.received;updatePaymentDisplay();break;
    case 'keypad':{let text=state.checkout.received;if(id==='C')text='';else if(id==='⌫')text=text.slice(0,-1);else if(id[0]==='+')text=((parseMoney(text)+Number(id)*100)/100).toFixed(2);else if(id==='.'&&!text.includes('.'))text=(text||'0')+'.';else if(id!=='.'&&text.length<11)text+=id;if(text===''||/^\d{0,8}(\.\d{0,2})?$/.test(text))state.checkout.received=text;$('#received').value=state.checkout.received;updatePaymentDisplay();break;}
    case 'pay':await pay();break;
    case 'recover-payment':closeModal();await recoverPayment();break;
    case 'precheck':if(state.cart.length){toast('Envía los productos nuevos para incluirlos en la precuenta.',true);break;}showReceipt(id,null,true);break;
    case 'receipt':showReceipt(id);break;
    case 'print':window.print();break;
    case 'about':about();break;
    case 'reset-demo':openBasicModal('Reiniciar la práctica',`<p>Se eliminarán los pedidos y pagos de esta demo y se cargarán ejemplos nuevos. Esta acción solo afecta a esta estación de práctica.</p><div class="field"><label for="reset-confirm">Escribe REINICIAR DEMO para continuar</label><input id="reset-confirm" autocomplete="off"></div><button class="btn danger full" id="reset-now">Reiniciar datos</button><button class="btn secondary full" data-action="close-modal">Conservar datos</button>`);$('#reset-now').onclick=async()=>{if($('#reset-confirm').value!=='REINICIAR DEMO')return toast('Escribe REINICIAR DEMO exactamente.',true);const result=await mutate('/api/demo/reset','POST',{confirm:'REINICIAR DEMO'},'Datos de muestra restaurados.');if(result){state.cart=[];state.orderId=null;state.checkout=null;saveDraft();await refresh();closeModal();navigate('home');}};break;
  }
});
document.addEventListener('input',event=>{
  const el=event.target;
  if(el.dataset.form){state.form[el.dataset.form]=el.value;saveDraft();}
  if(el.id==='product-search'){state.search=el.value;$('#products-grid').innerHTML=productsView();}
  if(el.id==='product-notes'&&state.modal?.type==='product')state.modal.notes=el.value;
  if(el.id==='received'){state.checkout.received=el.value;updatePaymentDisplay();}
  if(el.id==='custom-amount'){state.checkout.customAmount=el.value;state.checkout.requestId=crypto.randomUUID();updatePaymentDisplay();}
  if(el.id==='split-parts'){state.checkout.parts=Number(el.value);state.checkout.requestId=crypto.randomUUID();updatePaymentDisplay();}
});
document.addEventListener('change',async event=>{
  const el=event.target;
  if(el.id==='no-cilantro'){state.modal.options.cilantro=!el.checked;rerenderCustomization();}
  if(el.id==='salsa-aside')state.modal.options.salsaAside=el.checked;
  if(el.id==='external-confirmed'){state.checkout.externalConfirmed=el.checked;updatePaymentDisplay();}
  if(el.dataset.check){const o=orderById(el.dataset.order);if(state.busy){el.checked=!el.checked;return;}await mutate(`/api/orders/${o.id}`,'PATCH',{checks:{...o.checks,[el.dataset.check]:el.checked}},'Revisión de empaque guardada.');}
  if(el.dataset.driver){const o=orderById(el.dataset.driver);if(state.busy)return;await mutate(`/api/orders/${o.id}`,'PATCH',{driver:el.value},'Repartidor asignado.');}
});
document.addEventListener('keydown',event=>{
  if(!$('.modal'))return;
  if(event.key==='Escape'){event.preventDefault();closeModal();return;}
  if(event.key==='Tab'){const focusable=Array.from($('.modal').querySelectorAll('button:not(:disabled),input:not(:disabled),a[href],select,textarea,[tabindex="0"]'));const first=focusable[0],last=focusable.at(-1);if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}}
});
window.addEventListener('popstate',()=>{if(state.busy)return;closeModal();if(state.cart.length){state.page='order';history.replaceState(null,'',`#order${state.orderId?'?id='+state.orderId:''}`);render();toast('Tu pedido por enviar sigue aquí. Envía o descarta los productos antes de abrir otra cuenta.');return;}routeFromHash();render();});
window.addEventListener('beforeunload',()=>{if(state.data)saveDraft();});
function routeFromHash(){const [page,query]=location.hash.slice(1).split('?'),id=new URLSearchParams(query||'').get('id');if(page==='order'&&state.cart.length){state.page='order';history.replaceState(null,'',`#order${state.orderId?'?id='+state.orderId:''}`);return;}if(['home','tables','delivery','kitchen','history'].includes(page))state.page=page;else if(page==='order'&&id&&orderById(id)){const o=orderById(id);Object.assign(state,{page:'order',orderId:o.id,channel:o.channel,tableId:o.tableId,form:{customer:o.customer,phone:o.phone,address:o.address,reference:o.reference,notes:o.notes,guests:o.guests}});}else state.page='home';}
async function init(){try{await refresh();restoreDraft();routeFromHash();render();if(localStorage.getItem('mibuen-order-pending'))await sendOrder();if(localStorage.getItem('mibuen-payment-pending'))await recoverPayment();}catch(error){$('#app').innerHTML=`<main class="loading"><span class="brand-mark">A</span><h1>No pudimos abrir la estación</h1><p>${esc(error.message)}</p><button class="btn primary" id="reload-station">Volver a intentar</button></main>`;$('#reload-station').onclick=()=>location.reload();}}
setInterval(()=>{const clock=$('#clock');if(clock)clock.textContent=new Date().toLocaleTimeString('es-MX',{hour:'2-digit',minute:'2-digit'});},15000);
// Poll only quiet overview screens; never replace an in-progress form or modal.
setInterval(async()=>{if(state.data&&!state.busy&&!state.modal&&['home','tables','kitchen','delivery','history'].includes(state.page)){try{await refresh(true);}catch{state.online=false;render();}}},10000);
init();

/* Pruebas de interfaz reales, con SQLite y servidor temporales. Ver README. */
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'mi-buen-ui-'));
const base = 'http://127.0.0.1:18788';
const child = spawn(process.env.PYTHON || 'python3', [path.join(root, 'server.py'), '--port', '18788', '--db', path.join(scratch, 'ui.sqlite3')], { stdio: ['ignore', 'pipe', 'pipe'] });
const shots = path.join(root, 'capturas'); fs.mkdirSync(shots, { recursive: true });
let browser;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
async function boot() { for (let i=0;i<50;i++) { try { if ((await fetch(base+'/api/bootstrap')).ok) return; } catch {} await delay(100); } throw new Error('No inició el servidor temporal.'); }
async function snapshot(page, name) { await page.screenshot({ path: path.join(shots, name+'.png'), fullPage: true, style:'#toast{visibility:hidden!important}' }); }
async function bootstrap() { return (await fetch(base+'/api/bootstrap')).json(); }
async function waitMutation(page, callback, suffix) { const response=page.waitForResponse(r=>r.url().includes(suffix)&&r.request().method()!=='GET'); await callback(); const r=await response; assert(r.ok(), await r.text()); await page.waitForTimeout(100); }
async function click(page, action, id) { await page.locator(`[data-action="${action}"]${id===undefined?'':`[data-id="${id}"]`}`).first().click(); }
async function assertNoOverflow(page, name) { assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1), 'Desbordamiento horizontal: '+name); }
async function run() {
  await boot();
  browser=await chromium.launch({headless:true, ...(process.env.BROWSER_PATH?{executablePath:process.env.BROWSER_PATH}:{}), args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:1440,height:1000}});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base);await page.waitForSelector('.destination-grid');await assertNoOverflow(page,'inicio escritorio');await snapshot(page,'01-inicio-escritorio');
  assert.equal(await page.locator('.destination-card').count(),3);
  await click(page,'tables');assert.equal(await page.locator('.table-tile').count(),12);await snapshot(page,'02-mesas-escritorio');
  await click(page,'table',1);await click(page,'product',1);await click(page,'modal-qty',1);await click(page,'modal-qty',1);
  assert((await page.locator('.modal-footer').innerText()).includes('$135.00'));
  await click(page,'tortilla','harina');assert((await page.locator('.modal-footer').innerText()).includes('$150.00'));
  await click(page,'option','onion');await page.locator('#product-notes').fill('Bien dorado, sin sal');await snapshot(page,'03-personalizar-escritorio');
  await click(page,'add-custom');await click(page,'product',7);
  assert((await page.locator('.ticket-total').innerText()).includes('$185.00'));
  // El historial no debe mover este carrito a otra mesa.
  await page.evaluate(()=>{location.hash='#order?id=1';});await page.waitForTimeout(150);
  assert((await page.locator('.order-heading').innerText()).includes('MESA 01'));
  await waitMutation(page,()=>click(page,'send-order'),'/send');
  let data=await bootstrap(),o=data.orders.find(o=>o.tableId===1&&!o.closed);assert(o);assert.equal(o.total,18500);assert.equal(o.status,'kitchen');assert.equal(o.items[0].options.onion,false);assert.equal(o.items[0].notes,'Bien dorado, sin sal');
  await page.reload();await page.waitForSelector('.ticket');assert((await page.locator('.ticket-total').innerText()).includes('$185.00'));await snapshot(page,'04-pedido-escritorio');
  await click(page,'kitchen');let card=page.locator('.delivery-order').filter({hasText:o.folio});assert((await card.innerText()).includes('Bien dorado, sin sal'));
  await waitMutation(page,()=>card.locator('[data-action="status"]').click(),`/api/orders/${o.id}`);
  await click(page,'checkout',o.id);await click(page,'split','equal');await click(page,'tip',10);await page.locator('#received').fill('1000');
  assert((await page.locator('#payment-breakdown').innerText()).includes('$898.25'));
  await snapshot(page,'05-cobro-escritorio');await waitMutation(page,()=>click(page,'pay'),'/payments');
  data=await bootstrap();o=data.orders.find(x=>x.id===o.id);assert.equal(o.remaining,9250);assert.equal(o.payments.length,1);assert.equal(o.payments[0].tip,925);assert.equal(o.closed,false);
  assert((await page.locator('.receipt').innerText()).includes('$898.25'));await click(page,'close-modal');await click(page,'tables');assert(await page.locator('[data-action="table"][data-id="1"]').evaluate(el=>el.classList.contains('occupied')));
  await click(page,'table',1);await click(page,'checkout',o.id);await page.locator('#received').fill('80');assert(await page.locator('#confirm-payment').isDisabled());await click(page,'exact');
  // Simula pérdida de respuesta DESPUÉS de que el servidor registra el abono.
  let lost=false;
  await page.route('**/payments',async route=>{if(!lost){lost=true;await route.fetch();await route.abort('failed');}else await route.continue();});
  await click(page,'pay');await page.waitForSelector('[data-action="recover-payment"]');
  data=await bootstrap();assert.equal(data.orders.find(x=>x.id===o.id).payments.length,2);
  await page.reload();await page.waitForSelector('.receipt');
  data=await bootstrap();o=data.orders.find(x=>x.id===o.id);assert.equal(o.payments.length,2);assert.equal(o.closed,true);assert.equal(o.remaining,0);
  await click(page,'close-modal');await click(page,'tables');assert(!(await page.locator('[data-action="table"][data-id="1"]').evaluate(el=>el.classList.contains('occupied'))));
  // Recogida: personalización, cobro anticipado, empaque y entrega independiente.
  await click(page,'new-llevar');await page.locator('#f-customer').fill('Cliente de práctica');await click(page,'product',7);await waitMutation(page,()=>click(page,'send-pay'),'/send');
  await click(page,'method','card');await page.locator('#external-confirmed').check();await waitMutation(page,()=>click(page,'pay'),'/payments');await click(page,'close-modal');
  data=await bootstrap();let carry=data.orders.find(x=>x.customer==='Cliente de práctica');assert.equal(carry.status,'kitchen');assert.equal(carry.paymentStatus,'paid');assert(!carry.closed);
  await click(page,'kitchen');card=page.locator('.delivery-order').filter({hasText:carry.folio});await waitMutation(page,()=>card.locator('[data-action="status"]').click(),`/api/orders/${carry.id}`);card=page.locator('.delivery-order').filter({hasText:carry.folio});await waitMutation(page,()=>card.locator('[data-action="status"]').click(),`/api/orders/${carry.id}`);
  // Domicilio: validación, checklist, repartidor y cierre con pago previo.
  await click(page,'delivery');await click(page,'new-domicilio');await click(page,'product',8);await click(page,'send-order');assert(await page.locator('#f-customer').evaluate(el=>el===document.activeElement));
  await page.locator('#f-customer').fill('María Prueba');await page.locator('#f-phone').fill('5559876543');await page.locator('#f-address').fill('Calle de práctica 10, Centro');await page.locator('#f-reference').fill('Puerta verde');
  let orderLost=false;
  await page.route('**/api/orders',async route=>{if(route.request().method()==='POST'&&!orderLost){orderLost=true;await route.fetch();await route.abort('failed');}else await route.continue();});
  await click(page,'send-order');await page.waitForSelector('[data-action="recover-order"]');
  await click(page,'close-modal');await click(page,'product',8);assert.equal(await page.locator('[data-action="recover-order"]').count(),1);
  await page.reload();await page.waitForSelector('.ticket');await page.waitForFunction(()=>!localStorage.getItem('mibuen-order-pending'));
  data=await bootstrap();let dom=data.orders.find(x=>x.customer==='María Prueba');assert(dom);assert.equal(data.orders.filter(x=>x.customer==='María Prueba').length,1);assert.equal(dom.status,'kitchen');
  await click(page,'edit-customer');await page.locator('#edit-reference').fill('Portón verde, llamar al llegar');await waitMutation(page,()=>click(page,'save-customer'),`/api/orders/${dom.id}`);assert.equal((await bootstrap()).orders.find(x=>x.id===dom.id).reference,'Portón verde, llamar al llegar');
  await click(page,'delivery');card=page.locator('.delivery-order').filter({hasText:dom.folio});await waitMutation(page,()=>card.locator('[data-action="status"]').click(),`/api/orders/${dom.id}`);
  await click(page,'dispatch',dom.id);assert((await page.locator('#toast').innerText()).includes('Selecciona un repartidor'));
  for(const key of ['label','extras','napkins']){card=page.locator('.delivery-order').filter({hasText:dom.folio});await waitMutation(page,()=>card.locator(`[data-check="${key}"]`).check(),`/api/orders/${dom.id}`);}
  await waitMutation(page,()=>page.locator(`#driver-${dom.id}`).selectOption('Miguel López'),`/api/orders/${dom.id}`);await waitMutation(page,()=>click(page,'dispatch',dom.id),`/api/orders/${dom.id}`);await snapshot(page,'06-domicilios-escritorio');
  await click(page,'checkout',dom.id);await click(page,'exact');await waitMutation(page,()=>click(page,'pay'),'/payments');await click(page,'close-modal');await click(page,'delivery');card=page.locator('.delivery-order').filter({hasText:dom.folio});await waitMutation(page,()=>card.locator('[data-action="status"]').click(),`/api/orders/${dom.id}`);
  data=await bootstrap();dom=data.orders.find(x=>x.id===dom.id);assert(dom.closed);assert.equal(dom.payments.length,1);
  await click(page,'tables');await click(page,'table',5);await click(page,'product',7);await waitMutation(page,()=>click(page,'send-order'),'/send');
  data=await bootstrap();const cancelled=data.orders.find(x=>x.tableId===5&&!x.closed);await click(page,'cancel-order');await waitMutation(page,()=>click(page,'confirm-cancel'),`/api/orders/${cancelled.id}`);data=await bootstrap();assert.equal(data.orders.find(x=>x.id===cancelled.id).status,'cancelled');assert(data.orders.find(x=>x.id===cancelled.id).closed);
  // Tamaños: escritorio, tableta y móvil; navegación y modal por teclado.
  for(const [name,width,height]of [['tablet',768,1024],['movil',390,844]]){
    await page.setViewportSize({width,height});await click(page,'home');await assertNoOverflow(page,'inicio '+name);await snapshot(page,`07-inicio-${name}`);
    await click(page,'tables');await assertNoOverflow(page,'mesas '+name);await click(page,'table',3);await assertNoOverflow(page,'pedido '+name);await snapshot(page,`08-pedido-${name}`);
    await click(page,'product',1);await assertNoOverflow(page,'modal '+name);await snapshot(page,`09-personalizar-${name}`);await page.keyboard.press('Escape');assert.equal(await page.locator('.modal').count(),0);
    if(name==='movil'){await click(page,'show-ticket');assert(await page.locator('#order-ticket').evaluate(el=>el===document.activeElement));}
    await click(page,'delivery');await assertNoOverflow(page,'domicilio '+name);
  }
  assert.deepEqual(errors,[]);
  console.log('OK: 7 vistas, centavos/harina/notas, borrador/historial, recarga, pago parcial, respuestas perdidas de orden/pago sin duplicados, edición/cancelación, llevar, domicilio, 1440/768/390 px y teclado.');
}
run().catch(error=>{console.error(error);process.exitCode=1;}).finally(async()=>{await browser?.close();child.kill('SIGTERM');console.log('SQLite de prueba aislada:',scratch);});

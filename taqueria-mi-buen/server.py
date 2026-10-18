#!/usr/bin/env python3
"""MI BUEN: práctica local, Python 3.10+, sin servicios ni dependencias externas.
No contiene autenticación. No exponer a Internet ni usar datos reales.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
import json
import os
import sqlite3
import uuid
from datetime import datetime, timezone, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parent
MAX_BODY = 100_000
PRODUCTS = [
    dict(id=1, name='Taco de asada', category='tacos', price=4500, description='Carne asada al carbón, tortilla recién hecha.', badge='Favorito', emoji='🌮', customizable=True, available=True),
    dict(id=2, name='Quesadilla de asada', category='quesadillas', price=9000, description='Tortilla de harina, queso fundido y asada.', badge='', emoji='🧀', customizable=True, available=True),
    dict(id=3, name='Lirio de asada', category='quesadillas', price=11500, description='Asada, queso y tortilla grande dorada.', badge='Especialidad', emoji='🌯', customizable=True, available=True),
    dict(id=4, name='Taco de carne doble', category='tacos', price=7500, description='Doble porción de nuestra carne asada.', badge='', emoji='🌮', customizable=True, available=True),
    dict(id=5, name='Papa asada', category='paquetes', price=13000, description='Papa al carbón con asada y queso.', badge='', emoji='🥔', customizable=True, available=True),
    dict(id=6, name='Medio kilo de asada', category='paquetes', price=29000, description='Para compartir, con tortillas y complementos.', badge='Para compartir', emoji='🥩', customizable=True, available=True),
    dict(id=7, name='Coca-Cola', category='bebidas', price=3500, description='Refresco de cola, 355 ml.', badge='', emoji='🥤', customizable=False, available=True),
    dict(id=8, name='Agua fresca', category='bebidas', price=4000, description='Agua fresca de la casa, 500 ml.', badge='', emoji='🍹', customizable=False, available=True),
]
TABLES = [dict(id=i, name=f'Mesa {i:02}', seats=6 if i in (4, 8, 12) else 4) for i in range(1, 13)]
DEFAULT_OPTIONS = dict(tortilla='maiz', onion=True, cilantro=True, salsa=True, salsaAside=False)
SETTINGS = dict(businessName='MI BUEN · ASADA POS', station='Caja Principal 01', currency='MXN', demo=True)


def now():
    return datetime.now(timezone.utc).isoformat(timespec='seconds')


class APIError(Exception):
    def __init__(self, status, message, **details):
        self.status, self.message, self.details = status, message, details


def integer(value, label, minimum=0, maximum=1_000_000_000):
    if type(value) is not int or not minimum <= value <= maximum:
        raise APIError(422, f'{label} debe ser un entero entre {minimum} y {maximum}.')
    return value


def string(value, label, maximum=500):
    if not isinstance(value, str) or len(value) > maximum:
        raise APIError(422, f'{label} debe ser texto de hasta {maximum} caracteres.')
    return value.strip()


def object_value(value, label):
    if not isinstance(value, dict):
        raise APIError(422, f'{label} debe ser un objeto JSON.')
    return value


def enrich(order):
    order['subtotal'] = order['total'] = sum(i['total'] for i in order['items'])
    order['paid'] = sum(p['amount'] for p in order['payments'])
    order['remaining'] = order['total'] - order['paid']
    order['paymentStatus'] = 'paid' if order['paid'] and not order['remaining'] else ('partial' if order['paid'] else 'unpaid')
    completed = order['status'] in (('ready', 'paid') if order['channel'] == 'salon' else ('delivered', 'paid'))
    if order['paymentStatus'] == 'paid' and completed:
        order['status'] = 'paid'
    order['closed'] = order['status'] in ('paid', 'cancelled')
    return order


class Store:
    def __init__(self, path):
        self.path = str(path)
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        with self.connection() as db:
            db.execute('PRAGMA journal_mode=WAL')
            db.executescript('''
                CREATE TABLE IF NOT EXISTS metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL);
                CREATE TABLE IF NOT EXISTS mutation_requests (
                    route TEXT NOT NULL, request_id TEXT NOT NULL, payload TEXT NOT NULL,
                    status INTEGER NOT NULL, response TEXT NOT NULL,
                    PRIMARY KEY (route, request_id)
                );
                CREATE TABLE IF NOT EXISTS orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    channel TEXT NOT NULL, table_id INTEGER, closed INTEGER NOT NULL,
                    payload TEXT NOT NULL
                );
                CREATE UNIQUE INDEX IF NOT EXISTS active_table ON orders(table_id)
                WHERE channel='salon' AND closed=0;
            ''')
            db.execute('BEGIN IMMEDIATE')
            if not db.execute("SELECT 1 FROM metadata WHERE key='seeded'").fetchone():
                self.seed(db)
                db.execute("INSERT INTO metadata VALUES ('seeded','1')")

    @contextmanager
    def connection(self):
        db = sqlite3.connect(self.path, timeout=15)
        db.row_factory = sqlite3.Row
        db.execute('PRAGMA busy_timeout=15000')
        try:
            with db:
                yield db
        finally:
            db.close()

    def load(self, db, order_id):
        row = db.execute('SELECT payload FROM orders WHERE id=?', (order_id,)).fetchone()
        if row is None:
            raise APIError(404, 'Pedido no encontrado.')
        return json.loads(row['payload'])

    def save(self, db, order):
        enrich(order)
        order['updatedAt'] = now()
        db.execute('UPDATE orders SET channel=?,table_id=?,closed=?,payload=? WHERE id=?',
                   (order['channel'], order['tableId'], int(order['closed']), json.dumps(order, ensure_ascii=False), order['id']))
        return order

    def make_items(self, items, start=1):
        if not isinstance(items, list) or not 1 <= len(items) <= 100:
            raise APIError(422, 'items debe contener entre 1 y 100 productos.')
        result = []
        for index, item in enumerate(items, start):
            object_value(item, 'Producto')
            pid = integer(item.get('productId'), 'productId', 1, 100000)
            product = next((p for p in PRODUCTS if p['id'] == pid), None)
            if not product or not product['available']:
                raise APIError(422, 'Producto no disponible.')
            quantity = integer(item.get('quantity'), 'quantity', 1, 99)
            options = dict(DEFAULT_OPTIONS)
            if 'options' in item:
                supplied = object_value(item['options'], 'options')
                if set(supplied) - set(DEFAULT_OPTIONS):
                    raise APIError(422, 'Opción de producto desconocida.')
                options.update(supplied)
            if options['tortilla'] not in ('maiz', 'harina'):
                raise APIError(422, 'Tortilla inválida.')
            if any(type(options[key]) is not bool for key in ('onion', 'cilantro', 'salsa', 'salsaAside')):
                raise APIError(422, 'Los complementos deben ser booleanos.')
            if not product['customizable']:
                options = dict(DEFAULT_OPTIONS)
            unit_price = product['price'] + (500 if product['customizable'] and options['tortilla'] == 'harina' else 0)
            result.append(dict(id=index, productId=pid, name=product['name'], quantity=quantity,
                               unitPrice=unit_price, total=quantity * unit_price,
                               options=options, notes=string(item.get('notes', ''), 'Notas', 300)))
        return result

    def create(self, db, data):
        channel = data.get('channel')
        if channel not in ('salon', 'llevar', 'domicilio'):
            raise APIError(422, 'Canal inválido.')
        table_id = data.get('tableId')
        if channel == 'salon':
            integer(table_id, 'Mesa', 1, 12)
            row = db.execute("SELECT id FROM orders WHERE channel='salon' AND table_id=? AND closed=0", (table_id,)).fetchone()
            if row:
                raise APIError(409, 'Esta mesa ya tiene un pedido activo.', orderId=row['id'])
        elif table_id is not None:
            raise APIError(422, 'Solo los pedidos de salón usan mesa.')
        items = self.make_items(data.get('items'))
        guests = integer(data.get('guests', 1), 'Comensales', 1, 20)
        order = dict(channel=channel, tableId=table_id, guests=guests, status='draft', items=items,
                     payments=[], driver='', checks=dict(label=False, extras=False, napkins=False), createdAt=now())
        for key in ('customer', 'phone', 'address', 'reference', 'notes'):
            order[key] = string(data.get(key, ''), key, 1000 if key == 'address' else 500)
        if channel == 'domicilio' and (not order['customer'] or not order['phone'] or not order['address']):
            raise APIError(422, 'Domicilio requiere cliente, teléfono y dirección.')
        cursor = db.execute('INSERT INTO orders(channel,table_id,closed,payload) VALUES (?,?,0,?)', (channel, table_id, '{}'))
        order['id'] = cursor.lastrowid
        order['folio'] = f'MB-{order["id"]:04}'
        return self.save(db, order)

    def seed(self, db):
        examples = [
            dict(channel='salon', tableId=4, customer='Familia Hernández', guests=5, items=[dict(productId=6, quantity=2), dict(productId=2, quantity=1), dict(productId=7, quantity=4), dict(productId=8, quantity=1)]),
            dict(channel='salon', tableId=2, customer='Diego', guests=2, items=[dict(productId=1, quantity=6), dict(productId=8, quantity=2)]),
            dict(channel='salon', tableId=8, customer='Ana y Luis', guests=2, items=[dict(productId=3, quantity=2), dict(productId=7, quantity=2)]),
            dict(channel='llevar', customer='Sofía Ramírez', phone='5551234567', items=[dict(productId=1, quantity=8), dict(productId=5, quantity=1)]),
            dict(channel='llevar', customer='Carlos Mendoza', phone='5552345678', items=[dict(productId=2, quantity=3), dict(productId=8, quantity=2)]),
            dict(channel='domicilio', customer='Valeria Torres', phone='5553456789', address='Av. Reforma 123, Col. Centro', reference='Casa azul, tocar el timbre.', items=[dict(productId=6, quantity=1), dict(productId=7, quantity=2)]),
            dict(channel='domicilio', customer='Jorge Martínez', phone='5554567890', address='Calle del Sol 48, Col. Jardines', reference='Frente al parque.', items=[dict(productId=1, quantity=10), dict(productId=8, quantity=2)]),
        ]
        for idx, example in enumerate(examples):
            order = self.create(db, example)
            order['status'] = ['kitchen', 'draft', 'ready', 'kitchen', 'ready', 'ready', 'dispatched'][idx]
            order['createdAt'] = (datetime.now(timezone.utc) - timedelta(minutes=5 + idx * 3)).isoformat(timespec='seconds')
            if order['status'] == 'dispatched':
                order['driver'] = 'Miguel López'
                order['checks'] = dict(label=True, extras=True, napkins=True)
            self.save(db, order)

    def dispatch(self, method, path, data):
        with self.connection() as db:
            if method != 'GET':
                db.execute('BEGIN IMMEDIATE')
            pieces = path.strip('/').split('/')
            repeat_safe = method == 'POST' and (path == '/api/orders' or (len(pieces) == 4 and pieces[:2] == ['api', 'orders'] and pieces[3] in ('items', 'send')))
            request_id = None
            if repeat_safe and 'requestId' in data:
                request_id = string(data['requestId'], 'requestId', 100)
                if not request_id:
                    raise APIError(422, 'requestId no puede estar vacío.')
                serialized = json.dumps(data, sort_keys=True, ensure_ascii=False)
                previous = db.execute('SELECT * FROM mutation_requests WHERE route=? AND request_id=?', (path, request_id)).fetchone()
                if previous:
                    if previous['payload'] != serialized:
                        raise APIError(409, 'requestId ya se utilizó con otros datos.')
                    result = json.loads(previous['response'])
                    # Return current order; a late retry must not roll the UI back to an old status.
                    result['order'] = self.load(db, result['order']['id'])
                    result['replayed'] = True
                    return previous['status'], result
            status, result = self._dispatch(db, method, path, data)
            if request_id:
                db.execute('INSERT INTO mutation_requests VALUES (?,?,?,?,?)',
                           (path, request_id, serialized, status, json.dumps(result, ensure_ascii=False)))
            return status, result

    def _dispatch(self, db, method, path, data):
        if method == 'GET' and path == '/api/bootstrap':
            orders = [json.loads(row['payload']) for row in db.execute('SELECT payload FROM orders ORDER BY id DESC')]
            return 200, dict(products=PRODUCTS, tables=TABLES, orders=orders, settings=SETTINGS)
        if method == 'POST' and path == '/api/orders':
            return 201, dict(order=self.create(db, data))
        if method == 'POST' and path == '/api/demo/reset':
            if data.get('confirm') != 'REINICIAR DEMO':
                raise APIError(422, 'Confirma con REINICIAR DEMO.')
            db.execute('DELETE FROM orders')
            # Do not reuse identifiers: stale browser tabs must never mutate a new order.
            self.seed(db)
            return 200, dict(ok=True)
        segments = path.strip('/').split('/')
        if len(segments) < 3 or segments[:2] != ['api', 'orders'] or not segments[2].isdigit():
            raise APIError(404, 'Ruta no encontrada.')
        order = self.load(db, int(segments[2]))
        suffix = segments[3:]
        if method == 'GET' and not suffix:
            return 200, dict(order=order)
        if method == 'POST' and suffix == ['payments']:
            # Check idempotency even after closure so retrying a successful request is safe.
            request_id = string(data.get('requestId'), 'requestId', 100)
            if not request_id:
                raise APIError(422, 'requestId es obligatorio para evitar cobros duplicados.')
            previous = next((p for p in order['payments'] if p['requestId'] == request_id), None)
            if previous:
                signature = dict(amount=data.get('amount'), method=data.get('method'), tip=data.get('tip', 0), received=data.get('received', data.get('amount', 0) + data.get('tip', 0)) if type(data.get('amount')) is int and type(data.get('tip', 0)) is int else None)
                if any(previous[key] != value for key, value in signature.items()):
                    raise APIError(409, 'requestId ya se utilizó con otro importe o método.')
                return 200, dict(order=order, payment=previous, replayed=True)
            if order['closed'] or order['remaining'] == 0:
                raise APIError(409, 'El pedido ya está cerrado o liquidado.')
            amount = integer(data.get('amount'), 'amount', 1, order['remaining'])
            tip = integer(data.get('tip', 0), 'tip', 0, 1_000_000)
            payment_method = data.get('method')
            if payment_method not in ('cash', 'card', 'transfer'):
                raise APIError(422, 'Método de pago inválido.')
            received = integer(data.get('received', amount + tip), 'received', amount + tip)
            if payment_method != 'cash' and received != amount + tip:
                raise APIError(422, 'Solo efectivo genera cambio.')
            payment = dict(id=uuid.uuid4().hex, requestId=request_id, amount=amount, method=payment_method,
                           tip=tip, received=received, change=received - amount - tip, createdAt=now())
            order['payments'].append(payment)
            return 200, dict(order=self.save(db, order), payment=payment)
        if order['closed']:
            raise APIError(409, 'El pedido está cerrado.')
        if method == 'POST' and suffix == ['items']:
            if order['status'] not in ('draft', 'kitchen', 'ready'):
                raise APIError(409, 'No se puede modificar un pedido en reparto o entregado.')
            if order['paid']:
                raise APIError(409, 'No se pueden agregar productos después de registrar pagos.')
            new_items = self.make_items(data.get('items'), max((i['id'] for i in order['items']), default=0) + 1)
            if len(order['items']) + len(new_items) > 100:
                raise APIError(422, 'Un pedido admite hasta 100 líneas.')
            order['items'].extend(new_items)
            if order['status'] != 'draft':
                order['status'] = 'kitchen'
            return 200, dict(order=self.save(db, order))
        if method == 'DELETE' and len(suffix) == 2 and suffix[0] == 'items' and suffix[1].isdigit():
            if order['status'] != 'draft' or order['paid']:
                raise APIError(409, 'Solo se eliminan productos de pedidos sin enviar y sin pagos.')
            matches = [i for i in order['items'] if i['id'] == int(suffix[1])]
            if not matches:
                raise APIError(404, 'Producto del pedido no encontrado.')
            if len(order['items']) == 1:
                raise APIError(409, 'Conserva al menos un producto o cancela el pedido.')
            order['items'] = [i for i in order['items'] if i not in matches]
            return 200, dict(order=self.save(db, order))
        if method == 'POST' and suffix == ['send']:
            if order['status'] != 'draft':
                raise APIError(409, 'Solo se envían pedidos en borrador.')
            order['status'] = 'kitchen'
            return 200, dict(order=self.save(db, order))
        if method == 'PATCH' and not suffix:
            allowed = {'status', 'driver', 'checks', 'customer', 'phone', 'address', 'reference', 'notes'}
            if set(data) - allowed:
                raise APIError(422, 'Campo no editable.')
            for key in allowed - {'status', 'checks'}:
                if key in data:
                    order[key] = string(data[key], key, 1000 if key == 'address' else 500)
            if 'checks' in data:
                checks = object_value(data['checks'], 'checks')
                if set(checks) - set(order['checks']) or any(type(v) is not bool for v in checks.values()):
                    raise APIError(422, 'Checklist inválido.')
                order['checks'].update(checks)
            if 'status' in data and data['status'] != order['status']:
                target = data['status']
                if not isinstance(target, str):
                    raise APIError(422, 'status debe ser texto.')
                possible = {'draft': {'kitchen', 'cancelled'}, 'kitchen': {'ready', 'cancelled'}, 'ready': {'cancelled'}, 'dispatched': {'delivered'}, 'delivered': set()}
                if order['channel'] == 'llevar':
                    possible['ready'].add('delivered')
                if order['channel'] == 'domicilio':
                    possible['ready'].add('dispatched')
                if target not in possible.get(order['status'], set()):
                    raise APIError(409, 'Transición de estado no permitida.')
                if target == 'cancelled' and order['paid']:
                    raise APIError(409, 'No se cancela un pedido con pagos; esta práctica no incluye devoluciones.')
                if target == 'dispatched' and (not order['driver'] or not all(order['checks'].values())):
                    raise APIError(422, 'Selecciona repartidor y completa etiqueta, extras y servilletas.')
                order['status'] = target
            if order['channel'] == 'domicilio' and any(not order[k] for k in ('customer', 'phone', 'address')):
                raise APIError(422, 'Domicilio requiere cliente, teléfono y dirección.')
            return 200, dict(order=self.save(db, order))
        raise APIError(404, 'Ruta no encontrada.')


class Handler(BaseHTTPRequestHandler):
    server_version = 'MiBuenDemo/1.0'

    def log_message(self, format, *args):
        # Avoid printing untrusted customer data and request paths to terminal logs.
        pass

    def send_json(self, status, data):
        body = json.dumps(data, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'no-store')
        self.security_headers()
        self.end_headers()
        if self.command != 'HEAD':
            self.wfile.write(body)

    def security_headers(self):
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('X-Frame-Options', 'DENY')
        self.send_header('Referrer-Policy', 'no-referrer')
        self.send_header('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'")

    def handle_request(self):
        try:
            host = self.headers.get('Host', '')
            if host not in (f'127.0.0.1:{self.server.server_port}', f'localhost:{self.server.server_port}', '127.0.0.1', 'localhost'):
                raise APIError(403, 'Esta demo solo admite acceso local.')
            try:
                path = unquote(urlsplit(self.path).path)
            except (ValueError, UnicodeError):
                raise APIError(400, 'Ruta inválida.')
            if '\\' in path or '\x00' in path or '..' in path.split('/'):
                raise APIError(400, 'Ruta inválida.')
            if self.command not in ('GET', 'HEAD'):
                origin = self.headers.get('Origin')
                if origin and origin != f'http://{host}':
                    raise APIError(403, 'Origen externo no permitido.')
                if self.headers.get('Sec-Fetch-Site') == 'cross-site':
                    raise APIError(403, 'Solicitud externa no permitida.')
            if path.startswith('/api/'):
                if self.command == 'HEAD':
                    raise APIError(405, 'Método no permitido.')
                data = {}
                if self.command in ('POST', 'PATCH', 'DELETE'):
                    if self.headers.get('Transfer-Encoding'):
                        raise APIError(400, 'Transfer-Encoding no admitido.')
                    try:
                        length = int(self.headers.get('Content-Length', '0'))
                    except ValueError:
                        raise APIError(400, 'Content-Length inválido.')
                    if not 0 <= length <= MAX_BODY:
                        raise APIError(413, 'El cuerpo supera 100 KB.')
                    if length:
                        if self.headers.get_content_type() != 'application/json':
                            raise APIError(415, 'Usa application/json.')
                        try:
                            data = json.loads(self.rfile.read(length))
                        except (ValueError, UnicodeError):
                            raise APIError(400, 'JSON inválido.')
                        object_value(data, 'Solicitud')
                status, result = self.server.store.dispatch(self.command, path, data)
                self.send_json(status, result)
            else:
                if self.command not in ('GET', 'HEAD'):
                    raise APIError(405, 'Método no permitido.')
                root = ROOT / 'web'
                target = (root / ('index.html' if path == '/' else path.lstrip('/'))).resolve()
                if not target.is_relative_to(root.resolve()) or not target.is_file():
                    raise APIError(404, 'Archivo no encontrado.')
                if target.suffix.lower() not in ('.html', '.js', '.css', '.svg', '.png', '.jpg', '.jpeg', '.webp', '.ico', '.woff2'):
                    raise APIError(404, 'Archivo no disponible.')
                import mimetypes
                body = target.read_bytes()
                self.send_response(200)
                self.send_header('Content-Type', mimetypes.guess_type(str(target))[0] or 'application/octet-stream')
                self.send_header('Content-Length', str(len(body)))
                self.send_header('Cache-Control', 'no-cache')
                self.security_headers()
                self.end_headers()
                if self.command != 'HEAD':
                    self.wfile.write(body)
        except APIError as error:
            self.send_json(error.status, dict(error=error.message, **error.details))
        except (sqlite3.Error, OSError):
            self.send_json(500, dict(error='No se pudo completar la operación local. Revisa permisos y espacio en disco.'))

    do_GET = do_HEAD = do_POST = do_PATCH = do_DELETE = do_OPTIONS = handle_request


def create_server(port=8787, db_path=None):
    server = ThreadingHTTPServer(('127.0.0.1', port), Handler)
    server.daemon_threads = True
    server.store = Store(db_path or os.environ.get('TAQUERIA_DB', str(ROOT / 'data' / 'taqueria.sqlite3')))
    return server


def main():
    parser = argparse.ArgumentParser(description='MI BUEN — práctica local; sin autenticación, no publicar en Internet.')
    parser.add_argument('--port', type=int, default=8787)
    parser.add_argument('--db', help='Ruta SQLite alternativa; por defecto TAQUERIA_DB o data/taqueria.sqlite3.')
    args = parser.parse_args()
    server = create_server(args.port, args.db)
    print(f'MI BUEN · demo local: http://127.0.0.1:{server.server_port}', flush=True)
    print('Datos ficticios. Ctrl+C para detener. No exponer a Internet.', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()

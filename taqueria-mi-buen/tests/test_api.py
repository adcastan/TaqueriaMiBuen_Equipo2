"""Pruebas HTTP reales, BD temporal y sin modificar los datos de la demo."""
import concurrent.futures
import http.client
import importlib.util
import json
import tempfile
import threading
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('mibuen_server', ROOT / 'server.py')
server_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(server_module)


class APITest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.db = Path(cls.temp.name) / 'test.sqlite3'
        cls.server = server_module.create_server(0, cls.db)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.port = cls.server.server_port

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)
        cls.temp.cleanup()

    def request(self, method, path, payload=None, headers=None):
        client = http.client.HTTPConnection('127.0.0.1', self.port, timeout=5)
        body = json.dumps(payload).encode() if payload is not None else None
        request_headers = {'Content-Type': 'application/json'}
        request_headers.update(headers or {})
        try:
            client.request(method, path, body, request_headers)
            response = client.getresponse()
            content = response.read()
            return response.status, json.loads(content) if content else None
        finally:
            client.close()

    def make_order(self, **override):
        data = dict(channel='llevar', customer='Prueba', items=[dict(productId=1, quantity=2)])
        data.update(override)
        status, result = self.request('POST', '/api/orders', data)
        self.assertEqual(status, 201, result)
        return result['order']

    def pay(self, order, **override):
        payment = dict(amount=order['remaining'], method='cash', tip=0, received=order['remaining'], requestId=f'test-{order["id"]}')
        payment.update(override)
        return self.request('POST', f'/api/orders/{order["id"]}/payments', payment)

    def test_bootstrap_and_seed(self):
        status, data = self.request('GET', '/api/bootstrap')
        self.assertEqual(status, 200)
        self.assertEqual(len(data['tables']), 12)
        self.assertEqual(next(p for p in data['products'] if p['id'] == 1)['price'], 4500)
        table4 = next(o for o in data['orders'] if o['tableId'] == 4 and not o['closed'])
        self.assertEqual(table4['total'], 85000)
        self.assertEqual(table4['status'], 'kitchen')

    def test_totals_are_server_owned_and_quantity_validated(self):
        order = self.make_order(items=[dict(productId=1, quantity=3, unitPrice=1, total=1)], total=1)
        self.assertEqual(order['total'], 13500)
        for invalid in [0, -1, True, 1.5, '2', 100]:
            status, _ = self.request('POST', '/api/orders', dict(channel='llevar', items=[dict(productId=1, quantity=invalid)]))
            self.assertEqual(status, 422, invalid)
        status, _ = self.request('POST', '/api/orders', dict(channel='llevar', items=[dict(productId=999, quantity=1)]))
        self.assertEqual(status, 422)

    def test_flour_surcharge_and_drink_options(self):
        order = self.make_order(items=[dict(productId=1, quantity=3, options=dict(tortilla='harina'))])
        self.assertEqual(order['total'], 15000)
        self.assertEqual(order['items'][0]['unitPrice'], 5000)
        drink = self.make_order(items=[dict(productId=7, quantity=2, options=dict(tortilla='harina'))])
        self.assertEqual(drink['total'], 7000)
        self.assertEqual(drink['items'][0]['options']['tortilla'], 'maiz')

    def test_create_and_add_items_idempotency(self):
        body = dict(channel='llevar', items=[dict(productId=1, quantity=1)], requestId='create-once')
        first_status, first = self.request('POST', '/api/orders', body)
        second_status, second = self.request('POST', '/api/orders', body)
        self.assertEqual(first_status, 201)
        self.assertEqual(second_status, 201)
        self.assertEqual(first['order']['id'], second['order']['id'])
        self.assertTrue(second['replayed'])
        self.assertEqual(self.request('POST', '/api/orders', dict(body, customer='Different'))[0], 409)
        path = f'/api/orders/{first["order"]["id"]}'
        addition = dict(items=[dict(productId=7, quantity=1)], requestId='add-once')
        self.request('POST', path + '/items', addition)
        _, repeat = self.request('POST', path + '/items', addition)
        self.assertEqual(repeat['order']['total'], 8000)
        self.assertEqual(len(repeat['order']['items']), 2)
        self.request('POST', path + '/send', dict(requestId='send-once'))
        status, sent = self.request('POST', path + '/send', dict(requestId='send-once'))
        self.assertEqual(status, 200)
        self.assertEqual(sent['order']['status'], 'kitchen')
        self.assertTrue(sent['replayed'])

    def test_table_exclusivity_and_final_payment_frees_table(self):
        order = self.make_order(channel='salon', tableId=1)
        status, data = self.request('POST', '/api/orders', dict(channel='salon', tableId=1, items=[dict(productId=1, quantity=1)]))
        self.assertEqual(status, 409)
        self.assertEqual(data['orderId'], order['id'])
        status, result = self.pay(order)
        self.assertEqual(status, 200)
        self.assertFalse(result['order']['closed'])
        self.assertEqual(result['order']['status'], 'draft')
        for target in ('kitchen', 'ready'):
            status, result = self.request('PATCH', f'/api/orders/{order["id"]}', dict(status=target))
            self.assertEqual(status, 200, result)
        self.assertTrue(result['order']['closed'])
        self.assertEqual(result['order']['status'], 'paid')
        next_order = self.make_order(channel='salon', tableId=1)
        self.assertNotEqual(order['id'], next_order['id'])
        self.pay(next_order)
        for target in ('kitchen', 'ready'):
            self.request('PATCH', f'/api/orders/{next_order["id"]}', dict(status=target))

    def test_partial_payment_tips_change_and_idempotence(self):
        order = self.make_order()
        args = dict(amount=4000, method='cash', received=5000, tip=500, requestId='partial')
        status, first = self.pay(order, **args)
        self.assertEqual(status, 200)
        self.assertEqual(first['order']['paid'], 4000)
        self.assertEqual(first['order']['remaining'], 5000)
        self.assertEqual(first['order']['paymentStatus'], 'partial')
        self.assertEqual(first['payment']['change'], 500)
        status, duplicate = self.pay(order, **args)
        self.assertEqual(status, 200)
        self.assertTrue(duplicate['replayed'])
        self.assertEqual(len(duplicate['order']['payments']), 1)
        status, _ = self.pay(order, **dict(args, amount=3000))
        self.assertEqual(status, 409)
        status, final = self.pay(first['order'], requestId='final')
        self.assertEqual(status, 200)
        self.assertEqual(final['order']['remaining'], 0)
        self.assertEqual(final['order']['paymentStatus'], 'paid')
        self.assertFalse(final['order']['closed'])  # Pickup still pending.
        self.assertEqual(final['order']['status'], 'draft')
        for target in ('kitchen', 'ready', 'delivered'):
            status, result = self.request('PATCH', f'/api/orders/{order["id"]}', dict(status=target))
            self.assertEqual(status, 200, result)
        self.assertEqual(result['order']['status'], 'paid')
        self.assertTrue(result['order']['closed'])

    def test_concurrent_identical_payment_is_not_duplicated(self):
        order = self.make_order(channel='salon', tableId=3)
        def payment(_):
            return self.pay(order, requestId='same-concurrent')
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            results = list(executor.map(payment, range(4)))
        self.assertTrue(all(status == 200 for status, _ in results), results)
        _, data = self.request('GET', f'/api/orders/{order["id"]}')
        self.assertEqual(len(data['order']['payments']), 1)
        self.assertEqual(data['order']['remaining'], 0)

    def test_paid_kitchen_order_stays_visible_until_ready(self):
        order = self.make_order(channel='salon', tableId=5)
        path = f'/api/orders/{order["id"]}'
        self.assertEqual(self.request('POST', path + '/send', {})[0], 200)
        status, result = self.pay(order)
        self.assertEqual(status, 200)
        self.assertEqual(result['order']['status'], 'kitchen')
        self.assertEqual(result['order']['paymentStatus'], 'paid')
        self.assertEqual(result['order']['remaining'], 0)
        self.assertFalse(result['order']['closed'])
        status, bootstrap = self.request('GET', '/api/bootstrap')
        self.assertEqual(status, 200)
        self.assertTrue(any(o['id'] == order['id'] and o['status'] == 'kitchen' and not o['closed'] for o in bootstrap['orders']))
        occupied, _ = self.request('POST', '/api/orders', dict(channel='salon', tableId=5, items=[dict(productId=1, quantity=1)]))
        self.assertEqual(occupied, 409)
        status, ready = self.request('PATCH', path, dict(status='ready'))
        self.assertEqual(status, 200)
        self.assertEqual(ready['order']['status'], 'paid')
        self.assertTrue(ready['order']['closed'])
        replacement = self.make_order(channel='salon', tableId=5)
        self.assertNotEqual(replacement['id'], order['id'])

    def test_nested_invalid_values_return_json_errors(self):
        for bad in ([], {}, False):
            status, result = self.request('POST', '/api/orders', dict(channel=bad, items=[dict(productId=1, quantity=1)]))
            self.assertEqual(status, 422)
            self.assertIn('error', result)
            status, result = self.request('POST', '/api/orders', dict(channel='llevar', items=[dict(productId=1, quantity=1, options=dict(tortilla=bad))]))
            self.assertEqual(status, 422)
            self.assertIn('error', result)
        order = self.make_order()
        for bad in ([], {}, False):
            self.assertEqual(self.pay(order, method=bad)[0], 422)
            self.assertEqual(self.request('PATCH', f'/api/orders/{order["id"]}', dict(status=bad))[0], 422)
        self.assertEqual(self.request('PATCH', f'/api/orders/{order["id"]}', dict(checks=[]))[0], 422)

    def test_delivery_requires_dispatch_checklist_and_preserves_payment(self):
        order = self.make_order(channel='domicilio', customer='Prueba', phone='5550000000', address='Dirección ficticia')
        oid = order['id']
        status, _ = self.request('PATCH', f'/api/orders/{oid}', dict(status='dispatched'))
        self.assertEqual(status, 409)
        for target in ('kitchen', 'ready'):
            status, _ = self.request('PATCH', f'/api/orders/{oid}', dict(status=target))
            self.assertEqual(status, 200)
        self.assertEqual(self.request('PATCH', f'/api/orders/{oid}', dict(status='dispatched'))[0], 422)
        status, paid = self.pay(order)
        self.assertEqual(status, 200)
        self.assertEqual(paid['order']['status'], 'ready')
        self.assertFalse(paid['order']['closed'])
        status, result = self.request('PATCH', f'/api/orders/{oid}', dict(status='dispatched', driver='Repartidor demo', checks=dict(label=True, extras=True, napkins=True)))
        self.assertEqual(status, 200, result)
        status, result = self.request('PATCH', f'/api/orders/{oid}', dict(status='delivered'))
        self.assertEqual(status, 200)
        self.assertEqual(result['order']['status'], 'paid')

    def test_add_items_returns_ready_order_to_kitchen(self):
        order = self.make_order()
        path = f'/api/orders/{order["id"]}'
        self.assertEqual(self.request('POST', path + '/send', {})[0], 200)
        self.assertEqual(self.request('PATCH', path, dict(status='ready'))[0], 200)
        status, result = self.request('POST', path + '/items', dict(items=[dict(productId=7, quantity=1)]))
        self.assertEqual(status, 200)
        self.assertEqual(result['order']['status'], 'kitchen')
        self.assertEqual(result['order']['total'], 12500)
        self.assertEqual(self.request('DELETE', path + '/items/1')[0], 409)

    def test_invalid_payments_do_not_mutate_order(self):
        order = self.make_order()
        for fields in [dict(amount=9001), dict(amount=1.5), dict(received=8999), dict(method='crypto'), dict(requestId=''), dict(tip=-1)]:
            self.assertEqual(self.pay(order, **fields)[0], 422, fields)
        _, result = self.request('GET', f'/api/orders/{order["id"]}')
        self.assertEqual(result['order']['paid'], 0)

    def test_persistence_and_stored_text(self):
        text = '<img src=x onerror=alert(1)>'
        order = self.make_order(customer=text)
        reopened_store = server_module.Store(self.db)
        _, data = reopened_store.dispatch('GET', f'/api/orders/{order["id"]}', {})
        self.assertEqual(data['order']['customer'], text)
        self.assertEqual(data['order']['total'], 9000)

    def test_security_and_errors(self):
        self.assertEqual(self.request('GET', '/%2e%2e/server.py')[0], 400)
        self.assertEqual(self.request('GET', '/server.py')[0], 404)
        self.assertEqual(self.request('GET', '/api/no-such-route')[0], 404)
        self.assertEqual(self.request('POST', '/api/orders', {}, {'Origin': 'https://evil.example'})[0], 403)
        self.assertEqual(self.request('POST', '/api/orders', {}, {'Host': 'evil.example'})[0], 403)
        self.assertEqual(self.request('POST', '/api/orders', [1, 2])[0], 422)
        order = self.make_order()
        self.assertEqual(self.request('PATCH', f'/api/orders/{order["id"]}', dict(status=[]))[0], 422)
        client = http.client.HTTPConnection('127.0.0.1', self.port, timeout=5)
        client.request('POST', '/api/orders', body='{', headers={'Content-Type': 'application/json'})
        self.assertEqual(client.getresponse().status, 400)
        client.close()


if __name__ == '__main__':
    unittest.main(verbosity=2)

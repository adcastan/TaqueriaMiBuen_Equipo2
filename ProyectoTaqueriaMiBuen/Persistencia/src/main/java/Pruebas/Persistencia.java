/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package Pruebas;

import Conexion.JPAUtil;
import DAO.CategoriaDAO;
import DAO.CuentaDAO;
import DAO.KdsDAO;
import DAO.MesaDAO;
import DAO.PedidoDAO;
import DAO.ProductoDAO;
import Entidades.Categoria;
import Entidades.Cuenta;
import Entidades.DatosEntrega;
import Entidades.DetalleExtra;
import Entidades.DetallePedido;
import Entidades.IngredienteExtra;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.PedidoDomicilio;
import Entidades.PedidoLocal;
import Entidades.PedidoParaLlevar;
import Entidades.Producto;
import Entidades.Usuario;
import Enums.Rol;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author joelr
 */
public class Persistencia {

    public static void main(String[] args) {
        CuentaDAO cuentaDAO = new CuentaDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        ProductoDAO productoDAO = new ProductoDAO();
        MesaDAO mesaDAO = new MesaDAO();
        PedidoDAO pedidoDAO = new PedidoDAO();
        KdsDAO kdsDAO = new KdsDAO();

        try {
            // 1. Registrar un usuario con su cuenta
            System.out.println("--- 1. Registrar usuario con su cuenta ---");
            Usuario usuario = new Usuario("Mesero de prueba", "6441234567", LocalDate.now());
            Cuenta cuenta = new Cuenta(usuario, "1234", Rol.MESERO);
            cuentaDAO.registrar(cuenta);
            System.out.println("Guardado: " + cuenta);
            System.out.println("Contrasena guardada (hash): " + cuenta.getContrasena());

            Cuenta cuentaLeida = cuentaDAO.buscarPorUsuario(usuario.getIdUsuario());
            System.out.println("PIN 1234 correcto: " + cuentaLeida.validarPin("1234"));
            System.out.println("PIN 9999 correcto: " + cuentaLeida.validarPin("9999"));

            // 2. Registrar categoria y producto (si ya existen se reutilizan)
            System.out.println("--- 2. Registrar categoria y producto ---");
            Categoria postres = categoriaDAO.buscarPorNombre("Postres");
            if (postres == null) {
                postres = new Categoria("Postres", 4);
                categoriaDAO.registrar(postres);
            }
            System.out.println("Categoria: " + postres);

            Producto flan = productoDAO.buscarPorNombre("Flan napolitano");
            if (flan == null) {
                flan = new Producto(postres, "Flan napolitano", "Porcion individual", 40.00);
                productoDAO.registrar(flan);
            }
            System.out.println("Producto: " + flan);

            System.out.println("Menu disponible:");
            for (Producto p : productoDAO.listarDisponibles()) {
                System.out.println("  [" + p.getCategoria().getNombre() + "] " + p.getNombre() + " $" + p.getPrecio());
            }
            List<Integer> idsProductos = new ArrayList<>();
            idsProductos.add(flan.getIdProducto());
            System.out.println("No disponibles de la lista: " + productoDAO.buscarNoDisponibles(idsProductos));

            // 3. Registrar una mesa y consultar las disponibles
            System.out.println("--- 3. Registrar mesa ---");
            if (mesaDAO.buscarPorNumero(9) == null) {
                mesaDAO.registrar(new Mesa(9, 6));
            }
            System.out.println("Mesa 9: " + mesaDAO.buscarPorNumero(9));
            List<Mesa> disponibles = mesaDAO.listarDisponibles();
            System.out.println("Mesas en total: " + mesaDAO.listar().size());
            System.out.println("Mesas disponibles: " + disponibles.size());
            Mesa mesa = disponibles.get(0);

            // 4. Registrar un pedido local
            System.out.println("--- 4. Registrar pedido local en la mesa " + mesa.getNumero() + " ---");
            Producto taco = productoDAO.buscarConExtras("Taco de asada");
            Producto coca = productoDAO.buscarPorNombre("Coca-Cola vidrio 355 ml");

            PedidoLocal pedidoLocal = new PedidoLocal(usuario, mesa, 2);
            DetallePedido tacos = new DetallePedido(taco, 3, "Sin cebolla");
            for (IngredienteExtra extra : taco.getExtras()) {
                if (extra.getNombre().equals("Queso extra")) {
                    tacos.getExtras().add(new DetalleExtra(tacos, extra, extra.getPrecio()));
                }
            }
            pedidoLocal.agregarDetalle(tacos);
            pedidoLocal.agregarDetalle(new DetallePedido(coca, 2, null));
            int idLocal = pedidoDAO.insertarPedido(pedidoLocal);

            mostrarPedido(pedidoDAO.buscarPorId(idLocal));
            System.out.println("Estado de la mesa: " + mesaDAO.buscarPorId(mesa.getIdMesa()).getEstado());

            // La mesa ya esta ocupada, otro pedido sin cuenta separada se debe rechazar
            PedidoLocal otro = new PedidoLocal(usuario, mesa, 1);
            otro.agregarDetalle(new DetallePedido(coca, 1, null));
            try {
                pedidoDAO.insertarPedido(otro);
                System.out.println("Error: se acepto un pedido en una mesa ocupada");
            } catch (RuntimeException e) {
                System.out.println("Rechazado como se esperaba: " + e.getMessage());
            }

            // 5. Registrar un pedido para llevar
            System.out.println("--- 5. Registrar pedido para llevar ---");
            PedidoParaLlevar paraLlevar = new PedidoParaLlevar(usuario, "Carlos");
            paraLlevar.agregarDetalle(new DetallePedido(productoDAO.buscarPorNombre("Quesadilla asada"), 1, null));
            pedidoDAO.insertarPedido(paraLlevar);
            mostrarPedido(pedidoDAO.buscarPorId(paraLlevar.getIdPedido()));

            // 6. Registrar un pedido a domicilio
            System.out.println("--- 6. Registrar pedido a domicilio ---");
            DatosEntrega datos = new DatosEntrega("Ana Lopez", "6449876543", "Calle Sufragio 123, Col. Campanario", "Casa azul");
            System.out.println("Datos de entrega validos: " + datos.validar());
            PedidoDomicilio domicilio = new PedidoDomicilio(usuario, 25.00, datos);
            domicilio.agregarDetalle(new DetallePedido(productoDAO.buscarPorNombre("Asada doble carne"), 2, null));
            pedidoDAO.insertarPedido(domicilio);

            PedidoDomicilio domicilioLeido = (PedidoDomicilio) pedidoDAO.buscarPorId(domicilio.getIdPedido());
            mostrarPedido(domicilioLeido);
            System.out.println("Entregar a: " + domicilioLeido.getDatosEntrega().getNombreCliente()
                    + ", " + domicilioLeido.getDatosEntrega().getDireccion());
            System.out.println("Total con envio: $" + domicilioLeido.calcularTotal());

            // 7. Agregar productos al pedido local
            System.out.println("--- 7. Agregar productos al pedido local ---");
            DetallePedido flanes = new DetallePedido(flan, 2, null);
            flanes.setPedido(pedidoLocal);
            List<DetallePedido> nuevos = new ArrayList<>();
            nuevos.add(flanes);
            System.out.println("Siguiente turno de cocina: " + pedidoDAO.siguienteOrdenCola());
            pedidoDAO.insertarDetalles(nuevos);
            mostrarPedido(pedidoDAO.buscarActivoPorMesa(mesa.getIdMesa()));

            // 8. Cola de cocina
            System.out.println("--- 8. Cola de cocina ---");
            List<DetallePedido> pendientes = kdsDAO.consultarPendientes();
            for (DetallePedido d : pendientes) {
                System.out.println("  " + d);
            }
            DetallePedido primero = pendientes.get(0);
            kdsDAO.marcarListo(primero.getIdDetalle());
            System.out.println("Listo el turno " + primero.getOrdenColaPedido()
                    + ", pendientes ahora: " + kdsDAO.consultarPendientes().size());
            kdsDAO.registrarLatido(1);
            System.out.println("Segundos desde el ultimo latido del KDS: " + kdsDAO.segundosDesdeUltimoLatido());

            // 9. Cerrar el pedido local
            System.out.println("--- 9. Cerrar pedido local ---");
            Pedido cerrado = pedidoDAO.cerrarPedido(pedidoLocal.getIdPedido());
            System.out.println("Pedido " + cerrado.getFolio() + " queda " + cerrado.getEstado()
                    + " con total $" + cerrado.getTotal());
            System.out.println("Pedidos activos: " + pedidoDAO.listarActivos().size());

            // 10. Cancelar el pedido para llevar
            System.out.println("--- 10. Cancelar pedido para llevar ---");
            System.out.println("El mesero puede autorizar: " + cuentaLeida.puedeAutorizarCancelacion());
            Pedido cancelado = pedidoDAO.cancelarPedido(paraLlevar.getIdPedido(), "El cliente ya no lo quiso", "Cajero de turno");
            System.out.println("Pedido " + cancelado.getFolio() + " queda " + cancelado.getEstado()
                    + ", motivo: " + cancelado.getCancelacion().getMotivo());
            System.out.println("Pendientes en cocina: " + kdsDAO.consultarPendientes().size());
            System.out.println("Pedidos activos: " + pedidoDAO.listarActivos().size());

            System.out.println("Prueba terminada sin errores.");
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            JPAUtil.cerrar();
        }
    }

    private static void mostrarPedido(Pedido pedido) {
        System.out.println(pedido);
        for (DetallePedido d : pedido.getDetalles()) {
            System.out.println("  " + d);
        }
        System.out.println("  Subtotal: $" + pedido.calcularSubtotal());
    }
}

package com.mycompany.negocio;

import Conexion.JPAUtil;
import DAO.CategoriaDAO;
import DAO.MesaDAO;
import DAO.ProductoDAO;
import DTO.DetalleDTO;
import DTO.PedidoDTO;
import DTO.ResultadoEnvio;
import Entidades.Categoria;
import Entidades.DetallePedido;
import Entidades.IngredienteExtra;
import Entidades.Mesa;
import Entidades.Pedido;
import Entidades.Producto;
import Entidades.Usuario;
import Enums.EstadoMesa;
import Enums.EstadoPedido;
import Enums.Rol;
import Exceptions.MesaOcupadaException;
import Exceptions.NegocioException;
import IAccesoDatos.ICategoriaDAO;
import IAccesoDatos.IMesaDAO;
import IAccesoDatos.IProductoDAO;
import IServicios.IAutenticacionService;
import IServicios.IKdsService;
import IServicios.IMenuService;
import IServicios.IPedidoService;
import Servicios.AutenticacionService;
import Servicios.KdsService;
import Servicios.MenuService;
import Servicios.PedidoService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author joelr
 */
public class Negocio {

    // Recorrido de prueba de la capa de negocio: se corre a mano desde NetBeans con la BD prendida
    public static void main(String[] args) {
        AutenticacionService autenticacion = new AutenticacionService();
        MenuService menuService = new MenuService();
        PedidoService pedidoService = new PedidoService();
        KdsService kdsService = new KdsService();

        ICategoriaDAO categoriaDAO = new CategoriaDAO();
        IProductoDAO productoDAO = new ProductoDAO();
        IMesaDAO mesaDAO = new MesaDAO();

        try {
            // 1. Registrar usuarios (si el PIN ya entra es que ya estaban registrados)
            System.out.println("--- 1. Registrar usuarios ---");
            try {
                autenticacion.iniciarSesion("5555");
                System.out.println("Los usuarios de prueba ya estaban registrados");
            } catch (NegocioException e) {
                autenticacion.registrarCuenta(new Usuario("Mesero de prueba", null, LocalDate.now()), "5555", Rol.MESERO);
                autenticacion.registrarCuenta(new Usuario("Cajero de prueba", null, LocalDate.now()), "6666", Rol.CAJERO);
                System.out.println("Registrados: mesero (PIN 5555) y cajero (PIN 6666)");
            }
            // Fallo: registrar otra cuenta con un PIN que ya existe
            try {
                autenticacion.registrarCuenta(new Usuario("Otro mesero", null, LocalDate.now()), "5555", Rol.MESERO);
            } catch (NegocioException e) {
                System.out.println("Fallo esperado: " + e.getMessage());
            }

            // 2. Registrar categoria y productos (si ya existen se reutilizan)
            System.out.println("--- 2. Registrar categoria y productos ---");
            Categoria categoria = categoriaDAO.buscarPorNombre("Pruebas de negocio");
            if (categoria == null) {
                categoria = new Categoria("Pruebas de negocio", 9);
                categoriaDAO.registrar(categoria);
            }
            if (productoDAO.buscarPorNombre("Taco de prueba") == null) {
                Producto nuevoTaco = new Producto(categoria, "Taco de prueba", "Taco con queso extra", 45.00);
                nuevoTaco.getExtras().add(new IngredienteExtra("Queso extra de prueba", 15.00, null));
                productoDAO.registrar(nuevoTaco);
            }
            if (productoDAO.buscarPorNombre("Refresco de prueba") == null) {
                productoDAO.registrar(new Producto(categoria, "Refresco de prueba", "Lata 355 ml", 35.00));
            }
            if (productoDAO.buscarPorNombre("Horchata de prueba") == null) {
                Producto horchata = new Producto(categoria, "Horchata de prueba", "Producto agotado", 30.00);
                horchata.setDisponible(false);
                productoDAO.registrar(horchata);
            }
            Producto taco = productoDAO.buscarConExtras("Taco de prueba");
            Producto refresco = productoDAO.buscarPorNombre("Refresco de prueba");
            Producto horchata = productoDAO.buscarPorNombre("Horchata de prueba");
            IngredienteExtra queso = taco.getExtras().get(0);
            System.out.println("Producto: " + taco + " con extra " + queso.getNombre());
            System.out.println("Producto: " + refresco);
            System.out.println("Producto: " + horchata);
            // Fallo: buscar un producto que no esta registrado
            System.out.println("Fallo esperado, producto que no existe: " + productoDAO.buscarPorNombre("Taco de nada"));

            // 3. AutenticacionService
            System.out.println("--- 3. AutenticacionService ---");
            Usuario mesero = autenticacion.iniciarSesion("5555");
            System.out.println("iniciarSesion(5555): " + mesero.getNombre());
            System.out.println("obtenerRol: " + autenticacion.obtenerRol(mesero));
            System.out.println("tieneRol MESERO: " + autenticacion.tieneRol(mesero, Rol.MESERO));
            Usuario cajero = autenticacion.validarAutorizacion("6666");
            System.out.println("validarAutorizacion(6666): " + cajero.getNombre());
            // Fallo: iniciar sesion con un PIN que no es de nadie
            try {
                autenticacion.iniciarSesion("9876");
            } catch (NegocioException e) {
                System.out.println("Fallo esperado: " + e.getMessage());
            }

            // 4. MenuService
            System.out.println("--- 4. MenuService ---");
            Map<Categoria, List<Producto>> menu = menuService.obtenerMenu();
            for (Map.Entry<Categoria, List<Producto>> entrada : menu.entrySet()) {
                System.out.println("  " + entrada.getKey().getNombre() + ": " + entrada.getValue().size() + " productos");
            }
            List<Integer> ids = new ArrayList<>();
            ids.add(taco.getIdProducto());
            ids.add(refresco.getIdProducto());
            System.out.println("validarDisponibilidad: " + menuService.validarDisponibilidad(ids));
            System.out.println("obtenerProducto: " + menuService.obtenerProducto(taco.getIdProducto()));
            // Fallo: la lista trae un producto agotado
            List<Integer> conAgotado = new ArrayList<>();
            conAgotado.add(refresco.getIdProducto());
            conAgotado.add(horchata.getIdProducto());
            System.out.println("Fallo esperado, validarDisponibilidad con horchata agotada: "
                    + menuService.validarDisponibilidad(conAgotado));

            // 5. PedidoService: crear un pedido local
            System.out.println("--- 5. Crear pedido ---");
            System.out.println("Mesas: " + pedidoService.obtenerMesas().size());
            Mesa mesa = mesaDAO.listarDisponibles().get(0);
            PedidoDTO dto = new PedidoDTO();
            dto.setTipo(PedidoDTO.LOCAL);
            dto.setIdMesa(mesa.getIdMesa());
            dto.setNumeroPersonas(2);
            DetalleDTO tacos = new DetalleDTO(taco.getIdProducto(), 3, "Sin cebolla");
            tacos.getIdsExtras().add(queso.getIdExtra());
            dto.getDetalles().add(tacos);
            dto.getDetalles().add(new DetalleDTO(refresco.getIdProducto(), 2, null));
            ResultadoEnvio resultado = pedidoService.crearPedido(dto, mesero);
            System.out.println("crearPedido: folio " + resultado.getFolio() + ", KDS conectado " + resultado.isKdsConectado());
            mostrarPedido(pedidoService.obtenerPedido(resultado.getIdPedido()));
            System.out.println("obtenerPedidoActivo(mesa " + mesa.getNumero() + "): "
                    + pedidoService.obtenerPedidoActivo(mesa.getIdMesa()).getFolio());
            System.out.println("obtenerPedidosActivos: " + pedidoService.obtenerPedidosActivos().size());
            // Fallo: abrir otro pedido en la misma mesa (ya esta ocupada)
            try {
                pedidoService.crearPedido(dto, mesero);
            } catch (NegocioException e) {
                System.out.println("Fallo esperado: " + e.getMessage());
            }

            // 6. KdsService
            System.out.println("--- 6. KdsService ---");
            List<DetallePedido> cola = kdsService.obtenerCola(1);
            System.out.println("obtenerCola: " + cola.size() + " pendientes");
            System.out.println("hayKdsConectado: " + kdsService.hayKdsConectado());
            kdsService.marcarListo(cola.get(0).getIdDetalle());
            System.out.println("marcarListo, pendientes ahora: " + kdsService.obtenerCola(1).size());
            // Fallo: marcar listo un detalle que no existe
            try {
                kdsService.marcarListo(99999);
            } catch (RuntimeException e) {
                System.out.println("Fallo esperado: No existe el detalle 99999");
            }

            // 7. Agregar productos y cerrar el pedido
            System.out.println("--- 7. Agregar productos y cerrar ---");
            List<DetalleDTO> nuevos = new ArrayList<>();
            nuevos.add(new DetalleDTO(refresco.getIdProducto(), 1, null));
            pedidoService.agregarDetalles(resultado.getIdPedido(), nuevos);
            mostrarPedido(pedidoService.obtenerPedido(resultado.getIdPedido()));
            Pedido cerrado = pedidoService.cerrarPedido(resultado.getIdPedido());
            System.out.println("cerrarPedido: " + cerrado.getEstado() + " con total $" + cerrado.getTotal());
            // Fallo: agregar productos a un pedido que ya se cerro
            try {
                pedidoService.agregarDetalles(resultado.getIdPedido(), nuevos);
            } catch (NegocioException e) {
                System.out.println("Fallo esperado: " + e.getMessage());
            }

            // 8. Cancelar el pedido (lo autoriza el cajero y la mesa se libera)
            System.out.println("--- 8. Cancelar pedido ---");
            // Fallo: el mesero no puede autorizar la cancelacion
            try {
                pedidoService.cancelarPedido(resultado.getIdPedido(), "Pedido de prueba", mesero);
            } catch (NegocioException e) {
                System.out.println("Fallo esperado: " + e.getMessage());
            }
            pedidoService.cancelarPedido(resultado.getIdPedido(), "Pedido de prueba", cajero);
            Pedido cancelado = pedidoService.obtenerPedido(resultado.getIdPedido());
            System.out.println("cancelarPedido: " + cancelado.getEstado()
                    + ", autorizo " + cancelado.getCancelacion().getAutorizadaPor());
            System.out.println("Estado de la mesa: " + mesaDAO.buscarPorId(mesa.getIdMesa()).getEstado());

            System.out.println("Prueba terminada sin errores.");
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            JPAUtil.cerrar();
        }
    }

    // Imprime un pedido con sus renglones para revisarlo en la consola
    private static void mostrarPedido(Pedido pedido) {
        System.out.println(pedido);
        for (DetallePedido d : pedido.getDetalles()) {
            System.out.println("  " + d);
        }
        System.out.println("  Subtotal: $" + pedido.calcularSubtotal());
    }
}

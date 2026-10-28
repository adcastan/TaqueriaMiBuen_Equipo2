package Servicios;

import DAO.CompraDAO;
import DAO.IngredienteDAO;
import DAO.ProductoDAO;
import DAO.ProveedorDAO;
import DTO.CompraDTO;
import DTO.DetalleCompraDTO;
import Entidades.Compra;
import Entidades.DetalleCompra;
import Entidades.Ingrediente;
import Entidades.Producto;
import Entidades.ProductoIngrediente;
import Entidades.Proveedor;
import Entidades.Usuario;
import Exceptions.NegocioException;
import IAccesoDatos.ICompraDAO;
import IAccesoDatos.IIngredienteDAO;
import IAccesoDatos.IProductoDAO;
import IAccesoDatos.IProveedorDAO;
import IServicios.IInsumoService;
import java.util.List;

/**
 * Servicio De Insumos..
 * @author Equipo 2
 */
public class InsumoService implements IInsumoService {

    private final IIngredienteDAO ingredienteDAO = new IngredienteDAO();
    private final ICompraDAO compraDAO = new CompraDAO();
    private final IProveedorDAO proveedorDAO = new ProveedorDAO();
    private final IProductoDAO productoDAO = new ProductoDAO();

    // Da de alta un ingrediente del almacen revisando sus datos
    @Override
    public Ingrediente registrarIngrediente(String nombre, String unidad, double existencia, double stockMinimo) {
        validar(nombre, unidad, stockMinimo);
        if (existencia < 0) {
            throw new NegocioException("La existencia no puede ser negativa");
        }
        if (ingredienteDAO.buscarPorNombre(nombre.trim()) != null) {
            throw new NegocioException("Ya existe un ingrediente con ese nombre");
        }
        Ingrediente ingrediente = new Ingrediente(nombre.trim(), unidad.trim(), existencia, stockMinimo);
        ingredienteDAO.registrar(ingrediente);
        return ingrediente;
    }

    // Cambia nombre, unidad o minimo de un ingrediente
    @Override
    public Ingrediente actualizarIngrediente(int idIngrediente, String nombre, String unidad, double stockMinimo) {
        Ingrediente ingrediente = obtenerIngrediente(idIngrediente);
        validar(nombre, unidad, stockMinimo);
        Ingrediente otro = ingredienteDAO.buscarPorNombre(nombre.trim());
        if (otro != null && !otro.getIdIngrediente().equals(idIngrediente)) {
            throw new NegocioException("Ya existe un ingrediente con ese nombre");
        }
        ingrediente.setNombre(nombre.trim());
        ingrediente.setUnidad(unidad.trim());
        ingrediente.setStockMinimo(stockMinimo);
        ingredienteDAO.actualizar(ingrediente);
        return ingrediente;
    }

    // Corrige a mano cuanto hay de un ingrediente (despues de contar el almacen)
    @Override
    public Ingrediente ajustarExistencia(int idIngrediente, double existencia) {
        if (existencia < 0) {
            throw new NegocioException("La existencia no puede ser negativa");
        }
        Ingrediente ingrediente = obtenerIngrediente(idIngrediente);
        ingrediente.setExistencia(existencia);
        ingredienteDAO.actualizar(ingrediente);
        return ingrediente;
    }

    // Todos los ingredientes del almacen
    @Override
    public List<Ingrediente> obtenerIngredientes() {
        return ingredienteDAO.listar();
    }

    // Ingredientes que ya estan en su minimo o abajo: hay que surtir
    @Override
    public List<Ingrediente> obtenerBajoStock() {
        return ingredienteDAO.listarBajoStock();
    }

    /**
     * Crea la compra con sus detalles. 
     * El DAO la guarda y sube la existencia de cada ingrediente en la misma transaccion.
     */
    @Override
    public Compra registrarCompra(CompraDTO dto, Usuario usuario) {
        if (dto == null || dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new NegocioException("La compra no tiene ingredientes");
        }
        if (usuario == null) {
            throw new NegocioException("Inicia sesion para registrar la compra");
        }
        Proveedor proveedor = proveedorDAO.buscarPorId(dto.getIdProveedor());
        if (proveedor == null) {
            throw new NegocioException("No existe el proveedor " + dto.getIdProveedor());
        }
        Compra compra = new Compra(proveedor, usuario);
        for (DetalleCompraDTO detalleDTO : dto.getDetalles()) {
            Ingrediente ingrediente = obtenerIngrediente(detalleDTO.getIdIngrediente());
            if (detalleDTO.getCostoUnitario() < 0) {
                throw new NegocioException("El costo de " + ingrediente.getNombre() + " no puede ser negativo");
            }
            try {
                compra.getDetalles().add(new DetalleCompra(compra, ingrediente,
                        detalleDTO.getCantidad(), detalleDTO.getCostoUnitario()));
            } catch (IllegalArgumentException e) {
                throw new NegocioException(e.getMessage());
            }
        }
        compraDAO.registrar(compra);
        return compra;
    }

    // Dice cuanto de un ingrediente lleva un producto
    @Override
    public ProductoIngrediente asignarReceta(int idProducto, int idIngrediente, double cantidad) {
        Producto producto = productoDAO.buscarPorId(idProducto);
        if (producto == null) {
            throw new NegocioException("No existe el producto " + idProducto);
        }
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad de la receta debe ser mayor a cero");
        }
        ProductoIngrediente receta = new ProductoIngrediente(producto, obtenerIngrediente(idIngrediente), cantidad);
        ingredienteDAO.registrarReceta(receta);
        return receta;
    }

    // La receta completa de un producto
    @Override
    public List<ProductoIngrediente> obtenerReceta(int idProducto) {
        return ingredienteDAO.listarReceta(idProducto);
    }

    // Busca el ingrediente y avisa con un mensaje claro si no existe
    private Ingrediente obtenerIngrediente(int idIngrediente) {
        Ingrediente ingrediente = ingredienteDAO.buscarPorId(idIngrediente);
        if (ingrediente == null) {
            throw new NegocioException("No existe el ingrediente " + idIngrediente);
        }
        return ingrediente;
    }

    // Nombre y unidad son obligatorios y el minimo no puede ser negativo
    private void validar(String nombre, String unidad, double stockMinimo) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Escribe el nombre del ingrediente");
        }
        if (unidad == null || unidad.isBlank()) {
            throw new NegocioException("Escribe la unidad (kg, pza, lt...)");
        }
        if (stockMinimo < 0) {
            throw new NegocioException("El stock minimo no puede ser negativo");
        }
    }
}

package IServicios;

import DTO.CompraDTO;
import Entidades.Compra;
import Entidades.Ingrediente;
import Entidades.ProductoIngrediente;
import Entidades.Usuario;
import java.util.List;

/**
 * Servicio de insumos y compras.
 * La implementa InsumoService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IInsumoService {

    // Registra un ingrediente nuevo en el inventario.
    public Ingrediente registrarIngrediente(String nombre, String unidad, double existencia, double stockMinimo);

    // Cambia nombre, unidad o stock minimo de un ingrediente.
    public Ingrediente actualizarIngrediente(int idIngrediente, String nombre, String unidad, double stockMinimo);

    // Corrige la existencia despues de contar el inventario.
    public Ingrediente ajustarExistencia(int idIngrediente, double existencia);

    // Todos los ingredientes.
    public List<Ingrediente> obtenerIngredientes();

    // Ingredientes que llegaron a su stock minimo.
    public List<Ingrediente> obtenerBajoStock();

    // Registra una compra a un proveedor y sube la existencia de lo comprado.
    public Compra registrarCompra(CompraDTO dto, Usuario usuario);

    // Indica cuanto de un ingrediente lleva un producto.
    public ProductoIngrediente asignarReceta(int idProducto, int idIngrediente, double cantidad);

    // Ingredientes que lleva un producto.
    public List<ProductoIngrediente> obtenerReceta(int idProducto);
}

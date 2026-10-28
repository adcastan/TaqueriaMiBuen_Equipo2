package IAccesoDatos;

import Entidades.Producto;
import java.util.List;

/**
 * Operaciones de base de datos de los productos del menu. 
 * La implementa ProductoDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 * @author Equipo 2
 */
public interface IProductoDAO {

    // Guarda un producto nuevo.
    void registrar(Producto producto);

    // Producto por nombre o null.
    Producto buscarPorNombre(String nombre);

    // Producto por nombre con sus extras cargados.
    Producto buscarConExtras(String nombre);

    // Productos que se pueden vender.
    List<Producto> listarDisponibles();

    // De la lista de ids, los que estan agotados.
    List<Integer> buscarNoDisponibles(List<Integer> ids);

    // Producto por id con sus extras cargados.
    Producto buscarPorId(int idProducto);

    // Todo el menu con sus extras.
    List<Producto> listarConExtras();
    
    // Guarda los cambios de un producto (nombre, precio, categoria y extras).
    public void actualizar(Producto producto);

    // Marca el producto como disponible o agotado.
    public void cambiarDisponible(int idProducto, boolean disponible);
}

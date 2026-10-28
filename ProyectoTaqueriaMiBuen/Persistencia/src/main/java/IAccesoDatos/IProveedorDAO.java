package IAccesoDatos;

import Entidades.Proveedor;
import java.util.List;

/**
 * Operaciones de base de datos de los proveedores.
 * La implementa ProveedorDAO.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IProveedorDAO {

    // Guarda un proveedor nuevo.
    public void registrar(Proveedor proveedor);

    // Guarda los cambios de un proveedor.
    public void actualizar(Proveedor proveedor);

    // Borra un proveedor que no tenga compras.
    public void eliminar(int idProveedor);

    // Proveedor por su id o null.
    public Proveedor buscarPorId(int idProveedor);

    // Proveedor por su nombre o null.
    public Proveedor buscarPorNombre(String nombre);

    // Todos los proveedores en orden alfabetico.
    public List<Proveedor> listar();
}


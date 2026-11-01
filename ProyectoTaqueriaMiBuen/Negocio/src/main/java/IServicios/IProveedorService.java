package IServicios;

import Entidades.Proveedor;
import java.util.List;

/**
 * Servicio de proveedores.
 * La implementa ProveedorService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IProveedorService {

    // Registra un proveedor nuevo.
    public Proveedor registrarProveedor(String nombre, String telefono, String correo, String queSurte);

    // Cambia los datos de un proveedor.
    public Proveedor actualizarProveedor(int idProveedor, String nombre, String telefono, String correo, String queSurte);

    // Da de baja un proveedor (baja logica, no se borra).
    public void darDeBajaProveedor(int idProveedor);

    // Todos los proveedores.
    public List<Proveedor> obtenerProveedores();

    // Proveedor por su id.
    public Proveedor obtenerProveedor(int idProveedor);
}


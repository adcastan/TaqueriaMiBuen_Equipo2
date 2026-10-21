/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
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
    public Proveedor registrarProveedor(String nombre, String telefono, String correo);

    // Cambia los datos de un proveedor.
    public Proveedor actualizarProveedor(int idProveedor, String nombre, String telefono, String correo);

    // Elimina un proveedor que no tenga compras.
    public void eliminarProveedor(int idProveedor);

    // Todos los proveedores.
    public List<Proveedor> obtenerProveedores();

    // Proveedor por su id.
    public Proveedor obtenerProveedor(int idProveedor);
}


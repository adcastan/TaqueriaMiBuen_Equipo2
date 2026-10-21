/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.CompraDAO;
import DAO.ProveedorDAO;
import Entidades.Proveedor;
import Exceptions.NegocioException;
import IAccesoDatos.ICompraDAO;
import IAccesoDatos.IProveedorDAO;
import IServicios.IProveedorService;
import java.util.List;

/**
 * Servicio de Proveedor.
 * @author Equipo 2
 */
public class ProveedorService implements IProveedorService {

    private final IProveedorDAO proveedorDAO = new ProveedorDAO();
    private final ICompraDAO compraDAO = new CompraDAO();

    @Override
    public Proveedor registrarProveedor(String nombre, String telefono, String correo) {
        validar(nombre, telefono, correo);
        if (proveedorDAO.buscarPorNombre(nombre.trim()) != null) {
            throw new NegocioException("Ya existe un proveedor con ese nombre");
        }
        Proveedor proveedor = new Proveedor(nombre.trim(), vacioANulo(telefono), vacioANulo(correo));
        proveedorDAO.registrar(proveedor);
        return proveedor;
    }

    @Override
    public Proveedor actualizarProveedor(int idProveedor, String nombre, String telefono, String correo) {
        Proveedor proveedor = obtenerProveedor(idProveedor);
        validar(nombre, telefono, correo);
        Proveedor otro = proveedorDAO.buscarPorNombre(nombre.trim());
        if (otro != null && !otro.getIdProveedor().equals(idProveedor)) {
            throw new NegocioException("Ya existe un proveedor con ese nombre");
        }
        proveedor.setNombre(nombre.trim());
        proveedor.setTelefono(vacioANulo(telefono));
        proveedor.setCorreo(vacioANulo(correo));
        proveedorDAO.actualizar(proveedor);
        return proveedor;
    }

    @Override
    public void eliminarProveedor(int idProveedor) {
        obtenerProveedor(idProveedor);
        if (compraDAO.contarPorProveedor(idProveedor) > 0) {
            throw new NegocioException("No se puede eliminar un proveedor que ya tiene compras");
        }
        proveedorDAO.eliminar(idProveedor);
    }

    @Override
    public List<Proveedor> obtenerProveedores() {
        return proveedorDAO.listar();
    }

    @Override
    public Proveedor obtenerProveedor(int idProveedor) {
        Proveedor proveedor = proveedorDAO.buscarPorId(idProveedor);
        if (proveedor == null) {
            throw new NegocioException("No existe el proveedor " + idProveedor);
        }
        return proveedor;
    }

    // Validaciones dwe nombre, telefono y correo.
    private void validar(String nombre, String telefono, String correo) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Escribe el nombre del proveedor");
        }
        if (telefono != null && !telefono.isBlank() && !telefono.trim().matches("\\d{10}")) {
            throw new NegocioException("El telefono debe tener 10 digitos");
        }
        if (correo != null && !correo.isBlank() && !correo.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new NegocioException("El correo no es valido");
        }
    }

    private String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

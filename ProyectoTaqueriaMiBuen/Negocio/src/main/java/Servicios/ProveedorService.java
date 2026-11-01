package Servicios;

import DAO.ProveedorDAO;
import Entidades.Proveedor;
import Exceptions.NegocioException;
import IAccesoDatos.IProveedorDAO;
import IServicios.IProveedorService;
import java.util.List;

/**
 * Servicio de Proveedores (CU-06).
 * Registra, modifica, da de baja y consulta proveedores. Un proveedor no se
 * borra: si ya no se le compra se da de baja (activo = false) para no perder
 * sus compras anteriores.
 *
 * @author Equipo 2
 */
public class ProveedorService implements IProveedorService {

    private final IProveedorDAO proveedorDAO = new ProveedorDAO();

    // Da de alta un proveedor revisando sus datos
    @Override
    public Proveedor registrarProveedor(String nombre, String telefono, String correo, String queSurte) {
        validar(nombre, telefono, correo);
        if (proveedorDAO.buscarPorNombre(nombre.trim()) != null) {
            throw new NegocioException("Ya existe un proveedor con ese nombre");
        }
        Proveedor proveedor = new Proveedor(nombre.trim(), vacioANulo(telefono), vacioANulo(correo), vacioANulo(queSurte));
        proveedorDAO.registrar(proveedor);
        return proveedor;
    }

    // Cambia los datos de un proveedor
    @Override
    public Proveedor actualizarProveedor(int idProveedor, String nombre, String telefono, String correo, String queSurte) {
        Proveedor proveedor = obtenerProveedor(idProveedor);
        validar(nombre, telefono, correo);
        Proveedor otro = proveedorDAO.buscarPorNombre(nombre.trim());
        if (otro != null && !otro.getIdProveedor().equals(idProveedor)) {
            throw new NegocioException("Ya existe un proveedor con ese nombre");
        }
        proveedor.setNombre(nombre.trim());
        proveedor.setTelefono(vacioANulo(telefono));
        proveedor.setCorreo(vacioANulo(correo));
        proveedor.setQueSurte(vacioANulo(queSurte));
        proveedorDAO.actualizar(proveedor);
        return proveedor;
    }

    // Baja logica: ya no sale como opcion para comprar, pero sus compras se conservan
    @Override
    public void darDeBajaProveedor(int idProveedor) {
        Proveedor proveedor = obtenerProveedor(idProveedor);
        proveedor.setActivo(false);
        proveedorDAO.actualizar(proveedor);
    }

    // Todos los proveedores
    @Override
    public List<Proveedor> obtenerProveedores() {
        return proveedorDAO.listar();
    }

    // Busca un proveedor y avisa con un mensaje claro si no existe
    @Override
    public Proveedor obtenerProveedor(int idProveedor) {
        Proveedor proveedor = proveedorDAO.buscarPorId(idProveedor);
        if (proveedor == null) {
            throw new NegocioException("No existe el proveedor " + idProveedor);
        }
        return proveedor;
    }

    // Validaciones de nombre, telefono y correo.
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

    // Un texto vacio se guarda como null, para no llenar la base de espacios
    private String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

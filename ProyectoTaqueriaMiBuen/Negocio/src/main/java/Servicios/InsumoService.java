package Servicios;

import DAO.CompraDAO;
import DAO.IngredienteDAO;
import DAO.ProveedorDAO;
import DTO.CompraDTO;
import DTO.DetalleCompraDTO;
import Entidades.Compra;
import Entidades.DetalleCompra;
import Entidades.Ingrediente;
import Entidades.Proveedor;
import Entidades.Usuario;
import Exceptions.NegocioException;
import IAccesoDatos.ICompraDAO;
import IAccesoDatos.IIngredienteDAO;
import IAccesoDatos.IProveedorDAO;
import IServicios.IInsumoService;
import java.util.List;

/**
 * Servicio de Insumos (CU-07) y registro de compras (CU-06).
 *
 * Los insumos solo se llevan como catalogo: el cliente decidio no llevar
 * existencias, asi que aqui ya no hay stock minimo, ajustes de inventario ni
 * recetas por producto. Se registra el insumo, se modifica su ficha, se da de
 * baja y se consulta la lista.
 *
 * La compra se guarda tal cual (de contado o a credito, con proveedor o como
 * compra directa). Los abonos a compras a credito los hace el siguiente equipo.
 *
 * @author Equipo 2
 */
public class InsumoService implements IInsumoService {

    private final IIngredienteDAO ingredienteDAO = new IngredienteDAO();
    private final ICompraDAO compraDAO = new CompraDAO();
    private final IProveedorDAO proveedorDAO = new ProveedorDAO();

    // Da de alta un insumo revisando sus datos; el proveedor habitual es opcional
    @Override
    public Ingrediente registrarIngrediente(String nombre, String unidad, Integer idProveedor) {
        validar(nombre, unidad);
        if (ingredienteDAO.buscarPorNombre(nombre.trim()) != null) {
            throw new NegocioException("Ya existe un insumo con ese nombre");
        }
        Ingrediente ingrediente = new Ingrediente(nombre.trim(), unidad.trim(), buscarProveedor(idProveedor));
        ingredienteDAO.registrar(ingrediente);
        return ingrediente;
    }

    // Cambia la ficha del insumo: nombre, unidad y proveedor habitual
    @Override
    public Ingrediente actualizarIngrediente(int idIngrediente, String nombre, String unidad, Integer idProveedor) {
        Ingrediente ingrediente = obtenerIngrediente(idIngrediente);
        validar(nombre, unidad);
        Ingrediente otro = ingredienteDAO.buscarPorNombre(nombre.trim());
        if (otro != null && !otro.getIdIngrediente().equals(idIngrediente)) {
            throw new NegocioException("Ya existe un insumo con ese nombre");
        }
        ingrediente.setNombre(nombre.trim());
        ingrediente.setUnidad(unidad.trim());
        ingrediente.setProveedor(buscarProveedor(idProveedor));
        ingredienteDAO.actualizar(ingrediente);
        return ingrediente;
    }

    // Baja logica: el insumo ya no sale en la lista pero no se borra (hay compras que lo usan)
    @Override
    public void darDeBajaIngrediente(int idIngrediente) {
        Ingrediente ingrediente = obtenerIngrediente(idIngrediente);
        ingrediente.setActivo(false);
        ingredienteDAO.actualizar(ingrediente);
    }

    // Insumos que siguen en uso, en orden alfabetico
    @Override
    public List<Ingrediente> obtenerIngredientes() {
        return ingredienteDAO.listarActivos();
    }

    /**
     * Guarda la compra con sus renglones. Sin proveedor es una compra directa
     * (mercado, super). Si es a credito queda PENDIENTE; si no, PAGADA.
     */
    @Override
    public Compra registrarCompra(CompraDTO dto, Usuario usuario) {
        if (dto == null || dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new NegocioException("La compra no tiene insumos");
        }
        if (usuario == null) {
            throw new NegocioException("Inicia sesion para registrar la compra");
        }
        Compra compra = new Compra(buscarProveedor(dto.getIdProveedor()), usuario);
        compra.setACredito(dto.isACredito());
        compra.setNota(dto.getNota());
        // A credito sin proveedor no tiene sentido: no hay a quien pagarle despues
        if (dto.isACredito() && compra.getProveedor() == null) {
            throw new NegocioException("Una compra a credito necesita proveedor");
        }
        for (DetalleCompraDTO detalleDTO : dto.getDetalles()) {
            Ingrediente ingrediente = obtenerIngrediente(detalleDTO.getIdIngrediente());
            try {
                compra.getDetalles().add(new DetalleCompra(compra, ingrediente,
                        detalleDTO.getCantidad(), detalleDTO.getCostoUnitario()));
            } catch (IllegalArgumentException e) {
                throw new NegocioException(ingrediente.getNombre() + ": " + e.getMessage());
            }
        }
        compraDAO.registrar(compra);
        return compra;
    }

    // Busca el insumo y avisa con un mensaje claro si no existe
    private Ingrediente obtenerIngrediente(int idIngrediente) {
        Ingrediente ingrediente = ingredienteDAO.buscarPorId(idIngrediente);
        if (ingrediente == null) {
            throw new NegocioException("No existe el insumo " + idIngrediente);
        }
        return ingrediente;
    }

    // null si no se eligio proveedor; si se eligio, debe existir
    private Proveedor buscarProveedor(Integer idProveedor) {
        if (idProveedor == null) {
            return null;
        }
        Proveedor proveedor = proveedorDAO.buscarPorId(idProveedor);
        if (proveedor == null) {
            throw new NegocioException("No existe el proveedor " + idProveedor);
        }
        return proveedor;
    }

    // Nombre y unidad son obligatorios
    private void validar(String nombre, String unidad) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Escribe el nombre del insumo");
        }
        if (unidad == null || unidad.isBlank()) {
            throw new NegocioException("Escribe la unidad (kg, pza, lt...)");
        }
    }
}

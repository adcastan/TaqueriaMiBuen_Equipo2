/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.CategoriaDAO;
import DAO.IngredienteDAO;
import DAO.IngredienteExtraDAO;
import DAO.ProductoDAO;
import DTO.ProductoDTO;
import Entidades.Categoria;
import Entidades.Ingrediente;
import Entidades.IngredienteExtra;
import Entidades.Producto;
import Exceptions.NegocioException;
import IAccesoDatos.ICategoriaDAO;
import IAccesoDatos.IIngredienteDAO;
import IAccesoDatos.IIngredienteExtraDAO;
import IAccesoDatos.IProductoDAO;
import IServicios.IMenuService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta del menu para la toma de pedido (control Consultar Menu).
 *
 * @author Equipo 2
 */
public class MenuService implements IMenuService {

    private final ICategoriaDAO categoriaDAO = new CategoriaDAO();
    private final IProductoDAO productoDAO = new ProductoDAO();
    private final IIngredienteExtraDAO extraDAO = new IngredienteExtraDAO();
    private final IIngredienteDAO ingredienteDAO = new IngredienteDAO();

    /**
     * Regresa cada categoria con sus productos, en el orden del menu. Se
     * incluyen los agotados para que la pantalla los muestre deshabilitados.
     * @return 
     */
    @Override
    public Map<Categoria, List<Producto>> obtenerMenu() {
        Map<Categoria, List<Producto>> menu = new LinkedHashMap<>();
        List<Categoria> categorias = categoriaDAO.listar();
        for (Categoria categoria : categorias) {
            menu.put(categoria, new ArrayList<>());
        }
        //Se acomoda cada producto en la lista de su categoria
        for (Producto producto : productoDAO.listarConExtras()) {
            for (Categoria categoria : categorias) {
                if (categoria.getIdCategoria().equals(producto.getCategoria().getIdCategoria())) {
                    menu.get(categoria).add(producto);
                }
            }
        }
        return menu;
    }

    /**
     * Regresa true si todos los productos de la lista siguen disponibles.
     * @param idsProductos
     * @return 
     */
    @Override
    public boolean validarDisponibilidad(List<Integer> idsProductos) {
        return productoDAO.buscarNoDisponibles(idsProductos).isEmpty();
    }

    @Override
    public Producto obtenerProducto(int idProducto) {
        return productoDAO.buscarPorId(idProducto);
    }

    @Override
    public List<Categoria> obtenerCategorias() {
        return categoriaDAO.listar();
    }

    @Override
    public Categoria registrarCategoria(String nombre, int orden) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Escribe el nombre de la categoria");
        }
        if (categoriaDAO.buscarPorNombre(nombre.trim()) != null) {
            throw new NegocioException("Ya existe la categoria " + nombre.trim());
        }
        Categoria categoria = new Categoria(nombre.trim(), orden);
        categoriaDAO.registrar(categoria);
        return categoria;
    }

    /**
     * Primero se guarda el producto y despues se le ponen los extras que ya
     * existen, asi JPA no intenta registrar otra vez esos extras.
     * @param dto
     * @return 
     */
    @Override
    public Producto registrarProducto(ProductoDTO dto) {
        validarProducto(dto);
        Categoria categoria = obtenerCategoria(dto.getIdCategoria());
        Producto producto = new Producto(categoria, dto.getNombre().trim(), dto.getDescripcion(), dto.getPrecio());
        producto.setDisponible(dto.isDisponible());
        productoDAO.registrar(producto);
        List<IngredienteExtra> extras = buscarExtras(dto.getIdsExtras());
        if (!extras.isEmpty()) {
            producto.setExtras(extras);
            productoDAO.actualizar(producto);
        }
        return producto;
    }

    @Override
    public Producto actualizarProducto(int idProducto, ProductoDTO dto) {
        Producto producto = productoDAO.buscarPorId(idProducto);
        if (producto == null) {
            throw new NegocioException("No existe el producto " + idProducto);
        }
        validarProducto(dto);
        producto.setCategoria(obtenerCategoria(dto.getIdCategoria()));
        producto.setNombre(dto.getNombre().trim());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecio(dto.getPrecio());
        producto.setDisponible(dto.isDisponible());
        producto.setExtras(buscarExtras(dto.getIdsExtras()));
        productoDAO.actualizar(producto);
        return producto;
    }

    @Override
    public void cambiarDisponibilidad(int idProducto, boolean disponible) {
        if (productoDAO.buscarPorId(idProducto) == null) {
            throw new NegocioException("No existe el producto " + idProducto);
        }
        productoDAO.cambiarDisponible(idProducto, disponible);
    }

    @Override
    public IngredienteExtra registrarExtra(String nombre, double precio, Integer idIngrediente) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Escribe el nombre del extra");
        }
        if (precio < 0) {
            throw new NegocioException("El precio del extra no puede ser negativo");
        }
        Ingrediente ingrediente = null;
        if (idIngrediente != null) {
            ingrediente = ingredienteDAO.buscarPorId(idIngrediente);
            if (ingrediente == null) {
                throw new NegocioException("No existe el ingrediente " + idIngrediente);
            }
        }
        IngredienteExtra extra = new IngredienteExtra(nombre.trim(), precio, ingrediente);
        extraDAO.registrar(extra);
        return extra;
    }

    @Override
    public List<IngredienteExtra> obtenerExtras() {
        return extraDAO.listar();
    }

    private void validarProducto(ProductoDTO dto) {
        if (dto == null || dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new NegocioException("Escribe el nombre del producto");
        }
        if (dto.getPrecio() <= 0) {
            throw new NegocioException("El precio debe ser mayor a cero");
        }
    }

    private Categoria obtenerCategoria(int idCategoria) {
        Categoria categoria = categoriaDAO.buscarPorId(idCategoria);
        if (categoria == null) {
            throw new NegocioException("No existe la categoria " + idCategoria);
        }
        return categoria;
    }

    private List<IngredienteExtra> buscarExtras(List<Integer> ids) {
        List<IngredienteExtra> extras = new ArrayList<>();
        if (ids != null) {
            for (Integer idExtra : ids) {
                IngredienteExtra extra = extraDAO.buscarPorId(idExtra);
                if (extra == null) {
                    throw new NegocioException("No existe el extra " + idExtra);
                }
                extras.add(extra);
            }
        }
        return extras;
    }

}

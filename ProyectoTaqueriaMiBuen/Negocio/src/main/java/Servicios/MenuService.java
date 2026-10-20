/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.CategoriaDAO;
import DAO.ProductoDAO;
import Entidades.Categoria;
import Entidades.Producto;
import IAccesoDatos.ICategoriaDAO;
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
public class MenuService implements IMenuService{

    private final ICategoriaDAO categoriaDAO = new CategoriaDAO();
    private final IProductoDAO productoDAO = new ProductoDAO();

    /**
     * Regresa cada categoria con sus productos, en el orden del menu. 
     * Se incluyen los agotados para que la pantalla los muestre deshabilitados.
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
     */
    @Override
    public boolean validarDisponibilidad(List<Integer> idsProductos) {
        return productoDAO.buscarNoDisponibles(idsProductos).isEmpty();
    }

    @Override
    public Producto obtenerProducto(int idProducto) {
        return productoDAO.buscarPorId(idProducto);
    }
}

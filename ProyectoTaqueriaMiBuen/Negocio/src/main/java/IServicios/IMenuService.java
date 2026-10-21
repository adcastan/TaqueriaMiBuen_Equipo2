/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import DTO.ProductoDTO;
import Entidades.Categoria;
import Entidades.IngredienteExtra;
import Entidades.Producto;
import java.util.List;
import java.util.Map;

/**
 * Servicio de consulta del menu. La implementa MenuService. Las demas capas
 * usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IMenuService {

    // Categorias con sus productos en el orden del menu.
    public Map<Categoria, List<Producto>> obtenerMenu();

    // Regresa true si todos los productos siguen disponibles.
    public boolean validarDisponibilidad(List<Integer> idsProductos);

    // Obtiene un producto con sus extras mediante su ID.
    public Producto obtenerProducto(int idProducto);

    // Categorias del menu en orden.
    public List<Categoria> obtenerCategorias();

    // Registra una categoria nueva (CU-01 Administrar Menu).
    public Categoria registrarCategoria(String nombre, int orden);

    // Registra un producto nuevo con sus extras.
    public Producto registrarProducto(ProductoDTO dto);

    // Cambia los datos de un producto.
    public Producto actualizarProducto(int idProducto, ProductoDTO dto);

    // Marca un producto como disponible o agotado.
    public void cambiarDisponibilidad(int idProducto, boolean disponible);

    // Registra un ingrediente extra que se puede agregar a los productos.
    public IngredienteExtra registrarExtra(String nombre, double precio, Integer idIngrediente);

    // Todos los ingredientes extra.
    public List<IngredienteExtra> obtenerExtras();
}

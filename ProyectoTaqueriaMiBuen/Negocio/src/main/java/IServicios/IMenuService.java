/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import Entidades.Categoria;
import Entidades.Producto;
import java.util.List;
import java.util.Map;

/**
 * Servicio de consulta del menu. 
 * La implementa MenuService. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 * @author Equipo 2
 */
public interface IMenuService {

    // Categorias con sus productos en el orden del menu.
    public Map<Categoria, List<Producto>> obtenerMenu();

    // Regresa true si todos los productos siguen disponibles.
    public boolean validarDisponibilidad(List<Integer> idsProductos);

    // Obtiene un producto con sus extras mediante su ID.
    public Producto obtenerProducto(int idProducto);
}

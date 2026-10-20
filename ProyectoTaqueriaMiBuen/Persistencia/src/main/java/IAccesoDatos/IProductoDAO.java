/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.Producto;
import java.util.List;

/**
 * Operaciones de base de datos de los productos del menu. 
 * La implementa ProductoDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 * @author Equipo 2
 */
public interface IProductoDAO {

    // Guarda un producto nuevo.
    void registrar(Producto producto);

    // Producto por nombre o null.
    Producto buscarPorNombre(String nombre);

    // Producto por nombre con sus extras cargados.
    Producto buscarConExtras(String nombre);

    // Productos que se pueden vender.
    List<Producto> listarDisponibles();

    // De la lista de ids, los que estan agotados.
    List<Integer> buscarNoDisponibles(List<Integer> ids);

    // Producto por id con sus extras cargados.
    Producto buscarPorId(int idProducto);

    // Todo el menu con sus extras.
    List<Producto> listarConExtras();
}

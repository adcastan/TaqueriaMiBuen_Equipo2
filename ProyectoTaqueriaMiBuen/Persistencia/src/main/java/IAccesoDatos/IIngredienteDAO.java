/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.Ingrediente;
import Entidades.ProductoIngrediente;
import java.util.List;

/**
 * Operaciones de base de datos de los ingredientes (insumos) y las recetas.
 * La implementa IngredienteDAO.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IIngredienteDAO {

    // Guarda un ingrediente nuevo.
    public void registrar(Ingrediente ingrediente);

    // Guarda los cambios de un ingrediente.
    public void actualizar(Ingrediente ingrediente);

    // Ingrediente por su id o null.
    public Ingrediente buscarPorId(int idIngrediente);

    // Ingrediente por su nombre o null.
    public Ingrediente buscarPorNombre(String nombre);

    // Todos los ingredientes en orden alfabetico.
    public List<Ingrediente> listar();

    // Ingredientes con existencia igual o menor a su stock minimo.
    public List<Ingrediente> listarBajoStock();

    // Guarda cuanto de un ingrediente usa un producto (receta).
    public void registrarReceta(ProductoIngrediente receta);

    // Ingredientes que usa un producto.
    public List<ProductoIngrediente> listarReceta(int idProducto);
}

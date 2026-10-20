/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.Categoria;
import java.util.List;

/**
 * Operaciones de base de datos de las categorias del menu.
 * La implementa CategoriaDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface ICategoriaDAO {

    // Guarda una categoria nueva.
    public void registrar(Categoria categoria);

    // Regresa la categoria con ese nombre o null.
    public Categoria buscarPorNombre(String nombre);

    // Todas las categorias en el orden del menu.
    public List<Categoria> listar();
}
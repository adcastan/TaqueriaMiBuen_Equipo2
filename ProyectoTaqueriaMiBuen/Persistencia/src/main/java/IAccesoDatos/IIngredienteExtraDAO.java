/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.IngredienteExtra;
import java.util.List;

/**
 * Operaciones de base de datos de los ingredientes extra del menu.
 * La implementa IngredienteExtraDAO.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IIngredienteExtraDAO {

    // Guarda un extra nuevo.
    public void registrar(IngredienteExtra extra);

    // Guarda los cambios de un extra.
    public void actualizar(IngredienteExtra extra);

    // Extra por su id o null.
    public IngredienteExtra buscarPorId(int idExtra);

    // Todos los extras en orden alfabetico.
    public List<IngredienteExtra> listar();
}


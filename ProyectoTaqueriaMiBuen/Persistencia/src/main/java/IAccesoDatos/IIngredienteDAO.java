package IAccesoDatos;

import Entidades.Ingrediente;
import java.util.List;

/**
 * Operaciones de base de datos de los ingredientes (insumos). Solo catalogo.
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

    // Insumos que siguen en uso, en orden alfabetico.
    public List<Ingrediente> listarActivos();
}

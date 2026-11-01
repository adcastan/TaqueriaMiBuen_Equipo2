package IAccesoDatos;

import Entidades.Usuario;
import java.util.List;

/**
 * Operaciones de base de datos de los usuarios (empleados).
 * Los usuarios nuevos se guardan junto con su cuenta en ICuentaDAO.
 * La implementa UsuarioDAO.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IUsuarioDAO {

    // Usuario por su id o null.
    public Usuario buscarPorId(int idUsuario);

    // Todos los usuarios, activos y dados de baja.
    public List<Usuario> listar();

    // Guarda los cambios de nombre o telefono.
    public void actualizar(Usuario usuario);

    // Da de baja (false) o reactiva (true) a un usuario.
    public void cambiarActivo(int idUsuario, boolean activo);
}


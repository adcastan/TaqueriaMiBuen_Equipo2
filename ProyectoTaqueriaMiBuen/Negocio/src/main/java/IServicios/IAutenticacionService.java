package IServicios;

import Entidades.Cuenta;
import Entidades.Usuario;
import Enums.Rol;

/**
 * Servicio de inicio de sesion y roles. 
 * La implementa AutenticacionService. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IAutenticacionService {

    // Inicia Sesion, lanza error si no existe.
    public Usuario iniciarSesion(String pin);

    // Verifica rol, true si el usuario tiene ese rol.
    public boolean tieneRol(Usuario usuario, Rol rol);

    // Obtiene el rol de la cuenta del usuario.
    public Rol obtenerRol(Usuario usuario);

    // Valida la autorizacion del Usuario.
    public Usuario validarAutorizacion(String pin);

    // Registra una cuenta nueva. El PIN no se puede repetir porque se entra solo con el PIN.
    public Cuenta registrarCuenta(Usuario usuario, String pin, Rol rol);
}

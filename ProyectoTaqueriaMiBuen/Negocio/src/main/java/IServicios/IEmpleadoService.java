/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import Entidades.Cuenta;
import Entidades.Usuario;
import Enums.Rol;
import java.util.List;

/**
 * Servicio de empleados. La implementa EmpleadoService. Las demas capas usan
 * esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IEmpleadoService {

    // Registra un empleado con su cuenta, PIN y rol.
    public Cuenta registrarEmpleado(String nombre, String telefono, String pin, Rol rol);

    // Todas las cuentas de empleados con su usuario.
    public List<Cuenta> obtenerEmpleados();

    // Cambia nombre o telefono de un empleado.
    public Usuario actualizarDatos(int idUsuario, String nombre, String telefono);

    // Cambia el rol de un empleado.
    public void cambiarRol(int idUsuario, Rol rol);

    // Da de baja a un empleado, ya no puede iniciar sesion.
    public void darDeBaja(int idUsuario);

    // Vuelve a activar a un empleado dado de baja.
    public void reactivar(int idUsuario);
}

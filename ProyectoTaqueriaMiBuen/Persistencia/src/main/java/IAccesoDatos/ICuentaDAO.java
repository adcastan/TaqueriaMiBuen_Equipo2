/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.Cuenta;
import Enums.Rol;
import java.util.List;

/**
 * Operaciones de base de datos de las cuentas de usuario. 
 * La implementa CuentaDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface ICuentaDAO {

    // Guarda la cuenta y su usuario.
    public void registrar(Cuenta cuenta);

    // Cuenta de un usuario o null.
    public Cuenta buscarPorUsuario(int idUsuario);

    // Cuentas de usuarios activos, se usan para validar el PIN.
    public List<Cuenta> buscarActivas();
    
    // Todas las cuentas con su usuario, activos y dados de baja.
    public List<Cuenta> listar();

    // Cambia el rol de la cuenta de un usuario.
    public void actualizarRol(int idUsuario, Rol rol);
}

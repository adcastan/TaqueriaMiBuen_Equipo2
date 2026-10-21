/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.CuentaDAO;
import DAO.UsuarioDAO;
import Entidades.Cuenta;
import Entidades.Usuario;
import Enums.Rol;
import Exceptions.NegocioException;
import IAccesoDatos.ICuentaDAO;
import IAccesoDatos.IUsuarioDAO;
import IServicios.IAutenticacionService;
import IServicios.IEmpleadoService;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de Empleado.
 * @author Equipo 2
 */
public class EmpleadoService implements IEmpleadoService {

    private final ICuentaDAO cuentaDAO = new CuentaDAO();
    private final IUsuarioDAO usuarioDAO = new UsuarioDAO();
    private final IAutenticacionService autenticacionService = new AutenticacionService();

    @Override
    public Cuenta registrarEmpleado(String nombre, String telefono, String pin, Rol rol) {
        validar(nombre, telefono);
        if (rol == null) {
            throw new NegocioException("Selecciona el rol del empleado");
        }
        String tel = telefono == null || telefono.isBlank() ? null : telefono.trim();
        Usuario usuario = new Usuario(nombre.trim(), tel, LocalDate.now());
        return autenticacionService.registrarCuenta(usuario, pin, rol);
    }

    @Override
    public List<Cuenta> obtenerEmpleados() {
        return cuentaDAO.listar();
    }

    @Override
    public Usuario actualizarDatos(int idUsuario, String nombre, String telefono) {
        Usuario usuario = obtenerUsuario(idUsuario);
        validar(nombre, telefono);
        usuario.setNombre(nombre.trim());
        usuario.setTelefono(telefono == null || telefono.isBlank() ? null : telefono.trim());
        usuarioDAO.actualizar(usuario);
        return usuario;
    }

    @Override
    public void cambiarRol(int idUsuario, Rol rol) {
        if (rol == null) {
            throw new NegocioException("Selecciona el rol del empleado");
        }
        Usuario usuario = obtenerUsuario(idUsuario);
        if (rol != Rol.ADMINISTRADOR) {
            revisarUltimoAdministrador(usuario);
        }
        cuentaDAO.actualizarRol(idUsuario, rol);
    }

    @Override
    public void darDeBaja(int idUsuario) {
        Usuario usuario = obtenerUsuario(idUsuario);
        revisarUltimoAdministrador(usuario);
        usuarioDAO.cambiarActivo(idUsuario, false);
    }

    @Override
    public void reactivar(int idUsuario) {
        obtenerUsuario(idUsuario);
        usuarioDAO.cambiarActivo(idUsuario, true);
    }

    private Usuario obtenerUsuario(int idUsuario) {
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null) {
            throw new NegocioException("No existe el empleado " + idUsuario);
        }
        return usuario;
    }

    // Si el usuario es el unico administrador activo no se le puede quitar el rol ni dar de baja.
    private void revisarUltimoAdministrador(Usuario usuario) {
        int administradores = 0;
        boolean esAdministrador = false;
        for (Cuenta cuenta : cuentaDAO.buscarActivas()) {
            if (cuenta.getRol() == Rol.ADMINISTRADOR) {
                administradores++;
                if (cuenta.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
                    esAdministrador = true;
                }
            }
        }
        if (esAdministrador && administradores == 1) {
            throw new NegocioException("Debe quedar al menos un administrador activo");
        }
    }

    private void validar(String nombre, String telefono) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Escribe el nombre del empleado");
        }
        if (telefono != null && !telefono.isBlank() && !telefono.trim().matches("\\d{10}")) {
            throw new NegocioException("El telefono debe tener 10 digitos");
        }
    }
}

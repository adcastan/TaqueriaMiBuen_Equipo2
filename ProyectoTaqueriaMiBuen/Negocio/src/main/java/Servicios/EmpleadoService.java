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

    // Da de alta a un empleado con su PIN y su rol
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

    // Todos los empleados con su cuenta, para la pantalla de administracion
    @Override
    public List<Cuenta> obtenerEmpleados() {
        return cuentaDAO.listar();
    }

    // Cambia nombre y telefono de un empleado, revisando que vengan bien
    @Override
    public Usuario actualizarDatos(int idUsuario, String nombre, String telefono) {
        Usuario usuario = obtenerUsuario(idUsuario);
        validar(nombre, telefono);
        usuario.setNombre(nombre.trim());
        usuario.setTelefono(telefono == null || telefono.isBlank() ? null : telefono.trim());
        usuarioDAO.actualizar(usuario);
        return usuario;
    }

    // Cambia el rol de un empleado; no se permite dejar al local sin administrador
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

    // Baja logica: el empleado ya no puede entrar, pero su historial se queda
    @Override
    public void darDeBaja(int idUsuario) {
        Usuario usuario = obtenerUsuario(idUsuario);
        revisarUltimoAdministrador(usuario);
        usuarioDAO.cambiarActivo(idUsuario, false);
    }

    // Regresa a un empleado que se habia dado de baja
    @Override
    public void reactivar(int idUsuario) {
        obtenerUsuario(idUsuario);
        usuarioDAO.cambiarActivo(idUsuario, true);
    }

    // Busca al empleado y avisa con un mensaje claro si no existe
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

    // El nombre es obligatorio y el telefono, si lo ponen, debe tener 10 digitos
    private void validar(String nombre, String telefono) {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("Escribe el nombre del empleado");
        }
        if (telefono != null && !telefono.isBlank() && !telefono.trim().matches("\\d{10}")) {
            throw new NegocioException("El telefono debe tener 10 digitos");
        }
    }
}

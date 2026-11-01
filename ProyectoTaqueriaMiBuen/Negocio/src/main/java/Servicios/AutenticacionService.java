package Servicios;

import DAO.CuentaDAO;
import Entidades.Cuenta;
import Entidades.Usuario;
import Enums.Rol;
import java.util.Set;
import Exceptions.NegocioException;
import IAccesoDatos.ICuentaDAO;
import IServicios.IAutenticacionService;

/**
 * Inicio de sesion con PIN, registro de cuentas y revision de roles.
 * Como el PIN (contrasena) se guarda con hash BCrypt no se puede buscar directo en la BD, 
 * por eso se recorren las cuentas activas y se compara con validarPin(). 
 * Por la misma razon, al registrar una cuenta se revisa en el codigo que el PIN no este repetido.
 *
 * @author Equipo 2
 */
public class AutenticacionService implements IAutenticacionService {

    private final ICuentaDAO cuentaDAO = new CuentaDAO();

    /**
     * Regresa el usuario dueno del PIN (Contrasena). Si el PIN no es de nadie
     * lanza error.
     */
    @Override
    public Usuario iniciarSesion(String pin) {
        Cuenta cuenta = buscarCuentaPorPin(pin);
        if (cuenta == null) {
            throw new NegocioException("PIN incorrecto");
        }
        return cuenta.getUsuario();
    }

    /**
     * Revisa si el usuario tiene el rol indicado entre los suyos
     * (una cuenta puede tener varios roles).
     */
    @Override
    public boolean tieneRol(Usuario usuario, Rol rol) {
        return obtenerRoles(usuario).contains(rol);
    }

    /**
     * Roles de la cuenta del usuario; se guardan en la sesion al entrar.
     */
    @Override
    public Set<Rol> obtenerRoles(Usuario usuario) {
        Cuenta cuenta = cuentaDAO.buscarPorUsuario(usuario.getIdUsuario());
        if (cuenta == null) {
            throw new NegocioException("El usuario no tiene cuenta");
        }
        return cuenta.getRoles();
    }

    /**
     * Cuando un mesero cancela, un cajero o administrador escribe su PIN.
     *
     * @return usuario que autoriza.
     */
    @Override
    public Usuario validarAutorizacion(String pin) {
        Cuenta cuenta = buscarCuentaPorPin(pin);
        if (cuenta == null || !cuenta.puedeAutorizarCancelacion()) {
            throw new NegocioException("El PIN no es de un cajero o administrador");
        }
        return cuenta.getUsuario();
    }

    // Recorre las cuentas activas y regresa la que coincide con el PIN
    private Cuenta buscarCuentaPorPin(String pin) {
        if (pin == null || pin.isBlank()) {
            return null;
        }
        for (Cuenta cuenta : cuentaDAO.buscarActivas()) {
            if (cuenta.validarPin(pin)) {
                return cuenta;
            }
        }
        return null;
    }

    /**
     * Antes de crear la cuenta revisa que ninguna cuenta activa tenga el mismo
     * PIN.
     */
    @Override
    public Cuenta registrarCuenta(Usuario usuario, String pin, Rol... roles) {
        // El PIN es de 4 digitos y no se puede repetir: con el PIN se sabe quien entra
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new NegocioException("El PIN debe tener 4 numeros");
        }
        if (roles == null || roles.length == 0) {
            throw new NegocioException("Selecciona al menos un rol");
        }
        if (buscarCuentaPorPin(pin) != null) {
            throw new NegocioException("Ese PIN ingresado ya existe");
        }
        Cuenta cuenta = new Cuenta(usuario, pin, roles);
        cuentaDAO.registrar(cuenta);
        return cuenta;
    }
}

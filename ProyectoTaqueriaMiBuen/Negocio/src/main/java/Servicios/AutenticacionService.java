/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.CuentaDAO;
import Entidades.Cuenta;
import Entidades.Usuario;
import Enums.Rol;
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
     * Revisa si el usuario tiene el rol indicado.
     */
    @Override
    public boolean tieneRol(Usuario usuario, Rol rol) {
        return obtenerRol(usuario) == rol;
    }

    /**
     * Rol de la cuenta del usuario, se guarda en la sesion al entrar.
     */
    @Override
    public Rol obtenerRol(Usuario usuario) {
        Cuenta cuenta = cuentaDAO.buscarPorUsuario(usuario.getIdUsuario());
        if (cuenta == null) {
            throw new NegocioException("El usuario no tiene cuenta");
        }
        return cuenta.getRol();
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
    public Cuenta registrarCuenta(Usuario usuario, String pin, Rol rol) {
        if (pin == null || !pin.matches("\\d{4,6}")) {
            throw new NegocioException("El PIN debe tener de 4 a 6 numeros");
        }
        if (buscarCuentaPorPin(pin) != null) {
            throw new NegocioException("Ese PIN ingresado ya existe");
        }
        Cuenta cuenta = new Cuenta(usuario, pin, rol);
        cuentaDAO.registrar(cuenta);
        return cuenta;
    }
}

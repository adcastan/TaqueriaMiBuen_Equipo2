package Entidades;

import Enums.Rol;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Cuenta de acceso de un usuario. El PIN nunca se guarda tal cual, en la
 * columna contrasena solo queda el hash generado con BCrypt.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "cuenta")
public class Cuenta implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Integer idCuenta;

    // Al guardar una cuenta nueva tambien se guarda su usuario si todavia no existe
    @OneToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "contrasena", nullable = false, length = 60)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 13)
    private Rol rol;

    public Cuenta() {
    }

    public Cuenta(Usuario usuario, String pin, Rol rol) {
        this.usuario = usuario;
        this.rol = rol;
        this.contrasena = BCrypt.hashpw(pin, BCrypt.gensalt());
    }

    /**
     * Compara el PIN capturado contra el hash guardado.
     */
    public boolean validarPin(String pin) {
        if (pin == null || contrasena == null) {
            return false;
        }
        return BCrypt.checkpw(pin, contrasena);
    }

    /**
     * Solo cajero y administrador pueden autorizar la cancelacion de un pedido.
     */
    public boolean puedeAutorizarCancelacion() {
        return rol == Rol.CAJERO || rol == Rol.ADMINISTRADOR;
    }

    public Integer getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Integer idCuenta) {
        this.idCuenta = idCuenta;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "Cuenta{id=" + idCuenta + ", usuario=" + (usuario != null ? usuario.getNombre() : null) + ", rol=" + rol + "}";
    }
}

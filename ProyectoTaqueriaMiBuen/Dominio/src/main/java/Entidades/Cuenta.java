package Entidades;

import Enums.Rol;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Datos para entrar al sistema: el PIN (cifrado) y los roles del empleado.
 *
 * Una cuenta puede tener VARIOS roles. En la taqueria hay gente que a veces
 * esta en caja y a veces en la parrilla (por ejemplo el hijo del dueno), asi
 * que no se le obliga a tener una sola. Los roles se guardan en la tabla
 * cuenta_rol, un renglon por rol.
 *
 * El PIN nunca se guarda tal cual: se cifra con BCrypt. Para revisarlo se usa
 * validarPin(), que compara sin necesidad de descifrar.
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

    // PIN de 4 digitos cifrado con BCrypt (siempre mide 60 caracteres)
    @Column(name = "contrasena", nullable = false, length = 60)
    private String contrasena;

    // Contrasena para entrar desde fuera del local. Solo la tiene el administrador;
    // el PIN de 4 digitos no es suficiente cuando el sistema se abre por internet
    @Column(name = "contrasena_remota", length = 60)
    private String contrasenaRemota;

    // EAGER: los roles se necesitan en cada pantalla para saber que puede hacer la persona
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cuenta_rol", joinColumns = @JoinColumn(name = "id_cuenta"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 13)
    private Set<Rol> roles = EnumSet.noneOf(Rol.class);

    public Cuenta() {
    }

    /**
     * Crea la cuenta con su PIN (se cifra aqui mismo) y uno o mas roles.
     */
    public Cuenta(Usuario usuario, String pin, Rol... roles) {
        this.usuario = usuario;
        this.contrasena = BCrypt.hashpw(pin, BCrypt.gensalt());
        for (Rol r : roles) {
            this.roles.add(r);
        }
    }

    // Compara el PIN que escribieron con el que esta guardado cifrado
    public boolean validarPin(String pin) {
        if (pin == null || contrasena == null) {
            return false;
        }
        return BCrypt.checkpw(pin, contrasena);
    }

    // Igual que validarPin pero con la contrasena de acceso remoto
    public boolean validarContrasenaRemota(String contrasena) {
        if (contrasena == null || contrasenaRemota == null) {
            return false;
        }
        return BCrypt.checkpw(contrasena, contrasenaRemota);
    }

    // true si la cuenta tiene ese rol entre los suyos
    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }

    // true si tiene al menos uno de los roles que se le pasan
    public boolean tieneAlgunRol(Rol... buscados) {
        for (Rol r : buscados) {
            if (roles.contains(r)) {
                return true;
            }
        }
        return false;
    }

    // Solo cajero o administrador pueden autorizar cancelaciones con su PIN (RN-03)
    public boolean puedeAutorizarCancelacion() {
        return tieneAlgunRol(Rol.CAJERO, Rol.ADMINISTRADOR);
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

    // Cambia el PIN; se cifra antes de guardarlo
    public void cambiarPin(String pinNuevo) {
        this.contrasena = BCrypt.hashpw(pinNuevo, BCrypt.gensalt());
    }

    // Pone la contrasena remota (solo administrador); se cifra antes de guardarla
    public void cambiarContrasenaRemota(String nueva) {
        this.contrasenaRemota = nueva == null ? null : BCrypt.hashpw(nueva, BCrypt.gensalt());
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public void setRoles(Set<Rol> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "Cuenta{id=" + idCuenta + ", usuario=" + (usuario != null ? usuario.getNombre() : null) + ", roles=" + roles + "}";
    }
}

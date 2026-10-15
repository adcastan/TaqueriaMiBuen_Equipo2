package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Turno de caja abierto por un usuario. Al cerrarse se genera su corte.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "caja")
public class Caja implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Integer idCaja;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial", nullable = false)
    private double montoInicial;

    @Column(name = "abierta", nullable = false)
    private boolean abierta;

    public Caja() {
    }

    public Caja(Usuario usuario) {
        this.usuario = usuario;
    }

    public void abrir(double montoInicial) {
        this.montoInicial = montoInicial;
        this.fechaApertura = LocalDateTime.now();
        this.fechaCierre = null;
        this.abierta = true;
    }

    /**
     * Cierra la caja y prepara el corte con el total vendido y el efectivo
     * contado.
     */
    public Corte cerrar(double totalVentas, double efectivoContado) {
        if (!abierta) {
            throw new IllegalStateException("La caja ya esta cerrada");
        }
        this.abierta = false;
        this.fechaCierre = LocalDateTime.now();
        return new Corte(this, totalVentas, montoInicial + totalVentas, efectivoContado);
    }

    public Integer getIdCaja() {
        return idCaja;
    }

    public void setIdCaja(Integer idCaja) {
        this.idCaja = idCaja;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public double getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(double montoInicial) {
        this.montoInicial = montoInicial;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public void setAbierta(boolean abierta) {
        this.abierta = abierta;
    }
}

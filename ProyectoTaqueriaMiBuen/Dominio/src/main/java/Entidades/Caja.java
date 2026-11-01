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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Caja del dia (CU-04 Administrar Caja). Hay una sola caja abierta a la vez y
 * la comparten varias personas (dueno, cajeras, hijo del dueno); por eso cada
 * pago guarda quien cobro. Aqui se guarda quien la abrio y con cuanto.
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

    // Quien abrio la caja
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    // Fondo con el que empieza el dia (para dar cambio)
    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @Column(name = "abierta", nullable = false)
    private boolean abierta;

    public Caja() {
    }

    public Caja(Usuario usuario) {
        this.usuario = usuario;
    }

    // Deja la caja abierta con el dinero con el que empieza el turno
    public void abrir(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
        this.fechaApertura = LocalDateTime.now();
        this.abierta = true;
    }

    /**
     * Cierra la caja y arma el corte con lo que entro en efectivo. El corte
     * separado por metodo de pago lo termina el siguiente equipo; por ahora
     * se llenan en cero los totales que todavia no se calculan.
     */
    public Corte cerrar(Usuario quienCierra, BigDecimal ventasEfectivo, BigDecimal ventasTarjeta,
            BigDecimal ventasTransferencia, BigDecimal efectivoContado) {
        if (!abierta) {
            throw new IllegalStateException("La caja ya esta cerrada");
        }
        this.abierta = false;
        this.fechaCierre = LocalDateTime.now();
        // Al cajon solo entra el efectivo: fondo inicial + lo cobrado en efectivo
        BigDecimal esperado = montoInicial.add(ventasEfectivo);
        return new Corte(this, quienCierra, ventasEfectivo, ventasTarjeta, ventasTransferencia, esperado, efectivoContado);
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

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public void setAbierta(boolean abierta) {
        this.abierta = abierta;
    }
}

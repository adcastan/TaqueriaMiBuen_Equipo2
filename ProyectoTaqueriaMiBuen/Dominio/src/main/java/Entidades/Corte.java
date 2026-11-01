package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Corte de caja: lo que se hace al cerrar la caja del dia.
 *
 * La tabla ya trae una columna por cada forma de pago, las salidas y los
 * descuentos. En esta entrega se llenan efectivo, tarjeta y transferencia;
 * plataformas, salidas y descuentos van en cero hasta que el siguiente equipo
 * termine el CU-04.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "corte")
public class Corte implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_corte")
    private Integer idCorte;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_caja", nullable = false, unique = true)
    private Caja caja;

    // Quien hizo el corte
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total_ventas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "total_efectivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "total_tarjeta", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;

    @Column(name = "total_transferencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalTransferencia = BigDecimal.ZERO;

    // Lo de DiDi Food y Uber Eats: no entra a la caja, solo se informa
    @Column(name = "total_plataformas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPlataformas = BigDecimal.ZERO;

    @Column(name = "total_salidas", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSalidas = BigDecimal.ZERO;

    @Column(name = "total_descuentos", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "efectivo_esperado", nullable = false, precision = 10, scale = 2)
    private BigDecimal efectivoEsperado = BigDecimal.ZERO;

    @Column(name = "efectivo_contado", nullable = false, precision = 10, scale = 2)
    private BigDecimal efectivoContado = BigDecimal.ZERO;

    // Contado menos esperado: negativo = falta dinero, positivo = sobra
    @Column(name = "diferencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal diferencia = BigDecimal.ZERO;

    public Corte() {
    }

    public Corte(Caja caja, Usuario usuario, BigDecimal totalEfectivo, BigDecimal totalTarjeta,
            BigDecimal totalTransferencia, BigDecimal efectivoEsperado, BigDecimal efectivoContado) {
        this.caja = caja;
        this.usuario = usuario;
        this.totalEfectivo = totalEfectivo;
        this.totalTarjeta = totalTarjeta;
        this.totalTransferencia = totalTransferencia;
        this.totalVentas = totalEfectivo.add(totalTarjeta).add(totalTransferencia);
        this.efectivoEsperado = efectivoEsperado;
        this.efectivoContado = efectivoContado;
        this.diferencia = calcularDiferencia();
    }

    // Si nadie puso la fecha, se toma la de este momento al guardar
    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    // Lo que se conto en el cajon menos lo que deberia haber
    public BigDecimal calcularDiferencia() {
        return efectivoContado.subtract(efectivoEsperado).setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getIdCorte() {
        return idCorte;
    }

    public Caja getCaja() {
        return caja;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public BigDecimal getTotalEfectivo() {
        return totalEfectivo;
    }

    public BigDecimal getTotalTarjeta() {
        return totalTarjeta;
    }

    public BigDecimal getTotalTransferencia() {
        return totalTransferencia;
    }

    public BigDecimal getTotalPlataformas() {
        return totalPlataformas;
    }

    public void setTotalPlataformas(BigDecimal totalPlataformas) {
        this.totalPlataformas = totalPlataformas;
    }

    public BigDecimal getTotalSalidas() {
        return totalSalidas;
    }

    public void setTotalSalidas(BigDecimal totalSalidas) {
        this.totalSalidas = totalSalidas;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(BigDecimal totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public BigDecimal getEfectivoEsperado() {
        return efectivoEsperado;
    }

    public BigDecimal getEfectivoContado() {
        return efectivoContado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }
}

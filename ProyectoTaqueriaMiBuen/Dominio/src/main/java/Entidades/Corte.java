package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Corte de caja: compara el efectivo esperado contra el contado.
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

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total_ventas", nullable = false)
    private double totalVentas;

    @Column(name = "efectivo_esperado", nullable = false)
    private double efectivoEsperado;

    @Column(name = "efectivo_contado", nullable = false)
    private double efectivoContado;

    @Column(name = "diferencia", nullable = false)
    private double diferencia;

    public Corte() {
    }

    public Corte(Caja caja, double totalVentas, double efectivoEsperado, double efectivoContado) {
        this.caja = caja;
        this.totalVentas = totalVentas;
        this.efectivoEsperado = efectivoEsperado;
        this.efectivoContado = efectivoContado;
        this.diferencia = calcularDiferencia();
    }

    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    /**
     * Positivo si sobra efectivo, negativo si falta.
     */
    public double calcularDiferencia() {
        return efectivoContado - efectivoEsperado;
    }

    public Integer getIdCorte() {
        return idCorte;
    }

    public void setIdCorte(Integer idCorte) {
        this.idCorte = idCorte;
    }

    public Caja getCaja() {
        return caja;
    }

    public void setCaja(Caja caja) {
        this.caja = caja;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public double getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(double totalVentas) {
        this.totalVentas = totalVentas;
    }

    public double getEfectivoEsperado() {
        return efectivoEsperado;
    }

    public void setEfectivoEsperado(double efectivoEsperado) {
        this.efectivoEsperado = efectivoEsperado;
    }

    public double getEfectivoContado() {
        return efectivoContado;
    }

    public void setEfectivoContado(double efectivoContado) {
        this.efectivoContado = efectivoContado;
    }

    public double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(double diferencia) {
        this.diferencia = diferencia;
    }
}

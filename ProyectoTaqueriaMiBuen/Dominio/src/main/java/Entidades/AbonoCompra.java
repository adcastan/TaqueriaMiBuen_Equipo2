package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago parcial a una compra que se hizo a credito ("Liquidar pago" del CU-06).
 * Cuando la suma de abonos llega al total, la compra pasa a PAGADA.
 *
 * Por ahora solo esta el mapeo de la tabla; el servicio y la pantalla de
 * abonos los hace el siguiente equipo.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "abono_compra")
public class AbonoCompra implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_abono")
    private Integer idAbono;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    // Quien registro el abono
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public AbonoCompra() {
    }

    public AbonoCompra(Compra compra, Usuario usuario, BigDecimal monto) {
        this.compra = compra;
        this.usuario = usuario;
        this.monto = monto;
    }

    // Si nadie puso la fecha, se toma la de este momento al guardar
    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Integer getIdAbono() {
        return idAbono;
    }

    public Compra getCompra() {
        return compra;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}

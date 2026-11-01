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
 * Dinero que se saca de la caja durante el dia: hielo, gas, pagarle a un
 * proveedor que llego... Si se le pago a una compra, queda ligada a ella.
 * (Que las compras se paguen con dinero de la caja esta por confirmar con
 * el cliente.)
 *
 * Por ahora solo esta el mapeo de la tabla; registrar salidas y restarlas en
 * el corte lo hace el siguiente equipo (CU-04).
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "salida_caja")
public class SalidaCaja implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_salida")
    private Integer idSalida;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    // Quien saco el dinero
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "motivo", nullable = false, length = 150)
    private String motivo;

    // Compra que se pago con esta salida; null si fue otro gasto
    @ManyToOne
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public SalidaCaja() {
    }

    public SalidaCaja(Caja caja, Usuario usuario, BigDecimal monto, String motivo, Compra compra) {
        this.caja = caja;
        this.usuario = usuario;
        this.monto = monto;
        this.motivo = motivo;
        this.compra = compra;
    }

    // Si nadie puso la fecha, se toma la de este momento al guardar
    @PrePersist
    protected void antesDeGuardar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Integer getIdSalida() {
        return idSalida;
    }

    public Caja getCaja() {
        return caja;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMotivo() {
        return motivo;
    }

    public Compra getCompra() {
        return compra;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}

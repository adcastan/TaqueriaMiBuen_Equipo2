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

/**
 * Extra que el cliente pidio en un detalle. Guarda el costo que tenia el extra
 * en ese momento.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "detalle_extra")
public class DetalleExtra implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_extra")
    private Integer idDetalleExtra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_detalle", nullable = false)
    private DetallePedido detalle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_extra", nullable = false)
    private IngredienteExtra extra;

    @Column(name = "costo_extra", nullable = false)
    private double costoExtra;

    public DetalleExtra() {
    }

    public DetalleExtra(DetallePedido detalle, IngredienteExtra extra, double costoExtra) {
        this.detalle = detalle;
        this.extra = extra;
        this.costoExtra = costoExtra;
    }

    public Integer getIdDetalleExtra() {
        return idDetalleExtra;
    }

    public void setIdDetalleExtra(Integer idDetalleExtra) {
        this.idDetalleExtra = idDetalleExtra;
    }

    public DetallePedido getDetalle() {
        return detalle;
    }

    public void setDetalle(DetallePedido detalle) {
        this.detalle = detalle;
    }

    public IngredienteExtra getExtra() {
        return extra;
    }

    public void setExtra(IngredienteExtra extra) {
        this.extra = extra;
    }

    public double getCostoExtra() {
        return costoExtra;
    }

    public void setCostoExtra(double costoExtra) {
        this.costoExtra = costoExtra;
    }
}

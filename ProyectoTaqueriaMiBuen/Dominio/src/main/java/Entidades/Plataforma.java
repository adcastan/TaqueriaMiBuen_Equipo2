package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * App de reparto (DiDi Food o Uber Eats). La clave es la misma que el canal
 * del pedido (DIDI_FOOD, UBER_EATS). La comision sirve para que el reporte
 * de ventas muestre lo que de verdad le queda al dueno.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "plataforma")
public class Plataforma implements Serializable {

    @Id
    @Column(name = "clave", length = 10)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 40)
    private String nombre;

    // Porcentaje que se queda la app de cada venta (ej. 25.00)
    @Column(name = "comision_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionPct = BigDecimal.ZERO;

    public Plataforma() {
    }

    public Plataforma(String clave, String nombre, BigDecimal comisionPct) {
        this.clave = clave;
        this.nombre = nombre;
        this.comisionPct = comisionPct;
    }

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getComisionPct() {
        return comisionPct;
    }

    public void setComisionPct(BigDecimal comisionPct) {
        this.comisionPct = comisionPct;
    }
}

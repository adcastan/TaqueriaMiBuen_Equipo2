package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Una pieza de un paquete. Ejemplo: el "Paquete 3 tacos y refresco" tiene dos
 * renglones aqui: (Taco de asada, 3) y (Coca-Cola, 1). Asi la parrilla sabe
 * que tiene que hacer 3 tacos aunque en el pedido diga "1 paquete".
 *
 * La llave es doble (paquete + componente), por eso usa la clase Llave de abajo.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "paquete_componente")
@IdClass(PaqueteComponente.Llave.class)
public class PaqueteComponente implements Serializable {

    // El paquete al que pertenece esta pieza
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_paquete", nullable = false)
    private Producto paquete;

    // El producto que va dentro del paquete
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_componente", nullable = false)
    private Producto componente;

    // Cuantas piezas de ese producto trae el paquete
    @Column(name = "cantidad", nullable = false)
    private int cantidad = 1;

    public PaqueteComponente() {
    }

    public PaqueteComponente(Producto paquete, Producto componente, int cantidad) {
        this.paquete = paquete;
        this.componente = componente;
        this.cantidad = cantidad;
    }

    public Producto getPaquete() {
        return paquete;
    }

    public void setPaquete(Producto paquete) {
        this.paquete = paquete;
    }

    public Producto getComponente() {
        return componente;
    }

    public void setComponente(Producto componente) {
        this.componente = componente;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Llave doble que JPA necesita cuando la tabla no tiene un id propio.
     * Los nombres de los campos deben ser iguales a los de arriba.
     */
    public static class Llave implements Serializable {

        private Integer paquete;
        private Integer componente;

        public Llave() {
        }

        public Llave(Integer paquete, Integer componente) {
            this.paquete = paquete;
            this.componente = componente;
        }

        // JPA compara llaves con equals y hashCode, por eso se escriben a mano
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Llave)) {
                return false;
            }
            Llave otra = (Llave) o;
            return Objects.equals(paquete, otra.paquete) && Objects.equals(componente, otra.componente);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paquete, componente);
        }
    }
}

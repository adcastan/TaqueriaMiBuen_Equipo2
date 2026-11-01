package Entidades;

import Enums.EstadoMesa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Mesa del local donde se atienden los pedidos locales.
 *
 * Las mesas se pueden juntar para grupos grandes: una queda como principal
 * (ahi viven los pedidos) y las demas apuntan a ella con mesaPrincipal.
 * En el mapa las unidas se ven del mismo color que la principal con la
 * leyenda "Junto a mesa N". Cuando la principal se desocupa, el grupo se
 * deshace solo (RN-13).
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "mesa")
public class Mesa implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mesa")
    private Integer idMesa;

    @Column(name = "numero", nullable = false, unique = true)
    private int numero;

    @Column(name = "capacidad", nullable = false)
    private int capacidad = 4;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoMesa estado = EstadoMesa.DISPONIBLE;

    // Mesa principal del grupo cuando esta mesa se junto con otra; null si esta sola
    @ManyToOne
    @JoinColumn(name = "id_mesa_principal")
    private Mesa mesaPrincipal;

    public Mesa() {
    }

    public Mesa(int numero, int capacidad) {
        this.numero = numero;
        this.capacidad = capacidad;
    }

    // true si la mesa esta en verde
    public boolean estaDisponible() {
        return estado == EstadoMesa.DISPONIBLE;
    }

    // true si esta mesa esta pegada a otra (no es la principal del grupo)
    public boolean estaUnida() {
        return mesaPrincipal != null;
    }

    /**
     * La pega a la mesa principal: toma su mismo color. Solo se puede con una
     * mesa libre y que no sea la misma.
     */
    public void juntarCon(Mesa principal) {
        if (principal == null || principal.getIdMesa().equals(idMesa)) {
            throw new IllegalArgumentException("Elige otra mesa para juntar");
        }
        if (!estaDisponible() || estaUnida()) {
            throw new IllegalStateException("La mesa " + numero + " no esta libre");
        }
        if (principal.estaUnida()) {
            throw new IllegalStateException("La mesa " + principal.getNumero() + " ya esta junto a otra; usa su mesa principal");
        }
        mesaPrincipal = principal;
        estado = principal.getEstado();
    }

    // La despega del grupo y la deja libre
    public void separar() {
        mesaPrincipal = null;
        estado = EstadoMesa.DISPONIBLE;
    }

    public Mesa getMesaPrincipal() {
        return mesaPrincipal;
    }

    public void setMesaPrincipal(Mesa mesaPrincipal) {
        this.mesaPrincipal = mesaPrincipal;
    }

    public Integer getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(Integer idMesa) {
        this.idMesa = idMesa;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    public void setEstado(EstadoMesa estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Mesa{numero=" + numero + ", capacidad=" + capacidad + ", estado=" + estado + "}";
    }
}

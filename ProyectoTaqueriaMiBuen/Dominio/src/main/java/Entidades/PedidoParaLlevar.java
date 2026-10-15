package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Pedido que el cliente recoge en el mostrador.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "pedido_para_llevar")
@PrimaryKeyJoinColumn(name = "id_pedido")
public class PedidoParaLlevar extends Pedido {

    @Column(name = "nombre_cliente", nullable = false, length = 100)
    private String nombreCliente;

    public PedidoParaLlevar() {
    }

    public PedidoParaLlevar(Usuario usuario, String nombreCliente) {
        super(usuario);
        this.nombreCliente = nombreCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }
}

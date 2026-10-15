package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Datos de entrega de un pedido a domicilio. No tiene id propio, usa el mismo
 * id_pedido del pedido al que pertenece.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "datos_entrega")
public class DatosEntrega implements Serializable {

    @Id
    @Column(name = "id_pedido")
    private Integer idPedido;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_pedido")
    private PedidoDomicilio pedido;

    @Column(name = "nombre_cliente", nullable = false, length = 100)
    private String nombreCliente;

    @Column(name = "telefono", nullable = false, length = 10)
    private String telefono;

    @Column(name = "direccion", nullable = false, length = 200)
    private String direccion;

    @Column(name = "referencias", length = 200)
    private String referencias;

    public DatosEntrega() {
    }

    public DatosEntrega(String nombreCliente, String telefono, String direccion, String referencias) {
        this.nombreCliente = nombreCliente;
        this.telefono = telefono;
        this.direccion = direccion;
        this.referencias = referencias;
    }

    /**
     * Revisa que esten los campos obligatorios y que el telefono tenga 10
     * digitos.
     */
    public boolean validar() {
        boolean camposLlenos = nombreCliente != null && !nombreCliente.isBlank()
                && direccion != null && !direccion.isBlank();
        boolean telefonoValido = telefono != null && telefono.matches("\\d{10}");
        return camposLlenos && telefonoValido;
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public PedidoDomicilio getPedido() {
        return pedido;
    }

    public void setPedido(PedidoDomicilio pedido) {
        this.pedido = pedido;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencias() {
        return referencias;
    }

    public void setReferencias(String referencias) {
        this.referencias = referencias;
    }
}

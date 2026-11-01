package Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Valores que el administrador puede cambiar sin tocar el codigo.
 * Cada renglon es una clave con su valor, por ejemplo:
 * LIMITE_DESCUENTO_PCT = 10 (descuento maximo que da el cajero sin permiso).
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "configuracion")
public class Configuracion implements Serializable {

    // Nombre del ajuste; es la llave de la tabla
    @Id
    @Column(name = "clave", length = 40)
    private String clave;

    // Se guarda como texto; quien lo lee lo convierte a numero si hace falta
    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    public Configuracion() {
    }

    public Configuracion(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}

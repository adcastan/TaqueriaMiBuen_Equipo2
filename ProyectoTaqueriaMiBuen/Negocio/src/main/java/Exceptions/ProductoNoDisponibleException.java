package Exceptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Se lanza cuando al enviar a cocina alguno de los productos ya se dio de baja
 * del menu (flujo 2.2.11). Trae los ids para que la pantalla pueda marcar en
 * rojo justo esos renglones y el mesero sepa cuales quitar o cambiar.
 * El servlet la convierte en una respuesta 409.
 *
 * @author Equipo 2
 */
public class ProductoNoDisponibleException extends NegocioException {

    private final List<Integer> idsProductos;

    public ProductoNoDisponibleException(String mensaje, List<Integer> idsProductos) {
        super(mensaje);
        this.idsProductos = new ArrayList<>(idsProductos);
    }

    public List<Integer> getIdsProductos() {
        return idsProductos;
    }
}

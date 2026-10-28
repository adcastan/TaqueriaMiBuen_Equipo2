package Exceptions;

/**
 * Se lanza cuando otro mesero ya ocupo la mesa y no se pidio cuenta separada.
 * El servlet la convierte en una respuesta 409 (ver secuencia Enviar a cocina).
 *
 * @author Equipo 2
 */
public class MesaOcupadaException extends NegocioException {

    public MesaOcupadaException(String mensaje) {
        super(mensaje);
    }
}

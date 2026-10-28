package Exceptions;

/**
 * Error de negocio, por ejemplo un pedido sin productos o un PIN (Contrasena) que no puede autorizar. 
 * El mensaje se muestra al usuario.
 * @author Equipo 2
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }

    public NegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
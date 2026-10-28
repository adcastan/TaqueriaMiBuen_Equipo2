package Herramientas;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ayudante para los importes. Los double guardan los numeros en binario y a
 * veces 0.1 + 0.2 da 0.30000000000000004. En una taqueria eso no se vale:
 * el ticket tiene que decir $0.30 exactos. Por eso todos los importes pasan
 * por aqui antes de guardarse o mostrarse (requisito 3.5 del caso de uso).
 *
 * @author Equipo 2
 */
public final class Dinero {

    private Dinero() {
        // Solo tiene metodos estaticos, no tiene caso crear objetos de esta clase
    }

    /**
     * Deja la cantidad con dos decimales, redondeando como en la escuela
     * (de 5 para arriba sube). Se usa BigDecimal porque con Math.round un
     * 2.675 se convierte en 2.67 por culpa del binario; asi sale 2.68.
     */
    public static double redondear(double cantidad) {
        return BigDecimal.valueOf(cantidad).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

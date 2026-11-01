package Herramientas;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ayudante para los importes. Todo el dinero del sistema se maneja con
 * BigDecimal y no con double, porque los double guardan los numeros en
 * binario y a veces 0.1 + 0.2 da 0.30000000000000004. En una taqueria eso no
 * se vale: el ticket tiene que decir $0.30 exactos (requisito 3.5 del CU-02).
 * En la base de datos los importes van en DECIMAL(10,2), que es lo mismo.
 *
 * @author Equipo 2
 */
public final class Dinero {

    // Cero pesos con dos decimales, para no andar escribiendo BigDecimal.ZERO.setScale(2) a cada rato
    public static final BigDecimal CERO = new BigDecimal("0.00");

    private Dinero() {
        // Solo tiene metodos estaticos, no tiene caso crear objetos de esta clase
    }

    /**
     * Deja la cantidad con dos decimales, redondeando como en la escuela
     * (de 5 para arriba sube): 2.675 queda en 2.68.
     */
    public static BigDecimal redondear(BigDecimal cantidad) {
        if (cantidad == null) {
            return CERO;
        }
        return cantidad.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convierte un numero que llega de la pantalla (por ejemplo 35.5) a pesos
     * con dos decimales. Se usa valueOf y no new BigDecimal(double) porque el
     * segundo arrastra los decimales raros del binario.
     */
    public static BigDecimal de(double cantidad) {
        return redondear(BigDecimal.valueOf(cantidad));
    }

    /**
     * Suma dos importes; si alguno viene vacio cuenta como cero.
     */
    public static BigDecimal sumar(BigDecimal a, BigDecimal b) {
        BigDecimal x = a == null ? CERO : a;
        BigDecimal y = b == null ? CERO : b;
        return redondear(x.add(y));
    }

    /**
     * true si el importe es mayor que cero.
     */
    public static boolean esPositivo(BigDecimal cantidad) {
        return cantidad != null && cantidad.signum() > 0;
    }
}

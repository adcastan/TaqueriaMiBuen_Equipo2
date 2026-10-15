package Conexion;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maneja la conexion con la base de datos por medio de JPA.
 * La fabrica se crea una sola vez y de ella se sacan los EntityManager.
 *
 * @author Equipo 2
 */
public class JPAUtil {

    // Debe coincidir con el nombre de la unidad en persistence.xml
    private static final String UNIDAD = "PersistenciaTaqueriaMiBuen";

    // Muestra errores importantes en consola.
    private static final Logger LOG_HIBERNATE = Logger.getLogger("org.hibernate");

    private static EntityManagerFactory fabrica;

    public static EntityManager obtenerEntityManager() {
        if (fabrica == null) {
            LOG_HIBERNATE.setLevel(Level.SEVERE);
            fabrica = Persistence.createEntityManagerFactory(UNIDAD);
        }
        return fabrica.createEntityManager();
    }

    // Se llama al terminar el codigo para cerrar las conexiones
    public static void cerrar() {
        if (fabrica != null) {
            fabrica.close();
            fabrica = null;
        }
    }
}

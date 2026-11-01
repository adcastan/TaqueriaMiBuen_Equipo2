package Utilerias;

import Conexion.JPAUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Cuando Tomcat detiene la aplicacion se cierran las conexiones de JPA,
 * asi no quedan conexiones abiertas a MySQL.
 *
 * @author Equipo 2
 */
@WebListener
public class CierreAplicacion implements ServletContextListener {

    // Tomcat llama esto al apagar la aplicacion; cerramos la conexion a la base para no dejarla colgada
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        JPAUtil.cerrar();
    }
}


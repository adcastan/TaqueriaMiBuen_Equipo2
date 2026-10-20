/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        JPAUtil.cerrar();
    }
}


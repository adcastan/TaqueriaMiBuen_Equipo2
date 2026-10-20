/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DTO;

/**
 * Respuesta que regresa el servicio despues de mandar un pedido a cocina. 
 * Si kdsConectado es false, la pantalla avisa al mesero que se imprimio un ticket
 * de respaldo porque el monitor de cocina no responde.
 *
 * @author Equipo 2
 */
public class ResultadoEnvio {

    private int idPedido;
    private String folio;
    private boolean kdsConectado;

    public ResultadoEnvio(int idPedido, String folio, boolean kdsConectado) {
        this.idPedido = idPedido;
        this.folio = folio;
        this.kdsConectado = kdsConectado;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public String getFolio() {
        return folio;
    }

    public boolean isKdsConectado() {
        return kdsConectado;
    }
}

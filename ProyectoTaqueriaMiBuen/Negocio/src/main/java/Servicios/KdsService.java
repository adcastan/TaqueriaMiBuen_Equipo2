/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Servicios;

import DAO.KdsDAO;
import Entidades.DetallePedido;
import IAccesoDatos.IKdsDAO;
import IServicios.IKdsService;
import java.util.List;

/**
 * Comunicacion con el monitor de cocina (KDS).
 * Al igual que la impresora, el monitor se simula al no tener acceso al monitor kds.
 * @author Equipo 2
 */
public class KdsService implements IKdsService{

    // Si el monitor no ha pedido la cola en este tiempo se toma como desconectado
    private static final int SEGUNDOS_SIN_CONEXION = 30;

    private final IKdsDAO kdsDAO = new KdsDAO();

    /**
     * El monitor pide su cola cada pocos segundos. Cada vez que la pide se
     * registra un latido, asi sabemos que sigue conectado (secuencia KDS).
     */
    @Override
    public List<DetallePedido> obtenerCola(int idTerminal) {
        kdsDAO.registrarLatido(idTerminal);
        return kdsDAO.consultarPendientes();
    }

    @Override
    public void marcarListo(int idDetalle) {
        kdsDAO.marcarListo(idDetalle);
    }

    /**
     * Revisa el ultimo latido. 
     * Si el monitor nunca se conecto regresa false.
     */
    @Override
    public boolean hayKdsConectado() {
        int segundos = kdsDAO.segundosDesdeUltimoLatido();
        return segundos >= 0 && segundos <= SEGUNDOS_SIN_CONEXION;
    }
}


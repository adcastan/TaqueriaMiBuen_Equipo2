/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IServicios;

import Entidades.Caja;
import Entidades.Corte;
import Entidades.Usuario;

/**
 * Servicio de caja.
 * La implementa CajaService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface ICajaService {

    // Abre la caja del turno con el efectivo inicial.
    public Caja abrirCaja(Usuario usuario, double montoInicial);

    // Obtiene la Caja abierta en este momento o null.
    public Caja obtenerCajaAbierta();

    // Obtiene el total cobrado en la caja con un metodo (EFECTIVO o TARJETA).
    public double obtenerTotalCobrado(int idCaja, String metodo);

    // Cierra la caja abierta y genera el corte con el efectivo contado.
    public Corte cerrarCaja(double efectivoContado);

    // Obtiene el corte de una caja.
    public Corte obtenerCorte(int idCaja);
}


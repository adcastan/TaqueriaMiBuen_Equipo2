/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.Mesa;
import Enums.EstadoMesa;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Operaciones de base de datos de las mesas. 
 * La implementa MesaDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 * @author Equipo 2
 */
public interface IMesaDAO {

    // Guarda una mesa nueva.
    public void registrar(Mesa mesa);

    // Mesa por su id o null.
    public Mesa buscarPorId(int idMesa);

    // Mesa por su numero o null.
    public Mesa buscarPorNumero(int numero);

    // Mesas libres.
    public List<Mesa> listarDisponibles();

    // Cambia el estado de la mesa.
    public void actualizarEstado(int idMesa, EstadoMesa estado);

    // Lista todas las mesas.
    public List<Mesa> listar();

    // Lee la mesa bloqueandola dentro de la transaccion que recibe.
    public Mesa bloquearParaActualizar(EntityManager em, int idMesa);
}

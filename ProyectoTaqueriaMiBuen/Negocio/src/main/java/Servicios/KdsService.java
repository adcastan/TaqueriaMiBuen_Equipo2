package Servicios;

import DAO.KdsDAO;
import Entidades.DetallePedido;
import Enums.EstadoPreparacion;
import Exceptions.NegocioException;
import IAccesoDatos.IKdsDAO;
import IServicios.IKdsService;
import java.util.List;

/**
 * Comunicacion con el monitor de cocina (KDS).
 * Al igual que la impresora, el monitor se simula al no tener acceso al monitor kds.
 *
 * @author Equipo 2
 */
public class KdsService implements IKdsService {

    // Si el monitor no ha pedido la cola en este tiempo se toma como desconectado
    private static final int SEGUNDOS_SIN_CONEXION = 30;

    private final IKdsDAO kdsDAO = new KdsDAO();

    /**
     * El monitor pide su cola cada segundo. Cada vez que la pide se registra
     * un latido, asi sabemos que sigue conectado (secuencia KDS).
     */
    @Override
    public List<DetallePedido> obtenerCola(int idTerminal) {
        kdsDAO.registrarLatido(idTerminal);
        return kdsDAO.consultarPendientes();
    }

    // Atajo para marcar listo; por dentro usa cambiarEstado
    @Override
    public void marcarListo(int idDetalle) {
        cambiarEstado(idDetalle, EstadoPreparacion.LISTO.name());
    }

    /**
     * El cocinero avisa en que va el producto: "EN_PREPARACION" cuando lo
     * agarra y "LISTO" cuando lo termina. Cualquier otro texto se rechaza.
     */
    @Override
    public void cambiarEstado(int idDetalle, String estado) {
        EstadoPreparacion nuevo;
        try {
            nuevo = EstadoPreparacion.valueOf(estado == null ? "" : estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Estado no valido, usa EN_PREPARACION o LISTO");
        }
        try {
            kdsDAO.cambiarEstado(idDetalle, nuevo);
        } catch (RuntimeException e) {
            Throwable causa = e.getCause();
            if (causa instanceof IllegalStateException || causa instanceof IllegalArgumentException) {
                throw new NegocioException(causa.getMessage(), e);
            }
            throw e;
        }
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

package IAccesoDatos;

import Entidades.Caja;
import Entidades.Corte;
import Entidades.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Operaciones de base de datos de la caja y su corte.
 * La implementa CajaDAO.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface ICajaDAO {

    // Guarda una caja recien abierta.
    public void abrir(Caja caja);

    // Caja que esta abierta en este momento o null.
    public Caja buscarAbierta();

    // Caja por su id o null.
    public Caja buscarPorId(int idCaja);

    // Cierra la caja y guarda el corte con lo cobrado por metodo y el efectivo contado.
    public Corte cerrar(int idCaja, Usuario quienCierra, BigDecimal ventasEfectivo, BigDecimal ventasTarjeta,
            BigDecimal ventasTransferencia, BigDecimal efectivoContado);

    // Corte de una caja o null si todavia no se cierra.
    public Corte buscarCorte(int idCaja);

    // Cortes hechos entre dos fechas.
    public List<Corte> listarCortes(LocalDateTime inicio, LocalDateTime fin);
}


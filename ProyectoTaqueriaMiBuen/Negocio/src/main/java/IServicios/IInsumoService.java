package IServicios;

import DTO.CompraDTO;
import Entidades.Compra;
import Entidades.Ingrediente;
import Entidades.Usuario;
import java.util.List;

/**
 * Servicio de insumos (solo catalogo) y compras.
 * La implementa InsumoService.
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface IInsumoService {

    // Registra un insumo nuevo en el catalogo (proveedor habitual opcional).
    public Ingrediente registrarIngrediente(String nombre, String unidad, Integer idProveedor);

    // Cambia nombre, unidad o proveedor habitual de un insumo.
    public Ingrediente actualizarIngrediente(int idIngrediente, String nombre, String unidad, Integer idProveedor);

    // Da de baja un insumo (baja logica, no se borra).
    public void darDeBajaIngrediente(int idIngrediente);

    // Insumos que siguen en uso.
    public List<Ingrediente> obtenerIngredientes();

    // Registra una compra de insumos, de contado o a credito.
    public Compra registrarCompra(CompraDTO dto, Usuario usuario);
}

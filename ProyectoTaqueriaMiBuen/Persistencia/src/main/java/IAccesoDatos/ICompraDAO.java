/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package IAccesoDatos;

import Entidades.Compra;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Operaciones de base de datos de las compras de insumos. 
 * La implementa CompraDAO. 
 * Las demas capas usan esta interfaz y no la clase directamente.
 *
 * @author Equipo 2
 */
public interface ICompraDAO {

    // Guarda la compra con sus detalles y suma lo comprado a la existencia de cada ingrediente.
    public void registrar(Compra compra);

    // Compra con sus detalles o null.
    public Compra buscarPorId(int idCompra);

    // Compras hechas entre dos fechas, con sus detalles.
    public List<Compra> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fin);

    // Cuantas compras tiene un proveedor (para saber si se puede eliminar).
    public long contarPorProveedor(int idProveedor);
}

package DTO;

import Entidades.Compra;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte de compras de insumos de un periodo.
 *
 * @author Equipo 2
 */
public class ReporteCompras {

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private int numeroCompras;
    private BigDecimal totalComprado = BigDecimal.ZERO;
    private List<Compra> compras;

    public ReporteCompras(LocalDateTime fechaInicio, LocalDateTime fechaFin, List<Compra> compras) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.compras = compras;
        this.numeroCompras = compras.size();
        for (Compra compra : compras) {
            totalComprado = totalComprado.add(compra.getTotal());
        }
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public int getNumeroCompras() {
        return numeroCompras;
    }

    public BigDecimal getTotalComprado() {
        return totalComprado;
    }

    public List<Compra> getCompras() {
        return compras;
    }
}


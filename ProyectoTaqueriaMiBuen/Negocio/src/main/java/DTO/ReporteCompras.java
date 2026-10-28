package DTO;

import Entidades.Compra;
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
    private double totalComprado;
    private List<Compra> compras;

    public ReporteCompras(LocalDateTime fechaInicio, LocalDateTime fechaFin, List<Compra> compras) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.compras = compras;
        this.numeroCompras = compras.size();
        for (Compra compra : compras) {
            totalComprado += compra.getTotal();
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

    public double getTotalComprado() {
        return totalComprado;
    }

    public List<Compra> getCompras() {
        return compras;
    }
}


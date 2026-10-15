package Entidades;

import Enums.EstadoPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de ventas de un periodo.
 *
 * @author Equipo 2
 */
@Entity
@Table(name = "reporte_venta")
public class ReporteVenta implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "total_ventas", nullable = false)
    private double totalVentas;

    @Column(name = "numero_pedidos", nullable = false)
    private int numeroPedidos;

    @ManyToMany
    @JoinTable(name = "reporte_venta_pedido",
            joinColumns = @JoinColumn(name = "id_reporte"),
            inverseJoinColumns = @JoinColumn(name = "id_pedido"))
    private List<Pedido> pedidos = new ArrayList<>();

    public ReporteVenta() {
    }

    public ReporteVenta(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio");
        }
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    /**
     * Calcula los totales con los pedidos pagados de la lista.
     */
    public void generar(List<Pedido> pedidosDelPeriodo) {
        pedidos.clear();
        totalVentas = 0;
        for (Pedido p : pedidosDelPeriodo) {
            if (p.getEstado() == EstadoPedido.PAGADO) {
                pedidos.add(p);
                totalVentas += p.getTotal() != null ? p.getTotal() : 0;
            }
        }
        numeroPedidos = pedidos.size();
    }

    public Integer getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Integer idReporte) {
        this.idReporte = idReporte;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public double getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(double totalVentas) {
        this.totalVentas = totalVentas;
    }

    public int getNumeroPedidos() {
        return numeroPedidos;
    }

    public void setNumeroPedidos(int numeroPedidos) {
        this.numeroPedidos = numeroPedidos;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public void setPedidos(List<Pedido> pedidos) {
        this.pedidos = pedidos;
    }
}

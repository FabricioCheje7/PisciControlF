
package pe.edu.upeu.piscicontrolf.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Venta {

    private Long id;
    private Long produccionId;
    private LocalDate fecha;
    private String cliente;
    private BigDecimal cantidadKg;
    private BigDecimal precioUnitarioKg;
    private BigDecimal total;
    private String estado;
    private String observaciones;

    // CONSTRUCTOR VACIO
    public Venta() {
    }

    // CONSTRUCTOR COMPLETO
    public Venta(
            Long id,
            Long produccionId,
            LocalDate fecha,
            String cliente,
            BigDecimal cantidadKg,
            BigDecimal precioUnitarioKg,
            BigDecimal total,
            String estado,
            String observaciones) {

        this.id = id;
        this.produccionId = produccionId;
        this.fecha = fecha;
        this.cliente = cliente;
        this.cantidadKg = cantidadKg;
        this.precioUnitarioKg = precioUnitarioKg;
        this.total = total;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProduccionId() {
        return produccionId;
    }

    public void setProduccionId(Long produccionId) {
        this.produccionId = produccionId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public BigDecimal getCantidadKg() {
        return cantidadKg;
    }

    public void setCantidadKg(BigDecimal cantidadKg) {
        this.cantidadKg = cantidadKg;
    }

    public BigDecimal getPrecioUnitarioKg() {
        return precioUnitarioKg;
    }

    public void setPrecioUnitarioKg(
            BigDecimal precioUnitarioKg) {

        this.precioUnitarioKg = precioUnitarioKg;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}


package pe.edu.upeu.piscicontrolf.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Produccion {

    private Long id;
    private Long loteId;
    private LocalDate fecha;
    private int cantidadTruchas;
    private BigDecimal pesoTotalKg;
    private String estado;
    private String observaciones;

    // CONSTRUCTOR VACIO
    public Produccion() {
    }

    // CONSTRUCTOR CON PARAMETROS
    public Produccion(
            Long id,
            Long loteId,
            LocalDate fecha,
            int cantidadTruchas,
            BigDecimal pesoTotalKg,
            String estado,
            String observaciones) {

        this.id = id;
        this.loteId = loteId;
        this.fecha = fecha;
        this.cantidadTruchas = cantidadTruchas;
        this.pesoTotalKg = pesoTotalKg;
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

    public Long getLoteId() {
        return loteId;
    }

    public void setLoteId(Long loteId) {
        this.loteId = loteId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getCantidadTruchas() {
        return cantidadTruchas;
    }

    public void setCantidadTruchas(int cantidadTruchas) {
        this.cantidadTruchas = cantidadTruchas;
    }

    public BigDecimal getPesoTotalKg() {
        return pesoTotalKg;
    }

    public void setPesoTotalKg(BigDecimal pesoTotalKg) {
        this.pesoTotalKg = pesoTotalKg;
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

    @Override
    public String toString() {
        return "Producción " + id;
    }
}

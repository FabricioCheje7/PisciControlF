
package pe.edu.upeu.piscicontrolf.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Alimentacion {

    private Long id;
    private Long loteId;
    private LocalDate fecha;
    private String tipoAlimento;
    private BigDecimal cantidadKg;
    private String observaciones;

    // CONSTRUCTOR VACIO
    public Alimentacion() {
    }

    // CONSTRUCTOR CON PARAMETROS
    public Alimentacion(
            Long id,
            Long loteId,
            LocalDate fecha,
            String tipoAlimento,
            BigDecimal cantidadKg,
            String observaciones) {

        this.id = id;
        this.loteId = loteId;
        this.fecha = fecha;
        this.tipoAlimento = tipoAlimento;
        this.cantidadKg = cantidadKg;
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

    public String getTipoAlimento() {
        return tipoAlimento;
    }

    public void setTipoAlimento(String tipoAlimento) {
        this.tipoAlimento = tipoAlimento;
    }

    public BigDecimal getCantidadKg() {
        return cantidadKg;
    }

    public void setCantidadKg(BigDecimal cantidadKg) {
        this.cantidadKg = cantidadKg;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public String toString() {
        return "Alimentación " + id;
    }
}

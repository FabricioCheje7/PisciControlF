
package pe.edu.upeu.piscicontrolf.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Crecimiento {

    private Long id;
    private Long loteId;
    private LocalDate fecha;
    private BigDecimal pesoPromedioG;
    private BigDecimal tallaPromedioCm;
    private int cantidadMuestra;
    private String observaciones;

    // CONSTRUCTOR VACIO
    public Crecimiento() {
    }

    // CONSTRUCTOR CON PARAMETROS
    public Crecimiento(
            Long id,
            Long loteId,
            LocalDate fecha,
            BigDecimal pesoPromedioG,
            BigDecimal tallaPromedioCm,
            int cantidadMuestra,
            String observaciones) {

        this.id = id;
        this.loteId = loteId;
        this.fecha = fecha;
        this.pesoPromedioG = pesoPromedioG;
        this.tallaPromedioCm = tallaPromedioCm;
        this.cantidadMuestra = cantidadMuestra;
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

    public BigDecimal getPesoPromedioG() {
        return pesoPromedioG;
    }

    public void setPesoPromedioG(BigDecimal pesoPromedioG) {
        this.pesoPromedioG = pesoPromedioG;
    }

    public BigDecimal getTallaPromedioCm() {
        return tallaPromedioCm;
    }

    public void setTallaPromedioCm(BigDecimal tallaPromedioCm) {
        this.tallaPromedioCm = tallaPromedioCm;
    }

    public int getCantidadMuestra() {
        return cantidadMuestra;
    }

    public void setCantidadMuestra(int cantidadMuestra) {
        this.cantidadMuestra = cantidadMuestra;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public String toString() {
        return "Crecimiento " + id;
    }
}



package pe.edu.upeu.piscicontrolf.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CalidadAgua {

    // ATRIBUTOS
    private Long id;
    private Long estanqueId;
    private LocalDate fecha;

    private BigDecimal temperaturaC;
    private BigDecimal ph;
    private BigDecimal oxigenoMgL;

    private String estado;
    private String observaciones;

    // CONSTRUCTOR VACIO
    public CalidadAgua() {
    }

    // CONSTRUCTOR COMPLETO
    public CalidadAgua(
            Long id,
            Long estanqueId,
            LocalDate fecha,
            BigDecimal temperaturaC,
            BigDecimal ph,
            BigDecimal oxigenoMgL,
            String estado,
            String observaciones) {

        this.id = id;
        this.estanqueId = estanqueId;
        this.fecha = fecha;
        this.temperaturaC = temperaturaC;
        this.ph = ph;
        this.oxigenoMgL = oxigenoMgL;
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

    public Long getEstanqueId() {
        return estanqueId;
    }

    public void setEstanqueId(Long estanqueId) {
        this.estanqueId = estanqueId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTemperaturaC() {
        return temperaturaC;
    }

    public void setTemperaturaC(BigDecimal temperaturaC) {
        this.temperaturaC = temperaturaC;
    }

    public BigDecimal getPh() {
        return ph;
    }

    public void setPh(BigDecimal ph) {
        this.ph = ph;
    }

    public BigDecimal getOxigenoMgL() {
        return oxigenoMgL;
    }

    public void setOxigenoMgL(BigDecimal oxigenoMgL) {
        this.oxigenoMgL = oxigenoMgL;
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


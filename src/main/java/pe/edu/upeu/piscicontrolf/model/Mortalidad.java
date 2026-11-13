
package pe.edu.upeu.piscicontrolf.model;

import java.time.LocalDate;

public class Mortalidad {

    // ATRIBUTOS
    private Long id;
    private Long loteId;
    private LocalDate fecha;
    private String tipoIncidencia;
    private Integer cantidadMuertas;
    private String causa;
    private String estado;
    private String acciones;
    private String observaciones;

    // CONSTRUCTOR VACIO
    public Mortalidad() {
    }

    // CONSTRUCTOR COMPLETO
    public Mortalidad(
            Long id,
            Long loteId,
            LocalDate fecha,
            String tipoIncidencia,
            Integer cantidadMuertas,
            String causa,
            String estado,
            String acciones,
            String observaciones) {

        this.id = id;
        this.loteId = loteId;
        this.fecha = fecha;
        this.tipoIncidencia = tipoIncidencia;
        this.cantidadMuertas = cantidadMuertas;
        this.causa = causa;
        this.estado = estado;
        this.acciones = acciones;
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

    public String getTipoIncidencia() {
        return tipoIncidencia;
    }

    public void setTipoIncidencia(String tipoIncidencia) {
        this.tipoIncidencia = tipoIncidencia;
    }

    public Integer getCantidadMuertas() {
        return cantidadMuertas;
    }

    public void setCantidadMuertas(Integer cantidadMuertas) {
        this.cantidadMuertas = cantidadMuertas;
    }

    public String getCausa() {
        return causa;
    }

    public void setCausa(String causa) {
        this.causa = causa;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getAcciones() {
        return acciones;
    }

    public void setAcciones(String acciones) {
        this.acciones = acciones;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}


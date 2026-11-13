
package pe.edu.upeu.piscicontrolf.model;

import java.time.LocalDate;

public class Lote {

    private Long id;
    private Long estanqueId;
    private LocalDate fechaIngreso;
    private int cantidadTruchas;
    private String observaciones;

    // Constructor vacío
    public Lote() {
    }

    // Constructor con parámetros
    public Lote(Long id, Long estanqueId,
                LocalDate fechaIngreso,
                int cantidadTruchas,
                String observaciones) {

        this.id = id;
        this.estanqueId = estanqueId;
        this.fechaIngreso = fechaIngreso;
        this.cantidadTruchas = cantidadTruchas;
        this.observaciones = observaciones;
    }

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

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public int getCantidadTruchas() {
        return cantidadTruchas;
    }

    public void setCantidadTruchas(int cantidadTruchas) {
        this.cantidadTruchas = cantidadTruchas;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public String toString() {
        return "Lote " + id;
    }
}

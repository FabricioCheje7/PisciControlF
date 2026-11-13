
package pe.edu.upeu.piscicontrolf.model;

public class Estanque {

    private Long id;
    private String nombre;
    private int capacidad;
    private String ubicacion;
    private String estado;

    // Constructor vacío
    public Estanque() {
    }

    // Constructor con parámetros
    public Estanque(Long id, String nombre,
                    int capacidad, String ubicacion,
                    String estado) {
        this.id = id;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.ubicacion = ubicacion;
        this.estado = estado;
    }

    // Métodos GET y SET

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return nombre;
    }
}


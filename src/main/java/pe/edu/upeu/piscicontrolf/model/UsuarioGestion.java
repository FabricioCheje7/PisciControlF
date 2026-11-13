
package pe.edu.upeu.piscicontrolf.model;

public class UsuarioGestion {

    private Long id;
    private String nombre;
    private String usuario;
    private String rol;
    private String estado;

    // CONSTRUCTOR VACIO
    public UsuarioGestion() {
    }

    // CONSTRUCTOR COMPLETO
    public UsuarioGestion(
            Long id,
            String nombre,
            String usuario,
            String rol,
            String estado) {

        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.rol = rol;
        this.estado = estado;
    }

    // GETTERS Y SETTERS

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

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    // CONSULTAR ESTADO
    public boolean estaActivo() {
        return "ACTIVO".equalsIgnoreCase(estado);
    }

    // REPRESENTACION DEL USUARIO
    @Override
    public String toString() {
        return nombre + " (" + usuario + ")";
    }
}


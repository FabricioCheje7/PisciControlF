
package pe.edu.upeu.piscicontrolf.model;

public class Usuario {

    private Long id;
    private String nombre;
    private String usuario;
    private String passwordHash;
    private String rol;
    private String estado;

    public Usuario() {
    }

    public Usuario(Long id, String nombre,
                   String usuario, String passwordHash,
                   String rol, String estado) {

        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.estado = estado;
    }

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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
}


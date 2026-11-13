
package pe.edu.upeu.piscicontrolf.config;

import pe.edu.upeu.piscicontrolf.model.Usuario;

public class SesionUsuario {

    private static Usuario usuarioActual;

    // GUARDAR USUARIO QUE INICIO SESION
    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    // OBTENER USUARIO ACTUAL
    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    // COMPROBAR ADMINISTRADOR
    public static boolean esAdministrador() {
        return usuarioActual != null
                && "ADMINISTRADOR".equals(
                usuarioActual.getRol());
    }

    // COMPROBAR OPERADOR
    public static boolean esOperador() {
        return usuarioActual != null
                && "OPERADOR".equals(
                usuarioActual.getRol());
    }

    // CERRAR SESION
    public static void cerrarSesion() {
        usuarioActual = null;
    }

    // COMPROBAR QUE EXISTA UNA SESION
    public static boolean haySesion() {
        return usuarioActual != null;
    }
}

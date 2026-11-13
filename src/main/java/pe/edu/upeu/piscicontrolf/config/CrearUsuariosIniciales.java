
package pe.edu.upeu.piscicontrolf.config;

import pe.edu.upeu.piscicontrolf.service.AutenticacionService;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

public class CrearUsuariosIniciales {

    private static final Scanner teclado =
            new Scanner(System.in);

    public static void main(String[] args) {

        try (Connection con =
                     DatabaseConfig.getConnection()) {

            System.out.println(
                    "=== CONFIGURACION DE PISCICONTROL ===");

            crearUsuario(con, "ADMINISTRADOR");
            crearUsuario(con, "OPERADOR");

            System.out.println(
                    "\nConfiguracion finalizada.");

        } catch (Exception e) {
            System.err.println(
                    "Error: " + e.getMessage());
            e.printStackTrace();

        } finally {
            DatabaseConfig.cerrar();
        }
    }

    private static void crearUsuario(
            Connection con, String rol)
            throws SQLException {

        System.out.println(
                "\n--- CREAR " + rol + " ---");

        System.out.print("Nombre completo: ");
        String nombre = teclado.nextLine().trim();

        System.out.print("Nombre de usuario: ");
        String usuario = teclado.nextLine().trim();

        if (nombre.isBlank() || usuario.isBlank()
                || nombre.length() > 100
                || usuario.length() > 50) {

            System.out.println(
                    "Nombre o usuario no valido.");
            return;
        }

        String consulta =
                "SELECT id FROM usuarios WHERE usuario = ?";

        try (PreparedStatement ps =
                     con.prepareStatement(consulta)) {

            ps.setString(1, usuario);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    System.out.println(
                            "Ese usuario ya existe. "
                                    + "No se modifico su cuenta.");
                    return;
                }
            }
        }

        String password;

        while (true) {

            password = leerContrasena();

            if (password.length() >= 8) {
                break;
            }

            System.out.println(
                    "La contraseña necesita "
                            + "al menos 8 caracteres.");
        }

        String hash =
                AutenticacionService.generarHash(password);

        String sql = """
                INSERT INTO usuarios
                (nombre, usuario, password_hash, rol, estado)
                VALUES (?, ?, ?, ?, 'ACTIVO')
                """;

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, usuario);
            ps.setString(3, hash);
            ps.setString(4, rol);

            ps.executeUpdate();

            System.out.println(
                    "Cuenta de " + rol
                            + " creada correctamente.");
        }
    }

    private static String leerContrasena() {

        Console consola = System.console();

        if (consola != null) {

            char[] clave =
                    consola.readPassword("Contraseña: ");

            if (clave == null) {
                throw new IllegalStateException(
                        "No se ingreso una contraseña.");
            }

            try {
                return new String(clave);

            } finally {
                Arrays.fill(clave, '\0');
            }
        }

        // En IntelliJ la entrada puede ser visible.
        System.out.println(
                "Aviso: la contraseña sera visible "
                        + "mientras la escribe en IntelliJ.");

        System.out.print("Contraseña: ");
        return teclado.nextLine();
    }
}

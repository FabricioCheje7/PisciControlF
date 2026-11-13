
package pe.edu.upeu.piscicontrolf.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class PruebaConexion {

    public static void main(String[] args) {

        try (Connection conexion =
                     DatabaseConfig.getConnection()) {

            if (conexion.isValid(3)) {

                System.out.println(
                        "Conexión exitosa con PisciControl");

                System.out.println(
                        "Base de datos: " +
                                conexion.getMetaData()
                                        .getDatabaseProductName());

                System.out.println(
                        "URL: " +
                                conexion.getMetaData().getURL());

                try (Statement st = conexion.createStatement();
                     ResultSet rs = st.executeQuery("SELECT 1")) {

                    if (rs.next() && rs.getInt(1) == 1) {
                        System.out.println(
                                "Consulta SQL ejecutada correctamente");
                    }
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "Error de conexión: " + e.getMessage());

            e.printStackTrace();

        } finally {
            DatabaseConfig.cerrar();
        }
    }
}


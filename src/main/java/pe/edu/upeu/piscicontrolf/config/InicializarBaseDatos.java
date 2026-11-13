
package pe.edu.upeu.piscicontrolf.config;

import org.h2.tools.RunScript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class InicializarBaseDatos {

    public static void main(String[] args) {

        try (Connection con = DatabaseConfig.getConnection()) {

            InputStream archivo =
                    InicializarBaseDatos.class
                            .getResourceAsStream(
                                    "/schema_piscicontrol.sql");

            if (archivo == null) {
                throw new IllegalStateException(
                        "No se encontró schema_piscicontrol.sql");
            }

            // EJECUTAR EL SCRIPT SQL
            try (InputStream entrada = archivo;
                 Reader lector = new InputStreamReader(
                         entrada, StandardCharsets.UTF_8)) {

                RunScript.execute(con, lector);
            }

            System.out.println(
                    "Script SQL ejecutado correctamente.");

            // COMPROBAR LAS 7 TABLAS
            String consulta = """
                    SELECT TABLE_NAME
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = 'PUBLIC'
                    AND TABLE_NAME IN (
                        'USUARIOS',
                        'ESTANQUES',
                        'LOTES',
                        'ALIMENTACION',
                        'CRECIMIENTO',
                        'PRODUCCION',
                        'VENTAS',
                        'CALIDAD_AGUA',
                        'MORTALIDAD'
                    )
                    ORDER BY TABLE_NAME
                    """;

            int contador = 0;

            try (PreparedStatement ps =
                         con.prepareStatement(consulta);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    System.out.println(
                            "Tabla encontrada: "
                                    + rs.getString("TABLE_NAME"));

                    contador++;
                }
            }

            // VERIFICAR QUE EXISTAN TODAS
            if (contador != 9) {
                throw new IllegalStateException(
                        "Faltan tablas en la base de datos. "
                                + "Encontradas: " + contador
                                + " de 8.");
            }

            System.out.println(
                    "================================");

            System.out.println(
                    "BASE DE DATOS PISCICONTROLF PREPARADA");

            System.out.println(
                    "Total de tablas verificadas: "
                            + contador);

            System.out.println(
                    "================================");

        } catch (Exception e) {

            System.err.println(
                    "Error: " + e.getMessage());

            e.printStackTrace();

        } finally {

            DatabaseConfig.cerrar();
        }
    }
}

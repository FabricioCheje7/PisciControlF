
package pe.edu.upeu.piscicontrolf.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConfig {

    private static final HikariDataSource DATA_SOURCE =
            crearDataSource();

    private DatabaseConfig() {
    }

    private static HikariDataSource crearDataSource() {

        Properties propiedades = new Properties();

        try (InputStream archivo =
                     DatabaseConfig.class.getResourceAsStream(
                             "/application.properties")) {

            if (archivo == null) {
                throw new IllegalStateException(
                        "No se encontró application.properties");
            }

            propiedades.load(archivo);

            // Crear carpeta para la base de datos H2
            Files.createDirectories(Path.of("data"));

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al cargar la configuración", e);
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(
                propiedades.getProperty("db.url"));

        config.setUsername(
                propiedades.getProperty("db.username"));

        config.setPassword(
                propiedades.getProperty("db.password", ""));

        config.setMaximumPoolSize(5);
        config.setPoolName("PisciControlPool");

        return new HikariDataSource(config);
    }

    public static Connection getConnection()
            throws SQLException {

        return DATA_SOURCE.getConnection();
    }

    public static void cerrar() {
        DATA_SOURCE.close();
    }
}

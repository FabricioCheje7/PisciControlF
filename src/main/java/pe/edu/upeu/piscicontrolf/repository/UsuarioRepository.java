
package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioRepository {

    // BUSCAR USUARIO EN LA BASE DE DATOS
    public Usuario buscarPorUsuario(String nombreUsuario)
            throws SQLException {

        String sql = """
                SELECT id, nombre, usuario,
                       password_hash, rol, estado
                FROM usuarios
                WHERE usuario = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombreUsuario);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    return new Usuario(
                            rs.getLong("id"),
                            rs.getString("nombre"),
                            rs.getString("usuario"),
                            rs.getString("password_hash"),
                            rs.getString("rol"),
                            rs.getString("estado")
                    );
                }
            }
        }

        return null;
    }
}


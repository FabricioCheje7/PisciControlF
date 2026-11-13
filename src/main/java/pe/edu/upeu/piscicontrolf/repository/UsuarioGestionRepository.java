
package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.UsuarioGestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioGestionRepository {

    // LISTAR USUARIOS SIN MOSTRAR CONTRASEÑAS
    public List<UsuarioGestion> listar() throws SQLException {

        String sql = """
                SELECT id, nombre, usuario, rol, estado
                FROM usuarios
                ORDER BY id DESC
                """;

        List<UsuarioGestion> lista = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(convertir(rs));
            }
        }

        return lista;
    }

    // BUSCAR USUARIO POR ID
    public UsuarioGestion buscarPorId(Long id)
            throws SQLException {

        String sql = """
                SELECT id, nombre, usuario, rol, estado
                FROM usuarios
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? convertir(rs) : null;
            }
        }
    }

    // COMPROBAR NOMBRE DE USUARIO
    public boolean existeUsuario(String usuario, Long excluirId)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM usuarios
                WHERE LOWER(usuario) = LOWER(?)
                AND (? IS NULL OR id <> ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);

            if (excluirId == null) {
                ps.setNull(2, Types.BIGINT);
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(2, excluirId);
                ps.setLong(3, excluirId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    // REGISTRAR USUARIO
    // El servicio debe generar primero un hash seguro.
    public void registrar(
            UsuarioGestion usuario,
            String passwordHash) throws SQLException {

        String sql = """
                INSERT INTO usuarios
                    (nombre, usuario, password_hash, rol, estado)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, passwordHash);
            ps.setString(4, usuario.getRol());
            ps.setString(5, usuario.getEstado());

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo registrar el usuario.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getLong(1));
                }
            }
        }
    }

    // ACTUALIZAR DATOS, SIN CAMBIAR CONTRASEÑA
    public boolean actualizar(UsuarioGestion usuario)
            throws SQLException {

        String sql = """
                UPDATE usuarios
                SET nombre = ?,
                    usuario = ?,
                    rol = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, usuario.getRol());
            ps.setLong(4, usuario.getId());

            return ps.executeUpdate() == 1;
        }
    }

    // ACTIVAR O DESACTIVAR
    public boolean cambiarEstado(Long id, String estado)
            throws SQLException {

        String sql = """
                UPDATE usuarios
                SET estado = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setLong(2, id);

            return ps.executeUpdate() == 1;
        }
    }

    // RESTABLECER HASH DE CONTRASEÑA
    public boolean actualizarPasswordHash(
            Long id, String passwordHash) throws SQLException {

        String sql = """
                UPDATE usuarios
                SET password_hash = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setLong(2, id);

            return ps.executeUpdate() == 1;
        }
    }

    // CONTAR TODOS LOS USUARIOS
    public long contar() throws SQLException {
        return contarConsulta(
                "SELECT COUNT(*) FROM usuarios");
    }

    // CONTAR POR ESTADO
    public long contarPorEstado(String estado)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM usuarios
                WHERE UPPER(estado) = UPPER(?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    // CONTAR ADMINISTRADORES ACTIVOS
    public long contarAdministradoresActivos()
            throws SQLException {

        return contarConsulta("""
                SELECT COUNT(*)
                FROM usuarios
                WHERE UPPER(rol) = 'ADMINISTRADOR'
                AND UPPER(estado) = 'ACTIVO'
                """);
    }

    private long contarConsulta(String sql)
            throws SQLException {

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    // CONVERTIR RESULTADO SQL A OBJETO JAVA
    private UsuarioGestion convertir(ResultSet rs)
            throws SQLException {

        UsuarioGestion usuario = new UsuarioGestion();

        usuario.setId(rs.getLong("id"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setUsuario(rs.getString("usuario"));
        usuario.setRol(rs.getString("rol"));
        usuario.setEstado(rs.getString("estado"));

        return usuario;
    }
}



package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Estanque;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EstanqueRepository {

    // REGISTRAR ESTANQUE
    public void registrar(Estanque estanque) throws SQLException {

        String sql = """
                INSERT INTO estanques
                (nombre, capacidad, ubicacion, estado)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, estanque.getNombre());
            ps.setInt(2, estanque.getCapacidad());
            ps.setString(3, estanque.getUbicacion());
            ps.setString(4, "ACTIVO");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    estanque.setId(rs.getLong(1));
                }
            }
        }
    }

    // LISTAR ESTANQUES
    public List<Estanque> listar() throws SQLException {

        List<Estanque> lista = new ArrayList<>();

        String sql = """
                SELECT id, nombre, capacidad,
                       ubicacion, estado
                FROM estanques
                ORDER BY id DESC
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Estanque estanque = new Estanque(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getInt("capacidad"),
                        rs.getString("ubicacion"),
                        rs.getString("estado")
                );

                lista.add(estanque);
            }
        }

        return lista;
    }

    // MODIFICAR ESTANQUE
    public void actualizar(Estanque estanque) throws SQLException {

        String sql = """
                UPDATE estanques
                SET nombre = ?,
                    capacidad = ?,
                    ubicacion = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estanque.getNombre());
            ps.setInt(2, estanque.getCapacidad());
            ps.setString(3, estanque.getUbicacion());
            ps.setLong(4, estanque.getId());

            int filas = ps.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se encontró el estanque.");
            }
        }
    }

    // DESACTIVAR ESTANQUE
    public void desactivar(Long id) throws SQLException {

        String sql = """
                UPDATE estanques
                SET estado = 'INACTIVO'
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            int filas = ps.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se encontró el estanque.");
            }
        }
    }
}

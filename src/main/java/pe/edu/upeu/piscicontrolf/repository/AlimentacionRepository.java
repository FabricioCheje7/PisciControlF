
package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Alimentacion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlimentacionRepository {

    // REGISTRAR ALIMENTACION
    public void registrar(Alimentacion alimentacion)
            throws SQLException {

        String sql = """
                INSERT INTO alimentacion
                (lote_id, fecha, tipo_alimento,
                 cantidad_kg, observaciones)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, alimentacion.getLoteId());
            ps.setDate(2,
                    Date.valueOf(alimentacion.getFecha()));
            ps.setString(3,
                    alimentacion.getTipoAlimento());
            ps.setBigDecimal(4,
                    alimentacion.getCantidadKg());
            ps.setString(5,
                    alimentacion.getObservaciones());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    alimentacion.setId(rs.getLong(1));
                }
            }
        }
    }

    // LISTAR REGISTROS DE ALIMENTACION
    public List<Alimentacion> listar()
            throws SQLException {

        List<Alimentacion> lista = new ArrayList<>();

        String sql = """
                SELECT id, lote_id, fecha,
                       tipo_alimento, cantidad_kg,
                       observaciones
                FROM alimentacion
                ORDER BY fecha DESC, id DESC
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }

        return lista;
    }

    // CONSULTAR ALIMENTACION POR LOTE
    public List<Alimentacion> listarPorLote(Long loteId)
            throws SQLException {

        List<Alimentacion> lista = new ArrayList<>();

        String sql = """
                SELECT id, lote_id, fecha,
                       tipo_alimento, cantidad_kg,
                       observaciones
                FROM alimentacion
                WHERE lote_id = ?
                ORDER BY fecha DESC, id DESC
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, loteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }

        return lista;
    }

    // MODIFICAR ALIMENTACION
    public void actualizar(Alimentacion alimentacion)
            throws SQLException {

        String sql = """
                UPDATE alimentacion
                SET lote_id = ?,
                    fecha = ?,
                    tipo_alimento = ?,
                    cantidad_kg = ?,
                    observaciones = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, alimentacion.getLoteId());
            ps.setDate(2,
                    Date.valueOf(alimentacion.getFecha()));
            ps.setString(3,
                    alimentacion.getTipoAlimento());
            ps.setBigDecimal(4,
                    alimentacion.getCantidadKg());
            ps.setString(5,
                    alimentacion.getObservaciones());
            ps.setLong(6, alimentacion.getId());

            int filas = ps.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se encontró el registro de alimentación.");
            }
        }
    }

    // CONVERTIR RESULTADOS SQL EN OBJETOS JAVA
    private Alimentacion mapear(ResultSet rs)
            throws SQLException {

        return new Alimentacion(
                rs.getLong("id"),
                rs.getLong("lote_id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getString("tipo_alimento"),
                rs.getBigDecimal("cantidad_kg"),
                rs.getString("observaciones")
        );
    }
}

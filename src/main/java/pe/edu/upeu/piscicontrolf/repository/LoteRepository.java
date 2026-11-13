
package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Lote;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoteRepository {

    // REGISTRAR LOTE
    public void registrar(Lote lote) throws SQLException {

        String sql = """
                INSERT INTO lotes
                (estanque_id, fecha_ingreso,
                 cantidad_truchas, observaciones)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, lote.getEstanqueId());
            ps.setDate(2, Date.valueOf(lote.getFechaIngreso()));
            ps.setInt(3, lote.getCantidadTruchas());
            ps.setString(4, lote.getObservaciones());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    lote.setId(rs.getLong(1));
                }
            }
        }
    }

    // LISTAR TODOS LOS LOTES
    public List<Lote> listar() throws SQLException {

        List<Lote> lista = new ArrayList<>();

        String sql = """
                SELECT id, estanque_id, fecha_ingreso,
                       cantidad_truchas, observaciones
                FROM lotes
                ORDER BY fecha_ingreso DESC, id DESC
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Lote lote = new Lote(
                        rs.getLong("id"),
                        rs.getLong("estanque_id"),
                        rs.getDate("fecha_ingreso").toLocalDate(),
                        rs.getInt("cantidad_truchas"),
                        rs.getString("observaciones")
                );

                lista.add(lote);
            }
        }

        return lista;
    }

    // CONSULTAR LOTES DE UN ESTANQUE
    public List<Lote> listarPorEstanque(Long estanqueId)
            throws SQLException {

        List<Lote> lista = new ArrayList<>();

        String sql = """
                SELECT id, estanque_id, fecha_ingreso,
                       cantidad_truchas, observaciones
                FROM lotes
                WHERE estanque_id = ?
                ORDER BY fecha_ingreso DESC, id DESC
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, estanqueId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Lote lote = new Lote(
                            rs.getLong("id"),
                            rs.getLong("estanque_id"),
                            rs.getDate("fecha_ingreso").toLocalDate(),
                            rs.getInt("cantidad_truchas"),
                            rs.getString("observaciones")
                    );

                    lista.add(lote);
                }
            }
        }

        return lista;
    }

    // MODIFICAR LOTE
    public void actualizar(Lote lote) throws SQLException {

        String sql = """
                UPDATE lotes
                SET estanque_id = ?,
                    fecha_ingreso = ?,
                    cantidad_truchas = ?,
                    observaciones = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, lote.getEstanqueId());
            ps.setDate(2, Date.valueOf(lote.getFechaIngreso()));
            ps.setInt(3, lote.getCantidadTruchas());
            ps.setString(4, lote.getObservaciones());
            ps.setLong(5, lote.getId());

            int filas = ps.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se encontró el lote seleccionado.");
            }
        }
    }

    // CONSULTAR CAPACIDAD DISPONIBLE DEL ESTANQUE
    public int obtenerCapacidadDisponible(
            Long estanqueId, Long loteExcluidoId)
            throws SQLException {

        String sql = """
                SELECT e.capacidad -
                    COALESCE((
                        SELECT SUM(l.cantidad_truchas)
                        FROM lotes l
                        WHERE l.estanque_id = e.id
                          AND l.id <> ?
                    ), 0) AS disponible
                FROM estanques e
                WHERE e.id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1,
                    loteExcluidoId == null ? -1L : loteExcluidoId);

            ps.setLong(2, estanqueId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("disponible");
                }

                throw new SQLException(
                        "El estanque no existe.");
            }
        }
    }

    // BUSCAR LOTE POR ID
    public Lote buscarPorId(Long id) throws SQLException {

        String sql = """
                SELECT id, estanque_id, fecha_ingreso,
                       cantidad_truchas, observaciones
                FROM lotes
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return new Lote(
                            rs.getLong("id"),
                            rs.getLong("estanque_id"),
                            rs.getDate("fecha_ingreso").toLocalDate(),
                            rs.getInt("cantidad_truchas"),
                            rs.getString("observaciones")
                    );
                }
            }
        }

        return null;
    }


}

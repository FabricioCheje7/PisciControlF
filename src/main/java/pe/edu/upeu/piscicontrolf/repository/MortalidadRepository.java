
package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Mortalidad;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MortalidadRepository {

    // REGISTRAR INCIDENCIA
    public void registrar(Mortalidad mortalidad)
            throws SQLException {

        String sql = """
                INSERT INTO mortalidad (
                    lote_id,
                    fecha,
                    tipo_incidencia,
                    cantidad_muertas,
                    causa,
                    estado,
                    acciones,
                    observaciones
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, mortalidad.getLoteId());
            ps.setDate(2, Date.valueOf(mortalidad.getFecha()));
            ps.setString(3, mortalidad.getTipoIncidencia());
            ps.setInt(4, mortalidad.getCantidadMuertas());
            ps.setString(5, mortalidad.getCausa());
            ps.setString(6, mortalidad.getEstado());
            ps.setString(7, mortalidad.getAcciones());
            ps.setString(8, mortalidad.getObservaciones());

            int afectados = ps.executeUpdate();

            if (afectados != 1) {
                throw new SQLException(
                        "No se pudo registrar la incidencia.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    mortalidad.setId(rs.getLong(1));
                }
            }
        }
    }

    // LISTAR TODAS LAS INCIDENCIAS
    public List<Mortalidad> listar()
            throws SQLException {

        String sql = """
                SELECT *
                FROM mortalidad
                ORDER BY fecha DESC, id DESC
                """;

        List<Mortalidad> lista = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(convertir(rs));
            }
        }

        return lista;
    }

    // BUSCAR INCIDENCIA POR ID
    public Mortalidad buscarPorId(Long id)
            throws SQLException {

        String sql = """
                SELECT *
                FROM mortalidad
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return convertir(rs);
                }
            }
        }

        return null;
    }

    // CONSULTAR INCIDENCIAS POR LOTE
    public List<Mortalidad> listarPorLote(Long loteId)
            throws SQLException {

        String sql = """
                SELECT *
                FROM mortalidad
                WHERE lote_id = ?
                ORDER BY fecha DESC, id DESC
                """;

        List<Mortalidad> lista = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, loteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(convertir(rs));
                }
            }
        }

        return lista;
    }

    // MODIFICAR INCIDENCIA
    public boolean actualizar(Mortalidad mortalidad)
            throws SQLException {

        String sql = """
                UPDATE mortalidad
                SET lote_id = ?,
                    fecha = ?,
                    tipo_incidencia = ?,
                    cantidad_muertas = ?,
                    causa = ?,
                    estado = ?,
                    acciones = ?,
                    observaciones = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, mortalidad.getLoteId());
            ps.setDate(2, Date.valueOf(mortalidad.getFecha()));
            ps.setString(3, mortalidad.getTipoIncidencia());
            ps.setInt(4, mortalidad.getCantidadMuertas());
            ps.setString(5, mortalidad.getCausa());
            ps.setString(6, mortalidad.getEstado());
            ps.setString(7, mortalidad.getAcciones());
            ps.setString(8, mortalidad.getObservaciones());
            ps.setLong(9, mortalidad.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // CONTAR TODAS LAS INCIDENCIAS
    public long contar() throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM mortalidad
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    // TOTAL DE TRUCHAS MUERTAS REGISTRADAS
    public long totalMuertas() throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(cantidad_muertas), 0)
                FROM mortalidad
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    // TOTAL DE CASOS ABIERTOS
    public long contarCasosAbiertos() throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM mortalidad
                WHERE estado = 'ABIERTA'
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    // TRUCHAS MUERTAS EN UN LOTE
    public long totalMuertasPorLote(Long loteId)
            throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(cantidad_muertas), 0)
                FROM mortalidad
                WHERE lote_id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, loteId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    // CONVERTIR RESULTSET A OBJETO MORTALIDAD
    private Mortalidad convertir(ResultSet rs)
            throws SQLException {

        Mortalidad mortalidad = new Mortalidad();

        mortalidad.setId(rs.getLong("id"));
        mortalidad.setLoteId(rs.getLong("lote_id"));

        Date fecha = rs.getDate("fecha");
        mortalidad.setFecha(
                fecha != null ? fecha.toLocalDate() : null
        );

        mortalidad.setTipoIncidencia(
                rs.getString("tipo_incidencia")
        );

        mortalidad.setCantidadMuertas(
                rs.getInt("cantidad_muertas")
        );

        mortalidad.setCausa(
                rs.getString("causa")
        );

        mortalidad.setEstado(
                rs.getString("estado")
        );

        mortalidad.setAcciones(
                rs.getString("acciones")
        );

        mortalidad.setObservaciones(
                rs.getString("observaciones")
        );

        return mortalidad;
    }
}


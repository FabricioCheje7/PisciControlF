
package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Crecimiento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CrecimientoRepository {

    // REGISTRAR CRECIMIENTO
    public void registrar(Crecimiento crecimiento)
            throws SQLException {

        String sql = """
                INSERT INTO crecimiento
                (lote_id, fecha, peso_promedio_g,
                 talla_promedio_cm, cantidad_muestra,
                 observaciones)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, crecimiento.getLoteId());

            ps.setDate(2,
                    Date.valueOf(crecimiento.getFecha()));

            ps.setBigDecimal(3,
                    crecimiento.getPesoPromedioG());

            ps.setBigDecimal(4,
                    crecimiento.getTallaPromedioCm());

            ps.setInt(5,
                    crecimiento.getCantidadMuestra());

            ps.setString(6,
                    crecimiento.getObservaciones());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    crecimiento.setId(rs.getLong(1));
                }
            }
        }
    }

    // LISTAR TODOS LOS REGISTROS
    public List<Crecimiento> listar()
            throws SQLException {

        List<Crecimiento> lista = new ArrayList<>();

        String sql = """
                SELECT id, lote_id, fecha,
                       peso_promedio_g,
                       talla_promedio_cm,
                       cantidad_muestra,
                       observaciones
                FROM crecimiento
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

    // CONSULTAR CRECIMIENTO POR LOTE
    public List<Crecimiento> listarPorLote(Long loteId)
            throws SQLException {

        List<Crecimiento> lista = new ArrayList<>();

        String sql = """
                SELECT id, lote_id, fecha,
                       peso_promedio_g,
                       talla_promedio_cm,
                       cantidad_muestra,
                       observaciones
                FROM crecimiento
                WHERE lote_id = ?
                ORDER BY fecha ASC, id ASC
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

    // MODIFICAR CRECIMIENTO
    public void actualizar(Crecimiento crecimiento)
            throws SQLException {

        String sql = """
                UPDATE crecimiento
                SET lote_id = ?,
                    fecha = ?,
                    peso_promedio_g = ?,
                    talla_promedio_cm = ?,
                    cantidad_muestra = ?,
                    observaciones = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, crecimiento.getLoteId());

            ps.setDate(2,
                    Date.valueOf(crecimiento.getFecha()));

            ps.setBigDecimal(3,
                    crecimiento.getPesoPromedioG());

            ps.setBigDecimal(4,
                    crecimiento.getTallaPromedioCm());

            ps.setInt(5,
                    crecimiento.getCantidadMuestra());

            ps.setString(6,
                    crecimiento.getObservaciones());

            ps.setLong(7, crecimiento.getId());

            int filas = ps.executeUpdate();

            if (filas == 0) {
                throw new SQLException(
                        "No se encontró el registro de crecimiento.");
            }
        }
    }

    // CONVERTIR DATOS SQL A OBJETOS JAVA
    private Crecimiento mapear(ResultSet rs)
            throws SQLException {

        return new Crecimiento(
                rs.getLong("id"),
                rs.getLong("lote_id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getBigDecimal("peso_promedio_g"),
                rs.getBigDecimal("talla_promedio_cm"),
                rs.getInt("cantidad_muestra"),
                rs.getString("observaciones")
        );
    }
}

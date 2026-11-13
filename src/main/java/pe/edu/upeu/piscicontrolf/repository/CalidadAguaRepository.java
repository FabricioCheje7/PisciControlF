
package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.CalidadAgua;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CalidadAguaRepository {

    // REGISTRAR UNA MEDICION
    public void registrar(CalidadAgua calidad) throws SQLException {

        String sql = """
                INSERT INTO calidad_agua (
                    estanque_id,
                    fecha,
                    temperatura_c,
                    ph,
                    oxigeno_mg_l,
                    estado,
                    observaciones
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, calidad.getEstanqueId());
            ps.setDate(2, Date.valueOf(calidad.getFecha()));
            ps.setBigDecimal(3, calidad.getTemperaturaC());
            ps.setBigDecimal(4, calidad.getPh());
            ps.setBigDecimal(5, calidad.getOxigenoMgL());
            ps.setString(6, calidad.getEstado());
            ps.setString(7, calidad.getObservaciones());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    calidad.setId(rs.getLong(1));
                }
            }
        }
    }

    // LISTAR TODAS LAS MEDICIONES
    public List<CalidadAgua> listar() throws SQLException {

        String sql = """
                SELECT *
                FROM calidad_agua
                ORDER BY fecha DESC, id DESC
                """;

        List<CalidadAgua> lista = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(convertir(rs));
            }
        }

        return lista;
    }

    // BUSCAR UNA MEDICION POR ID
    public CalidadAgua buscarPorId(Long id) throws SQLException {

        String sql = """
                SELECT *
                FROM calidad_agua
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

    // LISTAR MEDICIONES DE UN ESTANQUE
    public List<CalidadAgua> listarPorEstanque(Long estanqueId)
            throws SQLException {

        String sql = """
                SELECT *
                FROM calidad_agua
                WHERE estanque_id = ?
                ORDER BY fecha DESC, id DESC
                """;

        List<CalidadAgua> lista = new ArrayList<>();

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, estanqueId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(convertir(rs));
                }
            }
        }

        return lista;
    }

    // MODIFICAR UNA MEDICION EXISTENTE
    public boolean actualizar(CalidadAgua calidad)
            throws SQLException {

        String sql = """
                UPDATE calidad_agua
                SET estanque_id = ?,
                    fecha = ?,
                    temperatura_c = ?,
                    ph = ?,
                    oxigeno_mg_l = ?,
                    estado = ?,
                    observaciones = ?
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, calidad.getEstanqueId());
            ps.setDate(2, Date.valueOf(calidad.getFecha()));
            ps.setBigDecimal(3, calidad.getTemperaturaC());
            ps.setBigDecimal(4, calidad.getPh());
            ps.setBigDecimal(5, calidad.getOxigenoMgL());
            ps.setString(6, calidad.getEstado());
            ps.setString(7, calidad.getObservaciones());
            ps.setLong(8, calidad.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // CONTAR LAS MEDICIONES REGISTRADAS
    public long contar() throws SQLException {

        String sql = "SELECT COUNT(*) FROM calidad_agua";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // CONVERTIR UN REGISTRO SQL EN OBJETO JAVA
    private CalidadAgua convertir(ResultSet rs)
            throws SQLException {

        CalidadAgua calidad = new CalidadAgua();

        calidad.setId(rs.getLong("id"));
        calidad.setEstanqueId(rs.getLong("estanque_id"));

        Date fecha = rs.getDate("fecha");
        calidad.setFecha(
                fecha != null ? fecha.toLocalDate() : null
        );

        calidad.setTemperaturaC(
                rs.getBigDecimal("temperatura_c")
        );

        calidad.setPh(
                rs.getBigDecimal("ph")
        );

        calidad.setOxigenoMgL(
                rs.getBigDecimal("oxigeno_mg_l")
        );

        calidad.setEstado(
                rs.getString("estado")
        );

        calidad.setObservaciones(
                rs.getString("observaciones")
        );

        return calidad;
    }
}


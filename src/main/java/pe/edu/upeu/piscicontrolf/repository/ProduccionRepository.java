package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Produccion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ProduccionRepository {

    // REGISTRAR PRODUCCION: VALIDACION FINAL DEL STOCK EN LA MISMA TRANSACCION
    public void registrar(Produccion produccion) throws SQLException {
        String sql = """
                INSERT INTO produccion
                (lote_id, fecha, cantidad_truchas,
                 peso_total_kg, estado, observaciones)
                VALUES (?, ?, ?, ?, 'REGISTRADA', ?)
                """;

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                long capacidad = bloquearLote(con, produccion.getLoteId());
                comprobarStock(con, produccion.getLoteId(), null,
                        produccion.getCantidadTruchas(), capacidad);

                try (PreparedStatement ps = con.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, produccion.getLoteId());
                    ps.setDate(2, Date.valueOf(produccion.getFecha()));
                    ps.setInt(3, produccion.getCantidadTruchas());
                    ps.setBigDecimal(4, produccion.getPesoTotalKg());
                    ps.setString(5, produccion.getObservaciones());

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("No se pudo registrar la producción.");
                    }
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            produccion.setId(rs.getLong(1));
                        }
                    }
                }

                con.commit();
                produccion.setEstado("REGISTRADA");
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // LISTAR TODAS LAS PRODUCCIONES, INCLUIDAS LAS ANULADAS
    public List<Produccion> listar() throws SQLException {
        List<Produccion> lista = new ArrayList<>();
        String sql = """
                SELECT id, lote_id, fecha, cantidad_truchas,
                       peso_total_kg, estado, observaciones
                FROM produccion
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

    // LISTAR PRODUCCIONES DE UN LOTE
    public List<Produccion> listarPorLote(Long loteId) throws SQLException {
        List<Produccion> lista = new ArrayList<>();
        String sql = """
                SELECT id, lote_id, fecha, cantidad_truchas,
                       peso_total_kg, estado, observaciones
                FROM produccion
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

    // ACTUALIZAR PRODUCCION SIN REACTIVAR ANULACIONES NI DESCONTAR DOS VECES
    public void actualizar(Produccion produccion) throws SQLException {
        String sql = """
                UPDATE produccion
                SET lote_id = ?, fecha = ?, cantidad_truchas = ?,
                    peso_total_kg = ?, observaciones = ?
                WHERE id = ? AND estado = 'REGISTRADA'
                """;

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                Long loteOriginal = bloquearProduccionActiva(
                        con, produccion.getId());

                // Una cosecha ya vendida no se debe cambiar sin ajustar ventas.
                verificarSinVentasRegistradas(con, produccion.getId());

                // Bloquear ambos lotes en orden fijo si cambió el lote.
                long nuevoId = produccion.getLoteId();
                long originalId = loteOriginal;
                long capacidadNuevo;
                if (originalId == nuevoId) {
                    capacidadNuevo = bloquearLote(con, nuevoId);
                } else if (originalId < nuevoId) {
                    bloquearLote(con, originalId);
                    capacidadNuevo = bloquearLote(con, nuevoId);
                } else {
                    capacidadNuevo = bloquearLote(con, nuevoId);
                    bloquearLote(con, originalId);
                }

                // Si se cambió el lote, la cosecha editada no resta en el nuevo lote.
                comprobarStock(con, nuevoId, produccion.getId(),
                        produccion.getCantidadTruchas(), capacidadNuevo);

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setLong(1, nuevoId);
                    ps.setDate(2, Date.valueOf(produccion.getFecha()));
                    ps.setInt(3, produccion.getCantidadTruchas());
                    ps.setBigDecimal(4, produccion.getPesoTotalKg());
                    ps.setString(5, produccion.getObservaciones());
                    ps.setLong(6, produccion.getId());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "La producción no existe o ya está anulada.");
                    }
                }
                con.commit();
                produccion.setEstado("REGISTRADA");
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ANULAR SIN ELIMINAR EL HISTORIAL NI DEJAR VENTAS ACTIVAS HUERFANAS
    public void anular(Long id) throws SQLException {
        String sql = """
                UPDATE produccion
                SET estado = 'ANULADA'
                WHERE id = ? AND estado = 'REGISTRADA'
                """;

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                bloquearProduccionActiva(con, id);
                verificarSinVentasRegistradas(con, id);

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setLong(1, id);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "La producción no existe o ya está anulada.");
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // CALCULAR COSECHAS ACTIVAS: LAS ANULADAS NO DESCUENTAN TRUCHAS
    public long obtenerCantidadCosechada(Long loteId, Long excluirId)
            throws SQLException {
        try (Connection con = DatabaseConfig.getConnection()) {
            return cantidadCosechada(con, loteId, excluirId);
        }
    }

    private long cantidadCosechada(Connection con, Long loteId,
                                   Long excluirId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(cantidad_truchas), 0) AS total
                FROM produccion
                WHERE lote_id = ? AND estado = 'REGISTRADA'
                """;
        if (excluirId != null) {
            sql += " AND id <> ?";
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, loteId);
            if (excluirId != null) {
                ps.setLong(2, excluirId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("total") : 0L;
            }
        }
    }

    // LOCK COMPARTIDO POR TODAS LAS COSECHAS DEL MISMO LOTE
    private long bloquearLote(Connection con, Long loteId)
            throws SQLException {
        String sql = """
                SELECT cantidad_truchas
                FROM lotes
                WHERE id = ?
                FOR UPDATE
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, loteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "El lote seleccionado no existe.");
                }
                return rs.getLong("cantidad_truchas");
            }
        }
    }

    private Long bloquearProduccionActiva(Connection con, Long id)
            throws SQLException {
        String sql = """
                SELECT lote_id, estado
                FROM produccion
                WHERE id = ?
                FOR UPDATE
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "La producción seleccionada no existe.");
                }
                if (!"REGISTRADA".equals(rs.getString("estado"))) {
                    throw new IllegalArgumentException(
                            "No se puede modificar ni anular una producción anulada.");
                }
                return rs.getLong("lote_id");
            }
        }
    }

    private void comprobarStock(Connection con, Long loteId,
                                Long excluirId, int cantidad,
                                long capacidadLote) throws SQLException {
        long cosechadas = cantidadCosechada(con, loteId, excluirId);
        long disponibles = capacidadLote - cosechadas;
        if (cantidad <= 0 || cantidad > disponibles) {
            throw new IllegalArgumentException(
                    "Cantidad insuficiente. Truchas disponibles: "
                            + Math.max(0L, disponibles));
        }
    }

    // ESTA CONSULTA REQUIERE ventas(produccion_id, estado) EN H2.
    // SI EL ESQUEMA DE VENTAS ES DISTINTO, ADAPTAR ANTES DE USAR ANULAR/MODIFICAR.
    private void verificarSinVentasRegistradas(Connection con, Long produccionId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total
                FROM ventas
                WHERE produccion_id = ? AND estado = 'REGISTRADA'
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, produccionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getLong("total") > 0) {
                    throw new IllegalArgumentException(
                            "Esta producción tiene ventas registradas. "
                                    + "No se puede modificar ni anular.");
                }
            }
        }
    }

    // CONVERTIR RESULTADOS SQL A OBJETOS JAVA
    private Produccion mapear(ResultSet rs) throws SQLException {
        return new Produccion(
                rs.getLong("id"),
                rs.getLong("lote_id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getInt("cantidad_truchas"),
                rs.getBigDecimal("peso_total_kg"),
                rs.getString("estado"),
                rs.getString("observaciones")
        );
    }
}

package pe.edu.upeu.piscicontrolf.repository;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Venta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VentaRepository {

    // REGISTRAR: EL STOCK SE VERIFICA DENTRO DE LA MISMA TRANSACCION.
    public void registrar(Venta venta) throws SQLException {
        validarDatos(venta);

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal pesoProduccion = bloquearProduccion(con, venta);
                BigDecimal vendidos = sumarKgVendidos(con, venta.getProduccionId(), null);
                comprobarStock(pesoProduccion, vendidos, venta.getCantidadKg());

                String sql = """
                        INSERT INTO ventas
                        (produccion_id, fecha, cliente, cantidad_kg,
                         precio_unitario_kg, total, estado, observaciones)
                        VALUES (?, ?, ?, ?, ?, ?, 'REGISTRADA', ?)
                        """;

                try (PreparedStatement ps = con.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, venta.getProduccionId());
                    ps.setDate(2, Date.valueOf(venta.getFecha()));
                    ps.setString(3, venta.getCliente());
                    ps.setBigDecimal(4, venta.getCantidadKg());
                    ps.setBigDecimal(5, venta.getPrecioUnitarioKg());
                    ps.setBigDecimal(6, venta.getTotal());
                    ps.setString(7, venta.getObservaciones());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            venta.setId(rs.getLong(1));
                        }
                    }
                }
                venta.setEstado("REGISTRADA");
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // LISTAR TODAS LAS VENTAS.
    public List<Venta> listar() throws SQLException {
        String sql = """
                SELECT id, produccion_id, fecha, cliente, cantidad_kg,
                       precio_unitario_kg, total, estado, observaciones
                FROM ventas
                ORDER BY fecha DESC, id DESC
                """;
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return leerLista(rs);
        }
    }

    // LISTAR LAS VENTAS DE UNA PRODUCCION.
    public List<Venta> listarPorProduccion(Long produccionId)
            throws SQLException {
        validarId(produccionId, "Seleccione una producción válida.");

        String sql = """
                SELECT id, produccion_id, fecha, cliente, cantidad_kg,
                       precio_unitario_kg, total, estado, observaciones
                FROM ventas
                WHERE produccion_id = ?
                ORDER BY fecha DESC, id DESC
                """;
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, produccionId);
            try (ResultSet rs = ps.executeQuery()) {
                return leerLista(rs);
            }
        }
    }

    // CONSULTAR UNA VENTA POR ID.
    public Venta buscarPorId(Long id) throws SQLException {
        validarId(id, "Seleccione una venta válida.");
        String sql = """
                SELECT id, produccion_id, fecha, cliente, cantidad_kg,
                       precio_unitario_kg, total, estado, observaciones
                FROM ventas WHERE id = ?
                """;
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // MODIFICAR UNA VENTA REGISTRADA.
    // LA PRODUCCION ASOCIADA NO PUEDE CAMBIAR EN ESTA OPERACION.
    public void actualizar(Venta venta) throws SQLException {
        validarDatos(venta);
        validarId(venta.getId(), "Seleccione una venta válida.");

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                Long produccionOriginal = buscarProduccionDeVenta(con, venta.getId());
                if (produccionOriginal == null) {
                    throw new IllegalArgumentException("La venta no existe.");
                }
                if (!Objects.equals(produccionOriginal, venta.getProduccionId())) {
                    throw new IllegalArgumentException(
                            "No se puede cambiar la producción de una venta. "
                            + "Anule la venta y registre otra.");
                }

                BigDecimal pesoProduccion = bloquearProduccion(con, venta);
                verificarVentaRegistrada(con, venta.getId(), produccionOriginal);

                BigDecimal vendidos = sumarKgVendidos(
                        con, venta.getProduccionId(), venta.getId());
                comprobarStock(pesoProduccion, vendidos, venta.getCantidadKg());

                String sql = """
                        UPDATE ventas
                        SET fecha = ?, cliente = ?, cantidad_kg = ?,
                            precio_unitario_kg = ?, total = ?, observaciones = ?
                        WHERE id = ? AND estado = 'REGISTRADA'
                        """;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setDate(1, Date.valueOf(venta.getFecha()));
                    ps.setString(2, venta.getCliente());
                    ps.setBigDecimal(3, venta.getCantidadKg());
                    ps.setBigDecimal(4, venta.getPrecioUnitarioKg());
                    ps.setBigDecimal(5, venta.getTotal());
                    ps.setString(6, venta.getObservaciones());
                    ps.setLong(7, venta.getId());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("No se pudo actualizar la venta.");
                    }
                }
                venta.setEstado("REGISTRADA");
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ANULAR SIN BORRAR EL HISTORIAL.
    public void anular(Long id) throws SQLException {
        validarId(id, "Seleccione una venta válida.");

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                Long produccionId = buscarProduccionDeVenta(con, id);
                if (produccionId == null) {
                    throw new IllegalArgumentException("La venta no existe.");
                }
                // BLOQUEAR LA PRODUCCION SERIALIZA SUS MOVIMIENTOS DE STOCK.
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT id FROM produccion WHERE id = ? FOR UPDATE")) {
                    ps.setLong(1, produccionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("La producción ya no existe.");
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("""
                        UPDATE ventas SET estado = 'ANULADA'
                        WHERE id = ? AND estado = 'REGISTRADA'
                        """)) {
                    ps.setLong(1, id);
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "La venta ya está anulada o no existe.");
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // TOTAL DE KG VENDIDOS; EXCLUYE LAS VENTAS ANULADAS.
    public BigDecimal obtenerCantidadVendidaKg(Long produccionId)
            throws SQLException {
        validarId(produccionId, "Seleccione una producción válida.");
        try (Connection con = DatabaseConfig.getConnection()) {
            return sumarKgVendidos(con, produccionId, null);
        }
    }

    // STOCK DISPONIBLE PARA MOSTRAR EN EL FORMULARIO.
    public BigDecimal obtenerKgDisponibles(Long produccionId)
            throws SQLException {
        validarId(produccionId, "Seleccione una producción válida.");
        try (Connection con = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("""
                    SELECT peso_total_kg, estado
                    FROM produccion WHERE id = ?
                    """)) {
                ps.setLong(1, produccionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"REGISTRADA".equals(rs.getString("estado"))) {
                        throw new IllegalArgumentException(
                                "Seleccione una producción registrada.");
                    }
                    BigDecimal total = rs.getBigDecimal("peso_total_kg");
                    return total.subtract(
                            sumarKgVendidos(con, produccionId, null));
                }
            }
        }
    }

    // BLOQUEAR LA PRODUCCION Y COMPROBAR SU ESTADO Y SU FECHA.
    private BigDecimal bloquearProduccion(Connection con, Venta venta)
            throws SQLException {
        String sql = """
                SELECT fecha, peso_total_kg, estado
                FROM produccion
                WHERE id = ?
                FOR UPDATE
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, venta.getProduccionId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("La producción no existe.");
                }
                if (!"REGISTRADA".equals(rs.getString("estado"))) {
                    throw new IllegalArgumentException(
                            "No se permiten ventas de producciones anuladas.");
                }
                LocalDate fechaProduccion = rs.getDate("fecha").toLocalDate();
                if (venta.getFecha().isBefore(fechaProduccion)) {
                    throw new IllegalArgumentException(
                            "La venta no puede ser anterior a la cosecha.");
                }
                return rs.getBigDecimal("peso_total_kg");
            }
        }
    }

    private BigDecimal sumarKgVendidos(
            Connection con, Long produccionId, Long excluirId)
            throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(cantidad_kg), 0) AS total
                FROM ventas
                WHERE produccion_id = ? AND estado = 'REGISTRADA'
                """;
        if (excluirId != null) {
            sql += " AND id <> ?";
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, produccionId);
            if (excluirId != null) {
                ps.setLong(2, excluirId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        }
    }

    private Long buscarProduccionDeVenta(Connection con, Long id)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT produccion_id FROM ventas WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private void verificarVentaRegistrada(
            Connection con, Long id, Long produccionId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT produccion_id, estado FROM ventas
                WHERE id = ? FOR UPDATE
                """)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()
                        || !"REGISTRADA".equals(rs.getString("estado"))
                        || !Objects.equals(produccionId, rs.getLong("produccion_id"))) {
                    throw new IllegalArgumentException(
                            "La venta no existe, cambió de producción o fue anulada.");
                }
            }
        }
    }

    private void comprobarStock(BigDecimal total,
                               BigDecimal vendidos,
                               BigDecimal cantidadNueva) {
        BigDecimal disponibles = total.subtract(vendidos);
        if (cantidadNueva.compareTo(disponibles) > 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Disponible: "
                    + disponibles.max(BigDecimal.ZERO).toPlainString() + " kg.");
        }
    }

    // VALIDACIONES BASICAS Y CALCULO DEL TOTAL EN SOLES.
    private void validarDatos(Venta venta) {
        if (venta == null) {
            throw new IllegalArgumentException("Los datos de la venta son obligatorios.");
        }
        validarId(venta.getProduccionId(), "Seleccione una producción válida.");
        if (venta.getFecha() == null || venta.getFecha().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ingrese una fecha válida, no futura.");
        }
        if (venta.getCliente() == null || venta.getCliente().isBlank()
                || venta.getCliente().trim().length() > 120) {
            throw new IllegalArgumentException("Ingrese un cliente de máximo 120 caracteres.");
        }
        venta.setCliente(venta.getCliente().trim());

        BigDecimal kg = venta.getCantidadKg();
        if (kg == null || kg.compareTo(BigDecimal.ZERO) <= 0
                || kg.stripTrailingZeros().scale() > 3
                || kg.compareTo(new BigDecimal("999999999.999")) > 0) {
            throw new IllegalArgumentException("Ingrese kilogramos positivos, con máximo tres decimales.");
        }
        BigDecimal precio = venta.getPrecioUnitarioKg();
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0
                || precio.stripTrailingZeros().scale() > 2
                || precio.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Ingrese un precio positivo, con máximo dos decimales.");
        }
        String observaciones = venta.getObservaciones();
        if (observaciones != null && observaciones.trim().length() > 255) {
            throw new IllegalArgumentException("Las observaciones no deben superar 255 caracteres.");
        }
        venta.setObservaciones(observaciones == null ? "" : observaciones.trim());

        BigDecimal total = kg.multiply(precio).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0
                || total.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException("El total debe ser mayor a S/ 0.00 y estar dentro del límite permitido.");
        }
        venta.setTotal(total);
    }

    private void validarId(Long id, String mensaje) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private List<Venta> leerLista(ResultSet rs) throws SQLException {
        List<Venta> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        return new Venta(
                rs.getLong("id"),
                rs.getLong("produccion_id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getString("cliente"),
                rs.getBigDecimal("cantidad_kg"),
                rs.getBigDecimal("precio_unitario_kg"),
                rs.getBigDecimal("total"),
                rs.getString("estado"),
                rs.getString("observaciones")
        );
    }
}

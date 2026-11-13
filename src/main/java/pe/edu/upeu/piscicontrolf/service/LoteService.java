
package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.repository.LoteRepository;
import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class LoteService {

    private final LoteRepository repository =
            new LoteRepository();


    private void verificarPermisoAdministrador() {

        if (!SesionUsuario.esAdministrador()) {
            throw new SecurityException(
                    "Solo el administrador puede registrar "
                            + "o modificar lotes.");
        }
    }

    // REGISTRAR LOTE
    public void registrar(Lote lote) throws SQLException {

        // VERIFICAR PERMISO
        verificarPermisoAdministrador();

        // VALIDAR INFORMACIÓN
        validarLote(lote);
        validarEstanqueActivo(lote.getEstanqueId());
        validarCapacidad(lote, null);

        // GUARDAR EN H2
        repository.registrar(lote);
    }


    // LISTAR TODOS LOS LOTES
    public List<Lote> listar() throws SQLException {

        return repository.listar();
    }

    // LISTAR LOTES POR ESTANQUE
    public List<Lote> listarPorEstanque(Long estanqueId)
            throws SQLException {

        if (estanqueId == null || estanqueId <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un estanque válido.");
        }

        return repository.listarPorEstanque(estanqueId);
    }

    // MODIFICAR LOTE
    public void actualizar(Lote lote) throws SQLException {
        verificarPermisoAdministrador();
        validarLote(lote);

        if (lote.getId() == null || lote.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un lote válido.");
        }

        validarEstanqueActivo(lote.getEstanqueId());

        validarCapacidad(lote, lote.getId());

        repository.actualizar(lote);
    }

    // VALIDACIONES GENERALES
    private void validarLote(Lote lote) {

        if (lote == null) {
            throw new IllegalArgumentException(
                    "Los datos del lote son obligatorios.");
        }

        if (lote.getEstanqueId() == null
                || lote.getEstanqueId() <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un estanque.");
        }

        if (lote.getFechaIngreso() == null) {
            throw new IllegalArgumentException(
                    "Ingrese la fecha de ingreso.");
        }

        if (lote.getFechaIngreso().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de ingreso no puede ser futura.");
        }

        if (lote.getCantidadTruchas() <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de truchas debe ser mayor que cero.");
        }

        String observaciones = lote.getObservaciones();

        if (observaciones != null) {

            observaciones = observaciones.trim();

            if (observaciones.length() > 255) {
                throw new IllegalArgumentException(
                        "Las observaciones no deben superar 255 caracteres.");
            }

            lote.setObservaciones(observaciones);
        }
    }

    // COMPROBAR QUE EL ESTANQUE EXISTA Y ESTE ACTIVO
    private void validarEstanqueActivo(Long estanqueId)
            throws SQLException {

        String sql = """
                SELECT estado
                FROM estanques
                WHERE id = ?
                """;

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, estanqueId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "El estanque seleccionado no existe.");
                }

                if (!"ACTIVO".equalsIgnoreCase(
                        rs.getString("estado"))) {

                    throw new IllegalArgumentException(
                            "No se pueden registrar lotes en un estanque inactivo.");
                }
            }
        }
    }

    // VALIDAR CAPACIDAD DEL ESTANQUE
    private void validarCapacidad(
            Lote lote, Long loteExcluidoId)
            throws SQLException {

        int disponible =
                repository.obtenerCapacidadDisponible(
                        lote.getEstanqueId(),
                        loteExcluidoId);

        if (lote.getCantidadTruchas() > disponible) {

            throw new IllegalArgumentException(
                    "Capacidad insuficiente. "
                            + "Espacios disponibles: "
                            + Math.max(0, disponible)
                            + " truchas.");
        }
    }

    public int obtenerCapacidadDisponible(Long estanqueId)
            throws SQLException {

        if (estanqueId == null || estanqueId <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un estanque válido.");
        }

        return repository.obtenerCapacidadDisponible(
                estanqueId, null);
    }


}

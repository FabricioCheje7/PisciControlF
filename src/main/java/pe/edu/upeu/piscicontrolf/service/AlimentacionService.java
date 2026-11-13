

package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Alimentacion;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.repository.AlimentacionRepository;
import pe.edu.upeu.piscicontrolf.repository.LoteRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AlimentacionService {

    private final AlimentacionRepository repository =
            new AlimentacionRepository();

    private final LoteRepository loteRepository =
            new LoteRepository();

    // PERMISO PARA REGISTRAR ALIMENTACION
    private void verificarPermisoRegistro() {

        boolean administrador =
                SesionUsuario.esAdministrador();

        boolean operador =
                SesionUsuario.esOperador();

        if (!administrador && !operador) {

            throw new SecurityException(
                    "No tiene permiso para registrar alimentación.");
        }
    }

    // PERMISO PARA MODIFICAR ALIMENTACION
    private void verificarPermisoModificacion() {

        if (!SesionUsuario.esAdministrador()) {

            throw new SecurityException(
                    "Solo el administrador puede modificar "
                            + "registros de alimentación.");
        }
    }

    // REGISTRAR ALIMENTACION
    public void registrar(Alimentacion alimentacion)
            throws SQLException {

        verificarPermisoRegistro();

        validarAlimentacion(alimentacion);

        repository.registrar(alimentacion);
    }

    // LISTAR ALIMENTACIONES
    public List<Alimentacion> listar()
            throws SQLException {

        return repository.listar();
    }

    // LISTAR ALIMENTACIONES POR LOTE
    public List<Alimentacion> listarPorLote(Long loteId)
            throws SQLException {

        if (loteId == null || loteId <= 0) {

            throw new IllegalArgumentException(
                    "Seleccione un lote válido.");
        }

        return repository.listarPorLote(loteId);
    }

    // MODIFICAR ALIMENTACION
    public void actualizar(Alimentacion alimentacion)
            throws SQLException {

        verificarPermisoModificacion();

        if (alimentacion == null
                || alimentacion.getId() == null
                || alimentacion.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Seleccione un registro válido.");
        }

        validarAlimentacion(alimentacion);

        repository.actualizar(alimentacion);
    }

    // VALIDAR LOS DATOS DE ALIMENTACION
    private void validarAlimentacion(
            Alimentacion alimentacion)
            throws SQLException {

        if (alimentacion == null) {

            throw new IllegalArgumentException(
                    "Los datos son obligatorios.");
        }

        // VALIDAR LOTE
        if (alimentacion.getLoteId() == null
                || alimentacion.getLoteId() <= 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un lote.");
        }

        Lote lote = loteRepository.buscarPorId(
                alimentacion.getLoteId());

        if (lote == null) {

            throw new IllegalArgumentException(
                    "El lote seleccionado no existe.");
        }

        // VALIDAR FECHA
        LocalDate fecha = alimentacion.getFecha();

        if (fecha == null) {

            throw new IllegalArgumentException(
                    "Ingrese la fecha de alimentación.");
        }

        if (fecha.isAfter(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "La fecha no puede ser futura.");
        }

        if (fecha.isBefore(lote.getFechaIngreso())) {

            throw new IllegalArgumentException(
                    "La alimentación no puede ser anterior "
                            + "al ingreso del lote.");
        }

        // VALIDAR TIPO DE ALIMENTO
        String tipo = alimentacion.getTipoAlimento();

        if (tipo == null || tipo.isBlank()) {

            throw new IllegalArgumentException(
                    "Ingrese el tipo de alimento.");
        }

        tipo = tipo.trim();

        if (tipo.length() > 100) {

            throw new IllegalArgumentException(
                    "El tipo de alimento supera 100 caracteres.");
        }

        alimentacion.setTipoAlimento(tipo);

        // VALIDAR CANTIDAD EN KILOGRAMOS
        BigDecimal cantidad =
                alimentacion.getCantidadKg();

        if (cantidad == null
                || cantidad.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero.");
        }

        if (cantidad.scale() > 3
                || cantidad.compareTo(
                new BigDecimal("9999999.999")) > 0) {

            throw new IllegalArgumentException(
                    "Ingrese una cantidad válida "
                            + "con máximo tres decimales.");
        }

        // VALIDAR OBSERVACIONES
        String observaciones =
                alimentacion.getObservaciones();

        if (observaciones != null) {

            observaciones = observaciones.trim();

            if (observaciones.length() > 255) {

                throw new IllegalArgumentException(
                        "Las observaciones son demasiado largas.");
            }

            alimentacion.setObservaciones(observaciones);
        }
    }
}

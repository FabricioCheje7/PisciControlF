
package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.model.Crecimiento;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.repository.CrecimientoRepository;
import pe.edu.upeu.piscicontrolf.repository.LoteRepository;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CrecimientoService {

    private final CrecimientoRepository repository =
            new CrecimientoRepository();

    private final LoteRepository loteRepository =
            new LoteRepository();

    // ADMINISTRADOR Y OPERADOR PUEDEN REGISTRAR
    private void verificarPermisoRegistro() {

        if (!SesionUsuario.esAdministrador()
                && !SesionUsuario.esOperador()) {

            throw new SecurityException(
                    "No tiene permiso para registrar "
                            + "mediciones de crecimiento.");
        }
    }

    // SOLO ADMINISTRADOR PUEDE MODIFICAR
    private void verificarPermisoModificacion() {

        if (!SesionUsuario.esAdministrador()) {

            throw new SecurityException(
                    "Solo el administrador puede modificar "
                            + "mediciones de crecimiento.");
        }
    }


    // REGISTRAR CRECIMIENTO
    public void registrar(Crecimiento crecimiento)
            throws SQLException {
        verificarPermisoRegistro();
        validarCrecimiento(crecimiento);

        repository.registrar(crecimiento);
    }

    // LISTAR TODOS LOS REGISTROS
    public List<Crecimiento> listar()
            throws SQLException {

        return repository.listar();
    }

    // LISTAR CRECIMIENTO POR LOTE
    public List<Crecimiento> listarPorLote(Long loteId)
            throws SQLException {

        if (loteId == null || loteId <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un lote válido.");
        }

        return repository.listarPorLote(loteId);
    }

    // MODIFICAR CRECIMIENTO
    public void actualizar(Crecimiento crecimiento)
            throws SQLException {
        verificarPermisoModificacion();
        if (crecimiento == null ||
                crecimiento.getId() == null ||
                crecimiento.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Seleccione un registro válido.");
        }

        validarCrecimiento(crecimiento);

        repository.actualizar(crecimiento);
    }

    // VALIDACIONES DEL SISTEMA
    private void validarCrecimiento(
            Crecimiento crecimiento) throws SQLException {

        if (crecimiento == null) {
            throw new IllegalArgumentException(
                    "Los datos son obligatorios.");
        }

        // VALIDAR LOTE
        Long loteId = crecimiento.getLoteId();

        if (loteId == null || loteId <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un lote.");
        }

        Lote lote = loteRepository.buscarPorId(loteId);

        if (lote == null) {
            throw new IllegalArgumentException(
                    "El lote seleccionado no existe.");
        }

        // VALIDAR FECHA
        LocalDate fecha = crecimiento.getFecha();

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Ingrese la fecha de medición.");
        }

        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha no puede ser futura.");
        }

        if (fecha.isBefore(lote.getFechaIngreso())) {
            throw new IllegalArgumentException(
                    "La fecha no puede ser anterior "
                            + "al ingreso del lote.");
        }

        // VALIDAR PESO
        BigDecimal peso = crecimiento.getPesoPromedioG();

        if (peso == null ||
                peso.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El peso promedio debe ser mayor que cero.");
        }

        if (peso.stripTrailingZeros().scale() > 2 ||
                peso.compareTo(
                        new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El peso debe tener máximo dos decimales "
                            + "y estar dentro del rango permitido.");
        }

        // VALIDAR TALLA
        BigDecimal talla = crecimiento.getTallaPromedioCm();

        if (talla == null ||
                talla.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La talla promedio debe ser mayor que cero.");
        }

        if (talla.stripTrailingZeros().scale() > 2 ||
                talla.compareTo(
                        new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "La talla debe tener máximo dos decimales "
                            + "y estar dentro del rango permitido.");
        }

        // VALIDAR CANTIDAD DE MUESTRA
        int muestra = crecimiento.getCantidadMuestra();

        if (muestra <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de muestra debe ser mayor que cero.");
        }

        if (muestra > lote.getCantidadTruchas()) {
            throw new IllegalArgumentException(
                    "La muestra no puede superar "
                            + "la cantidad de truchas del lote.");
        }

        // VALIDAR OBSERVACIONES
        String observaciones =
                crecimiento.getObservaciones();

        if (observaciones != null) {

            observaciones = observaciones.trim();

            if (observaciones.length() > 255) {
                throw new IllegalArgumentException(
                        "Las observaciones no deben superar "
                                + "255 caracteres.");
            }

            crecimiento.setObservaciones(observaciones);
        }
    }
}

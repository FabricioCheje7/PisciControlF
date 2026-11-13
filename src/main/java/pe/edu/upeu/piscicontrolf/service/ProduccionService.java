package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.model.Produccion;
import pe.edu.upeu.piscicontrolf.repository.LoteRepository;
import pe.edu.upeu.piscicontrolf.repository.ProduccionRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ProduccionService {

    private final ProduccionRepository repository =
            new ProduccionRepository();

    private final LoteRepository loteRepository =
            new LoteRepository();

    // ADMINISTRADOR Y OPERADOR PUEDEN CONSULTAR Y REGISTRAR
    private void verificarPermisoConsulta() {
        if (!SesionUsuario.esAdministrador()
                && !SesionUsuario.esOperador()) {
            throw new SecurityException(
                    "Debe iniciar sesión con un usuario autorizado.");
        }
    }

    private void verificarPermisoRegistro() {
        if (!SesionUsuario.esAdministrador()
                && !SesionUsuario.esOperador()) {
            throw new SecurityException(
                    "No tiene permiso para registrar producciones.");
        }
    }

    // SOLO EL ADMINISTRADOR PUEDE MODIFICAR O ANULAR
    private void verificarPermisoAdministrador() {
        if (!SesionUsuario.esAdministrador()) {
            throw new SecurityException(
                    "Solo el administrador puede modificar o anular producciones.");
        }
    }

    // BUSCAR UN REGISTRO EXISTENTE SIN CAMBIAR LAS API DEL REPOSITORIO
    private Produccion buscarProduccionExistente(Long id)
            throws SQLException {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una producción válida.");
        }

        return repository.listar().stream()
                .filter(p -> Objects.equals(p.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La producción seleccionada no existe."));
    }

    // REGISTRAR PRODUCCIÓN
    public void registrar(Produccion produccion)
            throws SQLException {

        verificarPermisoRegistro();
        validarProduccion(produccion, null);

        produccion.setEstado("REGISTRADA");
        repository.registrar(produccion);
    }

    // LISTAR PRODUCCIONES
    public List<Produccion> listar()
            throws SQLException {

        verificarPermisoConsulta();
        return repository.listar();
    }

    // LISTAR PRODUCCIONES POR LOTE
    public List<Produccion> listarPorLote(Long loteId)
            throws SQLException {

        verificarPermisoConsulta();

        if (loteId == null || loteId <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un lote válido.");
        }

        return repository.listarPorLote(loteId);
    }

    // MODIFICAR PRODUCCIÓN
    public void actualizar(Produccion produccion)
            throws SQLException {

        verificarPermisoAdministrador();

        if (produccion == null || produccion.getId() == null
                || produccion.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione una producción válida.");
        }

        Produccion existente =
                buscarProduccionExistente(produccion.getId());

        if (!"REGISTRADA".equals(existente.getEstado())) {
            throw new IllegalArgumentException(
                    "No se puede modificar una producción anulada "
                    + "o con un estado no permitido.");
        }

        // Excluir el ID editado evita contar dos veces esta cosecha.
        validarProduccion(produccion, produccion.getId());

        produccion.setEstado("REGISTRADA");
        repository.actualizar(produccion);
    }

    // ANULAR PRODUCCIÓN
    public void anular(Long id)
            throws SQLException {

        verificarPermisoAdministrador();
        Produccion existente = buscarProduccionExistente(id);

        if (!"REGISTRADA".equals(existente.getEstado())) {
            throw new IllegalArgumentException(
                    "La producción ya fue anulada "
                    + "o no se puede anular en su estado actual.");
        }

        // Antes de permitir anulación de producciones vendidas,
        // se debe verificar su relación con la tabla de ventas.
        // Esta comprobación requiere revisar el repositorio de ventas.
        repository.anular(id);
    }

    // CONSULTAR TRUCHAS DISPONIBLES DEL LOTE
    public long obtenerTruchasDisponibles(Long loteId)
            throws SQLException {

        verificarPermisoConsulta();

        if (loteId == null || loteId <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un lote válido.");
        }

        Lote lote = loteRepository.buscarPorId(loteId);

        if (lote == null) {
            throw new IllegalArgumentException(
                    "El lote seleccionado no existe.");
        }

        long cosechadas = repository.obtenerCantidadCosechada(
                loteId, null);

        return Math.max(
                0L, (long) lote.getCantidadTruchas() - cosechadas);
    }

    // VALIDAR DATOS DE PRODUCCIÓN
    private void validarProduccion(Produccion produccion,
                                   Long excluirId)
            throws SQLException {

        if (produccion == null) {
            throw new IllegalArgumentException(
                    "Los datos de producción son obligatorios.");
        }

        // LOTE
        Long loteId = produccion.getLoteId();

        if (loteId == null || loteId <= 0) {
            throw new IllegalArgumentException(
                    "Seleccione un lote.");
        }

        Lote lote = loteRepository.buscarPorId(loteId);

        if (lote == null) {
            throw new IllegalArgumentException(
                    "El lote seleccionado no existe.");
        }

        // FECHA
        LocalDate fecha = produccion.getFecha();

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Ingrese la fecha de cosecha.");
        }

        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha no puede ser futura.");
        }

        if (fecha.isBefore(lote.getFechaIngreso())) {
            throw new IllegalArgumentException(
                    "La cosecha no puede ser anterior "
                    + "a la fecha de ingreso del lote.");
        }

        // CANTIDAD DE TRUCHAS
        int cantidad = produccion.getCantidadTruchas();

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de truchas debe ser mayor que cero.");
        }

        long cosechadas = repository.obtenerCantidadCosechada(
                loteId, excluirId);

        long disponibles =
                (long) lote.getCantidadTruchas() - cosechadas;

        if (cantidad > disponibles) {
            throw new IllegalArgumentException(
                    "Cantidad insuficiente. Truchas disponibles: "
                    + Math.max(0L, disponibles));
        }

        // PESO
        BigDecimal peso = produccion.getPesoTotalKg();

        if (peso == null
                || peso.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El peso total debe ser mayor que cero.");
        }

        if (peso.stripTrailingZeros().scale() > 3
                || peso.compareTo(
                        new BigDecimal("999999999.999")) > 0) {
            throw new IllegalArgumentException(
                    "El peso debe tener máximo tres decimales "
                    + "y estar dentro del rango permitido.");
        }

        // OBSERVACIONES
        String observaciones = produccion.getObservaciones();

        if (observaciones != null) {
            observaciones = observaciones.trim();

            if (observaciones.length() > 255) {
                throw new IllegalArgumentException(
                        "Las observaciones no deben superar "
                        + "255 caracteres.");
            }

            produccion.setObservaciones(observaciones);
        }
    }
}


package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.CalidadAgua;
import pe.edu.upeu.piscicontrolf.repository.CalidadAguaRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CalidadAguaService {

    private final CalidadAguaRepository repository =
            new CalidadAguaRepository();

    // REGISTRAR MEDICION
    public void registrar(CalidadAgua calidad)
            throws SQLException {

        verificarPermisoRegistro();
        validarMedicion(calidad);

        calidad.setEstado(evaluarEstado(calidad));

        repository.registrar(calidad);
    }

    // MODIFICAR MEDICION
    public boolean actualizar(CalidadAgua calidad)
            throws SQLException {

        verificarPermisoModificacion();

        if (calidad == null ||
                calidad.getId() == null ||
                calidad.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Selecciona una medición válida.");
        }

        validarMedicion(calidad);

        calidad.setEstado(evaluarEstado(calidad));

        return repository.actualizar(calidad);
    }

    // LISTAR TODAS LAS MEDICIONES
    public List<CalidadAgua> listar()
            throws SQLException {

        verificarSesion();
        return repository.listar();
    }

    // BUSCAR POR ID
    public CalidadAgua buscarPorId(Long id)
            throws SQLException {

        verificarSesion();

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la medición no es válido.");
        }

        return repository.buscarPorId(id);
    }

    // CONSULTAR POR ESTANQUE
    public List<CalidadAgua> listarPorEstanque(
            Long estanqueId) throws SQLException {

        verificarSesion();

        if (estanqueId == null || estanqueId <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un estanque válido.");
        }

        return repository.listarPorEstanque(estanqueId);
    }

    // CONTAR MEDICIONES
    public long contar() throws SQLException {

        verificarSesion();
        return repository.contar();
    }

    // VALIDAR DATOS DEL FORMULARIO
    private void validarMedicion(CalidadAgua calidad) {

        if (calidad == null) {
            throw new IllegalArgumentException(
                    "No se recibieron datos de la medición.");
        }

        if (calidad.getEstanqueId() == null ||
                calidad.getEstanqueId() <= 0) {

            throw new IllegalArgumentException(
                    "Debes seleccionar un estanque.");
        }

        if (calidad.getFecha() == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar una fecha.");
        }

        if (calidad.getFecha().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha no puede ser futura.");
        }

        validarNumero(
                calidad.getTemperaturaC(),
                "Temperatura",
                new BigDecimal("0"),
                new BigDecimal("35"));

        validarNumero(
                calidad.getPh(),
                "pH",
                BigDecimal.ZERO,
                new BigDecimal("14"));

        validarNumero(
                calidad.getOxigenoMgL(),
                "Oxígeno disuelto",
                BigDecimal.ZERO,
                new BigDecimal("25"));

        if (calidad.getObservaciones() != null &&
                calidad.getObservaciones().length() > 255) {

            throw new IllegalArgumentException(
                    "Las observaciones no pueden superar " +
                            "255 caracteres.");
        }
    }

    // VALIDAR RANGO Y PRECISION NUMERICA
    private void validarNumero(
            BigDecimal valor,
            String nombre,
            BigDecimal minimo,
            BigDecimal maximo) {

        if (valor == null) {
            throw new IllegalArgumentException(
                    "Debes ingresar " + nombre + ".");
        }

        if (valor.compareTo(minimo) < 0 ||
                valor.compareTo(maximo) > 0) {

            throw new IllegalArgumentException(
                    nombre + " debe estar entre " +
                            minimo.toPlainString() + " y " +
                            maximo.toPlainString() + ".");
        }

        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    nombre + " admite como máximo " +
                            "2 decimales.");
        }
    }

    // EVALUACION ORIENTATIVA DEL AGUA
    public String evaluarEstado(CalidadAgua calidad) {

        if (calidad == null ||
                calidad.getTemperaturaC() == null ||
                calidad.getPh() == null ||
                calidad.getOxigenoMgL() == null) {

            return "SIN_EVALUAR";
        }

        BigDecimal temperatura = calidad.getTemperaturaC();
        BigDecimal ph = calidad.getPh();
        BigDecimal oxigeno = calidad.getOxigenoMgL();

        // CONDICIONES QUE REQUIEREN ATENCION URGENTE
        if (temperatura.compareTo(new BigDecimal("5")) < 0 ||
                temperatura.compareTo(new BigDecimal("23")) > 0 ||
                ph.compareTo(new BigDecimal("5.5")) < 0 ||
                ph.compareTo(new BigDecimal("9.5")) > 0 ||
                oxigeno.compareTo(new BigDecimal("5")) < 0) {

            return "CRITICO";
        }

        // RANGO OPERATIVO ORIENTATIVO
        boolean temperaturaAdecuada =
                temperatura.compareTo(new BigDecimal("10")) >= 0 &&
                        temperatura.compareTo(new BigDecimal("18")) <= 0;

        boolean phAdecuado =
                ph.compareTo(new BigDecimal("6.5")) >= 0 &&
                        ph.compareTo(new BigDecimal("8.5")) <= 0;

        boolean oxigenoAdecuado =
                oxigeno.compareTo(new BigDecimal("7")) >= 0 &&
                        oxigeno.compareTo(new BigDecimal("12")) <= 0;

        if (temperaturaAdecuada &&
                phAdecuado &&
                oxigenoAdecuado) {

            return "NORMAL";
        }

        return "ALERTA";
    }

    // CONTROL DE SESION
    private void verificarSesion() {

        if (!SesionUsuario.haySesion()) {
            throw new SecurityException(
                    "Debes iniciar sesión.");
        }
    }

    // ADMINISTRADOR Y OPERADOR
    private void verificarPermisoRegistro() {

        verificarSesion();

        if (!SesionUsuario.esAdministrador() &&
                !SesionUsuario.esOperador()) {

            throw new SecurityException(
                    "No tienes permiso para registrar mediciones.");
        }
    }

    // SOLO ADMINISTRADOR
    private void verificarPermisoModificacion() {

        verificarSesion();

        if (!SesionUsuario.esAdministrador()) {
            throw new SecurityException(
                    "Solo el administrador puede modificar mediciones.");
        }
    }
}



package pe.edu.upeu.piscicontrolf.service;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Mortalidad;
import pe.edu.upeu.piscicontrolf.repository.MortalidadRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class MortalidadService {

    private final MortalidadRepository repository =
            new MortalidadRepository();

    // REGISTRAR INCIDENCIA
    public void registrar(Mortalidad mortalidad)
            throws SQLException {

        verificarPermisoRegistro();

        validarIncidencia(mortalidad);

        verificarCantidadDisponible(mortalidad, null);

        repository.registrar(mortalidad);
    }

    // MODIFICAR INCIDENCIA
    public boolean actualizar(Mortalidad mortalidad)
            throws SQLException {

        verificarPermisoModificacion();

        if (mortalidad == null ||
                mortalidad.getId() == null ||
                mortalidad.getId() <= 0) {

            throw new IllegalArgumentException(
                    "Selecciona una incidencia válida.");
        }

        Mortalidad existente =
                repository.buscarPorId(mortalidad.getId());

        if (existente == null) {
            throw new IllegalArgumentException(
                    "La incidencia ya no existe.");
        }

        validarIncidencia(mortalidad);

        // Excluye el registro que se está modificando.
        verificarCantidadDisponible(
                mortalidad,
                mortalidad.getId());

        return repository.actualizar(mortalidad);
    }

    // LISTAR INCIDENCIAS
    public List<Mortalidad> listar()
            throws SQLException {

        verificarSesion();
        return repository.listar();
    }

    // BUSCAR INCIDENCIA POR ID
    public Mortalidad buscarPorId(Long id)
            throws SQLException {

        verificarSesion();

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la incidencia no es válido.");
        }

        return repository.buscarPorId(id);
    }

    // LISTAR INCIDENCIAS POR LOTE
    public List<Mortalidad> listarPorLote(Long loteId)
            throws SQLException {

        verificarSesion();

        if (loteId == null || loteId <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un lote válido.");
        }

        return repository.listarPorLote(loteId);
    }

    // CONTAR INCIDENCIAS
    public long contar() throws SQLException {

        verificarSesion();
        return repository.contar();
    }

    // TOTAL DE TRUCHAS MUERTAS
    public long totalMuertas() throws SQLException {

        verificarSesion();
        return repository.totalMuertas();
    }

    // CONTAR CASOS ABIERTOS
    public long contarCasosAbiertos()
            throws SQLException {

        verificarSesion();
        return repository.contarCasosAbiertos();
    }

    // TOTAL DE MORTALIDAD POR LOTE
    public long totalMuertasPorLote(Long loteId)
            throws SQLException {

        verificarSesion();

        if (loteId == null || loteId <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un lote válido.");
        }

        return repository.totalMuertasPorLote(loteId);
    }

    // VALIDAR INFORMACIÓN
    private void validarIncidencia(Mortalidad m) {

        if (m == null) {
            throw new IllegalArgumentException(
                    "No se recibieron datos de la incidencia.");
        }

        if (m.getLoteId() == null ||
                m.getLoteId() <= 0) {

            throw new IllegalArgumentException(
                    "Debes seleccionar un lote.");
        }

        if (m.getFecha() == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar la fecha.");
        }

        if (m.getFecha().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha no puede ser futura.");
        }

        String tipo = normalizar(m.getTipoIncidencia());

        if (!tipo.equals("MORTALIDAD") &&
                !tipo.equals("SANIDAD")) {

            throw new IllegalArgumentException(
                    "Selecciona Mortalidad o Sanidad.");
        }

        m.setTipoIncidencia(tipo);

        if (m.getCantidadMuertas() == null ||
                m.getCantidadMuertas() < 0) {

            throw new IllegalArgumentException(
                    "Ingresa una cantidad válida.");
        }

        if (tipo.equals("MORTALIDAD") &&
                m.getCantidadMuertas() == 0) {

            throw new IllegalArgumentException(
                    "En Mortalidad debes registrar " +
                            "al menos una trucha muerta.");
        }

        if (tipo.equals("SANIDAD") &&
                m.getCantidadMuertas() != 0) {

            throw new IllegalArgumentException(
                    "Para registrar truchas muertas, " +
                            "utiliza el tipo Mortalidad.");
        }

        String estado = normalizar(m.getEstado());

        if (!estado.equals("ABIERTA") &&
                !estado.equals("CERRADA")) {

            throw new IllegalArgumentException(
                    "Selecciona un estado válido.");
        }

        m.setEstado(estado);

        validarLongitud(m.getCausa(),
                "Causa", 120);

        validarLongitud(m.getAcciones(),
                "Acciones", 500);

        validarLongitud(m.getObservaciones(),
                "Observaciones", 500);

        m.setCausa(limpiar(m.getCausa()));
        m.setAcciones(limpiar(m.getAcciones()));
        m.setObservaciones(
                limpiar(m.getObservaciones()));
    }

    // COMPROBAR QUE LA MORTALIDAD NO SUPERE EL LOTE
    private void verificarCantidadDisponible(
            Mortalidad mortalidad,
            Long idExcluir) throws SQLException {

        long cantidadInicial =
                consultarCantidadInicial(
                        mortalidad.getLoteId());

        List<Mortalidad> registros =
                repository.listarPorLote(
                        mortalidad.getLoteId());

        long muertasAnteriores = 0;

        for (Mortalidad registro : registros) {

            if (idExcluir != null &&
                    idExcluir.equals(registro.getId())) {
                continue;
            }

            if ("MORTALIDAD".equals(
                    registro.getTipoIncidencia())) {

                muertasAnteriores +=
                        registro.getCantidadMuertas();
            }
        }

        long nuevasMuertas =
                mortalidad.getCantidadMuertas();

        if (muertasAnteriores + nuevasMuertas >
                cantidadInicial) {

            long disponibles = Math.max(
                    0,
                    cantidadInicial - muertasAnteriores);

            throw new IllegalArgumentException(
                    "La mortalidad supera la cantidad " +
                            "inicial de truchas del lote. " +
                            "Máximo registrable según este " +
                            "control: " + disponibles);
        }
    }

    // CONSULTAR CANTIDAD INICIAL DEL LOTE
    private long consultarCantidadInicial(Long loteId)
            throws SQLException {

        String sql = """
                SELECT cantidad_truchas
                FROM lotes
                WHERE id = ?
                """;

        try (Connection con =
                     DatabaseConfig.getConnection();

             PreparedStatement ps =
                     con.prepareStatement(sql)) {

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

    // VALIDAR LONGITUD DE TEXTOS
    private void validarLongitud(
            String valor,
            String campo,
            int maximo) {

        if (valor != null &&
                valor.strip().length() > maximo) {

            throw new IllegalArgumentException(
                    campo + " no puede superar " +
                            maximo + " caracteres.");
        }
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.strip();
    }

    private String normalizar(String texto) {
        return limpiar(texto)
                .toUpperCase(Locale.ROOT);
    }

    // VERIFICAR SESIÓN
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
                    "No tienes permiso para registrar " +
                            "incidencias.");
        }
    }

    // SOLO ADMINISTRADOR
    private void verificarPermisoModificacion() {

        verificarSesion();

        if (!SesionUsuario.esAdministrador()) {

            throw new SecurityException(
                    "Solo el Administrador puede " +
                            "modificar incidencias.");
        }
    }
}


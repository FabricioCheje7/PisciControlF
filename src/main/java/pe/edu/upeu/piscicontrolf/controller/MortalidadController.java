package pe.edu.upeu.piscicontrolf.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Mortalidad;
import pe.edu.upeu.piscicontrolf.service.MortalidadService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class MortalidadController {

    @FXML private Label lblTotalIncidencias;
    @FXML private Label lblTotalMuertas;
    @FXML private Label lblCasosAbiertos;

    @FXML private ComboBox<OpcionLote> cmbLote;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cmbTipoIncidencia;
    @FXML private TextField txtCantidadMuertas;
    @FXML private ComboBox<String> cmbCausa;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextArea txtAcciones;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtBuscar;
    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;

    @FXML private TableView<Mortalidad> tablaIncidencias;
    @FXML private TableColumn<Mortalidad, String> colId;
    @FXML private TableColumn<Mortalidad, String> colLote;
    @FXML private TableColumn<Mortalidad, String> colFecha;
    @FXML private TableColumn<Mortalidad, String> colTipo;
    @FXML private TableColumn<Mortalidad, String> colCantidad;
    @FXML private TableColumn<Mortalidad, String> colCausa;
    @FXML private TableColumn<Mortalidad, String> colEstado;
    @FXML private TableColumn<Mortalidad, String> colAcciones;
    @FXML private TableColumn<Mortalidad, String> colObservaciones;

    private final MortalidadService servicio = new MortalidadService();
    private final ObservableList<Mortalidad> incidencias =
            FXCollections.observableArrayList();
    private final Map<Long, String> nombresLotes = new HashMap<>();
    private FilteredList<Mortalidad> incidenciasFiltradas;
    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean ocupado;

    private record OpcionLote(Long id, String nombre) {
        @Override public String toString() {
            return nombre + " (ID: " + id + ")";
        }
    }

    @FXML
    public void initialize() {
        configurarColumnas();

        cmbTipoIncidencia.setItems(FXCollections.observableArrayList(
                "MORTALIDAD", "SANIDAD"));
        cmbCausa.setItems(FXCollections.observableArrayList(
                "SIN DETERMINAR", "FALTA DE OXIGENO",
                "CALIDAD DEL AGUA", "CAMBIO DE TEMPERATURA",
                "POSIBLE ENFERMEDAD", "ESTRES POR MANIPULACION",
                "OTRA"));
        cmbEstado.setItems(FXCollections.observableArrayList("ABIERTA", "CERRADA"));

        incidenciasFiltradas = new FilteredList<>(incidencias, fila -> true);
        tablaIncidencias.setItems(incidenciasFiltradas);
        dpFecha.setValue(LocalDate.now());
        cmbEstado.setValue("ABIERTA");

        cmbTipoIncidencia.valueProperty().addListener((obs, anterior, nuevo) -> {
            boolean sanidad = "SANIDAD".equals(nuevo);
            txtCantidadMuertas.setDisable(sanidad);
            if (sanidad) {
                txtCantidadMuertas.setText("0");
            } else if ("MORTALIDAD".equals(nuevo) &&
                    "0".equals(txtCantidadMuertas.getText().trim())) {
                txtCantidadMuertas.clear();
            }
        });
        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> aplicarBusqueda());
        tablaIncidencias.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionada) -> {
                    if (seleccionada != null) mostrarEnFormulario(seleccionada);
                    actualizarBotones();
                });

        actualizarBotones();
        if (!SesionUsuario.haySesion()) {
            mostrarError("Debes iniciar sesion para consultar las incidencias.");
            return;
        }

        cargarLotes();
        cargarIncidencias();
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(c -> celda(c.getValue().getId()));
        colLote.setCellValueFactory(c -> celda(nombreLote(c.getValue().getLoteId())));
        colFecha.setCellValueFactory(c -> celda(
                c.getValue().getFecha() == null ? "" :
                        c.getValue().getFecha().format(formatoFecha)));
        colTipo.setCellValueFactory(c -> celda(c.getValue().getTipoIncidencia()));
        colCantidad.setCellValueFactory(c -> celda(c.getValue().getCantidadMuertas()));
        colCausa.setCellValueFactory(c -> celda(c.getValue().getCausa()));
        colEstado.setCellValueFactory(c -> celda(c.getValue().getEstado()));
        colAcciones.setCellValueFactory(c -> celda(c.getValue().getAcciones()));
        colObservaciones.setCellValueFactory(c -> celda(c.getValue().getObservaciones()));
    }

    private ReadOnlyStringWrapper celda(Object valor) {
        return new ReadOnlyStringWrapper(valor == null ? "" : valor.toString());
    }

    private String nombreLote(Long id) {
        if (id == null) return "";
        return nombresLotes.getOrDefault(id, "Lote " + id);
    }

    /** Carga todos los lotes sin suponer que la columna se llama 'nombre'. */
    private void cargarLotes() {
        Task<List<OpcionLote>> tarea = new Task<>() {
            @Override
            protected List<OpcionLote> call() throws SQLException {
                List<OpcionLote> lista = new ArrayList<>();
                try (Connection con = DatabaseConfig.getConnection();
                     PreparedStatement ps = con.prepareStatement(
                             "SELECT * FROM lotes ORDER BY id");
                     ResultSet rs = ps.executeQuery()) {
                    Set<String> columnas = columnasDisponibles(rs.getMetaData());
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String nombre = primerTextoDisponible(rs, columnas,
                                "nombre", "codigo", "nombre_lote", "descripcion");
                        if (nombre == null || nombre.isBlank()) {
                            nombre = "Lote " + id;
                        }
                        lista.add(new OpcionLote(id, nombre));
                    }
                }
                return lista;
            }
        };
        tarea.setOnSucceeded(e -> {
            cmbLote.getItems().setAll(tarea.getValue());
            nombresLotes.clear();
            for (OpcionLote opcion : tarea.getValue()) {
                nombresLotes.put(opcion.id(), opcion.nombre());
            }
            Mortalidad seleccionada =
                    tablaIncidencias.getSelectionModel().getSelectedItem();
            if (seleccionada != null) mostrarEnFormulario(seleccionada);
            tablaIncidencias.refresh();
            aplicarBusqueda();
        });
        tarea.setOnFailed(e -> mostrarError(
                "No se pudieron cargar los lotes: " + mensaje(tarea.getException())));
        iniciarTarea(tarea, "mortalidad-lotes");
    }

    private Set<String> columnasDisponibles(ResultSetMetaData meta)
            throws SQLException {
        Set<String> columnas = new HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            columnas.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }
        return columnas;
    }

    private String primerTextoDisponible(ResultSet rs, Set<String> columnas,
                                        String... opciones) throws SQLException {
        for (String columna : opciones) {
            if (columnas.contains(columna)) {
                String valor = rs.getString(columna);
                if (valor != null && !valor.isBlank()) return valor.trim();
            }
        }
        return null;
    }

    private void cargarIncidencias() {
        Task<List<Mortalidad>> tarea = new Task<>() {
            @Override protected List<Mortalidad> call() throws SQLException {
                return servicio.listar();
            }
        };
        tarea.setOnSucceeded(e -> {
            incidencias.setAll(tarea.getValue());
            actualizarIndicadores();
            aplicarBusqueda();
            actualizarBotones();
        });
        tarea.setOnFailed(e -> mostrarError(
                "No se pudo cargar el historial: " + mensaje(tarea.getException())));
        iniciarTarea(tarea, "mortalidad-incidencias");
    }

    private void actualizarIndicadores() {
        lblTotalIncidencias.setText(String.valueOf(incidencias.size()));
        long totalMuertas = 0;
        long casosAbiertos = 0;
        for (Mortalidad m : incidencias) {
            if (m.getCantidadMuertas() != null) {
                totalMuertas += m.getCantidadMuertas();
            }
            if ("ABIERTA".equals(m.getEstado())) casosAbiertos++;
        }
        lblTotalMuertas.setText(String.valueOf(totalMuertas));
        lblCasosAbiertos.setText(String.valueOf(casosAbiertos));
    }

    private void aplicarBusqueda() {
        if (incidenciasFiltradas == null) return;
        String texto = txtBuscar.getText() == null ? "" :
                txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        incidenciasFiltradas.setPredicate(m -> {
            if (texto.isEmpty()) return true;
            String datos = (nombreLote(m.getLoteId()) + " " +
                    m.getLoteId() + " " + m.getId() + " " +
                    (m.getFecha() == null ? "" : m.getFecha().format(formatoFecha)) + " " +
                    m.getTipoIncidencia() + " " + m.getCantidadMuertas() + " " +
                    m.getCausa() + " " + m.getEstado() + " " +
                    m.getAcciones() + " " + m.getObservaciones())
                    .toLowerCase(Locale.ROOT);
            return datos.contains(texto);
        });
    }

    private void mostrarEnFormulario(Mortalidad m) {
        cmbLote.setValue(null);
        for (OpcionLote opcion : cmbLote.getItems()) {
            if (opcion.id().equals(m.getLoteId())) {
                cmbLote.setValue(opcion);
                break;
            }
        }
        dpFecha.setValue(m.getFecha());
        cmbTipoIncidencia.setValue(m.getTipoIncidencia());
        txtCantidadMuertas.setText(String.valueOf(m.getCantidadMuertas()));
        cmbCausa.setValue(m.getCausa());
        cmbEstado.setValue(m.getEstado());
        txtAcciones.setText(m.getAcciones() == null ? "" : m.getAcciones());
        txtObservaciones.setText(
                m.getObservaciones() == null ? "" : m.getObservaciones());
    }

    private Mortalidad leerFormulario() {
        OpcionLote opcion = cmbLote.getValue();
        if (opcion == null) {
            throw new IllegalArgumentException("Selecciona un lote.");
        }
        Mortalidad m = new Mortalidad();
        m.setLoteId(opcion.id());
        m.setFecha(dpFecha.getValue());
        m.setTipoIncidencia(cmbTipoIncidencia.getValue());
        m.setCantidadMuertas(leerCantidad());
        m.setCausa(cmbCausa.getValue());
        m.setEstado(cmbEstado.getValue());
        m.setAcciones(txtAcciones.getText());
        m.setObservaciones(txtObservaciones.getText());
        return m;
    }

    private int leerCantidad() {
        if ("SANIDAD".equals(cmbTipoIncidencia.getValue())) return 0;
        String texto = txtCantidadMuertas.getText() == null ? "" :
                txtCantidadMuertas.getText().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Ingresa la cantidad de truchas muertas.");
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser un numero entero. Ejemplo: 5");
        }
    }

    @FXML
    private void registrarIncidencia() {
        if (ocupado) return;
        if (!SesionUsuario.esAdministrador() && !SesionUsuario.esOperador()) {
            mostrarError("No tienes permiso para registrar incidencias.");
            return;
        }
        final Mortalidad incidencia;
        try {
            incidencia = leerFormulario();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
            return;
        }
        actualizarOcupado(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws Exception {
                servicio.registrar(incidencia);
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            actualizarOcupado(false);
            limpiarFormulario();
            cargarIncidencias();
            mostrarInformacion("Incidencia registrada correctamente.");
        });
        tarea.setOnFailed(e -> {
            actualizarOcupado(false);
            mostrarError("No se pudo registrar: " + mensaje(tarea.getException()));
        });
        iniciarTarea(tarea, "registrar-mortalidad");
    }

    @FXML
    private void modificarIncidencia() {
        if (ocupado) return;
        if (!SesionUsuario.esAdministrador()) {
            mostrarError("Solo el Administrador puede modificar incidencias.");
            return;
        }
        Mortalidad seleccionada =
                tablaIncidencias.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarError("Selecciona una incidencia del historial.");
            return;
        }
        final Mortalidad incidencia;
        try {
            incidencia = leerFormulario();
            incidencia.setId(seleccionada.getId());
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
            return;
        }
        actualizarOcupado(true);
        Task<Boolean> tarea = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return servicio.actualizar(incidencia);
            }
        };
        tarea.setOnSucceeded(e -> {
            actualizarOcupado(false);
            if (!tarea.getValue()) {
                mostrarError("La incidencia ya no existe. Actualiza el historial.");
                cargarIncidencias();
                return;
            }
            limpiarFormulario();
            cargarIncidencias();
            mostrarInformacion("Incidencia modificada correctamente.");
        });
        tarea.setOnFailed(e -> {
            actualizarOcupado(false);
            mostrarError("No se pudo modificar: " + mensaje(tarea.getException()));
        });
        iniciarTarea(tarea, "modificar-mortalidad");
    }

    @FXML
    private void limpiarFormulario() {
        tablaIncidencias.getSelectionModel().clearSelection();
        cmbLote.setValue(null);
        dpFecha.setValue(LocalDate.now());
        cmbTipoIncidencia.setValue(null);
        txtCantidadMuertas.clear();
        txtCantidadMuertas.setDisable(false);
        cmbCausa.setValue(null);
        cmbEstado.setValue("ABIERTA");
        txtAcciones.clear();
        txtObservaciones.clear();
        actualizarBotones();
    }

    private void actualizarOcupado(boolean valor) {
        ocupado = valor;
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean sesion = SesionUsuario.haySesion();
        btnRegistrar.setDisable(ocupado || !sesion ||
                (!SesionUsuario.esAdministrador() && !SesionUsuario.esOperador()));
        btnModificar.setDisable(ocupado || !SesionUsuario.esAdministrador() ||
                tablaIncidencias.getSelectionModel().getSelectedItem() == null);
    }

    private void iniciarTarea(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }

    private String mensaje(Throwable error) {
        if (error == null) return "Error desconocido";
        return error.getMessage() == null ? error.toString() : error.getMessage();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("PisciControlF - Mortalidad y Sanidad");
        alerta.setHeaderText("No se pudo completar la operacion");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarInformacion(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("PisciControlF - Mortalidad y Sanidad");
        alerta.setHeaderText("Operacion completada");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void navegar(ActionEvent evento, String archivo, String titulo) {
        if (!SesionUsuario.haySesion()) {
            mostrarError("Debes iniciar sesion para continuar.");
            return;
        }
        try {
            Navegacion.abrir(evento, archivo, titulo);
        } catch (IOException ex) {
            mostrarError("No se pudo abrir la pantalla: " + mensaje(ex));
        }
    }

    @FXML private void volverAlPanel(ActionEvent e) {
        if (SesionUsuario.esAdministrador()) {
            navegar(e, "panel-administrador.fxml", "Panel del Administrador");
        } else if (SesionUsuario.esOperador()) {
            navegar(e, "panel-operador.fxml", "Panel del Operador");
        } else {
            mostrarError("Debes iniciar sesion.");
        }
    }
    @FXML private void abrirEstanques(ActionEvent e) {
        navegar(e, "gestion-estanques.fxml", "Gestion de Estanques");
    }
    @FXML private void abrirLotes(ActionEvent e) {
        navegar(e, "gestion-lotes.fxml", "Gestion de Lotes");
    }
    @FXML private void abrirAlimentacion(ActionEvent e) {
        navegar(e, "gestion-alimentacion.fxml", "Gestion de Alimentacion");
    }
    @FXML private void abrirCrecimiento(ActionEvent e) {
        navegar(e, "gestion-crecimiento.fxml", "Gestion de Crecimiento");
    }
    @FXML private void abrirCalidadAgua(ActionEvent e) {
        navegar(e, "gestion-calidad-agua.fxml", "Calidad del Agua");
    }
    @FXML private void abrirProduccion(ActionEvent e) {
        navegar(e, "gestion-produccion.fxml", "Gestion de Produccion");
    }
    @FXML private void abrirVentas(ActionEvent e) {
        navegar(e, "gestion-ventas.fxml", "Gestion de Ventas");
    }
    @FXML private void abrirReportes(ActionEvent e) {
        navegar(e, "gestion-reportes.fxml", "Reportes Generales");
    }
}

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
import pe.edu.upeu.piscicontrolf.model.CalidadAgua;
import pe.edu.upeu.piscicontrolf.service.CalidadAguaService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalidadAguaController {

    @FXML private Label lblTotalMediciones;
    @FXML private Label lblUltimaTemperatura;
    @FXML private Label lblUltimoPh;
    @FXML private Label lblUltimoOxigeno;

    @FXML private ComboBox<OpcionEstanque> cmbEstanque;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtTemperatura;
    @FXML private TextField txtPh;
    @FXML private TextField txtOxigeno;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtBuscar;

    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;
    @FXML private Button btnLimpiar;

    @FXML private TableView<CalidadAgua> tablaCalidad;
    @FXML private TableColumn<CalidadAgua, String> colId;
    @FXML private TableColumn<CalidadAgua, String> colEstanque;
    @FXML private TableColumn<CalidadAgua, String> colFecha;
    @FXML private TableColumn<CalidadAgua, String> colTemperatura;
    @FXML private TableColumn<CalidadAgua, String> colPh;
    @FXML private TableColumn<CalidadAgua, String> colOxigeno;
    @FXML private TableColumn<CalidadAgua, String> colEstado;
    @FXML private TableColumn<CalidadAgua, String> colObservaciones;

    private final CalidadAguaService servicio = new CalidadAguaService();
    private final ObservableList<CalidadAgua> mediciones = FXCollections.observableArrayList();
    private final Map<Long, String> nombresEstanques = new HashMap<>();
    private FilteredList<CalidadAgua> medicionesFiltradas;
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean ocupado;

    private record OpcionEstanque(Long id, String nombre) {
        @Override public String toString() {
            return nombre + " (ID: " + id + ")";
        }
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> celda(c.getValue().getId()));
        colEstanque.setCellValueFactory(c -> celda(nombreEstanque(c.getValue().getEstanqueId())));
        colFecha.setCellValueFactory(c -> celda(
                c.getValue().getFecha() == null ? "" :
                        c.getValue().getFecha().format(formatoFecha)));
        colTemperatura.setCellValueFactory(c -> celda(
                numero(c.getValue().getTemperaturaC()) + " °C"));
        colPh.setCellValueFactory(c -> celda(numero(c.getValue().getPh())));
        colOxigeno.setCellValueFactory(c -> celda(
                numero(c.getValue().getOxigenoMgL()) + " mg/L"));
        colEstado.setCellValueFactory(c -> celda(c.getValue().getEstado()));
        colObservaciones.setCellValueFactory(c -> celda(c.getValue().getObservaciones()));

        medicionesFiltradas = new FilteredList<>(mediciones, fila -> true);
        tablaCalidad.setItems(medicionesFiltradas);
        dpFecha.setValue(LocalDate.now());

        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> aplicarBusqueda());
        tablaCalidad.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, actual) -> {
                    if (actual != null) mostrarEnFormulario(actual);
                    actualizarBotones();
                });

        if (!SesionUsuario.haySesion()) {
            actualizarBotones();
            mostrarError("Debes iniciar sesión para consultar la calidad del agua.");
            return;
        }

        actualizarBotones();
        cargarEstanques();
        cargarMediciones();
    }

    private ReadOnlyStringWrapper celda(Object valor) {
        return new ReadOnlyStringWrapper(valor == null ? "" : valor.toString());
    }

    private String numero(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString();
    }

    private String nombreEstanque(Long id) {
        if (id == null) return "";
        return nombresEstanques.getOrDefault(id, "Estanque " + id);
    }

    private void cargarEstanques() {
        Task<List<OpcionEstanque>> tarea = new Task<>() {
            @Override
            protected List<OpcionEstanque> call() throws SQLException {
                List<OpcionEstanque> lista = new ArrayList<>();
                String sql = "SELECT id, nombre FROM estanques ORDER BY nombre, id";
                try (Connection con = DatabaseConfig.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new OpcionEstanque(rs.getLong("id"), rs.getString("nombre")));
                    }
                }
                return lista;
            }
        };
        tarea.setOnSucceeded(e -> {
            cmbEstanque.getItems().setAll(tarea.getValue());
            nombresEstanques.clear();
            for (OpcionEstanque opcion : tarea.getValue()) {
                nombresEstanques.put(opcion.id(), opcion.nombre());
            }
            tablaCalidad.refresh();
            aplicarBusqueda();
        });
        tarea.setOnFailed(e -> mostrarError(
                "No se pudieron cargar los estanques: " + mensaje(tarea.getException())));
        iniciarTarea(tarea, "calidad-estanques");
    }

    private void cargarMediciones() {
        Task<List<CalidadAgua>> tarea = new Task<>() {
            @Override
            protected List<CalidadAgua> call() throws SQLException {
                return servicio.listar();
            }
        };
        tarea.setOnSucceeded(e -> {
            mediciones.setAll(tarea.getValue());
            actualizarIndicadores();
            aplicarBusqueda();
        });
        tarea.setOnFailed(e -> mostrarError(
                "No se pudo cargar el historial: " + mensaje(tarea.getException())));
        iniciarTarea(tarea, "calidad-mediciones");
    }

    private void actualizarIndicadores() {
        lblTotalMediciones.setText(String.valueOf(mediciones.size()));
        if (mediciones.isEmpty()) {
            lblUltimaTemperatura.setText("-- °C");
            lblUltimoPh.setText("--");
            lblUltimoOxigeno.setText("-- mg/L");
            return;
        }
        // El repositorio ordena por fecha descendente e ID descendente.
        CalidadAgua ultima = mediciones.get(0);
        lblUltimaTemperatura.setText(numero(ultima.getTemperaturaC()) + " °C");
        lblUltimoPh.setText(numero(ultima.getPh()));
        lblUltimoOxigeno.setText(numero(ultima.getOxigenoMgL()) + " mg/L");
    }

    private void aplicarBusqueda() {
        if (medicionesFiltradas == null) return;
        String texto = txtBuscar.getText() == null ? "" :
                txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        medicionesFiltradas.setPredicate(fila -> {
            if (texto.isEmpty()) return true;
            String datos = (nombreEstanque(fila.getEstanqueId()) + " " +
                    fila.getEstanqueId() + " " + fila.getId() + " " +
                    (fila.getFecha() == null ? "" : fila.getFecha().format(formatoFecha)) + " " +
                    fila.getEstado() + " " + fila.getObservaciones()).toLowerCase(Locale.ROOT);
            return datos.contains(texto);
        });
    }

    private void mostrarEnFormulario(CalidadAgua calidad) {
        for (OpcionEstanque opcion : cmbEstanque.getItems()) {
            if (opcion.id().equals(calidad.getEstanqueId())) {
                cmbEstanque.setValue(opcion);
                break;
            }
        }
        dpFecha.setValue(calidad.getFecha());
        txtTemperatura.setText(numero(calidad.getTemperaturaC()));
        txtPh.setText(numero(calidad.getPh()));
        txtOxigeno.setText(numero(calidad.getOxigenoMgL()));
        txtObservaciones.setText(calidad.getObservaciones() == null ? "" : calidad.getObservaciones());
    }

    private CalidadAgua leerFormulario() {
        OpcionEstanque opcion = cmbEstanque.getValue();
        if (opcion == null) {
            throw new IllegalArgumentException("Selecciona un estanque.");
        }
        CalidadAgua calidad = new CalidadAgua();
        calidad.setEstanqueId(opcion.id());
        calidad.setFecha(dpFecha.getValue());
        calidad.setTemperaturaC(leerDecimal(txtTemperatura, "temperatura"));
        calidad.setPh(leerDecimal(txtPh, "pH"));
        calidad.setOxigenoMgL(leerDecimal(txtOxigeno, "oxígeno disuelto"));
        calidad.setObservaciones(txtObservaciones.getText() == null ? "" :
                txtObservaciones.getText().trim());
        return calidad;
    }

    private BigDecimal leerDecimal(TextField campo, String nombre) {
        String texto = campo.getText() == null ? "" : campo.getText().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Ingresa el valor de " + nombre + ".");
        }
        try {
            return new BigDecimal(texto.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "El valor de " + nombre + " debe ser numérico. Ejemplo: 14.5");
        }
    }

    @FXML
    private void registrarCalidad() {
        if (ocupado) return;
        if (!SesionUsuario.esAdministrador() && !SesionUsuario.esOperador()) {
            mostrarError("No tienes permiso para registrar mediciones.");
            return;
        }
        final CalidadAgua medicion;
        try {
            medicion = leerFormulario();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
            return;
        }

        actualizarOcupado(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws Exception {
                servicio.registrar(medicion);
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            actualizarOcupado(false);
            limpiarFormulario();
            cargarMediciones();
            mostrarInformacion("Medición registrada. Estado orientativo: " + medicion.getEstado());
        });
        tarea.setOnFailed(e -> {
            actualizarOcupado(false);
            mostrarError("No se pudo registrar: " + mensaje(tarea.getException()));
        });
        iniciarTarea(tarea, "registrar-calidad");
    }

    @FXML
    private void modificarCalidad() {
        if (ocupado) return;
        if (!SesionUsuario.esAdministrador()) {
            mostrarError("Solo el Administrador puede modificar mediciones.");
            return;
        }
        CalidadAgua seleccionada = tablaCalidad.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarError("Selecciona una medición del historial.");
            return;
        }

        final CalidadAgua medicion;
        try {
            medicion = leerFormulario();
            medicion.setId(seleccionada.getId());
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
            return;
        }

        actualizarOcupado(true);
        Task<Boolean> tarea = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return servicio.actualizar(medicion);
            }
        };
        tarea.setOnSucceeded(e -> {
            actualizarOcupado(false);
            if (!tarea.getValue()) {
                mostrarError("La medición ya no existe. Actualiza el historial.");
                cargarMediciones();
                return;
            }
            limpiarFormulario();
            cargarMediciones();
            mostrarInformacion("Medición actualizada. Estado orientativo: " + medicion.getEstado());
        });
        tarea.setOnFailed(e -> {
            actualizarOcupado(false);
            mostrarError("No se pudo modificar: " + mensaje(tarea.getException()));
        });
        iniciarTarea(tarea, "modificar-calidad");
    }

    @FXML
    private void limpiarFormulario() {
        tablaCalidad.getSelectionModel().clearSelection();
        cmbEstanque.setValue(null);
        dpFecha.setValue(LocalDate.now());
        txtTemperatura.clear();
        txtPh.clear();
        txtOxigeno.clear();
        txtObservaciones.clear();
        actualizarBotones();
    }

    private void actualizarOcupado(boolean nuevo) {
        ocupado = nuevo;
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean sesion = SesionUsuario.haySesion();
        boolean puedeRegistrar = sesion &&
                (SesionUsuario.esAdministrador() || SesionUsuario.esOperador());
        btnRegistrar.setDisable(ocupado || !puedeRegistrar);
        btnModificar.setDisable(ocupado || !SesionUsuario.esAdministrador() ||
                tablaCalidad.getSelectionModel().getSelectedItem() == null);
        btnLimpiar.setDisable(ocupado || !sesion);
    }

    private void iniciarTarea(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }

    private String mensaje(Throwable error) {
        return error == null ? "Error desconocido" : error.getMessage();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("PisciControlF - Calidad del Agua");
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarInformacion(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("PisciControlF - Calidad del Agua");
        alerta.setHeaderText("Operación completada");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void navegar(ActionEvent evento, String archivo, String titulo) {
        if (!SesionUsuario.haySesion()) {
            mostrarError("Debes iniciar sesión para continuar.");
            return;
        }
        try {
            Navegacion.abrir(evento, archivo, titulo);
        } catch (IOException ex) {
            mostrarError("No se pudo abrir la pantalla: " + ex.getMessage());
        }
    }

    @FXML private void volverAlPanel(ActionEvent e) {
        if (SesionUsuario.esAdministrador()) {
            navegar(e, "panel-administrador.fxml", "Panel del Administrador");
        } else if (SesionUsuario.esOperador()) {
            navegar(e, "panel-operador.fxml", "Panel del Operador");
        } else {
            mostrarError("Debes iniciar sesión.");
        }
    }
    @FXML private void abrirEstanques(ActionEvent e) {
        navegar(e, "gestion-estanques.fxml", "Gestión de Estanques");
    }
    @FXML private void abrirLotes(ActionEvent e) {
        navegar(e, "gestion-lotes.fxml", "Gestión de Lotes");
    }
    @FXML private void abrirAlimentacion(ActionEvent e) {
        navegar(e, "gestion-alimentacion.fxml", "Gestión de Alimentación");
    }
    @FXML private void abrirCrecimiento(ActionEvent e) {
        navegar(e, "gestion-crecimiento.fxml", "Gestión de Crecimiento");
    }
    @FXML private void abrirProduccion(ActionEvent e) {
        navegar(e, "gestion-produccion.fxml", "Gestión de Producción");
    }
    @FXML private void abrirVentas(ActionEvent e) {
        navegar(e, "gestion-ventas.fxml", "Gestión de Ventas");
    }
    @FXML private void abrirReportes(ActionEvent e) {
        navegar(e, "gestion-reportes.fxml", "Reportes Generales");
    }
}

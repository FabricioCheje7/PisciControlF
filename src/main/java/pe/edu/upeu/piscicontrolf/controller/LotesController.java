
package pe.edu.upeu.piscicontrolf.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Estanque;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.service.EstanqueService;
import pe.edu.upeu.piscicontrolf.service.LoteService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LotesController {

    // FORMULARIO
    @FXML private ComboBox<Estanque> cmbEstanque;
    @FXML private DatePicker dpFechaIngreso;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtBuscar;
    @FXML private TextArea txtObservaciones;

    // INDICADORES
    @FXML private Label lblCapacidadTotal;
    @FXML private Label lblOcupadas;
    @FXML private Label lblDisponibles;
    @FXML private ProgressBar barraCapacidad;

    // BOTONES
    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;

    // TABLA
    @FXML private TableView<Lote> tablaLotes;
    @FXML private TableColumn<Lote, Long> colId;
    @FXML private TableColumn<Lote, String> colEstanque;
    @FXML private TableColumn<Lote, String> colFecha;
    @FXML private TableColumn<Lote, Integer> colCantidad;
    @FXML private TableColumn<Lote, String> colObservaciones;

    // SERVICIOS
    private final EstanqueService estanqueService =
            new EstanqueService();

    private final LoteService loteService =
            new LoteService();

    private final ObservableList<Lote> datos =
            FXCollections.observableArrayList();

    private final Map<Long, String> nombresEstanques =
            new HashMap<>();

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // INICIALIZAR PANTALLA
    @FXML
    public void initialize() {

        configurarTabla();
        configurarComboBox();
        configurarBuscador();
        configurarSeleccion();

        dpFechaIngreso.setValue(LocalDate.now());

        // PERMISOS SEGÚN ROL
        boolean esAdmin = SesionUsuario.esAdministrador();

        btnRegistrar.setDisable(!esAdmin);
        btnModificar.setDisable(!esAdmin);

        dpFechaIngreso.setDisable(!esAdmin);
        txtCantidad.setEditable(esAdmin);
        txtObservaciones.setEditable(esAdmin);

        // El operador puede seleccionar un
        // estanque para consultar su capacidad.

        cargarEstanques();
        cargarLotes();
        actualizarCapacidad();
    }

    // CONFIGURAR TABLA
    private void configurarTabla() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getId()));

        colEstanque.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        nombresEstanques.getOrDefault(
                                c.getValue().getEstanqueId(),
                                "Estanque no disponible")));

        colFecha.setCellValueFactory(c -> {

            LocalDate fecha =
                    c.getValue().getFechaIngreso();

            return new ReadOnlyStringWrapper(
                    fecha == null
                            ? ""
                            : fecha.format(formatoFecha));
        });

        colCantidad.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getCantidadTruchas()));

        colObservaciones.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getObservaciones() == null
                                ? ""
                                : c.getValue().getObservaciones()));
    }

    // MOSTRAR NOMBRE DE ESTANQUES
    private void configurarComboBox() {

        cmbEstanque.setConverter(
                new StringConverter<Estanque>() {

                    @Override
                    public String toString(Estanque estanque) {
                        return estanque == null
                                ? ""
                                : estanque.getNombre();
                    }

                    @Override
                    public Estanque fromString(String texto) {
                        return null;
                    }
                });

        cmbEstanque.valueProperty().addListener(
                (obs, anterior, nuevo) ->
                        actualizarCapacidad());
    }

    // BUSCADOR EN TIEMPO REAL
    private void configurarBuscador() {

        FilteredList<Lote> filtrados =
                new FilteredList<>(datos, l -> true);

        tablaLotes.setItems(filtrados);

        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> {

                    String texto = nuevo == null
                            ? ""
                            : nuevo.trim()
                            .toLowerCase(Locale.ROOT);

                    filtrados.setPredicate(lote -> {

                        String nombre = nombresEstanques
                                .getOrDefault(
                                        lote.getEstanqueId(), "")
                                .toLowerCase(Locale.ROOT);

                        String id =
                                String.valueOf(lote.getId());

                        return texto.isEmpty()
                                || nombre.contains(texto)
                                || id.contains(texto);
                    });
                });
    }

    // SELECCIONAR LOTE
    private void configurarSeleccion() {

        tablaLotes.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, lote) -> {

                    if (lote == null) {
                        return;
                    }

                    Estanque encontrado = cmbEstanque
                            .getItems()
                            .stream()
                            .filter(e ->
                                    e.getId().equals(
                                            lote.getEstanqueId()))
                            .findFirst()
                            .orElse(null);

                    cmbEstanque.setValue(encontrado);

                    dpFechaIngreso.setValue(
                            lote.getFechaIngreso());

                    txtCantidad.setText(
                            String.valueOf(
                                    lote.getCantidadTruchas()));

                    txtObservaciones.setText(
                            lote.getObservaciones() == null
                                    ? ""
                                    : lote.getObservaciones());
                });
    }

    // CARGAR ESTANQUES DESDE H2
    private void cargarEstanques() {

        try {
            nombresEstanques.clear();

            ObservableList<Estanque> activos =
                    FXCollections.observableArrayList();

            for (Estanque e : estanqueService.listar()) {

                nombresEstanques.put(
                        e.getId(),
                        e.getNombre());

                if ("ACTIVO".equalsIgnoreCase(
                        e.getEstado())) {

                    activos.add(e);
                }
            }

            cmbEstanque.setItems(activos);

        } catch (SQLException e) {

            mostrarError(
                    "No se pudieron cargar los estanques: "
                            + e.getMessage());
        }
    }

    // CARGAR LOTES
    private void cargarLotes() {

        try {
            datos.setAll(loteService.listar());

        } catch (SQLException e) {

            mostrarError(
                    "No se pudieron cargar los lotes: "
                            + e.getMessage());
        }
    }

    // VERIFICAR ADMINISTRADOR
    private boolean verificarAdministrador() {

        if (!SesionUsuario.esAdministrador()) {

            mostrarError(
                    "Solo el administrador puede registrar "
                            + "o modificar lotes.");

            return false;
        }

        return true;
    }

    // LEER DATOS DEL FORMULARIO
    private Lote leerFormulario(Long id) {

        Estanque estanque = cmbEstanque.getValue();

        if (estanque == null) {
            throw new IllegalArgumentException(
                    "Seleccione un estanque activo.");
        }

        LocalDate fecha = dpFechaIngreso.getValue();

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Seleccione la fecha de ingreso.");
        }

        int cantidad;

        try {
            cantidad = Integer.parseInt(
                    txtCantidad.getText().trim());

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Ingrese una cantidad numérica válida.");
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de truchas debe ser mayor que cero.");
        }

        return new Lote(
                id,
                estanque.getId(),
                fecha,
                cantidad,
                txtObservaciones.getText().trim());
    }

    // VALIDAR CAPACIDAD
    private void validarCapacidad(
            Lote nuevo, Lote anterior) throws SQLException {

        int disponible =
                loteService.obtenerCapacidadDisponible(
                        nuevo.getEstanqueId());

        // Si modificamos un lote en el mismo
        // estanque, se considera su cantidad anterior.
        if (anterior != null
                && anterior.getEstanqueId().equals(
                nuevo.getEstanqueId())) {

            disponible += anterior.getCantidadTruchas();
        }

        if (nuevo.getCantidadTruchas() > disponible) {

            throw new IllegalArgumentException(
                    "La cantidad supera la capacidad disponible "
                            + "del estanque. Disponibles para "
                            + "esta operación: "
                            + disponible + " truchas.");
        }
    }

    // REGISTRAR LOTE
    @FXML
    private void registrarLote() {

        if (!verificarAdministrador()) {
            return;
        }

        try {
            Lote lote = leerFormulario(null);

            validarCapacidad(lote, null);

            loteService.registrar(lote);

            cargarLotes();
            limpiarFormulario();

            mostrarMensaje(
                    "Lote registrado correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            mostrarError(e.getMessage());
        }
    }

    // MODIFICAR LOTE
    @FXML
    private void modificarLote() {

        if (!verificarAdministrador()) {
            return;
        }

        Lote seleccionado =
                tablaLotes.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {

            mostrarError(
                    "Seleccione un lote de la tabla.");

            return;
        }

        try {
            Lote modificado =
                    leerFormulario(seleccionado.getId());

            validarCapacidad(modificado, seleccionado);

            loteService.actualizar(modificado);

            cargarLotes();
            limpiarFormulario();

            mostrarMensaje(
                    "Lote modificado correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            mostrarError(e.getMessage());
        }
    }

    // LIMPIAR FORMULARIO
    @FXML
    private void limpiarFormulario() {

        tablaLotes.getSelectionModel()
                .clearSelection();

        cmbEstanque.setValue(null);

        dpFechaIngreso.setValue(LocalDate.now());

        txtCantidad.clear();
        txtObservaciones.clear();

        actualizarCapacidad();
    }

    // ACTUALIZAR INDICADORES DE CAPACIDAD
    private void actualizarCapacidad() {

        Estanque estanque = cmbEstanque.getValue();

        if (estanque == null) {

            lblCapacidadTotal.setText("0 truchas");
            lblOcupadas.setText("0 truchas");
            lblDisponibles.setText("0 truchas");

            barraCapacidad.setProgress(0);

            return;
        }

        try {
            int total = estanque.getCapacidad();

            int disponible =
                    loteService.obtenerCapacidadDisponible(
                            estanque.getId());

            int ocupadas = total - disponible;

            lblCapacidadTotal.setText(
                    total + " truchas");

            lblOcupadas.setText(
                    ocupadas + " truchas");

            lblDisponibles.setText(
                    disponible + " truchas");

            double progreso = total > 0
                    ? (double) ocupadas / total
                    : 0;

            barraCapacidad.setProgress(
                    Math.max(0, Math.min(1, progreso)));

        } catch (SQLException e) {

            lblCapacidadTotal.setText("—");
            lblOcupadas.setText("—");
            lblDisponibles.setText("—");
            barraCapacidad.setProgress(0);

            mostrarError(
                    "No se pudo consultar la capacidad: "
                            + e.getMessage());
        }
    }

    // MENSAJE DE ÉXITO
    private void mostrarMensaje(String mensaje) {

        Alert alerta =
                new Alert(Alert.AlertType.INFORMATION);

        alerta.setTitle("PisciControlF");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }

    // MENSAJE DE ERROR
    private void mostrarError(String mensaje) {

        Alert alerta =
                new Alert(Alert.AlertType.ERROR);

        alerta.setTitle("PisciControlF - Error");
        alerta.setHeaderText(
                "No se pudo realizar la operación");
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }

    // VOLVER AL PANEL SEGÚN EL ROL
    @FXML
    private void volverAlPanel(ActionEvent evento) {

        if (SesionUsuario.esAdministrador()) {

            navegar(
                    evento,
                    "panel-administrador.fxml",
                    "Panel del Administrador");

        } else if (SesionUsuario.esOperador()) {

            navegar(
                    evento,
                    "panel-operador.fxml",
                    "Panel del Operador");

        } else {

            navegar(
                    evento,
                    "login.fxml",
                    "Inicio de Sesión");
        }
    }

    // ABRIR ESTANQUES
    @FXML
    private void abrirEstanques(ActionEvent evento) {

        navegar(
                evento,
                "gestion-estanques.fxml",
                "Gestión de Estanques");
    }

    // ABRIR ALIMENTACIÓN
    @FXML
    private void abrirAlimentacion(ActionEvent evento) {

        navegar(
                evento,
                "gestion-alimentacion.fxml",
                "Gestión de Alimentación");
    }

    // ABRIR CRECIMIENTO
    @FXML
    private void abrirCrecimiento(ActionEvent evento) {

        navegar(
                evento,
                "gestion-crecimiento.fxml",
                "Gestión de Crecimiento");
    }

    // NAVEGACIÓN
    private void navegar(
            ActionEvent evento,
            String archivo,
            String titulo) {

        if (!SesionUsuario.haySesion()
                && !"login.fxml".equals(archivo)) {

            mostrarError(
                    "Debe iniciar sesión para continuar.");

            return;
        }

        try {
            Navegacion.abrir(
                    evento,
                    archivo,
                    titulo);

        } catch (IOException e) {

            mostrarError(
                    "No se pudo abrir la pantalla: "
                            + e.getMessage());
        }
    }
}

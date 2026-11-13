
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
import pe.edu.upeu.piscicontrolf.model.Alimentacion;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.service.AlimentacionService;
import pe.edu.upeu.piscicontrolf.service.LoteService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class AlimentacionController {

    // FORMULARIO
    @FXML private ComboBox<Lote> cmbLote;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtTipoAlimento;
    @FXML private TextField txtCantidadKg;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtBuscar;

    // INDICADOR
    @FXML private Label lblTotalRegistros;

    // BOTONES
    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;

    // TABLA
    @FXML private TableView<Alimentacion> tablaAlimentacion;
    @FXML private TableColumn<Alimentacion, Long> colId;
    @FXML private TableColumn<Alimentacion, String> colLote;
    @FXML private TableColumn<Alimentacion, String> colFecha;
    @FXML private TableColumn<Alimentacion, String> colTipo;
    @FXML private TableColumn<Alimentacion, String> colCantidad;
    @FXML private TableColumn<Alimentacion, String> colObservaciones;

    // SERVICIOS
    private final AlimentacionService service =
            new AlimentacionService();

    private final LoteService loteService =
            new LoteService();

    private final ObservableList<Alimentacion> datos =
            FXCollections.observableArrayList();

    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // INICIALIZAR PANTALLA
    @FXML
    public void initialize() {

        configurarTabla();
        configurarComboBox();
        configurarBuscador();
        configurarSeleccion();

        dpFecha.setValue(LocalDate.now());

        configurarPermisos();

        cargarLotes();
        cargarRegistros();
    }

    // CONFIGURAR COLUMNAS
    private void configurarTabla() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getId()));

        colLote.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        "Lote " + c.getValue().getLoteId()));

        colFecha.setCellValueFactory(c -> {

            LocalDate fecha =
                    c.getValue().getFecha();

            return new ReadOnlyStringWrapper(
                    fecha == null
                            ? ""
                            : fecha.format(formatoFecha));
        });

        colTipo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getTipoAlimento()));

        colCantidad.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getCantidadKg()
                                .toPlainString()));

        colObservaciones.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getObservaciones() == null
                                ? ""
                                : c.getValue().getObservaciones()));
    }

    // MOSTRAR NOMBRE DE LOS LOTES
    private void configurarComboBox() {

        cmbLote.setConverter(new StringConverter<Lote>() {

            @Override
            public String toString(Lote lote) {
                return lote == null
                        ? ""
                        : "Lote " + lote.getId();
            }

            @Override
            public Lote fromString(String texto) {
                return null;
            }
        });

        cmbLote.setCellFactory(lista -> new ListCell<Lote>() {

            @Override
            protected void updateItem(
                    Lote lote, boolean vacio) {

                super.updateItem(lote, vacio);

                setText(vacio || lote == null
                        ? null
                        : "Lote " + lote.getId());
            }
        });
    }

    // BUSCADOR EN TIEMPO REAL
    private void configurarBuscador() {

        FilteredList<Alimentacion> filtrados =
                new FilteredList<>(datos, a -> true);

        tablaAlimentacion.setItems(filtrados);

        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> {

                    String buscar = nuevo == null
                            ? ""
                            : nuevo.trim()
                            .toLowerCase(Locale.ROOT);

                    filtrados.setPredicate(a -> {

                        String tipo =
                                a.getTipoAlimento() == null
                                        ? ""
                                        : a.getTipoAlimento()
                                        .toLowerCase(Locale.ROOT);

                        String lote =
                                "lote " + a.getLoteId();

                        return buscar.isEmpty()
                                || tipo.contains(buscar)
                                || lote.contains(buscar);
                    });
                });
    }

    // SELECCIONAR REGISTRO DE LA TABLA
    private void configurarSeleccion() {

        tablaAlimentacion.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, anterior, seleccionado) -> {

                            if (seleccionado == null) {
                                return;
                            }

                            Lote loteEncontrado =
                                    cmbLote.getItems()
                                            .stream()
                                            .filter(l -> Objects.equals(
                                                    l.getId(),
                                                    seleccionado.getLoteId()))
                                            .findFirst()
                                            .orElse(null);

                            cmbLote.setValue(loteEncontrado);

                            dpFecha.setValue(
                                    seleccionado.getFecha());

                            txtTipoAlimento.setText(
                                    seleccionado.getTipoAlimento());

                            txtCantidadKg.setText(
                                    seleccionado.getCantidadKg()
                                            .toPlainString());

                            txtObservaciones.setText(
                                    seleccionado.getObservaciones() == null
                                            ? ""
                                            : seleccionado.getObservaciones());
                        });
    }

    // PERMISOS POR ROL
    private void configurarPermisos() {

        boolean administrador =
                SesionUsuario.esAdministrador();

        boolean operador =
                SesionUsuario.esOperador();

        boolean puedeRegistrar =
                administrador || operador;

        // AMBOS PUEDEN REGISTRAR
        btnRegistrar.setDisable(!puedeRegistrar);

        // SOLO ADMINISTRADOR PUEDE MODIFICAR
        btnModificar.setDisable(!administrador);

        cmbLote.setDisable(!puedeRegistrar);
        dpFecha.setDisable(!puedeRegistrar);

        txtTipoAlimento.setEditable(puedeRegistrar);
        txtCantidadKg.setEditable(puedeRegistrar);
        txtObservaciones.setEditable(puedeRegistrar);
    }

    // CARGAR LOTES
    private void cargarLotes() {

        try {
            cmbLote.setItems(
                    FXCollections.observableArrayList(
                            loteService.listar()));

        } catch (SQLException e) {

            mostrarError(
                    "No se pudieron cargar los lotes: "
                            + e.getMessage());
        }
    }

    // CARGAR REGISTROS DESDE H2
    private void cargarRegistros() {

        try {
            datos.setAll(service.listar());

            lblTotalRegistros.setText(
                    String.valueOf(datos.size()));

        } catch (SQLException e) {

            mostrarError(
                    "No se pudo cargar la alimentación: "
                            + e.getMessage());
        }
    }

    // LEER FORMULARIO
    private Alimentacion leerFormulario(Long id) {

        Lote lote = cmbLote.getValue();

        if (lote == null) {
            throw new IllegalArgumentException(
                    "Seleccione un lote de truchas.");
        }

        LocalDate fecha = dpFecha.getValue();

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Seleccione la fecha de alimentación.");
        }

        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de alimentación no puede ser futura.");
        }

        String tipo =
                txtTipoAlimento.getText().trim();

        if (tipo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ingrese el tipo de alimento.");
        }

        BigDecimal cantidad;

        try {
            String texto =
                    txtCantidadKg.getText()
                            .trim()
                            .replace(',', '.');

            cantidad = new BigDecimal(texto);

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Ingrese una cantidad válida en kilogramos.");
        }

        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero.");
        }

        if (cantidad.scale() > 3) {
            throw new IllegalArgumentException(
                    "Use como máximo tres decimales "
                            + "para los kilogramos.");
        }

        String observaciones =
                txtObservaciones.getText().trim();

        if (observaciones.length() > 255) {
            throw new IllegalArgumentException(
                    "Las observaciones no deben superar "
                            + "255 caracteres.");
        }

        return new Alimentacion(
                id,
                lote.getId(),
                fecha,
                tipo,
                cantidad,
                observaciones);
    }

    // REGISTRAR ALIMENTACIÓN
    @FXML
    private void registrarAlimentacion() {

        boolean autorizado =
                SesionUsuario.esAdministrador()
                        || SesionUsuario.esOperador();

        if (!autorizado) {
            mostrarError(
                    "No tiene permiso para registrar alimentación.");
            return;
        }

        try {
            Alimentacion alimentacion =
                    leerFormulario(null);

            service.registrar(alimentacion);

            cargarRegistros();
            limpiarFormulario();

            mostrarMensaje(
                    "Alimentación registrada correctamente.");

        } catch (SQLException
                 | IllegalArgumentException
                 | SecurityException e) {

            mostrarError(e.getMessage());
        }
    }

    // MODIFICAR ALIMENTACIÓN
    @FXML
    private void modificarAlimentacion() {

        if (!SesionUsuario.esAdministrador()) {
            mostrarError(
                    "Solo el administrador puede modificar "
                            + "registros de alimentación.");
            return;
        }

        Alimentacion seleccionado =
                tablaAlimentacion.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {
            mostrarError(
                    "Seleccione un registro de alimentación.");
            return;
        }

        try {
            Alimentacion actualizado =
                    leerFormulario(seleccionado.getId());

            service.actualizar(actualizado);

            cargarRegistros();
            limpiarFormulario();

            mostrarMensaje(
                    "Alimentación modificada correctamente.");

        } catch (SQLException
                 | IllegalArgumentException
                 | SecurityException e) {

            mostrarError(e.getMessage());
        }
    }

    // LIMPIAR FORMULARIO
    @FXML
    private void limpiarFormulario() {

        tablaAlimentacion.getSelectionModel()
                .clearSelection();

        cmbLote.setValue(null);
        dpFecha.setValue(LocalDate.now());

        txtTipoAlimento.clear();
        txtCantidadKg.clear();
        txtObservaciones.clear();

        cmbLote.requestFocus();
    }

    // VOLVER AL PANEL SEGÚN ROL
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
                    "Iniciar Sesión");
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

    // ABRIR LOTES
    @FXML
    private void abrirLotes(ActionEvent evento) {

        navegar(
                evento,
                "gestion-lotes.fxml",
                "Gestión de Lotes");
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
                    evento, archivo, titulo);

        } catch (IOException e) {

            mostrarError(
                    "No se pudo abrir la pantalla: "
                            + e.getMessage());
        }
    }

    // MENSAJES
    private void mostrarMensaje(String mensaje) {

        Alert alerta =
                new Alert(Alert.AlertType.INFORMATION);

        alerta.setTitle("PisciControlF");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {

        Alert alerta =
                new Alert(Alert.AlertType.ERROR);

        alerta.setTitle("PisciControlF - Error");
        alerta.setHeaderText(
                "No se pudo realizar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}


package pe.edu.upeu.piscicontrolf.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Crecimiento;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.service.CrecimientoService;
import pe.edu.upeu.piscicontrolf.service.LoteService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CrecimientoController {

    // FORMULARIO
    @FXML private ComboBox<Lote> cmbLote;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtPeso;
    @FXML private TextField txtTalla;
    @FXML private TextField txtMuestra;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtBuscar;

    // INDICADORES
    @FXML private Label lblTotalMediciones;
    @FXML private Label lblPesoActual;
    @FXML private Label lblTallaActual;

    // BOTONES
    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;

    // TABLA
    @FXML private TableView<Crecimiento> tablaCrecimiento;
    @FXML private TableColumn<Crecimiento, Long> colId;
    @FXML private TableColumn<Crecimiento, String> colLote;
    @FXML private TableColumn<Crecimiento, String> colFecha;
    @FXML private TableColumn<Crecimiento, String> colPeso;
    @FXML private TableColumn<Crecimiento, String> colTalla;
    @FXML private TableColumn<Crecimiento, Integer> colMuestra;
    @FXML private TableColumn<Crecimiento, String> colObservaciones;

    // GRAFICO
    @FXML private LineChart<String, Number> graficoPeso;

    // SERVICIOS
    private final CrecimientoService crecimientoService =
            new CrecimientoService();

    private final LoteService loteService =
            new LoteService();

    private final ObservableList<Crecimiento> datos =
            FXCollections.observableArrayList();

    private final DateTimeFormatter formato =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // INICIALIZAR
    @FXML
    public void initialize() {

        configurarTabla();
        configurarComboBox();
        configurarBuscador();
        configurarSeleccion();
        configurarPermisos();

        dpFecha.setValue(LocalDate.now());

        cmbLote.valueProperty().addListener(
                (obs, anterior, nuevo) ->
                        actualizarGrafico());

        cargarLotes();
        cargarMediciones();

        // MOSTRAR AUTOMATICAMENTE UN LOTE
        // QUE TENGA MEDICIONES
        seleccionarLoteInicial();
    }

    // CONFIGURAR COLUMNAS
    private void configurarTabla() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getId()));

        colLote.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        "Lote " + c.getValue().getLoteId()));

        colFecha.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getFecha()
                                .format(formato)));

        colPeso.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getPesoPromedioG()
                                .toPlainString()));

        colTalla.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getTallaPromedioCm()
                                .toPlainString()));

        colMuestra.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getCantidadMuestra()));

        colObservaciones.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getObservaciones() == null
                                ? ""
                                : c.getValue().getObservaciones()));
    }

    // MOSTRAR LOTES CON NOMBRES CLAROS
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
    }

    // BUSCADOR EN TIEMPO REAL
    private void configurarBuscador() {

        FilteredList<Crecimiento> filtrados =
                new FilteredList<>(datos, c -> true);

        tablaCrecimiento.setItems(filtrados);

        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> {

                    String buscar = nuevo == null
                            ? ""
                            : nuevo.trim()
                            .toLowerCase(Locale.ROOT);

                    filtrados.setPredicate(c -> {

                        String lote =
                                "lote " + c.getLoteId();

                        String fecha =
                                c.getFecha().format(formato);

                        return buscar.isEmpty()
                                || lote.contains(buscar)
                                || fecha.contains(buscar);
                    });
                });
    }

    // SELECCIONAR MEDICION DE LA TABLA
    private void configurarSeleccion() {

        tablaCrecimiento.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, c) -> {

                    if (c == null) {
                        return;
                    }

                    seleccionarLotePorId(c.getLoteId());

                    dpFecha.setValue(c.getFecha());

                    txtPeso.setText(
                            c.getPesoPromedioG()
                                    .toPlainString());

                    txtTalla.setText(
                            c.getTallaPromedioCm()
                                    .toPlainString());

                    txtMuestra.setText(
                            String.valueOf(
                                    c.getCantidadMuestra()));

                    txtObservaciones.setText(
                            c.getObservaciones() == null
                                    ? ""
                                    : c.getObservaciones());
                });
    }

    // PERMISOS
    private void configurarPermisos() {

        boolean administrador =
                SesionUsuario.esAdministrador();

        boolean operador =
                SesionUsuario.esOperador();

        boolean puedeRegistrar =
                administrador || operador;

        btnRegistrar.setDisable(!puedeRegistrar);
        btnModificar.setDisable(!administrador);

        cmbLote.setDisable(!puedeRegistrar);
        dpFecha.setDisable(!puedeRegistrar);

        txtPeso.setEditable(puedeRegistrar);
        txtTalla.setEditable(puedeRegistrar);
        txtMuestra.setEditable(puedeRegistrar);
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
                    "Error al cargar lotes: "
                            + e.getMessage());
        }
    }

    // CARGAR MEDICIONES DE H2
    private void cargarMediciones() {

        try {
            datos.setAll(crecimientoService.listar());

            lblTotalMediciones.setText(
                    String.valueOf(datos.size()));

            actualizarGrafico();

        } catch (SQLException e) {
            mostrarError(
                    "Error al cargar mediciones: "
                            + e.getMessage());
        }
    }

    // SELECCIONAR LOTE POR ID
    private void seleccionarLotePorId(Long id) {

        cmbLote.getItems()
                .stream()
                .filter(l ->
                        Objects.equals(l.getId(), id))
                .findFirst()
                .ifPresent(cmbLote::setValue);
    }

    // SELECCIONAR UN LOTE CON DATOS
    private void seleccionarLoteInicial() {

        datos.stream()
                .max(
                        Comparator
                                .comparing(Crecimiento::getFecha)
                                .thenComparing(Crecimiento::getId)
                )
                .ifPresent(c ->
                        seleccionarLotePorId(
                                c.getLoteId()));
    }

    // LEER FORMULARIO
    private Crecimiento leerFormulario(Long id) {

        Lote lote = cmbLote.getValue();

        if (lote == null) {
            throw new IllegalArgumentException(
                    "Seleccione un lote de truchas.");
        }

        LocalDate fecha = dpFecha.getValue();

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Seleccione la fecha de medición.");
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

        try {
            BigDecimal peso = new BigDecimal(
                    txtPeso.getText().trim()
                            .replace(',', '.'));

            BigDecimal talla = new BigDecimal(
                    txtTalla.getText().trim()
                            .replace(',', '.'));

            int muestra = Integer.parseInt(
                    txtMuestra.getText().trim());

            if (peso.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "El peso debe ser mayor que cero.");
            }

            if (talla.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "La talla debe ser mayor que cero.");
            }

            if (peso.scale() > 2 || talla.scale() > 2) {
                throw new IllegalArgumentException(
                        "Peso y talla admiten máximo "
                                + "dos decimales.");
            }

            if (muestra <= 0) {
                throw new IllegalArgumentException(
                        "La muestra debe ser mayor que cero.");
            }

            if (muestra > lote.getCantidadTruchas()) {
                throw new IllegalArgumentException(
                        "La muestra no puede superar "
                                + "la cantidad registrada del lote.");
            }

            return new Crecimiento(
                    id,
                    lote.getId(),
                    fecha,
                    peso,
                    talla,
                    muestra,
                    txtObservaciones.getText().trim()
            );

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Ingrese números válidos "
                            + "en peso, talla y muestra.");
        }
    }

    // REGISTRAR MEDICION
    @FXML
    private void registrarCrecimiento() {

        if (!SesionUsuario.esAdministrador()
                && !SesionUsuario.esOperador()) {

            mostrarError(
                    "No tiene permiso para registrar mediciones.");
            return;
        }

        try {
            Crecimiento crecimiento =
                    leerFormulario(null);

            crecimientoService.registrar(crecimiento);

            // ACTUALIZAR HISTORIAL Y GRAFICO
            cargarMediciones();

            // CONSERVAR EL LOTE SELECCIONADO
            limpiarCampos();

            mostrarMensaje(
                    "Medición registrada correctamente.");


        } catch (SQLException
                 | IllegalArgumentException
                 | SecurityException e) {

            mostrarError(e.getMessage());
        }

    }

    // MODIFICAR MEDICION
    @FXML
    private void modificarCrecimiento() {

        if (!SesionUsuario.esAdministrador()) {
            mostrarError(
                    "Solo el administrador puede "
                            + "modificar mediciones.");
            return;
        }

        Crecimiento seleccionado =
                tablaCrecimiento.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {
            mostrarError(
                    "Seleccione una medición de la tabla.");
            return;
        }

        try {
            Crecimiento actualizado =
                    leerFormulario(seleccionado.getId());

            crecimientoService.actualizar(actualizado);

            cargarMediciones();

            // NO DESELECCIONAR EL LOTE
            limpiarCampos();

            mostrarMensaje(
                    "Medición modificada correctamente.");


        } catch (SQLException
                 | IllegalArgumentException
                 | SecurityException e) {

            mostrarError(e.getMessage());
        }

    }

    // LIMPIAR SOLO LOS CAMPOS DE MEDICION
    private void limpiarCampos() {

        tablaCrecimiento.getSelectionModel()
                .clearSelection();

        dpFecha.setValue(LocalDate.now());

        txtPeso.clear();
        txtTalla.clear();
        txtMuestra.clear();
        txtObservaciones.clear();
    }

    // LIMPIAR TODO EL FORMULARIO
    @FXML
    private void limpiarFormulario() {

        limpiarCampos();

        // SOLO EL BOTON LIMPIAR QUITA EL LOTE
        cmbLote.getSelectionModel().clearSelection();
        cmbLote.setValue(null);
    }

    // GRAFICO E INDICADORES
    private void actualizarGrafico() {

        graficoPeso.getData().clear();

        lblPesoActual.setText("-- g");
        lblTallaActual.setText("-- cm");

        Lote lote = cmbLote.getValue();

        if (lote == null) {
            return;
        }

        // USAR LOS REGISTROS YA CARGADOS
        // DESDE H2 Y ORDENARLOS POR FECHA
        List<Crecimiento> mediciones =
                datos.stream()
                        .filter(c ->
                                Objects.equals(
                                        c.getLoteId(),
                                        lote.getId()))
                        .sorted(
                                Comparator
                                        .comparing(
                                                Crecimiento::getFecha)
                                        .thenComparing(
                                                Crecimiento::getId)
                        )
                        .toList();

        if (mediciones.isEmpty()) {
            return;
        }

        XYChart.Series<String, Number> serie =
                new XYChart.Series<>();

        serie.setName("Peso promedio");

        for (Crecimiento c : mediciones) {

            serie.getData().add(
                    new XYChart.Data<>(
                            c.getFecha().format(formato),
                            c.getPesoPromedioG()
                    )
            );
        }

        graficoPeso.getData().add(serie);

        // VERDADERA ULTIMA MEDICION
        Crecimiento ultimo =
                mediciones.get(mediciones.size() - 1);

        lblPesoActual.setText(
                ultimo.getPesoPromedioG()
                        .toPlainString() + " g");

        lblTallaActual.setText(
                ultimo.getTallaPromedioCm()
                        .toPlainString() + " cm");
    }

    // VOLVER AL PANEL SEGUN ROL
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

    @FXML
    private void abrirEstanques(ActionEvent evento) {
        navegar(
                evento,
                "gestion-estanques.fxml",
                "Gestión de Estanques");
    }

    @FXML
    private void abrirLotes(ActionEvent evento) {
        navegar(
                evento,
                "gestion-lotes.fxml",
                "Gestión de Lotes");
    }

    @FXML
    private void abrirAlimentacion(ActionEvent evento) {
        navegar(
                evento,
                "gestion-alimentacion.fxml",
                "Gestión de Alimentación");
    }

    // NAVEGACION
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
                "No se pudo completar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

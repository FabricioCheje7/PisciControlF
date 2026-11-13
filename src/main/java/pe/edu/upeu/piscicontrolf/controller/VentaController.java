package pe.edu.upeu.piscicontrolf.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Produccion;
import pe.edu.upeu.piscicontrolf.model.Venta;
import pe.edu.upeu.piscicontrolf.service.ProduccionService;
import pe.edu.upeu.piscicontrolf.service.VentaService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class VentaController {

    // FORMULARIO
    @FXML private ComboBox<Produccion> cmbProduccion;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtCliente;
    @FXML private TextField txtCantidadKg;
    @FXML private TextField txtPrecioUnitarioKg;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtBuscar;

    // INDICADORES Y STOCK
    @FXML private Label lblTotalVentas;
    @FXML private Label lblKgVendidos;
    @FXML private Label lblIngresosTotales;
    @FXML private Label lblTotalCalculado;
    @FXML private Label lblKgDisponibles;

    // BOTONES
    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;
    @FXML private Button btnAnular;
    @FXML private Button btnLimpiar;

    // TABLA
    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, Long> colId;
    @FXML private TableColumn<Venta, String> colProduccion;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, String> colCantidad;
    @FXML private TableColumn<Venta, String> colPrecio;
    @FXML private TableColumn<Venta, String> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;
    @FXML private TableColumn<Venta, String> colObservaciones;

    private final VentaService ventaService = new VentaService();
    private final ProduccionService produccionService = new ProduccionService();
    private final ObservableList<Venta> datos = FXCollections.observableArrayList();
    private final DateTimeFormatter fechaFormato = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
        configurarComboProducciones();
        configurarBuscador();
        configurarSeleccion();

        dpFecha.setValue(LocalDate.now());
        txtCantidadKg.textProperty().addListener((obs, anterior, nuevo) -> actualizarTotal());
        txtPrecioUnitarioKg.textProperty().addListener((obs, anterior, nuevo) -> actualizarTotal());
        cmbProduccion.valueProperty().addListener((obs, anterior, nuevo) -> actualizarDisponibilidad());

        actualizarBotones();
        actualizarTotal();
        cargarDatosIniciales();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colProduccion.setCellValueFactory(c ->
                new ReadOnlyStringWrapper("Producción " + c.getValue().getProduccionId()));
        colFecha.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getFecha().format(fechaFormato)));
        colCliente.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getCliente()));
        colCantidad.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(formatoKg(c.getValue().getCantidadKg())));
        colPrecio.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(formatoSoles(c.getValue().getPrecioUnitarioKg())));
        colTotal.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(formatoSoles(c.getValue().getTotal())));
        colEstado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getEstado()));
        colObservaciones.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getObservaciones() == null
                        ? "" : c.getValue().getObservaciones()));
    }

    private void configurarComboProducciones() {
        cmbProduccion.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produccion produccion) {
                return nombreProduccion(produccion);
            }

            @Override
            public Produccion fromString(String texto) {
                return null;
            }
        });

        cmbProduccion.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Produccion produccion, boolean vacio) {
                super.updateItem(produccion, vacio);
                setText(vacio ? null : nombreProduccion(produccion));
            }
        });
    }

    private String nombreProduccion(Produccion produccion) {
        if (produccion == null) {
            return "";
        }
        return "Producción " + produccion.getId()
                + " | Lote " + produccion.getLoteId()
                + " | " + formatoKg(produccion.getPesoTotalKg()) + " kg"
                + ("REGISTRADA".equals(produccion.getEstado()) ? "" : " | ANULADA");
    }

    private void configurarBuscador() {
        FilteredList<Venta> filtrados = new FilteredList<>(datos, v -> true);
        tablaVentas.setItems(filtrados);

        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String buscar = nuevo == null ? "" : nuevo.trim().toLowerCase(Locale.ROOT);
            filtrados.setPredicate(venta -> {
                String texto = (venta.getCliente() == null ? "" : venta.getCliente())
                        + " produccion " + venta.getProduccionId()
                        + " producción " + venta.getProduccionId()
                        + " " + venta.getFecha().format(fechaFormato)
                        + " " + (venta.getEstado() == null ? "" : venta.getEstado());
                return buscar.isEmpty() || texto.toLowerCase(Locale.ROOT).contains(buscar);
            });
        });
    }

    private void configurarSeleccion() {
        tablaVentas.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, venta) -> {
                    if (venta != null) {
                        cmbProduccion.getItems().stream()
                                .filter(p -> Objects.equals(p.getId(), venta.getProduccionId()))
                                .findFirst()
                                .ifPresentOrElse(cmbProduccion::setValue,
                                        () -> cmbProduccion.setValue(null));

                        dpFecha.setValue(venta.getFecha());
                        txtCliente.setText(venta.getCliente());
                        txtCantidadKg.setText(venta.getCantidadKg().toPlainString());
                        txtPrecioUnitarioKg.setText(venta.getPrecioUnitarioKg().toPlainString());
                        txtObservaciones.setText(venta.getObservaciones() == null
                                ? "" : venta.getObservaciones());
                    }
                    actualizarBotones();
                    actualizarDisponibilidad();
                });
    }

    private void actualizarBotones() {
        boolean administrador = SesionUsuario.esAdministrador();
        boolean autorizado = administrador || SesionUsuario.esOperador();
        Venta seleccionada = tablaVentas.getSelectionModel().getSelectedItem();
        boolean enEdicion = seleccionada != null;
        boolean registrada = enEdicion && "REGISTRADA".equals(seleccionada.getEstado());

        btnRegistrar.setDisable(!autorizado || enEdicion);
        btnModificar.setDisable(!administrador || !registrada);
        btnAnular.setDisable(!administrador || !registrada);

        // Una venta existente no puede trasladarse a otra producción.
        cmbProduccion.setDisable(!autorizado || enEdicion);
        dpFecha.setDisable(!autorizado || (enEdicion && !administrador));
        txtCliente.setEditable(autorizado && (!enEdicion || administrador));
        txtCantidadKg.setEditable(autorizado && (!enEdicion || administrador));
        txtPrecioUnitarioKg.setEditable(autorizado && (!enEdicion || administrador));
        txtObservaciones.setEditable(autorizado && (!enEdicion || administrador));
    }

    private record DatosIniciales(List<Produccion> producciones, List<Venta> ventas) { }

    // EVITA BLOQUEAR LA VENTANA AL CONSULTAR H2 POR PRIMERA VEZ.
    private void cargarDatosIniciales() {
        Task<DatosIniciales> tarea = new Task<>() {
            @Override
            protected DatosIniciales call() throws Exception {
                return new DatosIniciales(produccionService.listar(), ventaService.listar());
            }
        };

        tarea.setOnSucceeded(evento -> {
            DatosIniciales resultado = tarea.getValue();
            cmbProduccion.setItems(FXCollections.observableArrayList(resultado.producciones()));
            datos.setAll(resultado.ventas());
            actualizarIndicadores();
            actualizarDisponibilidad();
        });

        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            mostrarError("No se pudieron cargar las ventas: "
                    + (error == null ? "Error desconocido" : error.getMessage()));
        });

        Thread hilo = new Thread(tarea, "carga-ventas");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void recargarDatos() throws SQLException {
        // El formulario se limpia antes de recargar para no cambiar una selección antigua.
        cmbProduccion.setItems(FXCollections.observableArrayList(produccionService.listar()));
        datos.setAll(ventaService.listar());
        actualizarIndicadores();
        actualizarDisponibilidad();
    }

    private void actualizarIndicadores() {
        long ventasActivas = 0;
        BigDecimal kilogramos = BigDecimal.ZERO;
        BigDecimal ingresos = BigDecimal.ZERO;

        for (Venta venta : datos) {
            if ("REGISTRADA".equals(venta.getEstado())) {
                ventasActivas++;
                kilogramos = kilogramos.add(venta.getCantidadKg());
                ingresos = ingresos.add(venta.getTotal());
            }
        }

        lblTotalVentas.setText(String.valueOf(ventasActivas));
        lblKgVendidos.setText(formatoKg(kilogramos) + " kg");
        lblIngresosTotales.setText("S/ " + formatoSoles(ingresos));
    }

    private void actualizarDisponibilidad() {
        Produccion produccion = cmbProduccion.getValue();
        if (produccion == null) {
            lblKgDisponibles.setText("Seleccione una producción");
            return;
        }
        if (!"REGISTRADA".equals(produccion.getEstado())) {
            lblKgDisponibles.setText("Producción anulada");
            return;
        }

        try {
            BigDecimal disponibles = ventaService.obtenerKgDisponibles(produccion.getId());
            Venta seleccionada = tablaVentas.getSelectionModel().getSelectedItem();
            boolean edicion = seleccionada != null
                    && "REGISTRADA".equals(seleccionada.getEstado())
                    && Objects.equals(seleccionada.getProduccionId(), produccion.getId());

            if (edicion) {
                disponibles = disponibles.add(seleccionada.getCantidadKg());
            }
            lblKgDisponibles.setText(formatoKg(disponibles) + " kg"
                    + (edicion ? " (incluye venta seleccionada)" : ""));
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            lblKgDisponibles.setText("No disponible");
            mostrarError(e.getMessage());
        }
    }

    private void actualizarTotal() {
        String cantidad = txtCantidadKg.getText();
        String precio = txtPrecioUnitarioKg.getText();
        if (cantidad == null || cantidad.isBlank() || precio == null || precio.isBlank()) {
            lblTotalCalculado.setText("S/ 0.00");
            return;
        }
        try {
            BigDecimal total = ventaService.calcularTotal(
                    leerDecimal(cantidad), leerDecimal(precio));
            lblTotalCalculado.setText("S/ " + formatoSoles(total));
        } catch (IllegalArgumentException e) {
            lblTotalCalculado.setText("S/ --");
        }
    }

    private BigDecimal leerDecimal(String texto) {
        return new BigDecimal(texto.trim().replace(',', '.'));
    }

    private Venta leerFormulario(Long id) {
        Produccion produccion = cmbProduccion.getValue();
        if (produccion == null || !"REGISTRADA".equals(produccion.getEstado())) {
            throw new IllegalArgumentException("Seleccione una producción registrada.");
        }
        LocalDate fecha = dpFecha.getValue();
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ingrese una fecha válida, no futura.");
        }
        if (fecha.isBefore(produccion.getFecha())) {
            throw new IllegalArgumentException("La venta no puede ser anterior a la cosecha.");
        }

        BigDecimal cantidad;
        BigDecimal precio;
        try {
            cantidad = leerDecimal(txtCantidadKg.getText());
            precio = leerDecimal(txtPrecioUnitarioKg.getText());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ingrese números válidos en cantidad y precio.");
        }

        BigDecimal total = ventaService.calcularTotal(cantidad, precio);
        return new Venta(id, produccion.getId(), fecha,
                txtCliente.getText().trim(), cantidad, precio, total,
                "REGISTRADA", txtObservaciones.getText().trim());
    }

    @FXML
    private void registrarVenta() {
        if (!SesionUsuario.esAdministrador() && !SesionUsuario.esOperador()) {
            mostrarError("No tiene permiso para registrar ventas.");
            return;
        }
        if (tablaVentas.getSelectionModel().getSelectedItem() != null) {
            mostrarError("Presione Limpiar formulario para registrar una nueva venta.");
            return;
        }

        try {
            Venta venta = leerFormulario(null);
            ventaService.registrar(venta);
            limpiarFormulario();
            recargarDatos();
            mostrarMensaje("Venta registrada correctamente.");
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void modificarVenta() {
        if (!SesionUsuario.esAdministrador()) {
            mostrarError("Solo el administrador puede modificar ventas.");
            return;
        }
        Venta seleccionada = tablaVentas.getSelectionModel().getSelectedItem();
        if (seleccionada == null || !"REGISTRADA".equals(seleccionada.getEstado())) {
            mostrarError("Seleccione una venta registrada para modificar.");
            return;
        }

        try {
            Venta actualizada = leerFormulario(seleccionada.getId());
            ventaService.actualizar(actualizada);
            limpiarFormulario();
            recargarDatos();
            mostrarMensaje("Venta modificada correctamente.");
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void anularVenta() {
        if (!SesionUsuario.esAdministrador()) {
            mostrarError("Solo el administrador puede anular ventas.");
            return;
        }
        Venta seleccionada = tablaVentas.getSelectionModel().getSelectedItem();
        if (seleccionada == null || !"REGISTRADA".equals(seleccionada.getEstado())) {
            mostrarError("Seleccione una venta registrada para anular.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("PisciControlF");
        confirmacion.setHeaderText("Confirmar anulación de venta");
        confirmacion.setContentText("¿Desea anular la venta N.º " + seleccionada.getId()
                + "? Su cantidad volverá a estar disponible para vender.");
        if (confirmacion.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        try {
            ventaService.anular(seleccionada.getId());
            limpiarFormulario();
            recargarDatos();
            mostrarMensaje("Venta anulada correctamente.");
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void limpiarFormulario() {
        tablaVentas.getSelectionModel().clearSelection();
        cmbProduccion.setValue(null);
        dpFecha.setValue(LocalDate.now());
        txtCliente.clear();
        txtCantidadKg.clear();
        txtPrecioUnitarioKg.clear();
        txtObservaciones.clear();
        lblTotalCalculado.setText("S/ 0.00");
        lblKgDisponibles.setText("Seleccione una producción");
        actualizarBotones();
    }

    @FXML
    private void volverAlPanel(ActionEvent evento) {
        if (SesionUsuario.esAdministrador()) {
            navegar(evento, "panel-administrador.fxml", "Panel del Administrador");
        } else if (SesionUsuario.esOperador()) {
            navegar(evento, "panel-operador.fxml", "Panel del Operador");
        } else {
            navegar(evento, "login.fxml", "Iniciar sesión");
        }
    }

    @FXML
    private void abrirEstanques(ActionEvent evento) {
        navegar(evento, "gestion-estanques.fxml", "Gestión de Estanques");
    }

    @FXML
    private void abrirLotes(ActionEvent evento) {
        navegar(evento, "gestion-lotes.fxml", "Gestión de Lotes");
    }

    @FXML
    private void abrirAlimentacion(ActionEvent evento) {
        navegar(evento, "gestion-alimentacion.fxml", "Gestión de Alimentación");
    }

    @FXML
    private void abrirCrecimiento(ActionEvent evento) {
        navegar(evento, "gestion-crecimiento.fxml", "Gestión de Crecimiento");
    }

    @FXML
    private void abrirProduccion(ActionEvent evento) {
        navegar(evento, "gestion-produccion.fxml", "Gestión de Producción");
    }

    private void navegar(ActionEvent evento, String archivo, String titulo) {
        if (!SesionUsuario.haySesion() && !"login.fxml".equals(archivo)) {
            mostrarError("Debe iniciar sesión para continuar.");
            return;
        }
        try {
            Navegacion.abrir(evento, archivo, titulo);
        } catch (IOException e) {
            mostrarError("No se pudo abrir la pantalla: " + e.getMessage());
        }
    }

    private String formatoKg(BigDecimal numero) {
        return numero == null ? "0.000" :
                numero.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatoSoles(BigDecimal numero) {
        return numero == null ? "0.00" :
                numero.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void mostrarMensaje(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("PisciControlF");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("PisciControlF - Error");
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

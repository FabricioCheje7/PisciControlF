package pe.edu.upeu.piscicontrolf.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.model.Produccion;
import pe.edu.upeu.piscicontrolf.service.LoteService;
import pe.edu.upeu.piscicontrolf.service.ProduccionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProduccionController {

    // FORMULARIO
    @FXML private ComboBox<Lote> cmbLote;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPesoTotal;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtBuscar;

    // INDICADORES
    @FXML private Label lblTotalProducciones;
    @FXML private Label lblTotalTruchasCosechadas;
    @FXML private Label lblPesoTotalProducido;
    @FXML private Label lblTruchasDisponibles;

    // BOTONES
    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;
    @FXML private Button btnAnular;
    @FXML private Button btnLimpiar;

    // TABLA
    @FXML private TableView<Produccion> tablaProduccion;
    @FXML private TableColumn<Produccion, Long> colId;
    @FXML private TableColumn<Produccion, String> colLote;
    @FXML private TableColumn<Produccion, String> colFecha;
    @FXML private TableColumn<Produccion, Integer> colCantidad;
    @FXML private TableColumn<Produccion, String> colPesoTotal;
    @FXML private TableColumn<Produccion, String> colEstado;
    @FXML private TableColumn<Produccion, String> colObservaciones;

    private final ProduccionService produccionService = new ProduccionService();
    private final LoteService loteService = new LoteService();
    private final ObservableList<Produccion> datos = FXCollections.observableArrayList();
    private final DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarTabla();
        configurarComboLotes();
        configurarBuscador();
        configurarSeleccion();
        configurarPermisos();

        dpFecha.setValue(LocalDate.now());
        cmbLote.valueProperty().addListener((obs, anterior, nuevo) -> actualizarDisponibles());
        cargarDatosEnSegundoPlano();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colLote.setCellValueFactory(c -> new ReadOnlyStringWrapper("Lote " + c.getValue().getLoteId()));
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFecha().format(formato)));
        colCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCantidadTruchas()));
        colPesoTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPesoTotalKg().toPlainString()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEstado()));
        colObservaciones.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getObservaciones() == null ? "" : c.getValue().getObservaciones()));
    }

    private void configurarComboLotes() {
        cmbLote.setConverter(new StringConverter<>() {
            @Override
            public String toString(Lote lote) {
                return lote == null ? "" : "Lote " + lote.getId();
            }

            @Override
            public Lote fromString(String texto) {
                return null;
            }
        });

        cmbLote.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Lote lote, boolean vacio) {
                super.updateItem(lote, vacio);
                setText(vacio || lote == null ? null : "Lote " + lote.getId());
            }
        });
    }

    private void configurarBuscador() {
        FilteredList<Produccion> filtrados = new FilteredList<>(datos, p -> true);
        tablaProduccion.setItems(filtrados);

        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String buscar = nuevo == null ? "" : nuevo.trim().toLowerCase(Locale.ROOT);
            filtrados.setPredicate(p -> {
                String estado = p.getEstado() == null ? "" : p.getEstado().toLowerCase(Locale.ROOT);
                String texto = "lote " + p.getLoteId() + " " + p.getFecha().format(formato) + " " + estado;
                return buscar.isEmpty() || texto.contains(buscar);
            });
        });
    }

    private void configurarSeleccion() {
        tablaProduccion.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, p) -> {
                    if (p == null) {
                        actualizarDisponibles();
                        return;
                    }

                    cmbLote.getItems().stream()
                            .filter(l -> Objects.equals(l.getId(), p.getLoteId()))
                            .findFirst().ifPresent(cmbLote::setValue);

                    dpFecha.setValue(p.getFecha());
                    txtCantidad.setText(String.valueOf(p.getCantidadTruchas()));
                    txtPesoTotal.setText(p.getPesoTotalKg().toPlainString());
                    txtObservaciones.setText(p.getObservaciones() == null ? "" : p.getObservaciones());
                    actualizarDisponibles();
                });
    }

    private void configurarPermisos() {
        boolean admin = SesionUsuario.esAdministrador();
        boolean operador = SesionUsuario.esOperador();
        boolean puedeRegistrar = admin || operador;

        btnRegistrar.setDisable(!puedeRegistrar);
        btnModificar.setDisable(!admin);
        btnAnular.setDisable(!admin);
        cmbLote.setDisable(!puedeRegistrar);
        dpFecha.setDisable(!puedeRegistrar);
        txtCantidad.setEditable(puedeRegistrar);
        txtPesoTotal.setEditable(puedeRegistrar);
        txtObservaciones.setEditable(puedeRegistrar);
    }

    private record DatosIniciales(List<Lote> lotes, List<Produccion> producciones) {}

    private void cargarDatosEnSegundoPlano() {
        Task<DatosIniciales> tarea = new Task<>() {
            @Override
            protected DatosIniciales call() throws Exception {
                return new DatosIniciales(loteService.listar(), produccionService.listar());
            }
        };

        tarea.setOnSucceeded(evento -> {
            DatosIniciales resultado = tarea.getValue();
            cmbLote.setItems(FXCollections.observableArrayList(resultado.lotes()));
            datos.setAll(resultado.producciones());
            actualizarIndicadores();
            actualizarDisponibles();
        });

        tarea.setOnFailed(evento -> {
            Throwable causa = tarea.getException();
            mostrarError("No se pudieron cargar los datos: "
                    + (causa == null ? "Error desconocido" : causa.getMessage()));
        });

        Thread hilo = new Thread(tarea, "carga-produccion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarProducciones() {
        try {
            datos.setAll(produccionService.listar());
            actualizarIndicadores();
            actualizarDisponibles();
        } catch (SQLException | SecurityException e) {
            mostrarError(e.getMessage());
        }
    }

    private void actualizarIndicadores() {
        long total = 0;
        long truchas = 0;
        BigDecimal peso = BigDecimal.ZERO;

        for (Produccion p : datos) {
            if ("REGISTRADA".equals(p.getEstado())) {
                total++;
                truchas += p.getCantidadTruchas();
                peso = peso.add(p.getPesoTotalKg());
            }
        }

        lblTotalProducciones.setText(String.valueOf(total));
        lblTotalTruchasCosechadas.setText(String.valueOf(truchas));
        lblPesoTotalProducido.setText(peso.toPlainString() + " kg");
    }

    private void actualizarDisponibles() {
        Lote lote = cmbLote.getValue();
        if (lote == null) {
            lblTruchasDisponibles.setText("Seleccione un lote");
            return;
        }

        try {
            long disponibles = produccionService.obtenerTruchasDisponibles(lote.getId());
            Produccion seleccionada = tablaProduccion.getSelectionModel().getSelectedItem();
            boolean editandoMismoLote = seleccionada != null
                    && "REGISTRADA".equals(seleccionada.getEstado())
                    && Objects.equals(seleccionada.getLoteId(), lote.getId());

            if (editandoMismoLote) {
                // Cantidad máxima al reemplazar esta cosecha (no es stock libre para una nueva).
                disponibles += seleccionada.getCantidadTruchas();
                lblTruchasDisponibles.setText(disponibles + " truchas (para modificar)");
            } else {
                lblTruchasDisponibles.setText(disponibles + " truchas");
            }
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            lblTruchasDisponibles.setText("No disponible");
            mostrarError(e.getMessage());
        }
    }

    private Produccion leerFormulario(Long id) {
        Lote lote = cmbLote.getValue();
        if (lote == null) {
            throw new IllegalArgumentException("Seleccione un lote de truchas.");
        }

        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            throw new IllegalArgumentException("Seleccione la fecha de cosecha.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de cosecha no puede ser futura.");
        }
        if (lote.getFechaIngreso() != null && fecha.isBefore(lote.getFechaIngreso())) {
            throw new IllegalArgumentException("La cosecha no puede ser anterior al ingreso del lote.");
        }

        try {
            int cantidad = Integer.parseInt(txtCantidad.getText().trim());
            BigDecimal peso = new BigDecimal(txtPesoTotal.getText().trim().replace(',', '.'));

            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad de truchas debe ser mayor que cero.");
            }
            if (peso.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El peso total debe ser mayor que cero.");
            }
            if (peso.stripTrailingZeros().scale() > 3) {
                throw new IllegalArgumentException("El peso admite como máximo tres decimales.");
            }

            String observaciones = txtObservaciones.getText() == null
                    ? "" : txtObservaciones.getText().trim();
            if (observaciones.length() > 255) {
                throw new IllegalArgumentException("Las observaciones no deben superar 255 caracteres.");
            }

            return new Produccion(id, lote.getId(), fecha, cantidad, peso,
                    "REGISTRADA", observaciones);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ingrese una cantidad entera y un peso numérico válido.");
        }
    }

    @FXML
    private void registrarProduccion() {
        if (!SesionUsuario.esAdministrador() && !SesionUsuario.esOperador()) {
            mostrarError("No tiene permiso para registrar producciones.");
            return;
        }

        // Evita usar como stock libre la cantidad que se suma solo para editar.
        if (tablaProduccion.getSelectionModel().getSelectedItem() != null) {
            mostrarError("Presione 'Limpiar formulario' antes de registrar una nueva producción.");
            return;
        }

        try {
            Produccion nueva = leerFormulario(null);
            produccionService.registrar(nueva);
            limpiarFormulario();
            cargarProducciones();
            mostrarMensaje("Producción registrada correctamente.");
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void modificarProduccion() {
        if (!SesionUsuario.esAdministrador()) {
            mostrarError("Solo el administrador puede modificar producciones.");
            return;
        }

        Produccion seleccionada = tablaProduccion.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarError("Seleccione una producción de la tabla.");
            return;
        }
        if (!"REGISTRADA".equals(seleccionada.getEstado())) {
            mostrarError("No se puede modificar una producción anulada.");
            return;
        }

        try {
            Produccion actualizada = leerFormulario(seleccionada.getId());
            produccionService.actualizar(actualizada);
            limpiarFormulario();
            cargarProducciones();
            mostrarMensaje("Producción modificada correctamente.");
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void anularProduccion() {
        if (!SesionUsuario.esAdministrador()) {
            mostrarError("Solo el administrador puede anular producciones.");
            return;
        }

        Produccion seleccionada = tablaProduccion.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarError("Seleccione una producción para anular.");
            return;
        }
        if (!"REGISTRADA".equals(seleccionada.getEstado())) {
            mostrarError("La producción ya está anulada o no está activa.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("PisciControlF");
        confirmacion.setHeaderText("Confirmar anulación");
        confirmacion.setContentText("¿Desea anular la producción seleccionada?");
        if (confirmacion.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        try {
            produccionService.anular(seleccionada.getId());
            limpiarFormulario();
            cargarProducciones();
            mostrarMensaje("Producción anulada correctamente.");
        } catch (SQLException | IllegalArgumentException | SecurityException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void limpiarFormulario() {
        tablaProduccion.getSelectionModel().clearSelection();
        cmbLote.setValue(null);
        dpFecha.setValue(LocalDate.now());
        txtCantidad.clear();
        txtPesoTotal.clear();
        txtObservaciones.clear();
        lblTruchasDisponibles.setText("Seleccione un lote");
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

    @FXML
    private void abrirVentas(ActionEvent evento) {
        navegar(
                evento,
                "gestion-ventas.fxml",
                "Gestión de Ventas"
        );
    }

}

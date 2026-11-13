
package pe.edu.upeu.piscicontrolf.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import pe.edu.upeu.piscicontrolf.model.Estanque;
import pe.edu.upeu.piscicontrolf.service.EstanqueService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

public class EstanquesController {

    // CAMPOS DEL FORMULARIO
    @FXML private TextField txtNombre;
    @FXML private TextField txtCapacidad;
    @FXML private TextField txtUbicacion;
    @FXML private TextField txtBuscar;

    // INDICADORES
    @FXML private Label lblTotalEstanques;
    @FXML private Label lblEstanquesActivos;
    @FXML private Label lblEstanquesInactivos;

    // BOTONES
    @FXML private Button btnRegistrar;
    @FXML private Button btnModificar;
    @FXML private Button btnDesactivar;

    // TABLA
    @FXML private TableView<Estanque> tablaEstanques;
    @FXML private TableColumn<Estanque, Long> colId;
    @FXML private TableColumn<Estanque, String> colNombre;
    @FXML private TableColumn<Estanque, Integer> colCapacidad;
    @FXML private TableColumn<Estanque, String> colUbicacion;
    @FXML private TableColumn<Estanque, String> colEstado;

    private final EstanqueService service =
            new EstanqueService();

    private final ObservableList<Estanque> datos =
            FXCollections.observableArrayList();

    // INICIALIZAR PANTALLA
    @FXML
    public void initialize() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getId()));

        colNombre.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getNombre()));

        colCapacidad.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getCapacidad()));

        colUbicacion.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getUbicacion()));

        colEstado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getEstado()));

        // COLORES DE ESTADO
        configurarColoresEstado();

        // BUSCADOR EN TIEMPO REAL
        FilteredList<Estanque> filtrados =
                new FilteredList<>(datos, e -> true);

        tablaEstanques.setItems(filtrados);

        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> {

                    String busqueda = nuevo == null
                            ? ""
                            : nuevo.trim()
                            .toLowerCase(Locale.ROOT);

                    filtrados.setPredicate(e -> {

                        if (busqueda.isEmpty()) {
                            return true;
                        }

                        String nombre = e.getNombre() == null
                                ? ""
                                : e.getNombre()
                                .toLowerCase(Locale.ROOT);

                        String ubicacion = e.getUbicacion() == null
                                ? ""
                                : e.getUbicacion()
                                .toLowerCase(Locale.ROOT);

                        return nombre.contains(busqueda)
                                || ubicacion.contains(busqueda);
                    });
                });

        // SELECCIONAR ESTANQUE
        tablaEstanques.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, anterior, seleccionado) -> {

                            if (seleccionado != null) {

                                txtNombre.setText(
                                        seleccionado.getNombre());

                                txtCapacidad.setText(
                                        String.valueOf(
                                                seleccionado.getCapacidad()));

                                txtUbicacion.setText(
                                        seleccionado.getUbicacion());
                            }
                        });

        // PERMISOS DE LA PANTALLA
        boolean esAdmin =
                SesionUsuario.esAdministrador();

        btnRegistrar.setDisable(!esAdmin);
        btnModificar.setDisable(!esAdmin);
        btnDesactivar.setDisable(!esAdmin);

        txtNombre.setEditable(esAdmin);
        txtCapacidad.setEditable(esAdmin);
        txtUbicacion.setEditable(esAdmin);

        // CARGAR DATOS DE H2
        cargarEstanques();
    }

    // MOSTRAR ESTADOS CON COLORES
    private void configurarColoresEstado() {

        colEstado.setCellFactory(col ->
                new TableCell<Estanque, String>() {

                    private final Label etiqueta = new Label();

                    @Override
                    protected void updateItem(
                            String estado, boolean vacio) {

                        super.updateItem(estado, vacio);

                        setText(null);

                        if (vacio || estado == null) {
                            setGraphic(null);
                            return;
                        }

                        etiqueta.setText(estado);

                        etiqueta.getStyleClass().removeAll(
                                "estado-activo",
                                "estado-inactivo",
                                "estado-desconocido");

                        if ("ACTIVO".equalsIgnoreCase(estado)) {

                            etiqueta.getStyleClass()
                                    .add("estado-activo");

                        } else if ("INACTIVO"
                                .equalsIgnoreCase(estado)) {

                            etiqueta.getStyleClass()
                                    .add("estado-inactivo");

                        } else {

                            etiqueta.getStyleClass()
                                    .add("estado-desconocido");
                        }

                        if (!etiqueta.getStyleClass()
                                .contains("estado-etiqueta")) {

                            etiqueta.getStyleClass()
                                    .add("estado-etiqueta");
                        }

                        setGraphic(etiqueta);
                    }
                });
    }

    // COMPROBAR PERMISO DE ADMINISTRADOR
    private boolean verificarAdministrador() {

        if (!SesionUsuario.esAdministrador()) {

            mostrarError(
                    "Solo el administrador puede modificar estanques.");

            return false;
        }

        return true;
    }

    // LEER FORMULARIO
    private Estanque leerFormulario(
            Long id, String estado) {

        String nombre =
                txtNombre.getText().trim();

        String ubicacion =
                txtUbicacion.getText().trim();

        int capacidad;

        try {
            capacidad = Integer.parseInt(
                    txtCapacidad.getText().trim());

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Ingrese una capacidad numérica válida.");
        }

        if (nombre.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ingrese el nombre del estanque.");
        }

        if (capacidad <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad debe ser mayor que cero.");
        }

        if (ubicacion.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ingrese la ubicación del estanque.");
        }

        return new Estanque(
                id,
                nombre,
                capacidad,
                ubicacion,
                estado);
    }

    // REGISTRAR ESTANQUE
    @FXML
    private void registrarEstanque() {

        if (!verificarAdministrador()) {
            return;
        }

        try {
            Estanque estanque =
                    leerFormulario(null, "ACTIVO");

            service.registrar(estanque);

            cargarEstanques();
            limpiarFormulario();

            mostrarMensaje(
                    "Registro exitoso",
                    "El estanque fue registrado correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            mostrarError(e.getMessage());
        }
    }

    // MODIFICAR ESTANQUE
    @FXML
    private void modificarEstanque() {

        if (!verificarAdministrador()) {
            return;
        }

        Estanque seleccionado =
                tablaEstanques.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {

            mostrarError(
                    "Seleccione un estanque de la tabla.");

            return;
        }

        try {
            // CONSERVAR EL ESTADO DEL ESTANQUE
            Estanque modificado = leerFormulario(
                    seleccionado.getId(),
                    seleccionado.getEstado());

            service.actualizar(modificado);

            cargarEstanques();
            limpiarFormulario();

            mostrarMensaje(
                    "Actualización exitosa",
                    "El estanque fue modificado correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            mostrarError(e.getMessage());
        }
    }

    // DESACTIVAR ESTANQUE
    @FXML
    private void desactivarEstanque() {

        if (!verificarAdministrador()) {
            return;
        }

        Estanque seleccionado =
                tablaEstanques.getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {

            mostrarError(
                    "Seleccione un estanque de la tabla.");

            return;
        }

        if ("INACTIVO".equalsIgnoreCase(
                seleccionado.getEstado())) {

            mostrarError(
                    "El estanque ya se encuentra inactivo.");

            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desea desactivar el estanque seleccionado?",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle("PisciControlF");
        confirmacion.setHeaderText(
                "Confirmar desactivación");

        if (confirmacion.showAndWait()
                .orElse(ButtonType.NO) != ButtonType.YES) {

            return;
        }

        try {
            service.desactivar(seleccionado.getId());

            cargarEstanques();
            limpiarFormulario();

            mostrarMensaje(
                    "Estanque desactivado",
                    "El estado cambió a INACTIVO.");

        } catch (SQLException e) {

            mostrarError(e.getMessage());
        }
    }

    // LIMPIAR FORMULARIO
    @FXML
    private void limpiarFormulario() {

        tablaEstanques.getSelectionModel()
                .clearSelection();

        txtNombre.clear();
        txtCapacidad.clear();
        txtUbicacion.clear();

        txtNombre.requestFocus();
    }

    // CARGAR ESTANQUES DESDE H2
    private void cargarEstanques() {

        try {
            datos.setAll(service.listar());

            long activos = datos.stream()
                    .filter(e ->
                            "ACTIVO".equalsIgnoreCase(
                                    e.getEstado()))
                    .count();

            long inactivos = datos.stream()
                    .filter(e ->
                            "INACTIVO".equalsIgnoreCase(
                                    e.getEstado()))
                    .count();

            lblTotalEstanques.setText(
                    String.valueOf(datos.size()));

            lblEstanquesActivos.setText(
                    String.valueOf(activos));

            lblEstanquesInactivos.setText(
                    String.valueOf(inactivos));

        } catch (SQLException e) {

            mostrarError(
                    "No se pudieron cargar los estanques: "
                            + e.getMessage());
        }
    }

    // MENSAJES
    private void mostrarMensaje(
            String titulo, String mensaje) {

        Alert alerta =
                new Alert(Alert.AlertType.INFORMATION);

        alerta.setTitle(titulo);
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

    // VOLVER AL PANEL SEGÚN EL ROL
    @FXML
    private void volverAlPanel(ActionEvent evento) {

        String archivo;
        String titulo;

        if (SesionUsuario.esAdministrador()) {

            archivo = "panel-administrador.fxml";
            titulo = "Panel del Administrador";

        } else if (SesionUsuario.esOperador()) {

            archivo = "panel-operador.fxml";
            titulo = "Panel del Operador";

        } else {

            archivo = "login.fxml";
            titulo = "Iniciar Sesión";
        }

        navegar(evento, archivo, titulo);
    }

    // ABRIR LOTES
    @FXML
    private void abrirLotes(ActionEvent evento) {

        navegar(
                evento,
                "gestion-lotes.fxml",
                "Gestión de Lotes");
    }

    // ABRIR ALIMENTACIÓN
    @FXML
    private void abrirAlimentacion(ActionEvent evento) {

        navegar(
                evento,
                "gestion-alimentacion.fxml",
                "Gestión de Alimentación");
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

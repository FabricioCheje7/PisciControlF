package pe.edu.upeu.piscicontrolf.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.scene.control.*;
import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;
import pe.edu.upeu.piscicontrolf.config.SesionUsuario;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Controlador de Reportes Generales de PisciControlF. */
public class ReportesController {

    @FXML private Label lblTotalEstanques;
    @FXML private Label lblTotalLotes;
    @FXML private Label lblPesoProduccion;
    @FXML private Label lblIngresos;
    @FXML private Label lblDescripcionReporte;
    @FXML private Label lblTotalFilas;

    @FXML private ComboBox<String> cmbTipoReporte;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtBuscar;
    @FXML private Button btnGenerar;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnExportarExcel;
    @FXML private TableView<Map<String, String>> tablaReporte;

    private final ObservableList<Map<String, String>> filas =
            FXCollections.observableArrayList();
    private FilteredList<Map<String, String>> filasFiltradas;
    private List<String> columnasReporte = List.of();
    private String tipoReporteGenerado = "";
    private String descripcionReporteGenerado = "";
    private boolean reporteGenerado = false;
    private final DateTimeFormatter formatoFecha =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private record Definicion(String tabla, String campoFecha, String orden) {}
    private record ResultadoReporte(List<String> columnas,
                                    List<Map<String, String>> filas) {}
    private record Indicadores(long estanques, long lotes,
                              BigDecimal produccionKg, BigDecimal ingresos) {}

    @FXML
    public void initialize() {
        cmbTipoReporte.setItems(FXCollections.observableArrayList(
                "Estanques", "Lotes", "Alimentación", "Crecimiento",
                "Producción", "Ventas", "Calidad del Agua",
                "Mortalidad y Sanidad"));

        filasFiltradas = new FilteredList<>(filas, fila -> true);
        tablaReporte.setItems(filasFiltradas);
        tablaReporte.setPlaceholder(new Label(
                "Selecciona un tipo de reporte y pulsa Generar reporte."));

        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            String consulta = nuevo == null ? "" :
                    nuevo.trim().toLowerCase(Locale.ROOT);
            filasFiltradas.setPredicate(fila ->
                    consulta.isEmpty() || fila.values().stream()
                            .anyMatch(valor -> valor != null &&
                                    valor.toLowerCase(Locale.ROOT).contains(consulta)));
            actualizarContador();
            actualizarBotonesExportacion();
        });

        cmbTipoReporte.valueProperty().addListener((obs, anterior, tipo) -> {
            // Estanques no tiene una columna de fecha en el esquema actual.
            boolean sinFecha = "Estanques".equals(tipo);
            if (sinFecha) {
                dpDesde.setValue(null);
                dpHasta.setValue(null);
            }
            dpDesde.setDisable(sinFecha);
            dpHasta.setDisable(sinFecha);
            invalidarReporte();
        });

        // Asociar botones sin depender de onAction en el FXML.
        btnExportarPdf.setOnAction(e -> exportarPdf());
        btnExportarExcel.setOnAction(e -> exportarExcel());
        btnExportarPdf.setDisable(true);
        btnExportarExcel.setDisable(true);
        dpDesde.valueProperty().addListener((obs, anterior, nuevo) -> invalidarReporte());
        dpHasta.valueProperty().addListener((obs, anterior, nuevo) -> invalidarReporte());

        if (!SesionUsuario.haySesion()) {
            btnGenerar.setDisable(true);
            lblDescripcionReporte.setText("Inicia sesión para consultar reportes.");
            return;
        }

        cargarIndicadores();
    }

    /** Lee los indicadores sin bloquear la interfaz JavaFX. */
    private void cargarIndicadores() {
        Task<Indicadores> tarea = new Task<>() {
            @Override
            protected Indicadores call() throws SQLException {
                try (Connection con = DatabaseConfig.getConnection()) {
                    long estanques = consultarEntero(con,
                            "SELECT COUNT(*) FROM estanques");
                    long lotes = consultarEntero(con,
                            "SELECT COUNT(*) FROM lotes");
                    BigDecimal produccion = consultarDecimal(con,
                            "SELECT COALESCE(SUM(peso_total_kg), 0) " +
                            "FROM produccion WHERE estado = 'REGISTRADA'");
                    BigDecimal ingresos = consultarDecimal(con,
                            "SELECT COALESCE(SUM(total), 0) " +
                            "FROM ventas WHERE estado = 'REGISTRADA'");
                    return new Indicadores(estanques, lotes, produccion, ingresos);
                }
            }
        };

        tarea.setOnSucceeded(e -> {
            Indicadores i = tarea.getValue();
            lblTotalEstanques.setText(String.valueOf(i.estanques()));
            lblTotalLotes.setText(String.valueOf(i.lotes()));
            lblPesoProduccion.setText(i.produccionKg().toPlainString() + " kg");
            lblIngresos.setText("S/ " + i.ingresos().setScale(
                    2, java.math.RoundingMode.HALF_UP).toPlainString());
        });
        tarea.setOnFailed(e -> mostrarError(
                "No se pudieron cargar los indicadores: " +
                        tarea.getException().getMessage()));
        iniciarTarea(tarea, "indicadores-reportes");
    }

    private long consultarEntero(Connection con, String sql)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private BigDecimal consultarDecimal(Connection con, String sql)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return BigDecimal.ZERO;
            BigDecimal resultado = rs.getBigDecimal(1);
            return resultado == null ? BigDecimal.ZERO : resultado;
        }
    }

    @FXML
    private void generarReporte() {
        if (!SesionUsuario.haySesion()) {
            mostrarError("Debes iniciar sesión para consultar reportes.");
            return;
        }

        String tipo = cmbTipoReporte.getValue();
        if (tipo == null) {
            mostrarError("Selecciona un tipo de reporte.");
            return;
        }

        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            mostrarError("La fecha Desde no puede ser posterior a Hasta.");
            return;
        }

        // La definición procede únicamente de opciones fijas y conocidas.
        Definicion definicion = obtenerDefinicion(tipo);
        btnGenerar.setDisable(true);
        reporteGenerado = false;
        actualizarBotonesExportacion();
        lblDescripcionReporte.setText("Generando reporte de " + tipo + "...");

        Task<ResultadoReporte> tarea = new Task<>() {
            @Override
            protected ResultadoReporte call() throws SQLException {
                return consultarReporte(definicion, desde, hasta);
            }
        };

        tarea.setOnSucceeded(e -> {
            ResultadoReporte resultado = tarea.getValue();
            construirColumnas(resultado.columnas());
            filas.setAll(resultado.filas());
            txtBuscar.clear();
            columnasReporte = List.copyOf(resultado.columnas());
            tipoReporteGenerado = tipo;
            descripcionReporteGenerado = descripcion(tipo, desde, hasta);
            reporteGenerado = true;
            actualizarContador();
            lblDescripcionReporte.setText(descripcionReporteGenerado);
            btnGenerar.setDisable(false);
            actualizarBotonesExportacion();
        });
        tarea.setOnFailed(e -> {
            btnGenerar.setDisable(false);
            invalidarReporte();
            lblDescripcionReporte.setText("No se pudo generar el reporte.");
            mostrarError("Error al consultar H2: " +
                    tarea.getException().getMessage());
        });
        iniciarTarea(tarea, "consulta-reportes");
    }

    private Definicion obtenerDefinicion(String tipo) {
        return switch (tipo) {
            case "Estanques" -> new Definicion("estanques", null, "id DESC");
            case "Lotes" -> new Definicion("lotes", "fecha_ingreso",
                    "fecha_ingreso DESC, id DESC");
            case "Alimentación" -> new Definicion("alimentacion", "fecha",
                    "fecha DESC, id DESC");
            case "Crecimiento" -> new Definicion("crecimiento", "fecha",
                    "fecha DESC, id DESC");
            case "Producción" -> new Definicion("produccion", "fecha",
                    "fecha DESC, id DESC");
            case "Ventas" -> new Definicion("ventas", "fecha",
                    "fecha DESC, id DESC");
            case "Calidad del Agua" -> new Definicion("calidad_agua", "fecha",
                    "fecha DESC, id DESC");
            case "Mortalidad y Sanidad" -> new Definicion("mortalidad", "fecha",
                    "fecha DESC, id DESC");
            default -> throw new IllegalArgumentException(
                    "El tipo de reporte no está disponible.");
        };
    }

    private ResultadoReporte consultarReporte(Definicion d,
                                              LocalDate desde,
                                              LocalDate hasta)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + d.tabla());
        List<LocalDate> parametros = new ArrayList<>();
        if (d.campoFecha() != null) {
            if (desde != null) {
                sql.append(" WHERE ").append(d.campoFecha()).append(" >= ?");
                parametros.add(desde);
            }
            if (hasta != null) {
                sql.append(parametros.isEmpty() ? " WHERE " : " AND ")
                        .append(d.campoFecha()).append(" <= ?");
                parametros.add(hasta);
            }
        }
        sql.append(" ORDER BY ").append(d.orden());

        List<Map<String, String>> resultado = new ArrayList<>();
        List<String> columnas = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setDate(i + 1, Date.valueOf(parametros.get(i)));
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    columnas.add(meta.getColumnLabel(i));
                }
                while (rs.next()) {
                    Map<String, String> fila = new LinkedHashMap<>();
                    for (int i = 1; i <= columnas.size(); i++) {
                        fila.put(columnas.get(i - 1), textoCelda(rs.getObject(i)));
                    }
                    resultado.add(fila);
                }
            }
        }
        return new ResultadoReporte(columnas, resultado);
    }

    private String textoCelda(Object valor) {
        if (valor == null) return "";
        if (valor instanceof Date fecha) {
            return fecha.toLocalDate().format(formatoFecha);
        }
        if (valor instanceof LocalDate fecha) {
            return fecha.format(formatoFecha);
        }
        if (valor instanceof BigDecimal numero) {
            return numero.toPlainString();
        }
        return String.valueOf(valor);
    }

    private void construirColumnas(List<String> claves) {
        tablaReporte.getColumns().clear();
        for (String clave : claves) {
            TableColumn<Map<String, String>, String> columna =
                    new TableColumn<>(tituloColumna(clave));
            columna.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                    c.getValue().getOrDefault(clave, "")));
            columna.setPrefWidth(anchoColumna(clave));
            tablaReporte.getColumns().add(columna);
        }
    }

    private String tituloColumna(String nombre) {
        return switch (nombre.toUpperCase(Locale.ROOT)) {
            case "ID" -> "ID";
            case "LOTE_ID" -> "Lote";
            case "ESTANQUE_ID" -> "Estanque";
            case "PRODUCCION_ID" -> "Producción";
            case "CANTIDAD_TRUCHAS" -> "Truchas";
            case "CANTIDAD_MUESTRA" -> "Muestra";
            case "CANTIDAD_KG" -> "Cantidad (kg)";
            case "PESO_TOTAL_KG" -> "Peso total (kg)";
            case "PESO_PROMEDIO_G" -> "Peso medio (g)";
            case "TALLA_PROMEDIO_CM" -> "Talla media (cm)";
            case "PRECIO_UNITARIO_KG" -> "Precio/kg (S/)";
            case "TOTAL" -> "Total (S/)";
            case "FECHA_INGRESO" -> "Fecha de ingreso";
            case "TIPO_ALIMENTO" -> "Alimento";
            case "TEMPERATURA_C" -> "Temperatura (°C)";
            case "PH" -> "pH";
            case "OXIGENO_MG_L" -> "Oxígeno (mg/L)";
            case "TIPO_INCIDENCIA" -> "Incidencia";
            case "CANTIDAD_MUERTAS" -> "Truchas muertas";
            case "CAUSA" -> "Posible causa";
            case "ACCIONES" -> "Acciones realizadas";
            default -> {
                String limpio = nombre.toLowerCase(Locale.ROOT)
                        .replace('_', ' ');
                yield Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
            }
        };
    }

    private double anchoColumna(String clave) {
        String nombre = clave.toUpperCase(Locale.ROOT);
        if (nombre.equals("ID")) return 60;
        if (nombre.equals("OBSERVACIONES") || nombre.equals("ACCIONES")) return 230;
        if (nombre.equals("UBICACION") || nombre.equals("CAUSA")) return 180;
        if (nombre.equals("CLIENTE") || nombre.equals("NOMBRE")) return 170;
        if (nombre.equals("TIPO_INCIDENCIA") || nombre.equals("CANTIDAD_MUERTAS")) return 145;
        return 130;
    }

    private String descripcion(String tipo, LocalDate desde, LocalDate hasta) {
        if ("Estanques".equals(tipo) || (desde == null && hasta == null)) {
            return "Reporte de " + tipo + " · Todos los registros";
        }
        String inicio = desde == null ? "inicio" : desde.format(formatoFecha);
        String fin = hasta == null ? "hoy" : hasta.format(formatoFecha);
        return "Reporte de " + tipo + " · Desde " + inicio + " hasta " + fin;
    }

    private void actualizarContador() {
        int n = filasFiltradas.size();
        lblTotalFilas.setText(n + (n == 1 ? " registro" : " registros"));
    }

    @FXML
    private void limpiarFiltros() {
        cmbTipoReporte.setValue(null);
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        dpDesde.setDisable(false);
        dpHasta.setDisable(false);
        txtBuscar.clear();
        invalidarReporte();
        lblTotalFilas.setText("0 registros");
        lblDescripcionReporte.setText(
                "Selecciona un módulo y pulsa Generar reporte.");
    }

    private void iniciarTarea(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
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
    @FXML private void abrirCalidadAgua(ActionEvent e) {
        navegar(e, "gestion-calidad-agua.fxml", "Gestión de Calidad del Agua");
    }
    @FXML private void abrirMortalidad(ActionEvent e) {
        navegar(e, "gestion-mortalidad.fxml", "Gestión de Mortalidad y Sanidad");
    }
    @FXML private void abrirProduccion(ActionEvent e) {
        navegar(e, "gestion-produccion.fxml", "Gestión de Producción");
    }
    @FXML private void abrirVentas(ActionEvent e) {
        navegar(e, "gestion-ventas.fxml", "Gestión de Ventas");
    }

    private void navegar(ActionEvent evento, String archivo, String titulo) {
        if (!SesionUsuario.haySesion() && !"login.fxml".equals(archivo)) {
            mostrarError("Inicia sesión para continuar.");
            return;
        }
        try {
            Navegacion.abrir(evento, archivo, titulo);
        } catch (IOException e) {
            mostrarError("No se pudo abrir la pantalla: " + e.getMessage());
        }
    }

    // ==============================================================
    // EXPORTACION DE LOS DATOS DEL REPORTE ACTUAL
    // ==============================================================

    private void actualizarBotonesExportacion() {
        boolean habilitar = SesionUsuario.haySesion()
                && reporteGenerado
                && !columnasReporte.isEmpty()
                && filasFiltradas != null
                && !filasFiltradas.isEmpty();
        btnExportarPdf.setDisable(!habilitar);
        btnExportarExcel.setDisable(!habilitar);
    }

    /** Evita exportar un reporte anterior si cambian el tipo o las fechas. */
    private void invalidarReporte() {
        reporteGenerado = false;
        columnasReporte = List.of();
        filas.clear();
        tablaReporte.getColumns().clear();
        actualizarContador();
        actualizarBotonesExportacion();
    }

    /** Copia los datos que el usuario realmente tiene visibles tras buscar. */
    private List<Map<String, String>> copiarFilasVisibles() {
        List<Map<String, String>> copia = new ArrayList<>();
        for (Map<String, String> fila : filasFiltradas) {
            copia.add(new LinkedHashMap<>(fila));
        }
        return copia;
    }

    @FXML
    private void exportarPdf() {
        exportarArchivo(false);
    }

    @FXML
    private void exportarExcel() {
        exportarArchivo(true);
    }

    private void exportarArchivo(boolean excel) {
        if (!SesionUsuario.haySesion() || !reporteGenerado
                || columnasReporte.isEmpty() || filasFiltradas.isEmpty()) {
            mostrarError("Primero genera un reporte con registros para exportar.");
            return;
        }

        String extension = excel ? ".xlsx" : ".pdf";
        FileChooser selector = new FileChooser();
        selector.setTitle(excel ? "Guardar reporte Excel" : "Guardar reporte PDF");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                excel ? "Archivo Excel (*.xlsx)" : "Archivo PDF (*.pdf)",
                excel ? "*.xlsx" : "*.pdf"));
        String nombre = "PisciControlF_" + tipoReporteGenerado
                .replaceAll("[^a-zA-Z0-9]", "_") + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        selector.setInitialFileName(nombre + extension);

        File destino = selector.showSaveDialog(tablaReporte.getScene().getWindow());
        if (destino == null) return;
        if (!destino.getName().toLowerCase(Locale.ROOT).endsWith(extension)) {
            destino = new File(destino.getParentFile(), destino.getName() + extension);
        }

        final File archivoElegido = destino;
        final String tipo = tipoReporteGenerado;
        final String descripcionActual = descripcionReporteGenerado;
        final List<String> columnas = List.copyOf(columnasReporte);
        final List<Map<String, String>> filasExportar = copiarFilasVisibles();

        btnExportarPdf.setDisable(true);
        btnExportarExcel.setDisable(true);

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (excel) {
                    crearExcel(archivoElegido, tipo, descripcionActual,
                            columnas, filasExportar);
                } else {
                    crearPdf(archivoElegido, tipo, descripcionActual,
                            columnas, filasExportar);
                }
                return null;
            }
        };
        tarea.setOnSucceeded(e -> {
            actualizarBotonesExportacion();
            mostrarMensaje("Reporte exportado correctamente.\n" +
                    archivoElegido.getAbsolutePath());
        });
        tarea.setOnFailed(e -> {
            actualizarBotonesExportacion();
            mostrarError("No se pudo exportar el archivo: "
                    + tarea.getException().getMessage());
        });
        iniciarTarea(tarea, excel ? "exportacion-excel" : "exportacion-pdf");
    }

    private void crearExcel(File destino, String tipo, String descripcion,
                            List<String> columnas,
                            List<Map<String, String>> registros) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            XSSFSheet hoja = libro.createSheet(tipo);
            int ultimaColumna = columnas.size() - 1;

            CellStyle estiloTitulo = libro.createCellStyle();
            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 18);
            fuenteTitulo.setColor(IndexedColors.DARK_BLUE.getIndex());
            estiloTitulo.setFont(fuenteTitulo);

            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font fuenteCabecera = libro.createFont();
            fuenteCabecera.setBold(true);
            fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());
            estiloCabecera.setFont(fuenteCabecera);
            estiloCabecera.setAlignment(HorizontalAlignment.CENTER);
            estiloCabecera.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle estiloPar = libro.createCellStyle();
            estiloPar.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            estiloPar.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle estiloFecha = libro.createCellStyle();
            estiloFecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle estiloNumero = libro.createCellStyle();
            estiloNumero.setDataFormat(libro.createDataFormat().getFormat("#,##0.000"));
            CellStyle estiloEntero = libro.createCellStyle();
            estiloEntero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            CellStyle estiloDinero = libro.createCellStyle();
            estiloDinero.setDataFormat(libro.createDataFormat().getFormat("\"S/ \"#,##0.00"));

            Row tituloFila = hoja.createRow(0);
            Cell tituloCelda = tituloFila.createCell(0);
            tituloCelda.setCellValue("PisciControlF | Reporte de " + tipo);
            tituloCelda.setCellStyle(estiloTitulo);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, ultimaColumna));
            hoja.createRow(1).createCell(0).setCellValue(descripcion);
            hoja.createRow(2).createCell(0).setCellValue(
                    "Generado: " + LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            hoja.createRow(3).createCell(0).setCellValue(
                    "Registros exportados: " + registros.size());

            int cabeceraIdx = 5;
            Row encabezado = hoja.createRow(cabeceraIdx);
            encabezado.setHeightInPoints(27);
            for (int i = 0; i < columnas.size(); i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(tituloColumna(columnas.get(i)));
                celda.setCellStyle(estiloCabecera);
                int ancho = Math.max(15, Math.min(43,
                        (int) Math.round(anchoColumna(columnas.get(i)) / 7.0 + 7)));
                hoja.setColumnWidth(i, ancho * 256);
            }

            for (int r = 0; r < registros.size(); r++) {
                Row fila = hoja.createRow(cabeceraIdx + 1 + r);
                Map<String, String> registro = registros.get(r);
                for (int c = 0; c < columnas.size(); c++) {
                    String clave = columnas.get(c);
                    String valor = registro.getOrDefault(clave, "");
                    Cell celda = fila.createCell(c);
                    if (valor == null || valor.isBlank()) {
                        celda.setCellValue("");
                    } else if (esFecha(clave)) {
                        try {
                            LocalDate fecha = LocalDate.parse(valor, formatoFecha);
                            celda.setCellValue(java.util.Date.from(
                                    fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                            celda.setCellStyle(estiloFecha);
                        } catch (Exception ex) {
                            celda.setCellValue(valor);
                        }
                    } else if (esNumero(clave)) {
                        try {
                            celda.setCellValue(new BigDecimal(valor).doubleValue());
                            celda.setCellStyle(esDinero(clave) ? estiloDinero :
                                    esEntero(clave) ? estiloEntero : estiloNumero);
                        } catch (NumberFormatException ex) {
                            celda.setCellValue(valor);
                        }
                    } else {
                        celda.setCellValue(valor);
                    }
                    if (r % 2 == 1 && celda.getCellStyle().getIndex() == 0) {
                        celda.setCellStyle(estiloPar);
                    }
                }
            }
            hoja.createFreezePane(0, cabeceraIdx + 1);
            hoja.setAutoFilter(new CellRangeAddress(cabeceraIdx,
                    cabeceraIdx + registros.size(), 0, ultimaColumna));
            try (OutputStream salida = Files.newOutputStream(destino.toPath())) {
                libro.write(salida);
            }
        }
    }

    private boolean esFecha(String nombre) {
        String clave = nombre.toUpperCase(Locale.ROOT);
        return clave.equals("FECHA") || clave.equals("FECHA_INGRESO");
    }

    private boolean esDinero(String nombre) {
        String clave = nombre.toUpperCase(Locale.ROOT);
        return clave.equals("PRECIO_UNITARIO_KG") || clave.equals("TOTAL");
    }

    private boolean esEntero(String nombre) {
        String clave = nombre.toUpperCase(Locale.ROOT);
        return clave.equals("ID") || clave.endsWith("_ID")
                || clave.equals("CAPACIDAD")
                || clave.equals("CANTIDAD_TRUCHAS")
                || clave.equals("CANTIDAD_MUESTRA")
                || clave.equals("CANTIDAD_MUERTAS");
    }

    private boolean esNumero(String nombre) {
        String clave = nombre.toUpperCase(Locale.ROOT);
        return esDinero(clave) || esEntero(clave)
                || clave.equals("CANTIDAD_KG")
                || clave.equals("PESO_TOTAL_KG")
                || clave.equals("PESO_PROMEDIO_G")
                || clave.equals("TALLA_PROMEDIO_CM")
                || clave.equals("TEMPERATURA_C")
                || clave.equals("PH")
                || clave.equals("OXIGENO_MG_L");
    }

    private void crearPdf(File destino, String tipo, String descripcion,
                          List<String> columnas,
                          List<Map<String, String>> registros) throws IOException {
        PDRectangle tamanoPagina = new PDRectangle(
                PDRectangle.A3.getHeight(), PDRectangle.A3.getWidth());
        float margen = 35f;
        float anchoDisponible = tamanoPagina.getWidth() - 2 * margen;
        float sumaPesos = 0f;
        float[] anchos = new float[columnas.size()];
        for (int i = 0; i < columnas.size(); i++) {
            anchos[i] = (float) anchoColumna(columnas.get(i));
            sumaPesos += anchos[i];
        }
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = anchos[i] * anchoDisponible / sumaPesos;
        }

        PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        try (PDDocument documento = new PDDocument()) {
            documento.getDocumentInformation().setTitle("PisciControlF | " + tipo);
            PDPageContentStream lienzo = null;
            try {
                PDPage pagina = new PDPage(tamanoPagina);
                documento.addPage(pagina);
                lienzo = new PDPageContentStream(documento, pagina);
                float y = encabezadoPdf(lienzo, tamanoPagina, tipo,
                        descripcion, registros.size(), columnas, anchos,
                        margen, normal, negrita);
                int numeroFila = 0;
                for (Map<String, String> fila : registros) {
                    List<List<String>> lineasPorColumna = new ArrayList<>();
                    int maximo = 1;
                    for (int i = 0; i < columnas.size(); i++) {
                        List<String> lineas = partirTextoPdf(
                                fila.getOrDefault(columnas.get(i), ""),
                                normal, 8f, Math.max(12, anchos[i] - 10));
                        lineasPorColumna.add(lineas);
                        maximo = Math.max(maximo, lineas.size());
                    }
                    float alto = Math.max(25f, 11f * maximo + 10f);
                    if (y - alto < margen + 15) {
                        lienzo.close();
                        pagina = new PDPage(tamanoPagina);
                        documento.addPage(pagina);
                        lienzo = new PDPageContentStream(documento, pagina);
                        y = encabezadoPdf(lienzo, tamanoPagina, tipo,
                                descripcion, registros.size(), columnas, anchos,
                                margen, normal, negrita);
                    }
                    if (numeroFila % 2 == 0) {
                        lienzo.setNonStrokingColor(244f / 255f, 248f / 255f, 252f / 255f);
                        lienzo.addRect(margen, y - alto, anchoDisponible, alto);
                        lienzo.fill();
                    }
                    float x = margen;
                    for (int c = 0; c < columnas.size(); c++) {
                        float yy = y - 12;
                        for (String linea : lineasPorColumna.get(c)) {
                            escribirPdf(lienzo, normal, 8f,
                                    x + 5, yy, linea, 40, 60, 83);
                            yy -= 11;
                        }
                        x += anchos[c];
                    }
                    y -= alto;
                    lienzo.setStrokingColor(224f / 255f, 232f / 255f, 241f / 255f);
                    lienzo.moveTo(margen, y);
                    lienzo.lineTo(margen + anchoDisponible, y);
                    lienzo.stroke();
                    numeroFila++;
                }
                lienzo.close();
                lienzo = null;
                documento.save(destino);
            } finally {
                if (lienzo != null) lienzo.close();
            }
        }
    }

    private float encabezadoPdf(PDPageContentStream lienzo,
                               PDRectangle pagina, String tipo, String descripcion,
                               int cantidad, List<String> columnas, float[] anchos,
                               float margen, PDFont normal,
                               PDFont negrita) throws IOException {
        float alto = pagina.getHeight();
        lienzo.setNonStrokingColor(16f / 255f, 43f / 255f, 80f / 255f);
        lienzo.addRect(0, alto - 75, pagina.getWidth(), 75);
        lienzo.fill();
        escribirPdf(lienzo, negrita, 19f, margen, alto - 37,
                "PisciControlF | Reporte de " + tipo, 255, 255, 255);
        escribirPdf(lienzo, normal, 10f, margen, alto - 56,
                "Sistema de Gestion de Piscigranjas", 213, 229, 245);

        escribirPdf(lienzo, normal, 10f, margen, alto - 103,
                descripcion, 37, 67, 102);
        escribirPdf(lienzo, normal, 9f, margen, alto - 122,
                "Generado el " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + " | Registros: " + cantidad,
                90, 111, 138);

        float y = alto - 151;
        lienzo.setNonStrokingColor(27f / 255f, 80f / 255f, 132f / 255f);
        lienzo.addRect(margen, y - 34, pagina.getWidth() - 2 * margen, 34);
        lienzo.fill();
        float x = margen;
        for (int i = 0; i < columnas.size(); i++) {
            List<String> partes = partirTextoPdf(
                    tituloColumna(columnas.get(i)), negrita,
                    8f, Math.max(12, anchos[i] - 10));
            float yy = y - 13;
            for (int j = 0; j < Math.min(2, partes.size()); j++) {
                escribirPdf(lienzo, negrita, 8f, x + 5, yy,
                        partes.get(j), 255, 255, 255);
                yy -= 11;
            }
            x += anchos[i];
        }
        return y - 34;
    }

    /** Divide texto largo para que los campos no se superpongan en el PDF. */
    private List<String> partirTextoPdf(String texto, PDFont fuente,
                                       float puntos, float ancho) throws IOException {
        String limpio = textoParaPdf(texto == null ? "" : texto);
        List<String> resultado = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (char ch : limpio.toCharArray()) {
            if (ch == '\n') {
                resultado.add(actual.toString());
                actual.setLength(0);
                continue;
            }
            String prueba = actual.toString() + ch;
            if (!actual.isEmpty() &&
                    fuente.getStringWidth(prueba) * puntos / 1000f > ancho) {
                resultado.add(actual.toString());
                actual.setLength(0);
                if (ch != ' ') actual.append(ch);
            } else {
                actual.append(ch);
            }
        }
        resultado.add(actual.toString());
        return resultado;
    }

    /** Las fuentes PDF estándar admiten texto latino, no todos los símbolos Unicode. */
    private String textoParaPdf(String texto) {
        StringBuilder resultado = new StringBuilder();
        for (char ch : texto.toCharArray()) {
            if (ch == '\n') {
                resultado.append('\n');
            } else if ((ch >= 32 && ch <= 126)
                    || "áéíóúÁÉÍÓÚñÑüÜ¿¡°".indexOf(ch) >= 0) {
                resultado.append(ch);
            } else {
                resultado.append('?');
            }
        }
        return resultado.toString();
    }

    private void escribirPdf(PDPageContentStream lienzo, PDFont fuente,
                            float tamano, float x, float y, String texto,
                            int r, int g, int b) throws IOException {
        lienzo.beginText();
        lienzo.setFont(fuente, tamano);
        lienzo.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
        lienzo.newLineAtOffset(x, y);
        lienzo.showText(textoParaPdf(texto).replace('\n', ' '));
        lienzo.endText();
    }

    private void mostrarMensaje(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("PisciControlF - Reportes");
        alerta.setHeaderText("Exportación completada");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("PisciControlF - Reportes");
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

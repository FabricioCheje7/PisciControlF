
package pe.edu.upeu.piscicontrolf.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import pe.edu.upeu.piscicontrolf.model.Estanque;
import pe.edu.upeu.piscicontrolf.model.Lote;
import pe.edu.upeu.piscicontrolf.service.EstanqueService;
import pe.edu.upeu.piscicontrolf.service.LoteService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PanelPrincipalController {

    @FXML private Label lblEstanques;
    @FXML private Label lblLotes;
    @FXML private Label lblTruchas;
    @FXML private Label lblOcupacion;
    @FXML private ProgressBar barraOcupacion;
    @FXML private BarChart<String, Number> graficoTruchas;
    private final EstanqueService estanqueService =
            new EstanqueService();

    private final LoteService loteService =
            new LoteService();

    @FXML
    public void initialize() {
        cargarEstadisticas();
    }

    // CONSULTAR INFORMACION DE H2
    private void cargarEstadisticas() {

        try {
            List<Estanque> estanques =
                    estanqueService.listar();

            List<Lote> lotes =
                    loteService.listar();
            cargarGrafico(estanques, lotes);

            // TOTAL DE ESTANQUES
            lblEstanques.setText(
                    String.valueOf(estanques.size()));

            // TOTAL DE LOTES
            lblLotes.setText(
                    String.valueOf(lotes.size()));

            // TOTAL DE TRUCHAS REGISTRADAS
            long totalTruchas = lotes.stream()
                    .mapToLong(Lote::getCantidadTruchas)
                    .sum();

            lblTruchas.setText(
                    String.valueOf(totalTruchas));

            // CAPACIDAD DE ESTANQUES ACTIVOS
            Set<Long> estanquesActivos = new HashSet<>();
            long capacidadTotal = 0;

            for (Estanque estanque : estanques) {

                if ("ACTIVO".equalsIgnoreCase(
                        estanque.getEstado())) {

                    estanquesActivos.add(
                            estanque.getId());

                    capacidadTotal +=
                            estanque.getCapacidad();
                }
            }

            // TRUCHAS EN ESTANQUES ACTIVOS
            long ocupadas = lotes.stream()
                    .filter(l -> estanquesActivos.contains(
                            l.getEstanqueId()))
                    .mapToLong(Lote::getCantidadTruchas)
                    .sum();

            double porcentaje = capacidadTotal > 0
                    ? (ocupadas * 100.0) / capacidadTotal
                    : 0;

            barraOcupacion.setProgress(
                    Math.max(0,
                            Math.min(1, porcentaje / 100)));

            lblOcupacion.setText(
                    String.format(
                            Locale.forLanguageTag("es-PE"),
                            "%.1f %% de ocupación",
                            porcentaje));

        } catch (SQLException e) {

            mostrarError(
                    "No se pudieron cargar las estadísticas: "
                            + e.getMessage());
        }
    }

    // ABRIR GESTION DE ESTANQUES
    @FXML
    private void abrirEstanques(ActionEvent event) {

        abrirPantalla(
                event,
                "gestion-estanques.fxml",
                "Gestión de Estanques");
    }

    // ABRIR GESTION DE LOTES
    @FXML
    private void abrirLotes(ActionEvent event) {

        abrirPantalla(
                event,
                "gestion-lotes.fxml",
                "Gestión de Lotes");
    }

    // CAMBIAR DE PANTALLA
    private void abrirPantalla(
            ActionEvent event,
            String archivo,
            String titulo) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/pe/edu/upeu/piscicontrolf/"
                                    + archivo));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node)
                    event.getSource())
                    .getScene().getWindow();

            stage.getScene().setRoot(root);

            stage.setTitle(
                    "PisciControl | " + titulo);

        } catch (IOException e) {

            mostrarError(
                    "No se pudo abrir la pantalla: "
                            + e.getMessage());
        }
    }

    // MOSTRAR ERRORES
    private void mostrarError(String mensaje) {

        Alert alerta =
                new Alert(Alert.AlertType.ERROR);

        alerta.setTitle("PisciControl");
        alerta.setHeaderText("Ocurrió un error");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    // MOSTRAR TRUCHAS POR ESTANQUE
    private void cargarGrafico(
            List<Estanque> estanques,
            List<Lote> lotes) {

        Map<Long, Long> cantidades = new HashMap<>();

        // Sumar truchas registradas por estanque
        for (Lote lote : lotes) {

            cantidades.merge(
                    lote.getEstanqueId(),
                    (long) lote.getCantidadTruchas(),
                    Long::sum
            );
        }

        XYChart.Series<String, Number> serie =
                new XYChart.Series<>();

        serie.setName("Truchas registradas");

        // Crear una barra para cada estanque
        for (Estanque estanque : estanques) {

            long cantidad = cantidades.getOrDefault(
                    estanque.getId(), 0L);

            serie.getData().add(
                    new XYChart.Data<>(
                            estanque.getNombre(),
                            cantidad
                    )
            );
        }

        graficoTruchas.getData().clear();
        graficoTruchas.getData().add(serie);
    }

    @FXML
    private void abrirAlimentacion(ActionEvent evento) {

        abrirPantalla(
                evento,
                "gestion-alimentacion.fxml",
                "Gestión de Alimentación"
        );
    }

    @FXML
    private void abrirCrecimiento(ActionEvent evento) {

        abrirPantalla(
                evento,
                "gestion-crecimiento.fxml",
                "Gestión de Crecimiento"
        );
    }


    @FXML
    private void abrirProduccion(
            javafx.event.ActionEvent evento) {

        System.out.println(
                "PASO 1: Se hizo clic en Producción");

        long inicio = System.currentTimeMillis();

        try {

            Navegacion.abrir(
                    evento,
                    "gestion-produccion.fxml",
                    "Gestión de Producción"
            );

            System.out.println(
                    "PASO 2: Producción abrió correctamente");

            System.out.println(
                    "Tiempo de carga: "
                            + (System.currentTimeMillis() - inicio)
                            + " ms"
            );

        } catch (Exception e) {

            System.err.println(
                    "ERROR AL ABRIR PRODUCCIÓN:");

            e.printStackTrace();

            javafx.scene.control.Alert alerta =
                    new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR);

            alerta.setTitle("PisciControl");
            alerta.setHeaderText(
                    "No se pudo abrir Producción");

            alerta.setContentText(e.toString());
            alerta.showAndWait();
        }
    }

}




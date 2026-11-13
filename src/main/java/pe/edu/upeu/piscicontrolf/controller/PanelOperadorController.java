
package pe.edu.upeu.piscicontrolf.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.io.IOException;

public class PanelOperadorController {

    @FXML
    private void abrirLotes(ActionEvent evento) {
        navegar(evento,
                "gestion-lotes.fxml",
                "Gestión de Lotes");
    }

    @FXML
    private void abrirAlimentacion(ActionEvent evento) {
        navegar(evento,
                "gestion-alimentacion.fxml",
                "Gestión de Alimentación");
    }

    @FXML
    private void abrirCrecimiento(ActionEvent evento) {
        navegar(evento,
                "gestion-crecimiento.fxml",
                "Gestión de Crecimiento");
    }

    @FXML
    private void abrirProduccion(ActionEvent evento) {
        navegar(evento,
                "gestion-produccion.fxml",
                "Gestión de Producción");
    }

    @FXML
    private void abrirAgua() {
        pendiente("Calidad del Agua");
    }

    @FXML
    private void abrirMortalidad() {
        pendiente("Mortalidad y Sanidad");
    }

    @FXML
    private void cerrarSesion(ActionEvent evento) {
        navegar(evento,
                "login.fxml",
                "Inicio de Sesión");
    }

    private void navegar(
            ActionEvent evento,
            String archivo,
            String titulo) {

        try {
            Navegacion.abrir(
                    evento, archivo, titulo);

        } catch (IOException e) {

            Alert alerta = new Alert(
                    Alert.AlertType.ERROR);

            alerta.setTitle("PisciControl");
            alerta.setHeaderText(
                    "Error al abrir la pantalla");

            alerta.setContentText(e.getMessage());
            alerta.showAndWait();
        }
    }

    private void pendiente(String modulo) {

        Alert alerta = new Alert(
                Alert.AlertType.INFORMATION);

        alerta.setTitle("PisciControl");
        alerta.setHeaderText(modulo);

        alerta.setContentText(
                "Este módulo todavía está en desarrollo.");

        alerta.showAndWait();
    }
}

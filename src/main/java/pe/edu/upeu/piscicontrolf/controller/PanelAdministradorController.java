
package pe.edu.upeu.piscicontrolf.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import java.io.IOException;

public class PanelAdministradorController {

    @FXML
    private void abrirEstanques(ActionEvent e) {
        navegar(e, "gestion-estanques.fxml",
                "Gestión de Estanques");
    }

    @FXML
    private void abrirLotes(ActionEvent e) {
        navegar(e, "gestion-lotes.fxml",
                "Gestión de Lotes");
    }

    @FXML
    private void abrirAlimentacion(ActionEvent e) {
        navegar(e, "gestion-alimentacion.fxml",
                "Gestión de Alimentación");
    }

    @FXML
    private void abrirCrecimiento(ActionEvent e) {
        navegar(e, "gestion-crecimiento.fxml",
                "Gestión de Crecimiento");
    }

    @FXML
    private void abrirProduccion(ActionEvent e) {
        navegar(e, "gestion-produccion.fxml",
                "Gestión de Producción");
    }

    @FXML
    private void cerrarSesion(ActionEvent e) {
        SesionUsuario.cerrarSesion();
        navegar(e, "login.fxml", "Iniciar Sesión");
    }

    // MÓDULOS QUE TODAVÍA CONSTRUIREMOS

    @FXML
    private void abrirAgua(ActionEvent e) {
        navegar(
                e,
                "gestion-calidad-agua.fxml",
                "Gestión de Calidad del Agua"
        );
    }



    @FXML
    private void abrirMortalidad(ActionEvent e) {
        navegar(
                e,
                "gestion-mortalidad.fxml",
                "Gestión de Mortalidad y Sanidad"
        );
    }


    @FXML
    private void abrirVentas(ActionEvent e) {

        navegar(
                e,
                "gestion-ventas.fxml",
                "Gestión de Ventas"
        );
    }



    @FXML
    private void abrirReportes(ActionEvent e) {

        navegar(
                e,
                "gestion-reportes.fxml",
                "Reportes Generales"
        );
    }


    @FXML
    private void abrirUsuarios() {
        pendiente("Gestión de Usuarios");
    }

    @FXML
    private void abrirConfiguracion() {
        pendiente("Configuración");
    }

    private void navegar(
            ActionEvent evento,
            String archivo,
            String titulo) {

        try {
            Navegacion.abrir(evento, archivo, titulo);
        } catch (IOException e) {
            Alert alerta = new Alert(
                    Alert.AlertType.ERROR);
            alerta.setTitle("PisciControl");
            alerta.setHeaderText(
                    "No se pudo abrir la pantalla");
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
                "Esta función se implementará próximamente.");
        alerta.showAndWait();
    }
}

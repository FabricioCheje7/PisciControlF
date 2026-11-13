
package pe.edu.upeu.piscicontrolf.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public final class Navegacion {

    private Navegacion() {
    }

    public static void abrir(
            ActionEvent evento,
            String archivo,
            String titulo) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                Navegacion.class.getResource(
                        "/pe/edu/upeu/piscicontrolf/" + archivo)
        );

        Parent pantalla = loader.load();

        Stage ventana = (Stage) ((Node)
                evento.getSource())
                .getScene()
                .getWindow();

        ventana.getScene().setRoot(pantalla);

        ventana.setTitle(
                "PisciControl | " + titulo);
    }
}

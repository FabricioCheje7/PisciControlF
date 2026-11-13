
package pe.edu.upeu.piscicontrolf;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import pe.edu.upeu.piscicontrolf.config.DatabaseConfig;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {


        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "login.fxml")


        );

        Scene scene = new Scene(
                loader.load(), 1250, 760
        );

        stage.setTitle(
                "PisciControl | Gestión de Estanques"
        );

        stage.setScene(scene);

        stage.setMinWidth(1050);
        stage.setMinHeight(650);

        stage.setMaximized(true);

        stage.show();
    }

    @Override
    public void stop() {
        DatabaseConfig.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


package pe.edu.upeu.piscicontrolf.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import pe.edu.upeu.piscicontrolf.model.Usuario;
import pe.edu.upeu.piscicontrolf.service.AutenticacionService;
import pe.edu.upeu.piscicontrolf.config.SesionUsuario;
import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnIngresar;

    @FXML
    private Label lblMensaje;

    private final AutenticacionService autenticacionService =
            new AutenticacionService();

    // INICIAR SESIÓN
    @FXML
    private void ingresar() {

        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (usuario.isBlank() || password.isBlank()) {
            lblMensaje.setText(
                    "Ingrese su usuario y contraseña.");
            return;
        }

        lblMensaje.setText("Verificando credenciales...");
        btnIngresar.setDisable(true);

        // VALIDAR SIN BLOQUEAR LA PANTALLA
        Task<Usuario> tarea = new Task<>() {

            @Override
            protected Usuario call() throws Exception {
                return autenticacionService.iniciarSesion(
                        usuario, password);
            }
        };

        tarea.setOnSucceeded(evento -> {

            btnIngresar.setDisable(false);

            Usuario usuarioAutenticado = tarea.getValue();

            abrirPanel(usuarioAutenticado);
        });

        tarea.setOnFailed(evento -> {

            btnIngresar.setDisable(false);
            txtPassword.clear();

            Throwable error = tarea.getException();

            if (error instanceof IllegalArgumentException) {
                lblMensaje.setText(error.getMessage());
            } else {
                lblMensaje.setText(
                        "No se pudo verificar el acceso.");
                error.printStackTrace();
            }
        });

        Thread hilo = new Thread(tarea, "login-piscicontrol");
        hilo.setDaemon(true);
        hilo.start();
    }

    // ABRIR PANEL SEGÚN ROL
    private void abrirPanel(Usuario usuario) {

        String archivo;
        String titulo;

        if ("ADMINISTRADOR".equals(usuario.getRol())) {

            archivo = "panel-administrador.fxml";
            titulo = "PisciControl - Administrador";

        } else if ("OPERADOR".equals(usuario.getRol())) {

            archivo = "panel-operador.fxml";
            titulo = "PisciControl - Operador";

        } else {

            lblMensaje.setText("Rol no autorizado.");
            return;
        }

        try {

            URL recurso = getClass().getResource(
                    "/pe/edu/upeu/piscicontrolf/" + archivo);

            if (recurso == null) {
                throw new IOException(
                        "No se encontró " + archivo);
            }
            SesionUsuario.iniciar(usuario);
            FXMLLoader loader = new FXMLLoader(recurso);
            Parent pantalla = loader.load();

            Stage ventana = (Stage)
                    btnIngresar.getScene().getWindow();

            ventana.getScene().setRoot(pantalla);
            ventana.setTitle(titulo);
            ventana.centerOnScreen();

        } catch (IOException e) {
            SesionUsuario.cerrarSesion();
            lblMensaje.setText(
                    "No se pudo abrir el panel del usuario.");

            e.printStackTrace();
        }
    }
}

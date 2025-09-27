package hu.carenda.app.ui;

import hu.carenda.app.model.User;
import hu.carenda.app.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        setError("");
    }

    @FXML
    private void onLogin(ActionEvent e) {
        setError("");

        String u = usernameField.getText();
        String p = passwordField.getText();

        Optional<User> user = authService.login(u, p);
        if (user.isEmpty()) {
            setError("Hibás felhasználónév vagy jelszó.");
            return;
        }

        try {
            var url = java.util.Objects.requireNonNull(
                getClass().getResource("/hu/carenda/app/views/dashboard.fxml"),
                "Nem található: /hu/carenda/app/views/dashboard.fxml"
            );

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // (ha kell: átadod a bejelentkezett usert a dashboardnak)
            // DashboardController ctrl = loader.getController();
            // ctrl.setCurrentUser(user.get());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Carenda – Vezérlőpult");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            setError("Hiba: " + rootCauseMessage(ex));
        }
    }

    private void setError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg);
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getClass().getSimpleName() + ": " + String.valueOf(c.getMessage());
    }
}

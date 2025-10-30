package hu.carenda.app.ui;

import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;
import hu.carenda.app.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.Objects;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    @FXML
    private void initialize() {
        setError("");
    }

    @FXML
    private void onLogin(ActionEvent e) {
        setError("");

        String u = usernameField.getText();
        String p = passwordField.getText();

        Optional<User> userOpt = authService.login(u, p);
        if (userOpt.isEmpty()) {
            setError("Hibás felhasználónév vagy jelszó.");
            return;
        }

        User loggedInUser = userOpt.get();

        try {
            // Ha kötelező jelszót cserélni, akkor előbb azt intézzük
            if (loggedInUser.isMustChangePassword()) {
                boolean changed = showChangePasswordDialog(loggedInUser);
                if (!changed) {
                    // User rányomott, hogy mégse / bezárta az ablakot / nem adott meg érvényes új jelszót
                    setError("A jelszó megváltoztatása kötelező az első bejelentkezés után.");
                    return;
                }
                // ha sikeresen megváltoztatta, akkor mostantól már nem kötelező
                loggedInUser.setMustChangePassword(false);
            }

            // ha minden rendben, mehetünk a dashboardra
            openDashboardAndCloseLogin(loggedInUser);

        } catch (Exception ex) {
            ex.printStackTrace();
            setError("Hiba: " + rootCauseMessage(ex));
        }
    }

    /**
     * Megnyitja a Dashboard felületet, átadja a currentUser-t (ha szükséges),
     * majd bezárja a Login ablakot.
     */
    private void openDashboardAndCloseLogin(User currentUser) throws Exception {
        var url = Objects.requireNonNull(
                getClass().getResource("/hu/carenda/app/views/dashboard.fxml"),
                "Nem található: /hu/carenda/app/views/dashboard.fxml"
        );

        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();

        // Ha a DashboardController-nek van olyan metódusa, hogy setCurrentUser(...),
        // akkor itt átadhatod:
        DashboardController ctrl = loader.getController();
        ctrl.setCurrentUser(currentUser);

        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setTitle("Carenda – Vezérlőpult");
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();
    }

    /**
     * Megjeleníti a "Jelszó megváltoztatása" modális ablakot.
     * Visszatér: true = sikeresen mentett új jelszót, false = nem (megszakította vagy hiba).
     */
    private boolean showChangePasswordDialog(User userNeedingChange) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    Objects.requireNonNull(
                            getClass().getResource("/hu/carenda/app/views/change-password.fxml"),
                            "Nem található: /hu/carenda/app/views/change-password.fxml"
                    )
            );
            Parent dialogRoot = fxmlLoader.load();

            ChangePasswordController ctrl = fxmlLoader.getController();
            ctrl.setUserAndDao(userNeedingChange, userDao);

            // külön kis modal stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Új jelszó beállítása");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(usernameField.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);

            // blokkol, amíg be nem zárják
            dialogStage.showAndWait();

            // a controller jelzi vissza, hogy sikerült-e menteni új jelszót
            return ctrl.isPasswordChangedSuccessfully();

        } catch (Exception ex) {
            ex.printStackTrace();
            setError("Nem sikerült megnyitni a jelszócsere ablakot: " + rootCauseMessage(ex));
            return false;
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

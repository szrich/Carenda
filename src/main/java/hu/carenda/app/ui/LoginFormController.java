package hu.carenda.app.ui;

import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;
import hu.carenda.app.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;
import java.util.Objects;

/**
 * A bejelentkezési képernyő (login.fxml) vezérlője.
 * Felelős a felhasználó authentikálásáért és a Dashboardra való átirányításért.
 */
public class LoginFormController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    /**
     * A "Bejelentkezés" gomb eseménykezelője.
     */
    @FXML
    private void onLogin(ActionEvent e) {
        String u = usernameField.getText();
        String p = passwordField.getText();

        Optional<User> userOpt = authService.login(u, p);
        if (userOpt.isEmpty()) {
            showError("Bejelentkezési hiba", "Hibás felhasználónév vagy jelszó.");
            return;
        }

        User loggedInUser = userOpt.get();

        try {
            // Ha kötelező jelszót cserélni, akkor előbb azt intézzük
            if (loggedInUser.isMustChangePassword()) {
                
                // A 'showChangePasswordDialog' most már 'boolean'-t ad vissza
                boolean changedSuccessfully = showChangePasswordDialog(loggedInUser);
                
                if (!changedSuccessfully) {
                    // User rányomott, hogy mégse / bezárta az ablakot
                    showError("Kötelező jelszócsere", "A jelszó megváltoztatása kötelező az első bejelentkezés után.");
                    return;
                }
                // Ha sikeresen megváltoztatta, akkor mostantól már nem kötelező
                loggedInUser.setMustChangePassword(false);
            }

            // Ha minden rendben, mehetünk a dashboardra
            openDashboardAndCloseLogin(loggedInUser);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Hiba", "Váratlan hiba történt az átirányítás során: " + ex.getMessage());
        }
    }

    /**
     * Megnyitja a Dashboard felületet, átadja a belépett felhasználót,
     * majd bezárja a Login ablakot.
     *
     * @param currentUser A sikeresen bejelentkezett User objektum.
     */
    private void openDashboardAndCloseLogin(User currentUser) {
        try {
            var url = Objects.requireNonNull(
                    getClass().getResource("/hu/carenda/app/views/dashboard.fxml"),
                    "Nem található: /hu/carenda/app/views/dashboard.fxml"
            );

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // Átadjuk a belépett felhasználót a Dashboard Controllernek
            DashboardFormController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);

            // Jelenlegi (Login) ablak bezárása és cseréje a Dashboardra
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Carenda – Vezérlőpult");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setResizable(true); // A Dashboard legyen átméretezhető
            stage.show();

        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            showError("Hiba az átirányításkor", "Nem sikerült betölteni a vezérlőpultot: " + e.getMessage());
        }
    }

    /**
     * Megjeleníti a "Jelszó megváltoztatása" modális ablakot.
     *
     * @param userNeedingChange A felhasználó, akinek jelszót kell cserélnie.
     * @return true = sikeresen mentett új jelszót, false = nem (megszakította vagy hiba).
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

            // Controller inicializálása
            ChangePasswordFormController ctrl = fxmlLoader.getController();
            ctrl.setUserAndDao(userNeedingChange, userDao);

            // Modális ablak beállítása
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Új jelszó beállítása");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(usernameField.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);

            // Várakozás, amíg az ablakot be nem zárják
            dialogStage.showAndWait();

            // Az eredményt a Stage.UserData-ból olvassuk ki,
            //         nem a controller metódusából.
            return Boolean.TRUE.equals(dialogStage.getUserData());

        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
            showError("Hiba", "Nem sikerült megnyitni a jelszócsere ablakot: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Segédfüggvény egy hibaüzenet (Alert) megjelenítésére.
     *
     * @param title   Az ablak címe.
     * @param message A hiba részletes leírása.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null); // Nincs fejléc
        alert.setContentText(message);
        alert.showAndWait();
    }
}

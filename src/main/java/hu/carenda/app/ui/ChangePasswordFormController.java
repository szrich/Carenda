package hu.carenda.app.ui;

import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * A kötelező jelszóváltoztatás űrlapjának vezérlője.
 * Az űrlap modálisan jelenik meg, és addig nem engedi tovább a felhasználót,
 * amíg sikeresen nem változtatott jelszót.
 */
public class ChangePasswordFormController {

    @FXML private TextField newUsernameField;
    @FXML private PasswordField newPassField, newPassAgainField;
    @FXML private Button saveButton;
    // @FXML private Button cancelButton; // Ha van "Mégse" gomb, az is onCancel-t hívja

    private User currentUser;
    private UserDao userDao;
    
    /**
     * Minimálisan elvárt jelszóhossz.
     */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * A LoginController hívja meg, hogy átadja az adatokat.
     *
     * @param currentUser Az aktuálisan belépett User objektum.
     * @param userDao     A DAO példány az adatbázis-műveletekhez.
     */
    public void setUserAndDao(User currentUser, UserDao userDao) {
        this.currentUser = currentUser;
        this.userDao = userDao;
        // Előtöltjük a jelenlegi felhasználónevet
        newUsernameField.setText(currentUser.getUsername());
    }

    /**
     * FXML inicializálás (automatikusan hívódik).
     */
    @FXML
    private void initialize() {
        // Kezdeti teendők (ha lennének)
    }

    /**
     * A "Mentés" gomb eseménykezelője.
     * Validálja az adatokat, és ha minden rendben, frissíti a felhasználót.
     */
    @FXML
    private void onSave() {
        String newUsername = newUsernameField.getText().trim();
        String p1 = newPassField.getText();
        String p2 = newPassAgainField.getText();

        // --- Validáció ---

        if (newUsername.isBlank()) {
            showError("Validációs Hiba", "A felhasználónév nem lehet üres.");
            return;
        }

        if (p1.isBlank()) {
            showError("Validációs Hiba", "A jelszó nem lehet üres.");
            return;
        }

        if (!p1.equals(p2)) {
            showError("Validációs Hiba", "A két jelszó nem egyezik.");
            return;
        }

        if (p1.length() < MIN_PASSWORD_LENGTH) {
            showError("Validációs Hiba", "A jelszónak legalább " + MIN_PASSWORD_LENGTH + " karakter hosszúnak kell lennie.");
            return;
        }

        // --- Mentés ---

        try {
            // Új felhasználónév + új jelszó elmentése (bcrypt hash)
            userDao.updateUsernameAndPassword(currentUser.getId(), newUsername, p1);

            // Sikeres mentés, jelezzük a hívónak (a LoginControllernek)
            closeWindow(true);

        } catch (RuntimeException ex) {
            ex.printStackTrace();
            showError("Mentési Hiba", "Adatbázis hiba történt: " + ex.getMessage());
        }
    }

    /**
     * A "Mégse" gomb eseménykezelője.
     * Bezárja az ablakot mentés nélkül.
     */
    @FXML
    private void onCancel() {
        closeWindow(false);
    }

    /**
     * Bezárja az ablakot, és beállítja a UserData-t,
     * jelezve a hívónak (LoginController) a művelet sikerességét.
     *
     * @param success true, ha a mentés sikeres volt, egyébként false.
     */
    private void closeWindow(boolean success) {
        Stage st = (Stage) saveButton.getScene().getWindow();
        // A Forms.java-ban használt mintát követjük:
        st.setUserData(success); 
        st.close();
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

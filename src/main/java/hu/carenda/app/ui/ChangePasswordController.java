package hu.carenda.app.ui;

import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ChangePasswordController {

    @FXML private TextField newUsernameField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField newPassAgainField;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private User currentUser;
    private UserDao userDao;
    private boolean passwordChangedSuccessfully = false;

    public void setUserAndDao(User currentUser, UserDao userDao) {
        this.currentUser = currentUser;
        this.userDao = userDao;
        // előtöltjük a jelenlegi felhasználónevet
        newUsernameField.setText(currentUser.getUsername());
    }

    public boolean isPasswordChangedSuccessfully() {
        return passwordChangedSuccessfully;
    }

    @FXML
    private void initialize() {
        setStatus("");
    }

    @FXML
    private void onSave() {
        setStatus("");

        String newUsername = newUsernameField.getText().trim();
        String p1 = newPassField.getText();
        String p2 = newPassAgainField.getText();

        if (newUsername.isBlank()) {
            setStatus("A felhasználónév nem lehet üres.");
            return;
        }

        if (p1.isBlank() || p2.isBlank()) {
            setStatus("A jelszó nem lehet üres.");
            return;
        }

        if (!p1.equals(p2)) {
            setStatus("A két jelszó nem egyezik.");
            return;
        }

        if (p1.length() < 6) {
            setStatus("A jelszónak legalább 6 karakter hosszúnak kell lennie.");
            return;
        }

        try {
            // új felhasználónév + új jelszó elmentése (bcrypt hash)
            userDao.updateUsernameAndPassword(currentUser.getId(), newUsername, p1);

            passwordChangedSuccessfully = true;
            closeWindow();

        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Hiba mentés közben: " + rootCauseMessage(ex));
        }
    }

    @FXML
    private void onCancel() {
        passwordChangedSuccessfully = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage st = (Stage) saveButton.getScene().getWindow();
        st.close();
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getClass().getSimpleName() + ": " + String.valueOf(c.getMessage());
    }
}

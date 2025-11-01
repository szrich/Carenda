package hu.carenda.app.ui;

import hu.carenda.app.model.User;
import hu.carenda.app.repository.UserDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UsersAdminFormController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Number> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;

    @FXML private TextField newUsernameField;
    @FXML private TextField newFullNameField;
    @FXML private TextField newRoleField;
    @FXML private PasswordField newTempPasswordField;

    @FXML private Label statusLabel;

    private final UserDao userDao = new UserDao();
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // táblázat oszlopainak összekötése a User getterekkel
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()));
        colUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        colFullName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFullName()));
        colRole.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRoleName()));

        usersTable.setItems(userList);

        setStatus("");
    }

    /**
     * Ezt hívja a DashboardController közvetlenül megnyitás után.
     */
    public void loadUsersTable() {
        userList.setAll(userDao.findAll());
    }

    @FXML
    private void onAddUser() {
        setStatus("");

        String username = newUsernameField.getText().trim();
        String fullname = newFullNameField.getText().trim();
        String role = newRoleField.getText().trim();
        String tempPass = newTempPasswordField.getText();

        if (username.isBlank() || tempPass.isBlank()) {
            setStatus("Felhasználónév és ideiglenes jelszó kötelező.");
            return;
        }

        if (tempPass.length() < 6) {
            setStatus("Az ideiglenes jelszó legyen legalább 6 karakter.");
            return;
        }

        if (role.isBlank()) {
            role = "USER"; // default szerep
        }

        try {
            userDao.createUser(username, fullname, role, tempPass);
            // újratöltjük a listát
            userList.setAll(userDao.findAll());
            // mezők ürítése
            newUsernameField.clear();
            newFullNameField.clear();
            newRoleField.clear();
            newTempPasswordField.clear();
            setStatus("Felhasználó hozzáadva.");
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Hiba létrehozáskor: " + ex.getMessage());
        }
    }

    @FXML
    private void onDeleteUser() {
        setStatus("");
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Nincs kiválasztva felhasználó törléshez.");
            return;
        }

        try {
            userDao.deleteUser(selected.getId());
            userList.setAll(userDao.findAll());
            setStatus("Törölve.");
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Hiba törléskor: " + ex.getMessage());
        }
    }

    @FXML
    private void onResetPassword() {
        setStatus("");
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Nincs kiválasztva felhasználó.");
            return;
        }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Jelszó visszaállítása");
        dlg.setHeaderText("Új ideiglenes jelszó megadása ehhez a felhasználóhoz:");
        dlg.setContentText("Új jelszó:");

        dlg.showAndWait().ifPresent(newPwd -> {
            if (newPwd.length() < 6) {
                setStatus("A jelszó legyen legalább 6 karakter.");
                return;
            }
            try {
                userDao.resetPassword(selected.getId(), newPwd);
                setStatus("Új jelszó beállítva.");
                // újratöltjük, mert must_change_password újra 1-re kerül
                userList.setAll(userDao.findAll());
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus("Hiba reset közben: " + ex.getMessage());
            }
        });
    }

    private void setStatus(String s) {
        if (statusLabel != null) statusLabel.setText(s);
    }
}

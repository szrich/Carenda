package hu.carenda.app.ui;

import hu.carenda.app.model.Customer;
import hu.carenda.app.repository.CustomerDao;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class CustomerFormController {

    @FXML
    private TextField name, phone, email;
    private final CustomerDao dao = new CustomerDao();
    private Customer editing;

    public void setEditing(Customer c) {
        this.editing = c;
        if (c != null) {
            name.setText(c.getName());
            phone.setText(c.getPhone());
            email.setText(c.getEmail());
        }
    }

    @FXML
    public void onSave() {
        // --- VALIDÁCIÓ ---
        String n = name.getText().trim();
        String p = phone.getText().trim();
        String e = email.getText().trim();

        if (n.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "A név kötelező.").showAndWait();
            return;
        }
        if (!e.isEmpty() && !e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            new Alert(Alert.AlertType.WARNING, "Érvénytelen e-mail cím.").showAndWait();
            return;
        }

        // --- MENTÉS (a meglévő logikád maradjon) ---
        if (editing == null) {
            dao.insert(n, p, e);
        } else {
            dao.update(editing.getId(), n, p, e);
        }

        // --- BEZÁRÁS ---
        ((Stage) name.getScene().getWindow()).close();
    }

    @FXML
    public void onCancel() {
        close();
    }

    private void close() {
        ((Stage) name.getScene().getWindow()).close();
    }
}

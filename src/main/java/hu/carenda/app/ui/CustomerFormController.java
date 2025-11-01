package hu.carenda.app.ui;

import hu.carenda.app.model.Customer;
import hu.carenda.app.repository.CustomerDao;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.regex.Pattern;

/**
 * Az Ügyfél űrlap (customer-form.fxml) vezérlője.
 * Felelős az új ügyfelek létrehozásáért és a meglévők szerkesztéséért.
 */
public class CustomerFormController {

    @FXML
    private TextField name, phone, email;

    private final CustomerDao dao = new CustomerDao();
    private Customer editing;

    // Egyszerű e-mail validációs Regex (a JavaFX-ben nincs beépített)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * A Forms osztály hívja meg, hogy beállítsa a szerkesztendő ügyfelet.
     *
     * @param c A szerkesztendő Customer objektum, vagy null, ha új ügyfélről van szó.
     */
    public void setEditing(Customer c) {
        this.editing = c;
        if (c != null) {
            name.setText(c.getName());
            phone.setText(c.getPhone());
            email.setText(c.getEmail());
        }
    }

    /**
     * A "Mentés" gomb eseménykezelője.
     * Validálja az adatokat, majd menti (insert/update) az adatbázisba.
     */
    @FXML
    public void onSave() {
        try {
            // --- 1. Validáció ---
            String n = name.getText().trim();
            String p = phone.getText().trim();
            String e = email.getText().trim();

            if (n.isEmpty()) {
                showWarning("Validációs Hiba", "A név megadása kötelező.");
                return;
            }
            // Az e-mail csak akkor kötelező, ha meg van adva, de akkor formátumilag helyesnek kell lennie
            if (!e.isEmpty() && !EMAIL_PATTERN.matcher(e).matches()) {
                showWarning("Validációs Hiba", "Érvénytelen e-mail cím formátum.");
                return;
            }

            // --- 2. Mentés ---
            if (editing == null || editing.getId() == null) {
                // Új ügyfél létrehozása
                dao.insert(n, p, e);
            } else {
                // Meglévő ügyfél frissítése
                dao.update(editing.getId(), n, p, e);
            }

            // --- 3. Sikeres bezárás ---
            // Jelezzük a hívó ablaknak (Dashboard), hogy a mentés sikeres volt
            closeWindow(true);

        } catch (RuntimeException ex) {
            // Hiba adatbázis-művelet közben (pl. UNIQUE constraint)
            ex.printStackTrace();
            showError("Mentési Hiba", "Adatbázis hiba történt: " + ex.getMessage());
        }
    }

    /**
     * A "Mégse" gomb eseménykezelője.
     * Bezárja az ablakot mentés nélkül.
     */
    @FXML
    public void onCancel() {
        closeWindow(false);
    }

    /**
     * Bezárja az ablakot, és beállítja a UserData-t,
     * jelezve a hívónak (Forms -> Dashboard) a művelet sikerességét.
     *
     * @param success true, ha a mentés sikeres volt, egyébként false.
     */
    private void closeWindow(boolean success) {
        Stage st = (Stage) name.getScene().getWindow();
        st.setUserData(success); // Ezt olvassa ki a Forms.openModalForm
        st.close();
    }

    // --- UI Segédfüggvények (Alerts) ---

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

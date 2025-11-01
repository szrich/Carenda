package hu.carenda.app.ui;

import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * A Jármű űrlap (vehicle-form.fxml) vezérlője.
 * Felelős az új járművek létrehozásáért és a meglévők szerkesztéséért.
 */
public class VehicleFormController {

    @FXML
    private TextField plate, vin, engine_no, brand, model;
    @FXML
    private ComboBox<Integer> yearPicker;
    @FXML
    private ComboBox<String> fuel_type;
    @FXML
    private ComboBox<Customer> ownerCombo;
    @FXML
    private Button saveBtn;

    private final VehicleDao vehicleDao = new VehicleDao();
    private final CustomerDao customerDao = new CustomerDao();

    private Vehicle editing; // ha szerkesztünk, ezt kapjuk meg

    /**
     * Az FXML betöltése után hívódik meg.
     * Feltölti a ComboBox-okat (Ügyfél, Évjárat, Üzemanyag)
     * és beállítja a "Mentés" gomb letiltási logikáját.
     */
    @FXML
    public void initialize() {
        // Ügyfelek betöltése a legördülőbe
        var customers = FXCollections.observableArrayList(customerDao.findAll());
        ownerCombo.setItems(customers);

        // Szebb megjelenítés: név jelenjen meg
        ownerCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        ownerCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Üzemanyag típusok
        fuel_type.setItems(FXCollections.observableArrayList("BENZIN", "DÍZEL", "HIBRID", "ELEKTROMOS", "EGYÉB"));
        fuel_type.getSelectionModel().select("BENZIN");

        // Évek feltöltése (pl. 1980 → aktuális év, csökkenő sorrend)
        var years = FXCollections.<Integer>observableArrayList();
        int thisYear = java.time.Year.now().getValue();
        for (int y = thisYear; y >= 1980; y--) {
            years.add(y);
        }
        yearPicker.setItems(years);
        yearPicker.getSelectionModel().select(Integer.valueOf(thisYear)); // alapértelmezett

        // Mentés gomb letiltása, ha a rendszám VAGY a tulajdonos hiányzik
        saveBtn.disableProperty().bind(
                ownerCombo.valueProperty().isNull()
                        .or(plate.textProperty().isEmpty())
        );
    }

    /**
     * A Forms osztály hívja meg, hogy beállítsa a szerkesztendő járművet.
     *
     * @param v A szerkesztendő Vehicle objektum, vagy null, ha új járműről van szó.
     */
    public void setEditing(Vehicle v) {
        this.editing = v;
        if (v != null) {
            plate.setText(v.getPlate());
            vin.setText(v.getVin());
            engine_no.setText(v.getEngine_no());
            brand.setText(v.getBrand());
            model.setText(v.getModel());
            
            if (v.getYear() != null && yearPicker.getItems().contains(v.getYear())) {
                yearPicker.setValue(v.getYear());
            } else {
                yearPicker.getSelectionModel().clearSelection();
            }
            
            fuel_type.setValue(v.getFuel_type());

            // Tulajdonos beállítása (Integer ID alapján)
            if (v.getOwnerId() != null) {
                for (var c : ownerCombo.getItems()) {
                    // Integer összehasonlítás Objects.equals-szel
                    if (Objects.equals(c.getId(), v.getOwnerId())) {
                        ownerCombo.getSelectionModel().select(c);
                        break;
                    }
                }
            }
        }
    }

    /**
     * A "Mentés" gomb eseménykezelője.
     * Validálja az adatokat, majd menti (insert/update) az adatbázisba.
     */
    @FXML
    public void onSave() {
        try {
            // --- 1. Adatok és Validáció ---
            String pl = plate.getText().trim();
            String vn = vin.getText().trim().isEmpty() ? null : vin.getText().trim(); // Üres string helyett null
            String en = engine_no.getText().trim().isEmpty() ? null : engine_no.getText().trim(); // Üres string helyett null
            String br = brand.getText().trim();
            String md = model.getText().trim();
            Integer yp = yearPicker.getValue();
            String ft = fuel_type.getValue();
            Customer ow = ownerCombo.getValue();

            if (pl.isEmpty()) {
                showWarning("Validációs Hiba", "A rendszám megadása kötelező.");
                return;
            }
            if (br.isEmpty()) {
                showWarning("Validációs Hiba", "A gyártmány megadása kötelező.");
                return;
            }
            if (md.isEmpty()) {
                showWarning("Validációs Hiba", "A típus megadása kötelező.");
                return;
            }
            if (ow == null) {
                showWarning("Validációs Hiba", "A tulajdonos kiválasztása kötelező.");
                return;
            }
            if (ow.getId() == null) {
                // Ez akkor fordulhat elő, ha egy "új" ügyfél van kiválasztva, de még nincs elmentve
                showWarning("Validációs Hiba", "A kiválasztott ügyfél még nincs elmentve az adatbázisban.");
                return;
            }

            // --- 2. Mentés ---
            // Ellenőrzés, hogy az 'editing' ID-ja null-e (a CustomerFormController-rel konzisztensen)
            if (editing == null || editing.getId() == null) {
                // Új jármű
                vehicleDao.insert(pl, vn, en, br, md, yp, ft, ow.getId());
            } else {
                // Meglévő jármű frissítése
                vehicleDao.update(editing.getId(), pl, vn, en, br, md, yp, ft, ow.getId());
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
        Stage st = (Stage) plate.getScene().getWindow();
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

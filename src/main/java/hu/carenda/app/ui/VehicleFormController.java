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
import javafx.stage.Stage;

public class VehicleFormController {

    @FXML private TextField plate;
    @FXML private TextField makeModel;
    @FXML private ComboBox<Customer> ownerCombo;

    // <<< HIÁNYZÓ DAO-K PÓTLÁSA >>>
    private final VehicleDao vehicleDao = new VehicleDao();
    private final CustomerDao customerDao = new CustomerDao();

    private Vehicle editing; // ha szerkesztünk, ezt kapjuk meg

    @FXML
    public void initialize() {
        // Ügyfelek betöltése a legördülőbe
        var customers = FXCollections.observableArrayList(customerDao.findAll());
        ownerCombo.setItems(customers);

        // Szebb megjelenítés: név jelenjen meg
        ownerCombo.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        ownerCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    /** Szerkesztésnél hívjuk a Forms.vehicle(editing) után */
    public void setEditing(Vehicle v) {
        this.editing = v;
        if (v != null) {
            plate.setText(v.getPlate());
            makeModel.setText(v.getMakeModel());
            // tulaj beállítása
            if (v.getOwnerId() > 0) {
                for (var c : ownerCombo.getItems()) {
                    if (c.getId() == v.getOwnerId()) {
                        ownerCombo.getSelectionModel().select(c);
                        break;
                    }
                }
            }
        }
    }

    @FXML
    public void onSave() {
        // --- VALIDÁCIÓ ---
        String pl = plate.getText().trim();
        String mm = makeModel.getText().trim();
        Customer ow = ownerCombo.getValue();

        if (pl.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "A rendszám kötelező.").showAndWait();
            return;
        }
        if (mm.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "A gyártmány/típus kötelező.").showAndWait();
            return;
        }
        if (ow == null) {
            new Alert(Alert.AlertType.WARNING, "Válassz ügyfelet.").showAndWait();
            return;
        }

        // --- MENTÉS ---
        if (editing == null) {
            vehicleDao.insert(pl, mm, ow.getId());
        } else {
            vehicleDao.update(editing.getId(), pl, mm, ow.getId());
        }

        // --- BEZÁRÁS ---
        ((Stage) plate.getScene().getWindow()).close();
    }

    @FXML
    public void onCancel() {
        ((Stage) plate.getScene().getWindow()).close();
    }
}

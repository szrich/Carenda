package hu.carenda.app.ui;

import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

public class VehicleFormController {

    @FXML
    private TextField plate;
    @FXML
    private TextField vin;
    @FXML
    private TextField engine_no;
    @FXML
    private TextField brand;
    @FXML
    private TextField model;
    @FXML
    private ComboBox<Integer> yearPicker;
    @FXML
    private ComboBox<String> fuel_type;
    @FXML
    private ComboBox<Customer> ownerCombo;
    @FXML
    private javafx.scene.control.Button saveBtn;

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

        /*
        // csak számok engedélyezése
        odometer_km.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("[0-9]*")) {
                return change; // ha csak szám, engedjük
            }
            return null; // egyébként tiltjuk a bevitelt
        }));
        */
        fuel_type.setItems(FXCollections.observableArrayList("BENZIN", "DÍZEL", "HIBRID", "ELEKTROMOS"));
        fuel_type.getSelectionModel().select("BENZIN");

        // Évek feltöltése az FXML-es yearPicker-be (pl. 1980 → aktuális év, csökkenő sorrend)
        var years = FXCollections.<Integer>observableArrayList();
        int thisYear = java.time.Year.now().getValue();
        for (int y = thisYear; y >= 1980; y--) {
            years.add(y);
        }
        yearPicker.setItems(years);
        yearPicker.getSelectionModel().select(Integer.valueOf(thisYear)); // alapértelmezett

        saveBtn.disableProperty().bind(
                ownerCombo.valueProperty().isNull()
                        .or(plate.textProperty().isEmpty())
        );

    }

    /**
     * Szerkesztésnél hívjuk a Forms.vehicle(editing) után
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
        String vn = vin.getText().trim();
        String en = engine_no.getText().trim();
        String br = brand.getText().trim();
        String md = model.getText().trim();
        Integer yp = yearPicker.getValue();
        String ft = fuel_type.getValue();
        Customer ow = ownerCombo.getValue();

        if (pl.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "A rendszám kötelező.").showAndWait();
            return;
        }
        if (vn.isEmpty()) {
            vn = null;
        }
        if (en.isEmpty()) {
            en = null;
        }
        if (br.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "A gyártmány kötelező.").showAndWait();
            return;
        }
        if (md.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "A típus kötelező.").showAndWait();
            return;
        }
        if (ow == null) {
            new Alert(Alert.AlertType.WARNING, "Válassz ügyfelet.").showAndWait();
            return;
        }

        // --- MENTÉS ---
        if (editing == null) {
            vehicleDao.insert(pl, vn, en, br, md, yp, ft, ow.getId());
        } else {
            vehicleDao.update(editing.getId(), pl, vn, en, br, md, yp, ft, ow.getId());
        }

        // --- BEZÁRÁS ---
        ((Stage) plate.getScene().getWindow()).close();
    }

    @FXML
    public void onCancel() {
        ((Stage) plate.getScene().getWindow()).close();
    }
}

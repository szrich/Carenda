package hu.carenda.app.ui;

import hu.carenda.app.model.Appointment;
import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.AppointmentDao;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppointmentFormController {

    @FXML
    private ComboBox<Customer> customerCombo;
    @FXML
    private ComboBox<Vehicle> vehicleCombo;

    // ÚJ: DatePicker + óraspinner + percszpinner
    @FXML
    private DatePicker datePicker;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;

    @FXML
    private TextField duration;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextArea note;

    @FXML
    private Button saveBtn;

    private final AppointmentDao apptDao = new AppointmentDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();

    private Appointment editing;

    // mentéshez / visszatöltéshez ezt a formát használjuk
    private static final DateTimeFormatter LDT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @FXML
    public void initialize() {
        // Ügyfelek
        var customers = FXCollections.observableArrayList(customerDao.findAll());
        customerCombo.setItems(customers);
        customerCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        customerCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Jármű megjelenítés
        vehicleCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getPlate());
            }
        });
        vehicleCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Vehicle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getPlate());
            }
        });

        // Ügyfél → járművek betöltése
        customerCombo.valueProperty().addListener((obs, oldVal, nv) -> {
            if (nv != null) {
                var vehicles = FXCollections.observableArrayList(vehicleDao.findByCustomer(nv.getId()));
                vehicleCombo.setItems(vehicles);
                vehicleCombo.getSelectionModel().clearSelection();
            } else {
                vehicleCombo.setItems(FXCollections.observableArrayList());
            }
        });

        // Státuszok
        statusCombo.setItems(FXCollections.observableArrayList("PLANNED", "CONFIRMED", "DONE", "CANCELLED"));
        statusCombo.getSelectionModel().select("PLANNED");

        // Dátum + idő alapérték: most
        datePicker.setValue(LocalDate.now());

        // Óra spinner: 0–23
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalDateTime.now().getHour(), 1));
        hourSpinner.setEditable(true);

        // Perc spinner: 0–59 (5-ös léptékkel kényelmesebb)
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, LocalDateTime.now().getMinute(), 5));
        minuteSpinner.setEditable(true);

        // Alapérték a duration-nek, ha üres marad
        if (duration.getText() == null || duration.getText().isBlank()) {
            duration.setText("60");
        }

// Ha valamiért az FXML onAction nem fut, kössük rá programból is
        if (saveBtn != null) {
            saveBtn.setOnAction(e -> onSave());
        }

    }

    /**
     * A hívó (Dashboard) beállítja, ha szerkesztésről van szó
     */
    public void setEditing(Appointment a) {
        this.editing = a;
        if (a != null) {
            // Ügyfél kijelölése
            for (var c : customerCombo.getItems()) {
                if (c.getId() == a.getCustomerId()) {
                    customerCombo.getSelectionModel().select(c);
                    break;
                }
            }
            // A kiválasztott ügyfél járművei
            if (customerCombo.getValue() != null) {
                var vehicles = FXCollections.observableArrayList(
                        vehicleDao.findByCustomer(customerCombo.getValue().getId())
                );
                vehicleCombo.setItems(vehicles);
            }
            // Jármű kijelölése
            for (var v : vehicleCombo.getItems()) {
                if (v.getId() == a.getVehicleId()) {
                    vehicleCombo.getSelectionModel().select(v);
                    break;
                }
            }

            // Dátum/idő visszatöltés
            try {
                LocalDateTime ldt = LocalDateTime.parse(a.getStartTs(), LDT_FMT);
                datePicker.setValue(ldt.toLocalDate());
                hourSpinner.getValueFactory().setValue(ldt.getHour());
                minuteSpinner.getValueFactory().setValue(ldt.getMinute());
            } catch (Exception ignore) {
                // ha nem sikerül parzolni, maradnak az alapértékek
            }

            duration.setText(String.valueOf(a.getDurationMinutes()));
            note.setText(a.getNote() == null ? "" : a.getNote());
            if (a.getStatus() != null) {
                statusCombo.getSelectionModel().select(a.getStatus());
            }
        }
    }

    @FXML
    public void onSave() {
        try {
            var c = customerCombo.getValue();
            var v = vehicleCombo.getValue();
            var d = datePicker.getValue();
            var hh = hourSpinner.getValue();
            var mm = minuteSpinner.getValue();
            var durStr = duration.getText().trim();
            var st = statusCombo.getValue();
            var nt = note.getText();

            // Validáció
            if (c == null) {
                new Alert(Alert.AlertType.WARNING, "Válassz ügyfelet.").showAndWait();
                return;
            }
            if (v == null) {
                new Alert(Alert.AlertType.WARNING, "Válassz járművet.").showAndWait();
                return;
            }
            if (d == null) {
                new Alert(Alert.AlertType.WARNING, "Válassz dátumot.").showAndWait();
                return;
            }
            if (st == null || st.isBlank()) {
                statusCombo.getSelectionModel().selectFirst();
                st = statusCombo.getValue();
            }

            int dur;
            try {
                dur = Integer.parseInt(durStr);
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Az időtartam percben egész szám legyen.").showAndWait();
                return;
            }

            // LocalDate + óra + perc → ISO-String "yyyy-MM-dd'T'HH:mm"
            java.time.LocalDateTime ldt = d.atTime(hh == null ? 0 : hh, mm == null ? 0 : mm);
            String when = ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

            if (editing == null) {
                apptDao.insert(c.getId(), v.getId(), when, dur, nt, st);
            } else {
                apptDao.update(editing.getId(), c.getId(), v.getId(), when, dur, nt, st);
            }

            ((Stage) datePicker.getScene().getWindow()).close();

        } catch (Exception ex) {
            ex.printStackTrace(); // látszódjon a konzolon is
            new Alert(Alert.AlertType.ERROR, "Mentési hiba: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    public void onCancel() {
        ((Stage) datePicker.getScene().getWindow()).close();
    }
}

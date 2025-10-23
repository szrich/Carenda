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
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppointmentFormController {

    @FXML
    private ComboBox<Customer> customerCombo;
    @FXML
    private TextField phone;
    @FXML
    private TextField email;
    @FXML
    private ComboBox<Vehicle> vehicleCombo;
    @FXML
    private TextField brand;

    @FXML
    private TextField model;

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
        customerCombo.valueProperty().addListener((obs, oldCustomer, newCustomer) -> {
            if (newCustomer != null) {
                phone.setText(newCustomer.getPhone());
            } else {
                phone.clear();
            }
        });
        customerCombo.valueProperty().addListener((obs, oldEmail, newEmail) -> {
            if (newEmail != null) {
                email.setText(newEmail.getEmail());
            } else {
                email.clear();
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
        vehicleCombo.valueProperty().addListener((obs, oldBrand, newBrand) -> {
            if (newBrand != null) {
                brand.setText(newBrand.getBrand());
            } else {
                brand.clear();
            }
        });
        vehicleCombo.valueProperty().addListener((obs, oldModel, newModel) -> {
            if (newModel != null) {
                model.setText(newModel.getModel());
            } else {
                model.clear();
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
        statusCombo.setItems(FXCollections.observableArrayList("TERVEZETT", "BEFEJEZETT", "LEMONDOTT"));
        statusCombo.getSelectionModel().select("TERVEZETT");

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

        // Customer megjelenítés + "beírásból létrehozás" (JDK8 kompatibilis)
        customerCombo.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return (c == null) ? "" : c.getName();
            }

            @Override
            public Customer fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }

                // van-e már ilyen név?
                Customer existing = customerCombo.getItems().stream()
                        .filter(x -> s.equalsIgnoreCase(x.getName()))
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    return existing;
                }

                // Új ügyfél létrehozása a beírt névvel
                Customer nc = new Customer();
                nc.setId(0);                // jelzi, hogy új
                nc.setName(s.trim());
                // ha már be van írva telefon/email, átvesszük
                if (phone != null) {
                    nc.setPhone(phone.getText());
                }
                if (email != null) {
                    nc.setEmail(email.getText());
                }

                customerCombo.getItems().add(nc);
                customerCombo.getSelectionModel().select(nc);
                return nc;
            }
        });

        vehicleCombo.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                return (v == null) ? "" : v.getPlate();  // a combón a rendszám látszik
            }

            @Override
            public Vehicle fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }

                Vehicle existing = vehicleCombo.getItems().stream()
                        .filter(x -> s.equalsIgnoreCase(x.getPlate()))
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    return existing;
                }

                Vehicle nv = new Vehicle();
                nv.setId(0);                // új
                nv.setPlate(s.trim());      // beírt érték a rendszám
                if (model != null) {
                    nv.setBrand(model.getText()); // típus/modell mezőből
                }
                vehicleCombo.getItems().add(nv);
                vehicleCombo.getSelectionModel().select(nv);
                return nv;
            }
        });

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
            // --- 1️⃣ Adatok kiolvasása az űrlapból ---
            var c = customerCombo.getValue();
            var ph = phone.getText();
            var e = email.getText();
            var v = vehicleCombo.getValue();
            var b = brand.getText();
            var m = model.getText();
            var d = datePicker.getValue();
            var hh = hourSpinner.getValue();
            var mm = minuteSpinner.getValue();
            var durStr = duration.getText().trim();
            var st = statusCombo.getValue();
            var nt = note.getText();

            // --- 2️⃣ Alapvető validációk ---
            if (c == null) {
                new Alert(Alert.AlertType.WARNING, "Válassz, vagy írj be új ügyfelet.").showAndWait();
                return;
            }
            if (ph == null) {
                new Alert(Alert.AlertType.WARNING, "Írj be az ügyfélhez telefonszámot.").showAndWait();
                return;
            }
            if (e == null) {
                new Alert(Alert.AlertType.WARNING, "Írj be az ügyfélhez e-mail címet.").showAndWait();
                return;
            }
            if (v == null) {
                new Alert(Alert.AlertType.WARNING, "Válassz, vagy írj be új járművet.").showAndWait();
                return;
            }
            if (b == null) {
                new Alert(Alert.AlertType.WARNING, "Írjd be a gépjármű tipusát.").showAndWait();
                return;
            }
            if (m == null) {
                new Alert(Alert.AlertType.WARNING, "Írjd be a gépjármű tipusát.").showAndWait();
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

            // --- 3️⃣ Dátum + idő kombinálása ---
            LocalDateTime ldt = d.atTime(hh == null ? 0 : hh, mm == null ? 0 : mm);
            String when = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

            // 4) Ügyfél + jármű UPSERT (INSERT vagy UPDATE)
            int customerId = upsertCustomer(c, ph, e);
            int vehicleId = upsertVehicle(v, customerId, b, m);

// 5) Időpont INSERT/UPDATE
            boolean isNew = (editing == null) || editing.getId() == 0;
            if (isNew) {
                apptDao.insert(customerId, vehicleId, when, dur, nt, st);
            } else {
                apptDao.update(editing.getId(), customerId, vehicleId, when, dur, nt, st);
            }

// 6) jelez + zár
            Stage stage = (Stage) datePicker.getScene().getWindow();
            stage.setUserData(Boolean.TRUE);
            stage.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Mentési hiba: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    public void onCancel() {
        ((Stage) datePicker.getScene().getWindow()).close();
    }

    /**
     * Insert vagy Update a customers táblában. Visszaadja az ID-t.
     */
    private int upsertCustomer(Customer c, String phoneStr, String emailStr) {
        if (c == null) {
            throw new IllegalArgumentException("customer null");
        }
        if (phoneStr != null && !phoneStr.isBlank()) {
            c.setPhone(phoneStr.trim());
        }
        if (emailStr != null && !emailStr.isBlank()) {
            c.setEmail(emailStr.trim());
        }

        if (c.getId() == 0) {
            int newId = customerDao.insert(c.getName(), c.getPhone(), c.getEmail());
            c.setId(newId);
            return newId;
        } else {
            customerDao.update(c.getId(), c.getName(), c.getPhone(), c.getEmail());
            return c.getId();
        }
    }

    /**
     * Insert vagy Update a vehicles táblában az adott customerId-vel.
     * Visszaadja az ID-t.
     */
    private int upsertVehicle(Vehicle v, int customerId, String brandStr, String modelStr) {
        if (v == null) {
            throw new IllegalArgumentException("vehicle null");
        }
        
        if (brandStr != null && !brandStr.trim().isEmpty()) {
            v.setBrand(brandStr.trim());
        }

        // ha az űrlapon gépelték be/át, onnan vesszük a gyártmány/típust
        if (modelStr != null && !modelStr.trim().isEmpty()) {
            v.setModel(modelStr.trim());
        }

        // ha van ownerId meződ a modelben, tartsuk szinkronban
        try {
            v.setOwnerId(customerId); // Vehicle-ben ez a mezőnév: ownerId
        } catch (Throwable ignore) {
            // ha nincs ilyen setter, semmi gond – a DAO paraméterben úgyis megkapja
        }

        // minimális védelem: rendszám ne legyen üres
        if (v.getPlate() == null || v.getPlate().trim().isEmpty()) {
            throw new IllegalArgumentException("A jármű rendszáma (plate) kötelező.");
        }

        if (v.getId() == 0) {
            // ÚJ jármű – FIGYELEM: a DAO szignója (plate, makeModel, customerId)
            int newId = vehicleDao.insert(v.getPlate(), v.getVin(), v.getEngine_no(), v.getBrand(), v.getModel(), v.getYear(), v.getOdometer_km(), v.getFuel_type(), customerId);
            v.setId(newId);
            return newId;
        } else {
            // Meglévő jármű frissítése – szignó: (id, plate, makeModel, customerId)
            vehicleDao.update(v.getId(), v.getPlate(), v.getVin(), v.getEngine_no(), v.getBrand(), v.getModel(), v.getYear(), v.getOdometer_km(), v.getFuel_type(), customerId);
            return v.getId();
        }
    }

}

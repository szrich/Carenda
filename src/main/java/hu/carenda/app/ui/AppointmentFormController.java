package hu.carenda.app.ui;

import hu.carenda.app.model.Appointment;
import hu.carenda.app.model.Customer;
import hu.carenda.app.model.ServiceJobCard;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.AppointmentDao;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import hu.carenda.app.repository.ServiceJobCardDao;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Controller az Időpont (Appointment) űrlap (appointment-form.fxml) kezeléséhez.
 * Felelős az időpontok létrehozásáért, szerkesztéséért, valamint az ügyfél és
 * jármű adatok "on-the-fly" kezeléséért.
 */
public class AppointmentFormController {

    @FXML
    private ComboBox<Customer> customerCombo;
    @FXML
    private TextField phone, email, brand, model, duration;
    @FXML
    private ComboBox<Vehicle> vehicleCombo;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Spinner<Integer> hourSpinner, minuteSpinner;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextArea note;

    // A DAO-k (adatelérési réteg)
    private final AppointmentDao apptDao = new AppointmentDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ServiceJobCardDao jobCardDao = new ServiceJobCardDao();

    /** A szerkesztett időpont. Ha null, akkor új időpontot hozunk létre. */
    private Appointment editing;

    /** Egységes formátum az LocalDateTime tárolásához és olvasásához. */
    private static final DateTimeFormatter LDT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @FXML
    public void initialize() {
        setupCustomerCombo();
        setupVehicleCombo();
        setupStatusCombo();
        setupDateTimePickers();

        // Alapérték a duration-nek, ha üres marad
        if (duration.getText() == null || duration.getText().isBlank()) {
            duration.setText("60");
        }
    }

    /**
     * Beállítja az Ügyfél comboboxot, beleértve a cella kinézetét,
     * a kapcsolódó mezők (telefon, email) frissítését,
     * és az "on-the-fly" ügyfél-létrehozást.
     */
    private void setupCustomerCombo() {
        var customers = FXCollections.observableArrayList(customerDao.findAll());
        customerCombo.setItems(customers);

        // CellFactory: Hogyan nézzen ki a lenyíló lista
        customerCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // ButtonCell: Hogyan nézzen ki a kiválasztott elem
        customerCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Listener: Ha az ügyfél változik, frissítjük a telefon és email mezőket
        customerCombo.valueProperty().addListener((obs, oldCustomer, newCustomer) -> {
            if (newCustomer != null) {
                phone.setText(newCustomer.getPhone());
                email.setText(newCustomer.getEmail());
                // ... és betöltjük a hozzá tartozó járműveket
                var vehicles = FXCollections.observableArrayList(vehicleDao.findByCustomer(newCustomer.getId()));
                vehicleCombo.setItems(vehicles);
                vehicleCombo.getSelectionModel().clearSelection();
            } else {
                phone.clear();
                email.clear();
                vehicleCombo.setItems(FXCollections.observableArrayList());
            }
        });

        // Converter: Kezeli a szöveg beírást (új ügyfél létrehozása)
        customerCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer c) {
                return (c == null || c.getName() == null) ? "" : c.getName();
            }

            @Override
            public Customer fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                // Keresés a meglévők között
                Customer existing = customerCombo.getItems().stream()
                        .filter(x -> s.equalsIgnoreCase(x.getName()))
                        .findFirst()
                        .orElse(null);
                if (existing != null) {
                    return existing;
                }

                // Ha nincs, új létrehozása "on-the-fly"
                Customer nc = new Customer();
                nc.setId(null); // JAVÍTVA: 0 helyett null, jelezve, hogy új
                nc.setName(s.trim());
                if (phone != null) nc.setPhone(phone.getText());
                if (email != null) nc.setEmail(email.getText());

                customerCombo.getItems().add(nc);
                customerCombo.getSelectionModel().select(nc);
                return nc;
            }
        });
    }

    /**
     * Beállítja a Jármű comboboxot (kinézet, kapcsolódó mezők, "on-the-fly" létrehozás).
     */
    private void setupVehicleCombo() {
        // Jármű megjelenítés (Rendszám)
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

        // Listener: Ha a jármű változik, frissítjük a gyártmány/modell mezőket
        vehicleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                brand.setText(newVal.getBrand());
                model.setText(newVal.getModel());
            } else {
                brand.clear();
                model.clear();
            }
        });

        // Converter: Kezeli a szöveg beírást (új jármű létrehozása)
        vehicleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Vehicle v) {
                return (v == null || v.getPlate() == null) ? "" : v.getPlate();
            }

            @Override
            public Vehicle fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                // Keresés a meglévők között (rendszám alapján)
                Vehicle existing = vehicleCombo.getItems().stream()
                        .filter(x -> s.equalsIgnoreCase(x.getPlate()))
                        .findFirst()
                        .orElse(null);
                if (existing != null) {
                    return existing;
                }

                // Új létrehozása "on-the-fly"
                Vehicle nv = new Vehicle();
                nv.setId(null);
                nv.setPlate(s.trim().toUpperCase()); // Rendszámot érdemes nagybetűsíteni
                if (brand != null) nv.setBrand(brand.getText());
                if (model != null) nv.setModel(model.getText());

                vehicleCombo.getItems().add(nv);
                vehicleCombo.getSelectionModel().select(nv);
                return nv;
            }
        });
    }

    /**
     * Beállítja a Státusz comboboxot.
     */
    private void setupStatusCombo() {
        statusCombo.setItems(FXCollections.observableArrayList("TERVEZETT", "BEFEJEZETT", "LEMONDOTT"));
        statusCombo.getSelectionModel().select("TERVEZETT");
    }

    /**
     * Beállítja a dátum és idő választókat (DatePicker, Spinnerek).
     */
    private void setupDateTimePickers() {
        // Dátum + idő alapérték: most
        datePicker.setValue(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();

        // Óra spinner: 0–23
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, now.getHour(), 1));
        hourSpinner.setEditable(true);

        // Perc spinner: 0–59 (5-ös léptékkel kényelmesebb)
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, (now.getMinute() / 5) * 5, 5));
        minuteSpinner.setEditable(true);
    }


    /**
     * A hívó (pl. Dashboard) hívja meg, hogy átadja a szerkesztendő objektumot.
     * @param a A szerkesztendő Appointment, vagy null (ha új).
     */
    public void setEditing(Appointment a) {
        this.editing = a;
        if (a == null) {
            return; // Új időpont, nincs mit betölteni
        }

        // --- Adatok betöltése szerkesztéshez ---

        // 1. Ügyfél kijelölése
        customerCombo.getItems().stream()
                .filter(c -> Objects.equals(c.getId(), a.getCustomerId()))
                .findFirst()
                .ifPresent(customerCombo.getSelectionModel()::select);

        // 2. Járműlista frissítése (a listener miatt ez automatikus)
        // és a Jármű kijelölése
        vehicleCombo.getItems().stream()
                .filter(v -> Objects.equals(v.getId(), a.getVehicleId()))
                .findFirst()
                .ifPresent(vehicleCombo.getSelectionModel()::select);

        // 3. Dátum/idő visszatöltés
        try {
            LocalDateTime ldt = LocalDateTime.parse(a.getStartTs(), LDT_FMT);
            datePicker.setValue(ldt.toLocalDate());
            hourSpinner.getValueFactory().setValue(ldt.getHour());
            minuteSpinner.getValueFactory().setValue(ldt.getMinute());
        } catch (DateTimeParseException | NullPointerException ignore) {
            // ha nem sikerül parzolni, maradnak az alapértékek
        }

        // 4. Többi mező
        duration.setText(String.valueOf(a.getDurationMinutes()));
        note.setText(a.getNote() == null ? "" : a.getNote());
        if (a.getStatus() != null) {
            statusCombo.getSelectionModel().select(a.getStatus());
        }
    }

    @FXML
    public void onSave() {
        try {
            // --- Adatok kiolvasása és validálása ---
            Customer c = customerCombo.getValue();
            Vehicle v = vehicleCombo.getValue();
            LocalDate d = datePicker.getValue();
            Integer hh = hourSpinner.getValue();
            Integer mm = minuteSpinner.getValue();
            String st = statusCombo.getValue();
            String nt = note.getText();

            // Egyszerűsített validáció segédfüggvénnyel
            if (!validateInputs(c, v, d, st)) {
                return; // A validáló már mutatott hibaüzenetet
            }

            int dur;
            try {
                dur = Integer.parseInt(duration.getText().trim());
            } catch (NumberFormatException ex) {
                showWarning("Érvénytelen időtartam", "Az időtartam percben egész szám legyen.");
                return;
            }

            // --- Időpont összeállítása ---
            LocalDateTime ldt = d.atTime(hh, mm);
            LocalDateTime endLdt = ldt.plusMinutes(dur);
            String when = ldt.format(LDT_FMT);

            // --- Üzleti logika ellenőrzés - nyitvatartás - ütközés ---
            if (!checkBusinessHours(ldt, endLdt)) {
                return;
            }
            if (!checkAvailability(ldt, endLdt, (editing != null ? editing.getId() : null))) {
                return;
            }

            // --- Ügyfél, jármű mentése, frissítése ---
            // A DAO hívások már a segédfüggvényekben vannak
            int customerId = upsertCustomer(c, phone.getText(), email.getText());
            int vehicleId = upsertVehicle(v, customerId, brand.getText(), model.getText());

            // --- Időpont mentése - INSERT - UPDATE ---
            boolean isNew = (editing == null) || (editing.getId() == null); 
            if (isNew) {
                apptDao.insert(customerId, vehicleId, when, dur, nt, st);
            } else {
                apptDao.update(editing.getId(), customerId, vehicleId, when, dur, nt, st);
            }

            // --- Ablak bezárása és sikeresség jelzése a Formnak ---
            closeWindow(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Mentési hiba", "Váratlan hiba történt: " + ex.getMessage());
        }
    }

    @FXML
    public void onCancel() {
        closeWindow(false);
    }

    @FXML
    public void onJobCard() {
        try {
            // 1) Kell, hogy legyen elmentett időpont (különben nincs appointment_id)
            if (editing == null || editing.getId() == null) { 
                showWarning("Művelet nem lehetséges", "Előbb mentse el az időpontot, utána lehet munkalapot nyitni.");
                return;
            }

            Integer apptId = editing.getId();
            Integer custId = editing.getCustomerId();
            Integer vehId = editing.getVehicleId();

            if (custId == null || vehId == null) {
                showError("Hiányzó adatok", "Az időponthoz nincs ügyfél vagy jármű rendelve.");
                return;
            }

            // 2) Megnézzük, van-e már munkalap ehhez az appointmenthez
            ServiceJobCard cardToUse = jobCardDao.findByAppointmentId(apptId);

            if (cardToUse == null) {
                // 3a) Új munkalap előkészítése
                cardToUse = new ServiceJobCard();
                cardToUse.setAppointment_id(apptId);
                cardToUse.setCustomer_id(custId);
                cardToUse.setVehicle_id(vehId);
                cardToUse.setStatus("OPEN");
                cardToUse.setCreated_at(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            // 3b) Ha már létezik (cardToUse != null), azt fogjuk szerkeszteni.

            // 4) Szükséges kontextus objektumok betöltése
            var custObj = customerDao.findById(custId);
            var vehObj = vehicleDao.findById(vehId);

            // 5) Ablak megnyitása a refaktorált Forms.java segítségével
            // 'owner' ablak átadása és a Stage kezelés eltávolítása
            Window owner = customerCombo.getScene().getWindow();
            
            // Ez a hívás megnyitja az ablakot, modálisan, és megvárja, míg bezárul.
            Forms.serviceJobCard(owner, cardToUse, custObj, vehObj);

            // 6) (Opcionális) Frissítés a munkalap bezárása után
            // ... (pl. státusz frissítése, ha a munkalap állapota változott)

        } catch (Exception e) {
            e.printStackTrace();
            showError("Hiba a munkalap megnyitásakor", e.getMessage());
        }
    }

    // --- Validációs és Üzleti Logikai Segédfüggvények ---

    /**
     * Ellenőrzi a kötelező mezőket.
     * @return true, ha minden rendben, false, ha hiányos.
     */
    private boolean validateInputs(Customer c, Vehicle v, LocalDate d, String st) {
        if (c == null) {
            showWarning("Hiányzó adat", "Válasszon, vagy írjon be új ügyfelet.");
            return false;
        }
        if (v == null) {
            showWarning("Hiányzó adat", "Válasszon, vagy írjon be új járművet.");
            return false;
        }
        if (d == null) {
            showWarning("Hiányzó adat", "Válasszon dátumot.");
            return false;
        }
        if (st == null || st.isBlank()) {
            showWarning("Hiányzó adat", "Válasszon státuszt.");
            return false;
        }
        return true;
    }

    /**
     * Ellenőrzi, hogy az időpont nyitvatartási időn (8-17) belül van-e.
     */
    private boolean checkBusinessHours(LocalDateTime start, LocalDateTime end) {
        LocalTime openTime = LocalTime.of(8, 0);
        LocalTime closeTime = LocalTime.of(17, 0);

        if (start.toLocalTime().isBefore(openTime) || start.toLocalTime().isAfter(closeTime)) {
             showWarning("Nyitvatartási időn kívül", "Időpont csak 08:00 és 17:00 között rögzíthető.");
             return false;
        }
        
        if (end.toLocalTime().isAfter(closeTime)) {
            showWarning("Nyitvatartási időn kívül",
                    "A munka vége ( " + end.toLocalTime() + " ) kilógna 17:00 után.\n"
                    + "Válasszon korábbi időpontot vagy rövidebb időtartamot.");
            return false;
        }
        return true;
    }

    /**
     * Ellenőrzi a kapacitást (max 2 átfedés) az adott napon.
     * @param newStart Az új/módosított időpont kezdete.
     * @param newEnd Az új/módosított időpont vége.
     * @param editingId A szerkesztett időpont ID-ja (vagy null), hogy ne önmagával ütközzön.
     * @return true, ha van szabad kapacitás, false, ha tele van.
     */
    private boolean checkAvailability(LocalDateTime newStart, LocalDateTime newEnd, Integer editingId) {
        String dayIso = newStart.toLocalDate().toString(); // "YYYY-MM-DD"
        var sameDayAppointments = apptDao.findForDay(dayIso);

        int overlaps = 0;
        for (var ap : sameDayAppointments) {

            // ha épp UPDATE van, ne számoljuk bele saját magát:
            if (editingId != null && Objects.equals(ap.getId(), editingId)) { 
                continue;
            }

            try {
                LocalDateTime apStart = LocalDateTime.parse(ap.getStartTs(), LDT_FMT);
                LocalDateTime apEnd = apStart.plusMinutes(ap.getDurationMinutes());

                // Intervallum átfedés ellenőrzés: (apStart < newEnd) && (apEnd > newStart)
                boolean overlap = apStart.isBefore(newEnd) && apEnd.isAfter(newStart);

                if (overlap) {
                    overlaps++;
                }
            } catch (DateTimeParseException e) {
                // Hibás adat az adatbázisban, hagyjuk figyelmen kívül
            }
        }

        // ha már kettő másik van, akkor a mostani lenne a 3. => STOP
        if (overlaps >= 2) {
            showWarning("Nincs szabad kapacitás",
                    "Ebben az időszakban már van két lefoglalt munka.\n"
                    + "Válasszon másik időpontot.");
            return false;
        }
        return true;
    }


    // --- DAO Segédfüggvények (Upsert) ---

    /**
     * Létrehoz egy új ügyfelet, vagy frissíti a meglévőt az űrlap adatai alapján.
     * @return Az ügyfél ID-ja (akár új, akár meglévő).
     */
    private int upsertCustomer(Customer c, String phoneStr, String emailStr) {
        if (c == null) throw new IllegalArgumentException("Az ügyfél nem lehet null.");
        
        c.setPhone(phoneStr != null ? phoneStr.trim() : c.getPhone());
        c.setEmail(emailStr != null ? emailStr.trim() : c.getEmail());

        if (c.getId() == null) { 
            int newId = customerDao.insert(c.getName(), c.getPhone(), c.getEmail());
            c.setId(newId);
            return newId;
        } else {
            customerDao.update(c.getId(), c.getName(), c.getPhone(), c.getEmail());
            return c.getId();
        }
    }

    /**
     * Létrehoz egy új járművet, vagy frissíti a meglévőt az űrlap adatai alapján.
     * @return A jármű ID-ja (akár új, akár meglévő).
     */
    private int upsertVehicle(Vehicle v, int customerId, String brandStr, String modelStr) {
        if (v == null) throw new IllegalArgumentException("A jármű nem lehet null.");
        if (v.getPlate() == null || v.getPlate().trim().isEmpty()) {
            throw new IllegalArgumentException("A jármű rendszáma (plate) kötelező.");
        }

        // Frissítés az űrlap mezőiből, ha ki vannak töltve
        if (brandStr != null && !brandStr.trim().isEmpty()) v.setBrand(brandStr.trim());
        if (modelStr != null && !modelStr.trim().isEmpty()) v.setModel(modelStr.trim());
        v.setOwnerId(customerId); // Mindig frissítjük a tulajdonost

        if (v.getId() == null) { 
            int newId = vehicleDao.insert(v.getPlate(), v.getVin(), v.getEngine_no(), v.getBrand(), v.getModel(), v.getYear(), v.getFuel_type(), customerId);
            v.setId(newId);
            return newId;
        } else {
            vehicleDao.update(v.getId(), v.getPlate(), v.getVin(), v.getEngine_no(), v.getBrand(), v.getModel(), v.getYear(), v.getFuel_type(), customerId);
            return v.getId();
        }
    }

    // --- UI Segédfüggvények ---

    /**
     * Bezárja az űrlap ablakát, és beállítja az 'UserData'-t (siker/mégse).
     * @param saved true, ha a mentés sikeres volt, false, ha 'Mégse'.
     */
    private void closeWindow(boolean saved) {
        Stage stage = (Stage) customerCombo.getScene().getWindow();
        stage.setUserData(saved); // Ezt olvassa ki a Forms.java
        stage.close();
    }

    /** Egységesített hibaüzenet (Warning). */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** Egységesített hibaüzenet (Error). */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

package hu.carenda.app.ui;

import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import hu.carenda.app.model.Appointment;
import hu.carenda.app.model.ServiceJobCard;
import hu.carenda.app.model.User;
import hu.carenda.app.repository.AppointmentDao;
import hu.carenda.app.repository.ServiceJobCardDao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A fő Dashboard (műszerfal) vezérlője.
 * Kezeli a tabokat (Ügyfelek, Járművek, Időpontok, Munkalapok),
 * a táblázatokat, keresést és az űrlapok megnyitását.
 */
public class DashboardFormController {

    // --- FXML Változók ---

    @FXML
    private BorderPane scheduleRoot; // schedule.fxml root node-ja
    @FXML
    private Tab scheduleTab; // Naptár fül
    @FXML
    private TabPane tabPane;
    @FXML
    private Label headerLabel;
    @FXML
    private Button usersButton; // "Felhasználók" gomb (csak adminnak látszódik)

    // Ügyfél fül
    @FXML
    private TextField customerSearch;
    @FXML
    private TableView<Customer> customerTable;
    @FXML
    private TableColumn<Customer, Integer> cId;
    @FXML
    private TableColumn<Customer, String> cName, cPhone, cEmail;

    // Jármű fül
    @FXML
    private TextField vehicleSearch;
    @FXML
    private TableView<Vehicle> vehicleTable;
    @FXML
    private TableColumn<Vehicle, Integer> vId, vYear;
    @FXML
    private TableColumn<Vehicle, String> vPlate, vVin, vEngine_no, vBrand, vModel, vFuel_type, vOwner;

    // Időpont fül
    @FXML
    private TextField apptSearch;
    @FXML
    private TableView<Appointment> apptTable;
    @FXML
    private TableColumn<Appointment, Integer> aId, aDuration;
    @FXML
    private TableColumn<Appointment, String> aWhen, aCustomer, aVehicle, aStatus, aNote;

    // Munkalap fül
    @FXML
    private TextField jobCardSearch;
    @FXML
    private TableView<ServiceJobCard> jobCardTable;
    @FXML
    private TableColumn<ServiceJobCard, Integer> sId;
    @FXML
    private TableColumn<ServiceJobCard, String> sJobcardNo, sCreatedAt, sPlate, sBrand, sModel, sCustomer, sStatus;

    // Beágyazott Controller (schedule.fxml)
    @FXML
    private ScheduleFormController scheduleRootController;

    // --- DAO + Adatlisták ---

    private final AppointmentDao apptDao = new AppointmentDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ServiceJobCardDao sjcDao = new ServiceJobCardDao();

    private final ObservableList<Appointment> appointments = FXCollections.observableArrayList();
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private final ObservableList<ServiceJobCard> serviceJobCards = FXCollections.observableArrayList();

    /** Az aktuálisan belépett felhasználó, a LoginController állítja be. */
    private User currentUser;

    /**
     * Az FXML betöltése után hívódik meg. Beállítja a táblázatokat és a listenereket.
     */
    @FXML
    public void initialize() {
        if (headerLabel != null) {
            headerLabel.setText("Carenda");
        }
        if (usersButton != null) {
            usersButton.setVisible(false); // Alapból rejtett, amíg nem tudjuk a jogosultságot
            usersButton.setManaged(false);
        }

        setupCustomerTable();
        setupVehicleTable();
        setupAppointmentTable();
        setupJobCardTable();

        // Keresés Enter gombra
        customerSearch.setOnAction(e -> onCustomerSearch());
        vehicleSearch.setOnAction(e -> onVehicleSearch());
        apptSearch.setOnAction(e -> onApptSearch());
        jobCardSearch.setOnAction(e -> onJobCardSearch());

        // Tab váltáskor frissítünk mindent
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == null) return;
            
            hardRefreshAll();
            
            if (newTab == scheduleTab && scheduleRootController != null) {
                scheduleRootController.hardRefreshSchedule();
            }
        });

        // Kezdeti adatbetöltés
        hardRefreshAll();
    }

    /**
     * Beállítja az Ügyfél táblázat oszlopait.
     */
    private void setupCustomerTable() {
        cId.setCellValueFactory(d -> d.getValue().idProperty());
        cName.setCellValueFactory(d -> d.getValue().nameProperty());
        cPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
        cEmail.setCellValueFactory(d -> d.getValue().emailProperty());
        customerTable.setItems(customers);
    }

    /**
     * Beállítja a Jármű táblázat oszlopait.
     */
    private void setupVehicleTable() {
        vId.setCellValueFactory(d -> d.getValue().idProperty());
        vPlate.setCellValueFactory(d -> d.getValue().plateProperty());
        vVin.setCellValueFactory(d -> d.getValue().vinProperty());
        vEngine_no.setCellValueFactory(d -> d.getValue().engine_noProperty());
        vBrand.setCellValueFactory(d -> d.getValue().brandProperty());
        vModel.setCellValueFactory(d -> d.getValue().modelProperty());
        vYear.setCellValueFactory(d -> d.getValue().yearProperty());
        vFuel_type.setCellValueFactory(d -> d.getValue().fuel_typeProperty());
        vOwner.setCellValueFactory(d -> d.getValue().ownerNameProperty());
        vehicleTable.setItems(vehicles);
    }

    /**
     * Beállítja az Időpont táblázat oszlopait.
     */
    private void setupAppointmentTable() {
        aId.setCellValueFactory(d -> d.getValue().idProperty());
        aWhen.setCellValueFactory(d -> d.getValue().startTsProperty());
        aDuration.setCellValueFactory(d -> d.getValue().durationMinutesProperty());
        aCustomer.setCellValueFactory(d -> d.getValue().ownerNameProperty());
        aVehicle.setCellValueFactory(d -> d.getValue().vehiclePlateProperty());
        aStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        aNote.setCellValueFactory(d -> d.getValue().noteProperty());
        apptTable.setItems(appointments);
    }

    /**
     * Beállítja a Munkalap táblázat oszlopait.
     */
    private void setupJobCardTable() {
        sId.setCellValueFactory(d -> d.getValue().idProperty());
        sJobcardNo.setCellValueFactory(d -> d.getValue().jobcard_noProperty());
        sCreatedAt.setCellValueFactory(d -> d.getValue().created_atProperty());
        sPlate.setCellValueFactory(d -> d.getValue().plateProperty());
        sBrand.setCellValueFactory(d -> d.getValue().brandProperty());
        sModel.setCellValueFactory(d -> d.getValue().modelProperty());
        sCustomer.setCellValueFactory(d -> d.getValue().ownerNameProperty());
        sStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        jobCardTable.setItems(serviceJobCards);
    }

    /**
     * A LoginController hívja meg, hogy átadja a belépett felhasználót.
     * Beállítja a UI-t a felhasználó jogosultságai alapján (pl. Admin gomb).
     * @param u A belépett User objektum.
     */
    public void setCurrentUser(User u) {
        this.currentUser = u;

        // 1) Fejléc szöveg beállítása
        if (headerLabel != null) {
            String displayName = (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getUsername();
            String role = (u.getRoleName() != null && !u.getRoleName().isBlank()) ? u.getRoleName() : "USER";
            headerLabel.setText("Carenda \u2013 " + displayName + " (" + role + ")");
        }

        // 2) Admin gomb láthatósága
        if (usersButton != null) {
            boolean isAdmin = u.getRoleName() != null && u.getRoleName().equalsIgnoreCase("ADMIN");
            usersButton.setVisible(isAdmin);
            usersButton.setManaged(isAdmin);
        }
    }

    // --- Adatfrissítési Metódusok ---

    /**
     * Újratölti az összes táblázat tartalmát az adatbázisból,
     * figyelembe véve az aktuális keresőmező-értékeket.
     */
    private void hardRefreshAll() {
        onCustomerSearch();
        onVehicleSearch();
        onApptSearch();
        onJobCardSearch();
    }

    private void refreshCustomers() {
        customers.setAll(customerDao.findAll());
    }

    private void refreshVehicles() {
        vehicles.setAll(vehicleDao.findAllWithOwner());
    }

    private void refreshAppointments() {
        appointments.setAll(apptDao.findAll());
    }

    private void refreshJobCards() {
        serviceJobCards.setAll(sjcDao.findAllWithOwnerAndVehicleData());
    }

    // --- Eseménykezelők (FXML) ---

    // --- ÜGYFELEK ---

    @FXML
    public void onCustomerSearch() {
        String q = (customerSearch.getText() == null) ? "" : customerSearch.getText().trim();
        customers.setAll(q.isEmpty() ? customerDao.findAll() : customerDao.search(q));
    }

    @FXML
    public void onCustomerNew() {
        // JAVÍTVA: Átadjuk az 'owner' ablakot
        Window owner = customerTable.getScene().getWindow();
        boolean saved = Forms.customer(owner, null);
        
        if (saved) {
            refreshCustomers();
            refreshVehicles(); // Járműveket is, mert lehet, hogy új tulajdonoshoz tartoznak
        }
    }

    @FXML
    public void onCustomerEdit() {
        var sel = customerTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Nincs kijelölés", "Válasszon ügyfelet a listából!");
            return;
        }
        
        // Átadjuk az 'owner' ablakot
        Window owner = customerTable.getScene().getWindow();
        boolean saved = Forms.customer(owner, sel);
        
        if (saved) {
            refreshCustomers();
            refreshVehicles();
        }
    }

    @FXML
    public void onCustomerDelete() {
        var sel = customerTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Nincs kijelölés", "Válasszon ügyfelet a törléshez!");
            return;
        }

        boolean confirmed = showConfirmation(
                "Törlés megerősítése",
                "Biztosan törli: " + sel.getName() + "?\n(A járművei nem törlődnek, csak gazdátlanok lesznek.)"
        );
        
        if (confirmed) {
            try {
                customerDao.delete(sel.getId());
                refreshCustomers();
                refreshVehicles(); // Járműlista frissítése, mert az 'owner' név eltűnik
            } catch (Exception e) {
                showError("Törlési hiba", "Adatbázis hiba történt: " + e.getMessage());
            }
        }
    }

    // --- JÁRMŰVEK ---

    @FXML
    public void onVehicleSearch() {
        String q = (vehicleSearch.getText() == null) ? "" : vehicleSearch.getText().trim();
        vehicles.setAll(q.isEmpty() ? vehicleDao.findAllWithOwner() : vehicleDao.searchWithOwner(q));
    }

    @FXML
    public void onVehicleNew() {
        // Átadjuk az 'owner' ablakot
        Window owner = vehicleTable.getScene().getWindow();
        boolean saved = Forms.vehicle(owner, null);
        
        if (saved) {
            refreshVehicles();
        }
    }

    @FXML
    public void onVehicleEdit() {
        var sel = vehicleTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Nincs kijelölés", "Válasszon járművet a listából!");
            return;
        }
        
        // Átadjuk az 'owner' ablakot
        Window owner = vehicleTable.getScene().getWindow();
        boolean saved = Forms.vehicle(owner, sel);
        
        if (saved) {
            refreshVehicles();
        }
    }

    @FXML
    public void onVehicleDelete() {
        var sel = vehicleTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Nincs kijelölés", "Válasszon járművet a törléshez!");
            return;
        }

        boolean confirmed = showConfirmation(
                "Törlés megerősítése",
                "Biztosan törli a járművet: " + sel.getPlate() + "?"
        );
        
        if (confirmed) {
            try {
                vehicleDao.delete(sel.getId());
                refreshVehicles();
            } catch (Exception e) {
                showError("Törlési hiba", "Adatbázis hiba történt: " + e.getMessage());
            }
        }
    }

    // --- IDŐPONTOK ---

    @FXML
    public void onApptSearch() {
        String q = (apptSearch.getText() == null) ? "" : apptSearch.getText().trim();
        appointments.setAll(q.isEmpty() ? apptDao.findAll() : apptDao.search(q));
    }

    @FXML
    public void onApptNew() {
        var a = new Appointment();
        // Alapértékek beállítása
        a.setStartTs(LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0).toString());
        a.setDurationMinutes(60);
        a.setStatus("TERVEZETT");

        // Átadjuk az 'owner' ablakot
        Window owner = tabPane.getScene().getWindow();
        boolean saved = Forms.openAppointmentDialog(owner, a);
        
        if (saved) {
            onApptSearch();
        }
    }

    @FXML
    public void onApptEdit() {
        var sel = apptTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Nincs kijelölés", "Válasszon időpontot!");
            return;
        }

        // Másolat készítése szerkesztéshez
        var editing = new Appointment();
        editing.setId(sel.getId());
        editing.setCustomerId(sel.getCustomerId());
        editing.setVehicleId(sel.getVehicleId());
        editing.setStartTs(sel.getStartTs());
        editing.setDurationMinutes(sel.getDurationMinutes());
        editing.setNote(sel.getNote());
        editing.setStatus(sel.getStatus());

        // Átadjuk az 'owner' ablakot
        Window owner = apptTable.getScene().getWindow();
        boolean saved = Forms.openAppointmentDialog(owner, editing);
        
        if (saved) {
            onApptSearch();
        }
    }

    @FXML
    public void onApptDelete() {
        var sel = apptTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Nincs kijelölés", "Válasszon időpontot a törléshez!");
            return;
        }

        boolean confirmed = showConfirmation(
                "Törlés megerősítése",
                "Törlöd az időpontot: " + sel.getOwnerName() + " – " + sel.getVehiclePlate() + " (" + sel.getStartTs() + ")?"
        );
        
        if (confirmed) {
            try {
                apptDao.delete(sel.getId());
                refreshAppointments();
            } catch (Exception e) {
                showError("Törlési hiba", "Adatbázis hiba történt: " + e.getMessage());
            }
        }
    }

    // --- MUNKALAPOK ---

    @FXML
    public void onJobCardSearch() {
        String q = (jobCardSearch.getText() == null) ? "" : jobCardSearch.getText().trim();
        serviceJobCards.setAll(q.isEmpty() ? sjcDao.findAllWithOwnerAndVehicleData() : sjcDao.searchWithOwnerAndVehicleData(q));
    }

    @FXML
    public void onJobCardEdit() {
        var sel = jobCardTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo("Nincs kijelölés", "Válasszon ki egy munkalapot a listából!");
            return;
        }

        try {
            // Töltsük be a hozzátartozó ügyfelet és járművet
            Customer custObj = null;
            Vehicle vehObj = null;

            if (sel.getCustomer_id() != null) {
                custObj = customerDao.findById(sel.getCustomer_id());
            }
            if (sel.getVehicle_id() != null) {
                vehObj = vehicleDao.findById(sel.getVehicle_id());
            }
            
            // Átadjuk az 'owner' ablakot, és a 'Forms' kezeli a megjelenítést
            Window owner = jobCardTable.getScene().getWindow();
            boolean saved = Forms.serviceJobCard(owner, sel, custObj, vehObj);

            if (saved) {
                refreshJobCards();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Hiba a munkalap megnyitásakor", "Hiba: " + ex.getMessage());
        }
    }

    // --- ADMIN ÉS RENDSZER FUNKCIÓK ---

    @FXML
    public void onOpenUsersAdmin() {
        // Jogosultság ellenőrzés
        if (currentUser == null || !currentUser.getRoleName().equalsIgnoreCase("ADMIN")) {
            showError("Nincs jogosultság", "Ez a funkció csak ADMIN felhasználóknak elérhető.");
            return;
        }

        try {
            // Ez a metódus NEM a Forms.java-t használja, hanem saját logikája van.
            // A konzisztencia érdekében át lehetne mozgatni a Forms.java-ba,
            // de a működés szempontjából így is helyes.
            FXMLLoader fxml = new FXMLLoader(
                    Objects.requireNonNull(
                            getClass().getResource("/hu/carenda/app/views/users-admin.fxml"),
                            "Nem található: /hu/carenda/app/views/users-admin.fxml"
                    )
            );
            Parent root = fxml.load();

            UsersAdminFormController ctrl = fxml.getController();
            ctrl.loadUsersTable();

            Stage dlg = new Stage();
            dlg.setTitle("Felhasználókezelés");
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.initOwner(usersButton.getScene().getWindow());
            dlg.setScene(new Scene(root, 500, 400));
            dlg.setResizable(false);
            dlg.showAndWait();

        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            showError("Hiba az űrlap betöltésekor", "Nem található a 'users-admin.fxml' fájl.");
        }
    }

    @FXML
    public void onLogout() {
        // Kilépés helyett a Login képernyőre "visszadobás" (Scene csere)
        try {
            Stage stage = (Stage) tabPane.getScene().getWindow();
            
            Parent root = FXMLLoader.load(
                Objects.requireNonNull(
                    getClass().getResource("/hu/carenda/app/views/login.fxml"),
                    "Nem találom a login.fxml erőforrást."
                )
            );
            
            stage.setTitle("Carenda – Bejelentkezés");
            stage.setScene(new Scene(root, 420, 260));
            // A 'show()' nem kell, a Stage már látható, csak a Scene-t cseréljük.

        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            showError("Kilépési hiba", "Nem sikerült visszatölteni a Login képernyőt.");
        }
    }

    // --- UI Segédfüggvények (Alerts) ---

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}

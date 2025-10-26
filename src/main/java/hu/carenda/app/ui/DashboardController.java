package hu.carenda.app.ui;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import hu.carenda.app.model.Appointment;
import hu.carenda.app.model.ServiceJobCard;
import hu.carenda.app.model.User;
import hu.carenda.app.repository.AppointmentDao;
import hu.carenda.app.repository.ServiceJobCardDao;
import java.util.Objects;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class DashboardController {

    // ez az aktuálisan belépett felhasználó, LoginController-ből kapjuk
    private User currentUser;

    @FXML
    private Tab customersTab, vehiclesTab, appointmentsTab, jobcardsTab;

    @FXML
    private Label headerLabel;

    @FXML
    private Tab scheduleTab;  // hogy tudjuk azonosítani a tabot tabPane listenerben

    @FXML
    private BorderPane scheduleRoot; // ez a schedule.fxml root node-ja

    @FXML
    private ScheduleController scheduleRootController; // ez maga a ScheduleController példány

    @FXML
    private Button usersButton; // "Felhasználók" gomb (csak adminnak látszódik)

    @FXML
    private TextField apptSearch;
    @FXML
    private TableView<hu.carenda.app.model.Appointment> apptTable;
    @FXML
    private TableColumn<hu.carenda.app.model.Appointment, Number> aId, aDuration;
    @FXML
    private TableColumn<hu.carenda.app.model.Appointment, String> aWhen, aCustomer, aVehicle, aStatus, aNote;

    @FXML
    private TextField customerSearch, vehicleSearch, jobCardSearch;
    @FXML
    private TableView<Customer> customerTable;
    @FXML
    private TableColumn<Customer, Number> cId;
    @FXML
    private TableColumn<Customer, String> cName, cPhone, cEmail;

    @FXML
    private TableView<Vehicle> vehicleTable;
    @FXML
    private TableColumn<Vehicle, Number> vId, vYear;
    @FXML
    private TableColumn<Vehicle, String> vPlate, vVin, vEngine_no, vBrand, vModel, vFuel_type, vOwner;

    @FXML
    private TableColumn<ServiceJobCard, Number> sId;

    @FXML
    private TableColumn<ServiceJobCard, String> sJobcardNo, sCreatedAt, sPlate, sBrand, sModel, sCustomer, sStatus;

    @FXML
    private TableView<ServiceJobCard> jobCardTable;

    @FXML
    private TabPane tabPane;

    // ---- DAO + listák ----
    private final AppointmentDao apptDao = new AppointmentDao();
    private final javafx.collections.ObservableList<hu.carenda.app.model.Appointment> appointments = javafx.collections.FXCollections.observableArrayList();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ServiceJobCardDao sjcDao = new ServiceJobCardDao();
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private final ObservableList<ServiceJobCard> serviceJobCards = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        if (headerLabel != null) {
            headerLabel.setText("Carenda");
        }
        if (usersButton != null) {
            // induláskor rejtsük el, amíg nem tudjuk, hogy admin-e
            usersButton.setVisible(false);
            usersButton.setManaged(false);
        }

        // Ügyfél oszlopok
        cId.setCellValueFactory(d -> d.getValue().idProperty());
        cName.setCellValueFactory(d -> d.getValue().nameProperty());
        cPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
        cEmail.setCellValueFactory(d -> d.getValue().emailProperty());
        customerTable.setItems(customers);

        // Jármű oszlopok
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

        refreshCustomers();
        refreshVehicles();
        System.out.println("[DBG] DashboardController initialized");

        // Időpontok oszlopok
        aId.setCellValueFactory(d -> d.getValue().idProperty());
        aWhen.setCellValueFactory(d -> d.getValue().startTsProperty());
        aDuration.setCellValueFactory(d -> d.getValue().durationMinutesProperty());
        aCustomer.setCellValueFactory(d -> d.getValue().ownerNameProperty());
        aVehicle.setCellValueFactory(d -> d.getValue().vehiclePlateProperty());
        aStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        aNote.setCellValueFactory(d -> d.getValue().noteProperty());
        apptTable.setItems(appointments);

        // Munkalap oszlopok
        refreshJobCards();
        sId.setCellValueFactory(d -> d.getValue().idProperty());
        sJobcardNo.setCellValueFactory(d -> d.getValue().jobcard_noProperty());
        sCreatedAt.setCellValueFactory(d -> d.getValue().created_atProperty());
        sPlate.setCellValueFactory(d -> d.getValue().plateProperty());
        sBrand.setCellValueFactory(d -> d.getValue().brandProperty());
        sModel.setCellValueFactory(d -> d.getValue().modelProperty());
        sCustomer.setCellValueFactory(d -> d.getValue().ownerNameProperty());
        sStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        jobCardTable.setItems(serviceJobCards);

        // Keresés Enterrel
        customerSearch.setOnAction(e -> onCustomerSearch());
        vehicleSearch.setOnAction(e -> onVehicleSearch());
        apptSearch.setOnAction(e -> onApptSearch());
        jobCardSearch.setOnAction(e -> onJobCardSearch());

        // Kezdeti töltés
        refreshAppointments();

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == null) {
                return;
            }
            // Mindent újratöltünk friss DB-adattal
            hardRefreshAll();
            // ha épp a naptár tabra léptünk, frissítsük a naptárt is
            if (newTab == scheduleTab && scheduleRootController != null) {
                scheduleRootController.hardRefreshSchedule();
            }
        });

    }

    private void hardRefreshAll() {
        // ÜGYFELEK
        String custQ = (customerSearch.getText() == null) ? "" : customerSearch.getText().trim();
        if (custQ.isEmpty()) {
            refreshCustomers();
        } else {
            customers.setAll(customerDao.search(custQ));
        }

        // JÁRMŰVEK
        String vehQ = (vehicleSearch.getText() == null) ? "" : vehicleSearch.getText().trim();
        if (vehQ.isEmpty()) {
            refreshVehicles();
        } else {
            vehicles.setAll(vehicleDao.searchWithOwner(vehQ));
        }

        // IDŐPONTOK
        String apptQ = (apptSearch.getText() == null) ? "" : apptSearch.getText().trim();
        if (apptQ.isEmpty()) {
            refreshAppointments();
        } else {
            appointments.setAll(apptDao.search(apptQ));
        }

        // MUNKALAPOK
        String sjcQ = (jobCardSearch.getText() == null) ? "" : jobCardSearch.getText().trim();
        if (sjcQ.isEmpty()) {
            refreshJobCards();
        } else {
            serviceJobCards.setAll(sjcDao.searchWithOwnerAndVehicleData(sjcQ));
        }
    }

    public void setCurrentUser(User u) {
        this.currentUser = u;

        // 1) fejléc szöveg beállítása
        if (headerLabel != null) {
            String displayName;
            if (u.getFullName() != null && !u.getFullName().isBlank()) {
                displayName = u.getFullName();
            } else {
                displayName = u.getUsername();
            }

            String role = (u.getRoleName() != null && !u.getRoleName().isBlank())
                    ? u.getRoleName()
                    : "USER";

            // pl: "Carenda – Rendszergazda (ADMIN)" vagy "Carenda – Géza (USER)"
            headerLabel.setText("Carenda \u2013 " + displayName + " (" + role + ")");
        }

        // 2) admin gomb láthatósága
        if (usersButton != null) {
            boolean isAdmin = u.getRoleName() != null && u.getRoleName().equalsIgnoreCase("ADMIN");
            usersButton.setVisible(isAdmin);
            usersButton.setManaged(isAdmin);
        }
    }

    @FXML
    private void onOpenUsersAdmin() {
        // csak adminnak engedjük
        if (currentUser == null
                || currentUser.getRoleName() == null
                || !currentUser.getRoleName().equalsIgnoreCase("ADMIN")) {
            return;
        }

        try {
            FXMLLoader fxml = new FXMLLoader(
                    Objects.requireNonNull(
                            getClass().getResource("/hu/carenda/app/views/users-admin.fxml"),
                            "Nem található: /hu/carenda/app/views/users-admin.fxml"
                    )
            );
            Parent root = fxml.load();

            UsersAdminController ctrl = fxml.getController();
            ctrl.loadUsersTable();

            Stage dlg = new Stage();
            dlg.setTitle("Felhasználókezelés");
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.initOwner(usersButton.getScene().getWindow());
            dlg.setScene(new Scene(root, 500, 400));
            dlg.setResizable(false);
            dlg.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            // ide lehet majd Alert, ha akarsz
        }
    }

    private void refreshCustomers() {
        customers.setAll(customerDao.findAll());
    }

    private void refreshVehicles() {
        vehicles.setAll(vehicleDao.findAllWithOwner());
    }

    private void refreshJobCards() {
        serviceJobCards.setAll(sjcDao.findAllWithOwnerAndVehicleData());
    }

    // ---- ÜGYFELEK ----
    @FXML
    public void onCustomerSearch() {
        System.out.println("[DBG] onCustomerSearch");
        String q = customerSearch.getText().trim();
        customers.setAll(q.isEmpty() ? customerDao.findAll() : customerDao.search(q));
    }

    @FXML
    public void onCustomerNew() {
        System.out.println("[DBG] onCustomerNew");
        openCustomerForm(null);
    }

    @FXML
    public void onCustomerEdit() {
        System.out.println("[DBG] onCustomerEdit");
        var sel = customerTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            openCustomerForm(sel);
        } else {
            new Alert(Alert.AlertType.INFORMATION, "Válassz ügyfelet a listából!").showAndWait();
        }
    }

    @FXML
    public void onCustomerDelete() {
        System.out.println("[DBG] onCustomerDelete");
        var sel = customerTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION, "Válassz ügyfelet a törléshez!").showAndWait();
            return;
        }
        var ok = new Alert(Alert.AlertType.CONFIRMATION,
                "Törlöd: " + sel.getName() + "?", ButtonType.OK, ButtonType.CANCEL).showAndWait();
        if (ok.isPresent() && ok.get() == ButtonType.OK) {
            customerDao.delete(sel.getId());
            refreshCustomers();
            refreshVehicles(); // ha volt járműve, listák frissítése
        }
    }

    private void openCustomerForm(Customer editing) {
        var dlg = Forms.customer(editing);
        dlg.initOwner(customerTable.getScene().getWindow());
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.showAndWait();
        refreshCustomers();
        refreshVehicles();
    }

    // ---- JÁRMŰVEK ----
    @FXML
    public void onVehicleSearch() {
        System.out.println("[DBG] onVehicleSearch");
        String q = vehicleSearch.getText().trim();
        vehicles.setAll(q.isEmpty() ? vehicleDao.findAllWithOwner() : vehicleDao.searchWithOwner(q));
    }

    @FXML
    public void onVehicleNew() {
        System.out.println("[DBG] onVehicleNew");
        openVehicleForm(null);
    }

    @FXML
    public void onVehicleEdit() {
        System.out.println("[DBG] onVehicleEdit");
        var sel = vehicleTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            openVehicleForm(sel);
        } else {
            new Alert(Alert.AlertType.INFORMATION, "Válassz járművet a listából!").showAndWait();
        }
    }

    @FXML
    public void onVehicleDelete() {
        System.out.println("[DBG] onVehicleDelete");
        var sel = vehicleTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION, "Válassz járművet a törléshez!").showAndWait();
            return;
        }
        var ok = new Alert(Alert.AlertType.CONFIRMATION,
                "Törlöd a járművet: " + sel.getPlate() + "?", ButtonType.OK, ButtonType.CANCEL).showAndWait();
        if (ok.isPresent() && ok.get() == ButtonType.OK) {
            vehicleDao.delete(sel.getId());
            refreshVehicles();
        }
    }

    private void openVehicleForm(Vehicle editing) {
        var dlg = Forms.vehicle(editing);
        dlg.initOwner(vehicleTable.getScene().getWindow());
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.showAndWait();
        refreshVehicles();
    }

    private void refreshAppointments() {
        appointments.setAll(apptDao.findAll());
    }

    // ---- IDŐPONTOK ----
    @FXML
    public void onApptSearch() {
        String q = apptSearch.getText().trim();
        appointments.setAll(q.isEmpty() ? apptDao.findAll() : apptDao.search(q));
    }

    @FXML
    public void onApptNew() {
        var a = new Appointment();
        a.setStartTs(java.time.LocalDateTime.now().withHour(10).withMinute(0).toString());
        a.setDurationMinutes(60);
        a.setStatus("TERVEZETT");

        boolean saved = Forms.openAppointmentDialog(a, tabPane.getScene().getWindow());
        if (saved) {
            onApptSearch(); // a keresőmező aktuális szövege szerint frissít
            // vagy: refreshAppointments();  // ha mindig az összeset akarod
        }
    }

    @FXML
    public void onApptEdit() {
        var sel = apptTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Válassz időpontot!").showAndWait();
            return;
        }

        // Editálás: egyszerű megoldás – új példány, azonos értékekkel
        var editing = new hu.carenda.app.model.Appointment();
        editing.setId(sel.getId());
        editing.setCustomerId(sel.getCustomerId());
        editing.setVehicleId(sel.getVehicleId());
        editing.setStartTs(sel.getStartTs());
        editing.setDurationMinutes(sel.getDurationMinutes());
        editing.setNote(sel.getNote());
        editing.setStatus(sel.getStatus());

        boolean saved = Forms.openAppointmentDialog(editing, apptTable.getScene().getWindow());
        if (saved) {
            onApptSearch(); // vagy refreshAppointments();
        }
    }

    @FXML
    public void onApptDelete() {
        var sel = apptTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Válassz időpontot a törléshez!").showAndWait();
            return;
        }
        var ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Törlöd az időpontot: " + sel.getOwnerName() + " – " + sel.getVehiclePlate() + " (" + sel.getStartTs() + ")?",
                javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL).showAndWait();
        if (ok.isPresent() && ok.get() == javafx.scene.control.ButtonType.OK) {
            apptDao.delete(sel.getId());
            refreshAppointments();
        }
    }

    // ---- MUNKALAPOK ----
    @FXML
    public void onJobCardSearch() {
        String q = jobCardSearch.getText().trim();
        serviceJobCards.setAll(q.isEmpty() ? sjcDao.findAllWithOwnerAndVehicleData() : sjcDao.searchWithOwnerAndVehicleData(q));

    }

    @FXML
    public void onJobCardEdit() {
        // 1) Kijelölt munkalap a táblából
        var sel = jobCardTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Válassz ki egy munkalapot a listából!").showAndWait();
            return;
        }

        try {
            // 2) Töltsük be a hozzátartozó ügyfelet és járművet (hogy a form fejlécében is legyenek adatok)
            Customer custObj = null;
            Vehicle vehObj = null;

            if (sel.getCustomer_id() != null && sel.getCustomer_id() > 0) {
                custObj = customerDao.findById(sel.getCustomer_id());
            }

            if (sel.getVehicle_id() != null && sel.getVehicle_id() > 0) {
                vehObj = vehicleDao.findById(sel.getVehicle_id());
            }

            // 3) Nyissuk meg ugyanazzal a helperrel, amit már használsz AppointmentFormController-ben
            Stage dlg = Forms.serviceJobCard(sel, custObj, vehObj);
            dlg.initOwner(jobCardTable.getScene().getWindow());
            dlg.initModality(Modality.WINDOW_MODAL);

            // 4) Mutatjuk a dialógust
            dlg.showAndWait();

            // 5) Mentés után frissítjük a táblát
            refreshJobCards();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Hiba a munkalap megnyitásakor:\n" + ex.getMessage()
            ).showAndWait();
        }

    }

    private void openServiceJobCard(ServiceJobCard editing) {
        var dlg = Forms.serviceJobCard(editing);
        dlg.initOwner(jobCardTable.getScene().getWindow());
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.showAndWait();
        refreshVehicles();
    }

    // ---- KILÉPÉS ----
    @FXML
    public void onLogout() {
        //Stage st = (Stage) logoutButton.getScene().getWindow();
        //st.close();
        System.out.println("[DBG] onLogout");
        Stage s = (Stage) tabPane.getScene().getWindow();
        s.setTitle("Carenda – Bejelentkezés");
        s.setScene(new Scene(new Label("Kijelentkeztél. Indítsd újra az appot vagy lépj be ismét."), 420, 260));

    }

}

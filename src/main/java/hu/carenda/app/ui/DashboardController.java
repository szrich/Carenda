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

public class DashboardController {

    @FXML private TextField customerSearch, vehicleSearch;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Number> cId;
    @FXML private TableColumn<Customer, String> cName, cPhone, cEmail;

    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, Number> vId;
    @FXML private TableColumn<Vehicle, String> vPlate, vMakeModel, vOwner;

    @FXML private TabPane tabPane;

    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    
    // Időpontok UI elemek
    @FXML private TextField apptSearch;
    @FXML private TableView<hu.carenda.app.model.Appointment> apptTable;
    @FXML private TableColumn<hu.carenda.app.model.Appointment, Number> aId, aDuration;
    @FXML private TableColumn<hu.carenda.app.model.Appointment, String> aWhen, aCustomer, aVehicle, aStatus, aNote;

    // DAO + listák
    private final hu.carenda.app.repository.AppointmentDao apptDao = new hu.carenda.app.repository.AppointmentDao();
    private final javafx.collections.ObservableList<hu.carenda.app.model.Appointment> appointments =
        javafx.collections.FXCollections.observableArrayList();


    @FXML
    public void initialize() {

        // Ügyfél oszlopok
        cId.setCellValueFactory(d -> d.getValue().idProperty());
        cName.setCellValueFactory(d -> d.getValue().nameProperty());
        cPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
        cEmail.setCellValueFactory(d -> d.getValue().emailProperty());
        customerTable.setItems(customers);

        // Jármű oszlopok
        vId.setCellValueFactory(d -> d.getValue().idProperty());
        vPlate.setCellValueFactory(d -> d.getValue().plateProperty());
        vMakeModel.setCellValueFactory(d -> d.getValue().makeModelProperty());
        vOwner.setCellValueFactory(d -> d.getValue().ownerNameProperty());
        vehicleTable.setItems(vehicles);

        refreshCustomers();
        refreshVehicles();
        System.out.println("[DBG] DashboardController initialized");
        customerSearch.setOnAction(e -> onCustomerSearch());
        vehicleSearch.setOnAction(e -> onVehicleSearch());
        
        // Időpontok oszlopok
        aId.setCellValueFactory(d -> d.getValue().idProperty());
        aWhen.setCellValueFactory(d -> d.getValue().startTsProperty());
        aDuration.setCellValueFactory(d -> d.getValue().durationMinutesProperty());
        aCustomer.setCellValueFactory(d -> d.getValue().customerNameProperty());
        aVehicle.setCellValueFactory(d -> d.getValue().vehiclePlateProperty());
        aStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        aNote.setCellValueFactory(d -> d.getValue().noteProperty());
        apptTable.setItems(appointments);

        // Keresés Enterrel
        apptSearch.setOnAction(e -> onApptSearch());

        // Kezdeti töltés
        refreshAppointments();


    }

    private void refreshCustomers() { customers.setAll(customerDao.findAll()); }
    private void refreshVehicles() { vehicles.setAll(vehicleDao.findAllWithOwner()); }

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
        if (sel != null) openCustomerForm(sel);
        else new Alert(Alert.AlertType.INFORMATION, "Válassz ügyfelet a listából!").showAndWait();
    }

    @FXML
    public void onCustomerDelete() {
        System.out.println("[DBG] onCustomerDelete");
        var sel = customerTable.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Válassz ügyfelet a törléshez!").showAndWait(); return; }
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
        if (sel != null) openVehicleForm(sel);
        else new Alert(Alert.AlertType.INFORMATION, "Válassz járművet a listából!").showAndWait();
    }

    @FXML
    public void onVehicleDelete() {
        System.out.println("[DBG] onVehicleDelete");
        var sel = vehicleTable.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Válassz járművet a törléshez!").showAndWait(); return; }
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
    
    private void refreshAppointments() { appointments.setAll(apptDao.findAll()); }


    // ---- KILÉPÉS ----
    @FXML
    public void onLogout() {
        System.out.println("[DBG] onLogout");
        Stage s = (Stage) tabPane.getScene().getWindow();
        s.setTitle("Carenda – Bejelentkezés");
        s.setScene(new Scene(new Label("Kijelentkeztél. Indítsd újra az appot vagy lépj be ismét."), 420, 260));
    }
    
    @FXML public void onApptSearch() {
    String q = apptSearch.getText().trim();
    appointments.setAll(q.isEmpty() ? apptDao.findAll() : apptDao.search(q));
}

@FXML public void onApptNew() {
    var dlg = Forms.appointment(null);
    dlg.initOwner(apptTable.getScene().getWindow());
    dlg.initModality(javafx.stage.Modality.WINDOW_MODAL);
    dlg.showAndWait();
    refreshAppointments();
}

@FXML public void onApptEdit() {
    var sel = apptTable.getSelectionModel().getSelectedItem();
    if (sel == null) { new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Válassz időpontot!").showAndWait(); return; }

    // Editálás: egyszerű megoldás – új példány, azonos értékekkel
    var editing = new hu.carenda.app.model.Appointment();
    editing.setId(sel.getId());
    editing.setCustomerId(sel.getCustomerId());
    editing.setVehicleId(sel.getVehicleId());
    editing.setStartTs(sel.getStartTs());
    editing.setDurationMinutes(sel.getDurationMinutes());
    editing.setNote(sel.getNote());
    editing.setStatus(sel.getStatus());

    var dlg = Forms.appointment(editing);
    dlg.initOwner(apptTable.getScene().getWindow());
    dlg.initModality(javafx.stage.Modality.WINDOW_MODAL);
    dlg.showAndWait();
    refreshAppointments();
}

@FXML public void onApptDelete() {
    var sel = apptTable.getSelectionModel().getSelectedItem();
    if (sel == null) { new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Válassz időpontot a törléshez!").showAndWait(); return; }
    var ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
            "Törlöd az időpontot: " + sel.getCustomerName() + " – " + sel.getVehiclePlate() + " (" + sel.getStartTs() + ")?",
            javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL).showAndWait();
    if (ok.isPresent() && ok.get() == javafx.scene.control.ButtonType.OK) {
        apptDao.delete(sel.getId());
        refreshAppointments();
    }
}

}

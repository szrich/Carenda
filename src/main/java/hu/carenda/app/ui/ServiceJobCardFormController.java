package hu.carenda.app.ui;

// PDF generáláshoz szükséges importok
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import hu.carenda.app.model.ServiceJobCard;
import hu.carenda.app.model.ServiceJobCardPart;
import hu.carenda.app.model.ServiceJobCardWorkDesc;
import hu.carenda.app.repository.ServiceJobCardDao;
import hu.carenda.app.model.User;
import hu.carenda.app.repository.ServiceJobCardPartDao;
import hu.carenda.app.repository.ServiceJobCardWorkDescDao;
import hu.carenda.app.repository.UserDao;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.util.StringConverter;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * A Munkalap űrlap (serviceJobCard-form.fxml) vezérlője.
 * Felelős a munkalapok létrehozásáért, szerkesztéséért, mentéséért és PDF exportjáért.
 */
public class ServiceJobCardFormController {

    // ---------- FXML mezők ----------
    // Fejléc
    @FXML private ComboBox<String> status;
    @FXML private ComboBox<User> assignee;

    // Alapadatok blokk
    @FXML private TextField jobcardNo;
    @FXML private DatePicker createdAt;
    @FXML private Spinner<Integer> createdHourSpinner;
    @FXML private Spinner<Integer> createdMinuteSpinner;
    @FXML private TextField customerName;
    @FXML private TextField phone;
    @FXML private TextField email;
    @FXML private TextField plate;
    @FXML private TextField vin;
    @FXML private TextField brand;
    @FXML private TextField model;
    @FXML private TextField year;
    @FXML private TextField odometerKm;
    @FXML private ToggleGroup fuelLevelToggleGroup;
    @FXML private RadioButton fuelEmpty;
    @FXML private RadioButton fuelQuarter;
    @FXML private RadioButton fuelHalf;
    @FXML private RadioButton fuelThreeQuarter;
    @FXML private RadioButton fuelFull;
    
    // Ezeket a mezőket jelenleg nem használja a logika, de az FXML-ben létezhetnek
    @FXML private DatePicker modificationDate;
    @FXML private Spinner<Integer> modificationHourSpinner;
    @FXML private Spinner<Integer> modificationMinuteSpinner;
    @FXML private DatePicker dueDate;
    @FXML private Spinner<Integer> dueHourSpinner;
    @FXML private Spinner<Integer> dueMinuteSpinner;

    // Hiba és megállapítások blokk
    @FXML private TextArea faultDesc;
    @FXML private TextArea repairNote;
    @FXML private TextArea diagnosis;
    @FXML private TextArea internalNote;

    // Munka (labor) tábla
    @FXML private TableView<ServiceJobCardWorkDesc> workdescTable;
    @FXML private TableColumn<ServiceJobCardWorkDesc, String> colWorkdescName;
    @FXML private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescHours;
    @FXML private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescRate;
    @FXML private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescVat;
    @FXML private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescNet;
    @FXML private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescGross;
    @FXML private Button btnAddWorkdesc;
    @FXML private Button btnRemoveWorkdesc;

    // Alkatrész tábla
    @FXML private TableView<ServiceJobCardPart> partsTable;
    @FXML private TableColumn<ServiceJobCardPart, String> colPartSku;
    @FXML private TableColumn<ServiceJobCardPart, String> colPartName;
    @FXML private TableColumn<ServiceJobCardPart, Number> colPartQty;
    @FXML private TableColumn<ServiceJobCardPart, Number> colPartUnitPrice;
    @FXML private TableColumn<ServiceJobCardPart, Number> colPartVat;
    @FXML private TableColumn<ServiceJobCardPart, Number> colPartNet;
    @FXML private TableColumn<ServiceJobCardPart, Number> colPartGross;
    @FXML private Button btnAddPart;
    @FXML private Button btnRemovePart;

    // Összesítő blokk
    @FXML private TextField subtotalNet;
    @FXML private TextField vatAmount;
    @FXML private TextField totalGross;
    @FXML private TextField advance;
    @FXML private TextField amountDue;
    
    // Gombok
    @FXML private Button btnPdf;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    // ---------- DAO-k ----------
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ServiceJobCardDao jobCardDao = new ServiceJobCardDao();
    private final UserDao userDao = new UserDao();
    private final ServiceJobCardWorkDescDao workDescDao = new ServiceJobCardWorkDescDao();
    private final ServiceJobCardPartDao partDao = new ServiceJobCardPartDao();

    // ---------- Táblázat Adatforrások ----------
    private final ObservableList<ServiceJobCardWorkDesc> workdescItems = FXCollections.observableArrayList();
    private final ObservableList<ServiceJobCardPart> partItems = FXCollections.observableArrayList();

    // ---------- Aktuális állapot ----------
    private ServiceJobCard editing; // A munkalap, amit szerkesztünk
    private Customer customerData;  // Előtöltött ügyfél (lehet null)
    private Vehicle vehicleData;    // Előtöltött jármű (lehet null)
    // private User currentUser; // A "bejelentkezett user alapértelmezett" funkcióhoz szükséges

    // Formátum, amiben TÁROLUNK az adatbázisban (created_at stb.)
    private static final DateTimeFormatter LDT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    // Formátum a "most" bélyeghez (updated_at, finished_at)
    private static final DateTimeFormatter ISO_DB = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Az FXML betöltése után hívódik meg. Beállítja a ComboBox-okat,
     * spinnereket, táblázatokat és eseménykezelőket.
     */
    @FXML
    public void initialize() {
        // Státusz lista
        status.setItems(FXCollections.observableArrayList(
                "OPEN", "IN_PROGRESS", "READY", "DELIVERED", "CANCELLED"
        ));

        // Dátum/Idő beállítók inicializálása
        setupDateTimeSpinners();

        // Szerelők (Assignee) ComboBox beállítása
        setupAssigneeComboBox();
        
        // Üzemanyagszint beállítása
        setupFuelLevelRadioButtons();
        
        // Pénzügyi mezők írásvédetté tétele (kivéve előleg)
        subtotalNet.setEditable(false);
        vatAmount.setEditable(false);
        totalGross.setEditable(false);
        amountDue.setEditable(false);

        // Gomb eseménykezelők
        btnPdf.setOnAction(e -> onExportPdf());
        btnSave.setOnAction(e -> onSave());
        btnCancel.setOnAction(e -> onCancel());
        
        // 1. Validációs logikák szétbontása mezőnként
        BooleanBinding assigneeInvalid = Bindings.createBooleanBinding(() -> {
            User selected = assignee.getSelectionModel().getSelectedItem();
            return (selected == null || selected.getId() == 0);
        }, assignee.getSelectionModel().selectedItemProperty());

        BooleanBinding odometerInvalid = Bindings.createBooleanBinding(() -> {
            String odoText = odometerKm.getText();
            return (odoText == null || odoText.trim().isEmpty());
        }, odometerKm.textProperty());

        // 2. Listenerek hozzáadása a CSS osztályok cseréjéhez
        // Amikor a property változik, meghívja a validáló metódust
        assigneeInvalid.addListener((obs, oldVal, newVal) -> validateAssignee());
        odometerInvalid.addListener((obs, oldVal, newVal) -> validateOdometer());
        
        // 3. Kombinált binding a Mentés gomb letiltásához
        BooleanBinding isInvalid = assigneeInvalid.or(odometerInvalid);
        btnSave.disableProperty().bind(isInvalid);

        // --- Validáció vége ---
        
        // Táblázatok és cellák beállítása
        setupWorkdescTable();
        
        // Táblázatok és cellák beállítása
        setupWorkdescTable();
        setupPartsTable();
        setupTableEditHandlers();
        setupTableButtonHandlers();
    }

    /**
     * Beállítja az összes óra/perc spinnert.
     */
    private void setupDateTimeSpinners() {
        createdAt.setValue(LocalDate.now()); // Ma
        createdHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalDateTime.now().getHour(), 1));
        createdHourSpinner.setEditable(true);
        createdMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, LocalDateTime.now().getMinute(), 5));
        createdMinuteSpinner.setEditable(true);

        // A többi dátum/idő mezőt jelenleg nem használjuk aktívan,
        // de az FXML betöltés miatt inicializálni kell.
        if (modificationHourSpinner != null) {
            modificationHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0, 1));
        }
        if (modificationMinuteSpinner != null) {
            modificationMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));
        }
        if (dueHourSpinner != null) {
            dueHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0, 1));
        }
        if (dueMinuteSpinner != null) {
            dueMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));
        }
    }

    /**
     * Betölti a felhasználókat (szerelőket) a ComboBox-ba.
     */
    private void setupAssigneeComboBox() {
        ObservableList<User> mechanics = FXCollections.observableArrayList(userDao.findAll());

        // "Nincs hozzárendelve" opció
        User noAssignee = new User();
        noAssignee.setId(0); // Ezt a mentéskor 'null'-ra alakítjuk
        noAssignee.setFullName("Nincs hozzárendelve");
        mechanics.add(0, noAssignee);

        assignee.setItems(mechanics);

        // StringConverter a User objektumok nevének megjelenítéséhez
        assignee.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                return (u == null) ? "" : u.getFullName();
            }
            @Override
            public User fromString(String s) {
                // A 'fromString' itt nem releváns, mert a ComboBox nem szerkeszthető
                return assignee.getItems().stream()
                        .filter(u -> u.getFullName().equalsIgnoreCase(s))
                        .findFirst().orElse(null);
            }
        });
    }
    
    /**
     * Beállítja az üzemanyagszint RadioButton-ök 'userData' mezőit,
     * hogy a kiválasztott gomb könnyen átalakítható legyen a 0-8 adatbázis értékké.
     */
    private void setupFuelLevelRadioButtons() {
        // Mapping az adatbázis 'fuel_level_eighths' (0-8) mezőjéhez:
        // "üres"   -> 0
        // "negyed" -> 2 (8 * 1/4)
        // "fél"    -> 4 (8 * 1/2)
        // "3/4"    -> 6 (8 * 3/4)
        // "tele"   -> 8
        
        fuelEmpty.setUserData(0);
        fuelQuarter.setUserData(2);
        fuelHalf.setUserData(4);
        fuelThreeQuarter.setUserData(6);
        fuelFull.setUserData(8);
    }

    /**
     * Beállítja a Munkadíj táblázat (workdescTable) oszlopait.
     */
    private void setupWorkdescTable() {
        workdescTable.setEditable(true);
        colWorkdescName.setCellValueFactory(data -> data.getValue().nameProperty());
        colWorkdescHours.setCellValueFactory(data -> data.getValue().hoursProperty());
        colWorkdescRate.setCellValueFactory(data -> data.getValue().rate_centsProperty()); // Ft-ban jelenítjük meg
        colWorkdescVat.setCellValueFactory(data -> data.getValue().vat_percentProperty());
        
        // Számolt mezők (Nettó, Bruttó)
        colWorkdescNet.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(calcNetForRow(data.getValue())));
        colWorkdescGross.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(calcGrossForRow(data.getValue())));

        // Forint formázó a pénz mezőkhöz
        colWorkdescRate.setCellFactory(tc -> new TextFieldTableCell<>(new CentToForintConverter()));
        colWorkdescNet.setCellFactory(tc -> new TextFieldTableCell<>(new ForintConverter()));
        colWorkdescGross.setCellFactory(tc -> new TextFieldTableCell<>(new ForintConverter()));

        workdescTable.setItems(workdescItems);
    }

    /**
     * Beállítja az Alkatrész táblázat (partsTable) oszlopait.
     */
    private void setupPartsTable() {
        partsTable.setEditable(true);
        colPartSku.setCellValueFactory(data -> data.getValue().skuProperty());
        colPartName.setCellValueFactory(data -> data.getValue().nameProperty());
        colPartQty.setCellValueFactory(data -> data.getValue().quantityProperty());
        colPartUnitPrice.setCellValueFactory(data -> data.getValue().unit_price_centsProperty());
        colPartVat.setCellValueFactory(data -> data.getValue().vat_percentProperty());

        // Számolt mezők (Nettó, Bruttó)
        colPartNet.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(calcNetForPartRow(data.getValue())));
        colPartGross.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(calcGrossForPartRow(data.getValue())));
        
        // Forint formázó
        colPartUnitPrice.setCellFactory(tc -> new TextFieldTableCell<>(new CentToForintConverter()));
        colPartNet.setCellFactory(tc -> new TextFieldTableCell<>(new ForintConverter()));
        colPartGross.setCellFactory(tc -> new TextFieldTableCell<>(new ForintConverter()));

        partsTable.setItems(partItems);
    }

    /**
     * Beállítja a táblázatok celláinak szerkesztési eseménykezelőit.
     */
    private void setupTableEditHandlers() {
        // --- Munkadíj tábla szerkesztése ---
        
        var numberConverter = new NumberStringConverter();

        colWorkdescName.setCellFactory(TextFieldTableCell.forTableColumn());
        colWorkdescName.setOnEditCommit(evt -> {
            evt.getRowValue().setName(evt.getNewValue());
            recalcTotals();
        });

        colWorkdescHours.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colWorkdescHours.setOnEditCommit(evt -> {
            evt.getRowValue().setHours(evt.getNewValue().doubleValue());
            recalcTotals();
            workdescTable.refresh(); // Frissíti a számolt mezőket
        });

        colWorkdescRate.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colWorkdescRate.setOnEditCommit(evt -> {
            // A felhasználó Ft-ot ír be, mi cent-ben tároljuk
            evt.getRowValue().setRate_cents(evt.getNewValue().intValue() * 100);
            recalcTotals();
            workdescTable.refresh();
        });

        colWorkdescVat.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colWorkdescVat.setOnEditCommit(evt -> {
            evt.getRowValue().setVat_percent(evt.getNewValue().intValue());
            recalcTotals();
            workdescTable.refresh();
        });

        // --- Alkatrész tábla szerkesztése ---
        colPartSku.setCellFactory(TextFieldTableCell.forTableColumn());
        colPartSku.setOnEditCommit(evt -> evt.getRowValue().setSku(evt.getNewValue())); // Nincs újraszámolás

        colPartName.setCellFactory(TextFieldTableCell.forTableColumn());
        colPartName.setOnEditCommit(evt -> evt.getRowValue().setName(evt.getNewValue())); // Nincs újraszámolás

        colPartQty.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colPartQty.setOnEditCommit(evt -> {
            evt.getRowValue().setQuantity(evt.getNewValue().doubleValue());
            recalcTotals();
            partsTable.refresh();
        });

        colPartUnitPrice.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colPartUnitPrice.setOnEditCommit(evt -> {
            // A felhasználó Ft-ot ír be, mi cent-ben tároljuk
            evt.getRowValue().setUnit_price_cents(evt.getNewValue().intValue() * 100);
            recalcTotals();
            partsTable.refresh();
        });

        colPartVat.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colPartVat.setOnEditCommit(evt -> {
            evt.getRowValue().setVat_percent(evt.getNewValue().intValue());
            recalcTotals();
            partsTable.refresh();
        });
    }

    /**
     * Beállítja a "Sor hozzáadása" / "Sor törlése" gombok eseménykezelőit.
     */
    private void setupTableButtonHandlers() {
        btnAddWorkdesc.setOnAction(e -> {
            ServiceJobCardWorkDesc nw = new ServiceJobCardWorkDesc();
            nw.setId(null); // Új sor
            nw.setSjc_id(editing.getId()); // A jelenlegi munkalaphoz kötjük
            nw.setName("Új munka");
            nw.setHours(1.0);
            nw.setRate_cents(10000 * 100); // Alap 10.000 Ft óradíj
            nw.setVat_percent(27);
            workdescItems.add(nw);

            workdescTable.getSelectionModel().select(nw);
            workdescTable.edit(workdescItems.size() - 1, colWorkdescName);
            recalcTotals();
        });

        btnRemoveWorkdesc.setOnAction(e -> {
            ServiceJobCardWorkDesc sel = workdescTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                workdescItems.remove(sel);
                recalcTotals();
            }
        });

        btnAddPart.setOnAction(e -> {
            ServiceJobCardPart np = new ServiceJobCardPart();
            np.setId(null); // Új sor
            np.setSjc_id(editing.getId());
            np.setSku("");
            np.setName("Új alkatrész");
            np.setQuantity(1.0);
            np.setUnit_price_cents(0);
            np.setVat_percent(27);
            partItems.add(np);

            partsTable.getSelectionModel().select(np);
            partsTable.edit(partItems.size() - 1, colPartName);
            recalcTotals();
        });

        btnRemovePart.setOnAction(e -> {
            ServiceJobCardPart sel = partsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                partItems.remove(sel);
                recalcTotals();
            }
        });
    }


    // ---------- Adatbetöltés (A Forms osztály hívja meg) ----------

    /**
     * A Forms osztály hívja meg a dialógus megnyitásakor.
     * Beállítja a szerkesztendő objektumokat és feltölti az űrlapot.
     *
     * @param card A szerkesztendő ServiceJobCard (lehet új, előtöltött).
     * @param cust A kapcsolódó Customer (lehet null).
     * @param veh  A kapcsolódó Vehicle (lehet null).
     */
    public void setContext(ServiceJobCard card, Customer cust, Vehicle veh) {
        //this.currentUser = currentUser; // Az auto-assign funkcióhoz kellene
        this.editing = (card != null ? card : new ServiceJobCard());
        this.customerData = cust;
        this.vehicleData = veh;

        // Csak 'null'-t ellenőrzünk (az új Modellek alapján)
        boolean isNew = (editing.getId() == null);

        // Ha új a munkalap, gondoskodunk pár alapértelmezett értékről
        if (isNew) {
            // Munkalapszám generálása, ha nincs
            if (!notBlank(editing.getJobcard_no())) {
                editing.setJobcard_no(jobCardDao.generateNextJobCardNo());
            }
            // Státusz beállítása OPEN-re
            if (!notBlank(editing.getStatus())) {
                editing.setStatus("OPEN");
            }
            // Létrehozás idejének beállítása (ha még nincs)
            if (!notBlank(editing.getCreated_at())) {
                 editing.setCreated_at(LocalDateTime.now().format(LDT_FMT));
            }
        }

        // És végül töltsük fel az űrlapot a modellek adataival
        fillFormFromData();
    }
    
    /**
     * Kompatibilitási metódus a régi hívásokhoz (pl. AppointmentFormController).
     * @param jc A szerkesztendő munkalap.
     */
    public void setEditing(ServiceJobCard jc) {
        // Átirányítás az új, kontextust beállító metódusra
        // A 'currentUser'-t itt null-nak adjuk át, mert ez a hívási ág nem ismeri.
        setContext(jc, null, null);
    }

    /**
     * Feltölti az űrlap mezőit az 'editing', 'customerData' és 'vehicleData'
     * objektumokból.
     */
    private void fillFormFromData() {
        // Munkalap szám
        jobcardNo.setText(nvl(editing.getJobcard_no()));

        // Státusz
        status.getSelectionModel().select(notBlank(editing.getStatus()) ? editing.getStatus() : "OPEN");

        // Szerelő (Assignee)
        Integer techId = editing.getAssignee_user_id();
        User defaultAssignee = assignee.getItems().stream().filter(u -> u.getId() == 0).findFirst().orElse(null);
        
        if (techId != null && techId > 0) {
            // Meglévő munkalap: a mentett szerelőt keressük
            assignee.getItems().stream()
                    .filter(u -> Objects.equals(u.getId(), techId)) // Integer vs int
                    .findFirst()
                    .ifPresentOrElse(
                            assignee.getSelectionModel()::select,
                            () -> assignee.getSelectionModel().select(defaultAssignee) // Ha a user már nem létezik
                    );
        } 
        /*
        // Az "auto-assign" logikához ez a blokk kellene:
        else if (this.currentUser != null) {
            // Új munkalap: az aktuális (bejelentkezett) felhasználót keressük
            assignee.getItems().stream()
                    .filter(u -> Objects.equals(u.getId(), this.currentUser.getId()))
                    .findFirst()
                    .ifPresentOrElse(
                            assignee.getSelectionModel()::select,
                            () -> assignee.getSelectionModel().select(defaultAssignee) // Ha az aktuális user (pl. admin) nincs a listán
                    );
        } 
        */
        else {
            // Fallback: "Nincs hozzárendelve"
            assignee.getSelectionModel().select(defaultAssignee);
        }

        // Létrehozás dátuma (Created At)
        LocalDateTime createdLdt = tryParseDateTime(editing.getCreated_at());
        if (createdLdt == null) createdLdt = LocalDateTime.now(); // Fallback
        
        createdAt.setValue(createdLdt.toLocalDate());
        createdHourSpinner.getValueFactory().setValue(createdLdt.getHour());
        createdMinuteSpinner.getValueFactory().setValue(createdLdt.getMinute());

        // Módosítás dátuma (Updated At)
        LocalDateTime updatedLdt = tryParseDateTime(editing.getUpdated_at());
        if (updatedLdt != null) {
            if (modificationDate != null) modificationDate.setValue(updatedLdt.toLocalDate());
            if (modificationHourSpinner != null) modificationHourSpinner.getValueFactory().setValue(updatedLdt.getHour());
            if (modificationMinuteSpinner != null) modificationMinuteSpinner.getValueFactory().setValue(updatedLdt.getMinute());
        }

        // Ügyfél adatok (prioritás: contextus, majd ID alapú betöltés)
        fillCustomerData();

        // Jármű adatok (prioritás: contextus, majd ID alapú betöltés)
        fillVehicleData();

        // Km óra
        odometerKm.setText(editing.getOdometer_km() != null ? String.valueOf(editing.getOdometer_km()) : "");
        
        // Üzemanyagszint (alapértelmezett: 0 (üres))
        Integer fuelLevel = editing.getFuel_level_eighths();
        selectFuelLevelRadio(fuelLevel);

        // Hiba leírások
        faultDesc.setText(nvl(editing.getFault_desc()));
        repairNote.setText(nvl(editing.getRepair_note()));
        diagnosis.setText(nvl(editing.getDiagnosis()));
        internalNote.setText(nvl(editing.getInternal_note()));

        // Előleg (Forintban jelenítjük meg)
        advance.setText(editing.getAdvance_cents() != null ? String.valueOf(editing.getAdvance_cents() / 100) : "0");

        // Tételek betöltése DB-ből (csak meglévő munkalap esetén)
        workdescItems.clear();
        partItems.clear();
        if (editing.getId() != null) {
            workdescItems.addAll(workDescDao.findByJobCard(editing.getId()));
            partItems.addAll(partDao.findByJobCard(editing.getId()));
        }

        // Összesítő kalkulálása (vagy DB-ből, vagy a 0 sorok alapján)
        recalcTotals();
        
        // (A listenerek csak változáskor futnak le, 
        // ezért megnyitáskor manuálisan is meg kell hívni)
        validateAssignee();
        validateOdometer();
    }
    
    /**
     * Ellenőrzi az 'assignee' (Felelős) mezőt, és beállítja a '.validation-error'
     * CSS osztályt, ha érvénytelen.
     */
    private void validateAssignee() {
        final String errorClass = "validation-error";
        User selected = assignee.getSelectionModel().getSelectedItem();
        
        if (selected == null || selected.getId() == 0) {
            // Hozzáadja a stílust, de csak ha még nincs rajta
            if (!assignee.getStyleClass().contains(errorClass)) {
                assignee.getStyleClass().add(errorClass);
            }
        } else {
            // Eltávolítja a stílust
            assignee.getStyleClass().remove(errorClass);
        }
    }

    /**
     * Ellenőrzi az 'odometerKm' (Km-óra) mezőt, és beállítja a '.validation-error'
     * CSS osztályt, ha érvénytelen.
     */
    private void validateOdometer() {
        final String errorClass = "validation-error";
        String odoText = odometerKm.getText();
        
        if (odoText == null || odoText.trim().isEmpty()) {
            if (!odometerKm.getStyleClass().contains(errorClass)) {
                odometerKm.getStyleClass().add(errorClass);
            }
        } else {
            odometerKm.getStyleClass().remove(errorClass);
        }
    }

    /**
     * Feltölti az ügyfél adatokat az űrlapra.
     */
    private void fillCustomerData() {
        if (customerData != null) {
            customerName.setText(nvl(customerData.getName()));
            phone.setText(nvl(customerData.getPhone()));
            email.setText(nvl(customerData.getEmail()));
        } else if (editing.getCustomer_id() != null) {
            try {
                Customer c = customerDao.findById(editing.getCustomer_id());
                if (c != null) {
                    customerName.setText(nvl(c.getName()));
                    phone.setText(nvl(c.getPhone()));
                    email.setText(nvl(c.getEmail()));
                }
            } catch (Exception e) {
                e.printStackTrace(); // Hiba a konzolra
            }
        }
    }

    /**
     * Feltölti a jármű adatokat az űrlapra.
     */
    private void fillVehicleData() {
        if (vehicleData != null) {
            plate.setText(nvl(vehicleData.getPlate()));
            vin.setText(nvl(vehicleData.getVin()));
            brand.setText(nvl(vehicleData.getBrand()));
            model.setText(nvl(vehicleData.getModel()));
            year.setText(vehicleData.getYear() != null ? String.valueOf(vehicleData.getYear()) : "");
        } else if (editing.getVehicle_id() != null) {
            try {
                Vehicle v = vehicleDao.findById(editing.getVehicle_id());
                if (v != null) {
                    plate.setText(nvl(v.getPlate()));
                    vin.setText(nvl(v.getVin()));
                    brand.setText(nvl(v.getBrand()));
                    model.setText(nvl(v.getModel()));
                    year.setText(v.getYear() != null ? String.valueOf(v.getYear()) : "");
                }
            } catch (Exception e) {
                e.printStackTrace(); // Hiba a konzolra
            }
        }
    }
    
    /**
     * A mentett 'fuel_level_eighths' (0-8) érték alapján kiválasztja
     * a megfelelő RadioButton-t a felületen.
     * Ha az érték null vagy érvénytelen, az "üres" (0) lesz kiválasztva.
     * @param level A mentett érték (pl. 0, 2, 4, 6, 8 vagy null).
     */
    private void selectFuelLevelRadio(Integer level) {
        // Alapértelmezett kiválasztás (üres)
        Toggle toSelect = fuelEmpty; 
        
        if (level != null) {
            // Végigiterálunk a group gombjain
            for (Toggle toggle : fuelLevelToggleGroup.getToggles()) {
                // Megkeressük azt a gombot, aminek a 'userData'-ja egyezik a mentett értékkel
                if (toggle.getUserData() != null && toggle.getUserData().equals(level)) {
                    toSelect = toggle;
                    break; // Megvan a gomb
                }
            }
        }
        
        // Kiválasztjuk a megtalált gombot (vagy az alapértelmezett 'fuelEmpty'-t)
        fuelLevelToggleGroup.selectToggle(toSelect);
    }
    
    /**
     * Megpróbálja beolvasni a dátum/idő stringet többféle formátumban.
     * @param raw A nyers string az adatbázisból.
     * @return A LocalDateTime objektum, vagy null, ha a parse sikertelen.
     */
    private LocalDateTime tryParseDateTime(String raw) {
        if (!notBlank(raw)) return null;
        
        // 1. Próba: ISO formátum (pl. 2025-10-25T17:31:49.660023)
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignore) {}
        
        // 2. Próba: Rövidített formátum (pl. "yyyy-MM-dd'T'HH:mm")
        try {
            return LocalDateTime.parse(raw, LDT_FMT);
        } catch (Exception ignore) {}
        
        return null; // Sikertelen
    }


    // ---------- Mentés és Bezárás ----------

    /**
     * A "Mentés" gomb eseménykezelője.
     * Validálja az űrlapot, átmenti az adatokat a 'editing' objektumba,
     * majd elmenti az adatbázisba (insert/update).
     */
    @FXML
    public void onSave() {
        try {
            // 1. GUI -> 'editing' objektum frissítése
            collectFormDataToModel();

            // 2. Fő munkalap mentése (insert vagy update)
            boolean isNew = (editing.getId() == null);
            if (isNew) {
                // Beszúráskor a DAO visszakapja az új ID-t és beállítja az 'editing' objektumra
                jobCardDao.insert(editing);
            } else {
                jobCardDao.update(editing);
            }

            // Mostantól biztos, hogy editing.getId() érvényes
            Integer sjcId = editing.getId();
            if (sjcId == null) {
                // Ez nem történhet meg, ha a DAO helyesen működik
                showError("Kritikus hiba", "Nem sikerült ID-t kapni a munkalap mentésekor.");
                return;
            }

            // 3. Kapcsolódó tételek mentése (Delete / Insert stratégia)
            saveChildItems(sjcId);

            // 4. Ablak bezárása siker jelzéssel
            closeWindow(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Mentési Hiba", "Hiba a munkalap mentésekor:\n" + ex.getMessage());
        }
    }

    /**
     * Átmenti az adatokat az űrlap mezőiből az 'editing' ServiceJobCard objektumba.
     */
    private void collectFormDataToModel() {
        editing.setJobcard_no(emptyToNull(jobcardNo.getText()));

        String stSel = status.getSelectionModel().getSelectedItem();
        editing.setStatus(notBlank(stSel) ? stSel : "OPEN");

        // Szerelő (Assignee)
        User selectedUser = assignee.getSelectionModel().getSelectedItem();
        if (selectedUser != null && selectedUser.getId() > 0) {
            editing.setAssignee_user_id(selectedUser.getId());
        } else {
            editing.setAssignee_user_id(null); // "Nincs hozzárendelve" (0) -> null
        }

        // Km óra
        editing.setOdometer_km(parseIntSafe(odometerKm.getText()));
        
        // Üzemanyagszint
        Toggle selectedFuelToggle = fuelLevelToggleGroup.getSelectedToggle();
        if (selectedFuelToggle != null && selectedFuelToggle.getUserData() != null) {
            // A 'userData'-t Integer-ként mentettük a setupFuelLevelRadioButtons-ban
            editing.setFuel_level_eighths((Integer) selectedFuelToggle.getUserData());
        } else {
            // Ha valamiért nincs kiválasztva (ami nem történhet meg, de biztonságos), 
            // 0-t (üres) mentünk
            editing.setFuel_level_eighths(0); 
        }

        // Leírások
        editing.setFault_desc(safeTrim(faultDesc.getText()));
        editing.setRepair_note(safeTrim(repairNote.getText()));
        editing.setDiagnosis(safeTrim(diagnosis.getText()));
        editing.setInternal_note(safeTrim(internalNote.getText()));

        // Előleg (Ft-ban írjuk be, centben tároljuk)
        Integer advanceFt = parseIntSafe(advance.getText());
        editing.setAdvance_cents(advanceFt != null ? advanceFt * 100 : 0); // null vagy 0 -> 0 cent

        // Létrehozás dátuma (Created At)
        LocalDate pickedDate = createdAt.getValue() != null ? createdAt.getValue() : LocalDate.now();
        Integer hh = createdHourSpinner.getValue() != null ? createdHourSpinner.getValue() : 0;
        Integer mm = createdMinuteSpinner.getValue() != null ? createdMinuteSpinner.getValue() : 0;
        LocalDateTime ldtForSave = pickedDate.atTime(hh, mm);
        
        editing.setCreated_at(ldtForSave.format(LDT_FMT)); // Mindig frissítjük a GUI-n lévőre

        // Módosítás dátuma (Updated At) - Mindig 'most'
        String nowIso = LocalDateTime.now().format(ISO_DB);
        editing.setUpdated_at(nowIso);

        // Befejezés dátuma (Finished At)
        if ("DELIVERED".equalsIgnoreCase(editing.getStatus()) && !notBlank(editing.getFinished_at())) {
            editing.setFinished_at(nowIso); // Beállítás 'most'-ra, ha DELIVERED és még nincs beállítva
        }
    }

    /**
     * Elmenti a munka- és alkatrész-tételeket a munkalaphoz.
     * (Delete / Insert stratégiát használ)
     * @param sjcId A szülő munkalap ID-ja.
     */
    private void saveChildItems(Integer sjcId) {
        // 1. Régi tételek törlése
        workDescDao.deleteByJobCard(sjcId);
        partDao.deleteByJobCard(sjcId);

        // 2. Új tételek beszúrása az aktuális listából
        int lineNo = 1;
        for (ServiceJobCardWorkDesc w : workdescItems) {
            workDescDao.insert(sjcId, w.getName(), w.getHours(), w.getRate_cents(), w.getVat_percent(), lineNo++);
        }

        int partLineNo = 1;
        for (ServiceJobCardPart p : partItems) {
            partDao.insert(sjcId, p.getSku(), p.getName(), p.getQuantity(), p.getUnit_price_cents(), p.getVat_percent(), partLineNo++);
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
        Stage stg = (Stage) btnSave.getScene().getWindow();
        stg.setUserData(success); // Ezt olvassa ki a Forms.openModalForm
        stg.close();
    }


    // ---------- Segédfüggvények (String/Számítás/PDF) ----------

    // --- String/Szám segédek ---
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static String nvl(String s) { return (s == null) ? "" : s; }
    private static String safeTrim(String s) { return (s == null) ? "" : s.trim(); }
    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static Integer parseIntSafe(String txt) {
        if (txt == null) return null;
        String t = txt.trim().replace(" Ft", ""); // Esetleges " Ft" eltávolítása
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // --- Összesítő Kalkuláció ---

    /**
     * Újraszámolja az összesített (Nettó, ÁFA, Bruttó, Fizetendő) mezőket
     * a táblázatok és az 'Előleg' mező alapján.
     */
    @FXML
    private void recalcTotals() {
        double netSum = 0.0;
        double grossSum = 0.0;

        // Munkadíjak
        for (ServiceJobCardWorkDesc w : workdescItems) {
            double netCents = w.getHours() * w.getRate_cents();
            double grossCents = netCents * (1.0 + (w.getVat_percent() / 100.0));
            netSum += (netCents / 100.0);
            grossSum += (grossCents / 100.0);
        }

        // Alkatrészek
        for (ServiceJobCardPart p : partItems) {
            double netCents = p.getQuantity() * p.getUnit_price_cents();
            double grossCents = netCents * (1.0 + (p.getVat_percent() / 100.0));
            netSum += (netCents / 100.0);
            grossSum += (grossCents / 100.0);
        }

        double vatAmountFt = grossSum - netSum;

        // Előleg (text mező) levonása
        Integer advInt = parseIntSafe(advance.getText());
        double advFt = (advInt == null ? 0.0 : advInt);
        double dueFt = grossSum - advFt;

        // Forintban, kerekítve
        subtotalNet.setText(String.format("%.0f", netSum));
        vatAmount.setText(String.format("%.0f", vatAmountFt));
        totalGross.setText(String.format("%.0f", grossSum));
        amountDue.setText(String.format("%.0f", dueFt));
    }
    
    // --- Táblázat sor kalkulátorok (Cent -> Forint) ---
    private int calcNetForRow(ServiceJobCardWorkDesc w) {
        if (w == null) return 0;
        double netCents = w.getHours() * w.getRate_cents();
        return (int) Math.round(netCents / 100.0);
    }
    private int calcGrossForRow(ServiceJobCardWorkDesc w) {
        if (w == null) return 0;
        double netCents = w.getHours() * w.getRate_cents();
        double grossCents = netCents * (1.0 + (w.getVat_percent() / 100.0));
        return (int) Math.round(grossCents / 100.0);
    }
    private int calcNetForPartRow(ServiceJobCardPart p) {
        if (p == null) return 0;
        double netCents = p.getQuantity() * p.getUnit_price_cents();
        return (int) Math.round(netCents / 100.0);
    }
    private int calcGrossForPartRow(ServiceJobCardPart p) {
        if (p == null) return 0;
        double netCents = p.getQuantity() * p.getUnit_price_cents();
        double grossCents = netCents * (1.0 + (p.getVat_percent() / 100.0));
        return (int) Math.round(grossCents / 100.0);
    }

    // --- PDF Generálás ---

    @FXML
    private void onExportPdf() {
        try {
            // 1. Fájl helyének bekérése
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Munkalap PDF mentése");
            String safeJobNo = notBlank(editing.getJobcard_no())
                    ? editing.getJobcard_no().replaceAll("[^0-9A-Za-z_-]", "_")
                    : "munkalap";
            chooser.setInitialFileName(safeJobNo + ".pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Fájlok", "*.pdf"));
            
            java.io.File file = chooser.showSaveDialog(btnPdf.getScene().getWindow());
            if (file == null) {
                return; // User 'Cancel'-t nyomott
            }

            // 2. PDF generálása a háttérben
            generatePdfToFile(file.getAbsolutePath());

            // 3. Siker visszajelzése
            showInfo("PDF Mentve", "A munkalap sikeresen mentve:\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("PDF mentési hiba", "A PDF generálása sikertelen:\n" + ex.getMessage());
        }
    }

    /**
     * Legenerálja a munkalap PDF-jét a megadott útvonalra.
     * (OpenPDF library-t használ)
     * @param path A célfájl teljes útvonala.
     * @throws Exception Hiba esetén (pl. IO, dokumentum hiba)
     */
    private void generatePdfToFile(String path) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(path));
        doc.open();

        // Betűtípusok
        Font bold18 = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font bold12 = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normal10 = new Font(Font.HELVETICA, 10, Font.NORMAL);

        // --- Cím ---
        doc.add(new Paragraph("Munkalap #" + nvl(editing.getJobcard_no()), bold18));
        doc.add(new Paragraph("Állapot: " + nvl(editing.getStatus()), normal10));
        doc.add(new Paragraph("Felvétel ideje: " + nvl(editing.getCreated_at()), normal10));
        doc.add(new Paragraph(" ")); // Térköz

        // --- Ügyfél és Jármű blokk (2 oszlopos táblázat) ---
        String custBlock = "Ügyfél:\n"
                + customerName.getText() + "\n"
                + "Tel: " + phone.getText() + "\n"
                + "Email: " + email.getText() + "\n";

        String vehBlock = "Jármű:\n"
                + "Rendszám: " + plate.getText() + "\n"
                + "Típus: " + brand.getText() + " " + model.getText() + "\n"
                + "Évjárat: " + year.getText() + "\n"
                + "Km óra: " + odometerKm.getText() + " km\n";
        
        PdfPTable custVehTable = new PdfPTable(2);
        custVehTable.setWidthPercentage(100);
        custVehTable.setWidths(new float[]{1f, 1f});
        custVehTable.addCell(makeCell(custBlock, normal10, 6f));
        custVehTable.addCell(makeCell(vehBlock, normal10, 6f));
        doc.add(custVehTable);
        doc.add(new Paragraph(" "));

        // --- Hiba leírása ---
        if (notBlank(faultDesc.getText())) {
             doc.add(new Paragraph("Hiba leírása:", bold12));
             doc.add(new Paragraph(faultDesc.getText(), normal10));
             doc.add(new Paragraph(" "));
        }
        
        // --- Elvégzett munka táblázat ---
        doc.add(new Paragraph("Elvégzett munka", bold12));
        PdfPTable workTable = new PdfPTable(6);
        workTable.setWidthPercentage(100);
        workTable.setWidths(new float[]{3f, 1f, 1f, 1f, 1f, 1f});

        addHeaderCell(workTable, "Munka");
        addHeaderCell(workTable, "Óra");
        addHeaderCell(workTable, "Egységár (Ft)");
        addHeaderCell(workTable, "ÁFA %");
        addHeaderCell(workTable, "Nettó (Ft)");
        addHeaderCell(workTable, "Bruttó (Ft)");

        for (ServiceJobCardWorkDesc w : workdescItems) {
            workTable.addCell(makeCell(w.getName(), normal10));
            workTable.addCell(makeCell(String.valueOf(w.getHours()), normal10));
            workTable.addCell(makeCell(String.valueOf(w.getRate_cents() / 100), normal10));
            workTable.addCell(makeCell(String.valueOf(w.getVat_percent()), normal10));
            workTable.addCell(makeCell(String.valueOf(calcNetForRow(w)), normal10));
            workTable.addCell(makeCell(String.valueOf(calcGrossForRow(w)), normal10));
        }
        doc.add(workTable);
        doc.add(new Paragraph(" "));

        // --- Alkatrészek táblázat ---
        doc.add(new Paragraph("Felhasznált anyagok / alkatrészek", bold12));
        PdfPTable partTable = new PdfPTable(7);
        partTable.setWidthPercentage(100);
        partTable.setWidths(new float[]{1.5f, 2f, 1f, 1f, 1f, 1f, 1f});

        addHeaderCell(partTable, "Cikkszám");
        addHeaderCell(partTable, "Megnevezés");
        addHeaderCell(partTable, "Menny.");
        addHeaderCell(partTable, "Egységár (Ft)");
        addHeaderCell(partTable, "ÁFA %");
        addHeaderCell(partTable, "Nettó (Ft)");
        addHeaderCell(partTable, "Bruttó (Ft)");

        for (ServiceJobCardPart p : partItems) {
            partTable.addCell(makeCell(p.getSku(), normal10));
            partTable.addCell(makeCell(p.getName(), normal10));
            partTable.addCell(makeCell(String.valueOf(p.getQuantity()), normal10));
            partTable.addCell(makeCell(String.valueOf(p.getUnit_price_cents() / 100), normal10));
            partTable.addCell(makeCell(String.valueOf(p.getVat_percent()), normal10));
            partTable.addCell(makeCell(String.valueOf(calcNetForPartRow(p)), normal10));
            partTable.addCell(makeCell(String.valueOf(calcGrossForPartRow(p)), normal10));
        }
        doc.add(partTable);
        doc.add(new Paragraph(" "));

        // --- Összesítés ---
        doc.add(new Paragraph("Összesítés", bold12));
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(60);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[]{3f, 1f});

        addTotalsRow(totalsTable, "Részösszeg (nettó):", subtotalNet.getText() + " Ft", normal10, normal10);
        addTotalsRow(totalsTable, "ÁFA összege:", vatAmount.getText() + " Ft", normal10, normal10);
        addTotalsRow(totalsTable, "Végösszeg (bruttó):", totalGross.getText() + " Ft", bold12, bold12);
        addTotalsRow(totalsTable, "Előleg:", advance.getText() + " Ft", normal10, normal10);
        addTotalsRow(totalsTable, "Fizetendő:", amountDue.getText() + " Ft", bold12, bold12);

        doc.add(totalsTable);

        doc.close();
    }

    // --- PDF Cella Segédek ---
    private void addHeaderCell(PdfPTable table, String text) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
        cell.setPadding(4f);
        table.addCell(cell);
    }
    private PdfPCell makeCell(String text, Font font) {
        return makeCell(text, font, 4f);
    }
    private PdfPCell makeCell(String text, Font font, float padding) {
        PdfPCell cell = new PdfPCell(new Paragraph(text == null ? "" : text, font));
        cell.setPadding(padding);
        return cell;
    }
    private void addTotalsRow(PdfPTable table, String label, String value, Font lf, Font rf) {
        PdfPCell c1 = new PdfPCell(new Paragraph(label, lf));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c1.setPadding(2f);

        PdfPCell c2 = new PdfPCell(new Paragraph(value, rf));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(2f);
        
        table.addCell(c1);
        table.addCell(c2);
    }
    
    // --- UI Segédfüggvények (Alerts) ---

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

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
    
    // --- String Konverter segédosztályok a táblázatokhoz ---

    /**
     * Konvertál Number és String között (pl. 15000 -> "15000").
     */
    private static class NumberStringConverter extends StringConverter<Number> {
        @Override
        public String toString(Number n) {
            return (n == null ? "" : n.toString());
        }

        @Override
        public Number fromString(String s) {
            try {
                if (s == null || s.isBlank()) return 0;
                // Kezeli a "10.5" és "10" formátumot is
                if (s.contains(".")) {
                    return Double.parseDouble(s.trim());
                } else {
                    return Integer.parseInt(s.trim());
                }
            } catch (Exception ex) {
                return 0;
            }
        }
    }
    
    /**
     * Konvertál Cent (pl. 1500000) és Forint String (pl. "15000") között.
     * A szerkesztéshez használjuk.
     */
    private static class CentToForintConverter extends StringConverter<Number> {
        @Override
        public String toString(Number n) {
            // A modellből cent-et kapunk (Number), Forintot jelenítünk meg
            return (n == null ? "0" : String.valueOf(n.intValue() / 100));
        }

        @Override
        public Number fromString(String s) {
            // A felhasználó Forintot ír be (String), cent-et adunk vissza (Number)
            try {
                if (s == null || s.isBlank()) return 0;
                return Integer.parseInt(s.trim()) * 100;
            } catch (Exception ex) {
                return 0;
            }
        }
    }
    
    /**
     * Csak megjelenítésre, Forintot (Number) konvertál String-gé.
     * Nem szerkesztésre való (fromString nem csinál semmit).
     */
    private static class ForintConverter extends StringConverter<Number> {
        @Override
        public String toString(Number n) {
            // A számolt érték már Forintban van
            return (n == null ? "0" : String.valueOf(n.intValue()));
        }

        @Override
        public Number fromString(String s) {
            // Ezt az oszlopot nem szerkesztjük, de a biztonság kedvéért...
            try {
                if (s == null || s.isBlank()) return 0;
                return Integer.parseInt(s.trim());
            } catch (Exception ex) {
                return 0;
            }
        }
    }
}


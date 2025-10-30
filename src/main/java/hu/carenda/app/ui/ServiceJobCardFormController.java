package hu.carenda.app.ui;

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
import javafx.util.StringConverter;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.control.cell.TextFieldTableCell;

/**
 * Ez a controller a serviceJobCard-form.fxml-hez. Két módban használjuk: -
 * meglévő munkalap szerkesztése - új munkalap létrehozása (appointment alapján
 * előtöltve)
 *
 * A megnyitó kód (Forms.serviceJobCard) mindig meghívja:
 * ctrl.setContext(cardToUse, custObj, vehObj);
 *
 * A régi ctrl.setEditing(...) továbbra is működik (átirányít ide).
 */
public class ServiceJobCardFormController {

    // ---------- FXML mezők ----------
    // fejléc
    @FXML
    private ComboBox<String> status;
    @FXML
    private ComboBox<User> assignee;

    // Alapadatok blokk
    @FXML
    private TextField jobcardNo;
    @FXML
    private DatePicker createdAt;

    @FXML
    private Spinner<Integer> createdHourSpinner;
    @FXML
    private Spinner<Integer> createdMinuteSpinner;

    @FXML
    private TextField customerName;
    @FXML
    private TextField phone;
    @FXML
    private TextField email;

    @FXML
    private TextField plate;
    @FXML
    private TextField vin;
    @FXML
    private TextField brand;
    @FXML
    private TextField model;
    @FXML
    private TextField year;
    @FXML
    private TextField odometerKm;

    @FXML
    private DatePicker modificationDate;
    @FXML
    private Spinner<Integer> modificationHourSpinner;
    @FXML
    private Spinner<Integer> modificationMinuteSpinner;

    @FXML
    private DatePicker dueDate;
    @FXML
    private Spinner<Integer> dueHourSpinner;
    @FXML
    private Spinner<Integer> dueMinuteSpinner;

    // Hiba és megállapítások blokk
    @FXML
    private TextArea faultDesc;
    @FXML
    private TextArea repairNote;
    @FXML
    private TextArea diagnosis;
    @FXML
    private TextArea internalNote;

    // Munka (labor) tábla
    @FXML
    private TableView<ServiceJobCardWorkDesc> workdescTable;
    @FXML
    private TableColumn<ServiceJobCardWorkDesc, String> colWorkdescName;
    @FXML
    private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescHours;
    @FXML
    private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescRate;
    @FXML
    private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescVat;
    @FXML
    private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescNet;
    @FXML
    private TableColumn<ServiceJobCardWorkDesc, Number> colWorkdescGross;

    @FXML
    private Button btnAddWorkdesc;
    @FXML
    private Button btnEditWorkdesc;
    @FXML
    private Button btnRemoveWorkdesc;

    // Alkatrész tábla
    @FXML
    private TableView<ServiceJobCardPart> partsTable;
    @FXML
    private TableColumn<ServiceJobCardPart, String> colPartSku;
    @FXML
    private TableColumn<ServiceJobCardPart, String> colPartName;
    @FXML
    private TableColumn<ServiceJobCardPart, Number> colPartQty;
    @FXML
    private TableColumn<ServiceJobCardPart, Number> colPartUnitPrice;
    @FXML
    private TableColumn<ServiceJobCardPart, Number> colPartVat;
    @FXML
    private TableColumn<ServiceJobCardPart, Number> colPartNet;
    @FXML
    private TableColumn<ServiceJobCardPart, Number> colPartGross;

    @FXML
    private Button btnAddPart;
    @FXML
    private Button btnEditPart;
    @FXML
    private Button btnRemovePart;

    // Összesítő blokk
    @FXML
    private TextField subtotalNet;
    @FXML
    private TextField vatAmount;
    @FXML
    private TextField totalGross;
    @FXML
    private TextField advance;
    @FXML
    private TextField amountDue;

    @FXML
    private CheckBox termsAccepted;

    @FXML
    private Button btnPdf;

    @FXML
    private Button btnPrint;

    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSave;

    // ---------- DAO-k ----------
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ServiceJobCardDao jobCardDao = new ServiceJobCardDao();
    private final UserDao userDao = new UserDao();

    private final ObservableList<ServiceJobCardWorkDesc> workdescItems = FXCollections.observableArrayList();
    private final ObservableList<ServiceJobCardPart> partItems = FXCollections.observableArrayList();

    private final ServiceJobCardWorkDescDao workDescDao = new ServiceJobCardWorkDescDao();
    private final ServiceJobCardPartDao partDao = new ServiceJobCardPartDao();

    // ---------- Aktuális állapot ----------
    private ServiceJobCard editing; // ezt mentjük
    private Customer customerData;  // előtöltött ügyfél (lehet null)
    private Vehicle vehicleData;    // előtöltött jármű (lehet null)

    // Formátum, amiben TÁROLUNK az adatbázisban (created_at stb.)
    // Pl. "2025-10-25T17:31"
    private static final DateTimeFormatter LDT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // Ez csak "most" bélyeghez kell (updated_at, finished_at, fallback)
    private static final DateTimeFormatter ISO_DB = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ---------- initialize() ----------
    @FXML
    public void initialize() {
        // státusz lista
        status.setItems(FXCollections.observableArrayList(
                "OPEN",
                "IN_PROGRESS",
                "READY",
                "DELIVERED",
                "CANCELLED"
        ));

        // DatePicker kezdőérték: ma
        createdAt.setValue(LocalDate.now());

        // Óra spinner: 0–23
        createdHourSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, 23, LocalDateTime.now().getHour(), 1
                )
        );
        createdHourSpinner.setEditable(true);

        // Perc spinner: 0–59
        createdMinuteSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0, 59, LocalDateTime.now().getMinute(), 5
                )
        );
        createdMinuteSpinner.setEditable(true);

        // --- Szerelők betöltése ---
        ObservableList<User> mechanics = FXCollections.observableArrayList(userDao.findAll());

        // "nincs hozzárendelve" dummy opció
        User noAssignee = new User();
        noAssignee.setId(0); // FONTOS: ez NEM lesz lementve 0-ként, lásd onSave()
        noAssignee.setFullName("Nincs hozzárendelve");
        noAssignee.setUsername("n/a");
        mechanics.add(0, noAssignee);

        assignee.setItems(mechanics);

        assignee.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User u) {
                if (u == null) {
                    return "";
                }
                return u.getFullName();
            }

            @Override
            public User fromString(String s) {
                if (s == null) {
                    return null;
                }
                for (User u : assignee.getItems()) {
                    if (s.equalsIgnoreCase(u.getFullName())) {
                        return u;
                    }
                }
                return null;
            }
        });

        // pénzügy mezők UI-ban nem kézzel szerkeszthetők (kivéve előleg)
        subtotalNet.setEditable(false);
        vatAmount.setEditable(false);
        totalGross.setEditable(false);
        amountDue.setEditable(false);

        // gombok
        btnPdf.setOnAction(e -> onExportPdf());

        btnSave.setOnAction(e -> onSave());
        btnCancel.setOnAction(e -> onCancel());

        // arrival/due spinnereket most nem használjuk → biztonság kedvéért állítsunk be 0-át
        if (modificationHourSpinner != null) {
            modificationHourSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0, 1)
            );
        }
        if (modificationMinuteSpinner != null) {
            modificationMinuteSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5)
            );
        }
        if (dueHourSpinner != null) {
            dueHourSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0, 1)
            );
        }
        if (dueMinuteSpinner != null) {
            dueMinuteSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5)
            );
        }

        // táblák engedélyezése szerkeszthető módra
        workdescTable.setEditable(true);
        partsTable.setEditable(true);

// oszlop -> property binding
        colWorkdescName.setCellValueFactory(data
                -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName())
        );
        colWorkdescHours.setCellValueFactory(data
                -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getHours())
        );
        colWorkdescRate.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getRate_cents() / 100
                )
        );
        colWorkdescVat.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getVat_percent())
        );
// a Nettó / Bruttó tipikusan számolt érték, ezt általában getterben számolod ki:
        colWorkdescNet.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(calcNetForRow(data.getValue()))
        );
        colWorkdescGross.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(calcGrossForRow(data.getValue()))
        );

// ugyanez partsTable-hez
        colPartSku.setCellValueFactory(data
                -> new javafx.beans.property.SimpleStringProperty(data.getValue().getSku())
        );
        colPartName.setCellValueFactory(data
                -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName())
        );
        colPartQty.setCellValueFactory(data
                -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getQuantity())
        );

        colPartUnitPrice.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getUnit_price_cents() / 100
                )
        );
        colPartVat.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getVat_percent())
        );
        colPartNet.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(calcNetForPartRow(data.getValue()))
        );
        colPartGross.setCellValueFactory(data
                -> new javafx.beans.property.SimpleIntegerProperty(calcGrossForPartRow(data.getValue()))
        );

// és végül:
        workdescTable.setItems(workdescItems);
        partsTable.setItems(partItems);

        // String oszlop: pl. munka megnevezése (colWorkdescName)
        colWorkdescName.setCellFactory(TextFieldTableCell.forTableColumn());
        colWorkdescName.setOnEditCommit(evt -> {
            ServiceJobCardWorkDesc row = evt.getRowValue();
            row.setName(evt.getNewValue());
            recalcTotals();
        });

// szám mezők: kell StringConverter<Number> mert TextFieldTableCell alapból Stringet ad
        var numberConverter = new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                return (n == null ? "" : n.toString());
            }

            @Override
            public Number fromString(String s) {
                try {
                    if (s == null || s.isBlank()) {
                        return 0;
                    }
                    return Double.parseDouble(s.trim());
                } catch (Exception ex) {
                    return 0;
                }
            }
        };

// Óra (hours) oszlop
        colWorkdescHours.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colWorkdescHours.setOnEditCommit(evt -> {
            ServiceJobCardWorkDesc row = evt.getRowValue();
            row.setHours(evt.getNewValue().doubleValue());
            recalcTotals();
        });

// Óradíj (rate_cents) – ez általában Ft/óra*100 formátumban van tárolva nálad
        colWorkdescRate.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colWorkdescRate.setOnEditCommit(evt -> {
            ServiceJobCardWorkDesc row = evt.getRowValue();
            // amit a user beírt, azt Ft-nak vesszük → *100 hogy centbe kerüljön
            int newRateFt = evt.getNewValue().intValue(); // pl. 15000
            row.setRate_cents(newRateFt * 100);           // 1500000 cent
            recalcTotals();
        });

// ÁFA %
        colWorkdescVat.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colWorkdescVat.setOnEditCommit(evt -> {
            ServiceJobCardWorkDesc row = evt.getRowValue();
            row.setVat_percent(evt.getNewValue().intValue());
            recalcTotals();
        });

// Alkatrész táblánál is ugyanez:
        colPartSku.setCellFactory(TextFieldTableCell.forTableColumn());
        colPartSku.setOnEditCommit(evt -> {
            ServiceJobCardPart row = evt.getRowValue();
            row.setSku(evt.getNewValue());
            recalcTotals();
        });

        colPartName.setCellFactory(TextFieldTableCell.forTableColumn());
        colPartName.setOnEditCommit(evt -> {
            ServiceJobCardPart row = evt.getRowValue();
            row.setName(evt.getNewValue());
            recalcTotals();
        });

        colPartQty.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colPartQty.setOnEditCommit(evt -> {
            ServiceJobCardPart row = evt.getRowValue();
            row.setQuantity(evt.getNewValue().doubleValue());
            recalcTotals();
        });

        colPartUnitPrice.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colPartUnitPrice.setOnEditCommit(evt -> {
            ServiceJobCardPart row = evt.getRowValue();
            int newFt = evt.getNewValue().intValue();      // pl 12000 Ft
            row.setUnit_price_cents(newFt * 100);          // tárolás: 1200000 cent
            recalcTotals();
        });

        colPartVat.setCellFactory(tc -> new TextFieldTableCell<>(numberConverter));
        colPartVat.setOnEditCommit(evt -> {
            ServiceJobCardPart row = evt.getRowValue();
            row.setVat_percent(evt.getNewValue().intValue());
            recalcTotals();
        });

        btnAddWorkdesc.setOnAction(e -> {
            ServiceJobCardWorkDesc nw = new ServiceJobCardWorkDesc();
            nw.setId(0); // új sor
            nw.setSjc_id(editing.getId() == null ? 0 : editing.getId());
            nw.setName("Új munka");
            nw.setHours(1.0);
            nw.setRate_cents(0);
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

// Alkatrész sorok
        btnAddPart.setOnAction(e -> {
            ServiceJobCardPart np = new ServiceJobCardPart();
            np.setId(0);
            np.setSjc_id(editing.getId() == null ? 0 : editing.getId());
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

    // ---------- régi kompatibilitás ----------
    public void setEditing(ServiceJobCard jc) {
        setContext(jc, null, null);
    }

// Többféle formátumból próbálunk parse-olni.
    private LocalDateTime tryParseCreatedAt(String raw) {
        if (!notBlank(raw)) {
            return null;
        }

        try {
            // pl. "yyyy-MM-dd'T'HH:mm"
            return LocalDateTime.parse(raw, LDT_FMT);
        } catch (Exception ignore) {
        }

        try {
            // pl. "2025-10-25T17:31:49.660023"
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignore) {
        }

        return null;
    }

    private LocalDateTime tryParseUpdatedAt(String raw) {
        if (!notBlank(raw)) {
            return null;
        }

        // 1) próbáljuk ISO_LOCAL_DATE_TIME-ként
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignore) {
        }

        // 2) fallback a rövidebb formátumra (yyyy-MM-dd'T'HH:mm),
        //    csak ha valami régebbi verzióban így ment.
        try {
            return LocalDateTime.parse(raw, LDT_FMT);
        } catch (Exception ignore) {
        }

        return null;
    }

    /**
     * A Forms.serviceJobCard(...) ezt hívja meg. - card: meglévő munkalap vagy
     * egy új, feltöltött draft objektum - cust: ügyfél (lehet null) - veh:
     * jármű (lehet null)
     */
    public void setContext(ServiceJobCard card, Customer cust, Vehicle veh) {
        this.editing = (card != null ? card : new ServiceJobCard());
        this.customerData = cust;
        this.vehicleData = veh;

        boolean isNew = (editing.getId() == null || editing.getId() == 0);

        // Ha új a munkalap, gondoskodunk pár defaultról
        if (isNew) {
            // munkalapszám, ha nincs
            if (!notBlank(editing.getJobcard_no())) {
                String generated = jobCardDao.generateNextJobCardNo();
                editing.setJobcard_no(generated);
            }

            // státusz default: OPEN
            if (!notBlank(editing.getStatus())) {
                editing.setStatus("OPEN");
            }

        }

        // És végül töltsük fel az űrlapot jelenlegi adatokkal
        fillFormFromData();
    }

    // ---------- Form kitöltése a modellekből ----------
    private void fillFormFromData() {
        // munkalap szám
        jobcardNo.setText(nvl(editing.getJobcard_no()));

        // státusz
        if (notBlank(editing.getStatus())) {
            status.getSelectionModel().select(editing.getStatus());
        } else {
            status.getSelectionModel().select("OPEN");
        }

        // szerelő kiválasztása comboboxban
        Integer techId = editing.getAssignee_user_id();
        if (techId != null && techId > 0) {
            // keressük meg a user listában
            for (User u : assignee.getItems()) {
                if (u.getId() == techId) {
                    assignee.getSelectionModel().select(u);
                    break;
                }
            }
        } else {
            // nincs hozzárendelve -> dummy user id=0
            for (User u : assignee.getItems()) {
                if (u.getId() == 0) {
                    assignee.getSelectionModel().select(u);
                    break;
                }
            }
        }

        // created_at visszatöltése:
        LocalDateTime createdLdt = tryParseCreatedAt(editing.getCreated_at());

        if (createdLdt == null) {
            // nincs elmentett created_at -> új lap vagy hibás formátum
            // fallback: MOST
            createdLdt = LocalDateTime.now();
        }

        // DatePicker napja
        createdAt.setValue(createdLdt.toLocalDate());

        // Óraspinner értéke
        if (createdHourSpinner.getValueFactory() != null) {
            createdHourSpinner.getValueFactory().setValue(createdLdt.getHour());
        }

        // Percspinner értéke
        if (createdMinuteSpinner.getValueFactory() != null) {
            createdMinuteSpinner.getValueFactory().setValue(createdLdt.getMinute());
        }

        // ---- updated_at visszatöltése a módosítási mezőkbe ----
        LocalDateTime updatedLdt = tryParseUpdatedAt(editing.getUpdated_at());

        if (updatedLdt != null) {
            // van mentett módosítási idő → ezt mutatjuk
            modificationDate.setValue(updatedLdt.toLocalDate());

            if (modificationHourSpinner.getValueFactory() != null) {
                modificationHourSpinner.getValueFactory().setValue(updatedLdt.getHour());
            }
            if (modificationMinuteSpinner.getValueFactory() != null) {
                modificationMinuteSpinner.getValueFactory().setValue(updatedLdt.getMinute());
            }

        } else {
            // nincs mentett módosítási infó → hagyjuk üresen/0-n
            modificationDate.setValue(null);

            if (modificationHourSpinner.getValueFactory() != null) {
                modificationHourSpinner.getValueFactory().setValue(0);
            }
            if (modificationMinuteSpinner.getValueFactory() != null) {
                modificationMinuteSpinner.getValueFactory().setValue(0);
            }
        }

        // ügyfél adatok
        if (customerData != null) {
            customerName.setText(nvl(customerData.getName()));
            phone.setText(nvl(customerData.getPhone()));
            email.setText(nvl(customerData.getEmail()));
        } else if (editing.getCustomer_id() != null && editing.getCustomer_id() > 0) {
            try {
                Customer c = customerDao.findById(editing.getCustomer_id());
                if (c != null) {
                    customerName.setText(nvl(c.getName()));
                    phone.setText(nvl(c.getPhone()));
                    email.setText(nvl(c.getEmail()));
                } else {
                    customerName.setText("");
                    phone.setText("");
                    email.setText("");
                }
            } catch (Exception e) {
                e.printStackTrace();
                customerName.setText("");
                phone.setText("");
                email.setText("");
            }
        } else {
            customerName.setText("");
            phone.setText("");
            email.setText("");
        }

        // jármű adatok
        if (vehicleData != null) {
            plate.setText(nvl(vehicleData.getPlate()));
            vin.setText(nvl(vehicleData.getVin()));
            brand.setText(nvl(vehicleData.getBrand()));
            model.setText(nvl(vehicleData.getModel()));
            year.setText(vehicleData.getYear() != null ? String.valueOf(vehicleData.getYear()) : "");
        } else if (editing.getVehicle_id() != null && editing.getVehicle_id() > 0) {
            try {
                Vehicle v = vehicleDao.findById(editing.getVehicle_id());
                if (v != null) {
                    plate.setText(nvl(v.getPlate()));
                    vin.setText(nvl(v.getVin()));
                    brand.setText(nvl(v.getBrand()));
                    model.setText(nvl(v.getModel()));
                    year.setText(v.getYear() != null ? String.valueOf(v.getYear()) : "");
                } else {
                    plate.setText("");
                    vin.setText("");
                    brand.setText("");
                    model.setText("");
                    year.setText("");
                }
            } catch (Exception e) {
                e.printStackTrace();
                plate.setText("");
                vin.setText("");
                brand.setText("");
                model.setText("");
                year.setText("");
            }
        } else {
            plate.setText("");
            vin.setText("");
            brand.setText("");
            model.setText("");
            year.setText("");
        }

        // km óra
        if (editing.getOdometer_km() != null && editing.getOdometer_km() > 0) {
            odometerKm.setText(String.valueOf(editing.getOdometer_km()));
        } else {
            odometerKm.setText("");
        }

        // --- tételsorok betöltése a DB-ből a táblákhoz ---
        workdescItems.clear();
        partItems.clear();

        if (editing.getId() != null && editing.getId() > 0) {
            // meglévő munkalap: lekérjük a sorait
            workdescItems.addAll(workDescDao.findByJobCard(editing.getId()));
            partItems.addAll(partDao.findByJobCard(editing.getId()));
        } else {
            // új munkalap: üresen indul
        }

// frissítsük az összesítőt azonnal
        recalcTotals();

        // ezek most nem kötődnek modellhez
        dueDate.setValue(null);

        // hibaleírások
        faultDesc.setText(nvl(editing.getFault_desc()));
        repairNote.setText(nvl(editing.getRepair_note()));
        diagnosis.setText(nvl(editing.getDiagnosis()));
        internalNote.setText(nvl(editing.getInternal_note()));

        // pénzügyi blokkok (egyelőre nincs kalkuláció)
        subtotalNet.setText("");
        vatAmount.setText("");
        totalGross.setText("");
        amountDue.setText("");

        // előleg
        if (editing.getAdvance_cents() != null) {
            double advanceFt = editing.getAdvance_cents() / 100.0;
            // ha nem akarsz tizedeseket (mert forintnál nincs fillér), formázd egészre:
            advance.setText(String.format("%.0f", advanceFt));
        } else {
            advance.setText("0");
        }

        // --- pénzügyi összesítő a view_sjc_totals alapján ---
        if (editing.getId() != null && editing.getId() > 0) {
            var totals = jobCardDao.fetchTotals(editing.getId());
            if (totals != null) {
                // a view centekben ad vissza mindent. Te forintot akarsz mutatni
                // (nálad eddig is úgy tűnt, hogy advance Ft-ban van beírva a TextField-be).
                // Mivel a view_sjc_totals net_cents, gross_cents stb centben vannak,
                // osszuk el 100-zal, kerekítsünk normálisan forintra.

                int subtotalFt = (int) Math.round(totals.subtotal_net_cents / 100.0);
                int vatFt = (int) Math.round(totals.vat_cents / 100.0);
                int grossFt = (int) Math.round(totals.total_gross_cents / 100.0);
                int dueFt = (int) Math.round(totals.amount_due_cents / 100.0);

                Integer advCents = totals.advance_cents;
                int advFt = advCents == null ? 0 : (int) Math.round(advCents / 100.0);

                subtotalNet.setText(String.valueOf(subtotalFt));
                vatAmount.setText(String.valueOf(vatFt));
                totalGross.setText(String.valueOf(grossFt));
                advance.setText(String.valueOf(advFt));
                amountDue.setText(String.valueOf(dueFt));
            } else {
                subtotalNet.setText("0");
                vatAmount.setText("0");
                totalGross.setText("0");
                // advance-et meghagytuk fentebb, amountDue-t számoljuk:
                try {
                    int advFt = Integer.parseInt(advance.getText().trim());
                    amountDue.setText(String.valueOf(0 - advFt));
                } catch (Exception ignore) {
                    amountDue.setText("0");
                }
            }
        } else {
            // új lap: minden nulla
            subtotalNet.setText("0");
            vatAmount.setText("0");
            totalGross.setText("0");
            // advance-et meghagytad "0"-ra fentebb
            amountDue.setText("0");
        }

        termsAccepted.setSelected(true);
    }

    // ---------- Mentés gombra ----------
    @FXML
    public void onSave() {
        try {
            // 1) GUI -> editing

            // munkalap szám
            editing.setJobcard_no(emptyToNull(jobcardNo.getText()));

            // státusz
            String stSel = status.getSelectionModel().getSelectedItem();
            if (!notBlank(stSel)) {
                stSel = "OPEN";
            }
            editing.setStatus(stSel);

            // szerelő
            User selectedUser = assignee.getSelectionModel().getSelectedItem();

            editing.setAssignee_user_id(selectedUser.getId());

            // km óra
            editing.setOdometer_km(parseIntSafe(odometerKm.getText()));

            // leírások
            editing.setFault_desc(safeTrim(faultDesc.getText()));
            editing.setRepair_note(safeTrim(repairNote.getText()));
            editing.setDiagnosis(safeTrim(diagnosis.getText()));
            editing.setInternal_note(safeTrim(internalNote.getText()));

            // előleg
            editing.setAdvance_cents(
                    parseIntSafe(advance.getText()) != null
                    ? parseIntSafe(advance.getText()) * 100
                    : null
            );

            // created_at felépítése a DatePicker + óra + perc alapján, ha eddig nem volt
            boolean isNew = (editing.getId() == null || editing.getId() == 0);

            // dátum a DatePickerből
            LocalDate pickedDate = createdAt.getValue();
            // óra/percek a spinnekből
            Integer hh = createdHourSpinner.getValue();
            Integer mm = createdMinuteSpinner.getValue();

            if (pickedDate == null) {
                // elvileg nem kéne előforduljon, de fallback a biztonság kedvéért
                pickedDate = LocalDate.now();
            }
            if (hh == null) {
                hh = 0;
            }
            if (mm == null) {
                mm = 0;
            }

            LocalDateTime ldtForSave = pickedDate.atTime(hh, mm);

            // Ezt a stringet rakjuk DB formátumba: "yyyy-MM-dd'T'HH:mm"
            String createdAtStr = ldtForSave.format(LDT_FMT);

            // created_at logika:
            // - új rekordnál mindig beállítjuk
            // - meglévőnél csak akkor, ha eddig üres volt
            if (isNew || !notBlank(editing.getCreated_at())) {
                editing.setCreated_at(createdAtStr);
            }

            // updated_at: MINDIG mostani idő (mentés időpontja), függetlenül attól,
            // mi volt a modificationDate mezőben
            String nowIso = LocalDateTime.now().format(ISO_DB);
            editing.setUpdated_at(nowIso);

            // delivered státusznál finished_at is kapjon értéket
            if ("DELIVERED".equalsIgnoreCase(stSel)) {
                editing.setFinished_at(nowIso);
            }

            // NORMALIZÁLÓ BIZTONSÁGI LÉPÉS:
            // assignee_user_id nem mehet 0-val az adatbázisba (FK hiba lenne)
            if (editing.getAssignee_user_id() != null && editing.getAssignee_user_id() <= 0) {
                editing.setAssignee_user_id(null);
            }

            // DEBUG
            System.out.println("=== DEBUG ServiceJobCard BEFORE SAVE ===");
            System.out.println("jobcard_no           = " + editing.getJobcard_no());
            System.out.println("appointment_id       = " + editing.getAppointment_id());
            System.out.println("vehicle_id           = " + editing.getVehicle_id());
            System.out.println("customer_id          = " + editing.getCustomer_id());
            System.out.println("assignee_user_id     = " + editing.getAssignee_user_id());
            System.out.println("created_at           = " + editing.getCreated_at());
            System.out.println("updated_at           = " + editing.getUpdated_at());
            System.out.println("finished_at          = " + editing.getFinished_at());
            System.out.println("odometer_km          = " + editing.getOdometer_km());
            System.out.println("fuel_level_eighths   = " + editing.getFuel_level_eighths());
            System.out.println("advance_cents        = " + editing.getAdvance_cents());
            System.out.println("=========================================");

            // 2) mentés adatbázisba (fő munkalap)
            if (isNew) {
                jobCardDao.insert(editing);
            } else {
                jobCardDao.update(editing);
            }

// MOSTANTÓL biztos, hogy editing.getId() érvényes
            int sjcId = editing.getId();

// 3) a kapcsolódó tételek mentése
            workDescDao.deleteByJobCard(sjcId);
            partDao.deleteByJobCard(sjcId);

// újra beszúrjuk az aktuális listából
            int lineNo = 1;
            for (ServiceJobCardWorkDesc w : workdescItems) {
                w.setSjc_id(sjcId);
                workDescDao.insert(
                        sjcId,
                        w.getName(),
                        w.getHours(),
                        w.getRate_cents(),
                        w.getVat_percent(),
                        lineNo
                );
                lineNo++;
            }

            int partLineNo = 1;
            for (ServiceJobCardPart p : partItems) {
                p.setSjc_id(sjcId);
                partDao.insert(
                        sjcId,
                        p.getSku(),
                        p.getName(),
                        p.getQuantity(),
                        p.getUnit_price_cents(),
                        p.getVat_percent(),
                        partLineNo
                );
                partLineNo++;
            }

// 4) ablak bezárása
            Stage stg = (Stage) btnSave.getScene().getWindow();
            stg.setUserData(Boolean.TRUE);
            stg.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Mentési hiba a munkalapon:\n" + ex.getMessage()
            ).showAndWait();
        }
    }

    // ---------- Mégse gombra ----------
    @FXML
    public void onCancel() {
        Stage stg = (Stage) btnCancel.getScene().getWindow();
        stg.close();
    }

    // ---------- Kis segédfüggvények ----------
    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nvl(String s) {
        return (s == null) ? "" : s;
    }

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer parseIntSafe(String txt) {
        if (txt == null) {
            return null;
        }
        String t = txt.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void recalcTotals() {
        double netSum = 0.0;
        double grossSum = 0.0;

        // munkadíjak
        for (ServiceJobCardWorkDesc w : workdescItems) {
            double netFt = w.getHours() * (w.getRate_cents() / 100.0);
            double grossFt = netFt * (1.0 + (w.getVat_percent() / 100.0));
            netSum += netFt;
            grossSum += grossFt;
        }

        // alkatrészek
        for (ServiceJobCardPart p : partItems) {
            double netFt = p.getQuantity() * (p.getUnit_price_cents() / 100.0);
            double grossFt = netFt * (1.0 + (p.getVat_percent() / 100.0));
            netSum += netFt;
            grossSum += grossFt;
        }

        double vatAmountFt = grossSum - netSum;

        // előleg (text mező) levonása
        Integer advInt = parseIntSafe(advance.getText());
        double advFt = (advInt == null ? 0.0 : advInt);
        double dueFt = grossSum - advFt;

        subtotalNet.setText(String.format("%.0f", netSum));
        vatAmount.setText(String.format("%.0f", vatAmountFt));
        totalGross.setText(String.format("%.0f", grossSum));
        amountDue.setText(String.format("%.0f", dueFt));
    }

    // kis számolók a táblázat oszlopaihoz
    private int calcNetForRow(ServiceJobCardWorkDesc w) {
        if (w == null) {
            return 0;
        }
        // hours * rate_cents -> ez "cent"
        double netCents = w.getHours() * w.getRate_cents();
        // mi a táblában forintot szeretnénk látni, egészre kerekítve
        return (int) Math.round(netCents / 100.0);
    }

    private int calcGrossForRow(ServiceJobCardWorkDesc w) {
        if (w == null) {
            return 0;
        }
        double netCents = w.getHours() * w.getRate_cents();
        double grossCents = netCents * (1.0 + (w.getVat_percent() / 100.0));
        return (int) Math.round(grossCents / 100.0);
    }

    private int calcNetForPartRow(ServiceJobCardPart p) {
        if (p == null) {
            return 0;
        }
        // quantity * unit_price_cents -> cent
        double netCents = p.getQuantity() * p.getUnit_price_cents();
        return (int) Math.round(netCents / 100.0);
    }

    private int calcGrossForPartRow(ServiceJobCardPart p) {
        if (p == null) {
            return 0;
        }
        double netCents = p.getQuantity() * p.getUnit_price_cents();
        double grossCents = netCents * (1.0 + (p.getVat_percent() / 100.0));
        return (int) Math.round(grossCents / 100.0);
    }

    private void onExportPdf() {
        try {
            // kérdezzük meg hová mentse
            var chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Munkalap PDF mentése");
            String safeJobNo = (editing.getJobcard_no() != null && !editing.getJobcard_no().isBlank())
                    ? editing.getJobcard_no().replaceAll("[^0-9A-Za-z_-]", "_")
                    : "munkalap";
            chooser.setInitialFileName(safeJobNo + ".pdf");
            var file = chooser.showSaveDialog(btnPdf.getScene().getWindow());
            if (file == null) {
                return; // user cancel
            }

            generatePdfToFile(file.getAbsolutePath());

            new Alert(Alert.AlertType.INFORMATION,
                    "PDF sikeresen mentve:\n" + file.getAbsolutePath()
            ).showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "PDF mentési hiba:\n" + ex.getMessage()
            ).showAndWait();
        }
    }

    private void generatePdfToFile(String path) throws Exception {
        // importok ehhez a metódushoz:
        // import com.lowagie.text.*;
        // import com.lowagie.text.pdf.*;

        com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 36, 36, 36, 36);
        com.lowagie.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(path));
        doc.open();

        // Fejléc
        var bold18 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
        var bold12 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
        var normal10 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL);

        String jobNo = nvl(editing.getJobcard_no());
        String created = nvl(editing.getCreated_at());
        String statusTxt = nvl(editing.getStatus());

        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Munkalap #" + jobNo, bold18);
        title.setSpacingAfter(4f);
        doc.add(title);

        doc.add(new com.lowagie.text.Paragraph("Állapot: " + statusTxt, normal10));
        doc.add(new com.lowagie.text.Paragraph("Felvétel ideje: " + created, normal10));
        doc.add(new com.lowagie.text.Paragraph(" ")); // üres sor

        // Ügyfél és Jármű blokk két oszlopban
        String custBlock
                = "Ügyfél:\n"
                + customerName.getText() + "\n"
                + "Tel: " + phone.getText() + "\n"
                + "Email: " + email.getText() + "\n";

        String vehBlock
                = "Jármű:\n"
                + "Rendszám: " + plate.getText() + "\n"
                + "Típus: " + brand.getText() + " " + model.getText() + "\n"
                + "Évjárat: " + year.getText() + "\n"
                + "Km óra: " + odometerKm.getText() + " km\n";

        com.lowagie.text.pdf.PdfPTable custVehTable = new com.lowagie.text.pdf.PdfPTable(2);
        custVehTable.setWidthPercentage(100);
        custVehTable.setWidths(new float[]{1f, 1f});

        com.lowagie.text.pdf.PdfPCell custCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(custBlock, normal10));
        custCell.setPadding(6f);

        com.lowagie.text.pdf.PdfPCell vehCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(vehBlock, normal10));
        vehCell.setPadding(6f);

        custVehTable.addCell(custCell);
        custVehTable.addCell(vehCell);

        doc.add(custVehTable);
        doc.add(new com.lowagie.text.Paragraph(" ")); // üres sor

        // Elvégzett munka táblázat
        doc.add(new com.lowagie.text.Paragraph("Elvégzett munka", bold12));
        com.lowagie.text.pdf.PdfPTable workTable = new com.lowagie.text.pdf.PdfPTable(6);
        workTable.setWidthPercentage(100);
        workTable.setWidths(new float[]{3f, 1f, 1f, 1f, 1f, 1f});

        addHeaderCell(workTable, "Munka");
        addHeaderCell(workTable, "Óra");
        addHeaderCell(workTable, "Egységár (Ft)");
        addHeaderCell(workTable, "ÁFA %");
        addHeaderCell(workTable, "Nettó (Ft)");
        addHeaderCell(workTable, "Bruttó (Ft)");

        for (ServiceJobCardWorkDesc w : workdescItems) {
            int netFt = calcNetForRow(w);
            int grossFt = calcGrossForRow(w);
            workTable.addCell(makeCell(w.getName(), normal10));
            workTable.addCell(makeCell(String.valueOf(w.getHours()), normal10));
            workTable.addCell(makeCell(String.valueOf(w.getRate_cents() / 100), normal10));
            workTable.addCell(makeCell(String.valueOf(w.getVat_percent()), normal10));
            workTable.addCell(makeCell(String.valueOf(netFt), normal10));
            workTable.addCell(makeCell(String.valueOf(grossFt), normal10));
        }

        doc.add(workTable);
        doc.add(new com.lowagie.text.Paragraph(" "));

        // Alkatrészek táblázat
        doc.add(new com.lowagie.text.Paragraph("Felhasznált anyagok / alkatrészek", bold12));
        com.lowagie.text.pdf.PdfPTable partTable = new com.lowagie.text.pdf.PdfPTable(7);
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
            int netFt = calcNetForPartRow(p);
            int grossFt = calcGrossForPartRow(p);
            partTable.addCell(makeCell(p.getSku(), normal10));
            partTable.addCell(makeCell(p.getName(), normal10));
            partTable.addCell(makeCell(String.valueOf(p.getQuantity()), normal10));
            partTable.addCell(makeCell(String.valueOf(p.getUnit_price_cents() / 100), normal10));
            partTable.addCell(makeCell(String.valueOf(p.getVat_percent()), normal10));
            partTable.addCell(makeCell(String.valueOf(netFt), normal10));
            partTable.addCell(makeCell(String.valueOf(grossFt), normal10));
        }

        doc.add(partTable);
        doc.add(new com.lowagie.text.Paragraph(" "));

        // Összesítés
        doc.add(new com.lowagie.text.Paragraph("Összesítés", bold12));

        String subtotalFt = subtotalNet.getText();
        String vatFt = vatAmount.getText();
        String grossFt = totalGross.getText();
        String advanceFt = advance.getText();
        String dueFt = amountDue.getText();

        com.lowagie.text.pdf.PdfPTable totalsTable = new com.lowagie.text.pdf.PdfPTable(2);
        totalsTable.setWidthPercentage(60);
        totalsTable.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[]{3f, 1f});

        addTotalsRow(totalsTable, "Részösszeg (nettó):", subtotalFt + " Ft", normal10, normal10);
        addTotalsRow(totalsTable, "ÁFA összege:", vatFt + " Ft", normal10, normal10);
        addTotalsRow(totalsTable, "Végösszeg (bruttó):", grossFt + " Ft", bold12, bold12);
        addTotalsRow(totalsTable, "Előleg:", advanceFt + " Ft", normal10, normal10);
        addTotalsRow(totalsTable, "Fizetendő:", dueFt + " Ft", bold12, bold12);

        doc.add(totalsTable);

        doc.close();
    }

    private void addHeaderCell(com.lowagie.text.pdf.PdfPTable table, String text) {
        var font = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
        var cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(text, font));
        cell.setBackgroundColor(new java.awt.Color(230,230,230));
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private com.lowagie.text.pdf.PdfPCell makeCell(String text, com.lowagie.text.Font font) {
        var cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(text == null ? "" : text, font));
        cell.setPadding(4f);
        return cell;
    }

    private void addTotalsRow(com.lowagie.text.pdf.PdfPTable table,
            String label,
            String value,
            com.lowagie.text.Font lf,
            com.lowagie.text.Font rf) {

        var c1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(label, lf));
        c1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);

        var c2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(value, rf));
        c2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);

        table.addCell(c1);
        table.addCell(c2);
    }

}

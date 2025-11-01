package hu.carenda.app.ui;

import hu.carenda.app.model.Appointment;
import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.repository.AppointmentDao;
import hu.carenda.app.repository.CustomerDao;
import hu.carenda.app.repository.VehicleDao;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller a Naptár nézet (schedule.xml) kezeléséhez.
 * Felelős a napi és heti nézet kirajzolásáért, valamint az időpontok
 * vizuális megjelenítéséért és szerkesztésének indításáért.
 */
public class ScheduleFormController {

    // --- FXML ---
    @FXML
    private DatePicker dayPicker;
    @FXML
    private ToggleGroup viewToggle;
    @FXML
    private RadioButton dayRadio, weekRadio;
    @FXML
    private VBox timeGutter; // Óra sáv (bal oldal)
    @FXML
    private Pane canvas; // A rajzolási terület, ahol a blokkok megjelennek

    // --- DAO-k és cache-elt listák a címkékhez ---
    private final AppointmentDao apptDao = new AppointmentDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();

    /** Gyorsítótár az ügyfélnevek megjelenítéséhez. */
    private List<Customer> customers = new ArrayList<>();
    /** Gyorsítótár a rendszámok megjelenítéséhez. */
    private List<Vehicle> vehicles = new ArrayList<>();

    // --- Rács konstansok ---
    private static final int DAY_START_HOUR = 8;
    private static final int DAY_END_HOUR = 18;
    private static final double HOUR_PIXELS = 60.0;
    private static final double COL_WIDTH = 150.0;
    private static final double DAY_HEADER_OFFSET = 24.0; // Hely a nap fejlécének

    /** Egységes formátum az LocalDateTime tárolásához és olvasásához (mint az AppointmentFormController-ben). */
    private static final DateTimeFormatter LDT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @FXML
    public void initialize() {
        if (dayPicker.getValue() == null) {
            dayPicker.setValue(LocalDate.now());
        }
        // Listenerek a nézet vagy a dátum változására
        viewToggle.selectedToggleProperty().addListener((obs, o, n) -> reload());
        dayPicker.valueProperty().addListener((obs, o, n) -> reload());
        
        // Kezdeti nézet betöltése
        reload();
    }

    /**
     * A DashboardController hívja meg, amikor a felhasználó visszalép erre a fülre,
     * hogy biztosan friss adatokat lásson.
     */
    public void hardRefreshSchedule() {
        // csak annyit csinál, hogy újratölt mindent az aktuális nap / heti nézet szerint
        reload();
    }

    /**
     * Eseménykezelő: Új időpont gomb.
     * Megnyitja az űrlapot az aktuálisan kiválasztott nap 10:00 órájára időzítve.
     */
    @FXML
    public void onNew() {
        var start = dayPicker.getValue().atTime(10, 0);
        var a = new Appointment();
        a.setStartTs(start.format(LDT_FMT));
        a.setDurationMinutes(60);
        a.setStatus("TERVEZETT");

        // JAVÍTVA: A 'Window owner' az első paraméter az egységes Forms.java miatt.
        boolean saved = Forms.openAppointmentDialog(canvas.getScene().getWindow(), a);
        if (saved) {
            reload(); // Mentés után újrarajzoljuk a naptárat
        }
    }

    /**
     * Eseménykezelő: Törlés gomb.
     * (Megjegyzés: Ehhez a funkcióhoz ki kellene találni,
     * hogy melyik a "kijelölt" időpont a naptárban.)
     */
    @FXML
    public void onDelete() {
        // TODO: A naptárnézetből való törléshez implementálni kell egy "kijelölés" logikát.
        // Jelenleg nincs egyértelműen kijelölt elem, amit törölhetnénk.
        showInfo("Funkció nincs implementálva", "Időpont törlése a naptárnézetből még nem támogatott.");
    }

    /**
     * Eseménykezelő: Frissítés gomb (vagy bármilyen változás).
     * Törli a vásznat és újrarajzolja az egészet az aktuális beállítások szerint.
     */
    @FXML
    public void reload() {
        // 1. Frissítjük a gyorsítótárakat (ügyfélnevek, rendszámok)
        try {
            customers = customerDao.findAll();
            vehicles = vehicleDao.findAll();
        } catch (Exception e) {
            showError("Adatbázis hiba", "Nem sikerült betölteni az ügyfél vagy jármű adatokat: " + e.getMessage());
        }


        // 2. Törlünk mindent
        canvas.getChildren().clear();
        timeGutter.getChildren().clear();

        // 3. Bal oldali óracímkék kirajzolása (8:00, 9:00, ...)
        for (int h = DAY_START_HOUR; h <= DAY_END_HOUR; h++) {
            var lbl = new Label(String.format("%02d:00", h));
            lbl.setMinHeight(HOUR_PIXELS);
            lbl.setPrefHeight(HOUR_PIXELS);
            lbl.setMaxHeight(HOUR_PIXELS);
            lbl.setPrefWidth(60);
            timeGutter.getChildren().add(lbl);
        }

        // 4. Döntés a nézet (napi/heti) alapján
        var selectedDay = dayPicker.getValue();
        if (dayRadio.isSelected()) {
            drawDay(selectedDay);
        } else {
            // Hétfő kiszámítása
            var monday = selectedDay.minusDays((selectedDay.getDayOfWeek().getValue() + 6) % 7);
            drawWeek(monday);
        }
    }

    // --- NAPI NÉZET ---

    /**
     * Kirajzolja az egy napos nézetet (fejléc, rács, időpontok).
     * @param day A megjelenítendő nap.
     */
    private void drawDay(LocalDate day) {
        var from = day.atTime(0, 0);
        var to = day.plusDays(1).atTime(0, 0);
        var appts = apptDao.findBetween(from.toString(), to.toString());

        double height = DAY_HEADER_OFFSET + (DAY_END_HOUR - DAY_START_HOUR) * HOUR_PIXELS;
        canvas.setPrefHeight(height);
        canvas.setPrefWidth(1000); // Fix szélesség napi nézetben

        // --- Cím felül: 2025.10.27. Hétfő ---
        var dayNameFormatter = DateTimeFormatter.ofPattern("EEEE", new Locale("hu"));
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.", new Locale("hu"));

        String dayName = day.format(dayNameFormatter); // "hétfő"
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1); // "Hétfő"
        String headerText = dateFormatter.format(day) + " " + dayName;

        var headerLabel = new Label(headerText);
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        headerLabel.setLayoutX(8);
        headerLabel.setLayoutY(4);
        canvas.getChildren().add(headerLabel);

        // --- Rácsvonalak ---
        addHourLines(canvas.getPrefWidth());

        // --- Időpontok elrendezése és kirajzolása ---
        var placedList = layoutDayAppointments(appts);

        for (var placed : placedList) {
            var a = placed.appt;
            LocalDateTime start;
            try {
                start = LocalDateTime.parse(a.getStartTs(), LDT_FMT);
            } catch (DateTimeParseException e) {
                continue; // Hibás dátum, kihagyjuk
            }

            double minutesFromStart = Duration
                    .between(day.atTime(DAY_START_HOUR, 0), start).toMinutes();

            // JAVÍTVA (typo): + + -> +
            double y = DAY_HEADER_OFFSET
                    + Math.max(0, minutesFromStart * (HOUR_PIXELS / 60.0));
            
            double h = Math.max(22, a.getDurationMinutes() * (HOUR_PIXELS / 60.0));

            // Szélesség és pozíció az oszlop-elosztás alapján
            double fullWidth = 980; // Teljes elérhető szélesség
            double w = fullWidth / placed.totalCols;
            double x = placed.columnIndex * w;

            var block = makeBlock(a, x, y, w - 4, h); // -4px margó
            canvas.getChildren().add(block);
        }
    }

    // --- HETI NÉZET ---

    /**
     * Kirajzolja a hét napos nézetet (fejléc, rács, időpontok).
     * @param monday A hétfői nap (a hét kezdete).
     */
    private void drawWeek(LocalDate monday) {
        var from = monday.atStartOfDay();
        var to = monday.plusDays(7).atStartOfDay();
        var appts = apptDao.findBetween(from.toString(), to.toString());

        double height = (DAY_END_HOUR - DAY_START_HOUR) * HOUR_PIXELS;
        double totalWidth = COL_WIDTH * 7 + 10;
        canvas.setPrefHeight(height);
        canvas.setPrefWidth(totalWidth);

        // --- Rácsvonalak ---
        addHourLines(totalWidth);     // Vízszintes (órák)
        addDaySeparators(totalWidth); // Függőleges (napok)

        // --- Oszlop-fejlécek (H, K, Sz...) ---
        var formatter = DateTimeFormatter.ofPattern("EEEE", new Locale("hu"));
        for (int d = 0; d < 7; d++) {
            var head = new Label(monday.plusDays(d).format(formatter));
            head.setStyle("-fx-font-weight: bold;");
            head.setLayoutX(d * COL_WIDTH + 6);
            head.setLayoutY(4);
            canvas.getChildren().add(head);
        }

        // 1) Időpontok szétválogatása napok szerint
        List<List<Appointment>> perDay = new ArrayList<>();
        for (int i = 0; i < 7; i++) perDay.add(new ArrayList<>());

        for (var a : appts) {
            LocalDateTime start;
            try {
                start = LocalDateTime.parse(a.getStartTs(), LDT_FMT);
            } catch (DateTimeParseException e) {
                continue; // Hibás dátum, kihagyjuk
            }
            
            int dayIndex = (int) ChronoUnit.DAYS.between(monday, start.toLocalDate());
            if (dayIndex >= 0 && dayIndex <= 6) {
                perDay.get(dayIndex).add(a);
            }
        }

        // 2) Minden nap kirajzolása (a saját oszlop-elosztásával)
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            var dayList = perDay.get(dayIndex);
            var placedList = layoutDayAppointments(dayList);

            for (var placed : placedList) {
                var a = placed.appt;
                LocalDateTime start;
                try {
                    start = LocalDateTime.parse(a.getStartTs(), LDT_FMT);
                } catch (DateTimeParseException e) {
                    continue;
                }
                
                double minutesFromStart = Duration
                        .between(start.toLocalDate().atTime(DAY_START_HOUR, 0), start)
                        .toMinutes();

                // Napi oszlop bal széle
                double baseX = dayIndex * COL_WIDTH + 4;
                
                // JAVÍTVA: Hiányzott a DAY_HEADER_OFFSET
                double y = DAY_HEADER_OFFSET + Math.max(0, minutesFromStart * (HOUR_PIXELS / 60.0));
                double h = Math.max(22, a.getDurationMinutes() * (HOUR_PIXELS / 60.0));

                // Elosztás az oszlopon belül
                double fullWidth = COL_WIDTH - 8;
                double w = fullWidth / placed.totalCols;
                double x = baseX + placed.columnIndex * w;

                var block = makeBlock(a, x, y, w - 4, h); // -4px margó
                canvas.getChildren().add(block);
            }
        }
    }

    // --- Blokk készítő + kattintás kezelő ---

    /**
     * Létrehoz egy vizuális "blokkot" (VBox) egy időponthoz,
     * beállítja a stílusát és a kattintás eseménykezelőjét.
     */
    private Node makeBlock(Appointment a, double x, double y, double w, double h) {
        var box = new VBox(2);
        box.setLayoutX(x);
        box.setLayoutY(y);
        box.setPrefSize(w, h);
        box.getStyleClass().add("appt"); // Alap stílus

        // Státusz szerinti színosztály hozzáadása (CSS-ben definiálva)
        String status = (a.getStatus() == null) ? "" : a.getStatus().trim().toUpperCase();
        switch (status) {
            case "LEMONDOTT":
                box.getStyleClass().add("st-canceled");
                break;
            case "BEFEJEZETT":
                box.getStyleClass().add("st-done");
                break;
            case "TERVEZETT":
            default:
                box.getStyleClass().add("st-planned");
                break;
        }

        // Név és rendszám megkeresése a cache-ből
        // JAVÍTVA: Integer-ek összehasonlítása Objects.equals-szel
        String cust = customers.stream()
                .filter(c -> Objects.equals(c.getId(), a.getCustomerId()))
                .findFirst().map(Customer::getName).orElse("Ismeretlen ügyfél");
        String veh = vehicles.stream()
                .filter(v -> Objects.equals(v.getId(), a.getVehicleId()))
                .findFirst().map(Vehicle::getPlate).orElse("Ismeretlen jármű");

        var l1 = new Label(cust);
        var l2 = new Label(veh);
        l1.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        l2.setStyle("-fx-font-size: 11;");
        box.getChildren().addAll(l1, l2);

        // Kattintás: Megnyitja a szerkesztő dialógust
        box.setOnMouseClicked(e -> {
            e.consume();
            // A 'Window owner' az első paraméter.
            if (Forms.openAppointmentDialog(canvas.getScene().getWindow(), a)) {
                reload(); // Ha mentés történt, újrarajzolunk
            }
        });

        return box;
    }

    // --- GRID VONALAK (órák / napok) ---

    /** Kirajzolja a vízszintes óra-vonalakat. */
    private void addHourLines(double width) {
        for (int h = DAY_START_HOUR; h <= DAY_END_HOUR; h++) {
            double y = DAY_HEADER_OFFSET + (h - DAY_START_HOUR) * HOUR_PIXELS;
            Line line = new Line(0, y, width, y);
            styleGridLine(line); // JAVÍTVA: felesleges paraméter törölve
            canvas.getChildren().add(line);
        }
    }

    /** Kirajzolja a függőleges nap-elválasztó vonalakat (heti nézetben). */
    private void addDaySeparators(double totalWidth) {
        for (int d = 0; d <= 7; d++) {
            double x = d * COL_WIDTH + 4; // Igazodik a drawWeek()-beli x-hez
            Line v = new Line(x, 0, x, canvas.getPrefHeight());
            styleGridLine(v);
            canvas.getChildren().add(v);
        }
    }

    /** Egységes stílus a rácsvonalaknak. */
    private void styleGridLine(Line line) { // JAVÍTVA: felesleges paraméter törölve
        line.setStrokeWidth(1.0); // JAVÍTVA: áthelyezve ide
        line.setOpacity(0.25);
        line.setStroke(Color.GRAY);
        line.setMouseTransparent(true); // Ne fogja meg az egeret
    }

    // --- ELRENDEZÉSI LOGIKA (Ütközéskezelés) ---

    /**
     * Segédosztály az elrendezéshez.
     */
    private static class PlacedAppt {
        Appointment appt;
        int columnIndex;  // 0 vagy 1
        int totalCols;    // 1 vagy 2 az adott "overlap-csoportban"
    }

    /**
     * Egy adott nap időpontjait (ugyanabban az oszlopban / napon) széthasítja
     * max 2 oszlopra, ha átfedés van.
     * Feltételezi, hogy nincs 3+ átfedés, mert azt az üzleti logika tiltja.
     */
    private List<PlacedAppt> layoutDayAppointments(List<Appointment> apptsForThatDay) {
        // Rendezzük kezdési idő szerint
        apptsForThatDay.sort((a, b) -> {
            try {
                // Formatter használata a parsoláshoz
                var sa = LocalDateTime.parse(a.getStartTs(), LDT_FMT);
                var sb = LocalDateTime.parse(b.getStartTs(), LDT_FMT);
                return sa.compareTo(sb);
            } catch (DateTimeParseException e) {
                return 0;
            }
        });

        List<PlacedAppt> placed = new ArrayList<>();

        for (Appointment current : apptsForThatDay) {
            LocalDateTime curStart, curEnd;
            try {
                curStart = LocalDateTime.parse(current.getStartTs(), LDT_FMT);
                curEnd = curStart.plusMinutes(current.getDurationMinutes());
            } catch (DateTimeParseException e) {
                continue; // Hibás dátum, kihagyjuk
            }

            // Nézzük meg, ütközik-e már lerakottal
            PlacedAppt conflict = null;
            for (PlacedAppt p : placed) {
                try {
                    LocalDateTime pStart = LocalDateTime.parse(p.appt.getStartTs(), LDT_FMT);
                    LocalDateTime pEnd = pStart.plusMinutes(p.appt.getDurationMinutes());

                    // Átfedés ellenőrzés
                    boolean overlap = pStart.isBefore(curEnd) && pEnd.isAfter(curStart);
                    if (overlap) {
                        conflict = p;
                        break;
                    }
                } catch (DateTimeParseException e) {
                    // ignore
                }
            }

            PlacedAppt mine = new PlacedAppt();
            mine.appt = current;

            if (conflict == null) {
                // Nincs ütközés -> egyedül van
                mine.columnIndex = 0;
                mine.totalCols = 1;
            } else {
                // Van ütközés -> ketten egymás mellett
                // (Feltételezzük, hogy max 2-es ütközés lehet)
                mine.columnIndex = (conflict.columnIndex == 0) ? 1 : 0;
                conflict.totalCols = 2; // Frissítjük a másikat is
                mine.totalCols = 2;
            }
            placed.add(mine);
        }
        
        return placed;
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


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
import javafx.collections.FXCollections;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleController {

    // --- FXML ---
    @FXML
    private DatePicker dayPicker;
    @FXML
    private ToggleGroup viewToggle;
    @FXML
    private RadioButton dayRadio;
    @FXML
    private RadioButton weekRadio;

    @FXML
    private VBox timeGutter;
    @FXML
    private Pane canvas;

    // --- DAO-k és cache-elt listák a címkékhez ---
    private final AppointmentDao apptDao = new AppointmentDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();

    private List<Customer> customers = new ArrayList<>();
    private List<Vehicle> vehicles = new ArrayList<>();

    // --- rács konstansok ---
    private static final int DAY_START_HOUR = 8;
    private static final int DAY_END_HOUR = 18;
    private static final double HOUR_PIXELS = 60.0;
    private static final double COL_WIDTH = 150.0;

    private static final double DAY_HEADER_OFFSET = 24.0;

    @FXML
    public void initialize() {
        if (dayPicker.getValue() == null) {
            dayPicker.setValue(LocalDate.now());
        }
        viewToggle.selectedToggleProperty().addListener((obs, o, n) -> reload());
        dayPicker.valueProperty().addListener((obs, o, n) -> reload());
        reload();
    }

    public void hardRefreshSchedule() {
        // csak annyit csinál, hogy újratölt mindent az aktuális nap / heti nézet szerint
        reload();
    }

    @FXML
    public void onNew() {
        var start = dayPicker.getValue().atTime(10, 0);
        var a = new Appointment();
        a.setStartTs(start.toString());
        a.setDurationMinutes(60);
        a.setStatus("TERVEZETT");

        boolean saved = Forms.openAppointmentDialog(a, canvas.getScene().getWindow());
        if (saved) {
            reload(); // mentés már az űrlapban történt
        }
    }

    @FXML
    public void onDelete() {
        // itt hagyd meg a saját törlő logikádat
    }

    @FXML
    public void reload() {
        // frissítjük a címkézéshez használt listákat
        customers = customerDao.findAll();
        vehicles = vehicleDao.findAll();

        canvas.getChildren().clear();
        timeGutter.getChildren().clear();

        // bal oldali óracímkék
        for (int h = DAY_START_HOUR; h <= DAY_END_HOUR; h++) {
            var lbl = new Label(String.format("%02d:00", h));
            lbl.setMinHeight(HOUR_PIXELS);
            lbl.setPrefHeight(HOUR_PIXELS);
            lbl.setMaxHeight(HOUR_PIXELS);
            lbl.setPrefWidth(60);
            timeGutter.getChildren().add(lbl);
        }

        var selectedDay = dayPicker.getValue();
        if (dayRadio.isSelected()) {
            drawDay(selectedDay);
        } else {
            var monday = selectedDay.minusDays((selectedDay.getDayOfWeek().getValue() + 6) % 7);
            drawWeek(monday);
        }
    }

    // --------- NAPI NÉZET ---------
    private void drawDay(LocalDate day) {
        var from = day.atTime(0, 0);
        var to = day.plusDays(1).atTime(0, 0);
        var appts = apptDao.findBetween(from.toString(), to.toString());

        double height = DAY_HEADER_OFFSET + (DAY_END_HOUR - DAY_START_HOUR) * HOUR_PIXELS;
        canvas.setPrefHeight(height);

        canvas.setPrefWidth(1000);

        // --- cím felül: 2025.10.27. Hétfő ---
        var dayNameFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale("hu"));
        var dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd.", new java.util.Locale("hu"));

        String dayName = day.format(dayNameFormatter);        // "hétfő"
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1); // "Hétfő"

        String headerText = dateFormatter.format(day) + " " + dayName;

        var headerLabel = new Label(headerText);
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        headerLabel.setLayoutX(8);
        headerLabel.setLayoutY(4);
        canvas.getChildren().add(headerLabel);

        // --- RÁCSVONALAK ---
        addHourLines(canvas.getPrefWidth());

        // rendezzük és oszlopozzuk
        var placedList = layoutDayAppointments(appts);

        for (var placed : placedList) {
            var a = placed.appt;

            var start = LocalDateTime.parse(a.getStartTs());
            double minutesFromStart = java.time.Duration
                    .between(day.atTime(DAY_START_HOUR, 0), start).toMinutes();

            double y = DAY_HEADER_OFFSET
                    + Math.max(0, minutesFromStart * (HOUR_PIXELS / 60.0));

            double h = Math.max(22, a.getDurationMinutes() * (HOUR_PIXELS / 60.0));

            // szélesség felezés, ha kell
            double fullWidth = 980; // eddig fixen ezt adtad
            double w = fullWidth / placed.totalCols;
            double x = placed.columnIndex * w;

            var block = makeBlock(a, x, y, w - 4, h); // egy kis -4, hogy ne érjen teljesen össze
            canvas.getChildren().add(block);
        }
    }

    // --------- HETI NÉZET ---------
    private void drawWeek(LocalDate monday) {
        var from = monday.atStartOfDay();
        var to = monday.plusDays(7).atStartOfDay();
        var appts = apptDao.findBetween(from.toString(), to.toString());

        double height = (DAY_END_HOUR - DAY_START_HOUR) * HOUR_PIXELS;
        double totalWidth = COL_WIDTH * 7 + 10;
        canvas.setPrefHeight(height);
        canvas.setPrefWidth(totalWidth);

        // --- RÁCSVONALAK ---
        addHourLines(totalWidth);     // vízszintes vonalak (órák)
        addDaySeparators(totalWidth); // függőleges napelválasztók

        // oszlop-fejlécek (H, K, Sz...)
        for (int d = 0; d < 7; d++) {
            var formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale("hu"));
            var head = new Label(monday.plusDays(d).format(formatter));
            head.setStyle("-fx-font-weight: bold;");
            head.setLayoutX(d * COL_WIDTH + 6);
            head.setLayoutY(4);
            canvas.getChildren().add(head);
        }

        // 1) napokra bontjuk az appt-okat
        // pl. dayIndex -> List<Appointment>
        List<List<Appointment>> perDay = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            perDay.add(new ArrayList<>());
        }

        for (var a : appts) {
            var start = LocalDateTime.parse(a.getStartTs());
            int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(monday, start.toLocalDate());
            if (dayIndex < 0 || dayIndex > 6) {
                continue;
            }
            perDay.get(dayIndex).add(a);
        }

        // 2) minden naphoz lefuttatjuk a layoutot, és kirajzoljuk
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            var dayList = perDay.get(dayIndex);

            var placedList = layoutDayAppointments(dayList);

            for (var placed : placedList) {
                var a = placed.appt;
                var start = LocalDateTime.parse(a.getStartTs());

                double minutesFromStart = java.time.Duration
                        .between(start.toLocalDate().atTime(DAY_START_HOUR, 0), start)
                        .toMinutes();

                // napi oszlop bal oldala
                double baseX = dayIndex * COL_WIDTH + 4;
                double y = Math.max(0, minutesFromStart * (HOUR_PIXELS / 60.0));
                double h = Math.max(22, a.getDurationMinutes() * (HOUR_PIXELS / 60.0));

                // oszlopleosztás azon a napon belül
                double fullWidth = COL_WIDTH - 8;    // eddigi w
                double w = fullWidth / placed.totalCols;
                double x = baseX + placed.columnIndex * w;

                var block = makeBlock(a, x, y, w - 4, h);
                canvas.getChildren().add(block);
            }
        }
    }

    // --------- Blokk készítő + kattintás kezelő ---------
    private Node makeBlock(Appointment a, double x, double y, double w, double h) {
        var box = new VBox(2);
        box.setLayoutX(x);
        box.setLayoutY(y);
        box.setPrefSize(w, h);
        //box.setStyle("-fx-background-color: derive(-fx-accent, 40%); -fx-background-radius: 6; -fx-padding: 6;");
        // Alap stílus minden időpontdobozhoz
        box.getStyleClass().add("appt");

        // Státusz szerinti színosztály hozzáadása
        String status = a.getStatus() == null ? "" : a.getStatus().trim().toUpperCase();
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

        String cust = customers.stream()
                .filter(c -> c.getId() == a.getCustomerId())
                .findFirst().map(Customer::getName).orElse("");
        String veh = vehicles.stream()
                .filter(v -> v.getId() == a.getVehicleId())
                .findFirst().map(Vehicle::getPlate).orElse("");

        var l1 = new Label(cust);
        var l2 = new Label(veh);
        l1.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        l2.setStyle("-fx-font-size: 11;");
        box.getChildren().addAll(l1, l2);

        box.setOnMouseClicked(e -> {
            e.consume();
            if (Forms.openAppointmentDialog(a, canvas.getScene().getWindow())) {
                reload();
            }
        });

        return box;
    }

    // ---- GRID VONALAK (órák / napok) ----
    private void addHourLines(double width) {
        for (int h = DAY_START_HOUR; h <= DAY_END_HOUR; h++) {
            double y = DAY_HEADER_OFFSET + (h - DAY_START_HOUR) * HOUR_PIXELS;
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, y, width, y);
            styleGridLine(line, 1.0);
            line.setMouseTransparent(true);
            canvas.getChildren().add(line);
        }
    }

    private void addDaySeparators(double totalWidth) {
        // Heti nézetben: függőleges elválasztók minden naphoz
        for (int d = 0; d <= 7; d++) {
            double x = d * COL_WIDTH + 4;   // igazodik a drawWeek()-beli x-hez
            javafx.scene.shape.Line v = new javafx.scene.shape.Line(x, 0, x, canvas.getPrefHeight());
            styleGridLine(v, 1.0);
            v.setMouseTransparent(true);
            canvas.getChildren().add(v);
        }
    }

    private void styleGridLine(javafx.scene.shape.Line line, double strokeWidth) {
        line.setStrokeWidth(strokeWidth);
        line.setOpacity(0.25); // enyhe
        line.setStroke(javafx.scene.paint.Color.GRAY);
        // Ha szaggatottat szeretnél:
        // line.getStrokeDashArray().setAll(4.0, 4.0);
    }

    private static class PlacedAppt {

        Appointment appt;
        int columnIndex;   // 0 vagy 1
        int totalCols;     // 1 vagy 2 az adott "overlap-csoportban"
    }

    /**
     * Egy adott nap időpontjait (ugyanabban az oszlopban / napon) széthasítja
     * max 2 oszlopra.
     *
     * Feltételezzük, hogy nincs 3+ átfedés, mert azt már az üzleti logika
     * tiltja.
     */
    private List<PlacedAppt> layoutDayAppointments(List<Appointment> apptsForThatDay) {
        // Rendezzük kezdési idő szerint, hogy determinisztikus legyen
        apptsForThatDay.sort((a, b) -> {
            var sa = LocalDateTime.parse(a.getStartTs());
            var sb = LocalDateTime.parse(b.getStartTs());
            return sa.compareTo(sb);
        });

        List<PlacedAppt> placed = new ArrayList<>();

        for (Appointment current : apptsForThatDay) {
            LocalDateTime curStart = LocalDateTime.parse(current.getStartTs());
            LocalDateTime curEnd = curStart.plusMinutes(current.getDurationMinutes());

            // nézzük meg, ütközik-e már lerakottal
            PlacedAppt conflict = null;
            for (PlacedAppt p : placed) {
                LocalDateTime pStart = LocalDateTime.parse(p.appt.getStartTs());
                LocalDateTime pEnd = pStart.plusMinutes(p.appt.getDurationMinutes());

                boolean overlap = pStart.isBefore(curEnd) && pEnd.isAfter(curStart);
                if (overlap) {
                    conflict = p;
                    break;
                }
            }

            PlacedAppt mine = new PlacedAppt();
            mine.appt = current;

            if (conflict == null) {
                // nincs ütközés → egyedül van
                mine.columnIndex = 0;
                mine.totalCols = 1;
            } else {
                // van ütközés → ketten egymás mellett
                // Konfliktusban lévő biztosan columnIndex = 0 vagy 1
                // Akkor én kapom a másikat.
                if (conflict.columnIndex == 0) {
                    mine.columnIndex = 1;
                } else {
                    mine.columnIndex = 0;
                }
                // mindkettő totalCols = 2
                conflict.totalCols = 2;
                mine.totalCols = 2;
            }

            placed.add(mine);
        }

        // Még egy biztonsági kör: ha valaki totalCols=1 maradt, az rendben.
        return placed;
    }

}

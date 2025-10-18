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

    @FXML
    public void initialize() {
        if (dayPicker.getValue() == null) {
            dayPicker.setValue(LocalDate.now());
        }
        viewToggle.selectedToggleProperty().addListener((obs, o, n) -> reload());
        dayPicker.valueProperty().addListener((obs, o, n) -> reload());
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

        double height = (DAY_END_HOUR - DAY_START_HOUR) * HOUR_PIXELS;
        canvas.setPrefHeight(height);
        canvas.setPrefWidth(700);

        // --- RÁCSVONALAK ---
        addHourLines(canvas.getPrefWidth());

        for (var a : appts) {
            var start = LocalDateTime.parse(a.getStartTs());
            double minutesFromStart = java.time.Duration
                    .between(day.atTime(DAY_START_HOUR, 0), start).toMinutes();

            double y = Math.max(0, minutesFromStart * (HOUR_PIXELS / 60.0));
            double h = Math.max(22, a.getDurationMinutes() * (HOUR_PIXELS / 60.0));

            var block = makeBlock(a, 0, y, 680, h);
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

        // oszlop-fejlécek
        for (int d = 0; d < 7; d++) {
            var head = new Label(monday.plusDays(d).getDayOfWeek().toString().substring(0, 3));
            head.setStyle("-fx-font-weight: bold;");
            head.setLayoutX(d * COL_WIDTH + 6);
            head.setLayoutY(4);
            canvas.getChildren().add(head);
        }

        // időpont blokkok
        for (var a : appts) {
            var start = LocalDateTime.parse(a.getStartTs());
            int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(monday, start.toLocalDate());
            if (dayIndex < 0 || dayIndex > 6) {
                continue;
            }

            double minutesFromStart = java.time.Duration
                    .between(start.toLocalDate().atTime(DAY_START_HOUR, 0), start).toMinutes();

            double x = dayIndex * COL_WIDTH + 4;
            double y = Math.max(0, minutesFromStart * (HOUR_PIXELS / 60.0));
            double w = COL_WIDTH - 8;
            double h = Math.max(22, a.getDurationMinutes() * (HOUR_PIXELS / 60.0));

            var block = makeBlock(a, x, y, w, h);
            canvas.getChildren().add(block);
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
            double y = (h - DAY_START_HOUR) * HOUR_PIXELS;
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, y, width, y);
            styleGridLine(line, 1.0);        // vékony vonal
            line.setMouseTransparent(true);  // ne fogja a kattintást
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

}

package hu.carenda.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Az alkalmazás fő belépési pontja.
 * Felelős a JavaFX alkalmazás elindításáért és a bejelentkező (login) képernyő betöltéséért.
 */
public class MainApp extends Application {

    private static final String LOGIN_FXML_PATH = "/hu/carenda/app/views/login.fxml";
    private static final String APP_CSS_PATH = "/hu/carenda/app/css/app.css";
    private static final double LOGIN_WIDTH = 420;
    private static final double LOGIN_HEIGHT = 260;

    /**
     * A JavaFX alkalmazás fő belépési pontja.
     * Ezt a metódust hívja meg a JavaFX futtatókörnyezet.
     *
     * @param stage Az elsődleges "színpad" (ablak), amit az alkalmazás kap.
     */
    @Override
    public void start(Stage stage) {
        try {
            // 1. Erőforrás (FXML) betöltése
            URL fxmlUrl = getClass().getResource(LOGIN_FXML_PATH);
            Objects.requireNonNull(fxmlUrl, "Nem találom az erőforrást: " + LOGIN_FXML_PATH);

            Parent root = FXMLLoader.load(fxmlUrl);

            // 2. Scene (Jelenet) létrehozása
            Scene scene = new Scene(root, LOGIN_WIDTH, LOGIN_HEIGHT);

            // 3. Globális CSS stíluslap hozzáadása
            
            URL cssUrl = getClass().getResource(APP_CSS_PATH);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Figyelem: A globális CSS nem található: " + APP_CSS_PATH);
            }
            

            // 4. Stage (Ablak) beállítása
            stage.setTitle("Carenda – Bejelentkezés");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (IOException | NullPointerException | IllegalStateException e) {
            // 5. KRITIKUS HIBA KEZELÉSE
            // Ha a login.fxml nem töltődik be, az alkalmazás nem tud elindulni.
            // 'throws Exception' helyett egyértelmű hibaüzenetet mutatunk.
            e.printStackTrace();
            showFatalErrorDialog(
                    "Kritikus hiba",
                    "Az alkalmazás nem tudott elindulni.",
                    "Hiba az FXML fájl betöltése közben (" + LOGIN_FXML_PATH + ").\n" +
                            "Hiba részletei: " + e.getMessage()
            );
            // Mivel ez végzetes hiba, bezárjuk az appot
            System.exit(1);
        }
    }

    /**
     * Segédfüggvény egy végzetes hibaüzenet megjelenítésére.
     *
     * @param title   Az ablak címe.
     * @param header  A hiba fő üzenete.
     * @param content A hiba részletes leírása.
     */
    private void showFatalErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * A Java alkalmazás 'main' metódusa.
     * Ez indítja el a JavaFX Application.launch() metódusát.
     *
     * @param args Parancssori argumentumok (nincs használatban).
     */
    public static void main(String[] args) {
        launch(args);
    }
}

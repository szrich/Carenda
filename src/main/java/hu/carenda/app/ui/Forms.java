package hu.carenda.app.ui;

import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import hu.carenda.app.model.Appointment;
import hu.carenda.app.model.ServiceJobCard;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Utility osztály (Dialóguskezelő) a különböző FXML űrlapok
 * egységes, modális ablakként történő megnyitásához.
 */
public class Forms {

    // --- Publikus Metódusok (Ezeket hívják a Controllerek) ---

    /**
     * Megnyitja az Ügyfél űrlapot (létrehozás vagy szerkesztés).
     *
     * @param owner   A szülő ablak (Window), amihez a dialógust kötjük.
     * @param editing A szerkesztendő Customer objektum, vagy null (ha új ügyfelet hozunk létre).
     * @return true, ha a felhasználó mentette a változásokat, egyébként false.
     */
    public static boolean customer(Window owner, Customer editing) {
        // JAVÍTVA: A refaktorált Customer modell 'Integer getId()'-t használ
        String title = (editing == null || editing.getId() == null) ? "Új ügyfél" : "Ügyfél szerkesztése";
        
        // A 'controllerInitializer' egy lambda, ami betöltés után beállítja a controller állapotát.
        return openModalForm(
                owner,
                "/hu/carenda/app/views/customer-form.fxml",
                title,
                (CustomerFormController controller) -> {
                    controller.setEditing(editing);
                }
        );
    }

    /**
     * Megnyitja a Jármű űrlapot (létrehozás vagy szerkesztés).
     *
     * @param owner   A szülő ablak.
     * @param editing A szerkesztendő Vehicle objektum, vagy null (ha új járművet hozunk létre).
     * @return true, ha a felhasználó mentette a változásokat, egyébként false.
     */
    public static boolean vehicle(Window owner, Vehicle editing) {
        // JAVÍTVA: A refaktorált Vehicle modell 'Integer getId()'-t használ
        String title = (editing == null || editing.getId() == null) ? "Új jármű" : "Jármű szerkesztése";
        
        return openModalForm(
                owner,
                "/hu/carenda/app/views/vehicle-form.fxml",
                title,
                (VehicleFormController controller) -> {
                    controller.setEditing(editing);
                }
        );
    }

    /**
     * Megnyitja az Időpont űrlapot (létrehozás vagy szerkesztés).
     *
     * @param owner   A szülő ablak.
     * @param editing A szerkesztendő Appointment objektum, vagy null (ha új időpontot hozunk létre).
     * @return true, ha a felhasználó mentette a változásokat, egyébként false.
     */
    public static boolean openAppointmentDialog(Window owner, Appointment editing) {
        // JAVÍTVA: A refaktorált Appointment modell 'Integer getId()'-t használ
        String title = (editing == null || editing.getId() == null) ? "Új időpont" : "Időpont szerkesztése";
        
        return openModalForm(
                owner,
                "/hu/carenda/app/views/appointment-form.fxml",
                title,
                (AppointmentFormController controller) -> {
                    controller.setEditing(editing);
                }
        );
    }

    /**
     * Megnyitja a Munkalap űrlapot (létrehozás vagy szerkesztés).
     *
     * @param owner   A szülő ablak.
     * @param editing A szerkesztendő ServiceJobCard objektum, vagy null (ha új munkalapot hozunk létre).
     * @return true, ha a felhasználó mentette a változásokat, egyébként false.
     */
    public static boolean serviceJobCard(Window owner, ServiceJobCard editing) {
        // JAVÍTVA: A refaktorált ServiceJobCard modell 'Integer getId()'-t használ
        boolean isNew = (editing == null || editing.getId() == null);
        String title = isNew ? "Új munkalap" : "Munkalap szerkesztése";
        
        return openModalForm(
                owner,
                "/hu/carenda/app/views/serviceJobCard-form.fxml",
                title,
                (ServiceJobCardFormController controller) -> {
                    controller.setEditing(editing);
                }
        );
    }

    /**
     * A Munkalap űrlap egy speciális (túlterhelt) verziója,
     * ami előre kitölti az ügyfél és jármű adatokat.
     *
     * @param owner A szülő ablak.
     * @param card  A szerkesztendő kártya (általában null, ha újat hozunk létre kontextusból).
     * @param cust  Az előre kiválasztott Ügyfél.
     * @param veh   Az előre kiválasztott Jármű.
     * @return true, ha a felhasználó mentette a változásokat, egyébként false.
     */
    public static boolean serviceJobCard(Window owner, ServiceJobCard card, Customer cust, Vehicle veh) {
        // JAVÍTVA: A refaktorált ServiceJobCard modell 'Integer getId()'-t használ
        boolean isNew = (card == null || card.getId() == null);
        String title = isNew ? "Új munkalap" : "Munkalap szerkesztése";

        return openModalForm(
                owner,
                "/hu/carenda/app/views/serviceJobCard-form.fxml",
                title,
                (ServiceJobCardFormController controller) -> {
                    // A controller speciális, bővített inicializálóját hívjuk
                    controller.setContext(card, cust, veh);
                }
        );
    }


    // --- Privát Segédfüggvények ---
    
    /**
     * Egy központi, generikus metódus FXML űrlapok modális ablakként való megnyitásához.
     * Kezeli az FXML betöltését, a Controller inicializálását, az ablak beállítását,
     * a modális megjelenítést (showAndWait) és a visszatérési érték (UserData) kezelését.
     *
     * @param owner               A szülő ablak.
     * @param fxmlPath            Az FXML fájl útvonala (pl. "/hu/../view.fxml").
     * @param title               Az ablak címe.
     * @param controllerInitializer Egy 'Consumer' (lambda), ami megkapja a betöltött Controller
     * példányt, hogy beállíthassa (pl. 'ctrl.setEditing(...)').
     * @param <T>                 A Controller osztály típusa (automatikusan kikövetkezteti).
     * @return true, ha a dialógus 'userData'-ja 'Boolean.TRUE'-ra lett állítva (jellemzően Mentéskor),
     * egyébként false (Mégse vagy bezárás).
     */
    private static <T> boolean openModalForm(Window owner, String fxmlPath, String title, Consumer<T> controllerInitializer) {
        try {
            // 1. FXML Betöltése
            URL fxmlUrl = Forms.class.getResource(fxmlPath);
            Objects.requireNonNull(fxmlUrl, "Nem találom az FXML erőforrást: " + fxmlPath);
            
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            Parent root = fxmlLoader.load();

            // 2. Controller inicializálása (a kapott lambda lefuttatása)
            T controller = fxmlLoader.getController();
            controllerInitializer.accept(controller);

            // 3. Stage (Ablak) létrehozása és beállítása
            Stage stage = new Stage();
            stage.setTitle(title);
           // 3a. Scene létrehozása VÁLTOZÓBA
            Scene scene = new Scene(root);
            
            // 3b. Globális CSS stíluslap hozzáadása a Scene-hez
            try {
                // Ellenőrizd az útvonalat!
                String cssPath = Forms.class.getResource("/hu/carenda/app/css/app.css").toExternalForm();
                scene.getStylesheets().add(cssPath);
            } catch (Exception e) {
                System.err.println("Figyelem: Nem sikerült betölteni a CSS stíluslapot!");
                e.printStackTrace(); // Kiírja a hibát, de az app nem áll le
            }

            // 3c.Scene beállítása az ablakhoz
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL); // Modális (blokkolja a szülőt)
            stage.initOwner(owner);                    // Beállítjuk a szülő ablakot

            // 4. Megjelenítés és várakozás az eredményre
            stage.showAndWait();

            // 5. Eredmény visszaadása
            // A Controllereknek kell a 'stage.setUserData(Boolean.TRUE)'-t hívni mentéskor.
            return Boolean.TRUE.equals(stage.getUserData());

        } catch (IOException | NullPointerException | IllegalStateException e) {
            // Hiba esetén (pl. FXML nem található, FXML hiba)
            // egy felugró ablakot mutatunk a felhasználónak.
            e.printStackTrace(); // Hiba a konzolra
            showErrorDialog(
                    "Hiba az űrlap betöltésekor",
                    "Nem sikerült megnyitni az ablakot: " + title,
                    "Hiba részletei: " + e.getMessage()
            );
            return false; // Hiba esetén 'false'-t adunk vissza
        }
    }

    /**
     * Segédfüggvény egy hibaüzenet (Alert) megjelenítésére.
     *
     * @param title   Az ablak címe.
     * @param header  A hiba fő üzenete.
     * @param content A hiba részletes leírása.
     */
    private static void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}


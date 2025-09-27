package hu.carenda.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
            Objects.requireNonNull(
                getClass().getResource("/hu/carenda/app/views/login.fxml"),
                "Nem találom a /hu/carenda/app/views/login.fxml erőforrást. Ellenőrizd az útvonalat és a fájlnevet!")
        );
        stage.setTitle("Carenda – Bejelentkezés");
        stage.setScene(new Scene(root, 1420, 1260));
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}

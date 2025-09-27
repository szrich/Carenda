package hu.carenda.app.ui;

import hu.carenda.app.model.Customer;
import hu.carenda.app.model.Vehicle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Forms {

    public static Stage customer(Customer editing) {
        try {
            FXMLLoader f = new FXMLLoader(Forms.class.getResource("/hu/carenda/app/views/customer-form.fxml"));
            Parent root = (Parent) f.load(); // <-- FONTOS: cast Parent-re
            CustomerFormController ctrl = f.getController();
            ctrl.setEditing(editing);

            Stage dlg = new Stage();
            dlg.setTitle(editing == null ? "Új ügyfél" : "Ügyfél szerkesztése");
            dlg.setScene(new Scene(root));
            return dlg;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Stage vehicle(Vehicle editing) {
        try {
            FXMLLoader f = new FXMLLoader(Forms.class.getResource("/hu/carenda/app/views/vehicle-form.fxml"));
            Parent root = (Parent) f.load(); // <-- FONTOS: cast Parent-re
            VehicleFormController ctrl = f.getController();
            ctrl.setEditing(editing);

            Stage dlg = new Stage();
            dlg.setTitle(editing == null ? "Új jármű" : "Jármű szerkesztése");
            dlg.setScene(new Scene(root));
            return dlg;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static javafx.stage.Stage appointment(hu.carenda.app.model.Appointment editing) {
        try {
            var f = new javafx.fxml.FXMLLoader(Forms.class.getResource("/hu/carenda/app/views/appointment-form.fxml"));
            var root = (javafx.scene.Parent) f.load(); // CAST!
            var ctrl = (hu.carenda.app.ui.AppointmentFormController) f.getController();
            ctrl.setEditing(editing);

            var dlg = new javafx.stage.Stage();
            dlg.setTitle(editing == null ? "Új időpont" : "Időpont szerkesztése");
            dlg.setScene(new javafx.scene.Scene(root));
            return dlg;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

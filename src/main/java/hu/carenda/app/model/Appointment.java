package hu.carenda.app.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Appointment {

    private final ObjectProperty<Integer> id = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> customerId = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> vehicleId = new SimpleObjectProperty<>(null);
    private final StringProperty ownerName = new SimpleStringProperty();
    private final StringProperty vehiclePlate = new SimpleStringProperty();
    private final StringProperty startTs = new SimpleStringProperty();
    private final ObjectProperty<Integer> durationMinutes = new SimpleObjectProperty<>(null);
    private final StringProperty note = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    // --- Getterek, Setterek, Property-k ---

    public Integer getId() { return id.get(); }
    public void setId(Integer v) { id.set(v); }
    public ObjectProperty<Integer> idProperty() { return id; }

    public Integer getCustomerId() { return customerId.get(); }
    public void setCustomerId(Integer v) { customerId.set(v); }
    public ObjectProperty<Integer> customerIdProperty() { return customerId; }

    public Integer getVehicleId() { return vehicleId.get(); }
    public void setVehicleId(Integer v) { vehicleId.set(v); }
    public ObjectProperty<Integer> vehicleIdProperty() { return vehicleId; }

    public String getOwnerName() { return ownerName.get(); }
    public void setOwnerName(String v) { ownerName.set(v); }
    public StringProperty ownerNameProperty() { return ownerName; }

    public String getVehiclePlate() { return vehiclePlate.get(); }
    public void setVehiclePlate(String v) { vehiclePlate.set(v); }
    public StringProperty vehiclePlateProperty() { return vehiclePlate; }

    public String getStartTs() { return startTs.get(); }
    public void setStartTs(String v) { startTs.set(v); }
    public StringProperty startTsProperty() { return startTs; }

    public Integer getDurationMinutes() { return durationMinutes.get(); }
    public void setDurationMinutes(Integer v) { durationMinutes.set(v); }
    public ObjectProperty<Integer> durationMinutesProperty() { return durationMinutes; }

    public String getNote() { return note.get(); }
    public void setNote(String v) { note.set(v); }
    public StringProperty noteProperty() { return note; }

    public String getStatus() { return status.get(); }
    public void setStatus(String v) { status.set(v); }
    public StringProperty statusProperty() { return status; }
}

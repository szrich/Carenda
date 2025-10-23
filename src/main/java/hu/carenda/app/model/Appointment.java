package hu.carenda.app.model;

import javafx.beans.property.*;

public class Appointment {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty customerId = new SimpleIntegerProperty();
    private final IntegerProperty vehicleId = new SimpleIntegerProperty();
    private final StringProperty ownerName = new SimpleStringProperty();
    private final StringProperty vehiclePlate = new SimpleStringProperty();
    private final StringProperty startTs = new SimpleStringProperty();
    private final IntegerProperty durationMinutes = new SimpleIntegerProperty();
    private final StringProperty note = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public int getCustomerId() { return customerId.get(); }
    public void setCustomerId(int v) { customerId.set(v); }
    public IntegerProperty customerIdProperty() { return customerId; }

    public int getVehicleId() { return vehicleId.get(); }
    public void setVehicleId(int v) { vehicleId.set(v); }
    public IntegerProperty vehicleIdProperty() { return vehicleId; }

    public String getOwnerName() { return ownerName.get(); }
    public void setOwnerName(String v) { ownerName.set(v); }
    public StringProperty ownerNameProperty() { return ownerName; }

    public String getVehiclePlate() { return vehiclePlate.get(); }
    public void setVehiclePlate(String v) { vehiclePlate.set(v); }
    public StringProperty vehiclePlateProperty() { return vehiclePlate; }

    public String getStartTs() { return startTs.get(); }
    public void setStartTs(String v) { startTs.set(v); }
    public StringProperty startTsProperty() { return startTs; }

    public int getDurationMinutes() { return durationMinutes.get(); }
    public void setDurationMinutes(int v) { durationMinutes.set(v); }
    public IntegerProperty durationMinutesProperty() { return durationMinutes; }

    public String getNote() { return note.get(); }
    public void setNote(String v) { note.set(v); }
    public StringProperty noteProperty() { return note; }

    public String getStatus() { return status.get(); }
    public void setStatus(String v) { status.set(v); }
    public StringProperty statusProperty() { return status; }
}


package hu.carenda.app.model;

import javafx.beans.property.*;

public class Appointment {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty customerId = new SimpleIntegerProperty();
    private final IntegerProperty vehicleId = new SimpleIntegerProperty();
    private final StringProperty customerName = new SimpleStringProperty();
    private final StringProperty vehiclePlate = new SimpleStringProperty();
    private final StringProperty startTs = new SimpleStringProperty();
    private final IntegerProperty durationMinutes = new SimpleIntegerProperty();
    private final StringProperty note = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int v) { id.set(v); }

    public int getCustomerId() { return customerId.get(); }
    public IntegerProperty customerIdProperty() { return customerId; }
    public void setCustomerId(int v) { customerId.set(v); }

    public int getVehicleId() { return vehicleId.get(); }
    public IntegerProperty vehicleIdProperty() { return vehicleId; }
    public void setVehicleId(int v) { vehicleId.set(v); }

    public String getCustomerName() { return customerName.get(); }
    public StringProperty customerNameProperty() { return customerName; }
    public void setCustomerName(String v) { customerName.set(v); }

    public String getVehiclePlate() { return vehiclePlate.get(); }
    public StringProperty vehiclePlateProperty() { return vehiclePlate; }
    public void setVehiclePlate(String v) { vehiclePlate.set(v); }

    public String getStartTs() { return startTs.get(); }
    public StringProperty startTsProperty() { return startTs; }
    public void setStartTs(String v) { startTs.set(v); }

    public int getDurationMinutes() { return durationMinutes.get(); }
    public IntegerProperty durationMinutesProperty() { return durationMinutes; }
    public void setDurationMinutes(int v) { durationMinutes.set(v); }

    public String getNote() { return note.get(); }
    public StringProperty noteProperty() { return note; }
    public void setNote(String v) { note.set(v); }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String v) { status.set(v); }
}


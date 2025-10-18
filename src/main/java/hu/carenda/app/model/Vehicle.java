package hu.carenda.app.model;

import javafx.beans.property.*;

public class Vehicle {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty plate = new SimpleStringProperty();
    private final StringProperty makeModel = new SimpleStringProperty();
    private final IntegerProperty ownerId = new SimpleIntegerProperty();
    private final StringProperty ownerName = new SimpleStringProperty();

    public Vehicle() {}
    public Vehicle(int id, String plate, String makeModel, int ownerId, String ownerName) {
        setId(id); setPlate(plate); setMakeModel(makeModel); setOwnerId(ownerId); setOwnerName(ownerName);
    }

    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public String getPlate() { return plate.get(); }
    public void setPlate(String v) { plate.set(v); }
    public StringProperty plateProperty() { return plate; }

    public String getMakeModel() { return makeModel.get(); }
    public void setMakeModel(String v) { makeModel.set(v); }
    public StringProperty makeModelProperty() { return makeModel; }

    public int getOwnerId() { return ownerId.get(); }
    public void setOwnerId(int v) { ownerId.set(v); }
    public IntegerProperty ownerIdProperty() { return ownerId; }

    public String getOwnerName() { return ownerName.get(); }
    public void setOwnerName(String v) { ownerName.set(v); }
    public StringProperty ownerNameProperty() { return ownerName; }
    
    @Override
    public String toString() {
        return getPlate() != null ? getPlate() : "";
    }
}

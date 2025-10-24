package hu.carenda.app.model;

import javafx.beans.property.*;

public class Vehicle {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty plate = new SimpleStringProperty();
    private final StringProperty vin = new SimpleStringProperty();
    private final StringProperty engine_no = new SimpleStringProperty();
    private final StringProperty brand = new SimpleStringProperty();
    private final StringProperty model = new SimpleStringProperty();
    private final IntegerProperty year = new SimpleIntegerProperty();
    private final StringProperty fuel_type = new SimpleStringProperty();
    private final IntegerProperty ownerId = new SimpleIntegerProperty();
    private final StringProperty ownerName = new SimpleStringProperty();

    public Vehicle() {}
    public Vehicle(int id, String plate, String vin, String engine_no, String brand, String model, Integer year, String fuel_type, int ownerId, String ownerName) {
        setId(id); setPlate(plate); setVin(vin); setEngine_no(engine_no); setBrand(brand); setModel(model); setYear(year); setFuel_type(fuel_type);  setOwnerId(ownerId); setOwnerName(ownerName);
    }

    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public String getPlate() { return plate.get(); }
    public void setPlate(String v) { plate.set(v); }
    public StringProperty plateProperty() { return plate; }
    
    public String getVin() { return vin.get(); }
    public void setVin(String v) { vin.set(v); }
    public StringProperty vinProperty() { return vin; }
    
    public String getEngine_no() { return engine_no.get(); }
    public void setEngine_no(String v) { engine_no.set(v); }
    public StringProperty engine_noProperty() { return engine_no; }

    public String getBrand() { return brand.get(); }
    public void setBrand(String v) { brand.set(v); }
    public StringProperty brandProperty() { return brand; }
    
    public String getModel() { return model.get(); }
    public void setModel(String v) { model.set(v); }
    public StringProperty modelProperty() { return model; }
    
    public Integer getYear() { return year.get(); }
    public void setYear(Integer v) { year.set(v); }
    public IntegerProperty yearProperty() { return year; }
    
    public String getFuel_type() { return fuel_type.get(); }
    public void setFuel_type(String v) { fuel_type.set(v); }
    public StringProperty fuel_typeProperty() { return fuel_type; }
   
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

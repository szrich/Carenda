package hu.carenda.app.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Vehicle {
    
    private final ObjectProperty<Integer> id = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> ownerId = new SimpleObjectProperty<>(null);
    private final StringProperty plate = new SimpleStringProperty();
    private final StringProperty vin = new SimpleStringProperty();
    private final StringProperty engine_no = new SimpleStringProperty();
    private final StringProperty brand = new SimpleStringProperty();
    private final StringProperty model = new SimpleStringProperty();
    private final StringProperty fuel_type = new SimpleStringProperty();
    private final StringProperty ownerName = new SimpleStringProperty();
    private final ObjectProperty<Integer> year = new SimpleObjectProperty<>(null);

    public Vehicle() {}
    
    public Vehicle(Integer id, String plate, String vin, String engine_no, String brand, String model, 
                   Integer year, String fuel_type, Integer ownerId, String ownerName) {
        setId(id); 
        setPlate(plate); 
        setVin(vin); 
        setEngine_no(engine_no); 
        setBrand(brand); 
        setModel(model); 
        setYear(year);
        setFuel_type(fuel_type);  
        setOwnerId(ownerId); 
        setOwnerName(ownerName);
    }

    // --- id ---
    public Integer getId() { return id.get(); }
    public void setId(Integer v) { id.set(v); }
    public ObjectProperty<Integer> idProperty() { return id; }

    // --- plate ---
    public String getPlate() { return plate.get(); }
    public void setPlate(String v) { plate.set(v); }
    public StringProperty plateProperty() { return plate; }
    
    // --- vin ---
    public String getVin() { return vin.get(); }
    public void setVin(String v) { vin.set(v); }
    public StringProperty vinProperty() { return vin; }
    
    // --- engine_no ---
    public String getEngine_no() { return engine_no.get(); }
    public void setEngine_no(String v) { engine_no.set(v); }
    public StringProperty engine_noProperty() { return engine_no; }

    // --- brand ---
    public String getBrand() { return brand.get(); }
    public void setBrand(String v) { brand.set(v); }
    public StringProperty brandProperty() { return brand; }
    
    // --- model ---
    public String getModel() { return model.get(); }
    public void setModel(String v) { model.set(v); }
    public StringProperty modelProperty() { return model; }
    
    // --- year ---
    public Integer getYear() { return year.get(); }
    public void setYear(Integer v) { year.set(v); }
    public ObjectProperty<Integer> yearProperty() { return year; }
    
    // --- fuel_type ---
    public String getFuel_type() { return fuel_type.get(); }
    public void setFuel_type(String v) { fuel_type.set(v); }
    public StringProperty fuel_typeProperty() { return fuel_type; }
   
    // --- ownerId ---
    public Integer getOwnerId() { return ownerId.get(); }
    public void setOwnerId(Integer v) { ownerId.set(v); }
    public ObjectProperty<Integer> ownerIdProperty() { return ownerId; }

    // --- ownerName ---
    public String getOwnerName() { return ownerName.get(); }
    public void setOwnerName(String v) { ownerName.set(v); }
    public StringProperty ownerNameProperty() { return ownerName; }
    
    @Override
    public String toString() {
        return getPlate() != null ? getPlate() : "";
    }
}


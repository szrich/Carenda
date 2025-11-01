package hu.carenda.app.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Customer {
    private final ObjectProperty<Integer> id = new SimpleObjectProperty<>(null); 
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();

    public Customer() {}
    
    public Customer(Integer id, String name, String phone, String email) {
        setId(id); 
        setName(name); 
        setPhone(phone); 
        setEmail(email);
    }

    // --- Getterek, Setterek, Property-k ---

    public Integer getId() { return id.get(); }
    public void setId(Integer v) { id.set(v); }
    public ObjectProperty<Integer> idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public StringProperty nameProperty() { return name; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(v); }
    public StringProperty phoneProperty() { return phone; }

    public String getEmail() { return email.get(); }
    public void setEmail(String v) { email.set(v); }
    public StringProperty emailProperty() { return email; }
    
    @Override
    public String toString() {
        return getName() != null ? getName() : "";
    }
}

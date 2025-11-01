package hu.carenda.app.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Munkalap (Service Job Card) modell
 */
public class ServiceJobCard {

    private final ObjectProperty<Integer> id = new SimpleObjectProperty<>(null);
    private final StringProperty jobcard_no = new SimpleStringProperty();
    private final ObjectProperty<Integer> appointment_id = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> vehicle_id = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> customer_id = new SimpleObjectProperty<>(null);
    private final StringProperty fault_desc = new SimpleStringProperty();
    private final StringProperty repair_note = new SimpleStringProperty();
    private final StringProperty diagnosis = new SimpleStringProperty();
    private final StringProperty internal_note = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final ObjectProperty<Integer> assignee_user_id = new SimpleObjectProperty<>(null);
    private final StringProperty created_at = new SimpleStringProperty();
    private final StringProperty updated_at = new SimpleStringProperty();
    private final StringProperty finished_at = new SimpleStringProperty();
    private final ObjectProperty<Integer> odometer_km = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> fuel_level_eighths = new SimpleObjectProperty<>(null);
    private final StringProperty currency_code = new SimpleStringProperty();
    private final ObjectProperty<Integer> advance_cents = new SimpleObjectProperty<>(null);

    // JOIN-olt mezők 
    private final StringProperty ownerName = new SimpleStringProperty();
    private final StringProperty plate = new SimpleStringProperty();
    private final StringProperty brand = new SimpleStringProperty();
    private final StringProperty model = new SimpleStringProperty();
    
    public ServiceJobCard() {}

    public ServiceJobCard(Integer id, String jobcard_no, Integer appointment_id, Integer vehicle_id,
                          Integer customer_id, String fault_desc, String repair_note, String diagnosis,
                          String internal_note, String status, Integer assignee_user_id, String created_at,
                          String updated_at, String finished_at, Integer odometer_km,
                          Integer fuel_level_eighths, String currency_code, Integer advance_cents) {

        setId(id);
        setJobcard_no(jobcard_no);
        setAppointment_id(appointment_id);
        setVehicle_id(vehicle_id);
        setCustomer_id(customer_id);
        setFault_desc(fault_desc);
        setRepair_note(repair_note);
        setDiagnosis(diagnosis);
        setInternal_note(internal_note);
        setStatus(status);
        setAssignee_user_id(assignee_user_id);
        setCreated_at(created_at);
        setUpdated_at(updated_at);
        setFinished_at(finished_at);
        setOdometer_km(odometer_km);
        setFuel_level_eighths(fuel_level_eighths);
        setCurrency_code(currency_code);
        setAdvance_cents(advance_cents);
    }

    // --- Getterek, Setterek, Property-k ---

    // ---- ID ----
    public Integer getId() { return id.get(); }
    public void setId(Integer v) { id.set(v); }
    public ObjectProperty<Integer> idProperty() { return id; }

    // ---- JOB CARD NO ----
    public String getJobcard_no() { return jobcard_no.get(); }
    public void setJobcard_no(String v) { jobcard_no.set(v); }
    public StringProperty jobcard_noProperty() { return jobcard_no; }

    // ---- APPOINTMENT ----
    public Integer getAppointment_id() { return appointment_id.get(); }
    public void setAppointment_id(Integer v) { appointment_id.set(v); }
    public ObjectProperty<Integer> appointment_idProperty() { return appointment_id; }

    // ---- VEHICLE ----
    public Integer getVehicle_id() { return vehicle_id.get(); }
    public void setVehicle_id(Integer v) { vehicle_id.set(v); } 
    public ObjectProperty<Integer> vehicle_idProperty() { return vehicle_id; }

    // ---- CUSTOMER ----
    public Integer getCustomer_id() { return customer_id.get(); }
    public void setCustomer_id(Integer v) { customer_id.set(v); }
    public ObjectProperty<Integer> customer_idProperty() { return customer_id; }

    // ---- TEXT FIELDS ----
    public String getFault_desc() { return fault_desc.get(); }
    public void setFault_desc(String v) { fault_desc.set(v); }
    public StringProperty fault_descProperty() { return fault_desc; }

    public String getRepair_note() { return repair_note.get(); }
    public void setRepair_note(String v) { repair_note.set(v); }
    public StringProperty repair_noteProperty() { return repair_note; }

    public String getDiagnosis() { return diagnosis.get(); }
    public void setDiagnosis(String v) { diagnosis.set(v); }
    public StringProperty diagnosisProperty() { return diagnosis; }

    public String getInternal_note() { return internal_note.get(); }
    public void setInternal_note(String v) { internal_note.set(v); }
    public StringProperty internal_noteProperty() { return internal_note; }

    // ---- STATUS ----
    public String getStatus() { return status.get(); }
    public void setStatus(String v) { status.set(v); }
    public StringProperty statusProperty() { return status; }

    // ---- ASSIGNEE ----
    public Integer getAssignee_user_id() { return assignee_user_id.get(); }
    public void setAssignee_user_id(Integer v) { assignee_user_id.set(v); }
    public ObjectProperty<Integer> assignee_user_idProperty() { return assignee_user_id; }

    // ---- DATES ----
    public String getCreated_at() { return created_at.get(); }
    public void setCreated_at(String v) { created_at.set(v); }
    public StringProperty created_atProperty() { return created_at; }

    public String getUpdated_at() { return updated_at.get(); }
    public void setUpdated_at(String v) { updated_at.set(v); }
    public StringProperty updated_atProperty() { return updated_at; }

    public String getFinished_at() { return finished_at.get(); }
    public void setFinished_at(String v) { finished_at.set(v); }
    public StringProperty finished_atProperty() { return finished_at; }

    // ---- ODOMETER ----
    public Integer getOdometer_km() { return odometer_km.get(); }
    public void setOdometer_km(Integer v) { odometer_km.set(v); }
    public ObjectProperty<Integer> odometer_kmProperty() { return odometer_km; }

    // ---- FUEL LEVEL ----
    public Integer getFuel_level_eighths() { return fuel_level_eighths.get(); }
    public void setFuel_level_eighths(Integer v) { fuel_level_eighths.set(v); }
    public ObjectProperty<Integer> fuel_level_eighthsProperty() { return fuel_level_eighths; }

    // ---- CURRENCY ----
    public String getCurrency_code() { return currency_code.get(); }
    public void setCurrency_code(String v) { currency_code.set(v); }
    public StringProperty currency_codeProperty() { return currency_code; }

    // ---- ADVANCE ----
    public Integer getAdvance_cents() { return advance_cents.get(); }
    public void setAdvance_cents(Integer v) { advance_cents.set(v); }
    public ObjectProperty<Integer> advance_centsProperty() { return advance_cents; }
    
    // ---- JOIN-olt mezők ----
    public String getOwnerName() { return ownerName.get(); }
    public void setOwnerName(String v) { ownerName.set(v); }
    public StringProperty ownerNameProperty() { return ownerName; }
    
    public String getPlate() { return plate.get(); }
    public void setPlate(String v) { plate.set(v); }
    public StringProperty plateProperty() { return plate; }
    
    public String getBrand() { return brand.get(); }
    public void setBrand(String v) { brand.set(v); }
    public StringProperty brandProperty() { return brand; }
    
    public String getModel() { return model.get(); }
    public void setModel(String v) { model.set(v); }
    public StringProperty modelProperty() { return model; }
}

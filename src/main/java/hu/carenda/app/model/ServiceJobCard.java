package hu.carenda.app.model;

import javafx.beans.property.*;

/**
 *
 * @author szric
 */
public class ServiceJobCard {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty jobcard_no = new SimpleStringProperty();
    private final IntegerProperty appointment_id = new SimpleIntegerProperty();
    private final IntegerProperty vehicle_id = new SimpleIntegerProperty();
    private final IntegerProperty customer_id = new SimpleIntegerProperty();
    private final StringProperty fault_desc = new SimpleStringProperty();
    private final StringProperty repair_note = new SimpleStringProperty();
    private final StringProperty work_done = new SimpleStringProperty();
    private final StringProperty parts_used = new SimpleStringProperty();
    private final StringProperty created_at = new SimpleStringProperty();
    private final StringProperty updated_at = new SimpleStringProperty();
    
    public ServiceJobCard() {}
    public ServiceJobCard(int id, String jobcard_no, int appointment_id, int vehicle_id, int customer_id, String fault_desc, String repair_note, String work_done, String parts_used, String created_at, String updated_at) {
        setId(id); setJobcard_no(jobcard_no); setAppointment_id(appointment_id); setVehicle_id(vehicle_id); setCustomer_id(customer_id); setFault_desc(fault_desc); setRepair_note(repair_note); setWork_done(work_done); setParts_used(parts_used); setCreated_at(created_at); setUpdated_at(updated_at);
    }
    
    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public String getJobcard_no() { return jobcard_no.get(); }
    public void setJobcard_no(String v) { jobcard_no.set(v); }
    public StringProperty jobcard_noProperty() { return jobcard_no; }

    public int getAppointment_id() { return appointment_id.get(); }
    public void setAppointment_id(int v) { appointment_id.set(v); }
    public IntegerProperty appointment_idProperty() { return appointment_id; }
    
    public int getVehicle_id() { return vehicle_id.get(); }
    public void setVehicle_id(int v) { vehicle_id.set(v); }
    public IntegerProperty vehicle_idProperty() { return vehicle_id; }
    
    public int getCustomer_id() { return customer_id.get(); }
    public void setCustomer_id(int v) { customer_id.set(v); }
    public IntegerProperty customer_idProperty() { return customer_id; }
    
    public String getFault_desc() { return fault_desc.get(); }
    public void setFault_desc(String v) { fault_desc.set(v); }
    public StringProperty fault_descProperty() { return fault_desc; }
    
    public String getRepair_note() { return repair_note.get(); }
    public void setRepair_note(String v) { repair_note.set(v); }
    public StringProperty repair_noteProperty() { return repair_note; }
    
    public String getWork_done() { return work_done.get(); }
    public void setWork_done(String v) { work_done.set(v); }
    public StringProperty work_doneProperty() { return work_done; }
    
    public String getParts_used() { return parts_used.get(); }
    public void setParts_used(String v) { parts_used.set(v); }
    public StringProperty parts_usedProperty() { return parts_used; }
    
    public String getCreated_at() { return created_at.get(); }
    public void setCreated_at(String v) { created_at.set(v); }
    public StringProperty created_atProperty() { return created_at; }
    
    public String getUpdated_at() { return updated_at.get(); }
    public void setUpdated_at(String v) { updated_at.set(v); }
    public StringProperty updated_atProperty() { return updated_at; }
}

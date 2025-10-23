package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.ServiceJobCard;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class ServiceJobCardDao {
    private ServiceJobCard map(ResultSet rs) throws Exception {
        ServiceJobCard s = new ServiceJobCard();
        s.setId(rs.getInt("id"));
        s.setJobcard_no(rs.getString("jobcard_no"));
        s.setAppointment_id(rs.getInt("appointment_id"));
        s.setVehicle_id(rs.getInt("vehicle_id"));
        s.setCustomer_id(rs.getInt("customer_id"));
        s.setFault_desc(rs.getString("fault_desc"));
        s.setRepair_note(rs.getString("repair_note"));
        s.setWork_done(rs.getString("work_done")); 
        s.setParts_used(rs.getString("parts_used"));
        s.setCreated_at(rs.getString("created_at"));
        s.setUpdated_at(rs.getString("updated_at"));
        return s;
    }
    
    public List<ServiceJobCard> findAll() {
        String sql = "SELECT id, jobcard_no, appointment_id, vehicle_id, customer_id, fault_desc, repair_note, work_done, parts_used, created_at, updated_at FROM servicejobcard ORDER BY id";
        try (var c = Database.get(); var st = c.createStatement(); var rs = st.executeQuery(sql)) {
            List<ServiceJobCard> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}

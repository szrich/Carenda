package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Appointment;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDao {

    private Appointment map(ResultSet rs) throws Exception {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setCustomerId(rs.getInt("customer_id"));
        a.setVehicleId(rs.getInt("vehicle_id"));
        a.setStartTs(rs.getString("start_ts"));
        a.setDurationMinutes(rs.getInt("duration"));
        a.setNote(rs.getString("note"));
        a.setStatus(rs.getString("status"));
        return a;
    }

    public List<Appointment> findAll() {
        String sql = """
            SELECT id, customer_id, vehicle_id, start_ts, duration, note, status
              FROM appointments
             ORDER BY start_ts DESC, id DESC
            """;
        try (var c = Database.get(); var st = c.createStatement(); var rs = st.executeQuery(sql)) {
            List<Appointment> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Keresés ügyfél név / rendszám / státusz / megjegyzés alapján.
     */
    public List<Appointment> search(String q) {
        String like = "%" + q.trim().toLowerCase() + "%";
        String sql = """
            SELECT a.id, a.customer_id, a.vehicle_id, a.start_ts, a.duration, a.note, a.status
              FROM appointments a
              LEFT JOIN customers c ON c.id = a.customer_id
              LEFT JOIN vehicles  v ON v.id = a.vehicle_id
             WHERE lower(c.name)    LIKE ?
                OR lower(v.plate)   LIKE ?
                OR lower(a.status)  LIKE ?
                OR lower(a.note)    LIKE ?
             ORDER BY a.start_ts DESC, a.id DESC
            """;
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (var rs = ps.executeQuery()) {
                List<Appointment> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Appointment findById(int id) {
        String sql = """
            SELECT id, customer_id, vehicle_id, start_ts, duration, note, status
              FROM appointments WHERE id=?
            """;
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int insert(int customerId, int vehicleId, String startTs,
            int durationMinutes, String note, String status) {
        String sql = """
            INSERT INTO appointments(customer_id, vehicle_id, start_ts, duration, note, status)
            VALUES (?,?,?,?,?,?)
            """;
        try (var c = Database.get(); var ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, vehicleId);
            ps.setString(3, startTs);
            ps.setInt(4, durationMinutes);
            ps.setString(5, note);
            ps.setString(6, status);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void update(int id, int customerId, int vehicleId, String startTs,
            int durationMinutes, String note, String status) {
        String sql = """
            UPDATE appointments
               SET customer_id=?, vehicle_id=?, start_ts=?, duration=?, note=?, status=?
             WHERE id=?
            """;
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, vehicleId);
            ps.setString(3, startTs);
            ps.setInt(4, durationMinutes);
            ps.setString(5, note);
            ps.setString(6, status);
            ps.setInt(7, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM appointments WHERE id=?";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 1) Napi lekérdezés (YYYY-MM-DD)
    public java.util.List<hu.carenda.app.model.Appointment> findForDay(String dayIso) {
        String sql = """
        SELECT id, customer_id, vehicle_id, start_ts, duration, note, status
        FROM appointments
        WHERE substr(start_ts,1,10)=?
        ORDER BY start_ts
    """;
        try (var c = hu.carenda.app.db.Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, dayIso);
            try (var rs = ps.executeQuery()) {
                var out = new java.util.ArrayList<hu.carenda.app.model.Appointment>();
                while (rs.next()) {
                    var a = new hu.carenda.app.model.Appointment();
                    a.setId(rs.getInt("id"));
                    a.setCustomerId(rs.getInt("customer_id"));
                    a.setVehicleId(rs.getInt("vehicle_id"));
                    a.setStartTs(rs.getString("start_ts"));
                    a.setDurationMinutes(rs.getInt("duration"));
                    a.setNote(rs.getString("note"));
                    a.setStatus(rs.getString("status"));
                    out.add(a);
                }
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Appointment> findBetween(String fromIso, String toIso) {
        var sql = "SELECT * FROM appointments WHERE start_ts >= ? AND start_ts < ? ORDER BY start_ts";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, fromIso);
            ps.setString(2, toIso);
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<Appointment>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

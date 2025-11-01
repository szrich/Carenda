package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Appointment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDao {

    /**
     * Segédfüggvény: Csak az 'appointments' tábla alap mezőit map-eli.
     * A JOIN-olt mezőket (ownerName, vehiclePlate) a hívó metódusoknak kell
     * külön beállítania.
     */
    private Appointment map(ResultSet rs) throws SQLException {
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

    /**
     * Minden időpont lekérdezése, ügyféllel és járművel JOIN-olva.
     * @return 
     */
    public List<Appointment> findAll() {
        String sql = """
            SELECT a.id, a.customer_id, a.vehicle_id, a.start_ts, a.duration, a.note, a.status,
                   c.name AS customer_name, v.plate AS vehicle_plate
              FROM appointments a
              LEFT JOIN customers c ON c.id = a.customer_id
              LEFT JOIN vehicles v ON v.id = a.vehicle_id
             ORDER BY a.start_ts DESC, a.id DESC
            """;
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<Appointment> out = new ArrayList<>();
            while (rs.next()) {
                Appointment a = map(rs);
                a.setOwnerName(rs.getString("customer_name"));
                a.setVehiclePlate(rs.getString("vehicle_plate"));
                out.add(a);
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.findAll", e);
        }
    }

    /**
     * Keresés ügyfél név / rendszám / státusz / megjegyzés alapján.
     * @param q
     * @return 
     */
    public List<Appointment> search(String q) {
        String like = "%" + q.trim().toLowerCase() + "%";
        String sql = """
            SELECT a.id, a.customer_id, a.vehicle_id, a.start_ts, a.duration, a.note, a.status,
                   c.name AS customer_name, v.plate AS vehicle_plate
              FROM appointments a
              LEFT JOIN customers c ON c.id = a.customer_id
              LEFT JOIN vehicles  v ON v.id = a.vehicle_id
             WHERE lower(c.name)    LIKE ?
                OR lower(v.plate)   LIKE ?
                OR lower(a.status)  LIKE ?
                OR lower(a.note)    LIKE ?
             ORDER BY a.start_ts DESC, a.id DESC
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (var rs = ps.executeQuery()) {
                List<Appointment> out = new ArrayList<>();
                while (rs.next()) {
                    Appointment a = map(rs);
                    a.setOwnerName(rs.getString("customer_name"));
                    a.setVehiclePlate(rs.getString("vehicle_plate"));
                    out.add(a);
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.search", e);
        }
    }

    /**
     * Egy időpont lekérése ID alapján (JOIN-olt adatok nélkül).
     * @param id
     * @return 
     */
    public Appointment findById(int id) {
        String sql = """
            SELECT id, customer_id, vehicle_id, start_ts, duration, note, status
              FROM appointments WHERE id=?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.findById", e);
        }
    }

    /**
     * Új időpont beszúrása.
     * @param customerId
     * @param vehicleId
     * @param startTs
     * @param durationMinutes
     * @param note
     * @param status
     * @return Visszatér az adatbázis által generált új ID-val.
     */
    public int insert(int customerId, int vehicleId, String startTs,
                      int durationMinutes, String note, String status) {
        String sql = """
            INSERT INTO appointments(customer_id, vehicle_id, start_ts, duration, note, status)
            VALUES (?,?,?,?,?,?)
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, vehicleId);
            ps.setString(3, startTs);
            ps.setInt(4, durationMinutes);
            ps.setString(5, note);
            ps.setString(6, status);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    throw new SQLException("Időpont létrehozása sikertelen, nem kaptunk ID-t.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.insert", e);
        }
    }

    /**
     * Meglévő időpont frissítése.
     * @param id
     * @param customerId
     * @param vehicleId
     * @param startTs
     * @param durationMinutes
     * @param note
     * @param status
     */
    public void update(int id, int customerId, int vehicleId, String startTs,
                       int durationMinutes, String note, String status) {
        String sql = """
            UPDATE appointments
               SET customer_id=?, vehicle_id=?, start_ts=?, duration=?, note=?, status=?
             WHERE id=?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, vehicleId);
            ps.setString(3, startTs);
            ps.setInt(4, durationMinutes);
            ps.setString(5, note);
            ps.setString(6, status);
            ps.setInt(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.update", e);
        }
    }

    /**
     * Időpont törlése ID alapján.
     * @param id
     */
    public void delete(int id) {
        String sql = "DELETE FROM appointments WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.delete", e);
        }
    }

    /**
     * Napi lekérdezés (YYYY-MM-DD)
     * @param dayIso
     * @return 
     */
    public List<Appointment> findForDay(String dayIso) {
        String sql = """
            SELECT id, customer_id, vehicle_id, start_ts, duration, note, status
              FROM appointments
             WHERE substr(start_ts,1,10)=?
             ORDER BY start_ts
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dayIso);
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<Appointment>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.findForDay", e);
        }
    }

    /**
     * Időpontok lekérdezése két dátum között.
     * @param fromIso
     * @param toIso
     * @return 
     */
    public List<Appointment> findBetween(String fromIso, String toIso) {
        var sql = """
            SELECT id, customer_id, vehicle_id, start_ts, duration, note, status
              FROM appointments
             WHERE start_ts >= ? AND start_ts < ?
             ORDER BY start_ts
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fromIso);
            ps.setString(2, toIso);
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<Appointment>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: AppointmentDao.findBetween", e);
        }
    }
}

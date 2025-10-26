package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.ServiceJobCard;

import java.sql.ResultSet;
import java.sql.Types;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class ServiceJobCardDao {

    // Segéd: biztonságos int bind (kezeli a null-t)
    private void bindNullableInt(java.sql.PreparedStatement ps, int index, Integer value) throws Exception {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    // Eredményhalmaz -> modell
    private ServiceJobCard map(ResultSet rs) throws Exception {
        ServiceJobCard s = new ServiceJobCard();

        s.setId(rs.getObject("id", Integer.class));
        s.setJobcard_no(rs.getString("jobcard_no"));
        s.setAppointment_id(rs.getObject("appointment_id", Integer.class));
        s.setVehicle_id(rs.getObject("vehicle_id", Integer.class));
        s.setCustomer_id(rs.getObject("customer_id", Integer.class));
        s.setFault_desc(rs.getString("fault_desc"));
        s.setRepair_note(rs.getString("repair_note"));
        s.setDiagnosis(rs.getString("diagnosis"));
        s.setInternal_note(rs.getString("internal_note"));
        s.setStatus(rs.getString("status"));
        s.setAssignee_user_id(rs.getObject("assignee_user_id", Integer.class));
        s.setCreated_at(rs.getString("created_at"));
        s.setUpdated_at(rs.getString("updated_at"));
        s.setFinished_at(rs.getString("finished_at"));
        s.setOdometer_km(rs.getObject("odometer_km", Integer.class));
        s.setFuel_level_eighths(rs.getObject("fuel_level_eighths", Integer.class));
        s.setCurrency_code(rs.getString("currency_code"));
        s.setAdvance_cents(rs.getObject("advance_cents", Integer.class));

        return s;
    }

    public static class TotalsRow {

        public int sjc_id;
        public int subtotal_net_cents;
        public int vat_cents;
        public int total_gross_cents;
        public Integer advance_cents;      // lehet null
        public int amount_due_cents;
    }

    public TotalsRow fetchTotals(int sjcId) {
        String sql = """
        SELECT sjc_id,
               subtotal_net_cents,
               vat_cents,
               total_gross_cents,
               advance_cents,
               amount_due_cents
        FROM view_sjc_totals
        WHERE sjc_id = ?
    """;

        try (var c = hu.carenda.app.db.Database.get(); var ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    TotalsRow t = new TotalsRow();
                    t.sjc_id = rs.getInt("sjc_id");
                    t.subtotal_net_cents = rs.getInt("subtotal_net_cents");
                    t.vat_cents = rs.getInt("vat_cents");
                    t.total_gross_cents = rs.getInt("total_gross_cents");
                    // advance_cents lehet null az rs-ben
                    int advTmp = rs.getInt("advance_cents");
                    if (rs.wasNull()) {
                        t.advance_cents = null;
                    } else {
                        t.advance_cents = advTmp;
                    }
                    t.amount_due_cents = rs.getInt("amount_due_cents");
                    return t;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<ServiceJobCard> findAll() {
        String sql = """
            SELECT id, jobcard_no, appointment_id, vehicle_id, customer_id,
                   fault_desc, repair_note, diagnosis, internal_note, status,
                   assignee_user_id, created_at, updated_at, finished_at,
                   odometer_km, fuel_level_eighths, currency_code, advance_cents
              FROM servicejobcard
          ORDER BY id
        """;

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

    /**
     * A controller ezt hívja – itt egyszerűen ugyanazt adjuk vissza.
     */
    public List<ServiceJobCard> findAllWithOwnerAndVehicleData() {
        String sql = """
            SELECT s.id AS id, jobcard_no, appointment_id, vehicle_id, s.customer_id AS customer_id,
                   fault_desc, repair_note, diagnosis, internal_note, status,
                   assignee_user_id, created_at, updated_at, finished_at,
                   odometer_km, fuel_level_eighths, currency_code, advance_cents, 
                   c.name AS owner_name,
                   v.plate AS vehicle_plate,
                   v.brand AS vehicle_brand,
                   v.model AS vehicle_model
            FROM servicejobcard s
            JOIN customers c ON c.id = s.customer_id
            LEFT JOIN vehicles v ON v.id = s.vehicle_id
        ORDER BY id
        """;
        try (var c = Database.get(); var st = c.createStatement(); var rs = st.executeQuery(sql)) {

            List<ServiceJobCard> out = new ArrayList<>();
            while (rs.next()) {
                ServiceJobCard s = map(rs);
                s.setOwnerName(rs.getString("owner_name"));
                s.setPlate(rs.getString("vehicle_plate"));
                s.setBrand(rs.getString("vehicle_brand"));
                s.setModel(rs.getString("vehicle_model"));

                out.add(s);
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void insert(ServiceJobCard jc) {
        String sql = """
            INSERT INTO servicejobcard (
                jobcard_no,
                appointment_id,
                vehicle_id,
                customer_id,
                fault_desc,
                repair_note,
                diagnosis,
                internal_note,
                status,
                assignee_user_id,
                created_at,
                updated_at,
                finished_at,
                odometer_km,
                fuel_level_eighths,
                advance_cents
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {

            ps.setString(1, jc.getJobcard_no());
            bindNullableInt(ps, 2, jc.getAppointment_id());
            bindNullableInt(ps, 3, jc.getVehicle_id());
            bindNullableInt(ps, 4, jc.getCustomer_id());
            ps.setString(5, jc.getFault_desc());
            ps.setString(6, jc.getRepair_note());
            ps.setString(7, jc.getDiagnosis());
            ps.setString(8, jc.getInternal_note());
            ps.setString(9, jc.getStatus());
            bindNullableInt(ps, 10, jc.getAssignee_user_id());
            ps.setString(11, jc.getCreated_at());
            ps.setString(12, jc.getUpdated_at());
            ps.setString(13, jc.getFinished_at());
            bindNullableInt(ps, 14, jc.getOdometer_km());
            bindNullableInt(ps, 15, jc.getFuel_level_eighths());
            bindNullableInt(ps, 16, jc.getAdvance_cents());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Hiba a ServiceJobCard beszúrásakor", e);
        }
    }

    public ServiceJobCard findByAppointmentId(int appointmentId) {
        String sql = """
            SELECT id, jobcard_no, appointment_id, vehicle_id, customer_id,
                   fault_desc, repair_note, diagnosis, internal_note, status,
                   assignee_user_id, created_at, updated_at, finished_at,
                   odometer_km, fuel_level_eighths, currency_code, advance_cents
              FROM servicejobcard
             WHERE appointment_id = ?
             LIMIT 1
        """;

        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public List<ServiceJobCard> searchWithOwnerAndVehicleData(String q) {
        String like = "%" + q.trim().toLowerCase() + "%";

        String sql = """
        SELECT 
            s.id                 AS id,
            s.jobcard_no         AS jobcard_no,
            s.appointment_id     AS appointment_id,
            s.vehicle_id         AS vehicle_id,
            s.customer_id        AS customer_id,
            s.fault_desc         AS fault_desc,
            s.repair_note        AS repair_note,
            s.diagnosis          AS diagnosis,
            s.internal_note      AS internal_note,
            s.status             AS status,
            s.assignee_user_id   AS assignee_user_id,
            s.created_at         AS created_at,
            s.updated_at         AS updated_at,
            s.finished_at        AS finished_at,
            s.odometer_km        AS odometer_km,
            s.fuel_level_eighths AS fuel_level_eighths,
            s.currency_code      AS currency_code,
            s.advance_cents      AS advance_cents,

            c.name               AS owner_name,
            v.plate              AS vehicle_plate,
            v.brand              AS vehicle_brand,
            v.model              AS vehicle_model
        FROM servicejobcard s
        JOIN customers c ON c.id = s.customer_id
        LEFT JOIN vehicles v ON v.id = s.vehicle_id
        WHERE lower(s.jobcard_no)  LIKE ?
           OR lower(v.plate)       LIKE ?
           OR lower(v.brand)       LIKE ?
           OR lower(v.model)       LIKE ?
           OR lower(c.name)        LIKE ?
        ORDER BY s.id
        """;

        try (var conn = Database.get(); var ps = conn.prepareStatement(sql)) {

            // ugyanazt az 5 paramétert rakjuk be mindenhova
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);

            try (var rs = ps.executeQuery()) {
                List<ServiceJobCard> out = new ArrayList<>();
                while (rs.next()) {
                    ServiceJobCard s = map(rs); // alap mezők beállítva

                    // plusz mezők betöltése (ugyanúgy, mint a findAllWithOwnerAndVehicleData()-ban)
                    s.setOwnerName(rs.getString("owner_name"));
                    s.setPlate(rs.getString("vehicle_plate"));
                    s.setBrand(rs.getString("vehicle_brand"));
                    s.setModel(rs.getString("vehicle_model"));

                    out.add(s);
                }
                return out;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void update(ServiceJobCard jc) {
        String sql = """
            UPDATE servicejobcard
               SET jobcard_no = ?,
                   appointment_id = ?,
                   vehicle_id = ?,
                   customer_id = ?,
                   fault_desc = ?,
                   repair_note = ?,
                   diagnosis = ?,
                   internal_note = ?,
                   status = ?,
                   assignee_user_id = ?,
                   created_at = ?,
                   updated_at = ?,
                   finished_at = ?,
                   odometer_km = ?,
                   fuel_level_eighths = ?,
                   currency_code = ?,
                   advance_cents = ?
             WHERE id = ?
        """;

        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {

            ps.setString(1, jc.getJobcard_no());
            bindNullableInt(ps, 2, jc.getAppointment_id());
            bindNullableInt(ps, 3, jc.getVehicle_id());
            bindNullableInt(ps, 4, jc.getCustomer_id());
            ps.setString(5, jc.getFault_desc());
            ps.setString(6, jc.getRepair_note());
            ps.setString(7, jc.getDiagnosis());
            ps.setString(8, jc.getInternal_note());
            ps.setString(9, jc.getStatus());
            bindNullableInt(ps, 10, jc.getAssignee_user_id());
            ps.setString(11, jc.getCreated_at());
            ps.setString(12, jc.getUpdated_at());
            ps.setString(13, jc.getFinished_at());
            bindNullableInt(ps, 14, jc.getOdometer_km());
            bindNullableInt(ps, 15, jc.getFuel_level_eighths());
            ps.setString(16, jc.getCurrency_code());
            bindNullableInt(ps, 17, jc.getAdvance_cents());
            bindNullableInt(ps, 18, jc.getId()); // WHERE id = ?

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Hiba a ServiceJobCard frissítésekor", e);
        }
    }

    /**
     * Legközelebbi elérhető jobcard_no generálása. Formátum: YYYY-000001,
     * YYYY-000002, ...
     */
    public String generateNextJobCardNo() {
        int currentYear = Year.now().getValue();

        String sql = """
            SELECT jobcard_no
              FROM servicejobcard
             WHERE jobcard_no LIKE ?
             ORDER BY jobcard_no DESC
             LIMIT 1
        """;

        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {

            ps.setString(1, currentYear + "-%");

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastNo = rs.getString("jobcard_no"); // pl. "2025-000123"
                    String[] parts = lastNo.split("-");
                    int next = 1;
                    if (parts.length == 2) {
                        next = Integer.parseInt(parts[1]) + 1;
                    }
                    return String.format("%d-%06d", currentYear, next);
                } else {
                    // ebben az évben még nincs
                    return String.format("%d-%06d", currentYear, 1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Nem sikerült jobcard_no-t generálni", e);
        }
    }
}

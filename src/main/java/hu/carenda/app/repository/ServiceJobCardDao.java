package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.ServiceJobCard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class ServiceJobCardDao {

    // --- Egységesített segédfüggvények a NULL kezelésre ---

    /**
     * Beállít egy Integer értéket, vagy NULL-t, ha az érték null.
     * (Átnevezve bindNullableInt-ről az egységességért)
     */
    private void setIntOrNull(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    /**
     * Beállít egy String értéket, vagy NULL-t, ha az érték null vagy üres (blank).
     * (Hozzáadva az egységességért)
     */
    private void setStringOrNull(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null && !value.isBlank()) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    // --- DAO Metódusok ---

    /**
     * Eredményhalmaz -> modell
     * (rs.getObject(..., Integer.class) helyes, mert null-t ad vissza DB NULL esetén)
     */
    private ServiceJobCard map(ResultSet rs) throws SQLException {
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

    /**
     * Beágyazott osztály a munkalap végösszegekhez.
     */
    public static class TotalsRow {
        public int sjc_id;
        public int subtotal_net_cents;
        public int vat_cents;
        public int total_gross_cents;
        public Integer advance_cents;
        public int amount_due_cents;
    }

    /**
     * Munkalap végösszegek lekérése (view_sjc_totals nézetből).
     * @param sjcId
     * @return 
     */
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

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    TotalsRow t = new TotalsRow();
                    t.sjc_id = rs.getInt("sjc_id");
                    t.subtotal_net_cents = rs.getInt("subtotal_net_cents");
                    t.vat_cents = rs.getInt("vat_cents");
                    t.total_gross_cents = rs.getInt("total_gross_cents");

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
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.fetchTotals", e);
        }
        return null;
    }

    /**
     * Összes munkalap lekérése (JOIN nélkül).
     * @return 
     */
    public List<ServiceJobCard> findAll() {
        String sql = """
            SELECT id, jobcard_no, appointment_id, vehicle_id, customer_id,
                   fault_desc, repair_note, diagnosis, internal_note, status,
                   assignee_user_id, created_at, updated_at, finished_at,
                   odometer_km, fuel_level_eighths, currency_code, advance_cents
              FROM servicejobcard
             ORDER BY id
            """;

        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<ServiceJobCard> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.findAll", e);
        }
    }

    /**
     * Összes munkalap lekérése, ügyfél és jármű adatokkal JOIN-olva.
     * @return 
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
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

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
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.findAllWithOwnerAndVehicleData", e);
        }
    }

    /**
     * Új munkalap beszúrása.
     * @param jc
     * @return Visszatér az adatbázis által generált új ID-val.
     */
    public int insert(ServiceJobCard jc) {
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

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, jc.getJobcard_no());
            setIntOrNull(ps, 2, jc.getAppointment_id());
            setIntOrNull(ps, 3, jc.getVehicle_id());
            setIntOrNull(ps, 4, jc.getCustomer_id());
            setStringOrNull(ps, 5, jc.getFault_desc());
            setStringOrNull(ps, 6, jc.getRepair_note());
            setStringOrNull(ps, 7, jc.getDiagnosis());
            setStringOrNull(ps, 8, jc.getInternal_note());
            ps.setString(9, jc.getStatus());
            setIntOrNull(ps, 10, jc.getAssignee_user_id());
            setStringOrNull(ps, 11, jc.getCreated_at());
            setStringOrNull(ps, 12, jc.getUpdated_at());
            setStringOrNull(ps, 13, jc.getFinished_at());
            setIntOrNull(ps, 14, jc.getOdometer_km());
            setIntOrNull(ps, 15, jc.getFuel_level_eighths());
            setIntOrNull(ps, 16, jc.getAdvance_cents());

            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    jc.setId(newId);
                    return newId;
                } else {
                    throw new SQLException("Munkalap létrehozása sikertelen, nem kaptunk ID-t.");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.insert", e);
        }
    }

    /**
     * Munkalap keresése Appointment ID alapján.
     * @param appointmentId
     * @return 
     */
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

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, appointmentId);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.findByAppointmentId", e);
        }
    }

    /**
     * Keresés munkalapszám, rendszám, ügyfél, stb. alapján.
     * @param q
     * @return 
     */
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

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);

            try (var rs = ps.executeQuery()) {
                List<ServiceJobCard> out = new ArrayList<>();
                while (rs.next()) {
                    ServiceJobCard s = map(rs); // alap mezők
                    // plusz mezők
                    s.setOwnerName(rs.getString("owner_name"));
                    s.setPlate(rs.getString("vehicle_plate"));
                    s.setBrand(rs.getString("vehicle_brand"));
                    s.setModel(rs.getString("vehicle_model"));
                    out.add(s);
                }
                return out;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.searchWithOwnerAndVehicleData", e);
        }
    }

    /**
     * Meglévő munkalap frissítése.
     * @param jc
     */
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

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, jc.getJobcard_no());
            setIntOrNull(ps, 2, jc.getAppointment_id());
            setIntOrNull(ps, 3, jc.getVehicle_id());
            setIntOrNull(ps, 4, jc.getCustomer_id());
            setStringOrNull(ps, 5, jc.getFault_desc());
            setStringOrNull(ps, 6, jc.getRepair_note());
            setStringOrNull(ps, 7, jc.getDiagnosis());
            setStringOrNull(ps, 8, jc.getInternal_note());
            ps.setString(9, jc.getStatus());
            setIntOrNull(ps, 10, jc.getAssignee_user_id());
            setStringOrNull(ps, 11, jc.getCreated_at());
            setStringOrNull(ps, 12, jc.getUpdated_at());
            setStringOrNull(ps, 13, jc.getFinished_at());
            setIntOrNull(ps, 14, jc.getOdometer_km());
            setIntOrNull(ps, 15, jc.getFuel_level_eighths());
            setStringOrNull(ps, 16, jc.getCurrency_code());
            setIntOrNull(ps, 17, jc.getAdvance_cents());
            setIntOrNull(ps, 18, jc.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.update", e);
        }
    }

    /**
     * Legközelebbi elérhető jobcard_no generálása. Formátum: YYYY-000001, ...
     * @return 
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

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, currentYear + "-%");

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastNo = rs.getString("jobcard_no"); // pl. "2025-000123"
                    String[] parts = lastNo.split("-");
                    int next = 1;
                    if (parts.length == 2) {
                        try {
                            next = Integer.parseInt(parts[1]) + 1;
                        } catch (NumberFormatException e) {
                            next = 1;
                        }
                    }
                    return String.format("%d-%06d", currentYear, next);
                } else {
                    // ebben az évben még nincs
                    return String.format("%d-%06d", currentYear, 1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardDao.generateNextJobCardNo", e);
        }
    }
}

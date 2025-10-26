package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.ServiceJobCardWorkDesc;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ServiceJobCardWorkDescDao {

    private ServiceJobCardWorkDesc map(ResultSet rs) throws Exception {
        ServiceJobCardWorkDesc w = new ServiceJobCardWorkDesc();
        w.setId(rs.getInt("id"));
        w.setSjc_id(rs.getInt("sjc_id"));
        w.setName(rs.getString("name"));
        w.setHours(rs.getDouble("hours"));
        w.setRate_cents(rs.getInt("rate_cents"));
        w.setVat_percent(rs.getInt("vat_percent"));
        w.setSort_order(rs.getInt("sort_order"));
        return w;
    }

    /**
     * Összes sor visszaadása (debug / admin nézethez).
     */
    public List<ServiceJobCardWorkDesc> findAll() {
        String sql = """
            SELECT id, sjc_id, name, hours, rate_cents, vat_percent, sort_order
              FROM servicejobcard_workdesc
             ORDER BY sjc_id, sort_order, id
            """;
        try (var c = Database.get();
             var st = c.createStatement();
             var rs = st.executeQuery(sql)) {

            List<ServiceJobCardWorkDesc> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adott munkalaphoz (servicejobcard.id -> sjc_id) tartozó munkadíj tételek.
     */
    public List<ServiceJobCardWorkDesc> findByJobCard(int sjcId) {
        String sql = """
            SELECT id, sjc_id, name, hours, rate_cents, vat_percent, sort_order
              FROM servicejobcard_workdesc
             WHERE sjc_id=?
             ORDER BY sort_order, id
            """;
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);

            try (var rs = ps.executeQuery()) {
                List<ServiceJobCardWorkDesc> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Egy sor lekérése id alapján.
     */
    public ServiceJobCardWorkDesc findById(int id) {
        String sql = """
            SELECT id, sjc_id, name, hours, rate_cents, vat_percent, sort_order
              FROM servicejobcard_workdesc
             WHERE id=?
            """;
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Új munkadíj tétel beszúrása.
     * Visszatér az új sor autoincrement ID-jával.
     */
    public int insert(int sjcId,
                      String name,
                      double hours,
                      int rateCents,
                      int vatPercent,
                      int sortOrder) {

        String sql = """
            INSERT INTO servicejobcard_workdesc
                (sjc_id, name, hours, rate_cents, vat_percent, sort_order)
            VALUES (?,?,?,?,?,?)
            """;

        try (var c = Database.get();
             var ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sjcId);
            ps.setString(2, name);
            ps.setDouble(3, hours);
            ps.setInt(4, rateCents);
            ps.setInt(5, vatPercent);
            ps.setInt(6, sortOrder);

            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Létező tétel módosítása (általában név, óra, ár, ÁFA, sorrend).
     * A sjc_id-t direkt nem piszkáljuk itt.
     */
    public void update(int id,
                       String name,
                       double hours,
                       int rateCents,
                       int vatPercent,
                       int sortOrder) {

        String sql = """
            UPDATE servicejobcard_workdesc
               SET name=?,
                   hours=?,
                   rate_cents=?,
                   vat_percent=?,
                   sort_order=?
             WHERE id=?
            """;

        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setDouble(2, hours);
            ps.setInt(3, rateCents);
            ps.setInt(4, vatPercent);
            ps.setInt(5, sortOrder);
            ps.setInt(6, id);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Egy sor törlése id alapján.
     */
    public void delete(int id) {
        String sql = "DELETE FROM servicejobcard_workdesc WHERE id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Összes tétel törlése egy adott munkalaphoz (ha pl. újragenerálod).
     * (FK CASCADE miatt sokszor nem is kell, mert ha törlöd a munkalapot,
     * akkor a sorok maguktól mennek a kukába. A séma ON DELETE CASCADE-t használ
     * az sjc_id idegen kulcsnál. :contentReference[oaicite:6]{index=6})
     */
    public void deleteByJobCard(int sjcId) {
        String sql = "DELETE FROM servicejobcard_workdesc WHERE sjc_id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

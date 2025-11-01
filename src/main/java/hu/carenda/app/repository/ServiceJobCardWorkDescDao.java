package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.ServiceJobCardWorkDesc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceJobCardWorkDescDao {

    private ServiceJobCardWorkDesc map(ResultSet rs) throws SQLException {
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
     * @return 
     */
    public List<ServiceJobCardWorkDesc> findAll() {
        String sql = """
            SELECT id, sjc_id, name, hours, rate_cents, vat_percent, sort_order
              FROM servicejobcard_workdesc
             ORDER BY sjc_id, sort_order, id
            """;
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<ServiceJobCardWorkDesc> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardWorkDescDao.findAll", e);
        }
    }

    /**
     * Adott munkalaphoz (servicejobcard.id -> sjc_id) tartozó munkadíj tételek.
     * @param sjcId
     * @return 
     */
    public List<ServiceJobCardWorkDesc> findByJobCard(int sjcId) {
        String sql = """
            SELECT id, sjc_id, name, hours, rate_cents, vat_percent, sort_order
              FROM servicejobcard_workdesc
             WHERE sjc_id=?
             ORDER BY sort_order, id
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);

            try (var rs = ps.executeQuery()) {
                List<ServiceJobCardWorkDesc> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardWorkDescDao.findByJobCard", e);
        }
    }

    /**
     * Egy sor lekérése id alapján.
     * @param id
     * @return 
     */
    public ServiceJobCardWorkDesc findById(int id) {
        String sql = """
            SELECT id, sjc_id, name, hours, rate_cents, vat_percent, sort_order
              FROM servicejobcard_workdesc
             WHERE id=?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardWorkDescDao.findById", e);
        }
    }

    /**
     * Új munkadíj tétel beszúrása.
     * Visszatér az új sor autoincrement ID-jával.
     * @param sjcId
     * @param name
     * @param hours
     * @param rateCents
     * @param vatPercent
     * @param sortOrder
     * @return 
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

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sjcId);
            ps.setString(2, name);
            ps.setDouble(3, hours);
            ps.setInt(4, rateCents);
            ps.setInt(5, vatPercent);
            ps.setInt(6, sortOrder);

            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    throw new SQLException("Munkadíj tétel létrehozása sikertelen, nem kaptunk ID-t.");
                }
            }

        } catch (SQLException e) { 
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardWorkDescDao.insert", e);
        }
    }

    /**
     * Létező tétel módosítása (általában név, óra, ár, ÁFA, sorrend)
     * @param id.
     * @param name
     * @param hours
     * @param rateCents
     * @param vatPercent
     * @param sortOrder
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

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setDouble(2, hours);
            ps.setInt(3, rateCents);
            ps.setInt(4, vatPercent);
            ps.setInt(5, sortOrder);
            ps.setInt(6, id);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardWorkDescDao.update", e);
        }
    }

    /**
     * Egy sor törlése id alapján.
     * @param id
     */
    public void delete(int id) {
        String sql = "DELETE FROM servicejobcard_workdesc WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardWorkDescDao.delete", e);
        }
    }

    /**
     * Összes tétel törlése egy adott munkalaphoz.
     * @param sjcId
     */
    public void deleteByJobCard(int sjcId) {
        String sql = "DELETE FROM servicejobcard_workdesc WHERE sjc_id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardWorkDescDao.deleteByJobCard", e);
        }
    }
}

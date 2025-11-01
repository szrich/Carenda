package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.ServiceJobCardPart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ServiceJobCardPartDao {

    // --- Egységesített segédfüggvény a NULL kezelésre ---

    /**
     * Beállít egy String értéket, vagy NULL-t, ha az érték null vagy üres (blank).
     * (Hozzáadva az egységességért, pl. a 'sku' mezőhöz)
     */
    private void setStringOrNull(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null && !value.isBlank()) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    // --- DAO Metódusok ---

    private ServiceJobCardPart map(ResultSet rs) throws SQLException {
        ServiceJobCardPart p = new ServiceJobCardPart();
        p.setId(rs.getInt("id"));
        p.setSjc_id(rs.getInt("sjc_id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setQuantity(rs.getDouble("quantity"));
        p.setUnit_price_cents(rs.getInt("unit_price_cents"));
        p.setVat_percent(rs.getInt("vat_percent"));
        p.setSort_order(rs.getInt("sort_order"));
        return p;
    }

    /**
     * Összes alkatrész tétel (debug / admin).
     * @return 
     */
    public List<ServiceJobCardPart> findAll() {
        String sql = """
            SELECT id, sjc_id, sku, name, quantity, unit_price_cents, vat_percent, sort_order
              FROM servicejobcard_part
             ORDER BY sjc_id, sort_order, id
            """;
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<ServiceJobCardPart> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardPartDao.findAll", e);
        }
    }

    /**
     * Adott munkalaphoz (sjc_id) tartozó alkatrész tételek.
     * @param sjcId
     * @return 
     */
    public List<ServiceJobCardPart> findByJobCard(int sjcId) {
        String sql = """
            SELECT id, sjc_id, sku, name, quantity, unit_price_cents, vat_percent, sort_order
              FROM servicejobcard_part
             WHERE sjc_id=?
             ORDER BY sort_order, id
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);

            try (var rs = ps.executeQuery()) {
                List<ServiceJobCardPart> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardPartDao.findByJobCard", e);
        }
    }

    /**
     * Egy alkatrész sor lekérése id alapján.
     * @param id
     * @return 
     */
    public ServiceJobCardPart findById(int id) {
        String sql = """
            SELECT id, sjc_id, sku, name, quantity, unit_price_cents, vat_percent, sort_order
              FROM servicejobcard_part
             WHERE id=?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardPartDao.findById", e);
        }
    }

    /**
     * Új alkatrész tétel beszúrása.
     * Visszatér az új rekord ID-jával.
     * @param sjcId
     * @param sku
     * @param name
     * @param quantity
     * @param unitPriceCents
     * @param vatPercent
     * @param sortOrder
     * @return 
     */
    public int insert(int sjcId,
                      String sku,
                      String name,
                      double quantity,
                      int unitPriceCents,
                      int vatPercent,
                      int sortOrder) {

        String sql = """
            INSERT INTO servicejobcard_part
                (sjc_id, sku, name, quantity, unit_price_cents, vat_percent, sort_order)
            VALUES (?,?,?,?,?,?,?)
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sjcId);
            setStringOrNull(ps, 2, sku);
            ps.setString(3, name);
            ps.setDouble(4, quantity);
            ps.setInt(5, unitPriceCents);
            ps.setInt(6, vatPercent);
            ps.setInt(7, sortOrder);

            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    throw new SQLException("Alkatrész tétel létrehozása sikertelen, nem kaptunk ID-t.");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardPartDao.insert", e);
        }
    }

    /**
     * Sor módosítása (általában név, mennyiség, ár, áfa, sorrend).
     * @param id
     * @param sku
     * @param name
     * @param quantity
     * @param unitPriceCents
     * @param vatPercent
     * @param sortOrder
     */
    public void update(int id,
                       String sku,
                       String name,
                       double quantity,
                       int unitPriceCents,
                       int vatPercent,
                       int sortOrder) {

        String sql = """
            UPDATE servicejobcard_part
               SET sku=?,
                   name=?,
                   quantity=?,
                   unit_price_cents=?,
                   vat_percent=?,
                   sort_order=?
             WHERE id=?
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            setStringOrNull(ps, 1, sku);
            ps.setString(2, name);
            ps.setDouble(3, quantity);
            ps.setInt(4, unitPriceCents);
            ps.setInt(5, vatPercent);
            ps.setInt(6, sortOrder);
            ps.setInt(7, id);

            ps.executeUpdate();

        } catch (SQLException e) { 
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardPartDao.update", e);
        }
    }

    /**
     * Egy alkatrész tétel törlése id alapján.
     * @param id
     */
    public void delete(int id) {
        String sql = "DELETE FROM servicejobcard_part WHERE id=?"; 
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardPartDao.delete", e);
        }
    }

    /**
     * Egy teljes munkalaphoz tartozó ÖSSZES alkatrész törlése.
     * @param sjcId
     */
    public void deleteByJobCard(int sjcId) {
        String sql = "DELETE FROM servicejobcard_part WHERE sjc_id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: ServiceJobCardPartDao.deleteByJobCard", e);
        }
    }
}

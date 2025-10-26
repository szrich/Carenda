package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.ServiceJobCardPart;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ServiceJobCardPartDao {

    private ServiceJobCardPart map(ResultSet rs) throws Exception {
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
     */
    public List<ServiceJobCardPart> findAll() {
        String sql = """
            SELECT id, sjc_id, sku, name, quantity, unit_price_cents, vat_percent, sort_order
              FROM servicejobcard_part
             ORDER BY sjc_id, sort_order, id
            """;
        try (var c = Database.get();
             var st = c.createStatement();
             var rs = st.executeQuery(sql)) {

            List<ServiceJobCardPart> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adott munkalaphoz (sjc_id) tartozó alkatrész tételek.
     */
    public List<ServiceJobCardPart> findByJobCard(int sjcId) {
        String sql = """
            SELECT id, sjc_id, sku, name, quantity, unit_price_cents, vat_percent, sort_order
              FROM servicejobcard_part
             WHERE sjc_id=?
             ORDER BY sort_order, id
            """;
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);

            try (var rs = ps.executeQuery()) {
                List<ServiceJobCardPart> out = new ArrayList<>();
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
     * Egy alkatrész sor lekérése id alapján.
     */
    public ServiceJobCardPart findById(int id) {
        String sql = """
            SELECT id, sjc_id, sku, name, quantity, unit_price_cents, vat_percent, sort_order
              FROM servicejobcard_part
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
     * Új alkatrész tétel beszúrása.
     * Visszatér az új rekord ID-jával.
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

        try (var c = Database.get();
             var ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, sjcId);

            if (sku != null && !sku.isBlank()) {
                ps.setString(2, sku);
            } else {
                ps.setNull(2, Types.VARCHAR); // ugyanígy kezeled pl. VIN-nél is a VehicleDao.insert-ben. :contentReference[oaicite:7]{index=7}
            }

            ps.setString(3, name);
            ps.setDouble(4, quantity);
            ps.setInt(5, unitPriceCents);
            ps.setInt(6, vatPercent);
            ps.setInt(7, sortOrder);

            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sor módosítása (általában név, mennyiség, ár, áfa, sorrend).
     * A sjc_id-t itt sem piszkáljuk.
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

        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            if (sku != null && !sku.isBlank()) {
                ps.setString(1, sku);
            } else {
                ps.setNull(1, Types.VARCHAR);
            }

            ps.setString(2, name);
            ps.setDouble(3, quantity);
            ps.setInt(4, unitPriceCents);
            ps.setInt(5, vatPercent);
            ps.setInt(6, sortOrder);
            ps.setInt(7, id);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Egy alkatrész tétel törlése id alapján.
     */
    public void delete(int id) {
        String sql = "DELETE FROM servicejobcard_part WHERE id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Egy teljes munkalaphoz tartozó ÖSSZES alkatrész törlése.
     * (Ez igazából opcionális, mert az idegen kulcs sjc_id -> servicejobcard(id)
     * ON DELETE CASCADE, tehát ha törlöd a munkalapot, ezek is mennek vele. :contentReference[oaicite:8]{index=8})
     */
    public void deleteByJobCard(int sjcId) {
        String sql = "DELETE FROM servicejobcard_part WHERE sjc_id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {

            ps.setInt(1, sjcId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

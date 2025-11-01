package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {

    /**
     * Segédfüggvény, ami egy ResultSet-sorból Customer objektumot épít.
     */
    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        return c;
    }

    /**
     * Az összes ügyfél listázása névsorrendben.
     * @return 
     */
    public List<Customer> findAll() {
        String sql = """
            SELECT id, name, phone, email
              FROM customers
             ORDER BY name
            """;

        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            List<Customer> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: CustomerDao.findAll", e);
        }
    }

    /**
     * Név/telefon/e-mail LIKE keresés (case-insensitive).
     * @param q
     * @return 
     */
    public List<Customer> search(String q) {
        String like = "%" + q.trim().toLowerCase() + "%";
        String sql = """
            SELECT id, name, phone, email
              FROM customers
             WHERE lower(name)  LIKE ?
                OR lower(phone) LIKE ?
                OR lower(email) LIKE ?
             ORDER BY name
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (var rs = ps.executeQuery()) {
                List<Customer> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: CustomerDao.search", e);
        }
    }

    /**
     * Egy ügyfél lekérése ID alapján.
     * @param id
     * @return 
     */
    public Customer findById(int id) {
        String sql = """
            SELECT id, name, phone, email
              FROM customers
             WHERE id=?
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: CustomerDao.findById", e);
        }
    }

    /**
     * Új ügyfél beszúrása.
     *
     * @param name
     * @param phone
     * @param email
     * @return Visszatér az adatbázis által generált új ID-val.
     */
    public int insert(String name, String phone, String email) {
        String sql = """
            INSERT INTO customers(name, phone, email)
            VALUES (?,?,?)
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    throw new SQLException("Ügyfél létrehozása sikertelen, nem kaptunk ID-t.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: CustomerDao.insert", e);
        }
    }

    /**
     * Meglévő ügyfél adatainak frissítése.
     * @param id
     * @param name
     * @param phone
     * @param email
     */
    public void update(int id, String name, String phone, String email) {
        String sql = """
            UPDATE customers
               SET name=?, phone=?, email=?
             WHERE id=?
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setInt(4, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: CustomerDao.update", e);
        }
    }

    /**
     * Ügyfél törlése ID alapján.
     * @param id
     */
    public void delete(int id) {
        String sql = "DELETE FROM customers WHERE id=?";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: CustomerDao.delete", e);
        }
    }
}

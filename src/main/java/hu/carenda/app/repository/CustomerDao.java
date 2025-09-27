package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Customer;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {

    private Customer map(ResultSet rs) throws Exception {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        return c;
    }

    public List<Customer> findAll() {
        String sql = "SELECT id, name, phone, email FROM customers ORDER BY name";
        try (var c = Database.get();
             var st = c.createStatement();
             var rs = st.executeQuery(sql)) {
            List<Customer> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Név/telefon/e-mail LIKE keresés (case-insensitive). */
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
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (var rs = ps.executeQuery()) {
                List<Customer> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Customer findById(int id) {
        String sql = "SELECT id, name, phone, email FROM customers WHERE id=?";
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

    public int insert(String name, String phone, String email) {
        String sql = "INSERT INTO customers(name, phone, email) VALUES (?,?,?)";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void update(int id, String name, String phone, String email) {
        String sql = "UPDATE customers SET name=?, phone=?, email=? WHERE id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM customers WHERE id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

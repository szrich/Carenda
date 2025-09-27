package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Vehicle;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {

    private Vehicle map(ResultSet rs) throws Exception {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("id"));
        v.setPlate(rs.getString("plate"));
        v.setMakeModel(rs.getString("make_model"));
        v.setOwnerId(rs.getInt("customer_id"));
        return v;
    }

    public List<Vehicle> findAll() {
        String sql = "SELECT id, plate, make_model, customer_id FROM vehicles ORDER BY plate";
        try (var c = Database.get();
             var st = c.createStatement();
             var rs = st.executeQuery(sql)) {
            List<Vehicle> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** A controller ezt hívja – itt egyszerűen ugyanazt adjuk vissza. */
    public List<Vehicle> findAllWithOwner() {
        return findAll();
    }

    /** rendszám/gyártmány vagy tulaj neve alapján keres. */
    public List<Vehicle> searchWithOwner(String q) {
        String like = "%" + q.trim().toLowerCase() + "%";
        String sql = """
            SELECT v.id, v.plate, v.make_model, v.customer_id
              FROM vehicles v
              LEFT JOIN customers c ON c.id = v.customer_id
             WHERE lower(v.plate)      LIKE ?
                OR lower(v.make_model) LIKE ?
                OR lower(c.name)       LIKE ?
             ORDER BY v.plate
            """;
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (var rs = ps.executeQuery()) {
                List<Vehicle> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Vehicle findById(int id) {
        String sql = "SELECT id, plate, make_model, customer_id FROM vehicles WHERE id=?";
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

    public List<Vehicle> findByCustomer(int customerId) {
        String sql = "SELECT id, plate, make_model, customer_id FROM vehicles WHERE customer_id=? ORDER BY plate";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (var rs = ps.executeQuery()) {
                List<Vehicle> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int insert(String plate, String makeModel, int customerId) {
        String sql = "INSERT INTO vehicles(plate, make_model, customer_id) VALUES (?,?,?)";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plate);
            ps.setString(2, makeModel);
            ps.setInt(3, customerId);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void update(int id, String plate, String makeModel, int customerId) {
        String sql = "UPDATE vehicles SET plate=?, make_model=?, customer_id=? WHERE id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, plate);
            ps.setString(2, makeModel);
            ps.setInt(3, customerId);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM vehicles WHERE id=?";
        try (var c = Database.get();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

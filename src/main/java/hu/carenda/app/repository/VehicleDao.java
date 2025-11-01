package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {

    // --- Segédfüggvények a NULL kezelés egységesítésére ---

    /**
     * Beállít egy Integer értéket, vagy NULL-t, ha az érték null.
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
     */
    private void setStringOrNull(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null && !value.isBlank()) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    // --- DAO metódusok ---

    private Vehicle map(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("id"));
        v.setPlate(rs.getString("plate"));
        v.setVin(rs.getString("vin"));
        v.setEngine_no(rs.getString("engine_no"));
        v.setBrand(rs.getString("brand"));
        v.setModel(rs.getString("model"));
        v.setYear(rs.getInt("year"));
        v.setFuel_type(rs.getString("fuel_type"));
        v.setOwnerId(rs.getInt("customer_id"));
        return v;
    }

    public List<Vehicle> findAll() {
        String sql = """
            SELECT id, plate, vin, engine_no, brand, model, year, fuel_type, customer_id
              FROM vehicles
             ORDER BY plate
            """;

        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Vehicle> out = new ArrayList<>();
            while (rs.next()) {
                out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.findAll", e);
        }
    }

    /**
     * A controller ezt hívja – itt egyszerűen ugyanazt adjuk vissza.
     * @return 
     */
    public List<Vehicle> findAllWithOwner() {
        String sql = """
            SELECT v.id, v.plate, v.vin, v.engine_no, v.brand, v.model,
                   v.year, v.fuel_type, v.customer_id,
                   c.name AS owner_name
              FROM vehicles v
              JOIN customers c ON c.id = v.customer_id
             ORDER BY v.plate
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Vehicle> out = new ArrayList<>();
            while (rs.next()) {
                Vehicle v = map(rs);
                v.setOwnerName(rs.getString("owner_name"));
                out.add(v);
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.findAllWithOwner", e);
        }
    }

    /**
     * rendszám/gyártmány vagy tulaj neve alapján keres.
     * @param q
     * @return 
     */
    public List<Vehicle> searchWithOwner(String q) {
        String like = "%" + q.trim().toLowerCase() + "%";
        String sql = """
            SELECT v.id, v.plate, v.vin, v.engine_no, v.brand, v.model,
                   v.year, v.fuel_type, v.customer_id,
                   c.name AS owner_name
              FROM vehicles v
              JOIN customers c ON c.id = v.customer_id
             WHERE lower(v.plate) LIKE ?
                OR lower(v.brand) LIKE ?
                OR lower(v.model) LIKE ?
                OR lower(c.name)  LIKE ?
             ORDER BY v.plate
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);

            try (var rs = ps.executeQuery()) {
                List<Vehicle> out = new ArrayList<>();
                while (rs.next()) {
                    Vehicle v = map(rs);
                    v.setOwnerName(rs.getString("owner_name"));
                    out.add(v);
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.searchWithOwner", e);
        }
    }


    public Vehicle findById(int id) {
        String sql = """
            SELECT id, plate, vin, engine_no, brand, model, year, fuel_type, customer_id
              FROM vehicles
             WHERE id=?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.findById", e);
        }
    }

    public List<Vehicle> findByCustomer(int customerId) {
        String sql = """
            SELECT id, plate, vin, engine_no, brand, model, year, fuel_type, customer_id
              FROM vehicles
             WHERE customer_id=?
             ORDER BY plate
            """;

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (var rs = ps.executeQuery()) {
                List<Vehicle> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.findByCustomer", e);
        }
    }

    public int insert(String plate, String vin, String engine_no, String brand, String model, Integer year, String fuel_type, int customerId) {
        String sql = """
            INSERT INTO vehicles(plate, vin, engine_no, brand, model, year, fuel_type, customer_id)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, plate);
            setStringOrNull(ps, 2, vin);
            setStringOrNull(ps, 3, engine_no);
            ps.setString(4, brand);
            ps.setString(5, model);
            setIntOrNull(ps, 6, year);
            setStringOrNull(ps, 7, fuel_type);
            ps.setInt(8, customerId);

            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    throw new SQLException("Jármű létrehozása sikertelen, nem kaptunk ID-t.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.insert", e);
        }
    }

    public void update(int id, String plate, String vin, String engine_no, String brand, String model, int year, String fuel_type, int customerId) {
        String sql = """
            UPDATE vehicles
               SET plate=?, vin=?, engine_no=?, brand=?, model=?, year=?, fuel_type=?, customer_id=?
             WHERE id=?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, plate);
            setStringOrNull(ps, 2, vin);
            setStringOrNull(ps, 3, engine_no);
            ps.setString(4, brand);
            ps.setString(5, model);
            ps.setInt(6, year);
            setStringOrNull(ps, 7, fuel_type);
            ps.setInt(8, customerId);
            ps.setInt(9, id);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.update", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM vehicles WHERE id=?";

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: VehicleDao.delete", e);
        }
    }
}

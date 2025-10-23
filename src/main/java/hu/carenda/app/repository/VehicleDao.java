package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.Vehicle;
import java.sql.Types;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {

    private Vehicle map(ResultSet rs) throws Exception {
        Vehicle v = new Vehicle();
        v.setId(rs.getInt("id"));
        v.setPlate(rs.getString("plate"));
        v.setVin(rs.getString("vin"));
        v.setEngine_no(rs.getString("engine_no"));
        v.setBrand(rs.getString("brand"));
        v.setModel(rs.getString("model"));
        v.setYear(rs.getInt("year"));
        Object okm = rs.getObject("odometer_km");
        if (okm == null) {
            v.setOdometer_km(null);
        } else if (okm instanceof Number) {
            v.setOdometer_km(((Number) okm).intValue());
        } else {
            String s = rs.getString("odometer_km");
            s = s == null ? null : s.trim();
            v.setOdometer_km((s == null || s.isEmpty()) ? null : Integer.valueOf(s));
        }
        //v.setOdometer_km(rs.getObject("odometer_km", Integer.class)); 
        v.setFuel_type(rs.getString("fuel_type"));
        v.setOwnerId(rs.getInt("customer_id"));
        return v;
    }

    public List<Vehicle> findAll() {
        String sql = "SELECT id, plate, vin, engine_no, brand, model, year, odometer_km, fuel_type, customer_id FROM vehicles ORDER BY plate";
        try (var c = Database.get(); var st = c.createStatement(); var rs = st.executeQuery(sql)) {
            List<Vehicle> out = new ArrayList<>();
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
    public List<Vehicle> findAllWithOwner() {
        String sql = """
        SELECT v.id, v.plate, v.vin, v.engine_no, v.brand, v.model,
               v.year, v.odometer_km, v.fuel_type, v.customer_id,
               c.name AS owner_name
          FROM vehicles v
          JOIN customers c ON c.id = v.customer_id
         ORDER BY v.plate
        """;
        try (var c = Database.get(); var ps = c.prepareStatement(sql); var rs = ps.executeQuery()) {
            List<Vehicle> out = new ArrayList<>();
            while (rs.next()) {
                Vehicle v = map(rs);
                v.setOwnerName(rs.getString("owner_name")); // biztosan nem null
                out.add(v);
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * rendszám/gyártmány vagy tulaj neve alapján keres.
     */
    public List<Vehicle> searchWithOwner(String q) {
        String like = "%" + q.trim().toLowerCase() + "%";
        String sql = """
        SELECT v.id, v.plate, v.vin, v.engine_no, v.brand, v.model,
               v.year, v.odometer_km, v.fuel_type, v.customer_id,
               c.name AS owner_name
          FROM vehicles v
          JOIN customers c ON c.id = v.customer_id
         WHERE lower(v.plate) LIKE ?
            OR lower(v.brand) LIKE ?
            OR lower(v.model) LIKE ?
            OR lower(c.name)  LIKE ?
         ORDER BY v.plate
        """;

        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);

            try (var rs = ps.executeQuery()) {
                List<Vehicle> out = new ArrayList<>();
                while (rs.next()) {
                    Vehicle v = map(rs);                 // a meglévő map: id/plate/... stb.
                    v.setOwnerName(rs.getString("owner_name")); // név is betöltve
                    out.add(v);
                }
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Vehicle findById(int id) {
        String sql = "SELECT id, plate, vin, engine_no, brand, model, year, odometer_km, fuel_type, customer_id FROM vehicles WHERE id=?";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Vehicle> findByCustomer(int customerId) {
        String sql = "SELECT id, plate, vin, engine_no, brand, model, year, odometer_km, fuel_type, customer_id FROM vehicles WHERE customer_id=? ORDER BY plate";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (var rs = ps.executeQuery()) {
                List<Vehicle> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int insert(String plate, String vin, String engine_no, String brand, String model, Integer year, Integer odometer_km, String fuel_type, int customerId) {
        String sql = "INSERT INTO vehicles(plate, vin, engine_no, brand, model, year, odometer_km, fuel_type, customer_id) VALUES (?,?,?,?,?,?,?,?,?)";
        try (var c = Database.get(); var ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plate);
            if (vin != null) {
                ps.setString(2, vin);
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            if (engine_no != null) {
                ps.setString(3, engine_no);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, brand);
            ps.setString(5, model);
            ps.setObject(6, year, Types.INTEGER);
            if (odometer_km != null) {
                ps.setInt(7, odometer_km);
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, fuel_type);
            ps.setInt(9, customerId);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void update(int id, String plate, String vin, String engine_no, String brand, String model, int year, Integer odometer_km, String fuel_type, int customerId) {
        String sql = "UPDATE vehicles SET plate=?, vin=?, engine_no=?, brand=?, model=?, year=?, odometer_km=?, fuel_type=?, customer_id=? WHERE id=?";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, plate);
            ps.setString(2, vin);
            ps.setString(3, engine_no);
            ps.setString(4, brand);
            ps.setString(5, model);
            ps.setInt(6, year);
            if (odometer_km != null) {
                ps.setInt(7, odometer_km);
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, fuel_type);
            ps.setInt(9, customerId);
            ps.setInt(10, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM vehicles WHERE id=?";
        try (var c = Database.get(); var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    /**
     * Segédfüggvény, ami egy ResultSet-sorból User objektumot épít.
     */
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRoleName(rs.getString("role_name"));
        u.setMustChangePassword(rs.getInt("must_change_password") != 0);
        return u;
    }

    /**
     * Felhasználó lekérése felhasználónév alapján.
     * @param username
     * @return 
     */
    public Optional<User> findByUsername(String username) {
        final String sql = """
            SELECT id, username, full_name, password_hash, role_name, must_change_password
              FROM users
             WHERE username = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: UserDao.findByUsername", e);
        }
    }

    /**
     * Jelszó frissítése (hash-elve, és a must_change_password flag nullázása).
     * @param userId
     * @param newPlainPassword
     */
    public void updatePassword(int userId, String newPlainPassword) {
        final String sql = """
            UPDATE users
               SET password_hash = ?, must_change_password = 0
             WHERE id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String newHash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(10));
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: UserDao.updatePassword", e);
        }
    }

    /**
     * Felhasználónév és jelszó együttes frissítése (BCrypt hash-el, és a must_change_password nullázásával).
     * @param userId
     * @param newUsername
     * @param newPlainPassword
     */
    public void updateUsernameAndPassword(int userId, String newUsername, String newPlainPassword) {
        final String sql = """
            UPDATE users
               SET username = ?, password_hash = ?, must_change_password = 0
             WHERE id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String newHash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(10));
            ps.setString(1, newUsername);
            ps.setString(2, newHash);
            ps.setInt(3, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: UserDao.updateUsernameAndPassword", e);
        }
    }

    /**
     * Az összes felhasználó listázása.
     * @return 
     */
    public List<User> findAll() {
        final String sql = """
            SELECT id, username, full_name, password_hash, role_name, must_change_password
              FROM users
             ORDER BY username ASC
            """;

        List<User> out = new ArrayList<>();

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: UserDao.findAll", e);
        }

        return out;
    }

    /**
     * Új felhasználó létrehozása.
     *
     * @param username
     * @param fullName
     * @param roleName
     * @param tempPlainPassword
     * @return Visszatér az adatbázis által generált új ID-val.
     */
    public int createUser(String username, String fullName, String roleName, String tempPlainPassword) {
        final String sql = """
            INSERT INTO users (username, password_hash, full_name, role_name, must_change_password)
            VALUES (?,?,?,?,1)
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String hash = BCrypt.hashpw(tempPlainPassword, BCrypt.gensalt(10));

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, fullName);
            ps.setString(4, roleName);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    throw new SQLException("Felhasználó létrehozása sikertelen, nem kaptunk ID-t.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: UserDao.createUser", e);
        }
    }

    /**
     * Felhasználó törlése.
     * @param userId
     */
    public void deleteUser(int userId) {
        final String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: UserDao.deleteUser", e);
        }
    }

    /**
     * Jelszó visszaállítása (must_change_password = 1).
     * @param userId
     * @param newPlainPassword
     */
    public void resetPassword(int userId, String newPlainPassword) {
        final String sql = """
            UPDATE users
               SET password_hash = ?, must_change_password = 1
             WHERE id = ?
            """;

        try (Connection conn = Database.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String newHash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(10));
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Adatbázis hiba: UserDao.resetPassword", e);
        }
    }
}

package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class UserDao {

    /**
     * Felhasználó lekérése felhasználónév alapján.
     */
    public Optional<User> findByUsername(String username) {
        final String sql
                = "SELECT id, username, full_name, password_hash, role_name, must_change_password "
                + "FROM users WHERE username = ?";

        try {
            var conn = Database.get(); // NE zárd!
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }

                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRoleName(rs.getString("role_name"));

                    // új mező: must_change_password -> boolean
                    int must = rs.getInt("must_change_password");
                    u.setMustChangePassword(must != 0);

                    return Optional.of(u);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.findByUsername error", e);
        }
    }

    /**
     * Jelszó frissítése (hash-elve, és a must_change_password flag nullázása).
     */
    public void updatePassword(int userId, String newPlainPassword) {
        final String sql = "UPDATE users SET password_hash = ?, must_change_password = 0 WHERE id = ?";

        try {
            var conn = Database.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // bcrypt hash generálása
                String newHash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(10));

                ps.setString(1, newHash);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.updatePassword error", e);
        }
    }

    /**
     * Felhasználónév és jelszó együttes frissítése (BCrypt hash-el, és a
     * must_change_password nullázásával).
     */
    public void updateUsernameAndPassword(int userId, String newUsername, String newPlainPassword) {
        final String sql = "UPDATE users SET username = ?, password_hash = ?, must_change_password = 0 WHERE id = ?";

        try {
            var conn = Database.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String newHash = org.mindrot.jbcrypt.BCrypt.hashpw(newPlainPassword, org.mindrot.jbcrypt.BCrypt.gensalt(10));

                ps.setString(1, newUsername);
                ps.setString(2, newHash);
                ps.setInt(3, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.updateUsernameAndPassword error", e);
        }
    }

    public java.util.List<User> findAll() {
        final String sql
                = "SELECT id, username, full_name, password_hash, role_name, must_change_password "
                + "FROM users ORDER BY username ASC";

        java.util.List<User> out = new java.util.ArrayList<>();

        try {
            var conn = Database.get();
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRoleName(rs.getString("role_name"));
                    u.setMustChangePassword(rs.getInt("must_change_password") != 0);
                    out.add(u);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.findAll error", e);
        }

        return out;
    }

    public void createUser(String username, String fullName, String roleName, String tempPlainPassword) {
        final String sql
                = "INSERT INTO users (username, password_hash, full_name, role_name, must_change_password) "
                + "VALUES (?,?,?,?,1)";

        try {
            var conn = Database.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
                        tempPlainPassword,
                        org.mindrot.jbcrypt.BCrypt.gensalt(10)
                );

                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, fullName);
                ps.setString(4, roleName);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.createUser error", e);
        }
    }

    public void deleteUser(int userId) {
        final String sql = "DELETE FROM users WHERE id = ?";

        try {
            var conn = Database.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.deleteUser error", e);
        }
    }

    public void resetPassword(int userId, String newPlainPassword) {
        final String sql
                = "UPDATE users SET password_hash = ?, must_change_password = 1 WHERE id = ?";

        try {
            var conn = Database.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String newHash = org.mindrot.jbcrypt.BCrypt.hashpw(
                        newPlainPassword,
                        org.mindrot.jbcrypt.BCrypt.gensalt(10)
                );
                ps.setString(1, newHash);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.resetPassword error", e);
        }
    }

}

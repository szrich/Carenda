package hu.carenda.app.repository;

import hu.carenda.app.db.Database;
import hu.carenda.app.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class UserDao {

    public Optional<User> findByUsername(String username) {
        final String sql =
            "SELECT id, username, full_name, password_hash, role_name " +
            "FROM users WHERE username = ?";

        try {
            var conn = Database.get(); // NE zárd!
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();

                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setFullName(rs.getString("full_name"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRoleName(rs.getString("role_name"));
                    return Optional.of(u);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("UserDao.findByUsername error", e);
        }
    }
}

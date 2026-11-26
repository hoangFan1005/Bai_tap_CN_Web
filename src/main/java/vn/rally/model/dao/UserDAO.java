package vn.rally.model.dao;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import vn.rally.model.bean.User;

public final class UserDAO {
    public record Account(User user, String passwordHash) {
    }

    public Account findByEmail(String email) throws SQLException {
        String sql = "SELECT id, name, email, role, password_hash FROM users WHERE email = ?";
        try (var connection = Database.open(); var query = connection.prepareStatement(sql)) {
            query.setString(1, email);
            try (var row = query.executeQuery()) {
                if (!row.next()) {
                    return null;
                }
                return new Account(new User(row.getLong("id"), row.getString("name"), row.getString("email"),
                        "ADMIN".equals(row.getString("role"))), row.getString("password_hash"));
            }
        }
    }

    public boolean create(String name, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'USER')";
        try (var connection = Database.open(); var query = connection.prepareStatement(sql)) {
            query.setString(1, name);
            query.setString(2, email);
            query.setString(3, passwordHash);
            return query.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException exception) {
            if (exception.getErrorCode() == 1062) {
                return false;
            }
            throw exception;
        }
    }
}

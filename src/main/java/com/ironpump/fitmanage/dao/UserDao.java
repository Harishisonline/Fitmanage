package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access for users. All queries use PreparedStatement. */
public class UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not look up user: " + username, e);
        }
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("Could not load user " + id, e); }
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY role, username";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DatabaseException("Could not list users", e); }
    }

    public int insert(User u) {
        String sql = "INSERT INTO users (username, password_hash, display_name, role, linked_trainer_id, email) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getDisplayName());
            ps.setString(4, u.getRole().name());
            if (u.getLinkedTrainerId() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, u.getLinkedTrainerId());
            ps.setString(6, u.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { u.setUserId(keys.getInt(1)); return u.getUserId(); }
            }
            return 0;
        } catch (SQLException e) { throw new DatabaseException("Could not insert user " + u.getUsername(), e); }
    }

    public void updatePassword(int userId, String newHash) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET password_hash = ?, failed_attempts = 0, locked_until = NULL WHERE user_id = ?")) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not update password", e); }
    }

    public void updateProfile(int userId, String displayName, String email) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET display_name = ?, email = ? WHERE user_id = ?")) {
            ps.setString(1, displayName);
            ps.setString(2, email);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not update profile", e); }
    }

    public void recordFailedAttempt(int userId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { log.warn("recordFailedAttempt failed for user {}", userId, e); }
    }

    public void lock(int userId, int minutes) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET locked_until = DATE_ADD(NOW(), INTERVAL ? MINUTE), failed_attempts = 0 WHERE user_id = ?")) {
            ps.setInt(1, minutes);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { log.warn("lock failed for user {}", userId, e); }
    }

    public void unlock(int userId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("unlock user failed", e); }
    }

    public void updateLastLogin(int userId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("updateLastLogin failed", e); }
    }

    public void setActive(int userId, boolean active) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET active = ? WHERE user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not set user active", e); }
    }

    /** Full admin edit: username, role, linked trainer, active flag, display name, email. */
    public void update(User u) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET username = ?, display_name = ?, role = ?, linked_trainer_id = ?, email = ?, active = ? WHERE user_id = ?")) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getDisplayName());
            ps.setString(3, u.getRole().name());
            if (u.getLinkedTrainerId() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, u.getLinkedTrainerId());
            ps.setString(5, u.getEmail());
            ps.setBoolean(6, u.isActive());
            ps.setInt(7, u.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not update user " + u.getUserId(), e); }
    }

    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setDisplayName(rs.getString("display_name"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        int lt = rs.getInt("linked_trainer_id");
        u.setLinkedTrainerId(rs.wasNull() ? null : lt);
        u.setEmail(rs.getString("email"));
        u.setActive(rs.getBoolean("active"));
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        Timestamp lu = rs.getTimestamp("locked_until");
        u.setLockedUntil(lu == null ? null : lu.toLocalDateTime());
        Timestamp ll = rs.getTimestamp("last_login_at");
        u.setLastLoginAt(ll == null ? null : ll.toLocalDateTime());
        Timestamp c = rs.getTimestamp("created_at");
        u.setCreatedAt(c == null ? null : c.toLocalDateTime());
        Timestamp up = rs.getTimestamp("updated_at");
        u.setUpdatedAt(up == null ? null : up.toLocalDateTime());
        return u;
    }
}

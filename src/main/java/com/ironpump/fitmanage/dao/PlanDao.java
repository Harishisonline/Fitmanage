package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.Plan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlanDao {
    public List<Plan> findAll() {
        String sql = "SELECT * FROM plans ORDER BY duration_days";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Plan> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DatabaseException("Could not list plans", e); }
    }

    public List<Plan> findActive() {
        String sql = "SELECT * FROM plans WHERE active = TRUE ORDER BY duration_days";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Plan> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DatabaseException("Could not list active plans", e); }
    }

    public Optional<Plan> findById(int id) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM plans WHERE plan_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("Could not load plan " + id, e); }
    }

    public int insert(Plan p) {
        String sql = "INSERT INTO plans (name, duration_days, price, description, active) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getDurationDays());
            ps.setBigDecimal(3, p.getPrice());
            ps.setString(4, p.getDescription());
            ps.setBoolean(5, p.isActive());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) { p.setPlanId(k.getInt(1)); return p.getPlanId(); } }
            return 0;
        } catch (SQLException e) { throw new DatabaseException("Could not insert plan: " + p.getName(), e); }
    }

    public void update(Plan p) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE plans SET name=?, duration_days=?, price=?, description=?, active=? WHERE plan_id=?")) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getDurationDays());
            ps.setBigDecimal(3, p.getPrice());
            ps.setString(4, p.getDescription());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getPlanId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not update plan " + p.getPlanId(), e); }
    }

    public void setActive(int planId, boolean active) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE plans SET active = ? WHERE plan_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, planId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not set plan active", e); }
    }

    public int countActiveSubscribers(int planId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM memberships WHERE plan_id = ? AND status = 'ACTIVE'")) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("countActiveSubscribers failed", e); }
    }

    public boolean nameExists(String name, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM plans WHERE name = ?" + (excludeId != null ? " AND plan_id <> ?" : "");
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            if (excludeId != null) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new DatabaseException("nameExists check failed", e); }
    }

    private Plan map(ResultSet rs) throws SQLException {
        Plan p = new Plan();
        p.setPlanId(rs.getInt("plan_id"));
        p.setName(rs.getString("name"));
        p.setDurationDays(rs.getInt("duration_days"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setDescription(rs.getString("description"));
        p.setActive(rs.getBoolean("active"));
        Timestamp c = rs.getTimestamp("created_at");
        p.setCreatedAt(c == null ? null : c.toLocalDateTime());
        return p;
    }
}

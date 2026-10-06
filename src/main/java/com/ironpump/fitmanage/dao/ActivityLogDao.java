package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {
    public void log(Integer userId, Integer memberId, Integer trainerId, String action, String detail) {
        log(userId, memberId, trainerId, action, detail, null);
    }

    public void log(Integer userId, Integer memberId, Integer trainerId, String action, String detail, String ip) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO activity_log (user_id, member_id, trainer_id, action, detail, ip) VALUES (?, ?, ?, ?, ?, ?)")) {
            if (userId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, userId);
            if (memberId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, memberId);
            if (trainerId == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, trainerId);
            ps.setString(4, action);
            ps.setString(5, detail);
            if (ip == null || ip.isBlank()) ps.setNull(6, Types.VARCHAR); else ps.setString(6, ip);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not write activity log", e); }
    }

    public List<ActivityLog> recent(int limit) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM activity_log ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("recent activity failed", e); }
    }

    public List<ActivityLog> forMember(int memberId, int limit) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM activity_log WHERE member_id = ? ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, memberId); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("forMember activity failed", e); }
    }

    public List<ActivityLog> forTrainer(int trainerId, int limit) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM activity_log WHERE trainer_id = ? OR (user_id IN (SELECT user_id FROM users WHERE linked_trainer_id = ?)) ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, trainerId); ps.setInt(2, trainerId); ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("forTrainer activity failed", e); }
    }

    public List<ActivityLog> forUser(int userId, int limit) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM activity_log WHERE user_id = ? ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, userId); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<ActivityLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("forUser activity failed", e); }
    }

    private ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog a = new ActivityLog();
        a.setActivityId(rs.getLong("activity_id"));
        int u = rs.getInt("user_id"); a.setUserId(rs.wasNull() ? null : u);
        int m = rs.getInt("member_id"); a.setMemberId(rs.wasNull() ? null : m);
        int t = rs.getInt("trainer_id"); a.setTrainerId(rs.wasNull() ? null : t);
        a.setAction(rs.getString("action"));
        a.setDetail(rs.getString("detail"));
        a.setIp(rs.getString("ip"));
        Timestamp c = rs.getTimestamp("created_at"); a.setCreatedAt(c == null ? null : c.toLocalDateTime());
        return a;
    }
}

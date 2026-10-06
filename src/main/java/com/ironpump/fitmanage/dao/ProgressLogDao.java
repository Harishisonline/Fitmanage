package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.ProgressLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgressLogDao {
    public int insert(ProgressLog p) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO progress_logs (member_id, log_date, weight_kg, chest_cm, waist_cm, body_fat_pct, notes, logged_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getMemberId());
            ps.setDate(2, Date.valueOf(p.getLogDate()));
            ps.setBigDecimal(3, p.getWeightKg());
            if (p.getChestCm() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, p.getChestCm());
            if (p.getWaistCm() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, p.getWaistCm());
            ps.setBigDecimal(6, p.getBodyFatPct());
            ps.setString(7, p.getNotes());
            if (p.getLoggedBy() == null) ps.setNull(8, Types.INTEGER); else ps.setInt(8, p.getLoggedBy());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) { p.setLogId(k.getInt(1)); return p.getLogId(); } }
            return 0;
        } catch (SQLException e) { throw new DatabaseException("insert progress log failed", e); }
    }

    public List<ProgressLog> listForMember(int memberId, int limit) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM progress_logs WHERE member_id = ? ORDER BY log_date DESC LIMIT ?")) {
            ps.setInt(1, memberId); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProgressLog> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("list progress failed", e); }
    }

    public List<ProgressLog> listForMember(int memberId) {
        return listForMember(memberId, 100);
    }

    private ProgressLog map(ResultSet rs) throws SQLException {
        ProgressLog p = new ProgressLog();
        p.setLogId(rs.getInt("log_id"));
        p.setMemberId(rs.getInt("member_id"));
        p.setLogDate(rs.getDate("log_date").toLocalDate());
        p.setWeightKg(rs.getBigDecimal("weight_kg"));
        int c = rs.getInt("chest_cm"); p.setChestCm(rs.wasNull() ? null : c);
        int w = rs.getInt("waist_cm"); p.setWaistCm(rs.wasNull() ? null : w);
        p.setBodyFatPct(rs.getBigDecimal("body_fat_pct"));
        p.setNotes(rs.getString("notes"));
        int l = rs.getInt("logged_by"); p.setLoggedBy(rs.wasNull() ? null : l);
        Timestamp c2 = rs.getTimestamp("created_at"); p.setCreatedAt(c2 == null ? null : c2.toLocalDateTime());
        return p;
    }
}

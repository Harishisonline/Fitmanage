package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.Attendance;
import com.ironpump.fitmanage.model.DayCount;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceDao {

    public int insertCheckIn(int memberId, Integer trainerId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO attendance (member_id, check_in_at, trainer_id) VALUES (?, NOW(), ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, memberId);
            if (trainerId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, trainerId);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) return (int) k.getLong(1); }
            return 0;
        } catch (SQLException e) { throw new DatabaseException("Check-in failed", e); }
    }

    public boolean isCheckedIn(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE member_id = ? AND check_out_at IS NULL")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new DatabaseException("isCheckedIn failed", e); }
    }

    public void checkOut(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE attendance SET check_out_at = NOW() WHERE member_id = ? AND check_out_at IS NULL")) {
            ps.setInt(1, memberId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Check-out failed", e); }
    }

    public int countToday() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE DATE(check_in_at) = CURDATE()");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("countToday failed", e); }
    }

    public int countCurrentlyInside() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE DATE(check_in_at) = CURDATE() AND check_out_at IS NULL");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("countCurrentlyInside failed", e); }
    }

    public List<Attendance> listToday() {
        return listByDate(LocalDate.now());
    }

    public List<Attendance> listByDate(LocalDate date) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT a.*, CONCAT(m.first_name, ' ', m.last_name) AS member_name, m.member_code, m.phone, "
                + "p.name AS plan_name, t.name AS trainer_name "
                + "FROM attendance a JOIN members m ON a.member_id = m.member_id "
                + "LEFT JOIN memberships ms ON ms.member_id = m.member_id AND ms.status = 'ACTIVE' "
                + "LEFT JOIN plans p ON ms.plan_id = p.plan_id "
                + "LEFT JOIN trainers t ON t.trainer_id = a.trainer_id "
                + "WHERE DATE(a.check_in_at) = ? ORDER BY a.check_in_at DESC")) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                List<Attendance> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("listByDate attendance failed", e); }
    }

    public List<Attendance> listForMember(int memberId, int limit) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM attendance WHERE member_id = ? ORDER BY check_in_at DESC LIMIT ?")) {
            ps.setInt(1, memberId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Attendance> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("listForMember attendance failed", e); }
    }

    public List<DayCount> dailyCountsLast14Days() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT DATE(check_in_at) AS d, COUNT(*) AS n FROM attendance "
                + "WHERE check_in_at >= DATE_SUB(CURDATE(), INTERVAL 13 DAY) "
                + "GROUP BY DATE(check_in_at) ORDER BY d")) {
            try (ResultSet rs = ps.executeQuery()) {
                List<DayCount> out = new ArrayList<>();
                while (rs.next()) out.add(new DayCount(rs.getDate("d").toLocalDate(), rs.getInt("n")));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("dailyCounts failed", e); }
    }

    public Map<LocalDate, Integer> attendanceCountsByDay(int memberId, LocalDate from) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT DATE(check_in_at) AS d, COUNT(*) AS n FROM attendance WHERE member_id = ? AND check_in_at >= ? GROUP BY DATE(check_in_at)")) {
            ps.setInt(1, memberId);
            ps.setDate(2, Date.valueOf(from));
            try (ResultSet rs = ps.executeQuery()) {
                Map<LocalDate, Integer> out = new HashMap<>();
                while (rs.next()) out.put(rs.getDate("d").toLocalDate(), rs.getInt("n"));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("attendanceCountsByDay failed", e); }
    }

    public int countByMemberSince(int memberId, int days) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE member_id = ? AND check_in_at >= DATE_SUB(NOW(), INTERVAL ? DAY)")) {
            ps.setInt(1, memberId);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("countByMemberSince failed", e); }
    }

    public int countByMember(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM attendance WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("countByMember failed", e); }
    }

    public int avgSessionMinutesToday() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, check_in_at, COALESCE(check_out_at, NOW()))), 0) "
                + "FROM attendance WHERE DATE(check_in_at) = CURDATE()");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("avgSessionMinutesToday failed", e); }
    }

    public List<Attendance> currentlyInside() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT a.*, CONCAT(m.first_name, ' ', m.last_name) AS member_name, m.member_code, "
                + "t.name AS trainer_name FROM attendance a "
                + "JOIN members m ON a.member_id = m.member_id "
                + "LEFT JOIN trainers t ON t.trainer_id = a.trainer_id "
                + "WHERE a.check_out_at IS NULL AND DATE(a.check_in_at) = CURDATE() ORDER BY a.check_in_at");
             ResultSet rs = ps.executeQuery()) {
            List<Attendance> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DatabaseException("currentlyInside failed", e); }
    }

    private Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setAttendanceId(rs.getLong("attendance_id"));
        a.setMemberId(rs.getInt("member_id"));
        a.setCheckInAt(rs.getTimestamp("check_in_at").toLocalDateTime());
        Timestamp co = rs.getTimestamp("check_out_at");
        a.setCheckOutAt(co == null ? null : co.toLocalDateTime());
        int t = rs.getInt("trainer_id"); a.setTrainerId(rs.wasNull() ? null : t);
        a.setNotes(rs.getString("notes"));
        Timestamp c = rs.getTimestamp("created_at"); a.setCreatedAt(c == null ? null : c.toLocalDateTime());
        if (hasColumn(rs, "member_name")) a.setMemberName(rs.getString("member_name"));
        if (hasColumn(rs, "member_code")) a.setMemberCode(rs.getString("member_code"));
        if (hasColumn(rs, "plan_name")) a.setPlanName(rs.getString("plan_name"));
        if (hasColumn(rs, "trainer_name")) a.setTrainerName(rs.getString("trainer_name"));
        return a;
    }

    private static boolean hasColumn(ResultSet rs, String name) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (name.equalsIgnoreCase(md.getColumnLabel(i))) return true;
        }
        return false;
    }
}

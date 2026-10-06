package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.Membership;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MembershipDao {

    public int insert(Membership m, Connection c) throws SQLException {
        String sql = "INSERT INTO memberships (member_id, plan_id, trainer_id, start_date, end_date, status, renewal_of_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getMemberId());
            ps.setInt(2, m.getPlanId());
            if (m.getTrainerId() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, m.getTrainerId());
            ps.setDate(4, Date.valueOf(m.getStartDate()));
            ps.setDate(5, Date.valueOf(m.getEndDate()));
            ps.setString(6, m.getStatus().name());
            if (m.getRenewalOfId() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, m.getRenewalOfId());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) { m.setMembershipId(k.getInt(1)); return m.getMembershipId(); } }
            return 0;
        }
    }

    public Optional<Membership> findById(int id) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM memberships WHERE membership_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("load membership failed", e); }
    }

    /** The membership that covers today. A future renewal is not the current plan. */
    public Optional<Membership> findActiveForMember(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM memberships WHERE member_id = ? AND status <> 'CANCELLED' "
                        + "AND start_date <= CURDATE() AND end_date >= CURDATE() ORDER BY end_date DESC LIMIT 1")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("findActiveForMember failed", e); }
    }

    /** The next membership that has not started yet. */
    public Optional<Membership> findUpcomingForMember(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM memberships WHERE member_id = ? AND status <> 'CANCELLED' AND start_date > CURDATE() "
                        + "ORDER BY start_date ASC LIMIT 1")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("findUpcomingForMember failed", e); }
    }

    /** Open membership with the latest end date, including one that has not started. */
    public Optional<Membership> findLatestOpen(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM memberships WHERE member_id = ? AND status <> 'CANCELLED' AND end_date >= CURDATE() "
                        + "ORDER BY end_date DESC LIMIT 1")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("findLatestOpen failed", e); }
    }

    /** Renew used to mark a still-running plan EXPIRED. Put those rows back. */
    public int reopenPrematurelyExpired() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE memberships SET status = 'ACTIVE' WHERE status = 'EXPIRED' AND end_date >= CURDATE()")) {
            return ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("reopenPrematurelyExpired failed", e); }
    }

    public List<Membership> listForMember(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM memberships WHERE member_id = ? ORDER BY end_date DESC")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Membership> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("listForMember failed", e); }
    }

    public List<Membership> listForTrainer(int trainerId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM memberships WHERE trainer_id = ? AND status = 'ACTIVE' ORDER BY end_date DESC")) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Membership> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("listForTrainer failed", e); }
    }

    public List<Membership> expiringSoon(int withinDays) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM memberships WHERE status <> 'CANCELLED' AND start_date <= CURDATE() "
                        + "AND end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY) ORDER BY end_date ASC")) {
            ps.setInt(1, withinDays);
            try (ResultSet rs = ps.executeQuery()) {
                List<Membership> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("expiringSoon failed", e); }
    }

    public int countActive() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM memberships WHERE status <> 'CANCELLED' AND start_date <= CURDATE() AND end_date >= CURDATE()");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("countActive memberships failed", e); }
    }

    public void setStatus(int membershipId, Membership.Status status) {
        try (Connection c = DatabaseManager.get()) {
            setStatus(c, membershipId, status);
        } catch (SQLException e) { throw new DatabaseException("setStatus membership failed", e); }
    }

    public void setStatus(Connection c, int membershipId, Membership.Status status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE memberships SET status = ? WHERE membership_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, membershipId);
            ps.executeUpdate();
        }
    }

    public void updateTrainer(int membershipId, Integer trainerId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE memberships SET trainer_id = ? WHERE membership_id = ?")) {
            if (trainerId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, trainerId);
            ps.setInt(2, membershipId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("updateTrainer failed", e); }
    }

    public java.util.List<com.ironpump.fitmanage.model.MembershipView> findForMemberWithPlan(int memberId) {
        return queryViews("WHERE ms.member_id = ? ORDER BY ms.end_date DESC", memberId);
    }

    public java.util.List<com.ironpump.fitmanage.model.MembershipView> findForTrainerWithPlan(int trainerId) {
        return queryViews("WHERE ms.trainer_id = ? ORDER BY ms.end_date DESC", trainerId);
    }

    public java.util.List<com.ironpump.fitmanage.model.MembershipView> findExpiringSoonWithMember(int days) {
        String sql = baseViewSql()
                + " WHERE ms.status <> 'CANCELLED' AND ms.start_date <= CURDATE()"
                + " AND ms.end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY)"
                + " ORDER BY ms.end_date ASC";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<com.ironpump.fitmanage.model.MembershipView> out = new ArrayList<>();
                while (rs.next()) out.add(mapView(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("findExpiringSoonWithMember failed", e); }
    }

    public java.util.Optional<com.ironpump.fitmanage.model.MembershipView> findActiveView(int memberId) {
        String sql = baseViewSql() + " WHERE ms.member_id = ? AND ms.status <> 'CANCELLED'"
                + " AND ms.start_date <= CURDATE() AND ms.end_date >= CURDATE() ORDER BY ms.end_date DESC LIMIT 1";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? java.util.Optional.of(mapView(rs)) : java.util.Optional.empty();
            }
        } catch (SQLException e) { throw new DatabaseException("findActiveView failed", e); }
    }

    private java.util.List<com.ironpump.fitmanage.model.MembershipView> queryViews(String where, int id) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(baseViewSql() + " " + where)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<com.ironpump.fitmanage.model.MembershipView> out = new ArrayList<>();
                while (rs.next()) out.add(mapView(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("membership view query failed", e); }
    }

    private static String baseViewSql() {
        return "SELECT ms.*, p.name AS plan_name, p.duration_days, p.price, "
                + "t.name AS trainer_name, CONCAT(m.first_name, ' ', m.last_name) AS member_name, "
                + "m.member_code, m.phone AS member_phone "
                + "FROM memberships ms "
                + "JOIN plans p ON p.plan_id = ms.plan_id "
                + "JOIN members m ON m.member_id = ms.member_id "
                + "LEFT JOIN trainers t ON t.trainer_id = ms.trainer_id";
    }

    private com.ironpump.fitmanage.model.MembershipView mapView(ResultSet rs) throws SQLException {
        com.ironpump.fitmanage.model.MembershipView v = new com.ironpump.fitmanage.model.MembershipView();
        v.setMembership(map(rs));
        v.setPlanName(rs.getString("plan_name"));
        v.setDurationDays(rs.getInt("duration_days"));
        v.setPrice(rs.getBigDecimal("price"));
        v.setTrainerName(rs.getString("trainer_name"));
        v.setMemberName(rs.getString("member_name"));
        v.setMemberCode(rs.getString("member_code"));
        v.setMemberPhone(rs.getString("member_phone"));
        return v;
    }

    private Membership map(ResultSet rs) throws SQLException {
        Membership m = new Membership();
        m.setMembershipId(rs.getInt("membership_id"));
        m.setMemberId(rs.getInt("member_id"));
        m.setPlanId(rs.getInt("plan_id"));
        int t = rs.getInt("trainer_id"); m.setTrainerId(rs.wasNull() ? null : t);
        m.setStartDate(rs.getDate("start_date").toLocalDate());
        m.setEndDate(rs.getDate("end_date").toLocalDate());
        m.setStatus(Membership.Status.valueOf(rs.getString("status")));
        int r = rs.getInt("renewal_of_id"); m.setRenewalOfId(rs.wasNull() ? null : r);
        Timestamp c = rs.getTimestamp("created_at"); m.setCreatedAt(c == null ? null : c.toLocalDateTime());
        return m;
    }
}

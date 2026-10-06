package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDao {

    public int insert(Payment p, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payments (receipt_no, member_id, membership_id, amount, payment_type, method, reference, payment_date, notes, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getReceiptNo());
            ps.setInt(2, p.getMemberId());
            if (p.getMembershipId() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, p.getMembershipId());
            ps.setBigDecimal(4, p.getAmount());
            ps.setString(5, p.getPaymentType().name());
            ps.setString(6, p.getMethod().name());
            ps.setString(7, p.getReference());
            ps.setTimestamp(8, Timestamp.valueOf(p.getPaymentDate()));
            ps.setString(9, p.getNotes());
            ps.setInt(10, p.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) { p.setPaymentId(k.getInt(1)); return p.getPaymentId(); } }
            return 0;
        }
    }

    public String nextReceiptNo(int year, Connection c) throws SQLException {
        int next = 1;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT receipt_no FROM payments WHERE receipt_no LIKE ? ORDER BY receipt_no DESC LIMIT 1 FOR UPDATE")) {
            ps.setString(1, "FM-" + year + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String no = rs.getString(1);
                    int dash = no.lastIndexOf('-');
                    try { next = Integer.parseInt(no.substring(dash + 1)) + 1; }
                    catch (NumberFormatException ignored) { next = 1; }
                }
            }
        }
        return String.format("FM-%d-%05d", year, next);
    }

    public List<Payment> listForMember(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM payments WHERE member_id = ? ORDER BY payment_date DESC")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("listForMember payments failed", e); }
    }

    public List<Payment> listAll(int limit, int offset) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM payments ORDER BY payment_date DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit); ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("listAll payments failed", e); }
    }

    public BigDecimal totalToday() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE DATE(payment_date) = CURDATE()");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        catch (SQLException e) { throw new DatabaseException("totalToday failed", e); }
    }

    public BigDecimal totalLastDays(int days) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_date >= DATE_SUB(NOW(), INTERVAL ? DAY)")) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        } catch (SQLException e) { throw new DatabaseException("totalLastDays failed", e); }
    }

    public BigDecimal totalThisMonth() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE YEAR(payment_date) = YEAR(CURDATE()) AND MONTH(payment_date) = MONTH(CURDATE())");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        catch (SQLException e) { throw new DatabaseException("totalThisMonth failed", e); }
    }

    public BigDecimal sumForMembership(int membershipId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE membership_id = ?")) {
            ps.setInt(1, membershipId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        } catch (SQLException e) { throw new DatabaseException("sumForMembership failed", e); }
    }

    public BigDecimal sumForMember(int memberId) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        } catch (SQLException e) { throw new DatabaseException("sumForMember failed", e); }
    }

    public List<BigDecimal> dailyTotals(int days) {
        List<BigDecimal> out = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT DATE(payment_date) AS d, COALESCE(SUM(amount),0) AS n FROM payments "
                + "WHERE payment_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) GROUP BY DATE(payment_date)")) {
            ps.setInt(1, days - 1);
            java.util.Map<java.time.LocalDate, BigDecimal> map = new java.util.HashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getDate("d").toLocalDate(), rs.getBigDecimal("n"));
            }
            for (int i = days - 1; i >= 0; i--) {
                out.add(map.getOrDefault(today.minusDays(i), BigDecimal.ZERO));
            }
            return out;
        } catch (SQLException e) { throw new DatabaseException("dailyTotals failed", e); }
    }

    public int count() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM payments");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("count payments failed", e); }
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setReceiptNo(rs.getString("receipt_no"));
        p.setMemberId(rs.getInt("member_id"));
        int m = rs.getInt("membership_id"); p.setMembershipId(rs.wasNull() ? null : m);
        p.setAmount(rs.getBigDecimal("amount"));
        String type = rs.getString("payment_type");
        p.setPaymentType(type == null ? Payment.Type.PAYMENT : Payment.Type.valueOf(type));
        p.setMethod(Payment.Method.valueOf(rs.getString("method")));
        p.setReference(rs.getString("reference"));
        p.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
        p.setNotes(rs.getString("notes"));
        p.setCreatedBy(rs.getInt("created_by"));
        Timestamp c = rs.getTimestamp("created_at"); p.setCreatedAt(c == null ? null : c.toLocalDateTime());
        return p;
    }
}

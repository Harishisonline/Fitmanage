package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.TrainerPayment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrainerPaymentDao {
    public int insert(TrainerPayment tp, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO trainer_payments (trainer_id, amount, period_start, period_end, paid_on, method, reference, notes, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tp.getTrainerId());
            ps.setBigDecimal(2, tp.getAmount());
            ps.setDate(3, Date.valueOf(tp.getPeriodStart()));
            ps.setDate(4, Date.valueOf(tp.getPeriodEnd()));
            ps.setDate(5, Date.valueOf(tp.getPaidOn()));
            ps.setString(6, tp.getMethod().name());
            ps.setString(7, tp.getReference());
            ps.setString(8, tp.getNotes());
            ps.setInt(9, tp.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) { tp.setTrainerPaymentId(k.getInt(1)); return tp.getTrainerPaymentId(); } }
            return 0;
        }
    }

    public List<TrainerPayment> listForTrainer(int trainerId, Integer year) {
        StringBuilder sql = new StringBuilder("SELECT * FROM trainer_payments WHERE trainer_id = ?");
        List<Object> p = new ArrayList<>();
        p.add(trainerId);
        if (year != null) { sql.append(" AND YEAR(paid_on) = ?"); p.add(year); }
        sql.append(" ORDER BY paid_on DESC");
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < p.size(); i++) ps.setObject(i + 1, p.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<TrainerPayment> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("listForTrainer payouts failed", e); }
    }

    public java.math.BigDecimal sumForTrainerYear(int trainerId, int year) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM trainer_payments WHERE trainer_id = ? AND YEAR(paid_on) = ?")) {
            ps.setInt(1, trainerId); ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        } catch (SQLException e) { throw new DatabaseException("sumForTrainerYear failed", e); }
    }

    public java.math.BigDecimal sumForTrainerMonth(int trainerId, int year, int month) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) FROM trainer_payments WHERE trainer_id = ? AND YEAR(paid_on) = ? AND MONTH(paid_on) = ?")) {
            ps.setInt(1, trainerId); ps.setInt(2, year); ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        } catch (SQLException e) { throw new DatabaseException("sumForTrainerMonth failed", e); }
    }

    private TrainerPayment map(ResultSet rs) throws SQLException {
        TrainerPayment tp = new TrainerPayment();
        tp.setTrainerPaymentId(rs.getInt("trainer_payment_id"));
        tp.setTrainerId(rs.getInt("trainer_id"));
        tp.setAmount(rs.getBigDecimal("amount"));
        tp.setPeriodStart(rs.getDate("period_start").toLocalDate());
        tp.setPeriodEnd(rs.getDate("period_end").toLocalDate());
        tp.setPaidOn(rs.getDate("paid_on").toLocalDate());
        tp.setMethod(com.ironpump.fitmanage.model.Payment.Method.valueOf(rs.getString("method")));
        tp.setReference(rs.getString("reference"));
        tp.setNotes(rs.getString("notes"));
        tp.setCreatedBy(rs.getInt("created_by"));
        Timestamp c = rs.getTimestamp("created_at"); tp.setCreatedAt(c == null ? null : c.toLocalDateTime());
        return tp;
    }
}

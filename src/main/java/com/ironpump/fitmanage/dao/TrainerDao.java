package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.Trainer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrainerDao {
    public List<Trainer> findAll() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM trainers ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<Trainer> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DatabaseException("list trainers failed", e); }
    }

    public List<Trainer> findActive() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM trainers WHERE active = TRUE ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<Trainer> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new DatabaseException("list active trainers failed", e); }
    }

    public Optional<Trainer> findById(int id) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM trainers WHERE trainer_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("load trainer failed", e); }
    }

    public int count() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM trainers WHERE active = TRUE");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("count trainers failed", e); }
    }

    public int insert(Trainer t) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO trainers (name, phone, email, specialization, hire_date, join_date, dob, gender, availability, salary, notes, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getPhone());
            ps.setString(3, t.getEmail());
            ps.setString(4, t.getSpecialization());
            if (t.getHireDate() == null) ps.setNull(5, Types.DATE); else ps.setDate(5, Date.valueOf(t.getHireDate()));
            if (t.getJoinDate() == null) ps.setNull(6, Types.DATE); else ps.setDate(6, Date.valueOf(t.getJoinDate()));
            if (t.getDob() == null) ps.setNull(7, Types.DATE); else ps.setDate(7, Date.valueOf(t.getDob()));
            if (t.getGender() == null) ps.setNull(8, Types.VARCHAR); else ps.setString(8, t.getGender().name());
            ps.setString(9, t.getAvailability());
            ps.setBigDecimal(10, t.getSalary());
            ps.setString(11, t.getNotes());
            ps.setBoolean(12, t.isActive());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) { t.setTrainerId(k.getInt(1)); return t.getTrainerId(); } }
            return 0;
        } catch (SQLException e) { throw new DatabaseException("insert trainer failed", e); }
    }

    public void update(Trainer t) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE trainers SET name=?, phone=?, email=?, specialization=?, hire_date=?, join_date=?, dob=?, gender=?, availability=?, salary=?, notes=?, active=? WHERE trainer_id=?")) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getPhone());
            ps.setString(3, t.getEmail());
            ps.setString(4, t.getSpecialization());
            if (t.getHireDate() == null) ps.setNull(5, Types.DATE); else ps.setDate(5, Date.valueOf(t.getHireDate()));
            if (t.getJoinDate() == null) ps.setNull(6, Types.DATE); else ps.setDate(6, Date.valueOf(t.getJoinDate()));
            if (t.getDob() == null) ps.setNull(7, Types.DATE); else ps.setDate(7, Date.valueOf(t.getDob()));
            if (t.getGender() == null) ps.setNull(8, Types.VARCHAR); else ps.setString(8, t.getGender().name());
            ps.setString(9, t.getAvailability());
            ps.setBigDecimal(10, t.getSalary());
            ps.setString(11, t.getNotes());
            ps.setBoolean(12, t.isActive());
            ps.setInt(13, t.getTrainerId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("update trainer failed", e); }
    }

    public void setActive(int id, boolean active) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE trainers SET active = ? WHERE trainer_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("set trainer active failed", e); }
    }

    public boolean phoneExists(String phone, Integer excludeId) {
        String sql = "SELECT COUNT(*) FROM trainers WHERE phone = ?" + (excludeId != null ? " AND trainer_id <> ?" : "");
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            if (excludeId != null) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new DatabaseException("phoneExists trainer failed", e); }
    }

    private Trainer map(ResultSet rs) throws SQLException {
        Trainer t = new Trainer();
        t.setTrainerId(rs.getInt("trainer_id"));
        t.setName(rs.getString("name"));
        t.setPhone(rs.getString("phone"));
        t.setEmail(rs.getString("email"));
        t.setSpecialization(rs.getString("specialization"));
        Date d = rs.getDate("hire_date"); t.setHireDate(d == null ? null : d.toLocalDate());
        Date jd = rs.getDate("join_date"); t.setJoinDate(jd == null ? null : jd.toLocalDate());
        Date dob = rs.getDate("dob"); t.setDob(dob == null ? null : dob.toLocalDate());
        String g = rs.getString("gender");
        t.setGender(g == null ? null : Trainer.Gender.valueOf(g));
        t.setAvailability(rs.getString("availability"));
        t.setSalary(rs.getBigDecimal("salary"));
        t.setNotes(rs.getString("notes"));
        t.setActive(rs.getBoolean("active"));
        Timestamp c = rs.getTimestamp("created_at"); t.setCreatedAt(c == null ? null : c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at"); t.setUpdatedAt(u == null ? null : u.toLocalDateTime());
        return t;
    }
}

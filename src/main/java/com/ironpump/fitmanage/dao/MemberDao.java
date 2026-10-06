package com.ironpump.fitmanage.dao;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.model.Member;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDao {

    /** Find by phone OR member_code. Used by the attendance check-in box. */
    public Optional<Member> findByPhoneOrCode(String input) {
        String sql = "SELECT * FROM members WHERE phone = ? OR member_code = ? LIMIT 1";
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, input);
            ps.setString(2, input);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("Lookup by phone/code failed", e); }
    }

    public Optional<Member> findById(int id) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM members WHERE member_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("Could not load member " + id, e); }
    }

    public boolean phoneExists(String phone, Integer excludeId) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        String sql = "SELECT COUNT(*) FROM members WHERE REGEXP_REPLACE(phone, '[^0-9]', '') = ?"
                + (excludeId != null ? " AND member_id <> ?" : "");
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, digits);
            if (excludeId != null) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
        } catch (SQLException e) { throw new DatabaseException("phoneExists failed", e); }
    }

    public int countAll() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM members");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("count members failed", e); }
    }

    public int countByStatus(String status) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM members WHERE status = ?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("countByStatus failed", e); }
    }

    public int count() {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'");
             ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        catch (SQLException e) { throw new DatabaseException("count members failed", e); }
    }

    /** Returns the next member_code value. The 6-digit zero-padded suffix is max(id)+1. */
    public String nextMemberCode(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(member_id), 0) + 1 FROM members");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return String.format("FM-%06d", rs.getInt(1));
        }
    }

    public int insert(Member m, Connection c) throws SQLException {
        String sql = "INSERT INTO members (member_code, first_name, last_name, dob, gender, phone, email, address, emergency_name, emergency_phone, height_cm, weight_kg, medical_notes, status, joined_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String code = m.getMemberCode() == null ? nextMemberCode(c) : m.getMemberCode();
            m.setMemberCode(code);
            ps.setString(1, code);
            ps.setString(2, m.getFirstName());
            ps.setString(3, m.getLastName());
            ps.setDate(4, Date.valueOf(m.getDob()));
            ps.setString(5, m.getGender().name());
            ps.setString(6, m.getPhone());
            if (m.getEmail() == null || m.getEmail().isBlank()) ps.setNull(7, Types.VARCHAR);
            else ps.setString(7, m.getEmail().trim());
            ps.setString(8, m.getAddress());
            ps.setString(9, m.getEmergencyName());
            ps.setString(10, m.getEmergencyPhone());
            if (m.getHeightCm() == null) ps.setNull(11, Types.INTEGER); else ps.setInt(11, m.getHeightCm());
            ps.setBigDecimal(12, m.getWeightKg());
            ps.setString(13, m.getMedicalNotes());
            ps.setString(14, m.getStatus().name());
            ps.setDate(15, Date.valueOf(m.getJoinedAt() == null ? LocalDate.now() : m.getJoinedAt()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) { m.setMemberId(k.getInt(1)); return m.getMemberId(); } }
            return 0;
        }
    }

    public void update(Member m) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE members SET first_name=?, last_name=?, dob=?, gender=?, phone=?, email=?, address=?, emergency_name=?, emergency_phone=?, height_cm=?, weight_kg=?, medical_notes=?, status=? WHERE member_id=?")) {
            ps.setString(1, m.getFirstName());
            ps.setString(2, m.getLastName());
            ps.setDate(3, Date.valueOf(m.getDob()));
            ps.setString(4, m.getGender().name());
            ps.setString(5, m.getPhone());
            if (m.getEmail() == null || m.getEmail().isBlank()) ps.setNull(6, Types.VARCHAR);
            else ps.setString(6, m.getEmail().trim());
            ps.setString(7, m.getAddress());
            ps.setString(8, m.getEmergencyName());
            ps.setString(9, m.getEmergencyPhone());
            if (m.getHeightCm() == null) ps.setNull(10, Types.INTEGER); else ps.setInt(10, m.getHeightCm());
            ps.setBigDecimal(11, m.getWeightKg());
            ps.setString(12, m.getMedicalNotes());
            ps.setString(13, m.getStatus().name());
            ps.setInt(14, m.getMemberId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not update member " + m.getMemberId(), e); }
    }

    public void setStatus(int memberId, Member.Status status) {
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE members SET status = ? WHERE member_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, memberId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Could not set member status", e); }
    }

    public List<Member> list(int limit, int offset, String status, Integer planId, Integer trainerId, String search) {
        StringBuilder sql = new StringBuilder("SELECT m.* FROM members m WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status)) {
            sql.append(" AND m.status = ?"); params.add(status.toUpperCase());
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (m.first_name LIKE ? OR m.last_name LIKE ? OR CONCAT(m.first_name, ' ', m.last_name) LIKE ? OR m.phone LIKE ? OR m.member_code LIKE ?)");
            String like = "%" + search.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (planId != null || trainerId != null) {
            sql.append(" AND m.member_id IN (SELECT ms.member_id FROM memberships ms WHERE 1=1");
            if (planId != null)   { sql.append(" AND ms.plan_id = ?"); params.add(planId); }
            if (trainerId != null){ sql.append(" AND ms.trainer_id = ?"); params.add(trainerId); }
            sql.append(")");
        }
        sql.append(" ORDER BY m.member_id DESC LIMIT ? OFFSET ?");
        params.add(limit); params.add(offset);
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (i >= params.size() - 2 && p instanceof Integer n) ps.setInt(i + 1, n);
                else ps.setObject(i + 1, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Member> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("List members failed", e); }
    }

    public List<com.ironpump.fitmanage.model.MemberListRow> findWithDerivedStatus(
            Integer planId, Integer trainerId, String search, String derivedStatus, String joinedWindow, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
                "SELECT m.*, p.name AS plan_name, t.name AS trainer_name, ms.end_date AS expires_on, "
                + "CASE WHEN m.status = 'INACTIVE' THEN 'INACTIVE' "
                + "WHEN ms.end_date IS NULL OR ms.status = 'CANCELLED' OR ms.end_date < CURDATE() THEN 'EXPIRED' "
                + "WHEN ms.start_date > CURDATE() THEN 'UPCOMING' "
                + "WHEN ms.end_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 'EXPIRING' "
                + "ELSE 'ACTIVE' END AS derived_status "
                + "FROM members m "
                + "LEFT JOIN memberships ms ON ms.membership_id = ("
                + "  SELECT ms2.membership_id FROM memberships ms2 WHERE ms2.member_id = m.member_id AND ms2.status <> 'CANCELLED' "
                + "  ORDER BY (ms2.start_date <= CURDATE() AND ms2.end_date >= CURDATE()) DESC, "
                + "           (ms2.start_date > CURDATE()) DESC, "
                + "           CASE WHEN ms2.start_date > CURDATE() THEN ms2.start_date ELSE DATE '9999-12-31' END ASC, "
                + "           ms2.end_date DESC LIMIT 1) "
                + "LEFT JOIN plans p ON p.plan_id = ms.plan_id "
                + "LEFT JOIN trainers t ON t.trainer_id = ms.trainer_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (m.first_name LIKE ? OR m.last_name LIKE ? OR CONCAT(m.first_name, ' ', m.last_name) LIKE ? OR m.phone LIKE ? OR m.member_code LIKE ?)");
            String like = "%" + search.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (planId != null) { sql.append(" AND ms.plan_id = ?"); params.add(planId); }
        if (trainerId != null) { sql.append(" AND ms.trainer_id = ?"); params.add(trainerId); }
        if (derivedStatus != null && !derivedStatus.isBlank() && !"All".equalsIgnoreCase(derivedStatus)) {
            sql.append(" AND (CASE WHEN m.status = 'INACTIVE' THEN 'INACTIVE' "
                    + "WHEN ms.end_date IS NULL OR ms.status = 'CANCELLED' OR ms.end_date < CURDATE() THEN 'EXPIRED' "
                    + "WHEN ms.start_date > CURDATE() THEN 'UPCOMING' "
                    + "WHEN ms.end_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 'EXPIRING' "
                    + "ELSE 'ACTIVE' END) = ?");
            params.add(derivedStatus.toUpperCase());
        }
        if ("30".equals(joinedWindow)) sql.append(" AND m.joined_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
        else if ("90".equals(joinedWindow)) sql.append(" AND m.joined_at >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)");
        else if ("year".equals(joinedWindow)) sql.append(" AND YEAR(m.joined_at) = YEAR(CURDATE())");
        sql.append(" ORDER BY m.member_id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        try (Connection c = DatabaseManager.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (i >= params.size() - 2 && p instanceof Integer n) ps.setInt(i + 1, n);
                else ps.setObject(i + 1, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<com.ironpump.fitmanage.model.MemberListRow> out = new ArrayList<>();
                while (rs.next()) {
                    com.ironpump.fitmanage.model.MemberListRow row = new com.ironpump.fitmanage.model.MemberListRow();
                    row.setMember(map(rs));
                    row.setDerivedStatus(rs.getString("derived_status"));
                    row.setPlanName(rs.getString("plan_name"));
                    row.setTrainerName(rs.getString("trainer_name"));
                    Date exp = rs.getDate("expires_on");
                    row.setExpiresOn(exp == null ? null : exp.toLocalDate());
                    out.add(row);
                }
                return out;
            }
        } catch (SQLException e) { throw new DatabaseException("findWithDerivedStatus failed", e); }
    }

    public int insert(Member m) {
        try (Connection c = DatabaseManager.get()) {
            c.setAutoCommit(false);
            try {
                int id = insert(m, c);
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw new DatabaseException("insert member failed", e); }
    }

    private Member map(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setMemberCode(rs.getString("member_code"));
        m.setFirstName(rs.getString("first_name"));
        m.setLastName(rs.getString("last_name"));
        Date d = rs.getDate("dob"); m.setDob(d == null ? null : d.toLocalDate());
        m.setGender(Member.Gender.valueOf(rs.getString("gender")));
        m.setPhone(rs.getString("phone"));
        m.setEmail(rs.getString("email"));
        m.setAddress(rs.getString("address"));
        m.setEmergencyName(rs.getString("emergency_name"));
        m.setEmergencyPhone(rs.getString("emergency_phone"));
        int h = rs.getInt("height_cm"); m.setHeightCm(rs.wasNull() ? null : h);
        m.setWeightKg(rs.getBigDecimal("weight_kg"));
        m.setMedicalNotes(rs.getString("medical_notes"));
        m.setStatus(Member.Status.valueOf(rs.getString("status")));
        Date j = rs.getDate("joined_at"); m.setJoinedAt(j == null ? null : j.toLocalDate());
        Timestamp c = rs.getTimestamp("created_at"); m.setCreatedAt(c == null ? null : c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at"); m.setUpdatedAt(u == null ? null : u.toLocalDateTime());
        return m;
    }
}

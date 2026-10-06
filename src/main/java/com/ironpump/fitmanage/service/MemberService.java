package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.exception.DuplicatePhoneException;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.*;
import com.ironpump.fitmanage.util.Validator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/** Business logic for members. The router-facing pages call into this. */
public class MemberService {
    private final MemberDao memberDao = new MemberDao();
    private final PlanDao planDao = new PlanDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final ActivityLogDao activityLog = new ActivityLogDao();

    public List<Member> list(int limit, int offset, String status, Integer planId, Integer trainerId, String search) {
        return memberDao.list(limit, offset, status, planId, trainerId, search);
    }

    public List<com.ironpump.fitmanage.model.MemberListRow> listDerived(
            Integer planId, Integer trainerId, String search, String derivedStatus, String joinedWindow, int limit, int offset) {
        return memberDao.findWithDerivedStatus(planId, trainerId, search, derivedStatus, joinedWindow, limit, offset);
    }

    public int count() { return memberDao.count(); }
    public int countAll() { return memberDao.countAll(); }

    public Member findById(int id) {
        return memberDao.findById(id).orElseThrow(() -> new FitManageException("Member not found: " + id));
    }

    /** Full create + initial subscription + initial payment, in one transaction. */
    public int register(Member member, int planId, BigDecimal initialPayment, Payment.Method method, String paymentRef) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);

        // Validate
        Validator.requireMax("firstName", member.getFirstName(), 60);
        Validator.requireMax("lastName",  member.getLastName(),  60);
        Validator.pastDate("dob", member.getDob().toString());
        member.setPhone(Validator.phone("phone", member.getPhone()));
        if (member.getEmail() != null && !member.getEmail().isBlank())
            member.setEmail(Validator.email("email", member.getEmail()));
        else member.setEmail(null);
        if (member.getHeightCm() != null) Validator.intInRange("heightCm", member.getHeightCm().toString(), 50, 250);

        if (memberDao.phoneExists(member.getPhone(), null))
            throw new DuplicatePhoneException(member.getPhone());

        Plan plan = planDao.findById(planId)
                .orElseThrow(() -> new FitManageException("Plan not found: " + planId));
        if (!plan.isActive()) throw new FitManageException("Plan " + plan.getName() + " is not active.");
        if (member.getJoinedAt() == null) member.setJoinedAt(LocalDate.now());

        com.ironpump.fitmanage.db.DatabaseManager.get(); // ensures initialised

        try (Connection c = com.ironpump.fitmanage.db.DatabaseManager.get()) {
            c.setAutoCommit(false);
            try {
                int memberId = memberDao.insert(member, c);
                Membership m = new Membership();
                m.setMemberId(memberId);
                m.setPlanId(planId);
                m.setStartDate(member.getJoinedAt());
                m.setEndDate(member.getJoinedAt().plusDays(plan.getDurationDays()));
                m.setStatus(Membership.Status.ACTIVE);
                m.setTrainerId(member.getTrainerId());
                int membershipId = membershipDao.insert(m, c);

                if (initialPayment != null && initialPayment.signum() > 0) {
                    Payment p = new Payment();
                    p.setMemberId(memberId);
                    p.setMembershipId(membershipId);
                    p.setAmount(initialPayment);
                    p.setMethod(method);
                    p.setReference(paymentRef);
                    p.setPaymentDate(java.time.LocalDateTime.now());
                    p.setCreatedBy(SessionContext.currentUserId());
                    p.setReceiptNo(paymentDao.nextReceiptNo(LocalDate.now().getYear(), c));
                    paymentDao.insert(p, c);
                }

                c.commit();
                activityLog.log(SessionContext.currentUserId(), memberId, null, "MEMBER_CREATED",
                        "Member " + member.getMemberCode() + " registered.");
                return memberId;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } catch (java.sql.SQLException e) {
                c.rollback();
                throw memberWriteFailure(e);
            } finally { c.setAutoCommit(true); }
        } catch (java.sql.SQLException e) {
            throw memberWriteFailure(e);
        }
    }

    public void update(Member m) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        Validator.requireMax("firstName", m.getFirstName(), 60);
        Validator.requireMax("lastName", m.getLastName(), 60);
        if (m.getDob() != null) Validator.pastDate("dob", m.getDob().toString());
        m.setPhone(Validator.phone("phone", m.getPhone()));
        if (m.getEmail() != null && !m.getEmail().isBlank()) m.setEmail(Validator.email("email", m.getEmail()));
        else m.setEmail(null);
        if (m.getHeightCm() != null) Validator.intInRange("heightCm", m.getHeightCm().toString(), 50, 250);
        if (memberDao.phoneExists(m.getPhone(), m.getMemberId()))
            throw new DuplicatePhoneException(m.getPhone());
        memberDao.update(m);
        activityLog.log(SessionContext.currentUserId(), m.getMemberId(), null, "MEMBER_UPDATED", null);
    }

    public void deactivate(int memberId) {
        SessionContext.requireRole(User.Role.ADMIN);
        memberDao.setStatus(memberId, Member.Status.INACTIVE);
        activityLog.log(SessionContext.currentUserId(), memberId, null, "MEMBER_DEACTIVATED", null);
    }

    public void reactivate(int memberId) {
        SessionContext.requireRole(User.Role.ADMIN);
        memberDao.setStatus(memberId, Member.Status.ACTIVE);
        activityLog.log(SessionContext.currentUserId(), memberId, null, "MEMBER_REACTIVATED", null);
    }

    private static RuntimeException memberWriteFailure(java.sql.SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("email")) return new FitManageException("That email is already used by another member.");
        if (msg.contains("phone") || msg.contains("member_code"))
            return new FitManageException("That phone or member code is already registered.");
        if (msg.contains("chk_member_weight") || msg.contains("weight"))
            return new FitManageException("Weight must be between 10 and 300 kg.");
        if (msg.contains("chk_member_height") || msg.contains("height"))
            return new FitManageException("Height must be between 50 and 250 cm.");
        return new DatabaseException("Could not save the member.", e);
    }

    public Member findByPhoneOrCode(String input) {
        return memberDao.findByPhoneOrCode(input).orElse(null);
    }
}

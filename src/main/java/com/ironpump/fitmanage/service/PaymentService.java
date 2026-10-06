package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.*;
import com.ironpump.fitmanage.util.Validator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentService {
    private final PaymentDao paymentDao = new PaymentDao();
    private final ActivityLogDao activityLog = new ActivityLogDao();

    public List<Payment> listForMember(int memberId) { return paymentDao.listForMember(memberId); }
    public BigDecimal sumForMember(int memberId)    { return paymentDao.sumForMember(memberId); }
    public List<Payment> listAll(int limit, int offset) { return paymentDao.listAll(limit, offset); }
    public int count() { return paymentDao.count(); }
    public BigDecimal totalToday()        { return paymentDao.totalToday(); }
    public BigDecimal totalLastDays(int d) { return paymentDao.totalLastDays(d); }
    public BigDecimal totalThisMonth()    { return paymentDao.totalThisMonth(); }
    public BigDecimal sumForMembership(int membershipId) { return paymentDao.sumForMembership(membershipId); }

    public int record(Payment p) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        if (p.getPaymentType() == null) {
            p.setPaymentType(p.getAmount() != null && p.getAmount().signum() < 0 ? Payment.Type.REFUND : Payment.Type.PAYMENT);
        }
        if (p.getPaymentType() == Payment.Type.REFUND) {
            if (p.getAmount() == null || p.getAmount().signum() == 0)
                throw new FitManageException("Refund amount must not be zero.");
            if (p.getAmount().signum() > 0) p.setAmount(p.getAmount().negate());
        } else {
            p.setAmount(Validator.positiveMoney("amount", p.getAmount().toPlainString()));
            p.setPaymentType(Payment.Type.PAYMENT);
        }
        if (p.getPaymentDate() == null) p.setPaymentDate(LocalDateTime.now());
        if (p.getCreatedBy() == 0) p.setCreatedBy(SessionContext.currentUserId());
        if (p.getMembershipId() == null && p.getMemberId() > 0) {
            new MembershipDao().findActiveForMember(p.getMemberId())
                    .ifPresent(open -> p.setMembershipId(open.getMembershipId()));
        }
        if (p.getReference() != null && p.getReference().isBlank()) p.setReference(null);
        RuntimeException last = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try (Connection c = com.ironpump.fitmanage.db.DatabaseManager.get()) {
                c.setAutoCommit(false);
                try {
                    p.setReceiptNo(paymentDao.nextReceiptNo(LocalDate.now().getYear(), c));
                    int id = paymentDao.insert(p, c);
                    c.commit();
                    activityLog.log(SessionContext.currentUserId(), p.getMemberId(), null,
                            p.getPaymentType() == Payment.Type.REFUND ? "REFUND_RECORDED" : "PAYMENT_RECORDED",
                            p.getReceiptNo() + " Rs." + p.getAmount(), null);
                    return id;
                } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
                    c.rollback();
                    last = new FitManageException("Receipt number collided, retrying", dup);
                } catch (Exception e) {
                    c.rollback();
                    if (e instanceof RuntimeException re) throw re;
                    throw new com.ironpump.fitmanage.exception.DatabaseException("Record payment failed", e);
                } finally { c.setAutoCommit(true); }
            } catch (SQLException e) {
                throw new com.ironpump.fitmanage.exception.DatabaseException("Record payment failed", e);
            }
        }
        throw last == null ? new FitManageException("Could not allocate a receipt number.") : last;
    }
}

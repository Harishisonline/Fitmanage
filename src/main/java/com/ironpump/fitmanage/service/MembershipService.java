package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Renew, cancel, reassign, and list memberships. */
public class MembershipService {
    private final MembershipDao membershipDao = new MembershipDao();
    private final PlanDao planDao = new PlanDao();
    private final ActivityLogDao activityLog = new ActivityLogDao();

    public List<Membership> listForMember(int memberId) { return membershipDao.listForMember(memberId); }
    public List<Membership> listForTrainer(int trainerId) { return membershipDao.listForTrainer(trainerId); }
    public List<Membership> expiringSoon(int withinDays) { return membershipDao.expiringSoon(withinDays); }

    public Optional<Membership> activeForMember(int memberId) {
        return membershipDao.findActiveForMember(memberId);
    }

    public Membership findById(int id) {
        return membershipDao.findById(id).orElseThrow(() -> new FitManageException("Membership not found: " + id));
    }

    public java.math.BigDecimal outstandingForMember(int memberId) {
        return membershipDao.findActiveView(memberId).map(v -> {
            java.math.BigDecimal paid = new PaymentDao().sumForMembership(v.getMembership().getMembershipId());
            java.math.BigDecimal price = v.getPrice() == null ? java.math.BigDecimal.ZERO : v.getPrice();
            java.math.BigDecimal due = price.subtract(paid == null ? java.math.BigDecimal.ZERO : paid);
            return due.signum() < 0 ? java.math.BigDecimal.ZERO : due;
        }).orElse(java.math.BigDecimal.ZERO);
    }

    public java.util.List<com.ironpump.fitmanage.model.MembershipView> viewsForMember(int memberId) {
        return membershipDao.findForMemberWithPlan(memberId);
    }

    public java.util.List<com.ironpump.fitmanage.model.MembershipView> viewsForTrainer(int trainerId) {
        return membershipDao.findForTrainerWithPlan(trainerId);
    }

    public java.util.Optional<com.ironpump.fitmanage.model.MembershipView> activeView(int memberId) {
        return membershipDao.findActiveView(memberId);
    }

    public void renew(int memberId, int planId, Integer trainerId) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        Plan plan = requireActivePlan(planId);
        membershipDao.findUpcomingForMember(memberId).ifPresent(u -> {
            throw new FitManageException("A renewal is already scheduled through "
                    + u.getEndDate() + ". Cancel that renewal before scheduling another.");
        });
        Optional<Membership> anchor = membershipDao.findLatestOpen(memberId);
        LocalDate start = LocalDate.now();
        Integer renewalOf = null;
        if (anchor.isPresent()) {
            start = anchor.get().getEndDate().plusDays(1);
            renewalOf = anchor.get().getMembershipId();
        }
        Membership created = insertMembership(memberId, plan, trainerId, start, start.plusDays(plan.getDurationDays()), renewalOf);
        activityLog.log(SessionContext.currentUserId(), memberId, trainerId, "MEMBERSHIP_RENEWED",
                "Plan " + planId + ", " + created.getStartDate() + " to " + created.getEndDate());
    }

    /** Start a plan today. Refuses when a plan already covers today. */
    public void startPlan(int memberId, int planId, Integer trainerId) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        if (membershipDao.findActiveForMember(memberId).isPresent())
            throw new FitManageException("A plan already covers today. Use Change plan to replace it, or Renew to queue the next period.");
        Plan plan = requireActivePlan(planId);
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(plan.getDurationDays());
        Optional<Membership> upcoming = membershipDao.findUpcomingForMember(memberId);
        if (upcoming.isPresent() && !upcoming.get().getStartDate().isAfter(end)) {
            LocalDate capped = upcoming.get().getStartDate().minusDays(1);
            if (capped.isBefore(start))
                throw new FitManageException("A renewal already starts on " + upcoming.get().getStartDate()
                        + ". Cancel it before adding a plan.");
            end = capped;
        }
        Membership created = insertMembership(memberId, plan, trainerId, start, end, null);
        activityLog.log(SessionContext.currentUserId(), memberId, trainerId, "MEMBERSHIP_STARTED",
                "Plan " + planId + ", " + created.getStartDate() + " to " + created.getEndDate());
    }

    /** Cancel every open period and start the chosen plan today. */
    public void replacePlan(int memberId, int planId, Integer trainerId) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        Plan plan = requireActivePlan(planId);
        LocalDate start = LocalDate.now();
        Membership created = new Membership();
        created.setMemberId(memberId);
        created.setPlanId(plan.getPlanId());
        created.setTrainerId(trainerId);
        created.setStartDate(start);
        created.setEndDate(start.plusDays(plan.getDurationDays()));
        created.setStatus(Membership.Status.ACTIVE);
        try (java.sql.Connection c = com.ironpump.fitmanage.db.DatabaseManager.get()) {
            c.setAutoCommit(false);
            try {
                for (Membership existing : membershipDao.listForMember(memberId)) {
                    if (existing.getStatus() == Membership.Status.CANCELLED) continue;
                    if (existing.getEndDate() != null && !existing.getEndDate().isBefore(LocalDate.now()))
                        membershipDao.setStatus(c, existing.getMembershipId(), Membership.Status.CANCELLED);
                }
                membershipDao.insert(created, c);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (java.sql.SQLException e) {
            throw new com.ironpump.fitmanage.exception.DatabaseException("Could not change plan", e);
        }
        activityLog.log(SessionContext.currentUserId(), memberId, trainerId, "MEMBERSHIP_CHANGED",
                "Plan " + planId + ", ends " + created.getEndDate());
    }

    public Optional<Membership> upcomingForMember(int memberId) {
        return membershipDao.findUpcomingForMember(memberId);
    }

    private Plan requireActivePlan(int planId) {
        Plan plan = planDao.findById(planId)
                .orElseThrow(() -> new FitManageException("Plan not found: " + planId));
        if (!plan.isActive()) throw new FitManageException("That plan is inactive.");
        return plan;
    }

    private Membership insertMembership(int memberId, Plan plan, Integer trainerId, LocalDate start, LocalDate end, Integer renewalOf) {
        Membership m = new Membership();
        m.setMemberId(memberId);
        m.setPlanId(plan.getPlanId());
        m.setTrainerId(trainerId);
        m.setStartDate(start);
        m.setEndDate(end);
        m.setStatus(Membership.Status.ACTIVE);
        m.setRenewalOfId(renewalOf);
        try (java.sql.Connection c = com.ironpump.fitmanage.db.DatabaseManager.get()) {
            c.setAutoCommit(false);
            try {
                membershipDao.insert(m, c);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (java.sql.SQLException e) {
            throw new com.ironpump.fitmanage.exception.DatabaseException("Could not save membership", e);
        }
        return m;
    }

    /** Put a cancelled plan back when its end date is still today or later and nothing else covers those dates. */
    public void reactivateMembership(int membershipId) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        Membership m = findById(membershipId);
        if (m.getStatus() != Membership.Status.CANCELLED)
            throw new FitManageException("Only a cancelled plan can be reactivated.");
        if (m.getEndDate() == null || m.getEndDate().isBefore(LocalDate.now()))
            throw new FitManageException("That plan has already ended. Renew it or add a new plan.");
        if (membershipDao.findActiveForMember(m.getMemberId()).isPresent())
            throw new FitManageException("A plan already covers today. Cancel it before reactivating this one.");
        membershipDao.findUpcomingForMember(m.getMemberId()).ifPresent(upcoming -> {
            if (!upcoming.getStartDate().isAfter(m.getEndDate()))
                throw new FitManageException("A scheduled plan overlaps these dates. Cancel it before reactivating.");
        });
        membershipDao.setStatus(membershipId, Membership.Status.ACTIVE);
        activityLog.log(SessionContext.currentUserId(), m.getMemberId(), m.getTrainerId(), "MEMBERSHIP_REACTIVATED",
                "Membership " + membershipId + " through " + m.getEndDate());
    }

    /** Latest cancelled plan that still runs through today or later. */
    public Optional<Membership> reactivatableForMember(int memberId) {
        if (membershipDao.findActiveForMember(memberId).isPresent()) return Optional.empty();
        return membershipDao.listForMember(memberId).stream()
                .filter(m -> m.getStatus() == Membership.Status.CANCELLED)
                .filter(m -> m.getEndDate() != null && !m.getEndDate().isBefore(LocalDate.now()))
                .findFirst();
    }

    public void cancel(int membershipId, String reason) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        Membership m = findById(membershipId);
        membershipDao.setStatus(membershipId, Membership.Status.CANCELLED);
        activityLog.log(SessionContext.currentUserId(), m.getMemberId(), null, "MEMBERSHIP_CANCELLED", reason);
    }

    public void reassignTrainer(int membershipId, Integer newTrainerId) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        membershipDao.updateTrainer(membershipId, newTrainerId);
        Membership m = findById(membershipId);
        activityLog.log(SessionContext.currentUserId(), m.getMemberId(), newTrainerId, "TRAINER_REASSIGNED", null);
    }
}

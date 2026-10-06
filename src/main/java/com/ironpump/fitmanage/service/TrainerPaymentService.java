package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.*;
import com.ironpump.fitmanage.util.Validator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TrainerPaymentService {
    private final TrainerPaymentDao trainerPaymentDao = new TrainerPaymentDao();
    private final TrainerDao trainerDao = new TrainerDao();
    private final ActivityLogDao activityLog = new ActivityLogDao();

    public List<TrainerPayment> listForTrainer(int trainerId, Integer year) {
        return trainerPaymentDao.listForTrainer(trainerId, year);
    }

    public int record(TrainerPayment tp) {
        SessionContext.requireRole(User.Role.ADMIN);
        Validator.requireText("amount", tp.getAmount() == null ? "" : tp.getAmount().toPlainString());
        if (tp.getAmount().signum() < 0) throw new FitManageException("Amount must be non-negative.");
        if (tp.getPeriodEnd().isBefore(tp.getPeriodStart()))
            throw new FitManageException("Period end must be on or after period start.");
        Trainer t = trainerDao.findById(tp.getTrainerId())
                .orElseThrow(() -> new FitManageException("Trainer not found: " + tp.getTrainerId()));
        if (tp.getCreatedBy() == 0) tp.setCreatedBy(SessionContext.currentUserId());
        try (Connection c = com.ironpump.fitmanage.db.DatabaseManager.get()) {
            c.setAutoCommit(false);
            try {
                int id = trainerPaymentDao.insert(tp, c);
                c.commit();
                activityLog.log(SessionContext.currentUserId(), null, t.getTrainerId(),
                        "TRAINER_PAID", "Rs." + tp.getAmount() + " for " + tp.getPeriodStart() + " to " + tp.getPeriodEnd());
                return id;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) {
            throw new com.ironpump.fitmanage.exception.DatabaseException("record trainer payment failed", e);
        }
    }

    public BigDecimal sumYear(int trainerId, int year) { return trainerPaymentDao.sumForTrainerYear(trainerId, year); }
    public BigDecimal sumMonth(int trainerId, int year, int month) { return trainerPaymentDao.sumForTrainerMonth(trainerId, year, month); }
}

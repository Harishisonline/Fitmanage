package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.model.User;
import com.ironpump.fitmanage.util.Validator;

import java.util.List;

public class TrainerService {
    private final TrainerDao trainerDao = new TrainerDao();
    private final ActivityLogDao activityLog = new ActivityLogDao();

    public List<Trainer> list()       { return trainerDao.findAll(); }
    public List<Trainer> listActive() { return trainerDao.findActive(); }
    public int count()                { return trainerDao.count(); }

    public Trainer findById(int id) {
        return trainerDao.findById(id).orElseThrow(() -> new FitManageException("Trainer not found: " + id));
    }

    public int create(Trainer t) {
        SessionContext.requireRole(User.Role.ADMIN);
        Validator.requireMax("name", t.getName(), 80);
        t.setPhone(Validator.phone("phone", t.getPhone()));
        if (t.getEmail() != null && !t.getEmail().isBlank()) t.setEmail(Validator.email("email", t.getEmail()));
        if (trainerDao.phoneExists(t.getPhone(), null))
            throw new FitManageException("A trainer with phone " + t.getPhone() + " already exists.");
        if (t.getHireDate() == null) t.setHireDate(t.getJoinDate());
        if (t.getHireDate() == null)
            throw new com.ironpump.fitmanage.exception.ValidationException("hireDate", "Hire date is required.");
        int id = trainerDao.insert(t);
        activityLog.log(SessionContext.currentUserId(), null, id, "TRAINER_CREATED", t.getName());
        return id;
    }

    public void update(Trainer t) {
        User current = SessionContext.current();
        boolean isSelf = current != null && current.getLinkedTrainerId() != null
                && current.getLinkedTrainerId() == t.getTrainerId();
        if (!isSelf) SessionContext.requireRole(User.Role.ADMIN);
        Validator.requireMax("name", t.getName(), 80);
        t.setPhone(Validator.phone("phone", t.getPhone()));
        if (t.getEmail() != null && !t.getEmail().isBlank()) t.setEmail(Validator.email("email", t.getEmail()));
        if (trainerDao.phoneExists(t.getPhone(), t.getTrainerId()))
            throw new FitManageException("Another trainer with phone " + t.getPhone() + " already exists.");
        trainerDao.update(t);
        activityLog.log(SessionContext.currentUserId(), null, t.getTrainerId(), "TRAINER_UPDATED", t.getName());
    }

    public void setActive(int trainerId, boolean active) {
        SessionContext.requireRole(User.Role.ADMIN);
        trainerDao.setActive(trainerId, active);
    }
}

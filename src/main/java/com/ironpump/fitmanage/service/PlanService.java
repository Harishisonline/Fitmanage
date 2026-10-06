package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.PlanDao;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.exception.PlanInUseException;
import com.ironpump.fitmanage.model.Plan;
import com.ironpump.fitmanage.model.User;
import com.ironpump.fitmanage.util.Validator;

import java.util.List;

public class PlanService {
    private final PlanDao planDao = new PlanDao();

    public List<Plan> list()        { return planDao.findAll(); }
    public List<Plan> listActive()  { return planDao.findActive(); }
    public int activeSubscribers(int planId) { return planDao.countActiveSubscribers(planId); }

    public Plan findById(int id) {
        return planDao.findById(id).orElseThrow(() -> new FitManageException("Plan not found: " + id));
    }

    public int create(Plan p) {
        SessionContext.requireRole(User.Role.ADMIN);
        Validator.requireMax("name", p.getName(), 60);
        Validator.intInRange("duration_days", String.valueOf(p.getDurationDays()), 1, 3650);
        if (planDao.nameExists(p.getName(), null))
            throw new FitManageException("A plan named '" + p.getName() + "' already exists.");
        return planDao.insert(p);
    }

    public void update(Plan p) {
        SessionContext.requireRole(User.Role.ADMIN);
        Validator.requireMax("name", p.getName(), 60);
        if (planDao.nameExists(p.getName(), p.getPlanId()))
            throw new FitManageException("Another plan named '" + p.getName() + "' already exists.");
        planDao.update(p);
    }

    public void deactivate(int planId) {
        SessionContext.requireRole(User.Role.ADMIN);
        if (planDao.countActiveSubscribers(planId) > 0)
            throw new PlanInUseException(planId);
        planDao.setActive(planId, false);
    }

    public void reactivate(int planId) {
        SessionContext.requireRole(User.Role.ADMIN);
        planDao.setActive(planId, true);
    }
}

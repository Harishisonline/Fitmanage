package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.dao.ActivityLogDao;
import com.ironpump.fitmanage.model.ActivityLog;

import java.util.List;

/** Thin wrapper around ActivityLogDao. */
public class ActivityLogService {
    private final ActivityLogDao dao = new ActivityLogDao();
    public List<ActivityLog> recent(int n) { return dao.recent(n); }
    public List<ActivityLog> forUser(int userId, int n) { return dao.forUser(userId, n); }
    public List<ActivityLog> forMember(int memberId, int n) { return dao.forMember(memberId, n); }
    public List<ActivityLog> forTrainer(int trainerId, int n) { return dao.forTrainer(trainerId, n); }
}

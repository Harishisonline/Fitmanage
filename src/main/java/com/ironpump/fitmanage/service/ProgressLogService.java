package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.ActivityLogDao;
import com.ironpump.fitmanage.dao.ProgressLogDao;
import com.ironpump.fitmanage.model.ProgressLog;

import java.util.List;

/** Thin wrapper around ProgressLogDao. The page is in ui/members/MemberProgressPage. */
public class ProgressLogService {
    private final ProgressLogDao dao = new ProgressLogDao();
    private final ActivityLogDao activity = new ActivityLogDao();

    public List<ProgressLog> listForMember(int memberId) { return dao.listForMember(memberId); }

    public int log(int memberId, ProgressLog p) {
        if (p.getLoggedBy() == null) p.setLoggedBy(SessionContext.currentUserId());
        int id = dao.insert(p);
        activity.log(SessionContext.currentUserId(), memberId, null, "PROGRESS_LOGGED",
                p.getLogDate() + " " + p.getWeightKg() + "kg");
        return id;
    }
}

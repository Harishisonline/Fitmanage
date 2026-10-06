package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.*;

import java.util.List;

/** Check-in / check-out flow and attendance queries. */
public class AttendanceService {
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final MemberDao memberDao = new MemberDao();
    private final ActivityLogDao activityLog = new ActivityLogDao();

    public int checkIn(String phoneOrCode) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        Member m = memberDao.findByPhoneOrCode(phoneOrCode.trim())
                .orElseThrow(() -> new FitManageException("No member with that phone or ID."));
        if (m.getStatus() == Member.Status.INACTIVE)
            throw new FitManageException("Member is deactivated. Reactivate before checking in.");

        new com.ironpump.fitmanage.dao.MembershipDao().findActiveForMember(m.getMemberId()).ifPresentOrElse(
                ms -> {
                    if (ms.getEndDate().isBefore(java.time.LocalDate.now()))
                        throw new FitManageException("Membership expired. Please renew.");
                    if (ms.getStatus() != Membership.Status.ACTIVE)
                        throw new FitManageException("Membership not active.");
                },
                () -> { throw new FitManageException("No active membership. Please renew."); }
        );

        Integer trainerId = SessionContext.current() != null ? SessionContext.current().getLinkedTrainerId() : null;
        int id = attendanceDao.insertCheckIn(m.getMemberId(), trainerId);
        activityLog.log(SessionContext.currentUserId(), m.getMemberId(), trainerId, "CHECKIN", phoneOrCode);
        return id;
    }

    public void checkOut(int memberId) {
        SessionContext.requireRole(User.Role.ADMIN, User.Role.STAFF);
        attendanceDao.checkOut(memberId);
        activityLog.log(SessionContext.currentUserId(), memberId, null, "CHECKOUT", null);
    }

    public List<Attendance> today()         { return attendanceDao.listToday(); }
    public List<Attendance> byDate(java.time.LocalDate d) { return attendanceDao.listByDate(d); }
    public List<Attendance> forMember(int memberId, int limit) { return attendanceDao.listForMember(memberId, limit); }
    public int countToday()                  { return attendanceDao.countToday(); }
    public int countCurrentlyInside()        { return attendanceDao.countCurrentlyInside(); }
    public int avgSessionMinutesToday()      { return attendanceDao.avgSessionMinutesToday(); }
    public java.util.List<Attendance> currentlyInside() { return attendanceDao.currentlyInside(); }
    public int countByMemberSince(int memberId, int days) { return attendanceDao.countByMemberSince(memberId, days); }
    public int countByMember(int memberId) { return attendanceDao.countByMember(memberId); }
    public java.util.Map<java.time.LocalDate, Integer> attendanceCountsByDay(int memberId, java.time.LocalDate from) {
        return attendanceDao.attendanceCountsByDay(memberId, from);
    }
}

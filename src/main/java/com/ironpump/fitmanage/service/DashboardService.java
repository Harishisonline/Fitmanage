package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.model.ActivityLog;
import com.ironpump.fitmanage.model.DayCount;
import com.ironpump.fitmanage.model.MembershipView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Aggregations for the dashboard KPI strip. */
public class DashboardService {
    private final MemberDao memberDao = new MemberDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final PaymentDao paymentDao = new PaymentDao();

    public record Kpis(long totalMembers, long activeMemberships, long expiring7, long checkInsToday,
                       long currentlyInside, BigDecimal revenueToday, BigDecimal revenueThisMonth) {}

    public record AttendanceBucket(String label, int count) {}

    public Kpis kpis() {
        return new Kpis(
                memberDao.countAll(),
                membershipDao.countActive(),
                membershipDao.expiringSoon(7).size(),
                attendanceDao.countToday(),
                attendanceDao.countCurrentlyInside(),
                paymentDao.totalToday(),
                paymentDao.totalThisMonth());
    }

    public List<MembershipView> expiringSoon7() { return membershipDao.findExpiringSoonWithMember(7); }

    public List<ActivityLog> recentActivity(int n) { return new ActivityLogDao().recent(n); }

    public List<AttendanceBucket> attendanceLast14Buckets() {
        Map<LocalDate, Integer> byDay = attendanceDao.dailyCountsLast14Days().stream()
                .collect(Collectors.toMap(DayCount::day, DayCount::count, Integer::sum));
        List<AttendanceBucket> out = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            out.add(new AttendanceBucket(label, byDay.getOrDefault(day, 0)));
        }
        return out;
    }
}

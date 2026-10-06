package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.dao.*;
import com.ironpump.fitmanage.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Aggregated queries that feed the Reports page. */
public class ReportService {
    private final MemberDao memberDao = new MemberDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final PaymentDao paymentDao = new PaymentDao();

    public record ReportRow(String label, String value) {}

    public List<ReportRow> members() {
        long active = memberDao.countByStatus("ACTIVE");
        long inactive = memberDao.countByStatus("INACTIVE");
        long expiring = membershipDao.expiringSoon(30).size();
        return List.of(
                new ReportRow("Active members", String.valueOf(active)),
                new ReportRow("Expiring (30d)", String.valueOf(expiring)),
                new ReportRow("Inactive", String.valueOf(inactive))
        );
    }

    public List<ReportRow> revenue(LocalDate from, LocalDate to) {
        BigDecimal total = paymentDao.totalThisMonth();
        return List.of(
                new ReportRow("This month", "Rs. " + total),
                new ReportRow("Today", "Rs. " + paymentDao.totalToday()),
                new ReportRow("Last 7 days", "Rs. " + paymentDao.totalLastDays(7)),
                new ReportRow("Last 30 days", "Rs. " + paymentDao.totalLastDays(30))
        );
    }

    public List<ReportRow> attendance() {
        return List.of(
                new ReportRow("Check-ins today", String.valueOf(attendanceDao.countToday())),
                new ReportRow("Currently inside", String.valueOf(attendanceDao.countCurrentlyInside()))
        );
    }

    public List<String[]> expiringRows() {
        List<Membership> ms = membershipDao.expiringSoon(7);
        List<String[]> out = new ArrayList<>();
        for (Membership m : ms) out.add(new String[]{
                String.valueOf(m.getMemberId()),
                String.valueOf(m.getEndDate()),
                String.valueOf(m.getMemberId())
        });
        return out;
    }
}

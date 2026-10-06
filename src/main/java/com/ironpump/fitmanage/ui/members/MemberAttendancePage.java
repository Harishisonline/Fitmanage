package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.Attendance;
import com.ironpump.fitmanage.service.AttendanceService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.HeatmapWrapper;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class MemberAttendancePage extends BasePage {
    private final AttendanceService service = new AttendanceService();
    private final JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
    private final HeatmapWrapper heatmap = new HeatmapWrapper();
    private final PagedTable table = new PagedTable(new String[]{"Date", "Check-in", "Check-out", "Duration", "Trainer"});
    private int memberId;

    public MemberAttendancePage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 80));
        add(kpiRow, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x2a, 0x35, 0x40)), "15-week heatmap"));
        left.setPreferredSize(new Dimension(280, 220));
        left.add(heatmap, BorderLayout.CENTER);
        body.add(left, BorderLayout.WEST);
        body.add(table, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            memberId = ctx.get("id");
            try {
                List<Attendance> recent = service.forMember(memberId, 50);
                kpiRow.removeAll();
                kpiRow.add(new KpiCard("Total visits",  String.valueOf(recent.size())));
                kpiRow.add(new KpiCard("Last 30 days",  String.valueOf(recent.size())));
                kpiRow.add(new KpiCard("Avg session",  "—"));
                kpiRow.add(new KpiCard("Streak",        "—"));
                kpiRow.revalidate();

                heatmap.setCounts(service.attendanceCountsByDay(memberId, java.time.LocalDate.now().minusDays(105L)));

                List<String[]> data = recent.stream().map(a -> {
                    String dur = a.getCheckOutAt() == null ? "in progress"
                            : java.time.Duration.between(a.getCheckInAt(), a.getCheckOutAt()).toMinutes() + "m";
                    return new String[]{
                            DateUtils.format(a.getCheckInAt()),
                            DateUtils.format(a.getCheckInAt()).contains("·") ? a.getCheckInAt().toLocalTime().toString() : a.getCheckInAt().toString(),
                            a.getCheckOutAt() == null ? "—" : a.getCheckOutAt().toString(),
                            dur,
                            a.getTrainerId() == null ? "—" : "#" + a.getTrainerId()
                    };
                }).collect(Collectors.toList());
                table.setRows(data);
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    private String memberLabel() {
        try { return new com.ironpump.fitmanage.service.MemberService().findById(memberId).getFullName(); }
        catch (Exception e) { return "Member"; }
    }

    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Members", () -> Router.get().navigateTo(Route.MEMBERS_LIST)),
                new BreadcrumbItem(memberLabel(), () -> Router.get().navigateTo(Route.MEMBER_DETAIL, RouteContext.of("id", memberId))),
                new BreadcrumbItem("Attendance", () -> {}));
    }
}

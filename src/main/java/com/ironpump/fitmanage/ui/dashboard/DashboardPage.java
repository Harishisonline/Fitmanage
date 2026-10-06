package com.ironpump.fitmanage.ui.dashboard;

import com.ironpump.fitmanage.service.DashboardService;
import com.ironpump.fitmanage.service.MembershipService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.*;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Visual: see assets/ui-mockups/02-dashboard.html. */
public class DashboardPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(DashboardPage.class);
    private final DashboardService service = new DashboardService();
    private final MembershipService memberships = new MembershipService();
    private final JLabel status = new JLabel(" ");
    private final JPanel body = new JPanel();

    public DashboardPage() {
        setLayout(new BorderLayout());
        setBackground(Ui.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        JScrollPane sp = new JScrollPane(body);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Ui.BG);
        add(sp, BorderLayout.CENTER);
        status.setForeground(Ui.AMBER);
        add(status, BorderLayout.SOUTH);
    }

    @Override public void onEnter(RouteContext ctx) { reload(); }

    private void reload() {
        body.removeAll();
        JPanel pad = Ui.page();
        pad.setLayout(new BoxLayout(pad, BoxLayout.Y_AXIS));
        try {
            JLabel hello = Ui.sub("Welcome back. Here's what's happening at your gym today.");
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.add(Ui.h1("Dashboard"), BorderLayout.NORTH);
            head.add(hello, BorderLayout.SOUTH);
            pad.add(head);
            pad.add(Box.createVerticalStrut(14));

            DashboardService.Kpis k = service.kpis();
            JPanel kpis = Ui.kpiRow(
                    new KpiCard("Total members", String.valueOf(k.totalMembers()), new TrendLabel(12, "this week", true)),
                    new KpiCard("Active memberships", String.valueOf(k.activeMemberships()), "Retention"),
                    new KpiCard("Expiring in 7 days", String.valueOf(k.expiring7()), new TrendLabel(Math.max(0, (int) k.expiring7()), "vs last week", false)),
                    new KpiCard("Check-ins today", String.valueOf(k.checkInsToday()), "Inside now " + k.currentlyInside()));
            kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            pad.add(kpis);
            pad.add(Box.createVerticalStrut(14));

            JPanel mid = new JPanel(new GridLayout(1, 2, 12, 0));
            mid.setOpaque(false);
            mid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
            List<DashboardService.AttendanceBucket> buckets = service.attendanceLast14Buckets();
            List<String> labels = new ArrayList<>();
            List<Number> counts = new ArrayList<>();
            for (DashboardService.AttendanceBucket b : buckets) {
                labels.add(b.label());
                counts.add(b.count());
            }
            Card chart = new Card("Attendance · last 14 days");
            chart.add(ChartPanelFactory.attendanceBarChart(labels, counts), BorderLayout.CENTER);
            mid.add(chart);

            JButton add = Ui.primary("+ Add member");
            add.addActionListener(e -> Router.get().navigateTo(Route.MEMBER_FORM_NEW));
            JButton check = Ui.secondary("Check-in");
            check.addActionListener(e -> Router.get().navigateTo(Route.ATTENDANCE));
            JButton pay = Ui.secondary("Record payment");
            pay.addActionListener(e -> Router.get().navigateTo(Route.PAYMENTS_LIST, RouteContext.of("record", Boolean.TRUE)));
            JButton renew = Ui.secondary("Renew plan");
            renew.addActionListener(e -> Router.get().navigateTo(Route.MEMBERS_LIST));
            JButton invoice = Ui.secondary("Generate invoice");
            invoice.addActionListener(e -> Router.get().navigateTo(Route.PAYMENTS_LIST));
            JButton report = Ui.secondary("Run report");
            report.addActionListener(e -> Router.get().navigateTo(Route.REPORTS));
            Card quick = new Card("Quick actions");
            quick.add(new QuickActions(add, check, pay, renew, invoice, report), BorderLayout.CENTER);
            mid.add(quick);
            pad.add(mid);
            pad.add(Box.createVerticalStrut(14));

            JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
            lower.setOpaque(false);
            Card expiring = new Card("Expiring memberships");
            PagedTable table = new PagedTable(new String[]{"Member", "Plan", "Expires", ""});
            List<String[]> rows = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            List<int[]> renewKeys = new ArrayList<>();
            var soon = service.expiringSoon7();
            for (var v : soon) {
                var m = v.getMembership();
                rows.add(new String[]{
                        v.getMemberName() == null ? "Member" : v.getMemberName(),
                        v.getPlanName() == null ? "Plan" : v.getPlanName(),
                        DateUtils.format(m.getEndDate())
                });
                ids.add(m.getMemberId());
                renewKeys.add(new int[]{m.getMemberId(), m.getPlanId(), m.getTrainerId() == null ? 0 : m.getTrainerId()});
            }
            table.addActionColumn(List.of(new RowAction("Renew", Ui.GREEN, row -> {
                int absolute = table.absoluteRow(row);
                if (absolute < 0 || absolute >= renewKeys.size()) return;
                int[] key = renewKeys.get(Math.min(renewKeys.size() - 1, absolute));
                try {
                    memberships.renew(key[0], key[1], key[2] == 0 ? null : key[2]);
                    SwingUtilities.invokeLater(() -> {
                        reload();
                        status.setText("Renewed. The current plan stays active and the next period is scheduled.");
                    });
                } catch (Exception ex) {
                    log.error("Renew failed", ex);
                    status.setText(ex.getMessage());
                }
            })));
            table.setRowIds(ids);
            table.setRows(rows);
            expiring.add(table, BorderLayout.CENTER);
            lower.add(expiring);

            Card activity = new Card("Recent activity");
            TimelinePanel timeline = new TimelinePanel(service.recentActivity(8));
            activity.add(new JScrollPane(timeline), BorderLayout.CENTER);
            lower.add(activity);
            lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
            pad.add(lower);
            status.setText(" ");
        } catch (Exception ex) {
            log.error("Dashboard load failed", ex);
            status.setText(ex.getMessage() == null ? "Could not load dashboard" : ex.getMessage());
        }
        body.add(pad);
        body.revalidate();
        body.repaint();
    }

    @Override public List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)));
    }
}

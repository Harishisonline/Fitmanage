package com.ironpump.fitmanage.ui.reports;

import com.ironpump.fitmanage.dao.PaymentDao;
import com.ironpump.fitmanage.service.DashboardService;
import com.ironpump.fitmanage.service.ReportService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.Card;
import com.ironpump.fitmanage.ui.components.ChartPanelFactory;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.CsvExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Visual: see assets/ui-mockups/15-reports.html. */
public class ReportsPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(ReportsPage.class);
    private final ReportService service = new ReportService();
    private final JLabel status = new JLabel(" ");
    private final JPanel charts = new JPanel(new GridLayout(2, 2, 12, 12));
    private final JTextArea summary = new JTextArea(8, 40);
    private int days = 30;

    public ReportsPage() {
        setLayout(new BorderLayout());
        setBackground(Ui.BG);
        charts.setOpaque(false);
        summary.setEditable(false);
        summary.setBackground(Ui.PANEL);
        summary.setForeground(Ui.TEXT);
        status.setForeground(Ui.AMBER);
    }

    @Override public void onEnter(RouteContext ctx) { reload(); }

    private void reload() {
        removeAll();
        JPanel page = Ui.page();
        page.setLayout(new BorderLayout(0, 12));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.h1("Reports"));
        titles.add(Ui.sub("Insights from your gym data. Export to CSV or PDF."));
        head.add(titles, BorderLayout.WEST);
        JPanel chips = Ui.row(6);
        for (int d : new int[]{7, 30, 90}) {
            JButton b = d == days ? Ui.primary(d + "d") : Ui.secondary(d + "d");
            int pick = d;
            b.addActionListener(e -> SwingUtilities.invokeLater(() -> { days = pick; reload(); }));
            chips.add(b);
        }
        JButton csv = Ui.secondary("Export members CSV");
        csv.addActionListener(e -> exportCsv());
        chips.add(csv);
        head.add(chips, BorderLayout.EAST);
        page.add(head, BorderLayout.NORTH);

        charts.removeAll();
        try {
            List<BigDecimal> revenue = new PaymentDao().dailyTotals(Math.min(days, 14));
            List<Number> revenueNumbers = new ArrayList<>(revenue);
            List<String> labels = new ArrayList<>();
            for (int i = revenueNumbers.size(); i >= 1; i--) labels.add(LocalDate.now().minusDays(i - 1).getDayOfWeek().name().substring(0, 3));
            // labels were built newest-last because dailyTotals walks oldest-first. Rebuild in the same order.
            labels.clear();
            for (int i = revenueNumbers.size() - 1; i >= 0; i--) {
                labels.add(LocalDate.now().minusDays(i).getDayOfWeek().name().substring(0, 1)
                        + LocalDate.now().minusDays(i).getDayOfWeek().name().substring(1, 3).toLowerCase());
            }
            Card rev = new Card("Revenue");
            rev.add(ChartPanelFactory.revenueLineChart(revenueNumbers, labels), BorderLayout.CENTER);
            charts.add(rev);
            List<DashboardService.AttendanceBucket> buckets = new DashboardService().attendanceLast14Buckets();
            List<String> al = new ArrayList<>();
            List<Number> ac = new ArrayList<>();
            for (var b : buckets) { al.add(b.label()); ac.add(b.count()); }
            Card att = new Card("Attendance");
            att.add(ChartPanelFactory.attendanceBarChart(al, ac), BorderLayout.CENTER);
            charts.add(att);
            Card members = new Card("Members");
            StringBuilder sb = new StringBuilder();
            for (ReportService.ReportRow r : service.members()) sb.append(r.label()).append("    ").append(r.value()).append('\n');
            for (ReportService.ReportRow r : service.revenue(LocalDate.now().minusDays(days), LocalDate.now()))
                sb.append(r.label()).append("    ").append(r.value()).append('\n');
            for (ReportService.ReportRow r : service.attendance()) sb.append(r.label()).append("    ").append(r.value()).append('\n');
            summary.setText(sb.toString());
            members.add(new JScrollPane(summary), BorderLayout.CENTER);
            charts.add(members);
            Card note = new Card("Outstanding");
            note.add(Ui.sub("Use Payments for receipt-level outstanding. Period chips reload the charts."), BorderLayout.CENTER);
            charts.add(note);
            status.setText(" ");
        } catch (Exception ex) {
            log.error("Reports failed", ex);
            status.setText(ex.getMessage());
        }
        page.add(charts, BorderLayout.CENTER);
        page.add(status, BorderLayout.SOUTH);
        add(page);
        revalidate();
        repaint();
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(CsvExporter.datedName("members")));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            var rows = new com.ironpump.fitmanage.service.MemberService().list(10_000, 0, null, null, null, "")
                    .stream().map(m -> new String[]{
                            String.valueOf(m.getMemberId()),
                            CsvExporter.text(m.getMemberCode()),
                            m.getFullName(),
                            CsvExporter.text(m.getPhone()),
                            m.getStatus() == null ? "" : m.getStatus().name()
                    }).toList();
            CsvExporter.export(fc.getSelectedFile().toPath(), new String[]{"ID", "Code", "Name", "Phone", "Status"}, rows);
            status.setText("Exported " + fc.getSelectedFile().getAbsolutePath());
        } catch (Exception ex) {
            log.error("csv", ex);
            status.setText(ex.getMessage());
        }
    }

    @Override public List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Reports", () -> Router.get().navigateTo(Route.REPORTS)));
    }
}

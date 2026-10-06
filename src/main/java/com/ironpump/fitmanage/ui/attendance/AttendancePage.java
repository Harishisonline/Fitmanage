package com.ironpump.fitmanage.ui.attendance;

import com.ironpump.fitmanage.model.Attendance;
import com.ironpump.fitmanage.model.Member;
import com.ironpump.fitmanage.service.AttendanceService;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.CsvExporter;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class AttendancePage extends BasePage {
    private final AttendanceService attendanceService = new AttendanceService();
    private final MemberService memberService = new MemberService();
    private final JTextField input = new JTextField(20);
    private final JLabel heroName = new JLabel(" ");
    private final JLabel heroStatus = new JLabel(" ");
    private final JPanel kpiRow = new JPanel(new GridLayout(1, 3, 12, 0));
    private final PagedTable table = new PagedTable(new String[]{"Member", "In", "Out", "Duration"});
    private List<Attendance> shown = List.of();

    public AttendancePage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Hero / check-in form
        JPanel hero = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        hero.setBackground(new Color(0x1a, 0x20, 0x27));
        hero.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel lbl = new JLabel("Scan or type member phone / code:");
        lbl.setForeground(new Color(0x8b, 0x97, 0xa4));
        hero.add(lbl);
        input.setPreferredSize(new Dimension(220, 32));
        input.addActionListener(e -> checkIn());
        hero.add(input);
        JButton go = new JButton("Check in");
        go.setBackground(new Color(0x22, 0xC5, 0x5E));
        go.setForeground(new Color(0x06, 0x2b, 0x15));
        go.setOpaque(true);
        go.addActionListener(e -> checkIn());
        hero.add(go);
        JButton csv = new JButton("Export CSV");
        csv.addActionListener(e -> exportCsv());
        hero.add(csv);
        hero.add(Box.createHorizontalStrut(20));
        hero.add(heroName);
        hero.add(heroStatus);
        add(hero, BorderLayout.NORTH);

        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 80));
        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(hero);
        north.add(Box.createVerticalStrut(8));
        north.add(kpiRow);
        add(north, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) { refresh(); }

    private void checkIn() {
        try {
            int id = attendanceService.checkIn(input.getText());
            Member m = memberService.findByPhoneOrCode(input.getText().trim());
            heroName.setText("Checked in: " + (m == null ? "#" + id : m.getFullName()));
            heroStatus.setText(" ");
            input.setText("");
            refresh();
        } catch (Exception e) {
            heroStatus.setText(e.getMessage());
        }
    }

    private void refresh() {
        try {
            kpiRow.removeAll();
            kpiRow.add(new KpiCard("Check-ins today", String.valueOf(attendanceService.countToday())));
            kpiRow.add(new KpiCard("Currently inside", String.valueOf(attendanceService.countCurrentlyInside())));
            kpiRow.add(new KpiCard("Average session", attendanceService.avgSessionMinutesToday() + " min"));
            kpiRow.revalidate();
            List<Attendance> rows = attendanceService.today();
            shown = rows;
            List<String[]> data = rows.stream().map(a -> {
                String dur = a.getCheckOutAt() == null ? "in progress"
                        : java.time.Duration.between(a.getCheckInAt(), a.getCheckOutAt()).toMinutes() + "m";
                String name = a.getMemberName() == null ? ("#" + a.getMemberId()) : a.getMemberName();
                return new String[]{
                        name,
                        DateUtils.format(a.getCheckInAt()),
                        a.getCheckOutAt() == null ? "—" : DateUtils.format(a.getCheckOutAt()),
                        dur
                };
            }).collect(Collectors.toList());
            table.setRows(data);
        } catch (Exception e) { GlobalExceptionHandler.handle(e); }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(CsvExporter.datedName("attendance")));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        List<String[]> rows = new java.util.ArrayList<>();
        for (Attendance a : shown) {
            String dur = a.getCheckOutAt() == null ? ""
                    : String.valueOf(java.time.Duration.between(a.getCheckInAt(), a.getCheckOutAt()).toMinutes());
            rows.add(new String[]{
                    a.getMemberName() == null ? String.valueOf(a.getMemberId()) : a.getMemberName(),
                    a.getCheckInAt() == null ? "" : a.getCheckInAt().toString(),
                    a.getCheckOutAt() == null ? "" : a.getCheckOutAt().toString(),
                    dur
            });
        }
        CsvExporter.export(fc.getSelectedFile().toPath(), new String[]{"member", "in", "out", "minutes"}, rows);
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Attendance", () -> Router.get().navigateTo(Route.ATTENDANCE)));
    }
}

package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.ProgressLog;
import com.ironpump.fitmanage.service.ProgressLogService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.ChartPanelFactory;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MemberProgressPage extends BasePage {
    private final ProgressLogService service = new ProgressLogService();
    private final JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
    private final JPanel chartHolder = new JPanel(new BorderLayout());
    private final PagedTable table = new PagedTable(new String[]{"Date", "Weight", "Chest", "Waist", "Body fat", "Logged by"});
    private int memberId;

    public MemberProgressPage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h = new JLabel("Progress");
        h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
        h.setForeground(new Color(0xe6, 0xed, 0xf3));
        head.add(h, BorderLayout.WEST);
        JButton log = new JButton("+ Log progress");
        log.setBackground(new Color(0x22, 0xC5, 0x5E));
        log.setForeground(new Color(0x06, 0x2b, 0x15));
        log.setOpaque(true);
        log.addActionListener(e -> logProgress());
        head.add(log, BorderLayout.EAST);
        kpiRow.setOpaque(false);
        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(kpiRow, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        chartHolder.setOpaque(false);
        chartHolder.setPreferredSize(new Dimension(400, 220));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(chartHolder, BorderLayout.NORTH);
        body.add(table, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private void logProgress() {
        if (memberId <= 0) return;
        JTextField weight = new JTextField();
        JTextField chest = new JTextField();
        JTextField waist = new JTextField();
        JTextField fat = new JTextField();
        JTextField notes = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Weight (kg)")); form.add(weight);
        form.add(new JLabel("Chest (cm)")); form.add(chest);
        form.add(new JLabel("Waist (cm)")); form.add(waist);
        form.add(new JLabel("Body fat %")); form.add(fat);
        form.add(new JLabel("Notes")); form.add(notes);
        int ok = JOptionPane.showConfirmDialog(this, form, "Log progress", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            ProgressLog p = new ProgressLog();
            p.setMemberId(memberId);
            p.setLogDate(LocalDate.now());
            if (!weight.getText().isBlank()) p.setWeightKg(new BigDecimal(weight.getText().trim()));
            if (!chest.getText().isBlank()) p.setChestCm(Integer.parseInt(chest.getText().trim()));
            if (!waist.getText().isBlank()) p.setWaistCm(Integer.parseInt(waist.getText().trim()));
            if (!fat.getText().isBlank()) p.setBodyFatPct(new BigDecimal(fat.getText().trim()));
            p.setNotes(notes.getText().isBlank() ? null : notes.getText().trim());
            if (p.getWeightKg() == null && p.getChestCm() == null && p.getWaistCm() == null && p.getBodyFatPct() == null)
                throw new IllegalArgumentException("Enter at least one measurement.");
            service.log(memberId, p);
            SwingUtilities.invokeLater(this::reload);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Could not save the measurement." : ex.getMessage(),
                    "Log progress", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            memberId = ctx.get("id");
            reload();
        }
    }

    private void reload() {
        try {
            List<ProgressLog> rows = service.listForMember(memberId);
            kpiRow.removeAll();
            kpiRow.add(new KpiCard("Current weight", rows.isEmpty() || rows.get(0).getWeightKg() == null ? "—" : rows.get(0).getWeightKg() + " kg"));
            kpiRow.add(new KpiCard("Body fat", rows.isEmpty() || rows.get(0).getBodyFatPct() == null ? "—" : rows.get(0).getBodyFatPct() + "%"));
            kpiRow.add(new KpiCard("Entries", String.valueOf(rows.size())));
            kpiRow.add(new KpiCard("Last logged", rows.isEmpty() ? "—" : DateUtils.format(rows.get(0).getLogDate())));
            kpiRow.revalidate();

            List<Number> weights = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int i = rows.size() - 1; i >= 0; i--) {
                ProgressLog p = rows.get(i);
                if (p.getWeightKg() == null) continue;
                weights.add(p.getWeightKg());
                labels.add(DateUtils.format(p.getLogDate()));
            }
            chartHolder.removeAll();
            chartHolder.add(ChartPanelFactory.weightLineChart(
                    weights.isEmpty() ? List.of(0) : weights,
                    labels.isEmpty() ? List.of("—") : labels), BorderLayout.CENTER);
            chartHolder.revalidate();

            List<String[]> data = new ArrayList<>();
            for (ProgressLog p : rows) {
                data.add(new String[]{
                        DateUtils.format(p.getLogDate()),
                        p.getWeightKg() == null ? "—" : p.getWeightKg().toString() + " kg",
                        p.getChestCm() == null ? "—" : p.getChestCm() + " cm",
                        p.getWaistCm() == null ? "—" : p.getWaistCm() + " cm",
                        p.getBodyFatPct() == null ? "—" : p.getBodyFatPct() + "%",
                        p.getLoggedBy() == null ? "self" : "#" + p.getLoggedBy()
                });
            }
            table.setRows(data);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage() == null ? "Could not load progress." : e.getMessage(),
                    "Progress", JOptionPane.WARNING_MESSAGE);
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
                new BreadcrumbItem("Progress", () -> {}));
    }
}

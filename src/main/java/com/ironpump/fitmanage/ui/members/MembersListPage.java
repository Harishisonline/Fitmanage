package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.MemberListRow;
import com.ironpump.fitmanage.model.Plan;
import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.service.PlanService;
import com.ironpump.fitmanage.service.TrainerService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.*;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.CsvExporter;
import com.ironpump.fitmanage.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Visual: see assets/ui-mockups/03-members.html. */
public class MembersListPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(MembersListPage.class);
    private final MemberService members = new MemberService();
    private final PlanService plans = new PlanService();
    private final TrainerService trainers = new TrainerService();
    private final JLabel status = new JLabel(" ");
    private final JPanel body = new JPanel(new BorderLayout());
    private String pendingQuery = "";
    private List<MemberListRow> shown = List.of();

    public MembersListPage() {
        setLayout(new BorderLayout());
        setBackground(Ui.BG);
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
        status.setForeground(Ui.AMBER);
        add(status, BorderLayout.SOUTH);
    }

    @Override public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("q")) pendingQuery = String.valueOf(ctx.get("q"));
        reload();
    }

    private void reload() {
        body.removeAll();
        JPanel page = Ui.page();
        page.setLayout(new BorderLayout(0, 12));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.h1("Members"));
        titles.add(Ui.sub("All registered gym members"));
        head.add(titles, BorderLayout.WEST);
        page.add(head, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        try {
            List<MemberListRow> all = members.listDerived(null, null, "", null, null, 500, 0);
            long active = all.stream().filter(r -> "ACTIVE".equals(r.getDerivedStatus())).count();
            long expiring = all.stream().filter(r -> "EXPIRING".equals(r.getDerivedStatus())).count();
            long expired = all.stream().filter(r -> "EXPIRED".equals(r.getDerivedStatus())).count();
            JPanel kpis = Ui.kpiRow(
                    new KpiCard("Total members", String.valueOf(all.size())),
                    new KpiCard("Active", String.valueOf(active), null, Ui.GREEN),
                    new KpiCard("Expiring soon", String.valueOf(expiring), null, Ui.AMBER),
                    new KpiCard("Expired", String.valueOf(expired), null, Ui.RED));
            kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            center.add(kpis);
            center.add(Box.createVerticalStrut(10));

            List<Plan> planList = plans.list();
            List<Trainer> trainerList = trainers.list();
            String[] planNames = new String[planList.size() + 1];
            planNames[0] = "All plans";
            for (int i = 0; i < planList.size(); i++) planNames[i + 1] = planList.get(i).getName();
            String[] trainerNames = new String[trainerList.size() + 1];
            trainerNames[0] = "All trainers";
            for (int i = 0; i < trainerList.size(); i++) trainerNames[i + 1] = trainerList.get(i).getName();

            FilterBar filters = new FilterBar();
            JComboBox<String> statusFilter = filters.addFilter("", new String[]{"All", "ACTIVE", "EXPIRING", "UPCOMING", "EXPIRED", "INACTIVE"});
            JComboBox<String> planFilter = filters.addFilter("", planNames);
            JComboBox<String> trainerFilter = filters.addFilter("", trainerNames);
            JComboBox<String> joinedFilter = filters.addFilter("", new String[]{"Joined · Any time", "Last 30 days", "Last 90 days", "This year"});
            JTextField search = new JTextField(pendingQuery, 18);
            Ui.styleField(search);
            search.setToolTipText("Search by name, phone, or ID");
            filters.addControl(search);
            center.add(filters);

            PagedTable table = new PagedTable(new String[]{"Member", "Phone", "Plan", "Trainer", "Joined", "Expires", "Status"});
            table.addCheckboxColumn();
            Runnable[] refresh = new Runnable[1];
            refresh[0] = () -> fill(table, statusFilter, planFilter, trainerFilter, joinedFilter, search, planList, trainerList);
            for (JComboBox<?> box : new JComboBox<?>[]{statusFilter, planFilter, trainerFilter, joinedFilter}) {
                box.addActionListener(e -> refresh[0].run());
            }
            search.addActionListener(e -> refresh[0].run());
            search.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { refresh[0].run(); }
                public void removeUpdate(DocumentEvent e) { refresh[0].run(); }
                public void changedUpdate(DocumentEvent e) { refresh[0].run(); }
            });
            table.addActionColumn(List.of(
                    new RowAction("View", Ui.TEXT, row -> open(table, row, Route.MEMBER_DETAIL)),
                    new RowAction("Edit", Ui.GREEN, row -> open(table, row, Route.MEMBER_FORM_EDIT)),
                    new RowAction("Delete", Ui.RED, row -> deactivate(table, row))
            ));
            refresh[0].run();
            center.add(Box.createVerticalStrut(8));
            center.add(table);

            JPanel bulk = Ui.row(8);
            JButton export = Ui.secondary("Export CSV");
            export.addActionListener(e -> exportCsv(table));
            JLabel hint = Ui.muted("Select rows with the checkbox, then export.");
            bulk.add(hint);
            bulk.add(export);
            center.add(Box.createVerticalStrut(8));
            center.add(bulk);
            if (pendingQuery == null || pendingQuery.isBlank()) status.setText(" ");
        } catch (Exception ex) {
            log.error("Members list failed", ex);
            status.setText(ex.getMessage());
        }
        page.add(center, BorderLayout.CENTER);
        body.add(page);
        body.revalidate();
        body.repaint();
    }

    private void fill(PagedTable table, JComboBox<String> statusFilter, JComboBox<String> planFilter,
                      JComboBox<String> trainerFilter, JComboBox<String> joinedFilter, JTextField search,
                      List<Plan> planList, List<Trainer> trainerList) {
        Integer planId = planFilter.getSelectedIndex() <= 0 ? null : planList.get(planFilter.getSelectedIndex() - 1).getPlanId();
        Integer trainerId = trainerFilter.getSelectedIndex() <= 0 ? null : trainerList.get(trainerFilter.getSelectedIndex() - 1).getTrainerId();
        String joined = switch (joinedFilter.getSelectedIndex()) {
            case 1 -> "30";
            case 2 -> "90";
            case 3 -> "year";
            default -> null;
        };
        String st = String.valueOf(statusFilter.getSelectedItem());
        pendingQuery = search.getText();
        List<MemberListRow> rows = members.listDerived(planId, trainerId, search.getText(), st, joined, 500, 0);
        shown = rows;
        List<String[]> data = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (MemberListRow r : rows) {
            var m = r.getMember();
            data.add(new String[]{
                    m.getFullName() + "  " + m.getMemberCode(),
                    m.getPhone(),
                    r.getPlanName() == null ? "—" : r.getPlanName(),
                    r.getTrainerName() == null ? "—" : r.getTrainerName(),
                    DateUtils.format(m.getJoinedAt()),
                    r.getExpiresOn() == null ? "—" : DateUtils.format(r.getExpiresOn()),
                    r.getDerivedStatus()
            });
            ids.add(m.getMemberId());
        }
        table.setRowIds(ids);
        table.setRows(data);
        if (pendingQuery != null && !pendingQuery.isBlank())
            status.setText(rows.size() + " match" + (rows.size() == 1 ? "" : "es") + " for \"" + pendingQuery.trim() + "\"");
        if (table.table().getColumnCount() > 2) {
            table.table().getColumnModel().getColumn(1).setPreferredWidth(220);
            table.table().getColumnModel().getColumn(2).setPreferredWidth(140);
        }
    }

    private void open(PagedTable table, int row, Route route) {
        int id = table.idAt(row);
        if (id > 0) Router.get().navigateTo(route, RouteContext.of("id", id));
    }

    private void deactivate(PagedTable table, int row) {
        int id = table.idAt(row);
        if (id <= 0) return;
        int ok = JOptionPane.showConfirmDialog(this, "Deactivate this member?", "Delete", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            members.deactivate(id);
            SwingUtilities.invokeLater(() -> {
                reload();
                status.setText("Member deactivated. They stay in this list with status Inactive.");
            });
        } catch (Exception ex) {
            log.error("Deactivate failed", ex);
            status.setText(ex.getMessage());
        }
    }

    private void exportCsv(PagedTable table) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(CsvExporter.datedName("members")));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        List<Integer> checked = table.checkedIds();
        List<String[]> rows = new ArrayList<>();
        for (MemberListRow r : shown) {
            var m = r.getMember();
            if (!checked.isEmpty() && !checked.contains(m.getMemberId())) continue;
            rows.add(new String[]{
                    m.getFullName() + "  " + m.getMemberCode(),
                    CsvExporter.text(m.getPhone()),
                    r.getPlanName() == null ? "" : r.getPlanName(),
                    r.getTrainerName() == null ? "" : r.getTrainerName(),
                    CsvExporter.date(m.getJoinedAt()),
                    CsvExporter.date(r.getExpiresOn()),
                    r.getDerivedStatus() == null ? "" : r.getDerivedStatus()
            });
        }
        CsvExporter.export(fc.getSelectedFile().toPath(),
                new String[]{"Member", "Phone", "Plan", "Trainer", "Joined", "Expires", "Status"}, rows);
        status.setText("Exported " + fc.getSelectedFile().getAbsolutePath());
    }

    @Override public List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Members", () -> Router.get().navigateTo(Route.MEMBERS_LIST)));
    }
}

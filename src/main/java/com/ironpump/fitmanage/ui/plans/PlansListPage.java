package com.ironpump.fitmanage.ui.plans;

import com.ironpump.fitmanage.model.Plan;
import com.ironpump.fitmanage.service.PlanService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.components.RowAction;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.CurrencyUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlansListPage extends BasePage {
    private final PlanService service = new PlanService();
    private final PagedTable table = new PagedTable(new String[]{
            "Plan", "Duration", "Price", "Description", "Active subs", "Status"});
    private final JTextField search = new JTextField(18);
    private final JLabel subtitle = Ui.sub(" ");
    private final JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 0));
    private List<Plan> loaded = List.of();

    public PlansListPage() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.h1("Membership plans"));
        titles.add(subtitle);
        head.add(titles, BorderLayout.WEST);
        JPanel actions = Ui.row(8);
        Ui.styleField(search);
        search.setToolTipText("Search plans");
        search.addActionListener(e -> render(search.getText().trim().toLowerCase(Locale.ROOT)));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { render(search.getText().trim().toLowerCase(Locale.ROOT)); }
            public void removeUpdate(DocumentEvent e) { render(search.getText().trim().toLowerCase(Locale.ROOT)); }
            public void changedUpdate(DocumentEvent e) { render(search.getText().trim().toLowerCase(Locale.ROOT)); }
        });
        actions.add(search);
        JButton add = Ui.primary("+ Add plan");
        add.addActionListener(e -> Router.get().navigateTo(Route.PLAN_FORM_NEW));
        actions.add(add);
        head.add(actions, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        kpis.setOpaque(false);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(kpis, BorderLayout.NORTH);
        table.addActionColumn(List.of(
                new RowAction("Edit", Ui.SECONDARY, this::edit),
                new RowAction("Deactivate", Ui.RED, this::toggle)
        ));
        body.add(table, BorderLayout.CENTER);
        JLabel hint = Ui.muted("Soft delete only. Plans with active subscriptions are deactivated only when no member is still on them.");
        body.add(hint, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        try {
            loaded = service.list();
            int active = 0;
            int days = 0;
            BigDecimal price = BigDecimal.ZERO;
            int subs = 0;
            for (Plan p : loaded) {
                int n = service.activeSubscribers(p.getPlanId());
                subs += n;
                if (p.isActive()) {
                    active++;
                    days += p.getDurationDays();
                    if (p.getPrice() != null) price = price.add(p.getPrice());
                }
            }
            int avgDays = active == 0 ? 0 : days / active;
            BigDecimal avgPrice = active == 0 ? BigDecimal.ZERO : price.divide(BigDecimal.valueOf(active), 0, RoundingMode.HALF_UP);
            kpis.removeAll();
            kpis.add(new KpiCard("Active plans", String.valueOf(active)));
            kpis.add(new KpiCard("Avg duration", avgDays + " days"));
            kpis.add(new KpiCard("Avg price", CurrencyUtils.format(avgPrice, "INR")));
            kpis.add(new KpiCard("Members on plan", String.valueOf(subs), "Active subscriptions"));
            subtitle.setText("The catalogue of plans members can buy · " + active + " active, " + (loaded.size() - active) + " inactive");
            render(search.getText().trim().toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    private void render(String q) {
        List<String[]> rows = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (Plan p : loaded) {
            String blob = (p.getName() + " " + (p.getDescription() == null ? "" : p.getDescription())).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !blob.contains(q)) continue;
            int n = service.activeSubscribers(p.getPlanId());
            rows.add(new String[]{
                    p.getName() + "  ·  PLN-" + p.getPlanId(),
                    p.getDurationDays() + " days",
                    p.getPrice() == null ? "—" : CurrencyUtils.format(p.getPrice(), "INR"),
                    p.getDescription() == null || p.getDescription().isBlank() ? "—" : p.getDescription(),
                    String.valueOf(n),
                    p.isActive() ? "Active" : "Inactive"
            });
            ids.add(p.getPlanId());
        }
        table.setRowIds(ids);
        table.setRows(rows);
    }

    private void edit(int row) {
        int id = table.idAt(row);
        if (id > 0) Router.get().navigateTo(Route.PLAN_FORM_EDIT, RouteContext.of("id", id));
    }

    private void toggle(int row) {
        int id = table.idAt(row);
        if (id <= 0) return;
        try {
            Plan p = service.findById(id);
            if (p.isActive()) service.deactivate(id);
            else service.reactivate(id);
            onEnter(RouteContext.empty());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Plan", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Plans", () -> Router.get().navigateTo(Route.PLANS_LIST)));
    }
}

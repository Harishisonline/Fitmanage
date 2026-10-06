package com.ironpump.fitmanage.ui.trainers;

import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.service.MembershipService;
import com.ironpump.fitmanage.service.TrainerService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.AvatarLabel;
import com.ironpump.fitmanage.ui.components.Card;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.StatusBadge;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Visual: see assets/ui-mockups/09-trainers-list.html. */
public class TrainersListPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(TrainersListPage.class);
    private final TrainerService service = new TrainerService();
    private final MembershipService memberships = new MembershipService();
    private final JLabel status = new JLabel(" ");

    public TrainersListPage() {
        setLayout(new BorderLayout());
        setBackground(Ui.BG);
        status.setForeground(Ui.AMBER);
    }

    @Override public void onEnter(RouteContext ctx) { reload(); }

    private void reload() {
        removeAll();
        JPanel page = Ui.page();
        page.setLayout(new BorderLayout(0, 12));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Ui.h1("Trainers"), BorderLayout.WEST);
        JButton add = Ui.primary("+ Add trainer");
        add.addActionListener(e -> Router.get().navigateTo(Route.TRAINER_FORM_NEW));
        head.add(add, BorderLayout.EAST);
        page.add(head, BorderLayout.NORTH);
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        try {
            List<Trainer> rows = service.list();
            long active = rows.stream().filter(Trainer::isActive).count();
            center.add(Ui.kpiRow(
                    new KpiCard("Total trainers", String.valueOf(rows.size())),
                    new KpiCard("Active", String.valueOf(active), null, Ui.GREEN),
                    new KpiCard("Avg clients", rows.isEmpty() ? "—" : "—"),
                    new KpiCard("Monthly payroll", "Rs. " + rows.stream().map(t -> t.getSalary() == null ? java.math.BigDecimal.ZERO : t.getSalary()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))));
            center.add(Box.createVerticalStrut(12));
            JPanel grid = new JPanel(new GridLayout(0, 3, 12, 12));
            grid.setOpaque(false);
            for (Trainer t : rows) {
                Card card = new Card();
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.add(new AvatarLabel(t.getName(), 48), BorderLayout.WEST);
                JPanel text = new JPanel();
                text.setOpaque(false);
                text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
                JLabel name = new JLabel(t.getName());
                name.setForeground(Ui.TEXT);
                name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
                text.add(name);
                text.add(Ui.muted("Joined " + DateUtils.format(t.getJoinDate())));
                text.add(StatusBadge.forMemberStatus(t.isActive() ? "ACTIVE" : "INACTIVE"));
                row.add(text, BorderLayout.CENTER);
                JPanel wrap = new JPanel(new BorderLayout(0, 8));
                wrap.setOpaque(false);
                wrap.add(row, BorderLayout.NORTH);
                String spec = t.getSpecialization() == null ? "" : t.getSpecialization();
                wrap.add(Ui.sub(spec), BorderLayout.CENTER);
                int clients = memberships.viewsForTrainer(t.getTrainerId()).size();
                wrap.add(Ui.muted(clients + " active clients · Rs. " + t.getSalary()), BorderLayout.SOUTH);
                JButton view = Ui.primary("View profile");
                view.addActionListener(e -> Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", t.getTrainerId())));
                JButton toggle = Ui.secondary(t.isActive() ? "Deactivate" : "Activate");
                int trainerId = t.getTrainerId();
                boolean activeNow = t.isActive();
                toggle.addActionListener(e -> {
                    try {
                        service.setActive(trainerId, !activeNow);
                        SwingUtilities.invokeLater(this::reload);
                    } catch (Exception ex) {
                        log.error("Trainer status change failed", ex);
                        status.setText(ex.getMessage());
                    }
                });
                JPanel btns = Ui.row(6);
                btns.add(view);
                btns.add(toggle);
                JPanel outer = new JPanel(new BorderLayout());
                outer.setOpaque(false);
                outer.add(wrap, BorderLayout.CENTER);
                outer.add(btns, BorderLayout.SOUTH);
                card.add(outer, BorderLayout.CENTER);
                grid.add(card);
            }
            center.add(grid);
            status.setText("Showing 1–" + rows.size() + " of " + rows.size());
        } catch (Exception ex) {
            log.error("Trainers list failed", ex);
            status.setText(ex.getMessage());
        }
        page.add(new JScrollPane(center), BorderLayout.CENTER);
        page.add(status, BorderLayout.SOUTH);
        add(page);
        revalidate();
        repaint();
    }

    @Override public List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Trainers", () -> Router.get().navigateTo(Route.TRAINERS_LIST)));
    }
}

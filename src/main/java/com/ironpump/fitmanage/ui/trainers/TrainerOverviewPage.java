package com.ironpump.fitmanage.ui.trainers;

import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.service.MembershipService;
import com.ironpump.fitmanage.service.TrainerPaymentService;
import com.ironpump.fitmanage.service.TrainerService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.AvatarLabel;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class TrainerOverviewPage extends BasePage {
    private final TrainerService trainerService = new TrainerService();
    private final MembershipService membershipService = new MembershipService();
    private final TrainerPaymentService trainerPaymentService = new TrainerPaymentService();
    private int trainerId;
    private final JPanel avatarSlot = new JPanel(new BorderLayout());
    private final JLabel heroName = new JLabel(" ");
    private final JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
    private final JTextArea personal = new JTextArea(8, 60);
    private final PagedTable members = new PagedTable(new String[]{"Member", "Plan", "Start", "End", "Status"});
    private final JButton activeToggle = new JButton("Deactivate");

    public TrainerOverviewPage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Hero
        JPanel hero = new JPanel(new BorderLayout(12, 0));
        hero.setBackground(new Color(0x1a, 0x20, 0x27));
        hero.setBorder(new EmptyBorder(16, 16, 16, 16));
        avatarSlot.setOpaque(false);
        avatarSlot.setPreferredSize(new Dimension(72, 72));
        hero.add(avatarSlot, BorderLayout.WEST);
        JPanel hC = new JPanel();
        hC.setOpaque(false);
        hC.setLayout(new BoxLayout(hC, BoxLayout.Y_AXIS));
        heroName.setFont(heroName.getFont().deriveFont(Font.BOLD, 18f));
        heroName.setForeground(new Color(0xe6, 0xed, 0xf3));
        hC.add(heroName);
        hero.add(hC, BorderLayout.CENTER);
        JButton edit = new JButton("Edit");
        edit.addActionListener(e -> Router.get().navigateTo(Route.TRAINER_FORM_EDIT, RouteContext.of("id", trainerId)));
        activeToggle.addActionListener(e -> toggleActive());
        JPanel heroBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        heroBtns.setOpaque(false);
        heroBtns.add(activeToggle);
        heroBtns.add(edit);
        hero.add(heroBtns, BorderLayout.EAST);
        add(hero, BorderLayout.NORTH);

        // Tab bar
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setOpaque(false);
        addSubTab(tabs, "Overview",   true,  () -> {});
        addSubTab(tabs, "Personal",   false, () -> Router.get().navigateTo(Route.TRAINER_PERSONAL, RouteContext.of("id", trainerId)));
        addSubTab(tabs, "Members",    false, () -> Router.get().navigateTo(Route.TRAINER_MEMBERS,  RouteContext.of("id", trainerId)));
        addSubTab(tabs, "Salary",     false, () -> Router.get().navigateTo(Route.TRAINER_SALARY,   RouteContext.of("id", trainerId)));
        addSubTab(tabs, "Activity",   false, () -> Router.get().navigateTo(Route.TRAINER_ACTIVITY, RouteContext.of("id", trainerId)));
        JPanel tabsWrap = new JPanel();
        tabsWrap.setOpaque(false);
        tabsWrap.setLayout(new BoxLayout(tabsWrap, BoxLayout.Y_AXIS));
        tabsWrap.add(Box.createVerticalStrut(8));
        tabsWrap.add(tabs);
        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(hero);
        north.add(tabsWrap);
        add(north, BorderLayout.NORTH);

        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 90));
        add(kpiRow, BorderLayout.CENTER);

        JPanel body = new JPanel(new GridLayout(1, 2, 12, 0));
        body.setOpaque(false);
        JScrollPane sp1 = new JScrollPane(personal);
        sp1.setBorder(BorderFactory.createTitledBorder("Profile snapshot"));
        personal.setEditable(false); personal.setOpaque(false);
        JScrollPane sp2 = new JScrollPane(members);
        sp2.setBorder(BorderFactory.createTitledBorder("Assigned members"));
        body.add(sp1);
        body.add(sp2);
        add(body, BorderLayout.SOUTH);
    }

    private void addSubTab(JPanel bar, String label, boolean active, Runnable onClick) {
        JButton b = new JButton(label);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setForeground(active ? new Color(0x22, 0xC5, 0x5E) : new Color(0x8b, 0x97, 0xa4));
        b.setFont(b.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 13f));
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!active) b.addActionListener(e -> onClick.run());
        bar.add(b);
        JLabel sep = new JLabel("|");
        sep.setForeground(new Color(0x2a, 0x35, 0x40));
        bar.add(sep);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            trainerId = ctx.get("id");
            try {
                Trainer t = trainerService.findById(trainerId);
                heroName.setText(t.getName());
                activeToggle.setText(t.isActive() ? "Deactivate" : "Activate");
                avatarSlot.removeAll();
                avatarSlot.add(new AvatarLabel(t.getName(), 72));
                avatarSlot.revalidate();
                kpiRow.removeAll();
                List<com.ironpump.fitmanage.model.MembershipView> assigned = membershipService.viewsForTrainer(trainerId);
                kpiRow.add(new KpiCard("Assigned members", String.valueOf(assigned.size())));
                kpiRow.add(new KpiCard("Active plans", String.valueOf(assigned.stream().filter(v -> v.getMembership().getStatus() == com.ironpump.fitmanage.model.Membership.Status.ACTIVE).count())));
                kpiRow.add(new KpiCard("This month paid",  "Rs. " + trainerPaymentService.sumMonth(trainerId, java.time.LocalDate.now().getYear(), java.time.LocalDate.now().getMonthValue())));
                kpiRow.add(new KpiCard("This year paid",   "Rs. " + trainerPaymentService.sumYear(trainerId, java.time.LocalDate.now().getYear())));
                kpiRow.revalidate();

                personal.setText(
                        "Name:           " + t.getName() + "\n" +
                        "Specialization: " + (t.getSpecialization() == null ? "—" : t.getSpecialization()) + "\n" +
                        "Phone:          " + (t.getPhone() == null ? "—" : t.getPhone()) + "\n" +
                        "Email:          " + (t.getEmail() == null ? "—" : t.getEmail()) + "\n" +
                        "Join date:      " + t.getJoinDate() + "\n" +
                        "Salary:         Rs. " + t.getSalary() + "\n" +
                        "Active:         " + t.isActive() + "\n" +
                        "Availability:   " + (t.getAvailability() == null ? "—" : t.getAvailability())
                );

                List<String[]> data = assigned.stream().map(v -> new String[]{
                        v.getMemberName() == null ? "Member" : v.getMemberName(),
                        v.getPlanName() == null ? "Plan" : v.getPlanName(),
                        v.getMembership().getStartDate().toString(),
                        v.getMembership().getEndDate().toString(),
                        v.getMembership().displayStatus()
                }).collect(Collectors.toList());
                members.setRows(data);
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    private void toggleActive() {
        if (trainerId <= 0) return;
        try {
            Trainer t = trainerService.findById(trainerId);
            trainerService.setActive(trainerId, !t.isActive());
            onEnter(RouteContext.of("id", trainerId));
        } catch (Exception e) { GlobalExceptionHandler.handle(e); }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Trainers", () -> Router.get().navigateTo(Route.TRAINERS_LIST)),
                new BreadcrumbItem("#" + trainerId, () -> Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", trainerId))));
    }
}

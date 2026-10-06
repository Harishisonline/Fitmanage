package com.ironpump.fitmanage.ui.users;

import com.ironpump.fitmanage.model.User;
import com.ironpump.fitmanage.service.UserService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.components.RowAction;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersListPage extends BasePage {
    private final UserService service = new UserService();
    private final PagedTable table = new PagedTable(new String[]{
            "User", "Username", "Role", "Last login", "Status", "Created"});
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> roleFilter = new JComboBox<>(new String[]{"All roles", "ADMIN", "STAFF", "TRAINER"});
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All status", "Active", "Inactive"});
    private final JLabel subtitle = Ui.sub(" ");
    private final JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 0));
    private List<User> loaded = List.of();

    public UsersListPage() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.h1("Users"));
        titles.add(subtitle);
        head.add(titles, BorderLayout.WEST);
        JPanel actions = Ui.row(8);
        Ui.styleField(search);
        search.addActionListener(e -> render());
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { render(); }
            public void removeUpdate(DocumentEvent e) { render(); }
            public void changedUpdate(DocumentEvent e) { render(); }
        });
        search.setToolTipText("Search name or username");
        actions.add(search);
        JButton add = Ui.primary("+ Add user");
        add.addActionListener(e -> Router.get().navigateTo(Route.USER_FORM_NEW));
        actions.add(add);
        head.add(actions, BorderLayout.EAST);

        kpis.setOpaque(false);
        JPanel filters = Ui.row(8);
        filters.add(roleFilter);
        filters.add(statusFilter);
        roleFilter.addActionListener(e -> render());
        statusFilter.addActionListener(e -> render());

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(kpis, BorderLayout.CENTER);
        north.add(filters, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        table.addActionColumn(List.of(
                new RowAction("Edit", Ui.SECONDARY, this::edit),
                new RowAction("Reset PW", Ui.BLUE, this::resetPw),
                new RowAction("Deactivate", Ui.RED, this::toggle)
        ));
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(table, BorderLayout.CENTER);
        body.add(Ui.muted("Admins manage plans, trainers, and other users. Staff handle day-to-day operations. Trainers see their own roster."), BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        try {
            loaded = service.list();
            long admins = loaded.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();
            long staff = loaded.stream().filter(u -> u.getRole() == User.Role.STAFF).count();
            long trainers = loaded.stream().filter(u -> u.getRole() == User.Role.TRAINER).count();
            kpis.removeAll();
            kpis.add(new KpiCard("Total users", String.valueOf(loaded.size())));
            kpis.add(new KpiCard("Admins", String.valueOf(admins)));
            kpis.add(new KpiCard("Staff", String.valueOf(staff)));
            kpis.add(new KpiCard("Trainers", String.valueOf(trainers)));
            subtitle.setText("Manage staff and trainer login accounts · " + loaded.size() + " total");
            render();
        } catch (Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    private void render() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        String role = String.valueOf(roleFilter.getSelectedItem());
        String status = String.valueOf(statusFilter.getSelectedItem());
        List<String[]> rows = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (User u : loaded) {
            if (!"All roles".equals(role) && (u.getRole() == null || !u.getRole().name().equals(role))) continue;
            if ("Active".equals(status) && !u.isActive()) continue;
            if ("Inactive".equals(status) && u.isActive()) continue;
            String name = u.getDisplayName() == null || u.getDisplayName().isBlank() ? u.getUsername() : u.getDisplayName();
            String blob = (name + " " + u.getUsername()).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !blob.contains(q)) continue;
            rows.add(new String[]{
                    name,
                    u.getUsername(),
                    pretty(u.getRole()),
                    u.getLastLoginAt() == null ? "—" : DateUtils.format(u.getLastLoginAt()),
                    u.isActive() ? "Active" : "Inactive",
                    u.getCreatedAt() == null ? "—" : DateUtils.format(u.getCreatedAt())
            });
            ids.add(u.getUserId());
        }
        table.setRowIds(ids);
        table.setRows(rows);
    }

    private static String pretty(User.Role role) {
        if (role == null) return "—";
        return switch (role) {
            case ADMIN -> "Admin";
            case STAFF -> "Staff";
            case TRAINER -> "Trainer";
        };
    }

    private void edit(int row) {
        int id = table.idAt(row);
        if (id > 0) Router.get().navigateTo(Route.USER_FORM_EDIT, RouteContext.of("id", id));
    }

    private void resetPw(int row) {
        int id = table.idAt(row);
        if (id <= 0) return;
        String pw = JOptionPane.showInputDialog(this, "New password (min 8 characters)", "Reset password", JOptionPane.PLAIN_MESSAGE);
        if (pw == null || pw.isBlank()) return;
        try {
            service.adminResetPassword(id, pw.trim());
            JOptionPane.showMessageDialog(this, "Password updated.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Reset password", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void toggle(int row) {
        int id = table.idAt(row);
        if (id <= 0) return;
        try {
            User u = service.findById(id);
            service.setActive(id, !u.isActive());
            onEnter(RouteContext.empty());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "User", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Users", () -> Router.get().navigateTo(Route.USERS_LIST)));
    }
}

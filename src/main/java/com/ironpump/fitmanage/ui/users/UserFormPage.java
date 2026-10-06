package com.ironpump.fitmanage.ui.users;

import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.model.User;
import com.ironpump.fitmanage.service.TrainerService;
import com.ironpump.fitmanage.service.UserService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class UserFormPage extends BasePage {
    private final UserService service = new UserService();
    private final TrainerService trainerService = new TrainerService();
    private final boolean isEdit;
    private int userId;
    private final JTextField username = new JTextField();
    private final JTextField displayName = new JTextField();
    private final JTextField email = new JTextField();
    private final JComboBox<User.Role> role = new JComboBox<>(new User.Role[]{User.Role.ADMIN, User.Role.STAFF, User.Role.TRAINER});
    private final JPasswordField password = new JPasswordField();
    private final JComboBox<TrainerItem> linkedTrainer = new JComboBox<>();
    private final JCheckBox active = new JCheckBox("Active", true);
    private final JLabel status = new JLabel(" ");

    public static UserFormPage newForCreate() { return new UserFormPage(false, 0); }
    public static UserFormPage newForEdit()   { return new UserFormPage(true,  0); }

    private UserFormPage(boolean isEdit, int unused) {
        this.isEdit = isEdit;
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int row = 0;
        JLabel h1 = new JLabel(isEdit ? "Edit user" : "New user");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(h1, c); c.gridwidth = 1;
        addRow(form, c, row++, "Username *", username);
        addRow(form, c, row++, "Display name", displayName);
        addRow(form, c, row++, "Email", email);
        addRow(form, c, row++, "Role *", role);
        if (!isEdit) addRow(form, c, row++, "Password *", password);
        addRow(form, c, row++, "Linked trainer (TRAINER role only)", linkedTrainer);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(active, c);
        c.gridwidth = 1;

        JButton save = new JButton(isEdit ? "Save" : "Create");
        save.setBackground(new Color(0x22, 0xC5, 0x5E));
        save.setForeground(new Color(0x06, 0x2b, 0x15));
        save.setOpaque(true);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(save, c);
        save.addActionListener(e -> save());

        c.gridy = row;
        status.setForeground(new Color(0xef, 0x44, 0x44));
        form.add(status, c);
        add(new JScrollPane(form), BorderLayout.CENTER);

        try {
            for (Trainer t : trainerService.list()) linkedTrainer.addItem(new TrainerItem(t));
        } catch (Exception ignored) { /* DB might be empty */ }
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setForeground(new Color(0x8b, 0x97, 0xa4));
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(l, c);
        c.gridx = 1; c.weightx = 1;
        form.add(field, c);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (isEdit && ctx != null && ctx.has("id")) {
            userId = ctx.get("id");
            try {
                User u = service.findById(userId);
                username.setText(u.getUsername());
                displayName.setText(u.getDisplayName());
                email.setText(u.getEmail());
                role.setSelectedItem(u.getRole());
                active.setSelected(u.isActive());
                if (u.getLinkedTrainerId() != null) {
                    for (int i = 0; i < linkedTrainer.getItemCount(); i++) {
                        if (linkedTrainer.getItemAt(i).trainer.getTrainerId() == u.getLinkedTrainerId()) {
                            linkedTrainer.setSelectedIndex(i); break;
                        }
                    }
                }
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        } else {
            username.setEnabled(true);
        }
    }

    private void save() {
        try {
            User u = isEdit ? service.findById(userId) : new User();
            u.setUsername(username.getText().trim());
            u.setDisplayName(displayName.getText().trim());
            u.setEmail(email.getText().trim());
            u.setRole((User.Role) role.getSelectedItem());
            u.setActive(active.isSelected());
            TrainerItem ti = (TrainerItem) linkedTrainer.getSelectedItem();
            u.setLinkedTrainerId(ti == null ? null : ti.trainer.getTrainerId());
            if (isEdit) service.adminUpdate(u);
            else service.createStaffOrTrainer(u, new String(password.getPassword()));
            Router.get().navigateTo(Route.USERS_LIST);
        } catch (Exception e) { status.setText(e.getMessage()); }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Users", () -> Router.get().navigateTo(Route.USERS_LIST)),
                new BreadcrumbItem(isEdit ? "Edit" : "New", () -> {}));
    }

    private static class TrainerItem {
        final Trainer trainer;
        TrainerItem(Trainer t) { this.trainer = t; }
        @Override public String toString() { return trainer.getName() + " (#" + trainer.getTrainerId() + ")"; }
    }
}

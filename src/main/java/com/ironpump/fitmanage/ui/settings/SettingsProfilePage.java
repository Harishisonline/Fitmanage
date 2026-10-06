package com.ironpump.fitmanage.ui.settings;

import com.ironpump.fitmanage.App;
import com.ironpump.fitmanage.auth.AuthService;
import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.UserDao;
import com.ironpump.fitmanage.model.User;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Settings → Profile: change your own display name, email, password. */
public class SettingsProfilePage extends BasePage {
    private final User current;
    private final JTextField displayName = new JTextField();
    private final JTextField email = new JTextField();
    private final JPasswordField oldPwd = new JPasswordField();
    private final JPasswordField newPwd = new JPasswordField();
    private final JLabel status = new JLabel(" ");

    public SettingsProfilePage() {
        this.current = SessionContext.current();
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel h1 = new JLabel("Profile");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int row = 0;
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        JLabel who = new JLabel("Signed in as " + (current == null ? "?" : current.getDisplayName()) + "  (" + (current == null ? "?" : current.getRole()) + ")");
        who.setForeground(new Color(0x8b, 0x97, 0xa4));
        form.add(who, c); c.gridwidth = 1;
        addRow(form, c, row++, "Display name", displayName);
        addRow(form, c, row++, "Email", email);

        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        JLabel p = new JLabel("Change password");
        p.setForeground(new Color(0x8b, 0x97, 0xa4));
        p.setFont(p.getFont().deriveFont(Font.BOLD, 13f));
        form.add(p, c);
        c.gridwidth = 1;
        addRow(form, c, row++, "Current password", oldPwd);
        addRow(form, c, row++, "New password",     newPwd);

        JButton saveP = new JButton("Save profile");
        saveP.addActionListener(e -> saveProfile());
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(saveP, c);

        JButton saveW = new JButton("Update password");
        saveW.addActionListener(e -> updatePassword());
        c.gridy = row++;
        form.add(saveW, c);

        c.gridy = row;
        status.setForeground(new Color(0xef, 0x44, 0x44));
        form.add(status, c);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(h1);
        top.add(Box.createVerticalStrut(10));
        top.add(form);
        add(top, BorderLayout.NORTH);
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
        if (current != null) {
            displayName.setText(current.getDisplayName());
            email.setText(current.getEmail());
        }
    }

    private void saveProfile() {
        try {
            com.ironpump.fitmanage.util.Validator.requireMax("displayName", displayName.getText().trim(), 80);
            new com.ironpump.fitmanage.service.UserService()
                    .updateOwnProfile(current.getUserId(), displayName.getText().trim(), email.getText().trim());
            status.setText("Saved.");
            // Refresh in-memory user so subsequent dialogs see the new name/email
            com.ironpump.fitmanage.model.User fresh = new com.ironpump.fitmanage.dao.UserDao().findById(current.getUserId()).orElse(current);
            com.ironpump.fitmanage.auth.SessionContext.login(fresh, com.ironpump.fitmanage.App.cfg().getInt("app.idle.timeout.minutes"));
        } catch (Exception e) { status.setText(e.getMessage()); }
    }

    private void updatePassword() {
        try {
            new AuthService(new UserDao(), App.cfg())
                    .changeOwnPassword(current.getUserId(), new String(oldPwd.getPassword()), new String(newPwd.getPassword()));
            status.setText("Password updated.");
            oldPwd.setText(""); newPwd.setText("");
        } catch (Exception e) { status.setText(e.getMessage()); }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Settings", () -> Router.get().navigateTo(Route.SETTINGS_PROFILE)),
                new BreadcrumbItem("Profile", () -> {}));
    }
}

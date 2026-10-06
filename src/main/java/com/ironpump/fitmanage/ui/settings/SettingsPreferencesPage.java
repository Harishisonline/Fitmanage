package com.ironpump.fitmanage.ui.settings;

import com.ironpump.fitmanage.App;
import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.service.SettingsService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Settings → Preferences: theme, currency, date format, idle timeout. */
public class SettingsPreferencesPage extends BasePage {
    private final AppConfig cfg = App.cfg();
    private final SettingsService svc = new SettingsService(cfg);
    private final JComboBox<String> theme = new JComboBox<>(new String[]{"dark", "light"});
    private final JComboBox<String> currency = new JComboBox<>(new String[]{"INR", "USD", "EUR"});
    private final JComboBox<String> dateFmt = new JComboBox<>(new String[]{"yyyy-MM-dd", "dd/MM/yyyy", "dd MMM yyyy"});
    private final JTextField idle = new JTextField();
    private final JCheckBox autoCheckOut = new JCheckBox("Auto check-out members who stay > 4 hours");
    private final JLabel status = new JLabel(" ");

    public SettingsPreferencesPage() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel h1 = new JLabel("Preferences");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int row = 0;
        addRow(form, c, row++, "Theme (dark/light)", theme);
        addRow(form, c, row++, "Currency", currency);
        addRow(form, c, row++, "Date format", dateFmt);
        addRow(form, c, row++, "Idle timeout (minutes)", idle);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(autoCheckOut, c); c.gridwidth = 1;

        JButton save = new JButton("Save");
        save.setBackground(new Color(0x22, 0xC5, 0x5E));
        save.setForeground(new Color(0x06, 0x2b, 0x15));
        save.setOpaque(true);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(save, c);
        save.addActionListener(e -> save());

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
        theme.setSelectedItem(svc.theme().isEmpty() ? "dark" : svc.theme());
        currency.setSelectedItem(svc.currency().isEmpty() ? "INR" : svc.currency());
        dateFmt.setSelectedItem(svc.dateFormat().isEmpty() ? "yyyy-MM-dd" : svc.dateFormat());
        idle.setText(String.valueOf(svc.idleTimeoutMinutes()));
    }

    private void save() {
        try {
            int minutes = com.ironpump.fitmanage.util.Validator.intInRange("idleTimeout", idle.getText(), 1, 240);
            svc.set("app.date.format", (String) dateFmt.getSelectedItem());
            svc.set("app.idle.timeout.minutes", String.valueOf(minutes));
            svc.setCurrency((String) currency.getSelectedItem());
            String newTheme = (String) theme.getSelectedItem();
            if (!newTheme.equalsIgnoreCase(svc.theme())) {
                ThemeManager.applyAtRuntime(newTheme, svc);
            } else {
                svc.save();
            }
            status.setText("Saved.");
        } catch (Exception e) { status.setText(e.getMessage()); }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Settings", () -> Router.get().navigateTo(Route.SETTINGS_PROFILE)),
                new BreadcrumbItem("Preferences", () -> {}));
    }
}

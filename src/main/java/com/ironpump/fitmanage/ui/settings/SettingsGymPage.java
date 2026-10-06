package com.ironpump.fitmanage.ui.settings;

import com.ironpump.fitmanage.App;
import com.ironpump.fitmanage.service.SettingsService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class SettingsGymPage extends BasePage {
    private final SettingsService svc = new SettingsService(App.cfg());
    private final JTextField name = new JTextField();
    private final JTextField address = new JTextField();
    private final JTextField phone = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField gstin = new JTextField();
    private final JTextField footer = new JTextField();
    private final JLabel status = new JLabel(" ");

    public SettingsGymPage() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel h1 = new JLabel("Gym information");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int row = 0;
        addRow(form, c, row++, "Gym name", name);
        addRow(form, c, row++, "Address", address);
        addRow(form, c, row++, "Phone", phone);
        addRow(form, c, row++, "Email", email);
        addRow(form, c, row++, "GSTIN / Tax ID", gstin);
        addRow(form, c, row++, "Receipt footer", footer);
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

        JTextArea preview = new JTextArea();
        preview.setEditable(false);
        preview.setOpaque(true);
        preview.setBackground(Color.WHITE);
        preview.setForeground(new Color(0x0f, 0x17, 0x2a));
        preview.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        preview.setBorder(new EmptyBorder(16, 16, 16, 16));
        Runnable paintPreview = () -> preview.setText(
                name.getText() + "\n"
                        + address.getText() + "\n"
                        + phone.getText() + "  ·  " + email.getText() + "\n"
                        + "GSTIN: " + gstin.getText() + "\n\n"
                        + footer.getText());
        javax.swing.event.DocumentListener live = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { paintPreview.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { paintPreview.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { paintPreview.run(); }
        };
        for (JTextField f : new JTextField[]{name, address, phone, email, gstin, footer}) f.getDocument().addDocumentListener(live);

        JPanel split = new JPanel(new GridLayout(1, 2, 16, 0));
        split.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(form, BorderLayout.NORTH);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        JLabel ph = new JLabel("Receipt preview");
        ph.setForeground(new Color(0x8b, 0x97, 0xa4));
        right.add(ph, BorderLayout.NORTH);
        right.add(preview, BorderLayout.CENTER);
        split.add(left);
        split.add(right);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(h1, BorderLayout.NORTH);
        top.add(split, BorderLayout.CENTER);
        add(top, BorderLayout.CENTER);
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
        name.setText(svc.gymName());
        address.setText(svc.gymAddress());
        phone.setText(svc.gymPhone());
        email.setText(svc.gymEmail());
        gstin.setText(svc.gymGstin());
        footer.setText(svc.gymFooter());
    }

    private void save() {
        svc.setGymName(name.getText().trim());
        svc.setGymAddress(address.getText().trim());
        svc.setGymPhone(phone.getText().trim());
        svc.setGymEmail(email.getText().trim());
        svc.setGymGstin(gstin.getText().trim());
        svc.setGymFooter(footer.getText().trim());
        status.setText("Saved.");
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Settings", () -> Router.get().navigateTo(Route.SETTINGS_PROFILE)),
                new BreadcrumbItem("Gym info", () -> {}));
    }
}

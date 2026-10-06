package com.ironpump.fitmanage.ui.settings;

import com.ironpump.fitmanage.App;
import com.ironpump.fitmanage.service.BackupService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class SettingsDatabasePage extends BasePage {
    private final BackupService service = new BackupService(App.cfg());
    private final JLabel status = new JLabel(" ");
    private final JList<String> list = new JList<>();
    private List<Path> backups;

    public SettingsDatabasePage() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel h1 = new JLabel("Database backup & restore");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));
        add(h1, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.setOpaque(false);
        list.setBackground(new Color(0x1a, 0x20, 0x27));
        list.setForeground(new Color(0xe6, 0xed, 0xf3));
        body.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton backup = new JButton("Backup now");
        backup.setBackground(new Color(0x22, 0xC5, 0x5E));
        backup.setForeground(new Color(0x06, 0x2b, 0x15));
        backup.setOpaque(true);
        backup.addActionListener(e -> doBackup());
        actions.add(backup);
        JButton restore = new JButton("Restore selected");
        restore.addActionListener(e -> doRestore());
        actions.add(restore);
        JButton refresh = new JButton("Refresh list");
        refresh.addActionListener(e -> refresh());
        actions.add(refresh);
        body.add(actions, BorderLayout.NORTH);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        status.setForeground(new Color(0xef, 0x44, 0x44));
        south.add(status);
        body.add(south, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) { refresh(); }

    private void refresh() {
        try {
            backups = service.listBackups();
            String[] names = backups.stream().map(p -> {
                try {
                    long bytes = java.nio.file.Files.size(p);
                    String size = bytes < 1024 ? bytes + " B" : String.format("%.1f MB", bytes / 1024.0 / 1024.0);
                    var age = java.time.Duration.between(java.nio.file.Files.getLastModifiedTime(p).toInstant(), java.time.Instant.now());
                    String when = age.toHours() + " hours ago";
                    return p.getFileName() + " · " + size + " · " + when;
                } catch (Exception ex) {
                    return p.getFileName().toString();
                }
            }).toArray(String[]::new);
            list.setListData(names);
            status.setText(backups.size() + " backup(s) on disk.");
        } catch (Exception e) { GlobalExceptionHandler.handle(e); }
    }

    private void doBackup() {
        try {
            Path p = service.backupNow();
            status.setText("Wrote " + p);
            refresh();
        } catch (Exception e) { GlobalExceptionHandler.handle(e); }
    }

    private void doRestore() {
        if (list.isSelectionEmpty()) { status.setText("Pick a backup first."); return; }
        int choice = JOptionPane.showConfirmDialog(this,
                "Restore from selected backup? This will overwrite the current database.",
                "Confirm restore", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            service.restoreFrom(backups.get(list.getSelectedIndex()));
            status.setText("Restored. Restart the app for clean state.");
        } catch (Exception e) { GlobalExceptionHandler.handle(e); }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Settings", () -> Router.get().navigateTo(Route.SETTINGS_PROFILE)),
                new BreadcrumbItem("Database", () -> {}));
    }
}

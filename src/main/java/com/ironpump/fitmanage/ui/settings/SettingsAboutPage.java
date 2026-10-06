package com.ironpump.fitmanage.ui.settings;

import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class SettingsAboutPage extends BasePage {
    public SettingsAboutPage() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel h1 = new JLabel("About FitManage");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));

        JTextArea body = new JTextArea(
            "FitManage v2.0.0\n" +
            "Java 17 · Swing · MySQL · FlatLaf · jBCrypt\n\n" +
            "A complete gym management system for owners, staff, trainers, and members.\n\n" +
            "Stack:\n" +
            "  • Presentation: Java Swing (FlatLaf 3.x dark/light)\n" +
            "  • Persistence:  JDBC + MySQL 8\n" +
            "  • Charts:       JFreeChart\n" +
            "  • PDF:          Apache PDFBox 3.x\n" +
            "  • Crypto:       jBCrypt (workload=12)\n" +
            "  • Build:        Apache Maven\n\n" +
            "License: MIT"
        );
        body.setEditable(false);
        body.setOpaque(true);
        body.setBackground(new Color(0x1a, 0x20, 0x27));
        body.setForeground(new Color(0xe6, 0xed, 0xf3));
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(h1);
        top.add(Box.createVerticalStrut(10));
        top.add(new JScrollPane(body));
        add(top, BorderLayout.CENTER);
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Settings", () -> Router.get().navigateTo(Route.SETTINGS_PROFILE)),
                new BreadcrumbItem("About", () -> {}));
    }
}

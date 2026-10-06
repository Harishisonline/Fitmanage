package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

public class KpiCard extends JPanel {
    public KpiCard(String label, String value) { this(label, value, null, null); }

    public KpiCard(String label, String value, String subtitle) { this(label, value, subtitle, null); }

    public KpiCard(String label, String value, String subtitle, Color valueColor) {
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel l = new JLabel(label == null ? "" : label.toUpperCase());
        l.setForeground(Ui.SECONDARY);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        add(l, BorderLayout.NORTH);
        JLabel v = new JLabel(value == null ? "—" : value);
        v.setForeground(valueColor == null ? Ui.TEXT : valueColor);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 22f));
        add(v, BorderLayout.CENTER);
        if (subtitle != null && !subtitle.isBlank()) {
            JLabel s = new JLabel(subtitle);
            s.setForeground(Ui.SECONDARY);
            s.setFont(s.getFont().deriveFont(12f));
            add(s, BorderLayout.SOUTH);
        }
    }

    public KpiCard(String label, String value, TrendLabel trend) {
        this(label, value, null, null);
        if (trend != null) add(trend, BorderLayout.SOUTH);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Ui.PANEL);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.setColor(Ui.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}

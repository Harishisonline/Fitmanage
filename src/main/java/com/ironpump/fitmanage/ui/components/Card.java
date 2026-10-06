package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** Visual: see assets/ui-mockups/02-dashboard.html. Used by DashboardPage, MemberDetailPage, settings pages. */
public class Card extends JPanel {
    public Card() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    }

    public Card(String title) {
        this();
        if (title != null) {
            JLabel l = new JLabel(title);
            l.setForeground(Ui.TEXT);
            l.setFont(l.getFont().deriveFont(Font.BOLD, 14f));
            add(l, BorderLayout.NORTH);
        }
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

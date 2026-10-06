package com.ironpump.fitmanage.ui.components;

import com.ironpump.fitmanage.model.ActivityLog;
import com.ironpump.fitmanage.util.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** A vertical timeline of activity log entries. Paints the dots + lines itself. */
public class TimelinePanel extends JPanel {
    private List<ActivityLog> items;
    public TimelinePanel(List<ActivityLog> items) {
        this.items = items == null ? List.of() : items;
        setOpaque(true);
        setBackground(new Color(0x1a, 0x20, 0x27));
        setPreferredSize(new Dimension(320, Math.max(180, this.items.size() * 64 + 24)));
    }

    public void setItems(List<ActivityLog> items) {
        this.items = items == null ? List.of() : items;
        setPreferredSize(new Dimension(320, Math.max(180, this.items.size() * 64 + 24)));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = 18;
            int leftPad = 36;
            for (ActivityLog a : items) {
                g2.setColor(new Color(0x22, 0xC5, 0x5E));
                g2.fillOval(leftPad - 6, y - 6, 12, 12);
                g2.setColor(new Color(0xe6, 0xed, 0xf3));
                g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
                g2.drawString(a.getAction(), leftPad + 18, y + 4);
                g2.setColor(new Color(0x8b, 0x97, 0xa4));
                g2.setFont(getFont().deriveFont(11f));
                String detail = a.getDetail() == null ? "" : a.getDetail();
                g2.drawString(detail, leftPad + 18, y + 22);
                g2.drawString(DateUtils.format(a.getCreatedAt()), leftPad + 18, y + 38);
                y += 64;
                if (y > getHeight() - 20) break;
            }
            if (items.isEmpty()) {
                g2.setColor(new Color(0x8b, 0x97, 0xa4));
                g2.drawString("No activity yet.", leftPad + 18, y);
            }
        } finally { g2.dispose(); }
    }
}

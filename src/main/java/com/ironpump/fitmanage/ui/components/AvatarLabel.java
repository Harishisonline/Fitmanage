package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** Visual: see assets/ui-mockups/14-member-detail.html. Used by MemberDetailPage, TrainerOverviewPage, MembersListPage, Sidebar. */
public class AvatarLabel extends JComponent {
    private final String initials;
    private final Color fill;
    private final int px;

    public AvatarLabel(String name, int px) {
        this.px = px;
        this.initials = initialsOf(name);
        this.fill = colorFor(name);
        setPreferredSize(new Dimension(px, px));
        setMinimumSize(new Dimension(px, px));
        setMaximumSize(new Dimension(px, px));
        setOpaque(false);
    }

    public static Color colorFor(String name) {
        int h = name == null ? 0 : Math.floorMod(name.hashCode(), Ui.AVATAR.length);
        return Ui.AVATAR[h];
    }

    public static String initialsOf(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String w = parts[0];
            return w.substring(0, Math.min(2, w.length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillOval(0, 0, px - 1, px - 1);
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(11f, px * 0.34f)));
        FontMetrics fm = g2.getFontMetrics();
        int x = (px - fm.stringWidth(initials)) / 2;
        int y = (px - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(initials, x, y);
        g2.dispose();
    }
}

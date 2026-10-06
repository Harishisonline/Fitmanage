package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** Small colored pill (e.g. "Active", "Expiring", "Expired"). */
public class StatusBadge extends JLabel {
    public enum Kind { GREEN, AMBER, RED, GRAY, BLUE, INFO }

    public StatusBadge(String text, Kind kind) {
        super("  " + text + "  ");
        setOpaque(true);
        setFont(getFont().deriveFont(Font.BOLD, 10f));
        setHorizontalAlignment(CENTER);
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        applyColors(kind);
    }

    private void applyColors(Kind kind) {
        Color bg, fg;
        switch (kind) {
            case GREEN -> { bg = new Color(0x22, 0xC5, 0x5E, 40); fg = new Color(0x22, 0xC5, 0x5E); }
            case AMBER -> { bg = new Color(0xf5, 0x9e, 0x0b, 40); fg = new Color(0xf5, 0x9e, 0x0b); }
            case RED   -> { bg = new Color(0xef, 0x44, 0x44, 40); fg = new Color(0xef, 0x44, 0x44); }
            case BLUE  -> { bg = new Color(0x0e, 0xa5, 0xe9, 40); fg = new Color(0x0e, 0xa5, 0xe9); }
            case INFO  -> { bg = new Color(0x0e, 0xa5, 0xe9, 40); fg = new Color(0x0e, 0xa5, 0xe9); }
            default    -> { bg = new Color(0x8b, 0x97, 0xa4, 40); fg = new Color(0x8b, 0x97, 0xa4); }
        }
        setBackground(bg);
        setForeground(fg);
    }

    public static StatusBadge forMemberStatus(String status) {
        if (status == null) return new StatusBadge("Unknown", Kind.GRAY);
        return switch (status.toUpperCase()) {
            case "ACTIVE"    -> new StatusBadge("Active", Kind.GREEN);
            case "EXPIRING"  -> new StatusBadge("Expiring", Kind.AMBER);
            case "EXPIRED"   -> new StatusBadge("Expired", Kind.RED);
            case "INACTIVE"  -> new StatusBadge("Inactive", Kind.GRAY);
            case "UPCOMING"  -> new StatusBadge("Upcoming", Kind.BLUE);
            case "CANCELLED" -> new StatusBadge("Cancelled", Kind.GRAY);
            default         -> new StatusBadge(status, Kind.GRAY);
        };
    }

    public static StatusBadge forPaymentStatus(String status) {
        if (status == null) return new StatusBadge("Unknown", Kind.GRAY);
        return switch (status.toUpperCase()) {
            case "PAID"    -> new StatusBadge("Paid", Kind.GREEN);
            case "REFUND"  -> new StatusBadge("Refund", Kind.AMBER);
            default        -> new StatusBadge(status, Kind.GRAY);
        };
    }
}

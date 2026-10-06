package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Shared colours, type, and button styles from the mockups. */
public final class Ui {
    public static final Color BG = new Color(0x0f, 0x14, 0x19);
    public static final Color PANEL = new Color(0x1a, 0x20, 0x27);
    public static final Color SIDEBAR = new Color(0x0a, 0x0f, 0x14);
    public static final Color BORDER = new Color(0x2a, 0x35, 0x40);
    public static final Color BORDER_SOFT = new Color(0x1f, 0x29, 0x37);
    public static final Color GREEN = new Color(0x22, 0xc5, 0x5e);
    public static final Color RED = new Color(0xef, 0x44, 0x44);
    public static final Color AMBER = new Color(0xf5, 0x9e, 0x0b);
    public static final Color BLUE = new Color(0x0e, 0xa5, 0xe9);
    public static final Color PURPLE = new Color(0x8b, 0x5c, 0xf6);
    public static final Color CYAN = new Color(0x22, 0xd3, 0xee);
    public static final Color TEXT = new Color(0xe6, 0xed, 0xf3);
    public static final Color SECONDARY = new Color(0x8b, 0x97, 0xa4);
    public static final Color MUTED = new Color(0x5b, 0x67, 0x72);
    public static final Color[] AVATAR = { GREEN, PURPLE, BLUE, AMBER, RED };

    private Ui() {}

    public static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(18, 22, 18, 22));
        return p;
    }

    public static JLabel h1(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 26f));
        return l;
    }

    public static JLabel sub(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(SECONDARY);
        l.setFont(l.getFont().deriveFont(13f));
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(l.getFont().deriveFont(12f));
        return l;
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(GREEN);
        b.setForeground(new Color(0x05, 0x20, 0x10));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setForeground(TEXT);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(7, 12, 7, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton danger(String text) {
        JButton b = secondary(text);
        b.setForeground(RED);
        return b;
    }

    public static JPanel row(int gap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        return p;
    }

    public static JPanel kpiRow(JComponent... cards) {
        JPanel p = new JPanel(new GridLayout(1, Math.max(1, cards.length), 12, 0));
        p.setOpaque(false);
        for (JComponent c : cards) p.add(c);
        return p;
    }

    public static void styleField(JComponent c) {
        c.setBackground(SIDEBAR);
        c.setForeground(TEXT);
        if (c instanceof JTextField f) f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(6, 8, 6, 8)));
    }
}

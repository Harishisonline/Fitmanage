package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** Visual: see assets/ui-mockups/03-members.html. Used by MembersListPage, PaymentsListPage, TrainerMembersPage. */
public class FilterBar extends JPanel {
    private final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    public FilterBar() {
        super(new BorderLayout());
        setOpaque(false);
        filters.setOpaque(false);
        trailing.setOpaque(false);
        add(filters, BorderLayout.CENTER);
        add(trailing, BorderLayout.EAST);
    }

    public JComboBox<String> addFilter(String label, String[] options) {
        JLabel l = new JLabel(label);
        l.setForeground(Ui.MUTED);
        l.setFont(l.getFont().deriveFont(11f));
        filters.add(l);
        JComboBox<String> box = new JComboBox<>(options);
        box.setBackground(Ui.PANEL);
        box.setForeground(Ui.TEXT);
        filters.add(box);
        return box;
    }

    public void setTrailing(JComponent c) { trailing.removeAll(); if (c != null) trailing.add(c); }

    /** Left-hand cluster, same row as the dropdowns. */
    public void addControl(JComponent c) { if (c != null) filters.add(c); }

    @Override public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}

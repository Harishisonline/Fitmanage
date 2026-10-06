package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Visual: see assets/ui-mockups/02-dashboard.html. Used by MainFrame. */
public class TopbarSearch extends JPanel {
    private final JTextField field = new JTextField(18);

    public TopbarSearch(String placeholder, Consumer<String> onSearch) {
        super(new BorderLayout(4, 0));
        setOpaque(true);
        setBackground(Ui.SIDEBAR);
        setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        JLabel icon = new JLabel("  Search  ");
        icon.setForeground(Ui.MUTED);
        add(icon, BorderLayout.WEST);
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(6, 0, 6, 8));
        field.setForeground(Ui.TEXT);
        field.setCaretColor(Ui.TEXT);
        field.setToolTipText(placeholder);
        field.putClientProperty("JTextField.placeholderText", placeholder);
        Runnable fire = () -> onSearch.accept(field.getText().trim());
        field.addActionListener(e -> fire.run());
        add(field, BorderLayout.CENTER);
        JButton go = new JButton("Search");
        go.setFocusPainted(false);
        go.setBackground(Ui.GREEN);
        go.setForeground(new Color(0x05, 0x20, 0x10));
        go.setBorder(new EmptyBorder(4, 10, 4, 10));
        go.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        go.addActionListener(e -> fire.run());
        add(go, BorderLayout.EAST);
        setPreferredSize(new Dimension(380, 34));
    }

    public String text() { return field.getText().trim(); }
    public void clear() { field.setText(""); }
}

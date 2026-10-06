package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** JTextField with a debounced onChange callback. */
public class SearchField extends JTextField {
    public interface OnChange { void run(String text); }

    private final Timer timer;
    public SearchField(String placeholder, OnChange cb) {
        setColumns(20);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        timer = new Timer(220, e -> cb.run(getText().trim()));
        timer.setRepeats(false);
        getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { timer.restart(); }
        });
        getAccessibleContext().setAccessibleName(placeholder);
    }
}

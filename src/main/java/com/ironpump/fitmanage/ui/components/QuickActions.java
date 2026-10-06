package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** Visual: see assets/ui-mockups/02-dashboard.html. Used by DashboardPage, MemberDetailPage. */
public class QuickActions extends JPanel {
    public QuickActions(JButton... buttons) {
        super(new GridLayout(3, 2, 8, 8));
        setOpaque(false);
        for (JButton b : buttons) {
            b.setHorizontalAlignment(SwingConstants.LEFT);
            add(b);
        }
    }
}

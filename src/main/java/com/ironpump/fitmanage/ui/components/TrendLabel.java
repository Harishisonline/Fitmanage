package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** Visual: see assets/ui-mockups/02-dashboard.html. Used by DashboardPage, KpiCard. */
public class TrendLabel extends JLabel {
    public TrendLabel(int delta, String suffix, boolean isPositive) {
        super((isPositive ? "▲ " : "▼ ") + Math.abs(delta) + (suffix == null || suffix.isBlank() ? "" : " " + suffix));
        setForeground(isPositive ? Ui.GREEN : Ui.RED);
        setFont(getFont().deriveFont(Font.BOLD, 11f));
    }
}

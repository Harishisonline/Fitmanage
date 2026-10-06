package com.ironpump.fitmanage.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.service.SettingsService;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

/**
 * Applies FlatLaf + our token overrides at startup. Theme can be flipped at
 * runtime from the Settings page.
 */
public final class ThemeManager {
    public enum Theme { DARK, LIGHT }

    private ThemeManager() {}

    public static void applyInitial(String theme) {
        try {
            LookAndFeel laf = Theme.DARK.name().equalsIgnoreCase(theme)
                    ? new FlatDarkLaf() : new FlatLightLaf();
            UIManager.setLookAndFeel(laf);
            applyTokens(Theme.DARK.name().equalsIgnoreCase(theme));
        } catch (Exception e) {
            throw new FitManageException("Could not set look-and-feel: " + e.getMessage(), e);
        }
    }

    public static void applyAtRuntime(String themeName, SettingsService settings) {
        Theme t = Theme.valueOf(themeName.toUpperCase());
        applyInitial(t.name());
        settings.setTheme(t.name().toLowerCase());
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            w.invalidate();
            w.validate();
            w.repaint();
        }
    }

    private static void applyTokens(boolean dark) {
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TabbedPane.tabHeight", 32);
    }
}

package com.ironpump.fitmanage.ui.base;

import com.ironpump.fitmanage.ui.router.RouteContext;

import javax.swing.JPanel;
import java.util.List;

/**
 * Every navigable page extends this. The router calls onEnter before showing
 * and onLeave after hiding. Subclasses implement breadcrumbs() for the topbar.
 */
public abstract class BasePage extends JPanel {
    public void onEnter(RouteContext ctx) { /* default no-op */ }
    public void onLeave() { /* default no-op */ }
    public abstract List<BreadcrumbItem> breadcrumbs(RouteContext ctx);

    public record BreadcrumbItem(String label, Runnable onClick) {}
}

package com.ironpump.fitmanage.ui.components;

import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.RectangularShape;

/** Visual: see assets/ui-mockups/02-dashboard.html. Used by ChartPanelFactory, DashboardPage. */
public class GradientBar implements BarPainter {
    @Override
    public void paintBar(Graphics2D g2, BarRenderer renderer, int row, int column,
                         RectangularShape bar, RectangleEdge base) {
        GradientPaint paint = new GradientPaint(
                0f, (float) bar.getMaxY(), Ui.GREEN,
                0f, (float) bar.getMinY(), Ui.CYAN);
        g2.setPaint(paint);
        g2.fill(bar);
    }

    @Override
    public void paintBarShadow(Graphics2D g2, BarRenderer renderer, int row, int column,
                               RectangularShape bar, RectangleEdge base, boolean pegShadow) {
        // Mockup bars have no drop shadow.
    }
}

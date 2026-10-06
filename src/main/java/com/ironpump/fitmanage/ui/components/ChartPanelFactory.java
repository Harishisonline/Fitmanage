package com.ironpump.fitmanage.ui.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.util.List;

/** Factory wrapping common JFreeChart usages. */
public class ChartPanelFactory {

    public static ChartPanel attendanceBarChart(List<String> labels, List<Number> counts) {
        DefaultCategoryDataset ds = dataset("Check-ins", labels, counts);
        JFreeChart ch = ChartFactory.createBarChart(null, "", "Daily check-ins", ds);
        styleChart(ch);
        if (ch.getCategoryPlot().getRenderer() instanceof BarRenderer br) {
            br.setBarPainter(new GradientBar());
            br.setShadowVisible(false);
            br.setDrawBarOutline(false);
        }
        pinCountAxis(ch.getCategoryPlot(), counts);
        ChartPanel cp = new ChartPanel(ch);
        cp.setPreferredSize(new Dimension(640, 280));
        return cp;
    }

    public static ChartPanel attendanceBarChart(List<Number> counts) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (int i = 1; i <= counts.size(); i++) labels.add(String.valueOf(i));
        return attendanceBarChart(labels, counts);
    }

    public static ChartPanel revenueLineChart(List<Number> dailyRevenue, List<String> dayLabels) {
        DefaultCategoryDataset ds = dataset("Revenue", dayLabels, dailyRevenue);
        JFreeChart ch = ChartFactory.createLineChart("Revenue", "Day", "Rs.", ds);
        styleChart(ch);
        return new ChartPanel(ch);
    }

    public static ChartPanel weightLineChart(List<Number> weights, List<String> dayLabels) {
        DefaultCategoryDataset ds = dataset("kg", dayLabels, weights);
        JFreeChart ch = ChartFactory.createLineChart("Weight", "Day", "kg", ds);
        styleChart(ch);
        return new ChartPanel(ch);
    }

    /**
     * Column keys are unique per index. Weekday names repeat across 14 days;
     * {@code DefaultCategoryDataset} would otherwise keep only the first of each name.
     */
    private static DefaultCategoryDataset dataset(String series, List<String> labels, List<Number> values) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        if (values == null) return ds;
        for (int i = 0; i < values.size(); i++) {
            String label = labels != null && i < labels.size() ? labels.get(i) : String.valueOf(i + 1);
            double v = values.get(i) == null ? 0d : values.get(i).doubleValue();
            ds.addValue(v, series, new DayKey(i, label));
        }
        return ds;
    }

    /** Counts sit on a zero floor. An all-zero series gets a small positive top so the axis does not collapse. */
    private static void pinCountAxis(CategoryPlot plot, List<Number> counts) {
        double max = 0d;
        if (counts != null) {
            for (Number n : counts) if (n != null) max = Math.max(max, n.doubleValue());
        }
        NumberAxis axis = (NumberAxis) plot.getRangeAxis();
        axis.setAutoRange(false);
        double upper = max <= 0d ? 4d : Math.max(1d, max * 1.2d);
        axis.setRange(0d, upper);
    }

    private static void styleChart(JFreeChart ch) {
        ch.setBackgroundPaint(new Color(0x1a, 0x20, 0x27));
        if (ch.getTitle() != null) ch.getTitle().setPaint(new Color(0xe6, 0xed, 0xf3));
        CategoryPlot plot = ch.getCategoryPlot();
        plot.setBackgroundPaint(new Color(0x22, 0x2a, 0x33));
        plot.setRangeGridlinePaint(new Color(0x2a, 0x35, 0x40));
        plot.getDomainAxis().setTickLabelPaint(new Color(0xe6, 0xed, 0xf3));
        plot.getRangeAxis().setTickLabelPaint(new Color(0xe6, 0xed, 0xf3));
        if (plot.getRenderer() instanceof BarRenderer br) {
            br.setBarPainter(new GradientBar());
            br.setShadowVisible(false);
        } else {
            plot.getRenderer().setSeriesPaint(0, new Color(0x22, 0xC5, 0x5E));
        }
    }

    /** Comparable category key. Equality is the day index; the axis label is the weekday text. */
    private static final class DayKey implements Comparable<DayKey> {
        private final int order;
        private final String label;

        DayKey(int order, String label) {
            this.order = order;
            this.label = label == null ? "" : label;
        }

        @Override public int compareTo(DayKey o) { return Integer.compare(order, o.order); }
        @Override public String toString() { return label; }
        @Override public boolean equals(Object o) { return o instanceof DayKey d && d.order == order; }
        @Override public int hashCode() { return order; }
    }
}

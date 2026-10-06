package com.ironpump.fitmanage.ui.components;

import com.ironpump.fitmanage.util.HeatmapPainter;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Wrapper JPanel that holds the heatmap counts and repaints via HeatmapPainter. */
public class HeatmapWrapper extends JPanel {
    private Map<LocalDate, Integer> counts = new HashMap<>();
    public HeatmapWrapper() {
        setOpaque(true);
        setBackground(new Color(0x1a, 0x20, 0x27));
        setPreferredSize(new Dimension(
                HeatmapPainter.widthPx() + 40,
                HeatmapPainter.heightPx() + 40));
    }
    public void setCounts(Map<LocalDate, Integer> counts) {
        this.counts = counts == null ? new HashMap<>() : counts;
        repaint();
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        HeatmapPainter.paint(g, 20, 20, counts, LocalDate.now());
    }
}

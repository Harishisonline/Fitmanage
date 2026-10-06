package com.ironpump.fitmanage.util;

import com.ironpump.fitmanage.exception.FitManageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.util.Map;

/**
 * Paints a 15-week x 7-day attendance heatmap as a Graphics2D draw.
 * No third-party charting needed.
 */
public final class HeatmapPainter {
    private static final Logger log = LoggerFactory.getLogger(HeatmapPainter.class);

    private HeatmapPainter() {}

    public static final int COLS = 15; // 15 weeks
    public static final int ROWS = 7;  // 7 days
    public static final int CELL = 18;
    public static final int GAP  = 3;

    public static void paint(java.awt.Graphics g, int x, int y, Map<LocalDate, Integer> counts, LocalDate today) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(x, y);
            int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            // walk back 15 weeks (105 days)
            LocalDate start = today.minusDays(105L);
            // align so the rightmost column is the current week
            LocalDate gridStart = start.minusDays((start.getDayOfWeek().getValue() - 1));
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    LocalDate day = gridStart.plusDays((long) c * 7 + r);
                    int count = day.isAfter(today) ? 0 : counts.getOrDefault(day, 0);
                    Color fill = levelColor(count, max);
                    g2.setColor(fill);
                    g2.fillRect(c * (CELL + GAP), r * (CELL + GAP), CELL, CELL);
                    g2.setColor(new Color(255, 255, 255, 20));
                    g2.drawRect(c * (CELL + GAP), r * (CELL + GAP), CELL, CELL);
                }
            }
        } catch (RuntimeException e) {
            log.error("Heatmap paint failed", e);
            throw new FitManageException("Heatmap paint failed", e);
        } finally {
            g2.dispose();
        }
    }

    private static Color levelColor(int count, int max) {
        if (count <= 0) return new Color(0x22, 0x2A, 0x33);     // empty
        double pct = max == 0 ? 0 : count / (double) max;
        if (pct >= 0.75) return new Color(0x22, 0xC5, 0x5E, 220);  // level 4
        if (pct >= 0.50) return new Color(0x22, 0xC5, 0x5E, 150);  // level 3
        if (pct >= 0.25) return new Color(0x22, 0xC5, 0x5E,  80);  // level 2
        return                  new Color(0x22, 0xC5, 0x5E,  30);  // level 1
    }

    public static int widthPx()  { return COLS * (CELL + GAP); }
    public static int heightPx() { return ROWS * (CELL + GAP); }
}

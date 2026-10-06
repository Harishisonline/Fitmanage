package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** A JScrollPane wrapping a non-editable JTable with optional checkbox and action columns. */
public class PagedTable extends JPanel {
    private final JTable table;
    private final DefaultTableModel model;
    private final JLabel status = new JLabel(" ");
    private final JPanel bottom = new JPanel(new BorderLayout());
    private boolean checkbox;
    private List<RowAction> actions = List.of();
    private final List<Integer> rowIds = new ArrayList<>();
    private int pageSize = 8;
    private int page;
    private List<String[]> all = List.of();

    public PagedTable(String[] columns) {
        setLayout(new BorderLayout());
        setOpaque(false);
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return checkbox && c == 0; }
            @Override public Class<?> getColumnClass(int c) {
                return checkbox && c == 0 ? Boolean.class : String.class;
            }
        };
        table = new JTable(model);
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Ui.BORDER_SOFT);
        table.setBackground(Ui.PANEL);
        table.setForeground(Ui.TEXT);
        table.setSelectionBackground(new Color(0x22, 0x2a, 0x33));
        table.setSelectionForeground(Ui.TEXT);
        table.setFont(table.getFont().deriveFont(14f));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(Ui.PANEL);
        table.getTableHeader().setForeground(Ui.MUTED);
        table.setFillsViewportHeight(true);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        sp.getViewport().setBackground(Ui.PANEL);
        add(sp, BorderLayout.CENTER);
        status.setForeground(Ui.SECONDARY);
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 4));
        bottom.add(status, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (actions.isEmpty()) return;
                int viewCol = table.columnAtPoint(e.getPoint());
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol != table.getColumnCount() - 1) return;
                Rectangle cell = table.getCellRect(viewRow, viewCol, true);
                int localX = e.getX() - cell.x;
                int idx = actionIndexAt(localX, cell.width);
                if (idx < 0) return;
                int modelRow = table.convertRowIndexToModel(viewRow);
                actions.get(idx).fire(modelRow);
            }
        });
    }

    /** Right-aligned action labels. A click left of that cluster does not fire a neighbour. */
    private int actionIndexAt(int localX, int cellWidth) {
        FontMetrics fm = table.getFontMetrics(table.getFont().deriveFont(Font.BOLD, 12f));
        int gap = 8;
        int[] widths = new int[actions.size()];
        int total = gap;
        for (int i = 0; i < actions.size(); i++) {
            widths[i] = fm.stringWidth(actions.get(i).label()) + gap;
            total += widths[i];
        }
        int x = Math.max(0, cellWidth - total);
        for (int i = 0; i < actions.size(); i++) {
            if (localX >= x && localX < x + widths[i]) return i;
            x += widths[i];
        }
        return -1;
    }

    public JTable table() { return table; }
    public DefaultTableModel model() { return model; }

    public void addCheckboxColumn() {
        if (checkbox) return;
        checkbox = true;
        java.util.Vector<Object> cols = new java.util.Vector<>();
        cols.add("");
        for (int i = 0; i < model.getColumnCount(); i++) cols.add(model.getColumnName(i));
        model.setColumnIdentifiers(cols);
    }

    public void addActionColumn(List<RowAction> rowActions) {
        this.actions = rowActions == null ? List.of() : List.copyOf(rowActions);
        model.addColumn("");
        int col = table.getColumnCount() - 1;
        table.getColumnModel().getColumn(col).setCellRenderer(new ActionRenderer());
        int width = 24;
        for (RowAction a : this.actions) width += a.label().length() * 8 + 18;
        width = Math.max(160, Math.min(width, 420));
        table.getColumnModel().getColumn(col).setMinWidth(width);
        table.getColumnModel().getColumn(col).setPreferredWidth(width);
    }

    public void setPageSize(int n) { this.pageSize = Math.max(1, n); }

    public void setRows(List<String[]> rows) {
        all = rows == null ? List.of() : rows;
        page = 0;
        renderPage();
    }

    public void setRowIds(List<Integer> ids) {
        rowIds.clear();
        if (ids != null) rowIds.addAll(ids);
    }

    /** Page-local model row to an index in the full id list. */
    public int absoluteRow(int modelRow) {
        return page * pageSize + modelRow;
    }

    public int idAt(int modelRow) {
        int absolute = absoluteRow(modelRow);
        if (absolute < 0 || absolute >= rowIds.size()) return 0;
        return rowIds.get(absolute);
    }

    public int selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) return 0;
        int modelRow = table.convertRowIndexToModel(row);
        if (!rowIds.isEmpty()) return idAt(modelRow);
        int col = checkbox ? 1 : 0;
        Object v = model.getValueAt(modelRow, col);
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    public List<Integer> checkedIds() {
        List<Integer> ids = new ArrayList<>();
        if (!checkbox) return ids;
        for (int r = 0; r < model.getRowCount(); r++) {
            Object v = model.getValueAt(r, 0);
            if (Boolean.TRUE.equals(v)) ids.add(idAt(r));
        }
        return ids;
    }

    private void renderPage() {
        model.setRowCount(0);
        int from = page * pageSize;
        int to = Math.min(all.size(), from + pageSize);
        for (int i = from; i < to; i++) {
            String[] src = all.get(i);
            Object[] row = new Object[model.getColumnCount()];
            int dataStart = checkbox ? 1 : 0;
            if (checkbox) row[0] = Boolean.FALSE;
            int copy = Math.min(src.length, row.length - dataStart - (actions.isEmpty() ? 0 : 1));
            for (int c = 0; c < copy; c++) row[dataStart + c] = src[c];
            if (!actions.isEmpty()) row[row.length - 1] = "";
            model.addRow(row);
        }
        if (all.isEmpty()) status.setText("No results");
        else status.setText("Showing " + (from + 1) + "–" + to + " of " + all.size());
        if (bottom.getComponentCount() > 1) bottom.remove(1);
        JPanel pager = Ui.row(6);
        JButton prev = Ui.secondary("◀");
        JButton next = Ui.secondary("▶");
        prev.setEnabled(page > 0);
        next.setEnabled(to < all.size());
        prev.addActionListener(e -> { if (page > 0) { page--; renderPage(); } });
        next.addActionListener(e -> { if ((page + 1) * pageSize < all.size()) { page++; renderPage(); } });
        pager.add(prev);
        pager.add(next);
        bottom.add(pager, BorderLayout.EAST);
        revalidate();
        repaint();
    }

    private final class ActionRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
            p.setOpaque(true);
            p.setBackground(sel ? t.getSelectionBackground() : Ui.PANEL);
            for (RowAction a : actions) {
                JLabel l = new JLabel(a.label());
                l.setForeground(a.color() == null ? Ui.TEXT : a.color());
                l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
                p.add(l);
            }
            return p;
        }
    }
}

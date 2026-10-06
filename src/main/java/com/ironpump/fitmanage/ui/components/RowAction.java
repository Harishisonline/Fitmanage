package com.ironpump.fitmanage.ui.components;

import java.awt.Color;
import java.util.function.Consumer;

/** Visual: see assets/ui-mockups/03-members.html. Used by PagedTable, MembersListPage, PaymentsListPage. */
public class RowAction {
    private final String label;
    private final Color color;
    private final Consumer<Integer> callback;

    public RowAction(String label, Color color, Consumer<Integer> callback) {
        this.label = label;
        this.color = color;
        this.callback = callback;
    }

    public String label() { return label; }
    public Color color() { return color; }
    public void fire(int modelRow) { if (callback != null) callback.accept(modelRow); }
}

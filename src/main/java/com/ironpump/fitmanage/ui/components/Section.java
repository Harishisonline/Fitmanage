package com.ironpump.fitmanage.ui.components;

import javax.swing.*;
import java.awt.*;

/** Visual: see assets/ui-mockups/04-member-form.html. Used by MemberFormPage, PlanFormPage, TrainerFormPage, UserFormPage. */
public class Section extends JLabel {
    public Section(String text) {
        super(text == null ? "" : text.toUpperCase());
        setForeground(Ui.SECONDARY);
        setFont(getFont().deriveFont(Font.BOLD, 11f));
        setBorder(BorderFactory.createEmptyBorder(12, 0, 6, 0));
    }
}

package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.Plan;
import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.service.PlanService;
import com.ironpump.fitmanage.service.TrainerService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Picks an active plan and an optional trainer. */
public final class PlanChoiceDialog {
    public record Choice(int planId, Integer trainerId) {}

    private PlanChoiceDialog() {}

    public static Choice ask(Component parent, String title) {
        List<Plan> plans = new PlanService().listActive();
        if (plans.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Create an active plan first.", title, JOptionPane.WARNING_MESSAGE);
            return null;
        }
        JComboBox<Plan> planBox = new JComboBox<>(plans.toArray(Plan[]::new));
        planBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                Plan p = (Plan) value;
                String text = p == null ? "" : p.getName() + " · " + p.getDurationDays() + " days · Rs. " + p.getPrice();
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        List<Trainer> trainers = new TrainerService().listActive();
        JComboBox<Object> trainerBox = new JComboBox<>();
        trainerBox.addItem("No trainer");
        for (Trainer t : trainers) trainerBox.addItem(t);
        trainerBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                String text = value instanceof Trainer t ? t.getName() : String.valueOf(value);
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Plan"));
        form.add(planBox);
        form.add(new JLabel("Trainer"));
        form.add(trainerBox);
        int ok = JOptionPane.showConfirmDialog(parent, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return null;
        Plan plan = (Plan) planBox.getSelectedItem();
        if (plan == null) return null;
        Object trainer = trainerBox.getSelectedItem();
        Integer trainerId = trainer instanceof Trainer t ? t.getTrainerId() : null;
        return new Choice(plan.getPlanId(), trainerId);
    }
}

package com.ironpump.fitmanage.ui.plans;

import com.ironpump.fitmanage.model.Plan;
import com.ironpump.fitmanage.service.PlanService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class PlanFormPage extends BasePage {
    private final PlanService service = new PlanService();
    private final boolean isEdit;
    private int planId;
    private final JTextField name = new JTextField();
    private final JTextField days = new JTextField();
    private final JTextField price = new JTextField();
    private final JCheckBox active = new JCheckBox("Active", true);
    private final JTextArea description = new JTextArea(4, 30);
    private final JLabel status = new JLabel(" ");

    public static PlanFormPage newForCreate() { return new PlanFormPage(false, 0); }
    public static PlanFormPage newForEdit()   { return new PlanFormPage(true,  0); }

    private PlanFormPage(boolean isEdit, int unused) {
        this.isEdit = isEdit;
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;

        JLabel h1 = new JLabel(isEdit ? "Edit plan" : "New plan");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(h1, c); c.gridwidth = 1;

        addRow(form, c, row++, "Name *", name);
        addRow(form, c, row++, "Duration (days) *", days);
        addRow(form, c, row++, "Price (Rs.) *", price);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(active, c);
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        JLabel l = new JLabel("Description");
        l.setForeground(new Color(0x8b, 0x97, 0xa4));
        form.add(l, c);
        c.gridy = row++;
        form.add(new JScrollPane(description), c);

        JButton save = new JButton(isEdit ? "Save" : "Create");
        save.setBackground(new Color(0x22, 0xC5, 0x5E));
        save.setForeground(new Color(0x06, 0x2b, 0x15));
        save.setOpaque(true);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(save, c);
        save.addActionListener(e -> save());

        c.gridy = row;
        status.setForeground(new Color(0xef, 0x44, 0x44));
        form.add(status, c);

        add(new JScrollPane(form), BorderLayout.CENTER);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setForeground(new Color(0x8b, 0x97, 0xa4));
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(l, c);
        c.gridx = 1; c.weightx = 1;
        form.add(field, c);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (isEdit && ctx != null && ctx.has("id")) {
            planId = ctx.get("id");
            try {
                Plan p = service.findById(planId);
                name.setText(p.getName());
                days.setText(String.valueOf(p.getDurationDays()));
                price.setText(p.getPrice().toPlainString());
                active.setSelected(p.isActive());
                description.setText(p.getDescription());
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    private void save() {
        try {
            Plan p = isEdit ? service.findById(planId) : new Plan();
            p.setName(name.getText().trim());
            p.setDurationDays(Integer.parseInt(days.getText().trim()));
            p.setPrice(new BigDecimal(price.getText().trim()));
            p.setActive(active.isSelected());
            p.setDescription(description.getText().trim());
            if (isEdit) { service.update(p); Router.get().navigateTo(Route.PLANS_LIST); }
            else { service.create(p); Router.get().navigateTo(Route.PLANS_LIST); }
        } catch (Exception e) { status.setText(e.getMessage()); }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Plans", () -> Router.get().navigateTo(Route.PLANS_LIST)),
                new BreadcrumbItem(isEdit ? "Edit" : "New", () -> {}));
    }
}

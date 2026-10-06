package com.ironpump.fitmanage.ui.trainers;

import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.service.TrainerService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TrainerFormPage extends BasePage {
    private final TrainerService service = new TrainerService();
    private final boolean isEdit;
    private int trainerId;
    private final JTextField name = new JTextField();
    private final JTextField spec = new JTextField();
    private final JTextField phone = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField joinDate = new JTextField();
    private final JTextField dob = new JTextField();
    private final JComboBox<String> gender = new JComboBox<>(new String[]{"", "MALE", "FEMALE", "OTHER"});
    private final JTextField salary = new JTextField();
    private final JLabel preview = new JLabel(" ");
    private final JCheckBox active = new JCheckBox("Active", true);
    private final JTextArea notes = new JTextArea(3, 20);
    private final JTextArea availability = new JTextArea(2, 20);
    private final JLabel status = new JLabel(" ");

    public static TrainerFormPage newForCreate() { return new TrainerFormPage(false, 0); }
    public static TrainerFormPage newForEdit()   { return new TrainerFormPage(true,  0); }

    private TrainerFormPage(boolean isEdit, int unused) {
        this.isEdit = isEdit;
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int row = 0;
        JLabel h1 = new JLabel(isEdit ? "Edit trainer" : "New trainer");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(h1, c); c.gridwidth = 1;
        addRow(form, c, row++, "Name *", name);
        addRow(form, c, row++, "Specialization", spec);
        addRow(form, c, row++, "Phone *", phone);
        addRow(form, c, row++, "Email", email);
        addRow(form, c, row++, "Date of birth (YYYY-MM-DD)", dob);
        addRow(form, c, row++, "Gender", gender);
        addRow(form, c, row++, "Hire date (YYYY-MM-DD) *", joinDate);
        addRow(form, c, row++, "Monthly salary (Rs.)", salary);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(active, c);
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        JLabel l = new JLabel("Availability");
        l.setForeground(new Color(0x8b, 0x97, 0xa4));
        form.add(l, c);
        c.gridy = row++;
        form.add(new JScrollPane(availability), c);
        c.gridy = row++;
        l = new JLabel("Notes");
        l.setForeground(new Color(0x8b, 0x97, 0xa4));
        form.add(l, c);
        c.gridy = row++;
        form.add(new JScrollPane(notes), c);

        JButton save = new JButton(isEdit ? "Save" : "Create");
        save.setBackground(new Color(0x22, 0xC5, 0x5E));
        save.setForeground(new Color(0x06, 0x2b, 0x15));
        save.setOpaque(true);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        form.add(save, c);
        save.addActionListener(e -> save());

        c.gridy = row++;
        status.setForeground(new Color(0xef, 0x44, 0x44));
        form.add(status, c);
        preview.setForeground(new Color(0x8b, 0x97, 0xa4));
        c.gridy = row;
        form.add(preview, c);
        javax.swing.event.DocumentListener live = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshPreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshPreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshPreview(); }
        };
        name.getDocument().addDocumentListener(live);
        phone.getDocument().addDocumentListener(live);
        salary.getDocument().addDocumentListener(live);
        spec.getDocument().addDocumentListener(live);

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
            trainerId = ctx.get("id");
            try {
                Trainer t = service.findById(trainerId);
                name.setText(t.getName());
                spec.setText(t.getSpecialization());
                phone.setText(t.getPhone());
                email.setText(t.getEmail());
                joinDate.setText(t.getJoinDate() == null ? "" : t.getJoinDate().toString());
                dob.setText(t.getDob() == null ? "" : t.getDob().toString());
                gender.setSelectedItem(t.getGender() == null ? "" : t.getGender().name());
                salary.setText(t.getSalary() == null ? "" : t.getSalary().toPlainString());
                active.setSelected(t.isActive());
                notes.setText(t.getNotes());
                availability.setText(t.getAvailability());
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        } else {
            joinDate.setText(LocalDate.now().toString());
        }
    }

    private void refreshPreview() {
        preview.setText("Preview  ·  " + name.getText().trim()
                + "  ·  " + phone.getText().trim()
                + "  ·  Rs. " + (salary.getText().isBlank() ? "0" : salary.getText().trim())
                + "/mo  ·  " + spec.getText().trim());
    }

    private void save() {
        try {
            Trainer t = isEdit ? service.findById(trainerId) : new Trainer();
            t.setName(name.getText().trim());
            t.setSpecialization(spec.getText().trim());
            t.setPhone(phone.getText().trim());
            t.setEmail(email.getText().trim());
            t.setJoinDate(LocalDate.parse(joinDate.getText().trim()));
            t.setHireDate(t.getJoinDate());
            if (!dob.getText().isBlank()) t.setDob(LocalDate.parse(dob.getText().trim()));
            String g = String.valueOf(gender.getSelectedItem());
            if (g != null && !g.isBlank()) t.setGender(Trainer.Gender.valueOf(g));
            t.setSalary(salary.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(salary.getText().trim()));
            t.setActive(active.isSelected());
            t.setNotes(notes.getText().trim());
            t.setAvailability(availability.getText().trim());

            if (isEdit) { service.update(t); Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", t.getTrainerId())); }
            else { int id = service.create(t); Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", id)); }
        } catch (Exception e) { status.setText(e.getMessage()); }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Trainers", () -> Router.get().navigateTo(Route.TRAINERS_LIST)),
                new BreadcrumbItem(isEdit ? "Edit" : "New", () -> {}));
    }
}

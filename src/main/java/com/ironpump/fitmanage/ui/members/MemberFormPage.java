package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.Member;
import com.ironpump.fitmanage.model.Payment;
import com.ironpump.fitmanage.model.Plan;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.service.PlanService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.Card;
import com.ironpump.fitmanage.ui.components.Section;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/** Visual: see assets/ui-mockups/04-member-form.html. */
public class MemberFormPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(MemberFormPage.class);
    private final boolean editMode;
    private int memberId;
    private final MemberService members = new MemberService();
    private final PlanService plans = new PlanService();
    private final JLabel status = new JLabel(" ");
    private final JTextField first = new JTextField();
    private final JTextField last = new JTextField();
    private final JSpinner dob = new JSpinner(new SpinnerDateModel());
    private final JRadioButton male = new JRadioButton("Male", true);
    private final JRadioButton female = new JRadioButton("Female");
    private final JRadioButton other = new JRadioButton("Other");
    private final JTextField phone = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextArea address = new JTextArea(2, 20);
    private final JTextField emergencyName = new JTextField();
    private final JTextField emergencyPhone = new JTextField();
    private final JTextField height = new JTextField();
    private final JTextField weight = new JTextField();
    private final JTextArea medical = new JTextArea(2, 20);
    private final JLabel heightError = new JLabel(" ");
    private final JLabel weightError = new JLabel(" ");
    private final JComboBox<String> planBox = new JComboBox<>();
    private final JCheckBox recordPay = new JCheckBox("Record initial payment");
    private final JTextField payAmount = new JTextField();
    private final JComboBox<String> payMethod = new JComboBox<>(new String[]{"CASH", "UPI", "CARD", "BANK_TRANSFER", "CHEQUE"});
    private final JTextField payRef = new JTextField();
    private final JPanel membershipSection = new JPanel();
    private final JLabel preview = new JLabel();
    private final JLabel validation = new JLabel();
    private List<Plan> planList = List.of();

    public static MemberFormPage newForCreate() { return new MemberFormPage(false); }
    public static MemberFormPage newForEdit() { return new MemberFormPage(true); }

    private MemberFormPage(boolean editMode) {
        this.editMode = editMode;
        setLayout(new BorderLayout());
        setBackground(Ui.BG);
        dob.setEditor(new JSpinner.DateEditor(dob, "dd/MM/yyyy"));
        dob.setValue(Date.from(LocalDate.now().minusYears(25).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        ButtonGroup g = new ButtonGroup();
        g.add(male); g.add(female); g.add(other);
        for (JComponent c : new JComponent[]{first, last, phone, email, emergencyName, emergencyPhone, height, weight, payAmount, payRef}) {
            Ui.styleField(c);
        }
        heightError.setForeground(Ui.RED);
        weightError.setForeground(Ui.RED);
        status.setForeground(Ui.AMBER);
        DocumentListener live = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshSide(); }
            public void removeUpdate(DocumentEvent e) { refreshSide(); }
            public void changedUpdate(DocumentEvent e) { refreshSide(); }
        };
        for (JTextField f : new JTextField[]{first, last, phone, email, height, weight, payAmount}) f.getDocument().addDocumentListener(live);
        planBox.addActionListener(e -> refreshSide());
    }

    @Override public void onEnter(RouteContext ctx) {
        if (editMode && ctx != null && ctx.has("id")) memberId = ctx.get("id");
        removeAll();
        JPanel page = Ui.page();
        page.setLayout(new BorderLayout(16, 12));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.h1(editMode ? "Edit member" : "Add new member"));
        titles.add(Ui.sub(editMode
                ? "Update personal details. Plan changes and payments live on the member page."
                : "Fill in the details below. The system will create the member profile and the initial subscription in one step."));
        page.add(titles, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.add(new JScrollPane(formCard()));
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        Card live = new Card("Live preview");
        preview.setForeground(Ui.TEXT);
        live.add(preview, BorderLayout.CENTER);
        Card checks = new Card("Validation");
        validation.setForeground(Ui.SECONDARY);
        checks.add(validation, BorderLayout.CENTER);
        side.add(live);
        side.add(Box.createVerticalStrut(12));
        side.add(checks);
        columns.add(side);
        page.add(columns, BorderLayout.CENTER);

        JPanel footer = Ui.row(8);
        JButton cancel = Ui.secondary("Cancel");
        cancel.addActionListener(e -> Router.get().navigateTo(editMode ? Route.MEMBER_DETAIL : Route.MEMBERS_LIST,
                editMode ? RouteContext.of("id", memberId) : RouteContext.empty()));
        JButton save = Ui.primary(editMode ? "Save changes" : "Save member");
        save.addActionListener(e -> SwingUtilities.invokeLater(this::save));
        footer.add(status);
        footer.add(cancel);
        footer.add(save);
        page.add(footer, BorderLayout.SOUTH);
        add(page);
        load();
        revalidate();
        repaint();
    }

    private JPanel formCard() {
        Card card = new Card("Member details");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        form.add(new JLabel("All fields marked * are required."), c);
        c.gridy++; form.add(new Section("Personal information"), c);
        c.gridwidth = 1;
        addPair(form, c, "First name *", first, "Last name *", last);
        JPanel gender = Ui.row(8);
        male.setOpaque(false); female.setOpaque(false); other.setOpaque(false);
        male.setForeground(Ui.TEXT); female.setForeground(Ui.TEXT); other.setForeground(Ui.TEXT);
        gender.add(male); gender.add(female); gender.add(other);
        addPair(form, c, "Date of birth *", dob, "Gender *", gender);
        addPair(form, c, "Phone *", phone, "Email", email);
        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        form.add(Ui.muted("Used for check-in lookup. Must be unique."), c);
        c.gridy++; form.add(labeledArea("Address", address), c);
        c.gridwidth = 1;
        addPair(form, c, "Emergency name", emergencyName, "Emergency phone", emergencyPhone);
        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        form.add(new Section("Physical info (optional)"), c);
        c.gridwidth = 1;
        addPair(form, c, "Height (cm)", height, "Weight (kg)", weight);
        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        JPanel errs = new JPanel(new GridLayout(1, 2));
        errs.setOpaque(false);
        errs.add(heightError); errs.add(weightError);
        form.add(errs, c);
        c.gridy++; form.add(labeledArea("Medical notes", medical), c);
        membershipSection.setOpaque(false);
        membershipSection.setLayout(new GridBagLayout());
        GridBagConstraints m = new GridBagConstraints();
        m.insets = new Insets(4, 6, 4, 6);
        m.fill = GridBagConstraints.HORIZONTAL;
        m.weightx = 1; m.gridx = 0; m.gridy = 0; m.gridwidth = 2;
        membershipSection.add(new Section("Membership"), m);
        m.gridy++; membershipSection.add(planBox, m);
        recordPay.setOpaque(false); recordPay.setForeground(Ui.TEXT);
        m.gridy++; membershipSection.add(recordPay, m);
        m.gridwidth = 1; m.gridy++;
        membershipSection.add(payAmount, m);
        m.gridx = 1; membershipSection.add(payMethod, m);
        m.gridx = 0; m.gridy++; m.gridwidth = 2;
        membershipSection.add(payRef, m);
        c.gridy++; form.add(membershipSection, c);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private void addPair(JPanel form, GridBagConstraints c, String l1, JComponent f1, String l2, JComponent f2) {
        c.gridwidth = 1;
        c.gridx = 0; c.gridy++;
        form.add(stack(l1, f1), c);
        c.gridx = 1;
        form.add(stack(l2, f2), c);
    }

    private JPanel stack(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label.toUpperCase());
        l.setForeground(Ui.SECONDARY);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 10f));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel labeledArea(String label, JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        Ui.styleField(area);
        return stack(label, new JScrollPane(area));
    }

    private void load() {
        planList = plans.listActive();
        planBox.removeAllItems();
        for (Plan p : planList) planBox.addItem(p.getName() + " · Rs. " + p.getPrice());
        membershipSection.setVisible(!editMode);
        if (!editMode) {
            refreshSide();
            return;
        }
        try {
            Member m = members.findById(memberId);
            first.setText(m.getFirstName());
            last.setText(m.getLastName());
            if (m.getDob() != null) dob.setValue(Date.from(m.getDob().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            if (m.getGender() == Member.Gender.FEMALE) female.setSelected(true);
            else if (m.getGender() == Member.Gender.OTHER) other.setSelected(true);
            else male.setSelected(true);
            phone.setText(m.getPhone());
            email.setText(m.getEmail() == null ? "" : m.getEmail());
            address.setText(m.getAddress() == null ? "" : m.getAddress());
            emergencyName.setText(m.getEmergencyName() == null ? "" : m.getEmergencyName());
            emergencyPhone.setText(m.getEmergencyPhone() == null ? "" : m.getEmergencyPhone());
            height.setText(m.getHeightCm() == null ? "" : m.getHeightCm().toString());
            weight.setText(m.getWeightKg() == null ? "" : m.getWeightKg().stripTrailingZeros().toPlainString());
            medical.setText(m.getMedicalNotes() == null ? "" : m.getMedicalNotes());
        } catch (Exception ex) {
            log.error("Load member failed", ex);
            status.setText(ex.getMessage());
        }
        refreshSide();
    }

    private LocalDate dobValue() {
        Date d = (Date) dob.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void refreshSide() {
        Plan plan = planList.isEmpty() || planBox.getSelectedIndex() < 0 ? null : planList.get(planBox.getSelectedIndex());
        LocalDate start = LocalDate.now();
        LocalDate end = plan == null ? null : start.plusDays(plan.getDurationDays());
        String id = editMode ? "existing" : "FM-XXXX";
        preview.setText("<html>Member ID<br><b>" + id + "</b><br>Name<br><b>"
                + esc(first.getText()) + " " + esc(last.getText()) + "</b><br>Phone<br><b>"
                + esc(phone.getText()) + "</b><br>Plan<br><b>"
                + (plan == null || editMode ? "—" : esc(plan.getName())) + "</b><br>Start<br><b>"
                + (editMode ? "—" : DateUtils.format(start)) + "</b><br>Expiry<br><b>"
                + (end == null || editMode ? "—" : DateUtils.format(end)) + "</b><br>Amount due<br><b>"
                + (plan == null || editMode ? "—" : "Rs. " + plan.getPrice()) + "</b></html>");
        StringBuilder v = new StringBuilder("<html>");
        v.append(ok(!first.getText().isBlank(), "First name")).append("<br>");
        v.append(ok(!last.getText().isBlank(), "Last name")).append("<br>");
        v.append(ok(!phone.getText().isBlank(), "Phone")).append("<br>");
        v.append(ok(editMode || plan != null, "Plan")).append("</html>");
        validation.setText(v.toString());
    }

    private static String ok(boolean pass, String label) {
        return (pass ? "<span style='color:#22c55e'>✓ " : "<span style='color:#ef4444'>• ") + label + "</span>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;");
    }

    private void save() {
        heightError.setText(" ");
        weightError.setText(" ");
        status.setText(" ");
        try {
            try { dob.commitEdit(); }
            catch (java.text.ParseException pe) {
                fail("Date of birth must be a real date, written as dd/MM/yyyy.");
                return;
            }
            Member m = editMode ? members.findById(memberId) : new Member();
            m.setFirstName(first.getText().trim());
            m.setLastName(last.getText().trim());
            LocalDate born = dobValue();
            if (!born.isBefore(LocalDate.now())) {
                fail("Date of birth must be in the past.");
                return;
            }
            m.setDob(born);
            m.setGender(female.isSelected() ? Member.Gender.FEMALE : other.isSelected() ? Member.Gender.OTHER : Member.Gender.MALE);
            m.setPhone(phone.getText().trim());
            m.setEmail(blank(email.getText()));
            m.setAddress(blank(address.getText()));
            m.setEmergencyName(blank(emergencyName.getText()));
            m.setEmergencyPhone(blank(emergencyPhone.getText()));
            m.setMedicalNotes(blank(medical.getText()));
            if (!height.getText().isBlank()) {
                try { m.setHeightCm(Integer.parseInt(height.getText().trim())); }
                catch (NumberFormatException nfe) { heightError.setText("Height must be a whole number."); return; }
            } else m.setHeightCm(null);
            if (!weight.getText().isBlank()) {
                try { m.setWeightKg(new BigDecimal(weight.getText().trim())); }
                catch (NumberFormatException nfe) { weightError.setText("Weight must be a number."); return; }
            } else m.setWeightKg(null);
            if (editMode) {
                members.update(m);
                Router.get().navigateTo(Route.MEMBER_DETAIL, RouteContext.of("id", memberId));
                return;
            }
            if (planList.isEmpty() || planBox.getSelectedIndex() < 0) {
                fail("Create an active plan before adding a member.");
                return;
            }
            Plan plan = planList.get(planBox.getSelectedIndex());
            BigDecimal paid = null;
            Payment.Method method = Payment.Method.CASH;
            if (recordPay.isSelected()) {
                if (payAmount.getText().isBlank()) {
                    fail("Enter the initial payment amount, or uncheck Record initial payment.");
                    return;
                }
                paid = new BigDecimal(payAmount.getText().trim());
                method = Payment.Method.valueOf(String.valueOf(payMethod.getSelectedItem()));
            }
            int id = members.register(m, plan.getPlanId(), paid, method, blank(payRef.getText()));
            if (id <= 0) {
                fail("The member was not saved.");
                return;
            }
            Router.get().navigateTo(Route.MEMBER_DETAIL, RouteContext.of("id", id));
        } catch (Exception ex) {
            log.error("Save member failed", ex);
            fail(ex.getMessage() == null ? "Could not save the member." : ex.getMessage());
        }
    }

    private void fail(String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, editMode ? "Edit member" : "Add member", JOptionPane.WARNING_MESSAGE);
    }

    private static String blank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Override public List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Members", () -> Router.get().navigateTo(Route.MEMBERS_LIST)),
                new BreadcrumbItem(editMode ? "Edit" : "Add", () -> {}));
    }
}

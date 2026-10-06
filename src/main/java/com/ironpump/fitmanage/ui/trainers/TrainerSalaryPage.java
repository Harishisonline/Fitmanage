package com.ironpump.fitmanage.ui.trainers;

import com.ironpump.fitmanage.model.Payment;
import com.ironpump.fitmanage.model.TrainerPayment;
import com.ironpump.fitmanage.service.TrainerPaymentService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class TrainerSalaryPage extends BasePage {
    private final TrainerPaymentService service = new TrainerPaymentService();
    private final JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
    private final PagedTable table = new PagedTable(new String[]{"Date paid", "Period", "Amount", "Method", "Note"});
    private int trainerId;

    public TrainerSalaryPage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel h = new JLabel("Salary");
        h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
        h.setForeground(new Color(0xe6, 0xed, 0xf3));
        head.add(h, BorderLayout.WEST);
        JButton pay = new JButton("+ Record payout");
        pay.setBackground(new Color(0x22, 0xC5, 0x5E));
        pay.setForeground(new Color(0x06, 0x2b, 0x15));
        pay.setOpaque(true);
        pay.addActionListener(e -> recordPayout());
        head.add(pay, BorderLayout.EAST);
        kpiRow.setOpaque(false);
        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(kpiRow, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }

    private void recordPayout() {
        if (trainerId <= 0) return;
        JTextField amount = new JTextField();
        JComboBox<String> method = new JComboBox<>(new String[]{"CASH", "UPI", "CARD", "BANK_TRANSFER", "CHEQUE"});
        JTextField notes = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Amount (Rs.)")); form.add(amount);
        form.add(new JLabel("Method")); form.add(method);
        form.add(new JLabel("Note")); form.add(notes);
        int ok = JOptionPane.showConfirmDialog(this, form, "Record payout", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            LocalDate today = LocalDate.now();
            TrainerPayment tp = new TrainerPayment();
            tp.setTrainerId(trainerId);
            tp.setAmount(new BigDecimal(amount.getText().trim()));
            tp.setMethod(Payment.Method.valueOf(String.valueOf(method.getSelectedItem())));
            tp.setPeriodStart(today.withDayOfMonth(1));
            tp.setPeriodEnd(today);
            tp.setPaidOn(today);
            tp.setNotes(notes.getText().isBlank() ? null : notes.getText().trim());
            service.record(tp);
            SwingUtilities.invokeLater(() -> onEnter(RouteContext.of("id", trainerId)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? "Could not record the payout." : ex.getMessage(),
                    "Record payout", JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            trainerId = ctx.get("id");
            try {
                List<TrainerPayment> rows = service.listForTrainer(trainerId, LocalDate.now().getYear());
                kpiRow.removeAll();
                kpiRow.add(new KpiCard("This month", "Rs. " + service.sumMonth(trainerId, LocalDate.now().getYear(), LocalDate.now().getMonthValue())));
                kpiRow.add(new KpiCard("This year",  "Rs. " + service.sumYear(trainerId, LocalDate.now().getYear())));
                kpiRow.add(new KpiCard("Payments",   String.valueOf(rows.size())));
                kpiRow.add(new KpiCard("Avg / mo",   rows.isEmpty() ? "—" : "Rs. " + BigDecimal.valueOf(service.sumYear(trainerId, LocalDate.now().getYear()).doubleValue() / 12).setScale(0, java.math.RoundingMode.HALF_UP)));
                kpiRow.revalidate();

                List<String[]> data = rows.stream().map(t -> new String[]{
                        DateUtils.format(t.getPaidOn()),
                        DateUtils.format(t.getPeriodStart()) + " → " + DateUtils.format(t.getPeriodEnd()),
                        "Rs. " + t.getAmount(),
                        t.getMethod() == null ? "—" : t.getMethod().name(),
                        t.getNotes() == null ? "—" : t.getNotes()
                }).collect(Collectors.toList());
                table.setRows(data);
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Trainers", () -> Router.get().navigateTo(Route.TRAINERS_LIST)),
                new BreadcrumbItem("#" + trainerId, () -> Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", trainerId))),
                new BreadcrumbItem("Salary", () -> {}));
    }
}

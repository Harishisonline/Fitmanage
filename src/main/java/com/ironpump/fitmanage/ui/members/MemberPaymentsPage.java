package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.Payment;
import com.ironpump.fitmanage.service.PaymentService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.payments.PaymentFormDialog;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class MemberPaymentsPage extends BasePage {
    private final PaymentService service = new PaymentService();
    private final JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
    private final PagedTable table = new PagedTable(new String[]{"Date", "Receipt", "Method", "Amount", "Status"});
    private int memberId;

    public MemberPaymentsPage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel h = new JLabel("Payments");
        h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
        h.setForeground(new Color(0xe6, 0xed, 0xf3));
        top.add(h, BorderLayout.WEST);
        JButton add = new JButton("+ Record payment");
        add.setBackground(new Color(0x22, 0xC5, 0x5E));
        add.setForeground(new Color(0x06, 0x2b, 0x15));
        add.setOpaque(true);
        add.addActionListener(e -> {
            if (memberId <= 0) {
                JOptionPane.showMessageDialog(this, "Open a member before recording a payment.", "Record payment", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new PaymentFormDialog(SwingUtilities.getWindowAncestor(this), memberId, null,
                    () -> SwingUtilities.invokeLater(this::refresh)).setVisible(true);
        });
        top.add(add, BorderLayout.EAST);
        kpiRow.setOpaque(false);
        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(kpiRow, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            memberId = ctx.get("id");
            refresh();
        }
    }

    private void refresh() {
        try {
            List<Payment> rows = service.listForMember(memberId);
            List<String[]> data = rows.stream().map(p -> new String[]{
                    DateUtils.format(p.getPaymentDate()),
                    p.getReceiptNo(),
                    p.getMethod().name(),
                    "Rs. " + p.getAmount(),
                    p.getPaymentType() == com.ironpump.fitmanage.model.Payment.Type.REFUND ? "Refund" : "Paid"
            }).collect(Collectors.toList());
            table.setRows(data);

            java.math.BigDecimal total = rows.stream().map(Payment::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal outstanding = new com.ironpump.fitmanage.service.MembershipService().outstandingForMember(memberId);
            java.math.BigDecimal avg = rows.isEmpty() ? java.math.BigDecimal.ZERO
                    : total.divide(java.math.BigDecimal.valueOf(rows.size()), 0, java.math.RoundingMode.HALF_UP);
            kpiRow.removeAll();
            kpiRow.add(new KpiCard("Lifetime paid", "Rs. " + total));
            kpiRow.add(new KpiCard("Receipts", String.valueOf(rows.size())));
            kpiRow.add(new KpiCard("Outstanding", "Rs. " + outstanding, outstanding.signum() > 0 ? "Due" : "All current"));
            kpiRow.add(new KpiCard("Avg receipt", rows.isEmpty() ? "—" : "Rs. " + avg));
            kpiRow.revalidate();
        } catch (Exception e) { GlobalExceptionHandler.handle(e); }
    }

    private String memberLabel() {
        try { return new com.ironpump.fitmanage.service.MemberService().findById(memberId).getFullName(); }
        catch (Exception e) { return "Member"; }
    }

    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Members", () -> Router.get().navigateTo(Route.MEMBERS_LIST)),
                new BreadcrumbItem(memberLabel(), () -> Router.get().navigateTo(Route.MEMBER_DETAIL, RouteContext.of("id", memberId))),
                new BreadcrumbItem("Payments", () -> {}));
    }
}

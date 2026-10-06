package com.ironpump.fitmanage.ui.payments;

import com.ironpump.fitmanage.App;
import com.ironpump.fitmanage.model.Member;
import com.ironpump.fitmanage.model.Payment;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.service.MembershipService;
import com.ironpump.fitmanage.service.PaymentService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.KpiCard;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.components.RowAction;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.CsvExporter;
import com.ironpump.fitmanage.util.CurrencyUtils;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;
import com.ironpump.fitmanage.util.ReceiptRenderer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentsListPage extends BasePage {
    private final PaymentService service = new PaymentService();
    private final MemberService members = new MemberService();
    private final MembershipService memberships = new MembershipService();
    private final PagedTable table = new PagedTable(new String[]{
            "Date", "Receipt", "Member", "For", "Method", "Reference", "Amount", "Status"});
    private final JTextField search = new JTextField(16);
    private final JComboBox<String> method = new JComboBox<>(new String[]{"All methods", "CASH", "UPI", "CARD", "BANK_TRANSFER", "CHEQUE"});
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"All status", "Paid", "Refund"});
    private final JComboBox<String> window = new JComboBox<>(new String[]{"All time", "Today", "Last 7 days", "Last 30 days"});
    private final JLabel subtitle = Ui.sub(" ");
    private final JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 0));
    private List<Payment> loaded = List.of();
    private final Map<Integer, String> names = new HashMap<>();

    public PaymentsListPage() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Ui.h1("Payments"));
        titles.add(subtitle);
        head.add(titles, BorderLayout.WEST);
        JPanel actions = Ui.row(8);
        Ui.styleField(search);
        search.addActionListener(e -> render());
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { render(); }
            public void removeUpdate(DocumentEvent e) { render(); }
            public void changedUpdate(DocumentEvent e) { render(); }
        });
        search.setToolTipText("Search member, receipt, or reference");
        actions.add(search);
        JButton csv = Ui.secondary("CSV");
        csv.addActionListener(e -> exportCsv());
        actions.add(csv);
        JButton add = Ui.primary("+ Record payment");
        add.addActionListener(e -> record());
        actions.add(add);
        head.add(actions, BorderLayout.EAST);

        kpis.setOpaque(false);
        JPanel filters = Ui.row(8);
        filters.add(method);
        filters.add(kind);
        filters.add(window);
        method.addActionListener(e -> render());
        kind.addActionListener(e -> render());
        window.addActionListener(e -> render());

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(kpis, BorderLayout.CENTER);
        north.add(filters, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        table.addActionColumn(List.of(
                new RowAction("View", Ui.SECONDARY, this::view),
                new RowAction("Print", Ui.BLUE, this::print)
        ));
        add(table, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        refresh();
        if (ctx != null && Boolean.TRUE.equals(ctx.get("record"))) SwingUtilities.invokeLater(this::record);
    }

    private void refresh() {
        try {
            loaded = service.listAll(500, 0);
            names.clear();
            BigDecimal outstanding = BigDecimal.ZERO;
            int dueMembers = 0;
            for (Member m : members.list(500, 0, null, null, null, null)) {
                names.put(m.getMemberId(), m.getFirstName() + " " + m.getLastName());
                BigDecimal due = memberships.outstandingForMember(m.getMemberId());
                if (due != null && due.signum() > 0) {
                    outstanding = outstanding.add(due);
                    dueMembers++;
                }
            }
            kpis.removeAll();
            kpis.add(new KpiCard("Today", CurrencyUtils.format(service.totalToday(), "INR"), "Receipts today"));
            kpis.add(new KpiCard("Last 7 days", CurrencyUtils.format(service.totalLastDays(7), "INR")));
            kpis.add(new KpiCard("This month", CurrencyUtils.format(service.totalThisMonth(), "INR")));
            kpis.add(new KpiCard("Outstanding", CurrencyUtils.format(outstanding, "INR"), dueMembers + " members"));
            subtitle.setText("All recorded payments · " + service.count() + " receipts lifetime");
            render();
        } catch (Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    private void render() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        String methodPick = String.valueOf(method.getSelectedItem());
        String kindPick = String.valueOf(kind.getSelectedItem());
        String windowPick = String.valueOf(window.getSelectedItem());
        LocalDate today = LocalDate.now();
        List<String[]> rows = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (Payment p : loaded) {
            if (!"All methods".equals(methodPick) && (p.getMethod() == null || !p.getMethod().name().equals(methodPick))) continue;
            boolean refund = p.getPaymentType() == Payment.Type.REFUND || (p.getAmount() != null && p.getAmount().signum() < 0);
            if ("Paid".equals(kindPick) && refund) continue;
            if ("Refund".equals(kindPick) && !refund) continue;
            if (p.getPaymentDate() != null) {
                LocalDate d = p.getPaymentDate().toLocalDate();
                if ("Today".equals(windowPick) && !d.equals(today)) continue;
                if ("Last 7 days".equals(windowPick) && d.isBefore(today.minusDays(6))) continue;
                if ("Last 30 days".equals(windowPick) && d.isBefore(today.minusDays(29))) continue;
            }
            String member = names.getOrDefault(p.getMemberId(), "Member #" + p.getMemberId());
            String blob = (member + " " + p.getReceiptNo() + " " + (p.getReference() == null ? "" : p.getReference())).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !blob.contains(q)) continue;
            rows.add(new String[]{
                    p.getPaymentDate() == null ? "—" : DateUtils.format(p.getPaymentDate()),
                    p.getReceiptNo(),
                    member,
                    p.getNotes() == null || p.getNotes().isBlank() ? (refund ? "Refund" : "Membership") : p.getNotes(),
                    p.getMethod() == null ? "—" : p.getMethod().name(),
                    p.getReference() == null || p.getReference().isBlank() ? "—" : p.getReference(),
                    p.getAmount() == null ? "—" : CurrencyUtils.format(p.getAmount(), "INR"),
                    refund ? "Refund" : "Paid"
            });
            ids.add(p.getPaymentId());
        }
        table.setRowIds(ids);
        table.setRows(rows);
    }

    private Payment paymentAt(int row) {
        int id = table.idAt(row);
        for (Payment p : loaded) if (p.getPaymentId() == id) return p;
        return null;
    }

    private void view(int row) {
        Payment p = paymentAt(row);
        if (p == null) return;
        String member = names.getOrDefault(p.getMemberId(), "Member #" + p.getMemberId());
        JOptionPane.showMessageDialog(this,
                "Receipt " + p.getReceiptNo() + "\n"
                        + member + "\n"
                        + DateUtils.format(p.getPaymentDate()) + "\n"
                        + (p.getMethod() == null ? "" : p.getMethod().name()) + "  "
                        + CurrencyUtils.format(p.getAmount(), "INR") + "\n"
                        + (p.getReference() == null ? "" : p.getReference()),
                "Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    private void print(int row) {
        Payment p = paymentAt(row);
        if (p == null) return;
        try {
            Member m = members.findById(p.getMemberId());
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File(p.getReceiptNo() + ".pdf"));
            chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path path = chooser.getSelectedFile().toPath();
            if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) path = Path.of(path + ".pdf");
            ReceiptRenderer.writeReceipt(path, m, p, App.cfg());
            JOptionPane.showMessageDialog(this, "Saved " + path);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Print", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void record() {
        try {
            List<Member> all = members.list(500, 0, "ACTIVE", null, null, null);
            if (all.isEmpty()) {
                JOptionPane.showMessageDialog(this, "There are no active members to bill.", "Record payment", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JComboBox<Member> box = new JComboBox<>(all.toArray(Member[]::new));
            box.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                    Component c = super.getListCellRendererComponent(list, value, index, sel, focus);
                    if (value instanceof Member m) setText(m.getFullName() + "  " + m.getMemberCode());
                    return c;
                }
            });
            int ok = JOptionPane.showConfirmDialog(this, box, "Record payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            Member m = (Member) box.getSelectedItem();
            if (m == null) {
                JOptionPane.showMessageDialog(this, "Choose a member.", "Record payment", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new PaymentFormDialog(SwingUtilities.getWindowAncestor(this), m.getMemberId(),
                    service.listForMember(m.getMemberId()), this::refresh).setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage() == null ? "Could not open the payment form." : e.getMessage(),
                    "Record payment", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportCsv() {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("payments.csv"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            List<String[]> rows = new ArrayList<>();
            for (Payment p : loaded) {
                rows.add(new String[]{
                        CsvExporter.text(p.getReceiptNo()),
                        CsvExporter.date(p.getPaymentDate()),
                        names.getOrDefault(p.getMemberId(), String.valueOf(p.getMemberId())),
                        p.getMethod() == null ? "" : p.getMethod().name(),
                        p.getAmount() == null ? "" : p.getAmount().toPlainString(),
                        p.getPaymentType() == null ? "" : p.getPaymentType().name()
                });
            }
            CsvExporter.export(
                    chooser.getSelectedFile().toPath(),
                    new String[]{"receipt", "date", "member", "method", "amount", "type"},
                    rows);
        } catch (Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Payments", () -> Router.get().navigateTo(Route.PAYMENTS_LIST)));
    }
}

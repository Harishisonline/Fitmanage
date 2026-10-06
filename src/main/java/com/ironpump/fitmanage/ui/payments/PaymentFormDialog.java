package com.ironpump.fitmanage.ui.payments;

import com.ironpump.fitmanage.model.Member;
import com.ironpump.fitmanage.model.Payment;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.service.PaymentService;
import com.ironpump.fitmanage.util.ReceiptRenderer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;

/** Modal dialog used by the Payments page and the Member → Payments sub-page. */
public class PaymentFormDialog extends JDialog {
    private final PaymentService paymentService = new PaymentService();
    private final MemberService memberService = new MemberService();
    private final int memberId;
    private final Runnable onSaved;
    private final JTextField amount = new JTextField();
    private final JComboBox<String> method = new JComboBox<>(new String[]{"CASH", "UPI", "CARD", "BANK_TRANSFER", "CHEQUE"});
    private final JTextField reference = new JTextField();
    private final JLabel status = new JLabel(" ");

    public PaymentFormDialog(Window owner, int memberId, java.util.List<Payment> existing, Runnable onSaved) {
        super(owner, "Record payment", Dialog.ModalityType.APPLICATION_MODAL);
        this.memberId = memberId;
        this.onSaved = onSaved;
        setSize(420, 280);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; add(new JLabel("Amount (Rs.)"), c);
        c.gridx = 1; add(amount, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Method"), c);
        c.gridx = 1; add(method, c);
        c.gridx = 0; c.gridy = 2; add(new JLabel("Reference / note"), c);
        c.gridx = 1; add(reference, c);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        status.setForeground(new Color(0xef, 0x44, 0x44));
        add(status, c);
        c.gridy = 4; c.gridwidth = 1;
        JButton save = new JButton("Save");
        save.setBackground(new Color(0x22, 0xC5, 0x5E));
        save.setForeground(new Color(0x06, 0x2b, 0x15));
        save.setOpaque(true);
        save.addActionListener(e -> save());
        c.gridx = 0; add(save, c);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        c.gridx = 1; add(cancel, c);
    }

    public PaymentFormDialog(int memberId, java.util.List<Payment> existing, Runnable onSaved) {
        this(null, memberId, existing, onSaved);
    }

    private void save() {
        try {
            Payment p = new Payment();
            p.setMemberId(memberId);
            p.setAmount(new BigDecimal(amount.getText().trim()));
            p.setMethod(Payment.Method.valueOf((String) method.getSelectedItem()));
            p.setReference(reference.getText().trim());
            paymentService.record(p);
            try {
                Member m = memberService.findById(memberId);
                File out = new File(System.getProperty("user.home"),
                        "fitmanage-receipt-" + p.getReceiptNo() + ".pdf");
                ReceiptRenderer.writeReceipt(out.toPath(), m, p, com.ironpump.fitmanage.App.cfg());
                JOptionPane.showMessageDialog(this, "Saved receipt " + p.getReceiptNo() + ". PDF: " + out);
            } catch (Exception pdfEx) {
                JOptionPane.showMessageDialog(this, "Saved. (PDF skipped: " + pdfEx.getMessage() + ")");
            }
            if (onSaved != null) SwingUtilities.invokeLater(onSaved);
            dispose();
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Could not record the payment." : e.getMessage();
            status.setText(message);
            JOptionPane.showMessageDialog(this, message, "Record payment", JOptionPane.WARNING_MESSAGE);
        }
    }
}

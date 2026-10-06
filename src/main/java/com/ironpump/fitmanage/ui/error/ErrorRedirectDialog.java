package com.ironpump.fitmanage.ui.error;

import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

/**
 * The single error-redirect page used by the whole app. Pops up as a modal dialog
 * with a user-friendly message and a "Copy details" button that copies the full
 * stack trace to the clipboard. Operator hits "Back to dashboard" to recover
 * without losing the session.
 */
public class ErrorRedirectDialog extends JDialog {
    private static final Logger log = LoggerFactory.getLogger(ErrorRedirectDialog.class);

    private ErrorRedirectDialog(Frame owner, String userMessage, Throwable t) {
        super(owner, "Something went wrong", true);
        setSize(560, 360);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12, 12));

        String refCode = "ref-" + UUID.randomUUID().toString().substring(0, 8);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));

        JLabel icon = new JLabel("Something went wrong");
        icon.setFont(icon.getFont().deriveFont(Font.BOLD, 18f));
        top.add(icon);
        top.add(Box.createVerticalStrut(6));

        JTextArea msg = new JTextArea(userMessage == null ? "(no message)" : userMessage);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        msg.setEditable(false);
        msg.setOpaque(false);
        msg.setFont(msg.getFont().deriveFont(14f));
        top.add(msg);
        top.add(Box.createVerticalStrut(8));

        JLabel ref = new JLabel("Reference: " + refCode);
        ref.setForeground(Color.GRAY);
        top.add(ref);
        add(top, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copy = new JButton("Copy details");
        final Route returnTo = Route.DASHBOARD;
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(
                            "Reference: " + refCode + "\n\n" + stackTrace(t)), null);
            JOptionPane.showMessageDialog(this, "Details copied to clipboard.");
        });
        JButton back = new JButton("Back to dashboard");
        back.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> {
                try { Router.get().navigateTo(returnTo); }
                catch (Exception ex) { log.error("Could not navigate after error", ex); }
            });
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        actions.add(copy);
        actions.add(close);
        actions.add(back);
        add(actions, BorderLayout.SOUTH);

        log.error("ErrorRedirectDialog shown: ref={}, userMessage={}", refCode, userMessage, t);
    }

    /** Entry point. Safe to call from any thread. */
    public static void show(String userMessage, Throwable t) {
        if (SwingUtilities.isEventDispatchThread()) showNow(userMessage, t);
        else SwingUtilities.invokeLater(() -> showNow(userMessage, t));
    }

    private static void showNow(String userMessage, Throwable t) {
        Frame owner = null;
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w instanceof Frame f) { owner = f; break; }
        }
        try {
            new ErrorRedirectDialog(owner, userMessage, t).setVisible(true);
        } catch (Exception ui) {
            // Last-resort: print to stderr. Do NOT throw or we recurse.
            log.error("Could not show ErrorRedirectDialog", ui);
            t.printStackTrace();
        }
    }

    private static String stackTrace(Throwable t) {
        if (t == null) return "(no exception)";
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) { t.printStackTrace(pw); }
        return sw.toString();
    }
}

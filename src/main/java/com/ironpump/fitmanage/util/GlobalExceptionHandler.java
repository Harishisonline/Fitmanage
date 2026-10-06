package com.ironpump.fitmanage.util;

import com.ironpump.fitmanage.ui.error.ErrorRedirectDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

/**
 * Installed as the default uncaught handler for every thread.
 * Catches anything that escaped the service layer or the form's own try/catch
 * and routes it to a single friendly ErrorRedirectDialog (see ui/error/).
 *
 * Threading: Swing dialogs must be created on the EDT. We check
 * EventQueue.isDispatchThread() and post to the EDT if not.
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Throwable root = unwrap(e);
        log.error("Uncaught exception on thread {}: {}", t.getName(), root.getMessage(), root);
        showError(root);
    }

    public static void handle(Throwable e) {
        Throwable root = unwrap(e);
        log.error("Handled error: {}", root.getMessage(), root);
        showError(root);
    }

    private static Throwable unwrap(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur;
    }

    private static void showError(Throwable t) {
        String userMsg = (t instanceof com.ironpump.fitmanage.exception.FitManageException)
                ? ((com.ironpump.fitmanage.exception.FitManageException) t).getUserMessage()
                : "Something went wrong. The team has been notified.";
        if (SwingUtilities.isEventDispatchThread()) {
            ErrorRedirectDialog.show(userMsg, t);
        } else {
            SwingUtilities.invokeLater(() -> ErrorRedirectDialog.show(userMsg, t));
        }
    }
}

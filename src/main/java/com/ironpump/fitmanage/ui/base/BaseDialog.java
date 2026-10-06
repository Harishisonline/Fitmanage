package com.ironpump.fitmanage.ui.base;

import com.ironpump.fitmanage.util.GlobalExceptionHandler;

/** Convenience base for JDialogs that want error redirect on uncaught. */
public abstract class BaseDialog extends javax.swing.JDialog {
    protected BaseDialog(java.awt.Window owner, String title) {
        super(owner, title);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    }

    /** Show the standard error redirect page. Use from inside catch blocks. */
    protected void showError(Throwable t) { GlobalExceptionHandler.handle(t); }
}

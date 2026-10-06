package com.ironpump.fitmanage.exception;

/**
 * Root of the FitManage exception hierarchy. All business-thrown exceptions
 * extend this so the global error redirect page can render a single friendly
 * message and the logger can log a single root cause.
 */
public class FitManageException extends RuntimeException {
    private final String userMessage;

    public FitManageException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public FitManageException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage == null ? "Something went wrong." : userMessage;
    }
}

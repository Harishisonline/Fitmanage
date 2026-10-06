package com.ironpump.fitmanage.exception;

/**
 * Wraps any SQLException from the DAO layer. The service layer catches this
 * and re-throws as FitManageException with a user-friendly message. The
 * error page renders the user message, never the SQL state.
 */
public class DatabaseException extends FitManageException {
    public DatabaseException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
    public DatabaseException(String userMessage) {
        super(userMessage);
    }
}

package com.ironpump.fitmanage.exception;

/** Thrown when the operator tries to delete a plan that is still referenced. */
public class PlanInUseException extends FitManageException {
    public PlanInUseException(int planId) {
        super("Plan " + planId + " is in use and cannot be deleted. Deactivate it instead.");
    }
}

package com.ironpump.fitmanage.exception;

/** Thrown when a member id is not in the DB. */
public class MemberNotFoundException extends FitManageException {
    public MemberNotFoundException(int id) {
        super("Member not found: " + id);
    }
}

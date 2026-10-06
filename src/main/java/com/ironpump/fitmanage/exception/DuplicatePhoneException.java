package com.ironpump.fitmanage.exception;

/** Thrown when a member phone is already on file. */
public class DuplicatePhoneException extends FitManageException {
    public DuplicatePhoneException(String phone) {
        super("A member with this phone already exists: " + phone);
    }
}

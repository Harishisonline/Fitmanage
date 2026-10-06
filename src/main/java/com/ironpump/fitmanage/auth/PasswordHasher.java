package com.ironpump.fitmanage.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Wraps jBCrypt. workload 12 = 2^12 rounds ≈ 250ms on a 2018 CPU.
 * Hashes are 60 chars; we store in VARCHAR(72) to be safe.
 */
public final class PasswordHasher {
    private static final int WORKLOAD = 12;

    private PasswordHasher() {}

    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(WORKLOAD));
    }

    public static boolean verify(String plain, String stored) {
        if (plain == null || stored == null || stored.isEmpty()) return false;
        try { return BCrypt.checkpw(plain, stored); }
        catch (IllegalArgumentException e) { return false; }
    }
}

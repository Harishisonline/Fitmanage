package com.ironpump.fitmanage.auth;

import com.ironpump.fitmanage.model.User;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-level singleton: holds the currently logged-in user.
 * Services that mutate data MUST call requireRole() before doing the work.
 */
public final class SessionContext {
    private static final AtomicReference<User> current = new AtomicReference<>();
    private static volatile long expiresAtEpochMs = 0L;

    private SessionContext() {}

    public static void login(User user, int idleTimeoutMinutes) {
        current.set(user);
        touch(idleTimeoutMinutes);
        fire();
    }

    public static void touch(int idleTimeoutMinutes) {
        expiresAtEpochMs = System.currentTimeMillis() + idleTimeoutMinutes * 60_000L;
    }

    public static void logout() {
        current.set(null);
        expiresAtEpochMs = 0L;
        fire();
    }

    private static final java.util.List<Runnable> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static void addListener(Runnable listener) { if (listener != null) listeners.add(listener); }
    private static void fire() {
        for (Runnable r : listeners) {
            try { r.run(); } catch (RuntimeException ignored) { }
        }
    }

    public static User current() { return current.get(); }

    public static boolean isExpired() {
        return current.get() != null && System.currentTimeMillis() > expiresAtEpochMs;
    }

    public static boolean isLoggedIn() {
        User u = current.get();
        return u != null && !isExpired();
    }

    /** Throws if no user or wrong role. */
    public static void requireRole(User.Role... allowed) {
        User u = current.get();
        if (u == null) throw new com.ironpump.fitmanage.exception.FitManageException("Not logged in.");
        for (User.Role r : allowed) if (u.getRole() == r) return;
        throw new com.ironpump.fitmanage.exception.FitManageException(
                "Your role (" + u.getRole() + ") cannot perform this action.");
    }

    /** Convenience: current user id, or 0 if none. */
    public static int currentUserId() {
        User u = current.get();
        return u == null ? 0 : u.getUserId();
    }
}

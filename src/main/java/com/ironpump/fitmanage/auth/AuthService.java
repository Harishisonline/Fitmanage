package com.ironpump.fitmanage.auth;

import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.dao.UserDao;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Owns the login flow. Enforces 5-fail / 15-min lockout, idle timeout, and
 * "no login while locked" rule. The login UI calls this; nothing else does.
 */
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private final UserDao userDao;
    private final AppConfig cfg;

    public AuthService(UserDao userDao, AppConfig cfg) {
        this.userDao = userDao;
        this.cfg = cfg;
    }

    /** Returns the User on success. Throws FitManageException with a user-friendly message on failure. */
    public User login(String username, String password) {
        User u = userDao.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new FitManageException("Invalid username or password."));
        if (!u.isActive()) {
            throw new FitManageException("This account is disabled. Contact an administrator.");
        }
        if (u.isLocked()) {
            throw new FitManageException("Account locked. Try again in a few minutes.");
        }
        if (!PasswordHasher.verify(password, u.getPasswordHash())) {
            int newCount = u.getFailedAttempts() + 1;
            userDao.recordFailedAttempt(u.getUserId());
            if (newCount >= MAX_ATTEMPTS) {
                userDao.lock(u.getUserId(), LOCKOUT_MINUTES);
                throw new FitManageException("Too many failed attempts. Account locked for " + LOCKOUT_MINUTES + " minutes.");
            }
            int remaining = MAX_ATTEMPTS - newCount;
            throw new FitManageException("Invalid username or password. " + remaining + " attempts remaining.");
        }
        userDao.unlock(u.getUserId());
        int timeoutMin = cfg.getInt("app.idle.timeout.minutes");
        SessionContext.login(u, timeoutMin);
        u.setLastLoginAt(LocalDateTime.now());
        userDao.updateLastLogin(u.getUserId());
        new com.ironpump.fitmanage.dao.ActivityLogDao().log(u.getUserId(), null, null, "LOGIN", "User " + u.getUsername() + " logged in.");
        log.info("User {} logged in (role={})", u.getUsername(), u.getRole());
        return u;
    }

    public void logout() {
        User u = SessionContext.current();
        if (u != null) {
            log.info("User {} logged out", u.getUsername());
            new com.ironpump.fitmanage.dao.ActivityLogDao().log(u.getUserId(), null, null, "LOGOUT", null);
        }
        SessionContext.logout();
    }

    public void changeOwnPassword(int userId, String oldPlain, String newPlain) {
        User u = userDao.findById(userId)
                .orElseThrow(() -> new FitManageException("User not found."));
        if (!PasswordHasher.verify(oldPlain, u.getPasswordHash()))
            throw new FitManageException("Current password is incorrect.");
        com.ironpump.fitmanage.util.Validator.password(newPlain);
        userDao.updatePassword(userId, PasswordHasher.hash(newPlain));
    }
}

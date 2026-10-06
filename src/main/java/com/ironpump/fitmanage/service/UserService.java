package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.auth.PasswordHasher;
import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.dao.ActivityLogDao;
import com.ironpump.fitmanage.dao.UserDao;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.model.User;
import com.ironpump.fitmanage.util.Validator;

import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserDao userDao = new UserDao();
    private final ActivityLogDao activityLog = new ActivityLogDao();

    public List<User> list() { return userDao.findAll(); }
    public User findById(int id) { return userDao.findById(id).orElseThrow(() -> new FitManageException("User not found: " + id)); }
    public Optional<User> findByUsername(String u) { return userDao.findByUsername(u); }

    public int createStaffOrTrainer(User u, String rawPassword) {
        SessionContext.requireRole(User.Role.ADMIN);
        Validator.requireMax("username", u.getUsername(), 40);
        Validator.password(rawPassword);
        if (userDao.usernameExists(u.getUsername()))
            throw new FitManageException("Username already taken: " + u.getUsername());
        u.setPasswordHash(PasswordHasher.hash(rawPassword));
        int id = userDao.insert(u);
        activityLog.log(SessionContext.currentUserId(), null, null, "USER_CREATED", u.getUsername() + " (" + u.getRole() + ")");
        return id;
    }

    public void adminUpdate(User u) {
        SessionContext.requireRole(User.Role.ADMIN);
        Validator.requireMax("username", u.getUsername(), 40);
        Validator.requireMax("displayName", u.getDisplayName(), 80);
        if (u.getEmail() != null && !u.getEmail().isBlank()) u.setEmail(Validator.email("email", u.getEmail()));
        userDao.update(u);
        activityLog.log(SessionContext.currentUserId(), null, u.getLinkedTrainerId(), "USER_UPDATED", u.getUsername(), null);
    }

    public void updateOwnProfile(int userId, String displayName, String email) {
        Validator.requireMax("displayName", displayName, 80);
        if (email != null && !email.isBlank()) Validator.email("email", email);
        userDao.updateProfile(userId, displayName, email);
        activityLog.log(SessionContext.currentUserId(), null, null, "PROFILE_UPDATED", null);
    }

    public void adminResetPassword(int userId, String newPassword) {
        SessionContext.requireRole(User.Role.ADMIN);
        Validator.password(newPassword);
        userDao.updatePassword(userId, PasswordHasher.hash(newPassword));
        activityLog.log(SessionContext.currentUserId(), null, null, "PASSWORD_RESET", "user " + userId);
    }

    public void setActive(int userId, boolean active) {
        SessionContext.requireRole(User.Role.ADMIN);
        userDao.setActive(userId, active);
        activityLog.log(SessionContext.currentUserId(), null, null, active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "user " + userId);
    }

    public void unlock(int userId) {
        SessionContext.requireRole(User.Role.ADMIN);
        userDao.unlock(userId);
    }
}

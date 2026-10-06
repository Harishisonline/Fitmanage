package com.ironpump.fitmanage;

import com.ironpump.fitmanage.auth.AuthService;
import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.db.SchemaInitializer;
import com.ironpump.fitmanage.service.SettingsService;
import com.ironpump.fitmanage.ui.dashboard.DashboardPage;
import com.ironpump.fitmanage.ui.login.LoginFrame;
import com.ironpump.fitmanage.ui.main.MainFrame;
import com.ironpump.fitmanage.ui.members.*;
import com.ironpump.fitmanage.ui.payments.PaymentsListPage;
import com.ironpump.fitmanage.ui.plans.*;
import com.ironpump.fitmanage.ui.reports.ReportsPage;
import com.ironpump.fitmanage.ui.attendance.AttendancePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.ui.settings.*;
import com.ironpump.fitmanage.ui.theme.ThemeManager;
import com.ironpump.fitmanage.ui.trainers.*;
import com.ironpump.fitmanage.ui.users.*;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Entry point. Wires config + DB + theme + router + main frame, then shows
 * the login screen. After a successful login, the MainFrame is shown and all
 * pages are registered with the router.
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static AppConfig cfg;
    private static MainFrame mainFrame;

    public static void main(String[] args) {
        // 1. Install the global error handler before anything else.
        GlobalExceptionHandler.install();

        SwingUtilities.invokeLater(() -> {
            try {
                cfg = new AppConfig();
                DatabaseManager.init(cfg);

                // 2. Ensure schema + seed exist. If first run, regenerate bcrypt hashes
                //    programmatically and replace the placeholder values in 02_seed.sql.
                ensureSchemaAndSeed();

                // 3. Apply theme
                SettingsService settings = new SettingsService(cfg);
                ThemeManager.applyInitial(settings.theme().isEmpty() ? "dark" : settings.theme());

                // 4. Start the /health HTTP server so operators (and CI) can verify
                //    the JVM is up and the DB is reachable without opening the UI.
                com.ironpump.fitmanage.util.HealthServer.start(cfg);

                // 5. Show login
                LoginFrame login = new LoginFrame(cfg);
                login.setVisible(true);
            } catch (Throwable t) {
                log.error("Fatal startup error", t);
                if (t instanceof OutOfMemoryError) {
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "FitManage ran out of memory. Close other applications and try again.\n" + t.getMessage(),
                            "Out of memory", javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }
                GlobalExceptionHandler.handle(t);
                System.exit(1);
            }
        });
    }

    /** Called by LoginFrame after AuthService.login succeeds. */
    public static void onLoginSuccess(LoginFrame login) {
        login.dispose();
        mainFrame = new MainFrame();
        registerAllPages();
        mainFrame.setVisible(true);
        // Default landing page
        Router.get().navigateTo(Route.DASHBOARD);
    }

    public static void signOut() {
        try { new AuthService(new com.ironpump.fitmanage.dao.UserDao(), cfg).logout(); }
        catch (Exception ignored) { /* nothing we can do */ }
        if (mainFrame != null) mainFrame.dispose();
        Router.get().clearSession();
        LoginFrame login = new LoginFrame(cfg);
        login.setVisible(true);
    }

    public static void confirmAndExit() {
        int choice = JOptionPane.showConfirmDialog(mainFrame,
                "Sign out and close FitManage?", "Exit", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            try { new AuthService(new com.ironpump.fitmanage.dao.UserDao(), cfg).logout(); } catch (Exception ignored) {}
            System.exit(0);
        }
    }

    public static AppConfig cfg() { return cfg; }
    public static MainFrame mainFrame() { return mainFrame; }

    /** Wires every Route → BasePage factory. Called once after login. */
    private static void registerAllPages() {
        Router r = Router.get();
        r.register(Route.DASHBOARD,            DashboardPage::new);

        r.register(Route.MEMBERS_LIST,         MembersListPage::new);
        r.register(Route.MEMBER_FORM_NEW,      MemberFormPage::newForCreate);
        r.register(Route.MEMBER_FORM_EDIT,     MemberFormPage::newForEdit);
        r.register(Route.MEMBER_DETAIL,        MemberDetailPage::new);
        r.register(Route.MEMBER_MEMBERSHIPS,   MemberMembershipsPage::new);
        r.register(Route.MEMBER_PAYMENTS,      MemberPaymentsPage::new);
        r.register(Route.MEMBER_ATTENDANCE,    MemberAttendancePage::new);
        r.register(Route.MEMBER_PROGRESS,      MemberProgressPage::new);
        r.register(Route.MEMBER_ACTIVITY,      MemberActivityPage::new);

        r.register(Route.TRAINERS_LIST,        TrainersListPage::new);
        r.register(Route.TRAINER_FORM_NEW,     TrainerFormPage::newForCreate);
        r.register(Route.TRAINER_FORM_EDIT,    TrainerFormPage::newForEdit);
        r.register(Route.TRAINER_OVERVIEW,     TrainerOverviewPage::new);
        r.register(Route.TRAINER_PERSONAL,     TrainerPersonalPage::new);
        r.register(Route.TRAINER_MEMBERS,      TrainerMembersPage::new);
        r.register(Route.TRAINER_SALARY,       TrainerSalaryPage::new);
        r.register(Route.TRAINER_ACTIVITY,     TrainerActivityPage::new);

        r.register(Route.PLANS_LIST,           PlansListPage::new);
        r.register(Route.PLAN_FORM_NEW,        PlanFormPage::newForCreate);
        r.register(Route.PLAN_FORM_EDIT,       PlanFormPage::newForEdit);

        r.register(Route.ATTENDANCE,           AttendancePage::new);
        r.register(Route.PAYMENTS_LIST,        PaymentsListPage::new);
        r.register(Route.REPORTS,              ReportsPage::new);

        r.register(Route.USERS_LIST,           UsersListPage::new);
        r.register(Route.USER_FORM_NEW,        UserFormPage::newForCreate);
        r.register(Route.USER_FORM_EDIT,       UserFormPage::newForEdit);

        r.register(Route.SETTINGS_PROFILE,     SettingsProfilePage::new);
        r.register(Route.SETTINGS_PREFERENCES, SettingsPreferencesPage::new);
        r.register(Route.SETTINGS_GYM,         SettingsGymPage::new);
        r.register(Route.SETTINGS_DATABASE,    SettingsDatabasePage::new);
        r.register(Route.SETTINGS_ABOUT,        SettingsAboutPage::new);
    }

    /** Creates the schema and seeds the DB on first launch. */
    private static void ensureSchemaAndSeed() {
        try (Connection c = DatabaseManager.get()) {
            SchemaInitializer init = new SchemaInitializer();
            if (init.needsInit(c)) {
                log.info("First launch — running schema and seed scripts");
                init.runSchema(c);
                init.runSeed(c);
                regenerateDemoPasswords(c);
            }
            init.applyMigrations(c);
            int reopened = new com.ironpump.fitmanage.dao.MembershipDao().reopenPrematurelyExpired();
            if (reopened > 0) log.info("Restored {} memberships that were marked expired before their end date", reopened);
        } catch (SQLException e) {
            throw new com.ironpump.fitmanage.exception.DatabaseException(
                    "Could not initialise schema: " + e.getMessage(), e);
        }
    }

    private static void regenerateDemoPasswords(Connection c) {
        try {
            updateHash(c, "admin",   com.ironpump.fitmanage.auth.PasswordHasher.hash("admin123"));
            updateHash(c, "staff1",  com.ironpump.fitmanage.auth.PasswordHasher.hash("staff123"));
            updateHash(c, "trainer1", com.ironpump.fitmanage.auth.PasswordHasher.hash("train123"));
            log.info("Regenerated bcrypt hashes for demo users (admin123, staff123, train123)");
        } catch (Exception e) {
            log.warn("Could not regenerate demo passwords — operators may not be able to log in", e);
        }
    }

    private static void updateHash(Connection c, String username, String newHash) throws SQLException {
        try (var ps = c.prepareStatement("UPDATE users SET password_hash = ? WHERE username = ?")) {
            ps.setString(1, newHash);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }
}

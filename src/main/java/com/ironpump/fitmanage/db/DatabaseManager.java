package com.ironpump.fitmanage.db;

import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.config.DbConfig;
import com.ironpump.fitmanage.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single point of access to JDBC Connections.
 * No connection pool in v1 — one installation = one gym = low concurrency.
 * For pooling, swap to HikariCP later (one-line change in get()).
 */
public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static String URL;
    private static String USER;
    private static String PASS;

    private DatabaseManager() {}

    public static synchronized void init(AppConfig cfg) {
        DbConfig db = new DbConfig(cfg);
        initWithDriver(db.url(), db.user(), db.password(), "com.mysql.cj.jdbc.Driver");
    }

    /** Test-only entry: bypass AppConfig, point at any JDBC URL. */
    public static synchronized void initWithDriver(String url, String user, String pass, String driverClass) {
        URL  = url;
        USER = user;
        PASS = pass;
        try { Class.forName(driverClass); }
        catch (ClassNotFoundException e) {
            throw new DatabaseException("JDBC driver missing on classpath: " + driverClass, e);
        }
        log.info("DatabaseManager initialised: {}", URL);
    }

    /**
     * Hands out a fresh Connection. Caller is responsible for try-with-resources.
     * Throws DatabaseException with a user-friendly message on failure.
     */
    public static Connection get() {
        if (URL == null) throw new DatabaseException("Database not initialised. Call DatabaseManager.init(cfg) first.");
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Cannot connect to the database. (1) Check that MySQL is running. (2) Check Settings → Database.", e);
        }
    }

    /** Helper: returns true if a SELECT 1 succeeds. */
    public static boolean isReachable() {
        try (Connection c = get(); var ps = c.prepareStatement("SELECT 1"); var rs = ps.executeQuery()) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }
}

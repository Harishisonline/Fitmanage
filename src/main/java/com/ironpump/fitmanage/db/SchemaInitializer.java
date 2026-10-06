package com.ironpump.fitmanage.db;

import com.ironpump.fitmanage.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads bundled SQL files (01_schema.sql, 02_seed.sql) from the classpath
 * and runs them statement by statement. Splits on `;` outside of strings/comments
 * which is good enough for the bundled files (no stored procs, no triggers).
 */
public class SchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    /** Returns true if any table in the 'public' schema-like listing is missing. */
    public boolean needsInit(Connection c) throws SQLException {
        String[] tables = { "users", "plans", "members", "trainers", "memberships", "attendance", "payments", "trainer_payments", "progress_logs", "activity_log" };
        for (String t : tables) {
            try (var ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
                ps.setString(1, t);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) == 0) return true;
                }
            }
        }
        return false;
    }

    /** Apply additive migrations when the database already exists. */
    public void applyMigrations(Connection c) throws SQLException {
        if (!columnExists(c, "trainers", "dob") || !columnExists(c, "payments", "payment_type")) {
            runScript(c, "sql/03_trainer_demographics.sql");
        }
    }

    private boolean columnExists(Connection c, String table, String column) throws SQLException {
        try (var ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public void runSchema(Connection c) {
        runScript(c, "sql/01_schema.sql");
    }

    public void runSeed(Connection c) {
        runScript(c, "sql/02_seed.sql");
    }

    private void runScript(Connection c, String resourcePath) {
        String sql = readResource(resourcePath);
        List<String> statements = splitStatements(sql);
        try (Statement st = c.createStatement()) {
            for (String s : statements) {
                if (s.isBlank()) continue;
                st.execute(s);
            }
            log.info("Executed {} statements from {}", statements.size(), resourcePath);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to run " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    private String readResource(String path) {
        try (InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new DatabaseException("Missing classpath resource: " + path);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return r.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new DatabaseException("Could not read " + path, e);
        }
    }

    /** Naive but sufficient splitter: drops lines that are comments, then splits on `;`. */
    private List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
            buf.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                out.add(buf.toString().trim());
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) out.add(buf.toString().trim());
        return out;
    }
}

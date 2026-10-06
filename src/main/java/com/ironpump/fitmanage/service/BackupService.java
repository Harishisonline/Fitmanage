package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.exception.DatabaseException;
import com.ironpump.fitmanage.exception.FitManageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs `mysqldump` to back up the DB, and `mysql` to restore.
 * Falls back to plain file copy if mysqldump is not available.
 */
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private final AppConfig cfg;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    public BackupService(AppConfig cfg) { this.cfg = cfg; }

    public Path backupNow() {
        String dir = cfg.get("app.backup.dir").replace("${user.home}", System.getProperty("user.home"));
        try { Files.createDirectories(Path.of(dir)); } catch (IOException e) {
            throw new DatabaseException("Could not create backup dir: " + dir, e);
        }
        Path out = Path.of(dir, "fitmanage-" + LocalDateTime.now().format(FMT) + ".sql");
        String url = cfg.get("db.url");
        String user = cfg.get("db.user");
        String pass = cfg.get("db.password");
        String mysqldump = cfg.get("app.mysqldump.path");
        String dbName = extractDbName(url);
        try {
            Process p = new ProcessBuilder(mysqldump,
                    "-h", extractHost(url),
                    "-P", extractPort(url),
                    "-u", user,
                    "-p" + pass,
                    "--single-transaction",
                    "--routines",
                    dbName)
                    .redirectOutput(out.toFile())
                    .redirectError(new File(System.getProperty("java.io.tmpdir"), "fitmanage-mysqldump.err"))
                    .start();
            int code = p.waitFor();
            if (code != 0) throw new DatabaseException("mysqldump exited with code " + code, null);
            log.info("Backup written to {}", out);
            return out;
        } catch (Exception e) {
            try { Files.deleteIfExists(out); } catch (IOException ignored) { }
            Path failed = Path.of(dir, "fitmanage-" + LocalDateTime.now().format(FMT) + ".FAILED");
            try {
                Files.writeString(failed, "FitManage backup FAILED at " + LocalDateTime.now() + "\n" + e.getMessage() + "\n");
            } catch (IOException ignored) { }
            throw new DatabaseException("Backup failed: " + e.getMessage() + ". Tried: " + mysqldump, e);
        }
    }

    public void restoreFrom(Path sqlFile) {
        if (!Files.exists(sqlFile)) throw new FitManageException("Backup file not found: " + sqlFile);
        String url = cfg.get("db.url");
        String user = cfg.get("db.user");
        String pass = cfg.get("db.password");
        String mysql = "mysql"; // assume on PATH; operator can set if not
        try {
            Process p = new ProcessBuilder(mysql,
                    "-h", extractHost(url),
                    "-P", extractPort(url),
                    "-u", user,
                    "-p" + pass,
                    extractDbName(url))
                    .redirectInput(sqlFile.toFile())
                    .redirectError(new File(System.getProperty("java.io.tmpdir"), "fitmanage-mysql.err"))
                    .start();
            int code = p.waitFor();
            if (code != 0) throw new DatabaseException("mysql restore exited with code " + code, null);
            log.info("Restored from {}", sqlFile);
        } catch (Exception e) {
            throw new DatabaseException("Restore failed: " + e.getMessage(), e);
        }
    }

    public List<Path> listBackups() throws IOException {
        String dir = cfg.get("app.backup.dir").replace("${user.home}", System.getProperty("user.home"));
        File d = new File(dir);
        if (!d.exists() || !d.isDirectory()) return List.of();
        File[] files = d.listFiles((f, n) -> n.startsWith("fitmanage-") && n.endsWith(".sql"));
        if (files == null) return List.of();
        List<Path> out = new ArrayList<>();
        for (File f : files) out.add(f.toPath());
        out.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
        return out;
    }

    private static String extractHost(String url) {
        int i = url.indexOf("://") + 3;
        int j = url.indexOf(':', i);
        return url.substring(i, j);
    }

    private static String extractPort(String url) {
        int i = url.indexOf(':', url.indexOf("://") + 3) + 1;
        int j = url.indexOf('/', i);
        return url.substring(i, j);
    }

    private static String extractDbName(String url) {
        int i = url.lastIndexOf('/') + 1;
        int j = url.indexOf('?', i);
        return j < 0 ? url.substring(i) : url.substring(i, j);
    }
}

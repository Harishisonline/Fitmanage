package com.ironpump.fitmanage.service;

import com.ironpump.fitmanage.config.AppConfig;

/** Tiny service that reads/writes the in-memory properties of the running app. */
public class SettingsService {
    private final AppConfig cfg;
    public SettingsService(AppConfig cfg) { this.cfg = cfg; }

    public String get(String k)                       { return cfg.getOrDefault(k, ""); }
    public void set(String k, String v)              { cfg.set(k, v); }
    public void save() {
        java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("user.home"), ".fitmanage");
        java.nio.file.Path target = dir.resolve("application.properties");
        java.nio.file.Path tmp = dir.resolve("application.properties.tmp");
        try {
            java.nio.file.Files.createDirectories(dir);
            try (java.io.BufferedWriter w = java.nio.file.Files.newBufferedWriter(tmp)) {
                cfg.asProperties().store(w, "FitManage settings");
            }
            try {
                java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.io.IOException e) {
            throw new com.ironpump.fitmanage.exception.FitManageException("Could not save settings", e);
        }
    }

    public String theme()            { return get("app.theme"); }
    public void setTheme(String t)   { set("app.theme", t); save(); }
    public String currency()          { return get("app.currency"); }
    public void setCurrency(String c){ set("app.currency", c); save(); }
    public String dateFormat()        { return get("app.date.format"); }
    public String checkinDefault()    { return get("app.checkin.default"); }
    public int idleTimeoutMinutes()   { return Integer.parseInt(get("app.idle.timeout.minutes")); }
    public String gymName()           { return get("app.gym.name"); }
    public void setGymName(String n)  { set("app.gym.name", n); save(); }
    public String gymAddress()        { return get("app.gym.address"); }
    public void setGymAddress(String a){ set("app.gym.address", a); save(); }
    public String gymPhone()          { return get("app.gym.phone"); }
    public void setGymPhone(String p) { set("app.gym.phone", p); save(); }
    public String gymEmail()          { return get("app.gym.email"); }
    public void setGymEmail(String e) { set("app.gym.email", e); save(); }
    public String gymGstin()          { return get("app.gym.gstin"); }
    public void setGymGstin(String g) { set("app.gym.gstin", g); save(); }
    public String gymFooter()         { return get("app.gym.receipt_footer"); }
    public void setGymFooter(String f){ set("app.gym.receipt_footer", f); save(); }
    public String backupDir()         { return get("app.backup.dir"); }
    public void setBackupDir(String d){ set("app.backup.dir", d); save(); }
    public String mysqldumpPath()     { return get("app.mysqldump.path"); }
    public void setMysqldumpPath(String p){ set("app.mysqldump.path", p); save(); }
}

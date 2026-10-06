package com.ironpump.fitmanage.config;

/** Convenience accessors over AppConfig for the DB-related keys. */
public class DbConfig {
    private final AppConfig cfg;
    public DbConfig(AppConfig cfg) { this.cfg = cfg; }
    public String url()      { return cfg.get("db.url"); }
    public String user()     { return cfg.get("db.user"); }
    public String password() { return cfg.get("db.password"); }
}

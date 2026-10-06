package com.ironpump.fitmanage.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads application properties. Looks in this order:
 *   1. Environment variables (FITMANAGE_DB_URL, FITMANAGE_DB_PASSWORD, etc.)
 *   2. An external config file at ~/.fitmanage/application.properties (operator override)
 *   3. The bundled classpath resource application.properties
 */
public class AppConfig {
    private final Properties props = new Properties();

    public AppConfig() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            throw new com.ironpump.fitmanage.exception.DatabaseException(
                    "Could not load application.properties from classpath", e);
        }

        Path userCfg = Path.of(System.getProperty("user.home"), ".fitmanage", "application.properties");
        if (Files.exists(userCfg)) {
            try (InputStream in = Files.newInputStream(userCfg)) {
                Properties override = new Properties();
                override.load(in);
                props.putAll(override);
            } catch (IOException e) {
                System.err.println("Could not read " + userCfg + " : " + e.getMessage());
            }
        }

        // Environment variable overrides. FITMANAGE_APP_THEME -> app.theme, including keys
        // that are not already present in the properties file.
        System.getenv().forEach((envKey, envVal) -> {
            if (envKey != null && envKey.startsWith("FITMANAGE_") && envVal != null) {
                String key = envKey.substring("FITMANAGE_".length()).toLowerCase(java.util.Locale.ROOT).replace('_', '.');
                props.setProperty(key, envVal);
            }
        });
    }

    public String get(String key) {
        String v = props.getProperty(key);
        if (v == null) throw new com.ironpump.fitmanage.exception.DatabaseException(
                "Missing config key: " + key);
        return v;
    }

    public String getOrDefault(String key, String def) {
        return props.getProperty(key, def);
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key).trim());
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    public Properties asProperties() { return props; }
}

package com.ironpump.fitmanage.util;

import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.db.DatabaseManager;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tiny built-in HTTP server, started by App.main() if {@code app.health.port} is set
 * in application.properties (default: 8080). Exposes:
 *   GET /health      -> 200 OK with JSON body, or 503 if DB unreachable
 *   GET /version     -> 200 OK with build + JVM info
 *   GET /            -> 200 OK with a tiny index page that links to /health and /version
 *
 * It is intentionally minimal: no auth, no TLS, no fancy framework. Its job is to
 * give operators (and CI) a way to confirm FitManage is up and connected to MySQL
 * without having to open the Swing UI.
 */
public final class HealthServer {
    private static final Logger log = LoggerFactory.getLogger(HealthServer.class);
    private static final AtomicBoolean DB_REACHABLE = new AtomicBoolean(false);
    private static final Instant STARTED_AT = Instant.now();
    private static HttpServer server;

    private HealthServer() {}

    public static synchronized void start(AppConfig cfg) {
        if (server != null) return;
        int port = Integer.parseInt(cfg.getOrDefault("app.health.port", "8080"));
        String bind = cfg.getOrDefault("app.health.bind", "127.0.0.1");
        try {
            server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        } catch (IOException e) {
            log.error("Could not bind health server to {}:{} — continuing without /health endpoint", bind, port, e);
            return;
        }
        server.createContext("/health",   ex -> handleHealth(ex));
        server.createContext("/version",  ex -> handleVersion(ex));
        server.createContext("/",         ex -> handleIndex(ex));
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
        // Background thread: ping DB every 30s so /health can return a quick answer.
        Thread pinger = new Thread(HealthServer::pingerLoop, "fitmanage-db-pinger");
        pinger.setDaemon(true);
        pinger.start();
        log.info("Health server listening on http://{}:{}/ (GET /health, /version, /)", bind, port);
    }

    public static synchronized void stop() {
        if (server != null) { server.stop(0); server = null; }
    }

    private static void pingerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                DB_REACHABLE.set(DatabaseManager.isReachable());
            } catch (Throwable ignored) { DB_REACHABLE.set(false); }
            try { Thread.sleep(30_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }

    private static void handleHealth(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        // Always re-check synchronously on the health request, so we don't serve a stale OK.
        boolean reachable;
        try { reachable = DatabaseManager.isReachable(); } catch (Throwable ignored) { reachable = false; }
        DB_REACHABLE.set(reachable);

        int code = reachable ? 200 : 503;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     reachable ? "UP" : "DOWN");
        body.put("app",        "FitManage");
        body.put("version",    "1.0.0");
        body.put("uptimeSec",  java.time.Duration.between(STARTED_AT, Instant.now()).toSeconds());
        body.put("db",         reachable ? "reachable" : "unreachable");
        body.put("checkedAt",  Instant.now().toString());
        writeJson(ex, code, body);
    }

    private static void handleVersion(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app",     "FitManage");
        body.put("version", "1.0.0");
        body.put("java",    System.getProperty("java.version"));
        body.put("os",      System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        writeJson(ex, 200, body);
    }

    private static void handleIndex(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        String body = """
                <!doctype html>
                <html><head><title>FitManage</title>
                <style>body{font:14px/1.5 -apple-system,system-ui,sans-serif;background:#0f1419;color:#e6edf3;padding:48px;max-width:720px;margin:0 auto}
                h1{color:#22c55e}a{color:#22c55e}code{background:#1a2027;padding:2px 6px;border-radius:4px}</style>
                </head><body>
                <h1>FitManage</h1>
                <p>This is the FitManage health server. The actual app is a Java Swing desktop application
                — this HTTP endpoint exists so operators and CI can verify the JVM and DB are up
                without opening the desktop window.</p>
                <ul>
                  <li><a href="/health">/health</a> — returns 200 if the DB is reachable, 503 otherwise</li>
                  <li><a href="/version">/version</a> — build + JVM metadata</li>
                </ul>
                </body></html>
                """;
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void writeJson(com.sun.net.httpserver.HttpExchange ex, int code, Map<String, Object> body) throws IOException {
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0; int n = body.size();
        for (Map.Entry<String, Object> e : body.entrySet()) {
            sb.append("  \"").append(e.getKey()).append("\": ");
            Object v = e.getValue();
            if (v == null)      sb.append("null");
            else if (v instanceof Number) sb.append(v);
            else                sb.append("\"").append(v.toString().replace("\"", "\\\"")).append("\"");
            if (++i < n) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        byte[] b = sb.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}

package com.ironpump.fitmanage.ui.router;

import java.util.HashMap;
import java.util.Map;

/** A small typed bag of route parameters. Use put/get/has. */
public class RouteContext {
    private final Map<String, Object> params = new HashMap<>();
    public RouteContext put(String key, Object value) { params.put(key, value); return this; }
    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) params.get(key); }
    public boolean has(String key) { return params.containsKey(key); }
    public static RouteContext empty() { return new RouteContext(); }
    public static RouteContext of(String key, Object value) { return empty().put(key, value); }
}

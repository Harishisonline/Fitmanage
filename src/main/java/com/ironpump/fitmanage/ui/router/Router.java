package com.ironpump.fitmanage.ui.router;

import com.ironpump.fitmanage.ui.base.BasePage;

import javax.swing.JPanel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Singleton router. Pages are constructed lazily by their suppliers, then cached.
 * The MainFrame's CardLayout swaps the active page. onLeave / onEnter hooks
 * are called around every navigation.
 */
public class Router {
    private static final Router INSTANCE = new Router();
    public static Router get() { return INSTANCE; }

    private final Map<Route, Supplier<BasePage>> factories = new EnumMap<>(Route.class);
    private final Map<Route, BasePage> cache = new EnumMap<>(Route.class);
    private final Map<Route, RouteContext> contexts = new EnumMap<>(Route.class);
    private final Deque<Route> backStack = new ArrayDeque<>();
    private Route current = Route.DASHBOARD;
    private JPanel host;
    private Consumer<List<BasePage.BreadcrumbItem>> breadcrumbListener;
    private Consumer<Route> routeListener;
    private boolean suppressPush;

    public void setHost(JPanel host) { this.host = host; }

    public void setBreadcrumbListener(Consumer<List<BasePage.BreadcrumbItem>> listener) {
        this.breadcrumbListener = listener;
    }

    public void setRouteListener(Consumer<Route> listener) { this.routeListener = listener; }

    public void register(Route route, Supplier<BasePage> factory) { factories.put(route, factory); }

    public void navigateTo(Route route) { navigateTo(route, RouteContext.empty()); }

    public void navigateTo(Route route, RouteContext ctx) {
        RouteContext context = ctx == null ? RouteContext.empty() : ctx;
        Supplier<BasePage> factory = factories.get(route);
        if (factory == null) throw new IllegalStateException("No page registered for " + route);
        BasePage page = cache.computeIfAbsent(route, r -> factory.get());
        if (current != null && cache.get(current) != null && cache.get(current) != page) {
            cache.get(current).onLeave();
        }
        page.onEnter(context);
        if (host != null) {
            host.removeAll();
            host.add(page);
            host.revalidate();
            host.repaint();
        }
        if (!suppressPush && current != null && current != route) backStack.push(current);
        current = route;
        contexts.put(route, context);
        if (breadcrumbListener != null) breadcrumbListener.accept(page.breadcrumbs(context));
        if (routeListener != null) routeListener.accept(route);
    }

    public void back() {
        if (backStack.isEmpty()) return;
        Route prev = backStack.pop();
        RouteContext ctx = contexts.getOrDefault(prev, RouteContext.empty());
        suppressPush = true;
        try { navigateTo(prev, ctx); }
        finally { suppressPush = false; }
    }

    public boolean canGoBack() { return !backStack.isEmpty(); }
    public Route current() { return current; }

    /** Clear the cache (e.g. after a member is created) and re-navigate with the same context. */
    public void refresh() {
        RouteContext ctx = contexts.getOrDefault(current, RouteContext.empty());
        cache.clear();
        navigateTo(current, ctx);
    }

    /** Drop cached pages when the operator signs out. */
    public void clearSession() {
        cache.clear();
        contexts.clear();
        backStack.clear();
        current = Route.DASHBOARD;
    }
}

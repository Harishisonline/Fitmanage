package com.ironpump.fitmanage.ui.router;

/**
 * Every navigable page in the app. Modals are NOT routes (they live in JDialog).
 * Mirrors the route table in the PRD §3.2 exactly.
 */
public enum Route {
    DASHBOARD("/dashboard"),
    MEMBERS_LIST("/members"),
    MEMBER_FORM_NEW("/members/new"),
    MEMBER_FORM_EDIT("/members/{id}/edit"),
    MEMBER_DETAIL("/members/{id}"),
    MEMBER_MEMBERSHIPS("/members/{id}/memberships"),
    MEMBER_PAYMENTS("/members/{id}/payments"),
    MEMBER_ATTENDANCE("/members/{id}/attendance"),
    MEMBER_PROGRESS("/members/{id}/progress"),
    MEMBER_ACTIVITY("/members/{id}/activity"),

    TRAINERS_LIST("/trainers"),
    TRAINER_FORM_NEW("/trainers/new"),
    TRAINER_FORM_EDIT("/trainers/{id}/edit"),
    TRAINER_OVERVIEW("/trainers/{id}"),
    TRAINER_PERSONAL("/trainers/{id}/personal"),
    TRAINER_MEMBERS("/trainers/{id}/members"),
    TRAINER_SALARY("/trainers/{id}/salary"),
    TRAINER_ACTIVITY("/trainers/{id}/activity"),

    PLANS_LIST("/plans"),
    PLAN_FORM_NEW("/plans/new"),
    PLAN_FORM_EDIT("/plans/{id}/edit"),

    ATTENDANCE("/attendance"),
    PAYMENTS_LIST("/payments"),

    REPORTS("/reports"),

    USERS_LIST("/users"),
    USER_FORM_NEW("/users/new"),
    USER_FORM_EDIT("/users/{id}/edit"),

    SETTINGS_PROFILE("/settings/profile"),
    SETTINGS_PREFERENCES("/settings/preferences"),
    SETTINGS_GYM("/settings/gym"),
    SETTINGS_DATABASE("/settings/database"),
    SETTINGS_ABOUT("/settings/about");

    private final String pathTemplate;
    Route(String pathTemplate) { this.pathTemplate = pathTemplate; }
    public String pathTemplate() { return pathTemplate; }

    /** Render the path with placeholders filled. */
    public String renderPath(Object... params) {
        String s = pathTemplate;
        for (Object p : params) s = s.replaceFirst("\\{[^}]+}", String.valueOf(p));
        return s;
    }
}

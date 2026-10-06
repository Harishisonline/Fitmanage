package com.ironpump.fitmanage.ui.main;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.model.User;
import com.ironpump.fitmanage.ui.components.AvatarLabel;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.Router;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent sidebar. Section labels match the mockups (WORKSPACE / INSIGHTS / ADMIN).
 */
public class Sidebar extends JPanel {
    private final List<Nav> items = new ArrayList<>();
    private JPanel userCard;
    private Route active = Route.DASHBOARD;

    public Sidebar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Ui.SIDEBAR);
        setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));
        setPreferredSize(new Dimension(230, 0));
        setMaximumSize(new Dimension(230, Integer.MAX_VALUE));
        rebuild();
        SessionContext.addListener(this::rebuildUserCard);
    }

    private void rebuild() {
        removeAll();
        items.clear();
        addHeader();
        addNav("Dashboard", 'D', Route.DASHBOARD);
        addSpacer(8);
        addLabel("WORKSPACE");
        addNav("Members", 'M', Route.MEMBERS_LIST);
        addNav("Trainers", 'T', Route.TRAINERS_LIST);
        addNav("Plans", 'B', Route.PLANS_LIST);
        addNav("Attendance", 'A', Route.ATTENDANCE);
        addNav("Payments", 'P', Route.PAYMENTS_LIST);
        addSpacer(8);
        addLabel("INSIGHTS");
        addNav("Reports", 'R', Route.REPORTS);
        User u = SessionContext.current();
        addSpacer(8);
        addLabel("ADMIN");
        if (u != null && u.getRole() == User.Role.ADMIN) {
            addNav("Users", 'U', Route.USERS_LIST);
        }
        addSpacer(8);
        addLabel("ACCOUNT");
        addNav("Settings", 'S', Route.SETTINGS_PROFILE);
        JButton signOut = navButton("Sign out", 'X');
        signOut.addActionListener(e -> com.ironpump.fitmanage.App.signOut());
        add(signOut);
        add(Box.createVerticalGlue());
        userCard = new JPanel();
        add(userCard);
        rebuildUserCard();
        applyActive();
        revalidate();
        repaint();
    }

    public void setActiveRoute(Route route) {
        if (route == null) return;
        active = switch (route) {
            case MEMBER_FORM_NEW, MEMBER_FORM_EDIT, MEMBER_DETAIL, MEMBER_MEMBERSHIPS, MEMBER_PAYMENTS,
                    MEMBER_ATTENDANCE, MEMBER_PROGRESS, MEMBER_ACTIVITY -> Route.MEMBERS_LIST;
            case TRAINER_FORM_NEW, TRAINER_FORM_EDIT, TRAINER_OVERVIEW, TRAINER_PERSONAL, TRAINER_MEMBERS,
                    TRAINER_SALARY, TRAINER_ACTIVITY -> Route.TRAINERS_LIST;
            case PLAN_FORM_NEW, PLAN_FORM_EDIT -> Route.PLANS_LIST;
            case USER_FORM_NEW, USER_FORM_EDIT -> Route.USERS_LIST;
            case SETTINGS_PREFERENCES, SETTINGS_GYM, SETTINGS_DATABASE, SETTINGS_ABOUT -> Route.SETTINGS_PROFILE;
            default -> route;
        };
        applyActive();
    }

    private void applyActive() {
        for (Nav n : items) {
            boolean on = n.route == active;
            n.button.setForeground(on ? Ui.GREEN : Ui.SECONDARY);
            n.button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, on ? 3 : 0, 0, 0, Ui.GREEN),
                    BorderFactory.createEmptyBorder(8, on ? 17 : 20, 8, 12)));
        }
    }

    private void addHeader() {
        JPanel h = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        h.setOpaque(false);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        h.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 12));
        JLabel logo = new JLabel("F", SwingConstants.CENTER);
        logo.setOpaque(true);
        logo.setBackground(Ui.GREEN);
        logo.setForeground(Color.WHITE);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 16f));
        logo.setPreferredSize(new Dimension(28, 28));
        h.add(logo);
        JLabel name = new JLabel("FitManage");
        name.setForeground(Ui.TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        h.add(name);
        add(h);
    }

    private void addLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Ui.MUTED);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 10f));
        l.setBorder(BorderFactory.createEmptyBorder(8, 20, 4, 12));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        add(l);
    }

    private void addNav(String text, char mnemonic, Route route) {
        JButton b = navButton(text, mnemonic);
        b.addActionListener(e -> Router.get().navigateTo(route));
        items.add(new Nav(route, b));
        add(b);
    }

    private JButton navButton(String text, char mnemonic) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBackground(Ui.SIDEBAR);
        b.setForeground(Ui.SECONDARY);
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMnemonic(mnemonic);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setForeground(Ui.GREEN); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                boolean on = items.stream().anyMatch(n -> n.button == b && n.route == active);
                b.setForeground(on ? Ui.GREEN : Ui.SECONDARY);
            }
        });
        return b;
    }

    private void addSpacer(int h) { add(Box.createVerticalStrut(h)); }

    private void rebuildUserCard() {
        if (userCard == null) return;
        userCard.removeAll();
        User u = SessionContext.current();
        if (u == null) return;
        userCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userCard.setBackground(new Color(0x22, 0x2a, 0x33));
        userCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        String display = u.getDisplayName() == null || u.getDisplayName().isBlank() ? u.getUsername() : u.getDisplayName();
        userCard.add(new AvatarLabel(display == null ? "?" : display, 28));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(display == null ? "User" : display);
        name.setForeground(Ui.TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        JLabel role = new JLabel(u.getRole() == null ? "" : u.getRole().name());
        role.setForeground(Ui.SECONDARY);
        role.setFont(role.getFont().deriveFont(11f));
        text.add(name);
        text.add(role);
        userCard.add(text);
        userCard.revalidate();
        userCard.repaint();
    }

    private record Nav(Route route, JButton button) {}
}

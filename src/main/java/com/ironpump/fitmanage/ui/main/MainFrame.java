package com.ironpump.fitmanage.ui.main;

import com.ironpump.fitmanage.auth.SessionContext;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.TopbarSearch;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * The shell. Owns the sidebar + content area + topbar. The topbar shows the
 * breadcrumb chain from the active page's breadcrumbs() and a back-button.
 */
public class MainFrame extends JFrame {
    private final JPanel content = new JPanel(new CardLayout());
    private final JPanel topbar = new JPanel(new BorderLayout());
    private final JPanel breadcrumbBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final Sidebar sidebar;
    private final javax.swing.Timer idleTimer;
    private int idleMinutes = 30;

    public MainFrame() {
        super("FitManage");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                com.ironpump.fitmanage.App.confirmAndExit();
            }
        });

        sidebar = new Sidebar();
        JPanel contentWrap = new JPanel(new BorderLayout());
        contentWrap.setBackground(Ui.BG);
        topbar.setBackground(Ui.BG);
        topbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.BORDER),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        breadcrumbBar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        JButton back = new JButton("←");
        back.setFocusPainted(false);
        back.setContentAreaFilled(false);
        back.setForeground(Ui.TEXT);
        back.addActionListener(e -> Router.get().back());
        left.add(back);
        left.add(breadcrumbBar);
        topbar.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        TopbarSearch searchField = new TopbarSearch("Name, phone, or member code", this::searchMembers);
        JButton newMember = Ui.primary("+ New Member");
        newMember.addActionListener(e -> Router.get().navigateTo(Route.MEMBER_FORM_NEW));
        right.add(searchField);
        right.add(newMember);
        topbar.add(right, BorderLayout.EAST);
        content.setBackground(Ui.BG);
        contentWrap.add(topbar, BorderLayout.NORTH);
        contentWrap.add(content, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(contentWrap, BorderLayout.CENTER);

        Router.get().setHost(content);
        Router.get().setBreadcrumbListener(this::refreshBreadcrumb);
        Router.get().setRouteListener(sidebar::setActiveRoute);
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener(), AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        idleTimer = new javax.swing.Timer(60_000, e -> {
            if (SessionContext.isExpired()) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Your session has expired. Sign in again?",
                        "Session expired", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) com.ironpump.fitmanage.App.signOut();
                else { dispose(); System.exit(0); }
            }
        });
        idleTimer.start();
    }

    private AWTEventListener activityListener() {
        return event -> {
            if (event instanceof MouseEvent || event instanceof KeyEvent) SessionContext.touch(idleMinutes);
        };
    }

    /** Members by name, code, or phone. One match opens the profile; several open the filtered list. */
    private void searchMembers(String query) {
        if (query == null || query.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Type a name, phone, or member code, then press Enter or Search.",
                    "Search", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                var rows = new MemberService().listDerived(null, null, query, null, null, 50, 0);
                if (rows.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "No member matched \"" + query + "\".",
                            "Search", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (rows.size() == 1) {
                    Router.get().navigateTo(Route.MEMBER_DETAIL,
                            RouteContext.of("id", rows.get(0).getMember().getMemberId()));
                    return;
                }
                Router.get().navigateTo(Route.MEMBERS_LIST, RouteContext.of("q", query));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        ex.getMessage() == null ? "Search failed." : ex.getMessage(),
                        "Search", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    /** Called by the router every time the active page changes. */
    public void refreshBreadcrumb(List<BasePage.BreadcrumbItem> items) {
        breadcrumbBar.removeAll();
        for (int i = 0; i < items.size(); i++) {
            BasePage.BreadcrumbItem bi = items.get(i);
            JButton link = new JButton(bi.label());
            link.setOpaque(false);
            link.setContentAreaFilled(false);
            link.setBorderPainted(false);
            link.setForeground(new Color(0x22, 0xC5, 0x5E));
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addActionListener(e -> bi.onClick().run());
            breadcrumbBar.add(link);
            if (i < items.size() - 1) {
                JLabel sep = new JLabel(" / ");
                sep.setForeground(new Color(0x5b, 0x67, 0x72));
                breadcrumbBar.add(sep);
            }
        }
        breadcrumbBar.revalidate();
        breadcrumbBar.repaint();
    }

    public void setIdleMinutes(int m) { this.idleMinutes = m; }
}

package com.ironpump.fitmanage.ui.login;

import com.ironpump.fitmanage.App;
import com.ironpump.fitmanage.auth.AuthService;
import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.dao.UserDao;
import com.ironpump.fitmanage.exception.FitManageException;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/** Visual: see assets/ui-mockups/01-login.html. */
public class LoginFrame extends JFrame {
    private final JTextField user = new JTextField(20);
    private final JPasswordField pass = new JPasswordField(20);
    private final JCheckBox remember = new JCheckBox("Remember me");
    private final JLabel error = new JLabel(" ");
    private final AuthService auth;
    private final Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);

    public LoginFrame(AppConfig cfg) {
        super("FitManage — Sign in");
        this.auth = new AuthService(new UserDao(), cfg);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1040, 640);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridLayout(1, 2));
        JPanel left = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0a, 0x3d, 0x2e), getWidth(), getHeight(), new Color(0x0f, 0x14, 0x19)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(48, 48, 36, 48));
        JLabel logo = new JLabel("F", SwingConstants.CENTER);
        logo.setOpaque(true);
        logo.setBackground(Ui.GREEN);
        logo.setForeground(Color.WHITE);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 22f));
        logo.setPreferredSize(new Dimension(48, 48));
        logo.setMaximumSize(new Dimension(48, 48));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(logo);
        left.add(Box.createVerticalStrut(18));
        JLabel brand = new JLabel("FitManage");
        brand.setForeground(Ui.TEXT);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 16f));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(brand);
        left.add(Box.createVerticalStrut(28));
        JLabel title = new JLabel("<html><span style='font-size:28px;color:#e6edf3'>Run your gym<br/><b style='color:#22c55e'>without the paperwork.</b></span></html>");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(title);
        left.add(Box.createVerticalStrut(12));
        JLabel para = new JLabel("<html><body style='width:320px;color:#8b97a4'>Members, plans, attendance, and payments in one desktop workspace. Built for the front desk, not a spreadsheet.</body></html>");
        para.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(para);
        left.add(Box.createVerticalGlue());
        JPanel stats = new JPanel(new GridLayout(1, 3, 8, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(stat("12k+", "GYMS WORLDWIDE"));
        stats.add(stat("99.9%", "UPTIME"));
        stats.add(stat("4.8★", "OPERATOR RATING"));
        left.add(stats);
        left.add(Box.createVerticalStrut(16));
        JLabel copy = new JLabel("© 2026 FitManage");
        copy.setForeground(Ui.MUTED);
        copy.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(copy);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(new Color(0x1a, 0x20, 0x27));
        right.setBorder(BorderFactory.createEmptyBorder(48, 48, 36, 48));
        JLabel signin = new JLabel("Welcome back");
        signin.setFont(signin.getFont().deriveFont(Font.BOLD, 26f));
        signin.setForeground(Ui.TEXT);
        signin.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(signin);
        JLabel sub = new JLabel("Sign in to your gym's workspace.");
        sub.setForeground(Ui.SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(sub);
        right.add(Box.createVerticalStrut(24));
        Ui.styleField(user);
        Ui.styleField(pass);
        user.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        pass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        if (prefs.getBoolean("remember", false)) user.setText(prefs.get("username", ""));
        right.add(labeled("USERNAME", user));
        right.add(Box.createVerticalStrut(12));
        right.add(labeled("PASSWORD", pass));
        right.add(Box.createVerticalStrut(8));
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        remember.setOpaque(false);
        remember.setForeground(Ui.SECONDARY);
        remember.setSelected(prefs.getBoolean("remember", false));
        row.add(remember, BorderLayout.WEST);
        JButton forgot = new JButton("Forgot password?");
        forgot.setBorderPainted(false);
        forgot.setContentAreaFilled(false);
        forgot.setForeground(Ui.GREEN);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Ask an admin to reset the password from Users → Reset password.",
                "Forgot password", JOptionPane.INFORMATION_MESSAGE));
        row.add(forgot, BorderLayout.EAST);
        right.add(row);
        error.setForeground(Ui.RED);
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(error);
        JButton go = Ui.primary("Sign in");
        go.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        go.setAlignmentX(Component.LEFT_ALIGNMENT);
        go.addActionListener(e -> tryLogin());
        right.add(go);
        right.add(Box.createVerticalStrut(16));
        JLabel or = new JLabel("OR CONTINUE WITH", SwingConstants.CENTER);
        or.setForeground(Ui.MUTED);
        or.setFont(or.getFont().deriveFont(11f));
        or.setAlignmentX(Component.CENTER_ALIGNMENT);
        right.add(or);
        right.add(Box.createVerticalStrut(10));
        JPanel demos = new JPanel(new GridLayout(1, 2, 8, 0));
        demos.setOpaque(false);
        demos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JButton staff = Ui.secondary("Demo Staff");
        JButton trainer = Ui.secondary("Demo Trainer");
        staff.addActionListener(e -> fill("staff1", "staff123"));
        trainer.addActionListener(e -> fill("trainer1", "train123"));
        demos.add(staff);
        demos.add(trainer);
        right.add(demos);
        right.add(Box.createVerticalGlue());
        JLabel badge = new JLabel("v1.0.0 · Secure session");
        badge.setForeground(Ui.MUTED);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(badge);

        root.add(left);
        root.add(right);
        setContentPane(root);
        getRootPane().setDefaultButton(go);
    }

    private void fill(String u, String p) {
        user.setText(u);
        pass.setText(p);
        tryLogin();
    }

    private JPanel stat(String n, String label) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel num = new JLabel(n);
        num.setForeground(Ui.TEXT);
        num.setFont(num.getFont().deriveFont(Font.BOLD, 16f));
        JLabel l = new JLabel(label);
        l.setForeground(Ui.MUTED);
        l.setFont(l.getFont().deriveFont(9f));
        p.add(num);
        p.add(l);
        return p;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setForeground(Ui.SECONDARY);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private void tryLogin() {
        error.setText(" ");
        try {
            auth.login(user.getText().trim(), new String(pass.getPassword()));
            if (remember.isSelected()) {
                prefs.putBoolean("remember", true);
                prefs.put("username", user.getText().trim());
            } else {
                prefs.putBoolean("remember", false);
                prefs.remove("username");
            }
            App.onLoginSuccess(this);
        } catch (FitManageException e) {
            error.setText(e.getUserMessage());
            pass.setText("");
        } catch (Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }
}

package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.App;
import com.ironpump.fitmanage.model.Member;
import com.ironpump.fitmanage.model.Membership;
import com.ironpump.fitmanage.model.MembershipView;
import com.ironpump.fitmanage.service.AttendanceService;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.service.MembershipService;
import com.ironpump.fitmanage.service.PaymentService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.*;
import com.ironpump.fitmanage.ui.payments.PaymentFormDialog;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.ReceiptRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Visual: see assets/ui-mockups/14-member-detail.html. */
public class MemberDetailPage extends BasePage {
    private static final Logger log = LoggerFactory.getLogger(MemberDetailPage.class);
    private final MemberService members = new MemberService();
    private final MembershipService memberships = new MembershipService();
    private final PaymentService payments = new PaymentService();
    private final AttendanceService attendance = new AttendanceService();
    private final JLabel status = new JLabel(" ");
    private final JPanel body = new JPanel(new BorderLayout());
    private int memberId;
    private String cachedName = "Member";

    public MemberDetailPage() {
        setLayout(new BorderLayout());
        setBackground(Ui.BG);
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
        status.setForeground(Ui.AMBER);
        add(status, BorderLayout.SOUTH);
    }

    @Override public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) memberId = ctx.get("id");
        reload();
    }

    private void reload() {
        body.removeAll();
        try {
            Member m = members.findById(memberId);
            cachedName = m.getFullName();
            MembershipView active = memberships.activeView(memberId).orElse(null);
            int visits = attendance.countByMemberSince(memberId, 30);
            BigDecimal paid = payments.sumForMember(memberId);
            int receipts = payments.listForMember(memberId).size();
            String derived = "ACTIVE";
            String expires = "—";
            String expiresSub = "—";
            if (m.getStatus() == Member.Status.INACTIVE) derived = "INACTIVE";
            else if (active == null || active.getMembership().getEndDate().isBefore(java.time.LocalDate.now())) derived = "EXPIRED";
            else if (!active.getMembership().getEndDate().isAfter(java.time.LocalDate.now().plusDays(7))) derived = "EXPIRING";
            if (active != null) {
                long days = ChronoUnit.DAYS.between(java.time.LocalDate.now(), active.getMembership().getEndDate());
                expires = days + " days";
                expiresSub = DateUtils.format(active.getMembership().getEndDate());
            }

            JPanel page = Ui.page();
            page.setLayout(new BorderLayout(0, 12));
            Card hero = new Card();
            JPanel heroRow = new JPanel(new BorderLayout(12, 0));
            heroRow.setOpaque(false);
            heroRow.add(new AvatarLabel(m.getFullName(), 64), BorderLayout.WEST);
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JPanel nameRow = Ui.row(8);
            JLabel name = new JLabel(m.getFullName());
            name.setForeground(Ui.TEXT);
            name.setFont(name.getFont().deriveFont(Font.BOLD, 22f));
            nameRow.add(name);
            nameRow.add(StatusBadge.forMemberStatus(derived));
            text.add(nameRow);
            text.add(Ui.sub(m.getMemberCode() + " · " + m.getPhone() + " · Joined " + DateUtils.format(m.getJoinedAt())));
            heroRow.add(text, BorderLayout.CENTER);
            JPanel actions = Ui.row(8);
            JButton print = Ui.secondary("Print summary");
            print.addActionListener(e -> printSummary(m, active, visits, paid, receipts));
            JButton deact = Ui.danger(m.getStatus() == Member.Status.INACTIVE ? "Reactivate" : "Deactivate");
            deact.addActionListener(e -> {
                try {
                    if (m.getStatus() == Member.Status.INACTIVE) members.reactivate(memberId);
                    else members.deactivate(memberId);
                    refreshSoon();
                } catch (Exception ex) { status.setText(ex.getMessage()); }
            });
            JButton edit = Ui.primary("Edit");
            edit.addActionListener(e -> Router.get().navigateTo(Route.MEMBER_FORM_EDIT, RouteContext.of("id", memberId)));
            actions.add(print); actions.add(deact); actions.add(edit);
            heroRow.add(actions, BorderLayout.EAST);
            hero.add(heroRow, BorderLayout.CENTER);
            page.add(hero, BorderLayout.NORTH);

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(tabs());
            center.add(Box.createVerticalStrut(10));
            JPanel kpis = Ui.kpiRow(
                    new KpiCard("Current plan", active == null ? "—" : active.getPlanName(), active == null ? "" : "Rs. " + active.getPrice()),
                    new KpiCard("Expires", expires, expiresSub),
                    new KpiCard("Trainer", active == null || active.getTrainerName() == null ? "—" : active.getTrainerName()),
                    new KpiCard("Visits (30d)", String.valueOf(visits), "last 30 days"),
                    new KpiCard("Lifetime paid", "Rs. " + paid, receipts + " receipt" + (receipts == 1 ? "" : "s")));
            kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            center.add(kpis);
            center.add(Box.createVerticalStrut(12));

            JPanel cols = new JPanel(new GridLayout(1, 2, 12, 0));
            cols.setOpaque(false);
            Card personal = new Card("Personal information");
            JLabel info = new JLabel("<html>"
                    + line("Full name", m.getFullName())
                    + line("Date of birth", DateUtils.format(m.getDob()))
                    + line("Gender", m.getGender() == null ? "—" : m.getGender().name())
                    + line("Phone", m.getPhone())
                    + line("Email", m.getEmail())
                    + line("Address", m.getAddress())
                    + line("Emergency", (m.getEmergencyName() == null ? "" : m.getEmergencyName()) + " " + (m.getEmergencyPhone() == null ? "" : m.getEmergencyPhone()))
                    + line("Height / weight", (m.getHeightCm() == null ? "—" : m.getHeightCm() + " cm") + " · " + (m.getWeightKg() == null ? "—" : m.getWeightKg() + " kg"))
                    + line("Medical notes", m.getMedicalNotes())
                    + "</html>");
            info.setForeground(Ui.TEXT);
            personal.add(info, BorderLayout.CENTER);
            cols.add(personal);

            Membership upcomingMs = memberships.upcomingForMember(memberId).orElse(null);
            Card sub = new Card("Current subscription");
            String plan = active == null ? "No active plan" : active.getPlanName();
            String period = active == null ? "—" : DateUtils.format(active.getMembership().getStartDate()) + " – " + DateUtils.format(active.getMembership().getEndDate());
            String upcomingLine = upcomingMs == null ? "—" : "Starts " + DateUtils.format(upcomingMs.getStartDate()) + ", ends " + DateUtils.format(upcomingMs.getEndDate());
            JLabel subText = new JLabel("<html>" + line("Plan", plan) + line("Period", period)
                    + line("Trainer", active == null ? "—" : active.getTrainerName())
                    + line("Price", active == null ? "—" : "Rs. " + active.getPrice())
                    + line("Scheduled renewal", upcomingLine) + "</html>");
            subText.setForeground(Ui.TEXT);
            Membership restore = active == null ? memberships.reactivatableForMember(memberId).orElse(null) : null;
            JPanel subBtns = Ui.row(8);
            JButton renew = Ui.primary(active == null ? "Add plan" : "Renew");
            renew.addActionListener(e -> scheduleOrStart(active));
            JButton upgrade = Ui.secondary("Upgrade");
            upgrade.addActionListener(e -> upgradePlan(active));
            JButton reactivate = Ui.secondary("Reactivate");
            reactivate.setVisible(restore != null);
            reactivate.addActionListener(e -> reactivatePlan(restore));
            JButton cancel = Ui.danger("Cancel");
            cancel.addActionListener(e -> cancelOpen(active, upcomingMs));
            subBtns.add(renew); subBtns.add(upgrade); subBtns.add(reactivate); subBtns.add(cancel);
            JPanel subWrap = new JPanel(new BorderLayout());
            subWrap.setOpaque(false);
            subWrap.add(subText, BorderLayout.CENTER);
            subWrap.add(subBtns, BorderLayout.SOUTH);
            sub.add(subWrap, BorderLayout.CENTER);
            cols.add(sub);
            center.add(cols);
            center.add(Box.createVerticalStrut(12));
            Card quick = new Card("Quick actions");
            JButton check = Ui.secondary("Check in now");
            check.addActionListener(ev -> Router.get().navigateTo(Route.ATTENDANCE));
            JButton rec = Ui.secondary("Record payment");
            rec.addActionListener(ev -> new PaymentFormDialog(SwingUtilities.getWindowAncestor(this), memberId, payments.listForMember(memberId), this::refreshSoon).setVisible(true));
            JButton prog = Ui.secondary("View progress");
            prog.addActionListener(ev -> Router.get().navigateTo(Route.MEMBER_PROGRESS, RouteContext.of("id", memberId)));
            JButton last = Ui.secondary("Print last receipt");
            last.addActionListener(ev -> printLastReceipt());
            JButton mem = Ui.secondary("Memberships");
            mem.addActionListener(ev -> Router.get().navigateTo(Route.MEMBER_MEMBERSHIPS, RouteContext.of("id", memberId)));
            JButton act = Ui.secondary("Activity");
            act.addActionListener(ev -> Router.get().navigateTo(Route.MEMBER_ACTIVITY, RouteContext.of("id", memberId)));
            quick.add(new QuickActions(check, rec, prog, last, mem, act), BorderLayout.CENTER);
            center.add(quick);
            page.add(new JScrollPane(center), BorderLayout.CENTER);
            body.add(page);
            status.setText(" ");
        } catch (Exception ex) {
            log.error("Member detail failed", ex);
            status.setText(ex.getMessage());
        }
        body.revalidate();
        body.repaint();
    }

    private void refreshSoon() {
        SwingUtilities.invokeLater(this::reload);
    }

    private void scheduleOrStart(MembershipView active) {
        try {
            if (active != null) {
                memberships.renew(memberId, active.getMembership().getPlanId(), active.getMembership().getTrainerId());
                refreshSoon();
                return;
            }
            Membership previous = memberships.listForMember(memberId).stream().findFirst().orElse(null);
            if (previous != null) {
                memberships.startPlan(memberId, previous.getPlanId(), previous.getTrainerId());
                refreshSoon();
                return;
            }
            askAndStart("Add plan");
        } catch (Exception ex) {
            log.error("Could not add or renew plan", ex);
            String msg = message(ex);
            boolean replace = msg.contains("covers today") || msg.contains("renewal") || msg.contains("scheduled");
            int pick = JOptionPane.showConfirmDialog(this,
                    msg + (replace ? "\n\nUpgrade replaces the open plan and starts a new one today." : "\n\nChoose a plan manually?"),
                    active == null ? "Add plan" : "Renew",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (pick != JOptionPane.OK_OPTION) {
                status.setText(msg);
                return;
            }
            if (replace) upgradePlan(active);
            else askAndStart(active == null ? "Add plan" : "Renew");
        }
    }

    private void askAndStart(String title) {
        PlanChoiceDialog.Choice choice = PlanChoiceDialog.ask(this, title);
        if (choice == null) return;
        try {
            memberships.startPlan(memberId, choice.planId(), choice.trainerId());
            refreshSoon();
        } catch (Exception ex) {
            log.error("Add plan failed", ex);
            alert(title, ex);
        }
    }

    private void upgradePlan(MembershipView active) {
        int ok = JOptionPane.showConfirmDialog(this,
                active == null
                        ? "Pick a plan to start today."
                        : "Upgrade replaces the plan that covers today, and any scheduled renewal, with a different plan starting today.",
                "Upgrade plan", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        PlanChoiceDialog.Choice choice = PlanChoiceDialog.ask(this, "Upgrade plan");
        if (choice == null) return;
        if (active != null && choice.planId() == active.getMembership().getPlanId()) {
            alert("Upgrade plan", "Pick a different plan. Renew keeps this plan and schedules the next period.");
            return;
        }
        try {
            memberships.replacePlan(memberId, choice.planId(), choice.trainerId());
            refreshSoon();
        } catch (Exception ex) {
            log.error("Upgrade plan failed", ex);
            alert("Upgrade plan", ex);
        }
    }

    private void reactivatePlan(Membership cancelled) {
        if (cancelled == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Restore the cancelled plan through " + DateUtils.format(cancelled.getEndDate()) + "?",
                "Reactivate plan", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            memberships.reactivateMembership(cancelled.getMembershipId());
            refreshSoon();
        } catch (Exception ex) {
            log.error("Reactivate plan failed", ex);
            alert("Reactivate plan", ex);
        }
    }

    private void alert(String title, Exception ex) { alert(title, message(ex)); }

    private void alert(String title, String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private static String message(Exception ex) {
        return ex.getMessage() == null ? "That action could not be completed." : ex.getMessage();
    }

    private void cancelOpen(MembershipView active, Membership upcoming) {
        if (active == null && upcoming == null) {
            alert("Cancel plan", "There is no plan to cancel. Use Add plan, Renew, or Upgrade.");
            return;
        }
        boolean current = false;
        boolean scheduled = false;
        if (active != null && upcoming != null) {
            String[] options = {"Cancel current", "Cancel scheduled", "Cancel both"};
            int pick = JOptionPane.showOptionDialog(this,
                    "Current plan runs through " + DateUtils.format(active.getMembership().getEndDate())
                            + ". A renewal is scheduled through " + DateUtils.format(upcoming.getEndDate()) + ".",
                    "Cancel plan", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (pick < 0) return;
            current = pick == 0 || pick == 2;
            scheduled = pick == 1 || pick == 2;
        } else if (active != null) {
            int ok = JOptionPane.showConfirmDialog(this, "Cancel the plan that covers today?", "Cancel plan", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            current = true;
        } else {
            int ok = JOptionPane.showConfirmDialog(this, "Cancel the scheduled renewal?", "Cancel plan", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            scheduled = true;
        }
        try {
            if (current) memberships.cancel(active.getMembership().getMembershipId(), "Cancelled current plan");
            if (scheduled) memberships.cancel(upcoming.getMembershipId(), "Cancelled scheduled renewal");
            refreshSoon();
        } catch (Exception ex) {
            alert("Cancel plan", ex);
        }
    }

    private void printSummary(Member m, MembershipView active, int visits, BigDecimal paid, int receipts) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(m.getMemberCode() + "-summary.pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = pdfPath(fc.getSelectedFile().toPath());
        String gym = App.cfg() == null ? "FitManage" : App.cfg().getOrDefault("app.gym.name", "FitManage");
        String plan = active == null ? "No active plan" : active.getPlanName();
        String period = active == null ? "-" : DateUtils.format(active.getMembership().getStartDate()) + " - " + DateUtils.format(active.getMembership().getEndDate());
        ReceiptRenderer.writePdf(path, new String[]{
                "#" + gym,
                "#Member summary",
                "Name: " + m.getFullName(),
                "Member ID: " + m.getMemberCode(),
                "Phone: " + m.getPhone(),
                "Email: " + (m.getEmail() == null ? "-" : m.getEmail()),
                "Joined: " + DateUtils.format(m.getJoinedAt()),
                "Status: " + m.getStatus(),
                "Plan: " + plan,
                "Period: " + period,
                "Trainer: " + (active == null || active.getTrainerName() == null ? "-" : active.getTrainerName()),
                "Visits in 30 days: " + visits,
                "Lifetime paid: Rs. " + paid + " (" + receipts + " receipts)"
        });
        status.setText("Saved " + path);
        openFile(path);
    }

    private void printLastReceipt() {
        var list = payments.listForMember(memberId);
        if (list.isEmpty()) {
            status.setText("No receipt to print yet.");
            return;
        }
        var payment = list.get(0);
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(payment.getReceiptNo() + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Path path = pdfPath(fc.getSelectedFile().toPath());
            ReceiptRenderer.writeReceipt(path, members.findById(memberId), payment, App.cfg());
            status.setText("Saved " + path);
            openFile(path);
        } catch (Exception ex) {
            status.setText(ex.getMessage());
        }
    }

    private static Path pdfPath(Path path) {
        String name = path.toString();
        if (!name.toLowerCase().endsWith(".pdf")) path = Path.of(name + ".pdf");
        return path.toAbsolutePath();
    }

    private void openFile(Path path) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(path.toFile());
        } catch (Exception ex) {
            log.warn("Could not open {}", path, ex);
        }
    }

    private JPanel tabs() {
        JPanel p = Ui.row(4);
        addTab(p, "Profile", Route.MEMBER_DETAIL, true);
        addTab(p, "Memberships", Route.MEMBER_MEMBERSHIPS, false);
        addTab(p, "Payments", Route.MEMBER_PAYMENTS, false);
        addTab(p, "Attendance", Route.MEMBER_ATTENDANCE, false);
        addTab(p, "Progress", Route.MEMBER_PROGRESS, false);
        addTab(p, "Activity log", Route.MEMBER_ACTIVITY, false);
        return p;
    }

    private void addTab(JPanel p, String label, Route route, boolean on) {
        JButton b = on ? Ui.primary(label) : Ui.secondary(label);
        b.addActionListener(e -> Router.get().navigateTo(route, RouteContext.of("id", memberId)));
        p.add(b);
    }

    private static String line(String k, String v) {
        return "<span style='color:#8b97a4'>" + k + "</span><br><b>" + (v == null || v.isBlank() ? "—" : v) + "</b><br>";
    }

    @Override public List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Members", () -> Router.get().navigateTo(Route.MEMBERS_LIST)),
                new BreadcrumbItem(cachedName, () -> {}));
    }
}

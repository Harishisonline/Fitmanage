package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.Member;
import com.ironpump.fitmanage.model.MembershipView;
import com.ironpump.fitmanage.service.MemberService;
import com.ironpump.fitmanage.service.MembershipService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.components.RowAction;
import com.ironpump.fitmanage.ui.components.Ui;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Visual: see assets/ui-mockups/21-member-memberships.html. */
public class MemberMembershipsPage extends BasePage {
    private final MembershipService service = new MembershipService();
    private final MemberService members = new MemberService();
    private final PagedTable table = new PagedTable(new String[]{"Plan", "Period", "Trainer", "Price", "Status"});
    private final JLabel header = new JLabel("Memberships");
    private final JLabel status = new JLabel(" ");
    private int memberId;
    private String memberName = "Member";
    private List<MembershipView> loaded = List.of();

    public MemberMembershipsPage() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 22f));
        header.setForeground(Ui.TEXT);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header, BorderLayout.WEST);
        JPanel actions = Ui.row(8);
        JButton add = Ui.primary("Add plan");
        add.addActionListener(e -> addPlan());
        JButton renew = Ui.secondary("Renew");
        renew.addActionListener(e -> renewCurrent());
        JButton upgrade = Ui.secondary("Upgrade");
        upgrade.addActionListener(e -> upgrade());
        actions.add(add);
        actions.add(renew);
        actions.add(upgrade);
        top.add(actions, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        table.addActionColumn(List.of(
                new RowAction("Reactivate", Ui.GREEN, this::reactivateRow),
                new RowAction("Cancel", Ui.RED, this::cancelRow)));
        add(table, BorderLayout.CENTER);
        status.setForeground(Ui.AMBER);
        add(status, BorderLayout.SOUTH);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) memberId = ctx.get("id");
        reload();
    }

    private void reload() {
        try {
            Member member = members.findById(memberId);
            memberName = member.getFullName();
            header.setText("Memberships · " + memberName);
            loaded = service.viewsForMember(memberId);
            List<String[]> data = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            for (MembershipView v : loaded) {
                var m = v.getMembership();
                data.add(new String[]{
                        v.getPlanName() == null ? "Plan" : v.getPlanName(),
                        DateUtils.format(m.getStartDate()) + " – " + DateUtils.format(m.getEndDate()),
                        v.getTrainerName() == null ? "—" : v.getTrainerName(),
                        "Rs. " + v.getPrice(),
                        m.displayStatus()
                });
                ids.add(m.getMembershipId());
            }
            table.setRowIds(ids);
            table.setRows(data);
            status.setText("Upcoming is a renewal that has not started. Expired means the end date has passed.");
        } catch (Exception ex) {
            status.setText(ex.getMessage());
        }
    }

    private void addPlan() {
        PlanChoiceDialog.Choice choice = PlanChoiceDialog.ask(this, "Add plan");
        if (choice == null) return;
        try {
            service.startPlan(memberId, choice.planId(), choice.trainerId());
            SwingUtilities.invokeLater(this::reload);
        } catch (Exception ex) {
            fail("Add plan", ex);
        }
    }

    private void renewCurrent() {
        try {
            var active = service.activeView(memberId).orElse(null);
            if (active != null) {
                service.renew(memberId, active.getMembership().getPlanId(), active.getMembership().getTrainerId());
                SwingUtilities.invokeLater(this::reload);
                return;
            }
            var previous = service.listForMember(memberId).stream().findFirst().orElse(null);
            if (previous == null) {
                addPlan();
                return;
            }
            service.startPlan(memberId, previous.getPlanId(), previous.getTrainerId());
            SwingUtilities.invokeLater(this::reload);
        } catch (Exception ex) {
            fail("Renew", ex);
        }
    }

    private void upgrade() {
        var active = service.activeView(memberId).orElse(null);
        PlanChoiceDialog.Choice choice = PlanChoiceDialog.ask(this, "Upgrade plan");
        if (choice == null) return;
        if (active != null && choice.planId() == active.getMembership().getPlanId()) {
            fail("Upgrade plan", "Pick a different plan. Renew keeps the current plan.");
            return;
        }
        try {
            service.replacePlan(memberId, choice.planId(), choice.trainerId());
            SwingUtilities.invokeLater(this::reload);
        } catch (Exception ex) {
            fail("Upgrade plan", ex);
        }
    }

    private void reactivateRow(int row) {
        int id = table.idAt(row);
        if (id <= 0) return;
        MembershipView view = loaded.stream().filter(v -> v.getMembership().getMembershipId() == id).findFirst().orElse(null);
        if (view == null) return;
        if (!"CANCELLED".equals(view.getMembership().displayStatus())) {
            fail("Reactivate", "Only a cancelled plan can be reactivated.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Restore this cancelled plan?", "Reactivate", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            service.reactivateMembership(id);
            SwingUtilities.invokeLater(this::reload);
        } catch (Exception ex) {
            fail("Reactivate", ex);
        }
    }

    private void fail(String title, Exception ex) {
        fail(title, ex.getMessage() == null ? "That action could not be completed." : ex.getMessage());
    }

    private void fail(String title, String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void cancelRow(int row) {
        int id = table.idAt(row);
        if (id <= 0) return;
        MembershipView view = loaded.stream().filter(v -> v.getMembership().getMembershipId() == id).findFirst().orElse(null);
        if (view == null) return;
        String shown = view.getMembership().displayStatus();
        if ("CANCELLED".equals(shown) || "EXPIRED".equals(shown)) {
            status.setText("That plan is already " + shown.toLowerCase() + ".");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Cancel this " + shown.toLowerCase() + " plan?", "Cancel", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            service.cancel(id, "Cancelled from memberships");
            SwingUtilities.invokeLater(this::reload);
        } catch (Exception ex) {
            status.setText(ex.getMessage());
        }
    }

    @Override
    public List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Members", () -> Router.get().navigateTo(Route.MEMBERS_LIST)),
                new BreadcrumbItem(memberName, () -> Router.get().navigateTo(Route.MEMBER_DETAIL, RouteContext.of("id", memberId))),
                new BreadcrumbItem("Memberships", () -> {}));
    }
}

package com.ironpump.fitmanage.ui.trainers;

import com.ironpump.fitmanage.service.MembershipService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.PagedTable;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.DateUtils;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class TrainerMembersPage extends BasePage {
    private final MembershipService service = new MembershipService();
    private final PagedTable table = new PagedTable(new String[]{"Member", "Plan", "Start", "End", "Status"});
    private int trainerId;

    public TrainerMembersPage() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel h1 = new JLabel("Members assigned to this trainer");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 20f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));
        add(h1, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            trainerId = ctx.get("id");
            try {
                List<com.ironpump.fitmanage.model.MembershipView> rows = service.viewsForTrainer(trainerId);
                List<String[]> data = rows.stream().map(v -> new String[]{
                        v.getMemberName() == null ? "Member" : v.getMemberName(),
                        v.getPlanName() == null ? "Plan" : v.getPlanName(),
                        DateUtils.format(v.getMembership().getStartDate()),
                        DateUtils.format(v.getMembership().getEndDate()),
                        v.getMembership().displayStatus()
                }).collect(Collectors.toList());
                table.setRows(data);
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Trainers", () -> Router.get().navigateTo(Route.TRAINERS_LIST)),
                new BreadcrumbItem("#" + trainerId, () -> Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", trainerId))),
                new BreadcrumbItem("Members", () -> {}));
    }
}

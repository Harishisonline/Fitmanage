package com.ironpump.fitmanage.ui.members;

import com.ironpump.fitmanage.model.ActivityLog;
import com.ironpump.fitmanage.service.ActivityLogService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.TimelinePanel;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class MemberActivityPage extends BasePage {
    private final ActivityLogService service = new ActivityLogService();
    private final TimelinePanel timeline = new TimelinePanel(java.util.List.of());
    private int memberId;

    public MemberActivityPage() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        add(timeline, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            memberId = ctx.get("id");
            try {
                List<ActivityLog> recent = service.forMember(memberId, 100);
                timeline.setItems(recent);
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    private String memberLabel() {
        try { return new com.ironpump.fitmanage.service.MemberService().findById(memberId).getFullName(); }
        catch (Exception e) { return "Member"; }
    }

    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Members", () -> Router.get().navigateTo(Route.MEMBERS_LIST)),
                new BreadcrumbItem(memberLabel(), () -> Router.get().navigateTo(Route.MEMBER_DETAIL, RouteContext.of("id", memberId))),
                new BreadcrumbItem("Activity log", () -> {}));
    }
}

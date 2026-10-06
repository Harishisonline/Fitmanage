package com.ironpump.fitmanage.ui.trainers;

import com.ironpump.fitmanage.model.ActivityLog;
import com.ironpump.fitmanage.service.ActivityLogService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.components.TimelinePanel;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class TrainerActivityPage extends BasePage {
    private final ActivityLogService service = new ActivityLogService();
    private final TimelinePanel timeline = new TimelinePanel(List.of());
    private int trainerId;

    public TrainerActivityPage() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel h1 = new JLabel("Trainer activity log");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 20f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));
        add(h1, BorderLayout.NORTH);
        add(timeline, BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            trainerId = ctx.get("id");
            try {
                List<ActivityLog> recent = service.forTrainer(trainerId, 200);
                timeline.setItems(recent);
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Trainers", () -> Router.get().navigateTo(Route.TRAINERS_LIST)),
                new BreadcrumbItem("#" + trainerId, () -> Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", trainerId))),
                new BreadcrumbItem("Activity", () -> {}));
    }
}

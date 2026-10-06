package com.ironpump.fitmanage.ui.trainers;

import com.ironpump.fitmanage.model.Trainer;
import com.ironpump.fitmanage.service.TrainerService;
import com.ironpump.fitmanage.ui.base.BasePage;
import com.ironpump.fitmanage.ui.router.Route;
import com.ironpump.fitmanage.ui.router.RouteContext;
import com.ironpump.fitmanage.ui.router.Router;
import com.ironpump.fitmanage.util.GlobalExceptionHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class TrainerPersonalPage extends BasePage {
    private final TrainerService service = new TrainerService();
    private final JTextArea body = new JTextArea();
    private int trainerId;

    public TrainerPersonalPage() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x0f, 0x14, 0x19));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel h1 = new JLabel("Personal & employment details");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 20f));
        h1.setForeground(new Color(0xe6, 0xed, 0xf3));
        add(h1, BorderLayout.NORTH);
        body.setEditable(false); body.setOpaque(true);
        body.setBackground(new Color(0x1a, 0x20, 0x27));
        body.setForeground(new Color(0xe6, 0xed, 0xf3));
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(new JScrollPane(body), BorderLayout.CENTER);
    }

    @Override
    public void onEnter(RouteContext ctx) {
        if (ctx != null && ctx.has("id")) {
            trainerId = ctx.get("id");
            try {
                Trainer t = service.findById(trainerId);
                body.setText(
                        "Trainer ID:       " + t.getTrainerId() + "\n" +
                        "Full name:        " + t.getName() + "\n" +
                        "Specialization:   " + (t.getSpecialization() == null ? "—" : t.getSpecialization()) + "\n" +
                        "Phone:            " + (t.getPhone() == null ? "—" : t.getPhone()) + "\n" +
                        "Email:            " + (t.getEmail() == null ? "—" : t.getEmail()) + "\n" +
                        "Date of birth:    " + (t.getDob() == null ? "—" : t.getDob()) + "\n" +
                        "Gender:           " + (t.getGender() == null ? "—" : t.getGender().name()) + "\n" +
                        "Join date:        " + t.getJoinDate() + "\n" +
                        "Monthly salary:   Rs. " + t.getSalary() + "\n" +
                        "Active:           " + (t.isActive() ? "Yes" : "No") + "\n" +
                        "Availability:     " + (t.getAvailability() == null ? "—" : t.getAvailability()) + "\n" +
                        "Notes:            " + (t.getNotes() == null ? "—" : t.getNotes())
                );
            } catch (Exception e) { GlobalExceptionHandler.handle(e); }
        }
    }

    @Override
    public java.util.List<BreadcrumbItem> breadcrumbs(RouteContext ctx) {
        return List.of(
                new BreadcrumbItem("Home", () -> Router.get().navigateTo(Route.DASHBOARD)),
                new BreadcrumbItem("Trainers", () -> Router.get().navigateTo(Route.TRAINERS_LIST)),
                new BreadcrumbItem("#" + trainerId, () -> Router.get().navigateTo(Route.TRAINER_OVERVIEW, RouteContext.of("id", trainerId))),
                new BreadcrumbItem("Personal", () -> {}));
    }
}

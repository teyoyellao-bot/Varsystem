package varsystem.ui.shop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import varsystem.data.DB;
import static varsystem.data.Session.MYSHOP;
import varsystem.model.Feedback;
import varsystem.model.Req;
import static varsystem.service.RequestService.*;
import varsystem.ui.components.Chart;
import static varsystem.ui.components.Dialogs.exportCsv;
import varsystem.ui.components.RoundedPanel;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.*;
import static varsystem.util.Formats.DT;

public final class ShopReportsPage {

    private ShopReportsPage() { }

    private static final String[] MONTH_LABELS = {
        "Jan", "Feb", "Mar", "Apr",
        "May", "Jun", "Jul", "Aug",
        "Sep", "Oct", "Nov", "Dec"
    };

    // =========================================
    // UPDATE SHOP RATING FROM CUSTOMER FEEDBACK
    // =========================================
    private static void updateCurrentShopRating() {

        if (MYSHOP == null) {
            return;
        }

        List<Feedback> ratings = new ArrayList<>();

        for (Feedback f : DB.feedbacks) {

            if (f != null
                    && f.shop != null
                    && MYSHOP.name != null
                    && f.shop.equalsIgnoreCase(MYSHOP.name)
                    && f.rating >= 1
                    && f.rating <= 5) {

                ratings.add(f);
            }
        }

        // UPDATE TOTAL REVIEWS
        MYSHOP.reviews = ratings.size();

        // NO RATINGS YET
        if (ratings.isEmpty()) {

            MYSHOP.rating = 0.0;

            return;
        }

        // COMPUTE AVERAGE CUSTOMER RATING
        double totalRating = 0;

        for (Feedback f : ratings) {
            totalRating += f.rating;
        }

        MYSHOP.rating = totalRating / ratings.size();
    }

    private static double[] monthlyMechanicJobs(List<Req> requests) {

        double[] counts = new double[12];

        int currentYear = LocalDate.now().getYear();

        for (Req r : requests) {

            if (r == null
                    || r.accepted == null
                    || r.mechanic == null
                    || r.mechanic.isBlank()) {
                continue;
            }

            if (r.accepted.getYear() == currentYear) {
                counts[r.accepted.getMonthValue() - 1]++;
            }
        }

        return counts;
    }

    public static JComponent build() {

        // UPDATE RATING BEFORE DISPLAYING REPORTS
        updateCurrentShopRating();

        List<Req> rs = shopReqs();

        JButton ex = btn("Export Report \u2193", BLUE, e -> {

            List<String[]> rows = new ArrayList<>();

            for (Req r : rs) {

                if (r == null) {
                    continue;
                }

                rows.add(new String[]{
                    r.id,
                    r.uname,
                    r.vehicle,
                    r.problem,
                    r.status,
                    r.submitted == null ? "" : r.submitted.format(DT),
                    "" + (r.fee + r.travel)
                });
            }

            exportCsv(
                "shop_report.csv",
                new String[]{
                    "ID",
                    "Customer",
                    "Vehicle",
                    "Service",
                    "Status",
                    "Submitted",
                    "Total Cost"
                },
                rows
            );
        });

        JComponent[] pg = page(
            "Reports & Analytics",
            "Review your shop's jobs, completion rate, service trends, and performance.",
            ex
        );

        JPanel body = (JPanel) pg[1];

        long clients = rs.stream()
            .filter(r -> r != null && r.uemail != null)
            .map(r -> r.uemail)
            .distinct()
            .count();

        long fresh = DB.users.stream()
            .filter(u -> u != null
                && "User".equalsIgnoreCase(u.role)
                && u.joined != null
                && u.joined.isAfter(
                    LocalDateTime.now().minusDays(30)
                )
                && rs.stream().anyMatch(r ->
                    r != null
                    && r.uemail != null
                    && u.email != null
                    && r.uemail.equalsIgnoreCase(u.email)
                ))
            .count();

        // =========================================
        // SHOP RATING DISPLAY
        // =========================================

        String ratingText;

        if (MYSHOP == null) {

            ratingText = "★ 0.0 (0 reviews)";

        } else {

            ratingText = String.format(
                    "★ %.1f (%d reviews)",
                    MYSHOP.rating,
                    MYSHOP.reviews
            );
        }

        body.add(
            stats(new String[][]{
                {"Total Client", "" + clients},
                {
                    "Ratings",
                    ratingText
                },
                {
                    "Number of Mechanics",
                    "" + (MYSHOP == null || MYSHOP.mechs == null
                        ? 0
                        : MYSHOP.mechs.size())
                },
                {"New Customers", "" + fresh}
            }),
            BorderLayout.NORTH
        );

        // TOP SERVICES
        JPanel top = vbox();

        int i = 1;

        for (Map.Entry<String, Long> e : byProblem(rs).entrySet()) {

            JPanel row = new JPanel(new BorderLayout());

            row.setOpaque(false);
            row.setBorder(new EmptyBorder(8, 0, 8, 0));

            row.add(
                lbl(i++ + "   " + e.getKey(), 13, true, txt()),
                BorderLayout.WEST
            );

            row.add(
                lbl("" + e.getValue(), 13, true, BLUE),
                BorderLayout.EAST
            );

            top.add(row);
        }

        RoundedPanel tc = section("Top Services", top, null);

        tc.setPreferredSize(new Dimension(340, 100));

        // MONTHLY JOBS GRAPH
        Chart monthlyChart = new Chart(
            MONTH_LABELS,
            monthlyMechanicJobs(rs),
            true,
            950
        );

        // HORIZONTAL SCROLLING
        JScrollPane monthlyScroll = new JScrollPane(
            monthlyChart,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        monthlyScroll.setBorder(null);
        monthlyScroll.setOpaque(false);

        monthlyScroll.getViewport().setOpaque(false);

        monthlyScroll.getHorizontalScrollBar()
            .setUnitIncrement(25);

        monthlyScroll.setPreferredSize(
            new Dimension(500, 220)
        );

        // AUTOMATIC GRAPH REFRESH EVERY 3 SECONDS
        Timer refresh = new Timer(3000, e -> {

            monthlyChart.setData(
                MONTH_LABELS,
                monthlyMechanicJobs(shopReqs())
            );

        });

        refresh.setInitialDelay(0);

        // RUN ONLY WHILE THE REPORTS PAGE IS VISIBLE
        monthlyChart.addHierarchyListener(e -> {

            if ((e.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) != 0) {

                if (monthlyChart.isShowing()) {
                    refresh.start();
                } else {
                    refresh.stop();
                }
            }
        });

        // REPORTS CONTENT
        JPanel c = new JPanel(new BorderLayout(14, 0));

        c.setOpaque(false);

        c.add(
            section("Monthly Jobs", monthlyScroll, null),
            BorderLayout.CENTER
        );

        c.add(tc, BorderLayout.EAST);

        body.add(c, BorderLayout.CENTER);

        return pg[0];
    }
}

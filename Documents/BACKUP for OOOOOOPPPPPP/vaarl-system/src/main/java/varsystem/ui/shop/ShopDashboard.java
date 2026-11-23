
package varsystem.ui.shop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import varsystem.data.DB;
import static varsystem.data.Session.MYSHOP;
import varsystem.model.Feedback;
import varsystem.model.Req;
import static varsystem.service.RequestService.*;
import varsystem.ui.components.Chart;
import varsystem.ui.components.MapPanel;
import varsystem.ui.components.RoundedPanel;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.go;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.*;

public final class ShopDashboard {

    private ShopDashboard() { }

    // JANUARY TO DECEMBER
    private static final String[] MONTH_LABELS = {
        "Jan", "Feb", "Mar", "Apr",
        "May", "Jun", "Jul", "Aug",
        "Sep", "Oct", "Nov", "Dec"
    };

    // =========================================
    // UPDATE SHOP RATING FROM CUSTOMER FEEDBACK
    // =========================================

    private static void updateShopRating() {

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

        // NO CUSTOMER RATINGS YET
        if (ratings.isEmpty()) {

            MYSHOP.rating = 0.0;
            MYSHOP.reviews = 0;

            return;
        }

        // COMPUTE AVERAGE RATING
        double totalRating = 0;

        for (Feedback f : ratings) {
            totalRating += f.rating;
        }

        MYSHOP.reviews = ratings.size();
        MYSHOP.rating = totalRating / ratings.size();
    }

    // =========================================
    // DATE AND TIME HEADER
    // =========================================

    private static JPanel dateHeader() {

        JLabel dt = lbl("", 20, true, txt());

        JPanel days = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 10, 0)
        );

        days.setOpaque(false);

        String[] dn = {
            "Mon", "Tue", "Wed", "Thu",
            "Fri", "Sat", "Sun"
        };

        int today = LocalDate.now().getDayOfWeek().getValue() - 1;

        for (int i = 0; i < 7; i++) {

            JButton b = btn(
                dn[i],
                i == today ? BLUE : new Color(0x1E, 0x4A, 0x8C),
                e -> { }
            );

            b.setForeground(Color.WHITE);

            if (i == today) {
                b.setBackground(BLUE);
            }

            days.add(b);
        }

        JPanel datePanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );

        datePanel.setOpaque(false);
        datePanel.add(dt);

        JPanel header = new JPanel(
            new BorderLayout()
        );

        header.setOpaque(false);

        header.add(days, BorderLayout.WEST);
        header.add(datePanel, BorderLayout.EAST);

        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(
                "MMMM d, yyyy   h:mm:ss a",
                Locale.ENGLISH
            );

        dt.setText(
            LocalDateTime.now().format(formatter)
        );

        Timer tm = new Timer(1000, e -> {

            dt.setText(
                LocalDateTime.now().format(formatter)
            );

        });

        // REFRESH ONLY WHILE HEADER IS VISIBLE
        header.addHierarchyListener(e -> {

            if ((e.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) != 0) {

                if (header.isShowing()) {
                    tm.start();
                } else {
                    tm.stop();
                }
            }
        });

        return header;
    }

    // =========================================
    // MONTHLY JOB DATA BASED ON ASSIGNED MECHANICS
    // =========================================

    private static double[] monthlyMechanicJobs() {

        double[] counts = new double[12];

        int currentYear = LocalDate.now().getYear();

        for (Req r : shopReqs()) {

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

    // =========================================
    // BUILD DASHBOARD
    // =========================================

    public static JComponent build() {

        // UPDATE RATING BEFORE DISPLAYING DASHBOARD
        updateShopRating();

        JComponent[] pg = page(
            "Shop Dashboard",
            "Welcome back! Here's an overview of your repair shop."
        );

        JPanel body = (JPanel) pg[1];

        List<Req> rs = shopReqs();

        long pend = cnt(rs, "Pending");
        long acc = cnt(rs, "Accepted");
        long on = cnt(rs, "On Going");
        long done = cnt(rs, "Completed");

        // =========================================
        // DATE HEADER AND DASHBOARD STATISTICS
        // =========================================

        JPanel dashboardTop = new JPanel();

        dashboardTop.setOpaque(false);

        dashboardTop.setLayout(
            new BoxLayout(dashboardTop, BoxLayout.Y_AXIS)
        );

        JPanel date = dateHeader();

        date.setBorder(
            new EmptyBorder(0, 0, 15, 0)
        );

        dashboardTop.add(date);

        String shopRating = MYSHOP == null
                ? "0.0 ★"
                : String.format(
                    Locale.ENGLISH,
                    "%.1f ★",
                    MYSHOP.rating
                );

        String reviewCount = MYSHOP == null
                ? "0 customer reviews"
                : MYSHOP.reviews + " customer reviews";

        dashboardTop.add(stats(new String[][]{
            {"New Requests", "" + pend, "waiting for action"},
            {"Today's Jobs", "" + (acc + on), on + " in service"},
            {"Completed", "" + done, "all time"},
            {"Shop Rating", shopRating, reviewCount}
        }));

        body.add(dashboardTop, BorderLayout.NORTH);

        // =========================================
        // RECENT SERVICE REQUESTS
        // =========================================

        List<Object[]> rows = new ArrayList<>();

        rs.stream()
            .filter(r -> r != null)
            .sorted((a, b) -> {

                if (a.submitted == null) return 1;
                if (b.submitted == null) return -1;

                return b.submitted.compareTo(a.submitted);
            })
            .limit(5)
            .forEach(r -> rows.add(new Object[]{
                r.id,
                r.uname,
                r.vehicle,
                r.problem,
                r.status
            }));

        RoundedPanel recent = section(
            "Recent Service Requests",
            grid(
                new String[]{
                    "ID", "Customer", "Vehicle", "Service", "Status"
                },
                new double[]{1, 2, 2, 2, 1},
                rows
            ),
            link("View All", e -> go("Service Requests"))
        );

        // =========================================
        // REQUEST STATUS OVERVIEW
        // =========================================

        long tot = Math.max(1, rs.size());

        JPanel ov = vbox();

        String[] nm = {
            "Pending", "Accepted", "On Going", "Completed"
        };

        long[] ct = {pend, acc, on, done};

        Color[] cl = {
            ORANGE, BLUE, PURPLE, GREEN
        };

        for (int i = 0; i < 4; i++) {

            JPanel row = new JPanel(
                new BorderLayout()
            );

            row.setOpaque(false);

            row.setBorder(
                new EmptyBorder(8, 0, 8, 0)
            );

            row.add(
                flow(
                    badge(" ", cl[i]),
                    lbl(nm[i], 13, false, txt())
                ),
                BorderLayout.WEST
            );

            row.add(
                lbl(
                    ct[i] + " (" + (ct[i] * 100 / tot) + "%)",
                    13,
                    true,
                    txt()
                ),
                BorderLayout.EAST
            );

            ov.add(row);
        }

        RoundedPanel status = section(
            "Request Status Overview",
            ov,
            null
        );

        status.setPreferredSize(
            new Dimension(330, 100)
        );

        JPanel upper = new JPanel(
            new BorderLayout(14, 0)
        );

        upper.setOpaque(false);

        upper.add(recent, BorderLayout.CENTER);
        upper.add(status, BorderLayout.EAST);

        // =========================================
        // MONTHLY JOBS - JANUARY TO DECEMBER
        // =========================================

        Chart monthlyChart = new Chart(
            MONTH_LABELS,
            monthlyMechanicJobs(),
            false,
            950
        );

        JScrollPane monthlyScroll = new JScrollPane(
            monthlyChart,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        monthlyScroll.setBorder(null);
        monthlyScroll.setOpaque(false);
        monthlyScroll.getViewport().setOpaque(false);

        monthlyScroll.getHorizontalScrollBar()
            .setUnitIncrement(25);

        monthlyScroll.getVerticalScrollBar()
            .setUnitIncrement(20);

        monthlyScroll.setPreferredSize(
            new Dimension(500, 220)
        );

        RoundedPanel chart = section(
            "Monthly Jobs",
            monthlyScroll,
            null
        );

        // AUTOMATIC REFRESH EVERY 3 SECONDS
        Timer monthlyRefresh = new Timer(3000, e -> {

            monthlyChart.setData(
                MONTH_LABELS,
                monthlyMechanicJobs()
            );

        });

        monthlyRefresh.setInitialDelay(0);

        // REFRESH ONLY WHILE CHART IS VISIBLE
        monthlyChart.addHierarchyListener(e -> {

            if ((e.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) != 0) {

                if (monthlyChart.isShowing()) {
                    monthlyRefresh.start();
                } else {
                    monthlyRefresh.stop();
                }
            }
        });

        // =========================================
        // MOST REQUESTED SERVICES
        // =========================================

        JPanel top = vbox();

        int i = 1;

        for (Map.Entry<String, Long> e : byProblem(rs).entrySet()) {

            if (i > 5) {
                break;
            }

            JPanel row = new JPanel(
                new BorderLayout()
            );

            row.setOpaque(false);

            row.setBorder(
                new EmptyBorder(6, 0, 6, 0)
            );

            row.add(
                lbl(
                    i + "   " + e.getKey(),
                    13,
                    true,
                    txt()
                ),
                BorderLayout.WEST
            );

            row.add(
                lbl(
                    e.getValue() + " ("
                        + (e.getValue() * 100 / tot) + "%)",
                    13,
                    true,
                    BLUE
                ),
                BorderLayout.EAST
            );

            top.add(row);

            i++;
        }

        if (i == 1) {
            top.add(
                lbl("No data yet.", 13, false, sub())
            );
        }

        RoundedPanel most = section(
            "Most Requested Services",
            top,
            null
        );

        // =========================================
        // CHART AND MOST REQUESTED SERVICES
        // =========================================

        JPanel lower = new JPanel(
            new GridLayout(1, 2, 14, 0)
        );

        lower.setOpaque(false);

        lower.add(chart);
        lower.add(most);

        lower.setPreferredSize(
            new Dimension(100, 250)
        );

        // =========================================
        // EXISTING DASHBOARD CONTENT
        // =========================================

        JPanel mid = new JPanel(
            new BorderLayout(0, 14)
        );

        mid.setOpaque(false);

        mid.add(upper, BorderLayout.CENTER);
        mid.add(lower, BorderLayout.SOUTH);

        // =========================================
        // SHOP LOCATION AND CUSTOMER REQUEST MAP
        // =========================================

        MapPanel map = new MapPanel();

        if (MYSHOP != null) {

            map.setShops(
                java.util.Collections.singletonList(MYSHOP)
            );

            map.setCustomerRequests(
                rs,
                MYSHOP.name
            );

        } else {

            map.setShops(
                java.util.Collections.emptyList()
            );

            map.setCustomerRequests(
                java.util.Collections.emptyList(),
                null
            );
        }

        RoundedPanel mapSection = section(
            "Shop Location & Shared Customer Locations",
            map,
            null
        );

        mapSection.setPreferredSize(
            new Dimension(100, 350)
        );

        // =========================================
        // SCROLLABLE DASHBOARD
        // =========================================

        JPanel content = new JPanel();

        content.setOpaque(false);

        content.setLayout(
            new BoxLayout(content, BoxLayout.Y_AXIS)
        );

        mid.setAlignmentX(
            java.awt.Component.LEFT_ALIGNMENT
        );

        mapSection.setAlignmentX(
            java.awt.Component.LEFT_ALIGNMENT
        );

        mid.setPreferredSize(
            new Dimension(900, 520)
        );

        mid.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 520)
        );

        mapSection.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 350)
        );

        content.add(mid);
        content.add(Box.createVerticalStrut(14));
        content.add(mapSection);

        // VERTICAL SCROLLING FOR ENTIRE DASHBOARD
        JScrollPane scroll = new JScrollPane(
            content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        scroll.getVerticalScrollBar()
            .setUnitIncrement(20);

        body.add(scroll, BorderLayout.CENTER);

        return pg[0];
    }
}


package varsystem.ui.user;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Req;
import varsystem.ui.auth.LoginBackground;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.link;
import static varsystem.ui.components.Ui.statusColor;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.go;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.LIGHT;
import static varsystem.ui.theme.Theme.NAVY;
import static varsystem.ui.theme.Theme.pageBg;
import static varsystem.ui.theme.Theme.sub;
import static varsystem.ui.theme.Theme.txt;

public final class UserDashboard {

    private UserDashboard() {
    }

    // DARK BLUE GRADIENT COLORS
    private static final Color GRADIENT_DARK =
            new Color(0x00, 0x16, 0x38);

    private static final Color GRADIENT_MID =
            new Color(0x08, 0x2D, 0x60);

    private static final Color GRADIENT_LIGHT =
            new Color(0x10, 0x4B, 0x85);

    private static final Color GRADIENT_BORDER =
            new Color(0x3A, 0x79, 0xB8);

    private static final Color WHITE_BLUE =
            new Color(0xD9, 0xEA, 0xFF);

    public static JComponent build() {

        // BACKGROUND
        JPanel p = new LoginBackground();
        p.setLayout(new BorderLayout(0, 15));
        p.setBorder(new EmptyBorder(24, 34, 24, 34));

        // DATE AND TIME
        JLabel dt = lbl("", 17, true, Color.WHITE);

        String[] dn = {
            "Mon", "Tue", "Wed", "Thu",
            "Fri", "Sat", "Sun"
        };

        int today = LocalDate.now().getDayOfWeek().getValue() - 1;

        JPanel days = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        days.setOpaque(false);

        for (int i = 0; i < dn.length; i++) {

            JButton day = btn(
                    dn[i],
                    i == today ? LIGHT : NAVY,
                    e -> {
                    }
            );

            day.setForeground(
                    i == today ? NAVY : Color.WHITE
            );

            days.add(day);
        }

        // REAL-TIME CLOCK
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            dt.setText(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern(
                            "MMMM d, yyyy   h:mm:ss a",
                            Locale.ENGLISH
                    )
            ));
        });

        dt.setText(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(
                        "MMMM d, yyyy   h:mm:ss a",
                        Locale.ENGLISH
                )
        ));

        timer.start();

        // NOTIFICATION BUTTON
        JButton notification = btn(
                "Notifications",
                NAVY,
                e -> showNotifications()
        );

        JPanel topRight = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );
        topRight.setOpaque(false);
        topRight.add(notification);
        topRight.add(dt);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(days, BorderLayout.WEST);
        top.add(topRight, BorderLayout.EAST);

        p.add(top, BorderLayout.NORTH);

        // MAIN CONTENT
        JPanel content = vbox();
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 0, 20, 0));

        // WELCOME MESSAGE
        String name = "User";

        if (Session.ME != null
                && Session.ME.name != null
                && !Session.ME.name.trim().isEmpty()) {

            name = Session.ME.name;
        }

        JLabel welcome = lbl(
                "WELCOME BACK!",
                35,
                true,
                Color.WHITE
        );
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(welcome);

        JLabel hello = lbl(
                "Hello, " + name + "!",
                25,
                true,
                new Color(0x65, 0xA8, 0xFF)
        );
        hello.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(hello);

        JLabel subtitle = lbl(
                "Manage your vehicle assistance and repair requests here.",
                14,
                false,
                Color.WHITE
        );
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(subtitle);

        content.add(Box.createVerticalStrut(22));

        // USER REQUEST STATISTICS
        List<Req> requests = getUserRequests();

        int total = requests.size();
        int completed = 0;
        int active = 0;

        for (Req r : requests) {

            if ("Completed".equalsIgnoreCase(r.status)) {
                completed++;
            }

            if (r.isActive()) {
                active++;
            }
        }

        JComponent statistics = createStatistics(
                total,
                completed,
                active
        );

        statistics.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statistics);

        content.add(Box.createVerticalStrut(12));

        // QUICK ACTIONS
        JPanel actionContent = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 5)
        );
        actionContent.setOpaque(false);

        JButton findShop = btn(
                "Find a Repair Shop",
                BLUE,
                e -> go("Find Shop")
        );

        JButton myRequests = btn(
                "My Requests",
                NAVY,
                e -> go("My Requests")
        );

        actionContent.add(findShop);
        actionContent.add(myRequests);

        JPanel quickActions = createGradientSection(
                "Quick Actions",
                actionContent,
                null
        );

        quickActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(quickActions);

        content.add(Box.createVerticalStrut(15));

        // RECENT ACTIVITY
        JPanel recentContent = new JPanel();
        recentContent.setOpaque(false);
        recentContent.setLayout(
                new BoxLayout(recentContent, BoxLayout.Y_AXIS)
        );

        List<Req> recent = getUserRequests();

        recent.sort(
                Comparator.comparing(
                        (Req r) -> r.submitted,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        if (recent.isEmpty()) {

            JLabel empty = lbl(
                    "No recent activity yet.",
                    13,
                    false,
                    WHITE_BLUE
            );

            recentContent.add(empty);

        } else {

            int limit = Math.min(5, recent.size());

            for (int i = 0; i < limit; i++) {

                Req r = recent.get(i);

                JPanel item = new JPanel(
                        new BorderLayout(10, 5)
                );
                item.setOpaque(false);
                item.setBorder(
                        new EmptyBorder(9, 5, 9, 5)
                );

                JPanel details = new JPanel();
                details.setOpaque(false);
                details.setLayout(
                        new BoxLayout(details, BoxLayout.Y_AXIS)
                );

                String vehicle = r.vehicle == null
                        ? "Vehicle Assistance Request"
                        : r.vehicle;

                String problem = r.problem == null
                        ? "Service Request"
                        : r.problem;

                details.add(lbl(
                        vehicle,
                        13,
                        true,
                        Color.WHITE
                ));

                details.add(lbl(
                        problem,
                        11,
                        false,
                        WHITE_BLUE
                ));

                String status = r.status == null
                        ? "Unknown"
                        : r.status;

                Color color = statusColor(status);

                item.add(details, BorderLayout.CENTER);

                item.add(
                        lbl(
                                status,
                                12,
                                true,
                                color == null ? WHITE_BLUE : color
                        ),
                        BorderLayout.EAST
                );

                recentContent.add(item);

                if (i < limit - 1) {
                    JSeparator separator = new JSeparator();
                    separator.setForeground(
                            new Color(100, 160, 215, 120)
                    );
                    separator.setBackground(
                            new Color(100, 160, 215, 120)
                    );
                    recentContent.add(separator);
                }
            }
        }

        JPanel recentSection = createGradientSection(
                "Recent Activity",
                recentContent,
                link("View All", e -> go("My Requests"))
        );

        recentSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(recentSection);

        // SCROLLABLE CONTENT
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        scroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );

        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // STATISTICS CARDS
    private static JComponent createStatistics(
            int total,
            int completed,
            int active) {

        JPanel container = new JPanel(
                new GridLayout(1, 3, 16, 0)
        );
        container.setOpaque(false);

        container.add(createStatCard(
                "TOTAL REQUESTS",
                String.valueOf(total),
                "All submitted requests"
        ));

        container.add(createStatCard(
                "COMPLETED SERVICES",
                String.valueOf(completed),
                "Successfully completed"
        ));

        container.add(createStatCard(
                "ACTIVE REQUESTS",
                String.valueOf(active),
                "Pending or ongoing"
        ));

        return container;
    }

    // INDIVIDUAL STATISTICS CARD
    private static JComponent createStatCard(
            String title,
            String value,
            String description) {

        GradientPanel card = new GradientPanel(
                new BorderLayout(),
                GRADIENT_DARK,
                GRADIENT_LIGHT,
                18
        );

        card.setBorder(
                new EmptyBorder(18, 22, 18, 22)
        );

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(
                new BoxLayout(texts, BoxLayout.Y_AXIS)
        );

        JLabel titleLabel = lbl(
                title,
                14,
                true,
                WHITE_BLUE
        );

        JLabel valueLabel = lbl(
                value,
                30,
                true,
                Color.WHITE
        );

        JLabel descriptionLabel = lbl(
                description,
                13,
                false,
                WHITE_BLUE
        );

        texts.add(titleLabel);
        texts.add(Box.createVerticalStrut(5));
        texts.add(valueLabel);
        texts.add(Box.createVerticalStrut(2));
        texts.add(descriptionLabel);

        card.add(texts, BorderLayout.CENTER);

        return card;
    }

    // GRADIENT SECTION PANEL
    private static JPanel createGradientSection(
            String title,
            JComponent body,
            JComponent trailing) {

        GradientPanel panel = new GradientPanel(
                new BorderLayout(0, 12),
                GRADIENT_DARK,
                GRADIENT_MID,
                18
        );

        panel.setBorder(
                new EmptyBorder(20, 24, 20, 24)
        );

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);

        JLabel titleLabel = lbl(
                title,
                21,
                true,
                Color.WHITE
        );

        heading.add(titleLabel, BorderLayout.WEST);

        if (trailing != null) {
            heading.add(trailing, BorderLayout.EAST);
        }

        body.setOpaque(false);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);

        return panel;
    }

    // CUSTOM DARK BLUE GRADIENT PANEL
    private static class GradientPanel extends JPanel {

        private final Color startColor;
        private final Color endColor;
        private final int arc;

        GradientPanel(
                LayoutManager layout,
                Color startColor,
                Color endColor,
                int arc) {

            super(layout);

            this.startColor = startColor;
            this.endColor = endColor;
            this.arc = arc;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            GradientPaint gradient = new GradientPaint(
                    0,
                    0,
                    startColor,
                    getWidth(),
                    getHeight(),
                    endColor
            );

            g2.setPaint(gradient);

            RoundRectangle2D rounded =
                    new RoundRectangle2D.Float(
                            0,
                            0,
                            getWidth() - 1,
                            getHeight() - 1,
                            arc,
                            arc
                    );

            g2.fill(rounded);

            g2.setColor(GRADIENT_BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(rounded);

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // GET ONLY THE LOGGED-IN USER'S REQUESTS
    private static List<Req> getUserRequests() {

        List<Req> result = new ArrayList<>();

        if (Session.ME == null || Session.ME.email == null) {
            return result;
        }

        String email = Session.ME.email;

        for (Req r : DB.reqs) {

            if (r != null
                    && r.uemail != null
                    && r.uemail.equalsIgnoreCase(email)) {

                result.add(r);
            }
        }

        return result;
    }

    // NOTIFICATIONS WINDOW
    private static void showNotifications() {

        JPanel panel = new JPanel();
        panel.setLayout(
                new BoxLayout(panel, BoxLayout.Y_AXIS)
        );

        panel.setBorder(
                new EmptyBorder(12, 15, 12, 15)
        );

        panel.setBackground(pageBg());

        panel.add(lbl(
                "Your Request Updates",
                16,
                true,
                txt()
        ));

        panel.add(Box.createVerticalStrut(12));

        List<Req> requests = getUserRequests();

        requests.sort(
                Comparator.comparing(
                        (Req r) -> r.submitted,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        if (requests.isEmpty()) {

            panel.add(lbl(
                    "You don't have any notifications yet.",
                    13,
                    false,
                    sub()
            ));

        } else {

            for (Req r : requests) {

                JPanel item = new JPanel();
                item.setOpaque(false);
                item.setLayout(
                        new BoxLayout(item, BoxLayout.Y_AXIS)
                );

                item.setBorder(
                        new EmptyBorder(8, 0, 8, 0)
                );

                String vehicle = r.vehicle == null
                        ? "Vehicle Request"
                        : r.vehicle;

                String status = r.status == null
                        ? "Unknown"
                        : r.status;

                Color color = statusColor(status);

                item.add(lbl(
                        vehicle,
                        13,
                        true,
                        txt()
                ));

                item.add(lbl(
                        "Current status: " + status,
                        12,
                        true,
                        color == null ? txt() : color
                ));

                if (r.submitted != null) {

                    item.add(lbl(
                            "Submitted: " + r.submitted.format(
                                    DateTimeFormatter.ofPattern(
                                            "MMM d, yyyy - h:mm a",
                                            Locale.ENGLISH
                                    )
                            ),
                            11,
                            false,
                            sub()
                    ));
                }

                panel.add(item);
                panel.add(new JSeparator());
            }
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(
                new Dimension(430, 350)
        );
        scroll.setBorder(null);

        JOptionPane.showMessageDialog(
                null,
                scroll,
                "Notifications",
                JOptionPane.PLAIN_MESSAGE
        );
    }
}

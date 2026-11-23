
package varsystem.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import varsystem.data.DB;
import varsystem.model.Req;
import static varsystem.service.RequestService.byProblem;
import static varsystem.service.RequestService.cnt;
import varsystem.ui.common.NotificationsDialog;
import varsystem.ui.common.RequestDetailDialog;
import varsystem.ui.components.Chart;
import static varsystem.ui.components.Dialogs.infoArea;
import varsystem.ui.components.Pie;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.grid;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.link;
import static varsystem.ui.components.Ui.section;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.go;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.GREEN;
import static varsystem.ui.theme.Theme.ORANGE;
import static varsystem.ui.theme.Theme.PURPLE;
import static varsystem.ui.theme.Theme.sub;
import static varsystem.ui.theme.Theme.txt;

public final class AdminDashboard {

    private AdminDashboard() { }

    private static final Color MUTED =
            new Color(125, 135, 150);

    private static final String[] MONTH_LABELS = {
        "Jan", "Feb", "Mar", "Apr",
        "May", "Jun", "Jul", "Aug",
        "Sep", "Oct", "Nov", "Dec"
    };

    // =========================================
    // DATE AND TIME HEADER
    // =========================================

    private static JPanel dateHeader() {

        // FONT SIZE REDUCED FROM 20 TO 15
        JLabel dt = lbl("", 15, true, txt());

        JPanel days = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 10, 0)
        );

        days.setOpaque(false);

        String[] dn = {
            "Mon", "Tue", "Wed", "Thu",
            "Fri", "Sat", "Sun"
        };

        int today =
            LocalDate.now().getDayOfWeek().getValue() - 1;

        for (int i = 0; i < 7; i++) {

            JButton b = btn(
                dn[i],
                i == today
                    ? BLUE
                    : new Color(0x1E, 0x4A, 0x8C),
                e -> { }
            );

            b.setForeground(Color.WHITE);
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
    // GET ALL SERVICE REQUESTS
    // =========================================

    private static List<Req> getAllRequests() {

        List<Req> requests = new ArrayList<>();

        for (Req r : DB.reqs) {

            if (r != null) {
                requests.add(r);
            }
        }

        return requests;
    }

    // =========================================
    // GET REQUESTS FOR CURRENT YEAR
    // =========================================

    private static List<Req> getYearlyRequests() {

        List<Req> requests = new ArrayList<>();

        int currentYear = LocalDate.now().getYear();

        for (Req r : DB.reqs) {

            if (r != null
                    && r.submitted != null
                    && r.submitted.getYear() == currentYear) {

                requests.add(r);
            }
        }

        return requests;
    }

    // =========================================
    // MONTHLY REQUEST DATA
    // =========================================

    private static double[] monthlyRequestData(
            List<Req> requests) {

        double[] counts = new double[12];

        int currentYear = LocalDate.now().getYear();

        for (Req r : requests) {

            if (r == null || r.submitted == null) {
                continue;
            }

            if (r.submitted.getYear() == currentYear) {
                counts[r.submitted.getMonthValue() - 1]++;
            }
        }

        return counts;
    }

    // =========================================
    // TOTAL REQUESTS
    // =========================================

    private static long totalRequests() {
        return getAllRequests().size();
    }

    // =========================================
    // DARK SCROLLBAR STYLE
    // =========================================

    private static JComponent darkScrollbars(
            JComponent component) {

        if (!(component instanceof JScrollPane)) {
            return component;
        }

        JScrollPane scroll = (JScrollPane) component;

        Color track = new Color(0x0B, 0x28, 0x53);
        Color thumb = new Color(0x24, 0x5A, 0xA0);
        Color hover = new Color(0x32, 0x78, 0xD8);

        scroll.setBorder(
            BorderFactory.createEmptyBorder()
        );

        scroll.setOpaque(false);
        scroll.setBackground(track);

        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(track);

        JScrollBar[] bars = {
            scroll.getVerticalScrollBar(),
            scroll.getHorizontalScrollBar()
        };

        for (JScrollBar bar : bars) {

            bar.setOpaque(true);
            bar.setBackground(track);

            bar.setBorder(
                BorderFactory.createEmptyBorder()
            );

            bar.setUnitIncrement(16);

            bar.setUI(new BasicScrollBarUI() {

                @Override
                protected void configureScrollBarColors() {
                    thumbColor = thumb;
                    trackColor = track;
                }

                @Override
                protected void paintTrack(
                        Graphics graphics,
                        JComponent c,
                        Rectangle bounds) {

                    Graphics2D g2 =
                        (Graphics2D) graphics.create();

                    g2.setColor(track);

                    g2.fillRect(
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height
                    );

                    g2.dispose();
                }

                @Override
                protected void paintThumb(
                        Graphics graphics,
                        JComponent c,
                        Rectangle bounds) {

                    if (bounds.isEmpty()
                            || !scrollbar.isEnabled()) {
                        return;
                    }

                    Graphics2D g2 =
                        (Graphics2D) graphics.create();

                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    g2.setColor(
                        isDragging || isThumbRollover()
                            ? hover
                            : thumb
                    );

                    g2.fillRoundRect(
                        bounds.x + 2,
                        bounds.y + 2,
                        Math.max(1, bounds.width - 4),
                        Math.max(1, bounds.height - 4),
                        10,
                        10
                    );

                    g2.dispose();
                }

                @Override
                protected JButton createDecreaseButton(
                        int orientation) {
                    return zeroButton();
                }

                @Override
                protected JButton createIncreaseButton(
                        int orientation) {
                    return zeroButton();
                }

                private JButton zeroButton() {

                    JButton button = new JButton();

                    button.setPreferredSize(
                        new Dimension(0, 0)
                    );

                    button.setMinimumSize(
                        new Dimension(0, 0)
                    );

                    button.setMaximumSize(
                        new Dimension(0, 0)
                    );

                    button.setOpaque(false);
                    button.setFocusable(false);
                    button.setBorderPainted(false);
                    button.setContentAreaFilled(false);

                    return button;
                }
            });

            bar.setPreferredSize(
                bar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(10, 0)
                    : new Dimension(0, 10)
            );
        }

        JPanel corner = new JPanel();

        corner.setOpaque(true);
        corner.setBackground(track);

        scroll.setCorner(
            JScrollPane.LOWER_RIGHT_CORNER,
            corner
        );

        return scroll;
    }

    // =========================================
    // BUILD ADMIN DASHBOARD
    // =========================================

    public static JComponent build() {

        JComponent[] pg = page(
            "Admin Dashboard",
            "Welcome back, Admin! Here's an overview of your system."
        );

        JPanel body = (JPanel) pg[1];

        body.setBorder(
            new EmptyBorder(10, 10, 10, 10)
        );

        // =========================================
        // TOP SECTION - DATE HEADER
        // =========================================

        JPanel topSection = new JPanel();

        topSection.setOpaque(false);

        topSection.setLayout(
            new BoxLayout(topSection, BoxLayout.Y_AXIS)
        );

        JPanel date = dateHeader();

        date.setBorder(
            new EmptyBorder(0, 0, 15, 0)
        );

        topSection.add(date);

        body.add(topSection, BorderLayout.NORTH);

        // =========================================
        // ALL SERVICE REQUESTS
        // =========================================

        List<Req> all = getAllRequests();

        JPanel g = new JPanel(
            new GridLayout(0, 2, 14, 14)
        );

        g.setOpaque(false);

        // =========================================
        // RECENT SERVICE REQUESTS
        // =========================================

        List<Object[]> rows = new ArrayList<>();

        all.stream()
            .filter(r -> r != null)
            .sorted((a, b) -> {

                if (a.submitted == null) return 1;
                if (b.submitted == null) return -1;

                return b.submitted.compareTo(a.submitted);
            })
            .limit(5)
            .forEach(r ->

                rows.add(new Object[]{
                    r.id,
                    r.uname,
                    r.problem,
                    r.status,

                    btn(
                        "View",
                        BLUE,
                        e -> RequestDetailDialog.show(r, true)
                    )
                })
            );

        g.add(section(
            "Recent Service Requests",

            darkScrollbars(
                grid(
                    new String[]{
                        "ID", "User", "Problem", "Status", ""
                    },

                    new double[]{
                        1.2, 2, 2, 1, .6
                    },

                    rows
                )
            ),

            link(
                "View All",
                e -> go("Service Request")
            )
        ));

        // =========================================
        // REQUEST STATUS OVERVIEW
        // =========================================

        String[] nm = {
            "Pending",
            "Accepted",
            "On Going",
            "Completed"
        };

        Color[] cl = {
            ORANGE,
            BLUE,
            PURPLE,
            GREEN
        };

        double[] v = new double[4];

        JPanel leg = vbox();

        leg.setBorder(
            new EmptyBorder(5, 15, 5, 5)
        );

        for (int i = 0; i < 4; i++) {

            v[i] = cnt(all, nm[i]);

            JPanel legendRow = new JPanel(
                new BorderLayout(8, 0)
            );

            legendRow.setOpaque(false);

            legendRow.setBorder(
                new EmptyBorder(2, 0, 2, 10)
            );

            JPanel statusGroup = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 9, 0)
            );

            statusGroup.setOpaque(false);

            JPanel colorBox = new JPanel();

            colorBox.setBackground(cl[i]);

            colorBox.setPreferredSize(
                new Dimension(11, 11)
            );

            colorBox.setMinimumSize(
                new Dimension(11, 11)
            );

            colorBox.setMaximumSize(
                new Dimension(11, 11)
            );

            statusGroup.add(colorBox);

            statusGroup.add(
                lbl(nm[i], 13, true, txt())
            );

            legendRow.add(
                statusGroup,
                BorderLayout.WEST
            );

            legendRow.add(
                lbl(
                    String.valueOf((int) v[i]),
                    13,
                    true,
                    txt()
                ),
                BorderLayout.EAST
            );

            leg.add(legendRow);
        }

        // =========================================
        // PIE CHART
        // =========================================

        JPanel pc = new JPanel(
            new BorderLayout()
        );

        pc.setOpaque(false);

        pc.setBorder(
            new EmptyBorder(5, 5, 5, 5)
        );

        pc.add(
            new Pie(
                v,
                cl,
                String.valueOf(all.size()),
                "Total Requests"
            ),
            BorderLayout.CENTER
        );

        // =========================================
        // TOTAL REQUESTS BESIDE PIE CHART
        // =========================================

        JPanel totalPanel = new JPanel();

        totalPanel.setLayout(
            new BoxLayout(
                totalPanel,
                BoxLayout.Y_AXIS
            )
        );

        totalPanel.setOpaque(false);

        totalPanel.setBorder(
            new EmptyBorder(15, 10, 10, 10)
        );

        totalPanel.add(
            lbl(
                "TOTAL REQUESTS",
                12,
                true,
                sub()
            )
        );

        totalPanel.add(
            Box.createVerticalStrut(8)
        );

        totalPanel.add(
            lbl(
                String.valueOf(totalRequests()),
                30,
                true,
                BLUE
            )
        );

        totalPanel.add(
            Box.createVerticalStrut(5)
        );

        totalPanel.add(
            lbl(
                "All service requests",
                11,
                false,
                sub()
            )
        );

        totalPanel.add(
            lbl(
                "from all shops",
                11,
                false,
                sub()
            )
        );

        // =========================================
        // STATUS + PIE + TOTAL REQUESTS
        // =========================================

        JPanel overview = new JPanel(
            new GridLayout(1, 3, 8, 0)
        );

        overview.setOpaque(false);

        overview.add(leg);
        overview.add(pc);
        overview.add(totalPanel);

        g.add(section(
            "Request Status Overview",
            overview,
            null
        ));

        // =========================================
        // TOP REPAIR SHOPS
        // =========================================

        List<Object[]> sr = new ArrayList<>();

        DB.shops.stream()
            .filter(s -> s != null)
            .sorted((a, b) ->
                Double.compare(b.rating, a.rating)
            )
            .limit(5)
            .forEach(s ->

                sr.add(new Object[]{
                    s.name,
                    "\u2605 " + String.format(
                        Locale.ENGLISH,
                        "%.1f",
                        s.rating
                    ),
                    s.status
                })
            );

        g.add(section(
            "Top Repair Shops",

            darkScrollbars(
                grid(
                    new String[]{
                        "Shop", "Rating", "Status"
                    },

                    new double[]{
                        3, 1, 1
                    },

                    sr
                )
            ),

            link(
                "View All",
                e -> go("Manage Repair Shop")
            )
        ));

        // =========================================
        // MONTHLY REQUESTS
        // =========================================

        List<Req> yearlyRequests =
            getYearlyRequests();

        double[] monthlyData =
            monthlyRequestData(yearlyRequests);

        g.add(section(
            "Monthly Requests",

            new Chart(
                MONTH_LABELS,
                monthlyData,
                false
            ),

            null
        ));

        // =========================================
        // MOST REQUESTED SERVICES
        // =========================================

        JPanel mr = vbox();

        Map<String, Long> mostRequested =
            byProblem(all);

        int i = 1;

        for (Map.Entry<String, Long> e :
                mostRequested.entrySet()) {

            if (i > 5) {
                break;
            }

            JPanel row = new JPanel(
                new BorderLayout(10, 0)
            );

            row.setOpaque(false);

            row.setBorder(
                new EmptyBorder(9, 0, 9, 0)
            );

            JPanel left = new JPanel(
                new BorderLayout(8, 0)
            );

            left.setOpaque(false);

            left.add(
                lbl("#" + i, 12, true, BLUE),
                BorderLayout.WEST
            );

            left.add(
                lbl(e.getKey(), 12, true, txt()),
                BorderLayout.CENTER
            );

            row.add(left, BorderLayout.CENTER);

            row.add(
                lbl(
                    e.getValue() + " requests",
                    11,
                    true,
                    BLUE
                ),
                BorderLayout.EAST
            );

            mr.add(row);

            i++;
        }

        if (mostRequested.isEmpty()) {

            mr.add(
                lbl(
                    "No service requests available yet.",
                    12,
                    false,
                    MUTED
                )
            );
        }

        g.add(section(
            "Most Requested Services",
            mr,
            null
        ));

        // =========================================
        // RECENT NOTIFICATIONS
        // =========================================

        JPanel nl = vbox();

        DB.notifs.stream()
            .limit(4)
            .forEach(n -> {

                JPanel notificationRow = new JPanel(
                    new BorderLayout(8, 0)
                );

                notificationRow.setOpaque(false);

                notificationRow.setBorder(
                    new EmptyBorder(7, 0, 7, 0)
                );

                notificationRow.add(
                    lbl("\u2022", 18, true, BLUE),
                    BorderLayout.WEST
                );

                JTextArea a = infoArea(n, 2, 22);

                a.setForeground(txt());

                a.setFont(
                    new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                    )
                );

                a.setLineWrap(true);
                a.setWrapStyleWord(true);

                notificationRow.add(
                    a,
                    BorderLayout.CENTER
                );

                nl.add(notificationRow);
            });

        if (DB.notifs.isEmpty()) {

            nl.add(
                lbl(
                    "No notifications available.",
                    12,
                    false,
                    MUTED
                )
            );
        }

        g.add(section(
            "Recent Notifications",
            nl,
            link(
                "View All",
                e -> NotificationsDialog.show()
            )
        ));

        // =========================================
        // FINAL DASHBOARD LAYOUT
        // =========================================

        body.add(g, BorderLayout.CENTER);

        return pg[0];
    }
}

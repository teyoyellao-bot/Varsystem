
package varsystem.ui.mechanic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Req;
import varsystem.service.NotificationService;
import varsystem.ui.components.MapPanel;

import static varsystem.ui.components.Ui.card;
import static varsystem.ui.components.Ui.section;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;

public final class MechanicPages {

    private MechanicPages() {}

    private static final Color NAVY = new Color(7, 27, 58);
    private static final Color NAVY_CARD = new Color(18, 49, 91);
    private static final Color BLUE = new Color(22, 114, 255);
    private static final Color LIGHT_BLUE = new Color(234, 243, 255);
    private static final Color BG = new Color(240, 245, 252);
    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;
    private static final Color TEXT = new Color(25, 40, 62);
    private static final Color MUTED = new Color(115, 133, 156);
    private static final Color BORDER = new Color(215, 226, 240);
    private static final Color GREEN = new Color(22, 163, 108);
    private static final Color ORANGE = new Color(245, 158, 11);
    private static final Color RED = new Color(220, 65, 75);

    private static String mechanicName() {
        return Session.ME == null ? "" : Session.ME.name;
    }

    // ==========================================
    // JOB DATA
    // ==========================================

    private static List<Req> jobs() {

        List<Req> out = new ArrayList<>();

        if (Session.ME == null || Session.ME.shop == null)
            return out;

        for (Req r : DB.reqs) {

            if (r == null
                    || r.mechanic == null
                    || !r.mechanic.equalsIgnoreCase(mechanicName())
                    || r.shop == null
                    || !r.shop.equalsIgnoreCase(Session.ME.shop)) {
                continue;
            }

            if (r.status != null
                    && (r.status.equalsIgnoreCase("Completed")
                    || r.status.equalsIgnoreCase("Cancelled")
                    || r.status.equalsIgnoreCase("Declined"))) {

                r.shareLocation = false;
                r.userLatitude = Double.NaN;
                r.userLongitude = Double.NaN;
            }

            out.add(r);
        }

        out.sort((a, b) -> {
            if (a.submitted == null) return 1;
            if (b.submitted == null) return -1;
            return b.submitted.compareTo(a.submitted);
        });

        return out;
    }

    private static boolean active(Req r) {
        return r != null
                && r.status != null
                && (r.status.equalsIgnoreCase("Assigned")
                || r.status.equalsIgnoreCase("Accepted")
                || r.status.equalsIgnoreCase("On Going"));
    }

    private static boolean locationStatusAllowed(Req r) {
        return r != null
                && r.status != null
                && (r.status.equalsIgnoreCase("Assigned")
                || r.status.equalsIgnoreCase("Accepted")
                || r.status.equalsIgnoreCase("On Going"));
    }

    private static boolean canAccessUserLocation(Req r) {

        if (r == null || Session.ME == null)
            return false;

        if (Session.ME.shop == null
                || mechanicName().isBlank()
                || r.mechanic == null
                || r.shop == null) {
            return false;
        }

        if (!r.mechanic.equalsIgnoreCase(mechanicName()))
            return false;

        if (!r.shop.equalsIgnoreCase(Session.ME.shop))
            return false;

        if (!locationStatusAllowed(r))
            return false;

        if (!r.shareLocation)
            return false;

        if (!Double.isFinite(r.userLatitude)
                || !Double.isFinite(r.userLongitude)
                || r.userLatitude < -90
                || r.userLatitude > 90
                || r.userLongitude < -180
                || r.userLongitude > 180) {
            return false;
        }

        return r.canShareLocationWith(Session.ME.shop);
    }

    private static String date(java.time.LocalDateTime d) {
        return d == null
                ? "—"
                : d.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
    }

    private static Color statusColor(String status) {

        if (status == null)
            return BLACK;

        if (status.equalsIgnoreCase("Completed"))
            return GREEN;

        if (status.equalsIgnoreCase("On Going"))
            return BLUE;

        if (status.equalsIgnoreCase("Assigned")
                || status.equalsIgnoreCase("Accepted"))
            return ORANGE;

        if (status.equalsIgnoreCase("Declined")
                || status.equalsIgnoreCase("Cancelled"))
            return RED;

        return BLACK;
    }

    // ==========================================
    // MAIN PAGE
    // ==========================================

    public static JComponent build(String selected) {

        List<Req> all = jobs();

        long activeCount = all.stream()
                .filter(MechanicPages::active)
                .count();

        long completedCount = all.stream()
                .filter(r -> "Completed".equalsIgnoreCase(r.status))
                .count();

        long assignedCount = all.stream()
                .filter(r -> "Assigned".equalsIgnoreCase(r.status)
                        || "Accepted".equalsIgnoreCase(r.status))
                .count();

        String title = selected == null ? "Dashboard" : selected;

        JComponent[] pg = page(
                title,
                "Mechanic workspace • "
                        + mechanicName()
                        + " • "
                        + (Session.ME == null ? "" : Session.ME.shop)
        );

        JPanel body = (JPanel) pg[1];
        body.setBackground(BG);

        if ("Dashboard".equals(title)) {

            JPanel cards = new JPanel(new GridLayout(1, 3, 14, 14));
            cards.setOpaque(false);
            cards.setBorder(new EmptyBorder(5, 5, 16, 5));

            cards.add(metricCard("Assigned Requests",
                    String.valueOf(assignedCount), BLUE));

            cards.add(metricCard("Active Jobs",
                    String.valueOf(activeCount), ORANGE));

            cards.add(metricCard("Completed Jobs",
                    String.valueOf(completedCount), GREEN));

            body.add(cards, BorderLayout.NORTH);

            JPanel recent = new JPanel(new BorderLayout());
            recent.setOpaque(false);

            recent.add(jobTable(all, "Assigned Requests"), BorderLayout.CENTER);

            body.add(
                    section("My Recent Assignments", recent, null),
                    BorderLayout.CENTER
            );

        } else {

            List<Req> filtered = switch (title) {

                case "Active Jobs" ->
                        all.stream()
                                .filter(MechanicPages::active)
                                .toList();

                case "Completed Jobs", "Job History" ->
                        all.stream()
                                .filter(r ->
                                        "Completed".equalsIgnoreCase(r.status)
                                        || "Declined".equalsIgnoreCase(r.status)
                                        || "Cancelled".equalsIgnoreCase(r.status))
                                .toList();

                default ->
                        all.stream()
                                .filter(r ->
                                        "Assigned".equalsIgnoreCase(r.status)
                                        || "Accepted".equalsIgnoreCase(r.status))
                                .toList();
            };

            body.add(jobTable(filtered, title), BorderLayout.CENTER);
        }

        return pg[0];
    }

    // ==========================================
    // MECHANIC MAP PAGE
    // ==========================================

    public static JComponent buildMap() {

        JComponent[] pg = page(
                "Map",
                "Customer locations for your active assigned requests • "
                        + mechanicName()
                        + " • "
                        + (Session.ME == null ? "" : Session.ME.shop)
        );

        JPanel body = (JPanel) pg[1];
        body.setBackground(BG);

        MapPanel map = new MapPanel();

        Runnable updateMap = () -> {

            List<Req> authorized = jobs().stream()
                    .filter(MechanicPages::canAccessUserLocation)
                    .toList();

            map.setCustomerRequests(
                    authorized,
                    Session.ME == null ? null : Session.ME.shop
            );
        };

        updateMap.run();

        map.setMechanicLocateEnabled(true);

        body.add(map, BorderLayout.CENTER);

        Timer mapTimer = new Timer(2000, e -> updateMap.run());

        mapTimer.setRepeats(true);
        mapTimer.start();

        map.addHierarchyListener(e -> {

            if ((e.getChangeFlags()
                    & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
                    && !map.isDisplayable()) {

                mapTimer.stop();
            }
        });

        return pg[0];
    }

    // ==========================================
    // DASHBOARD METRIC CARDS
    // ==========================================

    private static JPanel metricCard(
            String heading,
            String value,
            Color accent
    ) {

        JPanel p = card();

        p.setBackground(WHITE);
        p.setLayout(new BorderLayout(8, 12));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(20, 22, 20, 22)
        ));

        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new java.awt.Dimension(5, 1));

        JPanel content = new JPanel(new BorderLayout(5, 10));
        content.setOpaque(false);

        JLabel title = new JLabel(heading);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(MUTED);

        JLabel count = new JLabel(value);
        count.setFont(new Font("Segoe UI", Font.BOLD, 30));
        count.setForeground(NAVY);

        content.add(title, BorderLayout.NORTH);
        content.add(count, BorderLayout.CENTER);

        p.add(accentBar, BorderLayout.WEST);
        p.add(content, BorderLayout.CENTER);

        return p;
    }

    // ==========================================
    // JOB TABLE
    // ==========================================

    private static JComponent jobTable(List<Req> records, String view) {

        JPanel host = new JPanel(new BorderLayout(0, 12));
        host.setOpaque(false);
        host.setBorder(new EmptyBorder(8, 4, 4, 4));

        String[] cols = {
                "Request",
                "Customer",
                "Vehicle",
                "Problem",
                "Status",
                "Date"
        };

        Object[][] data = new Object[records.size()][cols.length];

        for (int i = 0; i < records.size(); i++) {

            Req r = records.get(i);

            data[i] = new Object[]{
                    r.id,
                    r.uname,
                    r.vehicle,
                    r.problem,
                    r.status,
                    date(r.submitted)
            };
        }

        JTable table = new JTable(data, cols);

        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(BLACK);
        table.setBackground(WHITE);
        table.setSelectionForeground(BLACK);
        table.setSelectionBackground(new Color(215, 232, 255));

        table.setShowGrid(false);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableCellRenderer centerRenderer =
                new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(centerRenderer);
        }

        JTableHeader header = table.getTableHeader();

        header.setPreferredSize(new java.awt.Dimension(0, 42));
        header.setBackground(new Color(220, 235, 255));
        header.setForeground(BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl,
                    Object value,
                    boolean selected,
                    boolean focus,
                    int row,
                    int column
            ) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        tbl, value, selected, focus, row, column
                );

                label.setOpaque(true);
                label.setBackground(new Color(220, 235, 255));
                label.setForeground(BLACK);
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

                return label;
            }
        });

        for (int i = 0; i < table.getColumnCount(); i++) {

            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(switch (i) {
                        case 0 -> 110;
                        case 1 -> 150;
                        case 2 -> 140;
                        case 3 -> 260;
                        case 4 -> 120;
                        case 5 -> 160;
                        default -> 120;
                    });
        }

        table.getColumnModel().getColumn(4)
                .setCellRenderer(new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tbl,
                            Object value,
                            boolean selected,
                            boolean focus,
                            int row,
                            int column
                    ) {

                        JLabel label = (JLabel) super
                                .getTableCellRendererComponent(
                                        tbl, value, selected, focus, row, column
                                );

                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        label.setOpaque(true);
                        label.setForeground(BLACK);
                        label.setBackground(selected
                                ? new Color(215, 232, 255)
                                : WHITE);

                        return label;
                    }
                });

        JScrollPane scroll = new JScrollPane(table);

        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(WHITE);

        host.add(scroll, BorderLayout.CENTER);

        // ACTION PANEL
        // View Details is available for all job statuses.

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 8)
        );

        actions.setOpaque(false);

        JComboBox<String> pick = new JComboBox<>(
                records.stream()
                        .map(r -> safe(r.id) + " — "
                                + safe(r.uname) + " ("
                                + safe(r.status) + ")")
                        .toArray(String[]::new)
        );

        pick.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pick.setBackground(WHITE);
        pick.setForeground(BLACK);
        pick.setPreferredSize(new java.awt.Dimension(370, 36));

        // VIEW DETAILS BUTTON - BLACK TEXT
        JButton details = styledButton("View Details", NAVY);

        details.setForeground(Color.BLACK);

        details.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (details.isEnabled()) {
                    details.setBackground(NAVY.brighter());
                    details.setForeground(Color.BLACK);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                details.setBackground(NAVY);
                details.setForeground(Color.BLACK);
            }
        });

        JButton jobAction = styledButton("Start Job", BLUE);

        details.setEnabled(!records.isEmpty());
        jobAction.setEnabled(false);

        Runnable updateButtons = () -> {

            int ix = pick.getSelectedIndex();

            if (ix < 0 || ix >= records.size()) {
                details.setEnabled(false);
                jobAction.setEnabled(false);
                jobAction.setText("Start Job");
                jobAction.setBackground(BLUE);
                return;
            }

            Req selected = records.get(ix);

            details.setEnabled(true);

            if ("Assigned".equalsIgnoreCase(selected.status)
                    || "Accepted".equalsIgnoreCase(selected.status)) {

                jobAction.setText("Start Job");
                jobAction.setBackground(BLUE);
                jobAction.setEnabled(true);

            } else if ("On Going".equalsIgnoreCase(selected.status)) {

                jobAction.setText("Complete Job");
                jobAction.setBackground(GREEN);
                jobAction.setEnabled(true);

            } else {

                jobAction.setText("Job Completed");
                jobAction.setBackground(GREEN);
                jobAction.setEnabled(false);
            }
        };

        pick.addActionListener(e -> updateButtons.run());

        // VIEW DETAILS
        details.addActionListener(e -> {

            int ix = pick.getSelectedIndex();

            if (ix < 0 || ix >= records.size()) {
                JOptionPane.showMessageDialog(
                        host,
                        "Please select a job first.",
                        "No Job Selected",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            showDetails(records.get(ix));
        });

        // START / COMPLETE JOB
        jobAction.addActionListener(e -> {

            int ix = pick.getSelectedIndex();

            if (ix < 0 || ix >= records.size())
                return;

            Req selected = records.get(ix);

            if ("Assigned".equalsIgnoreCase(selected.status)
                    || "Accepted".equalsIgnoreCase(selected.status)) {

                int confirm = JOptionPane.showConfirmDialog(
                        host,
                        "Start working on this request?\n\n"
                                + "Customer: " + selected.uname
                                + "\nVehicle: " + selected.vehicle,
                        "Start Job Confirmation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    update(selected, "On Going");
                }

                updateButtons.run();
                return;
            }

            if ("On Going".equalsIgnoreCase(selected.status)) {

                int confirm = JOptionPane.showConfirmDialog(
                        host,
                        "Are you sure the job is finished?\n\n"
                                + "Customer: " + selected.uname
                                + "\nVehicle: " + selected.vehicle,
                        "Complete Job Confirmation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    update(selected, "Completed");
                }

                updateButtons.run();
                return;
            }

            JOptionPane.showMessageDialog(
                    host,
                    "This job can no longer be started or completed.",
                    "Invalid Job Status",
                    JOptionPane.WARNING_MESSAGE
            );

            updateButtons.run();
        });

        actions.add(pick);
        actions.add(details);
        actions.add(jobAction);

        host.add(actions, BorderLayout.SOUTH);

        updateButtons.run();

        return host;
    }

    // ==========================================
    // CUSTOMER MAP
    // ==========================================

    private static void openCustomerMap(Req r) {

        if (!canAccessUserLocation(r)) {
            JOptionPane.showMessageDialog(
                    null,
                    "User location is no longer available.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        MapPanel map = new MapPanel();

        map.setPreferredSize(new java.awt.Dimension(760, 500));

        map.setCustomerRequests(
                java.util.Collections.singletonList(r),
                Session.ME.shop
        );

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(WHITE);
        content.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel(
                "Customer Location • " + r.uname,
                SwingConstants.LEFT
        );

        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(NAVY);

        JLabel subtitle = new JLabel(
                "Request: " + r.id + "   |   " + r.status
        );

        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        content.add(heading, BorderLayout.NORTH);
        content.add(map, BorderLayout.CENTER);

        JDialog dialog = new JDialog(
                (java.awt.Frame) null,
                "Customer Map",
                true
        );

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content);
        dialog.setSize(800, 590);
        dialog.setMinimumSize(new java.awt.Dimension(650, 480));
        dialog.setLocationRelativeTo(null);

        Timer mapRefresh = new Timer(1000, e -> {

            if (!canAccessUserLocation(r)) {
                ((Timer) e.getSource()).stop();
                dialog.dispose();
                return;
            }

            map.repaint();
        });

        Timer accessMonitor = new Timer(800, e -> {

            if (!canAccessUserLocation(r)) {

                mapRefresh.stop();
                ((Timer) e.getSource()).stop();
                dialog.dispose();

                JOptionPane.showMessageDialog(
                        null,
                        "User location access has been revoked.\n"
                                + "The request is no longer active or authorized.",
                        "Location Access Revoked",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        mapRefresh.start();
        accessMonitor.start();

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                mapRefresh.stop();
                accessMonitor.stop();
            }
        });

        dialog.setVisible(true);
    }

    // ==========================================
    // CUSTOM BUTTONS
    // ==========================================

    private static JButton styledButton(String text, Color color) {

        JButton button = new JButton(text);

        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(WHITE);
        button.setBackground(color);

        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                new EmptyBorder(9, 18, 9, 18)
        ));

        button.setCursor(new java.awt.Cursor(
                java.awt.Cursor.HAND_CURSOR
        ));

        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(color.brighter());
                    button.setForeground(WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
                button.setForeground(WHITE);
            }
        });

        return button;
    }

    // ==========================================
    // JOB STATUS UPDATE
    // ==========================================

    private static void update(Req r, String status) {

        if (r == null
                || Session.ME == null
                || r.mechanic == null
                || !r.mechanic.equalsIgnoreCase(mechanicName())
                || r.shop == null
                || !r.shop.equalsIgnoreCase(Session.ME.shop)) {
            return;
        }

        if ("On Going".equals(status)
                && !("Assigned".equalsIgnoreCase(r.status)
                || "Accepted".equalsIgnoreCase(r.status))) {
            return;
        }

        if ("Completed".equals(status)
                && !"On Going".equalsIgnoreCase(r.status)) {
            return;
        }

        r.status = status;

        if ("On Going".equals(status)) {
            NotificationService.repairStarted(r);
        }

        if ("Completed".equals(status)) {
            r.completed = java.time.LocalDateTime.now();

            r.shareLocation = false;
            r.userLatitude = Double.NaN;
            r.userLongitude = Double.NaN;

            NotificationService.serviceCompleted(r);
        }

        DB.saveData();
        refresh();
    }

    // ==========================================
    // JOB DETAILS - CLEANER DESIGN
    // ==========================================

    private static void showDetails(Req r) {

        if (r == null)
            return;

        JDialog dialog = new JDialog(
                (java.awt.Frame) null,
                "Job Details",
                true
        );

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(740, 590);
        dialog.setMinimumSize(new java.awt.Dimension(620, 500));
        dialog.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);

        // HEADER
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 5));
        headerText.setOpaque(false);

        JLabel title = new JLabel("JOB DETAILS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(WHITE);

        JLabel subtitle = new JLabel("Request ID: " + safe(r.id));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(190, 211, 240));

        headerText.add(title);
        headerText.add(subtitle);

        JLabel statusBadge = new JLabel(" " + safe(r.status) + " ");
        statusBadge.setOpaque(true);
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusBadge.setHorizontalAlignment(SwingConstants.CENTER);
        statusBadge.setBorder(new EmptyBorder(8, 10, 8, 10));

        Color badgeBackground = new Color(225, 235, 250);
        Color badgeForeground = NAVY;

        if ("Completed".equalsIgnoreCase(r.status)) {
            badgeBackground = new Color(220, 252, 231);
            badgeForeground = new Color(22, 125, 75);
        } else if ("On Going".equalsIgnoreCase(r.status)) {
            badgeBackground = new Color(219, 234, 254);
            badgeForeground = new Color(29, 78, 216);
        } else if ("Cancelled".equalsIgnoreCase(r.status)
                || "Declined".equalsIgnoreCase(r.status)) {
            badgeBackground = new Color(254, 226, 226);
            badgeForeground = new Color(185, 28, 28);
        } else if ("Assigned".equalsIgnoreCase(r.status)
                || "Accepted".equalsIgnoreCase(r.status)) {
            badgeBackground = new Color(255, 237, 213);
            badgeForeground = new Color(180, 105, 15);
        }

        statusBadge.setBackground(badgeBackground);
        statusBadge.setForeground(badgeForeground);

        header.add(headerText, BorderLayout.CENTER);
        header.add(statusBadge, BorderLayout.EAST);

        // COMPACT DETAILS GRID
        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(16, 18, 16, 18));

        cards.add(detailCard("CUSTOMER", new String[][]{
                {"Name", safe(r.uname)},
                {"Contact", safe(r.uphone)}
        }));

        cards.add(detailCard("VEHICLE", new String[][]{
                {"Vehicle", safe(r.vehicle)},
                {"Problem", safe(r.problem)}
        }));

        cards.add(detailCard("REQUEST", new String[][]{
                {"Request ID", safe(r.id)},
                {"Shop", safe(r.shop)},
                {"Mechanic", safe(r.mechanic)},
                {"Date Requested", date(r.submitted)}
        }));

        cards.add(detailCard("JOB INFORMATION", new String[][]{
                {"Status", safe(r.status)},
                {"Date Completed", date(r.completed)}
        }));

        JPanel serviceCard = detailCard("SERVICE DETAILS", new String[][]{
                {"Services / Additional Information", safe(r.details)}
        });

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 18, 16, 18));

        content.add(cards, BorderLayout.NORTH);
        content.add(serviceCard, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.getViewport().setBackground(BG);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(WHITE);
        footer.setBorder(new EmptyBorder(10, 16, 10, 16));

        // CLOSE BUTTON - BLACK TEXT INCLUDING HOVER
        JButton close = styledButton("Close", NAVY);

        close.setForeground(Color.BLACK);

        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (close.isEnabled()) {
                    close.setBackground(NAVY.brighter());
                    close.setForeground(Color.BLACK);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                close.setBackground(NAVY);
                close.setForeground(Color.BLACK);
            }
        });

        close.addActionListener(e -> dialog.dispose());

        footer.add(close);

        main.add(header, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        main.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    // ==========================================
    // JOB DETAILS CARD - COMPACT
    // ==========================================

    private static JPanel detailCard(
            String heading,
            String[][] information
    ) {

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(WHITE);

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(14, 15, 14, 15)
        ));

        JLabel title = new JLabel(heading);
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(BLUE);

        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 9));
        rows.setOpaque(false);

        for (String[] item : information) {

            String label = item[0] == null ? "" : item[0];

            String value = item[1] == null
                    || item[1].isBlank()
                    ? "—"
                    : item[1];

            JPanel row = new JPanel(new BorderLayout(0, 3));
            row.setOpaque(false);

            JLabel fieldLabel = new JLabel(label);
            fieldLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            fieldLabel.setForeground(MUTED);

            JLabel fieldValue = new JLabel(
                    "<html><div style='width:230px;'>"
                    + value.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;")
                          .replace("\n", "<br>")
                    + "</div></html>"
            );

            fieldValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
            fieldValue.setForeground(TEXT);

            row.add(fieldLabel, BorderLayout.NORTH);
            row.add(fieldValue, BorderLayout.CENTER);

            rows.add(row);
        }

        panel.add(title, BorderLayout.NORTH);
        panel.add(rows, BorderLayout.CENTER);

        return panel;
    }

    // ==========================================
    // SAFE TEXT
    // ==========================================

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}


package varsystem.ui.frame;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import varsystem.data.DB;
import static varsystem.data.Session.ME;
import varsystem.ui.admin.AdminDashboard;
import varsystem.ui.admin.AdminMapPage;
import varsystem.ui.admin.AdminReportsPage;
import varsystem.ui.admin.AdminRequestsPage;
import varsystem.ui.admin.AdminSettingsPage;
import varsystem.ui.admin.AdminShopsPage;
import varsystem.ui.admin.AdminSupportPage;
import varsystem.ui.admin.AdminUsersPage;
import varsystem.ui.auth.LoginScreen;
import varsystem.ui.common.ChatDialog;
import varsystem.ui.common.NotificationsDialog;
import varsystem.ui.common.ProfilePage;
import static varsystem.ui.components.Dialogs.ask;
import varsystem.ui.components.Logo;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.vbox;
import varsystem.ui.mechanic.MechanicPages;
import varsystem.ui.shop.ShopDashboard;
import varsystem.ui.shop.ShopMechanicsPage;
import varsystem.ui.shop.ShopReportsPage;
import varsystem.ui.shop.ShopRequestsPage;
import varsystem.ui.shop.ShopSchedulePage;
import varsystem.ui.shop.ShopServicePricesPage;
import varsystem.ui.shop.ShopServicesPage;
import varsystem.ui.shop.ShopSettingsPage;
import varsystem.ui.theme.Theme;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.DARK;
import static varsystem.ui.theme.Theme.NAVY;
import static varsystem.ui.theme.Theme.pageBg;
import varsystem.ui.user.FindShopPage;
import varsystem.ui.user.MyRequestsPage;
import varsystem.ui.user.UserDashboard;
import varsystem.ui.user.UserHistoryPage;

/** The application window: sidebar navigation + page area. */
public final class MainFrame {

    private MainFrame() { }

    public static JFrame APP;
    public static String CUR = "";

    public static final LinkedHashMap<String, Supplier<JComponent>> PAGES =
            new LinkedHashMap<>();

    public static final Map<String, JButton> NAV = new HashMap<>();

    public static JPanel CONTENT;

    private static final Color ICON_WHITE = Color.WHITE;
    private static final Color ICON_NAVY = new Color(0x03, 0x19, 0x3E);

    private static final List<BadgeButton> CHAT_BADGES =
            new ArrayList<>();

    private static final List<BadgeButton> NOTIFICATION_BADGES =
            new ArrayList<>();

    private static Timer badgeTimer;

    // ==================================================
    // BUTTON WITH RED BADGE
    // ==================================================

    public static class BadgeButton extends JButton {

        private int badgeCount = 0;

        public BadgeButton(String text) {
            super(text);
            badgeCount = 0;
        }

        public void setBadgeCount(int count) {
            badgeCount = Math.max(0, count);
            repaint();
        }

        public int getBadgeCount() {
            return badgeCount;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            if (badgeCount <= 0) {
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            String text = badgeCount > 99
                    ? "99+"
                    : String.valueOf(badgeCount);

            Font badgeFont = new Font(
                    "SansSerif",
                    Font.BOLD,
                    10
            );

            g.setFont(badgeFont);

            FontMetrics fm = g.getFontMetrics();

            int badgeWidth = Math.max(
                    19,
                    fm.stringWidth(text) + 10
            );

            int badgeHeight = 19;

            int x = getWidth() - badgeWidth - 7;
            int y = 3;

            g.setColor(new Color(220, 35, 48));

            g.fillRoundRect(
                    x,
                    y,
                    badgeWidth,
                    badgeHeight,
                    18,
                    18
            );

            g.setColor(Color.WHITE);

            g.drawString(
                    text,
                    x + (badgeWidth - fm.stringWidth(text)) / 2,
                    y + (badgeHeight - fm.getHeight()) / 2
                            + fm.getAscent()
            );

            g.dispose();
        }
    }

    // ==================================================
    // START
    // ==================================================

    public static void start() {

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | UnsupportedLookAndFeelException ignored) {
            // Keep default Look and Feel.
        }

        APP = new JFrame("Vehicle Assistance and Repair Locator");
        APP.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        APP.setSize(1320, 780);
        APP.setMinimumSize(new Dimension(1120, 680));
        APP.setLocationRelativeTo(null);

        LoginScreen.show();
        APP.setVisible(true);
    }

    public static void setRoot(JComponent c) {
        APP.setContentPane(c);
        APP.revalidate();
        APP.repaint();
    }

    // ==================================================
    // MAIN SCREEN
    // ==================================================

    public static void showMain() {

        if (ME == null) {
            LoginScreen.show();
            return;
        }

        if (badgeTimer != null) {
            badgeTimer.stop();
            badgeTimer = null;
        }

        CHAT_BADGES.clear();
        NOTIFICATION_BADGES.clear();

        boolean admin = "Admin".equalsIgnoreCase(ME.role);
        boolean shop = "Shop Manager".equalsIgnoreCase(ME.role);
        boolean mechanic = "Mechanic".equalsIgnoreCase(ME.role);

        Theme.DARK = admin || shop;

        PAGES.clear();
        NAV.clear();

        if (admin) {

            PAGES.put("Dashboard", AdminDashboard::build);
            PAGES.put("Manage Users", AdminUsersPage::build);
            PAGES.put("Manage Repair Shop", AdminShopsPage::build);
            PAGES.put("Map", AdminMapPage::build);
            PAGES.put("Service Request", AdminRequestsPage::build);
            PAGES.put("Reports & Analytics", AdminReportsPage::build);
            PAGES.put("Support Inbox", AdminSupportPage::build);
            PAGES.put("Setting", AdminSettingsPage::build);

        } else if (shop) {

            PAGES.put("Dashboard", ShopDashboard::build);
            PAGES.put("Mechanics", ShopMechanicsPage::build);
            PAGES.put("History", ShopSchedulePage::build);
            PAGES.put("Service Requests", ShopRequestsPage::build);
            PAGES.put("Services", ShopServicesPage::build);
            PAGES.put("Service Prices", ShopServicePricesPage::build);
            PAGES.put("Reports & Analytics", ShopReportsPage::build);
            PAGES.put("Shop Settings", ShopSettingsPage::build);
            PAGES.put("Profile", ProfilePage::build);

        } else if (mechanic) {

            PAGES.put("Dashboard", () -> MechanicPages.build("Dashboard"));
            PAGES.put("Assigned Requests", () -> MechanicPages.build("Assigned Requests"));
            PAGES.put("Active Jobs", () -> MechanicPages.build("Active Jobs"));
            PAGES.put("Job History", () -> MechanicPages.build("Job History"));
            PAGES.put("Map", MechanicPages::buildMap);
            PAGES.put("Profile", ProfilePage::build);

        } else {

            PAGES.put("Dashboard", UserDashboard::build);
            PAGES.put("Find Shop", FindShopPage::build);
            PAGES.put("My Requests", MyRequestsPage::build);

            // USER REPAIR HISTORY
            PAGES.put("History", UserHistoryPage::build);

            PAGES.put("Profile", ProfilePage::build);
        }

        JPanel root = new JPanel(new BorderLayout());

        // SIDEBAR

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(NAVY);
        side.setBorder(new EmptyBorder(22, 14, 18, 14));
        side.setPreferredSize(new Dimension(250, 100));

        Logo lg = new Logo(74);
        lg.setAlignmentX(Component.CENTER_ALIGNMENT);

        side.add(lg);
        side.add(Box.createVerticalStrut(8));

        for (String s : new String[]{
                "VEHICLE",
                "ASSISTANCE AND",
                "REPAIR LOCATOR"
        }) {
            JLabel l = lbl(s, 12, true, Color.WHITE);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            side.add(l);
        }

        side.add(Box.createVerticalStrut(22));

        // NAVIGATION

        for (String name : PAGES.keySet()) {

            BadgeButton b = new BadgeButton(name);

            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(new Font("SansSerif", Font.PLAIN, 14));
            b.setFocusPainted(false);

            b.setBorder(new EmptyBorder(11, 16, 11, 16));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.setPreferredSize(new Dimension(220, 44));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);

            b.setCursor(Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
            ));

            b.setContentAreaFilled(false);
            b.setOpaque(true);

            String iconName = getPageIcon(name);

            b.putClientProperty("lucideName", iconName);
            b.setIcon(new LucideIcon(iconName, 19, ICON_WHITE));
            b.setIconTextGap(13);

            b.addActionListener(e -> go(name));

            NAV.put(name, b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
        }

        side.add(Box.createVerticalGlue());

        // LOGOUT

        JButton lo = new JButton("Log out");

        lo.setIcon(new LucideIcon("log-out", 19, ICON_WHITE));
        lo.setIconTextGap(12);

        lo.setForeground(Color.WHITE);
        lo.setBackground(NAVY);
        lo.setOpaque(true);
        lo.setContentAreaFilled(false);
        lo.setBorder(new EmptyBorder(11, 16, 11, 16));
        lo.setFocusPainted(false);
        lo.setFont(new Font("SansSerif", Font.BOLD, 14));
        lo.setHorizontalAlignment(SwingConstants.LEFT);
        lo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        lo.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));

        lo.addActionListener(e -> logout());

        side.add(lo);

        // CONTENT

        CONTENT = new JPanel(new BorderLayout());

        root.add(side, BorderLayout.WEST);
        root.add(CONTENT, BorderLayout.CENTER);

        setRoot(root);

        go("Dashboard");

        // INITIAL BADGE COUNTS

        refreshBadges();

        // AUTOMATIC BADGE REFRESH EVERY SECOND

        badgeTimer = new Timer(1000, e -> refreshBadges());
        badgeTimer.start();
    }

    // ==================================================
    // BADGE COUNTER REFRESH
    // ==================================================

    public static void refreshBadges() {

        if (ME == null) {
            return;
        }

        int chatCount = 0;
        int notificationCount = 0;
        int supportCount = 0;
        int requestCount = 0;

        try {
            chatCount = DB.getUnreadChatCount();
        } catch (Exception ignored) { }

        try {
            notificationCount = DB.getUnreadNotificationCount();
        } catch (Exception ignored) { }

        try {
            supportCount = DB.getUnreadSupportCount();
        } catch (Exception ignored) { }

        try {
            requestCount = DB.getUnreadServiceRequestCount();
        } catch (Exception ignored) { }

        final int chats = chatCount;
        final int notifications = notificationCount;
        final int support = supportCount;
        final int requests = requestCount;

        Runnable update = () -> {

            for (BadgeButton b : CHAT_BADGES) {
                b.setBadgeCount(chats);
            }

            for (BadgeButton b : NOTIFICATION_BADGES) {
                b.setBadgeCount(notifications);
            }

            setNavigationBadge(
                    "Support Inbox",
                    support
            );

            setNavigationBadge(
                    "Service Request",
                    requests
            );

            setNavigationBadge(
                    "Service Requests",
                    requests
            );

            setNavigationBadge(
                    "My Requests",
                    requests
            );
        };

        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }

    private static void setNavigationBadge(
            String pageName,
            int count) {

        JButton button = NAV.get(pageName);

        if (button instanceof BadgeButton badgeButton) {
            badgeButton.setBadgeCount(count);
        }
    }

    // ==================================================
    // PAGE ICON MAPPING
    // ==================================================

    private static String getPageIcon(String name) {

        return switch (name) {
            case "Dashboard" -> "layout-dashboard";
            case "Manage Users" -> "users-round";
            case "Manage Repair Shop" -> "store";
            case "Map" -> "map-pin";

            case "Service Request",
                 "Service Requests",
                 "My Requests" -> "clipboard-list";

            case "Services" -> "wrench";
            case "Service Prices" -> "dollar-sign";
            case "Reports & Analytics" -> "chart-no-axes-combined";
            case "Support Inbox" -> "life-buoy";

            case "Setting",
                 "Shop Settings" -> "settings";

            case "Mechanics",
                 "Assigned Requests" -> "users-round";
            case "Active Jobs" -> "wrench";
            case "Completed Jobs",
                 "Job History",
                 "History" -> "calendar-clock";
            case "Notifications" -> "bell";
            case "Preferences" -> "settings";
            case "Find Shop" -> "search";
            case "Profile" -> "circle-user-round";
            default -> "circle-help";
        };
    }

    // ==================================================
    // LOGOUT
    // ==================================================

    public static void logout() {

        if (!ask("Are you sure you want to log out?")) {
            return;
        }

        if (badgeTimer != null) {
            badgeTimer.stop();
            badgeTimer = null;
        }

        DB.log("Logout: " + ME.username);

        CHAT_BADGES.clear();
        NOTIFICATION_BADGES.clear();

        Theme.DARK = true;

        LoginScreen.show();
    }

    // ==================================================
    // NAVIGATION
    // ==================================================

    public static void go(String name) {

        if (!PAGES.containsKey(name)) {
            return;
        }

        CUR = name;

        for (Map.Entry<String, JButton> e : NAV.entrySet()) {

            boolean on = e.getKey().equals(name);
            JButton button = e.getValue();

            button.setBackground(on ? Color.WHITE : NAVY);
            button.setForeground(on ? NAVY : Color.WHITE);

            String iconName = (String)
                    button.getClientProperty("lucideName");

            button.setIcon(
                    new LucideIcon(
                            iconName,
                            19,
                            on ? ICON_NAVY : ICON_WHITE
                    )
            );
        }

        CONTENT.removeAll();

        CONTENT.add(
                PAGES.get(name).get(),
                BorderLayout.CENTER
        );

        CONTENT.revalidate();
        CONTENT.repaint();

        refreshBadges();
    }

    public static void refresh() {
        go(CUR);
    }

    // ==================================================
    // PAGE HEADER
    // ==================================================

    public static JComponent[] page(
            String title,
            String subtitle,
            JComponent... extra) {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(pageBg());

        JPanel head = new JPanel(new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setPaint(
                        new GradientPaint(
                                0, 0,
                                DARK ? NAVY : new Color(0x08, 0x2A, 0x66),
                                getWidth(), 0,
                                new Color(0x0D, 0x57, 0xB5)
                        )
                );

                g2.fillRect(
                        0, 0,
                        getWidth(),
                        getHeight()
                );

                g2.dispose();
            }
        };

        head.setBorder(new EmptyBorder(14, 22, 14, 22));

        JPanel t = vbox();

        t.add(lbl(title, 24, true, Color.WHITE));

        t.add(lbl(
                subtitle,
                12,
                false,
                new Color(0xCF, 0xE0, 0xFF)
        ));

        head.add(t, BorderLayout.WEST);

        JPanel r = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 8)
        );

        r.setOpaque(false);

        for (JComponent c : extra) {
            r.add(c);
        }

        if (!"Admin".equalsIgnoreCase(ME.role)) {

            BadgeButton chats = new BadgeButton("Chats");

            chats.setFont(new Font("SansSerif", Font.BOLD, 13));
            chats.setForeground(Color.WHITE);
            chats.setBackground(BLUE);
            chats.setFocusPainted(false);
            chats.setBorderPainted(false);
            chats.setContentAreaFilled(true);
            chats.setCursor(Cursor.getPredefinedCursor(
                    Cursor.HAND_CURSOR
            ));

            chats.setIcon(
                    new LucideIcon("message-circle", 17, Color.WHITE)
            );

            chats.setIconTextGap(7);

            chats.addActionListener(e -> {
                DB.markAllChatsRead();
                refreshBadges();
                ChatDialog.open(null);
                refreshBadges();
            });

            CHAT_BADGES.add(chats);
            r.add(chats);
        }

        // ==================================================
        // NOTIFICATIONS HEADER BUTTON
        // ENABLED FOR ALL ROLES INCLUDING MECHANIC
        // ==================================================

        BadgeButton notifications =
                new BadgeButton("Notifications");

        notifications.setFont(new Font("SansSerif", Font.BOLD, 13));
        notifications.setForeground(Color.WHITE);
        notifications.setBackground(new Color(0x1E, 0x4A, 0x8C));
        notifications.setFocusPainted(false);
        notifications.setBorderPainted(false);
        notifications.setContentAreaFilled(true);
        notifications.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR
        ));

        notifications.setIcon(
                new LucideIcon("bell", 17, Color.WHITE)
        );

        notifications.setIconTextGap(7);

        notifications.addActionListener(e -> {
            NotificationsDialog.show();
            refreshBadges();
        });

        NOTIFICATION_BADGES.add(notifications);
        r.add(notifications);

        r.add(lbl(
                "Shop Manager".equalsIgnoreCase(ME.role)
                        ? "Shop Manager • " + ME.shop
                        : ME.name,
                13,
                true,
                Color.WHITE
        ));

        head.add(r, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout());

        body.setBackground(pageBg());
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        root.add(head, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        return new JComponent[]{root, body};
    }

    // ==================================================
    // LUCIDE STYLE ICON DRAWER
    // ==================================================

    public static class LucideIcon implements Icon {

        private final String name;
        private final int size;
        private final Color color;

        public LucideIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(
                Component c,
                Graphics graphics,
                int x,
                int y) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.translate(x, y);
            g.scale(size / 24.0, size / 24.0);

            g.setColor(color);
            g.setStroke(new BasicStroke(
                    1.8f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            drawIcon(g);
            g.dispose();
        }

        private void drawIcon(Graphics2D g) {

            switch (name) {

                case "layout-dashboard" -> {
                    roundRect(g, 3, 3, 8, 8, 1);
                    roundRect(g, 13, 3, 8, 5, 1);
                    roundRect(g, 13, 10, 8, 11, 1);
                    roundRect(g, 3, 13, 8, 8, 1);
                }

                case "star" -> {
                    Path2D star = new Path2D.Double();

                    star.moveTo(12, 2);
                    star.lineTo(15, 9);
                    star.lineTo(22, 9);
                    star.lineTo(17, 14);
                    star.lineTo(19, 21);
                    star.lineTo(12, 17);
                    star.lineTo(5, 21);
                    star.lineTo(7, 14);
                    star.lineTo(2, 9);
                    star.lineTo(9, 9);
                    star.lineTo(12, 2);
                    star.closePath();

                    g.fill(star);
                }

                case "phone-call" -> {
                    Path2D phone = new Path2D.Double();

                    phone.moveTo(5, 3);
                    phone.lineTo(9, 3);
                    phone.lineTo(11, 8);
                    phone.lineTo(8, 10);
                    phone.curveTo(9, 13, 11, 15, 14, 16);
                    phone.lineTo(16, 13);
                    phone.lineTo(21, 15);
                    phone.lineTo(21, 19);
                    phone.curveTo(21, 21, 19, 22, 17, 21);
                    phone.curveTo(8, 19, 4, 14, 3, 7);
                    phone.curveTo(3, 5, 3, 3, 5, 3);
                    phone.closePath();

                    g.fill(phone);
                }

                case "users", "users-round" -> {

                    g.draw(new Ellipse2D.Double(8.5, 3, 7, 7));
                    g.draw(new Ellipse2D.Double(2.5, 6.5, 5.5, 5.5));
                    g.draw(new Ellipse2D.Double(16, 6.5, 5.5, 5.5));

                    Path2D centerUser = new Path2D.Double();
                    centerUser.moveTo(4.5, 20);
                    centerUser.curveTo(4.5, 16, 7.3, 13.8, 12, 13.8);
                    centerUser.curveTo(16.7, 13.8, 19.5, 16, 19.5, 20);
                    g.draw(centerUser);

                    Path2D leftUser = new Path2D.Double();
                    leftUser.moveTo(1.5, 20);
                    leftUser.curveTo(1.5, 17, 3, 15, 6, 14.5);
                    leftUser.curveTo(7, 14.3, 7.5, 14.3, 8, 14.5);
                    g.draw(leftUser);

                    Path2D rightUser = new Path2D.Double();
                    rightUser.moveTo(16, 14.5);
                    rightUser.curveTo(17, 14.3, 17.5, 14.3, 18, 14.5);
                    rightUser.curveTo(21, 15, 22.5, 17, 22.5, 20);
                    g.draw(rightUser);
                }

                case "store" -> {
                    path(g,
                            3, 10, 21, 10,
                            3, 10, 5, 4,
                            5, 4, 19, 4,
                            19, 4, 21, 10
                    );
                    line(g, 5, 10, 5, 21);
                    line(g, 19, 10, 19, 21);
                    line(g, 5, 21, 19, 21);
                    line(g, 10, 21, 10, 14);
                    line(g, 10, 14, 14, 14);
                    line(g, 14, 14, 14, 21);
                    line(g, 3, 10, 21, 10);
                }

                case "map-pin" -> {
                    Path2D pin = new Path2D.Double();

                    pin.moveTo(12, 22);
                    pin.curveTo(10, 19, 5, 13, 5, 9);
                    pin.curveTo(5, 5, 8, 3, 12, 3);
                    pin.curveTo(16, 3, 19, 5, 19, 9);
                    pin.curveTo(19, 13, 14, 19, 12, 22);
                    pin.closePath();

                    g.setStroke(new BasicStroke(
                            2.5f,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    ));

                    g.draw(pin);
                    g.fill(new Ellipse2D.Double(9, 6, 6, 6));

                    g.setColor(color);
                    g.fill(new Ellipse2D.Double(10, 7, 4, 4));
                }

                case "clipboard-list" -> {
                    roundRect(g, 5, 4, 14, 17, 2);
                    roundRect(g, 9, 2, 6, 4, 1);
                    line(g, 9, 11, 15, 11);
                    line(g, 9, 15, 15, 15);
                    line(g, 9, 18, 13, 18);
                }

                case "wrench" -> {
                    path(g,
                            14, 6, 17, 3,
                            17, 3, 21, 3,
                            21, 3, 18, 8,
                            18, 8, 16, 10,
                            16, 10, 7, 19,
                            7, 19, 4, 20,
                            4, 20, 5, 17,
                            5, 17, 14, 8
                    );
                }

                case "chart-no-axes-combined" -> {
                    line(g, 3, 3, 3, 21);
                    line(g, 3, 21, 21, 21);
                    line(g, 6, 16, 11, 11);
                    line(g, 11, 11, 15, 14);
                    line(g, 15, 14, 21, 6);
                    line(g, 16, 6, 21, 6);
                    line(g, 21, 6, 21, 11);
                }

                case "life-buoy" -> {
                    oval(g, 3, 3, 18, 18);
                    oval(g, 8, 8, 8, 8);
                    line(g, 5, 5, 9, 9);
                    line(g, 15, 15, 19, 19);
                    line(g, 19, 5, 15, 9);
                    line(g, 9, 15, 5, 19);
                }

                case "settings" -> {
                    Path2D gear = new Path2D.Double();

                    double[] offsets = {
                            -Math.PI / 8,
                            -Math.PI / 12,
                            -Math.PI / 12,
                            Math.PI / 12,
                            Math.PI / 12,
                            Math.PI / 8
                    };

                    double[] radii = {
                            8.5, 8.5, 10.5, 10.5, 8.5, 8.5
                    };

                    for (int tooth = 0; tooth < 8; tooth++) {

                        double centerAngle =
                                -Math.PI / 2 + tooth * Math.PI / 4;

                        for (int i = 0; i < offsets.length; i++) {

                            double angle = centerAngle + offsets[i];

                            double x = 12 + Math.cos(angle) * radii[i];
                            double y = 12 + Math.sin(angle) * radii[i];

                            if (tooth == 0 && i == 0) {
                                gear.moveTo(x, y);
                            } else {
                                gear.lineTo(x, y);
                            }
                        }
                    }

                    gear.closePath();
                    g.draw(gear);
                    g.draw(new Ellipse2D.Double(8, 8, 8, 8));
                }

                case "calendar-clock" -> {
                    roundRect(g, 3, 5, 18, 16, 2);
                    line(g, 7, 3, 7, 7);
                    line(g, 17, 3, 17, 7);
                    line(g, 3, 10, 21, 10);
                    oval(g, 12, 12, 8, 8);
                    line(g, 16, 14, 16, 16);
                    line(g, 16, 16, 18, 17);
                }

                case "search" -> {
                    oval(g, 3, 3, 13, 13);
                    line(g, 13, 13, 21, 21);
                }

                case "user-round" -> {
                    oval(g, 8, 3, 8, 8);
                    arc(g, 3, 13, 18, 12, 180, 180);
                }

                case "circle-user-round" -> {
                    g.draw(new Ellipse2D.Double(2, 2, 20, 20));
                    g.draw(new Ellipse2D.Double(9, 6, 6, 6));

                    Path2D profile = new Path2D.Double();
                    profile.moveTo(6, 19);
                    profile.curveTo(6, 15, 8.5, 13, 12, 13);
                    profile.curveTo(15.5, 13, 18, 15, 18, 19);

                    g.draw(profile);
                }

                case "log-out" -> {
                    line(g, 10, 17, 10, 21);
                    line(g, 10, 21, 4, 21);
                    line(g, 4, 21, 4, 3);
                    line(g, 4, 3, 10, 3);
                    line(g, 10, 3, 10, 7);
                    line(g, 13, 8, 19, 12);
                    line(g, 19, 12, 13, 16);
                    line(g, 19, 12, 8, 12);
                }

                case "message-circle" -> {
                    path(g,
                            21, 11, 21, 13,
                            21, 13, 19, 17,
                            19, 17, 15, 19,
                            15, 19, 8, 19,
                            8, 19, 4, 17,
                            4, 17, 3, 13,
                            3, 13, 3, 10,
                            3, 10, 5, 6,
                            5, 6, 9, 4,
                            9, 4, 15, 4,
                            15, 4, 19, 6,
                            19, 6, 21, 11
                    );
                    line(g, 8, 19, 6, 22);
                }

                case "bell" -> {
                    path(g,
                            18, 9, 18, 13,
                            18, 13, 20, 17,
                            20, 17, 4, 17,
                            4, 17, 6, 13,
                            6, 13, 6, 9,
                            6, 9, 12, 3,
                            12, 3, 18, 9
                    );
                    arc(g, 9, 17, 6, 6, 180, 180);
                }

                case "dollar-sign" -> {
                    oval(g, 3, 3, 18, 18);
                    line(g, 12, 6, 12, 18);
                    arc(g, 7, 7, 10, 5, 180, 180);
                    arc(g, 7, 12, 10, 5, 0, 180);
                }

                default -> {
                    oval(g, 4, 4, 16, 16);
                    line(g, 12, 8, 12, 12);
                    oval(g, 11, 15, 2, 2);
                }
            }
        }

        private void line(
                Graphics2D g,
                double x1, double y1,
                double x2, double y2) {

            g.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        private void oval(
                Graphics2D g,
                double x, double y,
                double w, double h) {

            g.draw(new Ellipse2D.Double(x, y, w, h));
        }

        private void roundRect(
                Graphics2D g,
                double x, double y,
                double w, double h,
                double arc) {

            g.draw(new RoundRectangle2D.Double(
                    x, y, w, h, arc * 2, arc * 2
            ));
        }

        private void arc(
                Graphics2D g,
                double x, double y,
                double w, double h,
                double start, double extent) {

            g.draw(new Arc2D.Double(
                    x, y, w, h,
                    start, extent,
                    Arc2D.OPEN
            ));
        }

        private void path(Graphics2D g, double... points) {

            Path2D p = new Path2D.Double();

            p.moveTo(points[0], points[1]);

            for (int i = 2; i < points.length; i += 2) {
                p.lineTo(points[i], points[i + 1]);
            }

            g.draw(p);
        }
    }
}

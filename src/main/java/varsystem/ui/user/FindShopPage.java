
package varsystem.ui.user;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Shop;
import varsystem.service.LocationService;
import varsystem.service.LocationService.Location;
import varsystem.ui.components.MapPanel;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.NAVY;

public final class FindShopPage {

    private FindShopPage() {}

    private static final Color GRADIENT_DARK =
            new Color(0x00, 0x16, 0x38);

    private static final Color GRADIENT_MID =
            new Color(0x08, 0x2D, 0x60);

    private static final Color GRADIENT_LIGHT =
            new Color(0x10, 0x4B, 0x85);

    private static final Color GRADIENT_BORDER =
            new Color(0x3A, 0x79, 0xB8);

    private static final Color LIGHT_TEXT =
            new Color(0xD9, 0xEA, 0xFF);

    // =================================================
    // ADMIN CONFIGURABLE RADIUS
    // =================================================

    static double radius() {
        int configured = DB.intS(
                "Map & Location.Nearby Shop Radius",
                3
        );
        return Math.max(1, Math.min(50, configured));
    }

    // =================================================
    // VALIDATE COORDINATES
    // =================================================

    private static boolean validCoordinates(
            double latitude,
            double longitude) {

        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    // =================================================
    // HAVERSINE DISTANCE
    // =================================================

    private static double distanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double earthRadius = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        a = Math.max(0.0, Math.min(1.0, a));

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        // FIX: correct variable name
        return earthRadius * c;
    }

    // =================================================
    // BUILD PAGE
    // =================================================

    public static JComponent build() {

        JComponent[] pg = page(
                "Find Shop",
                "Search nearby repair shops and request assistance."
        );

        JPanel originalBody = (JPanel) pg[1];

        PageGradientPanel body =
                new PageGradientPanel(
                        new BorderLayout(10, 0)
                );

        body.setBorder(new EmptyBorder(0, 0, 0, 0));

        pg[0].remove(originalBody);
        pg[0].add(body, BorderLayout.CENTER);
        pg[0].setOpaque(false);

        JTextField q = new JTextField();

        MapPanel map = new MapPanel();

        double[] userLatitude = {Double.NaN};
        double[] userLongitude = {Double.NaN};
        boolean[] locationAvailable = {false};

        // =================================================
        // SHOP CARDS
        // =================================================

        JPanel cards = new JPanel();

        cards.setLayout(
                new BoxLayout(cards, BoxLayout.Y_AXIS)
        );

        cards.setOpaque(false);

        // =================================================
        // LOCATE BUTTON
        // =================================================

        GradientButton locateButton =
                new GradientButton("Locate Me");

        // =================================================
        // SEARCH AND DISPLAY
        // =================================================

        Runnable fill = () -> {

            cards.removeAll();

            if (!locationAvailable[0]
                    || !validCoordinates(
                            userLatitude[0],
                            userLongitude[0])) {

                map.setShops(new ArrayList<>());

                cards.add(
                        lbl(
                                "Locate Me to find nearby repair shops.",
                                13,
                                false,
                                LIGHT_TEXT
                        )
                );

                cards.revalidate();
                cards.repaint();
                return;
            }

            String search = q.getText()
                    .toLowerCase()
                    .trim();

            double rad = radius();

            Map<Shop, Double> distances = new HashMap<>();
            List<Shop> res = new ArrayList<>();

            // =================================================
            // FILTER SHOPS
            // =================================================

            for (Shop shop : DB.shops) {

                if (shop == null
                        || shop.name == null
                        || !"Active".equalsIgnoreCase(
                                shop.status == null
                                        ? ""
                                        : shop.status.trim())) {
                    continue;
                }

                if (!shop.hasLocation()
                        || !validCoordinates(
                                shop.latitude,
                                shop.longitude)) {
                    continue;
                }

                double distance = distanceKm(
                        userLatitude[0],
                        userLongitude[0],
                        shop.latitude,
                        shop.longitude
                );

                distances.put(shop, distance);

                if (distance > rad) {
                    continue;
                }

                boolean matchesSearch =
                        search.isEmpty()
                        || shop.name.toLowerCase().contains(search)
                        || (shop.location != null
                                && shop.location.toLowerCase()
                                        .contains(search))
                        || (shop.services != null
                                && shop.services.stream()
                                        .anyMatch(service ->
                                                service != null
                                                && service.toLowerCase()
                                                        .contains(search)));

                if (!matchesSearch) {
                    continue;
                }

                res.add(shop);
            }

            // =================================================
            // SORT NEAREST FIRST
            // =================================================

            res.sort(
                    Comparator.comparingDouble(
                            shop -> distances.getOrDefault(
                                    shop,
                                    Double.MAX_VALUE
                            )
                    )
            );

            // =================================================
            // MAP
            // =================================================

            map.setShops(res);

            // =================================================
            // SHOP LIST
            // =================================================

            if (res.isEmpty()) {

                cards.add(
                        lbl(
                                search.isEmpty()
                                        ? "No nearby repair shops found."
                                        : "No nearby repair shops match your search.",
                                13,
                                false,
                                LIGHT_TEXT
                        )
                );

            } else {

                for (Shop shop : res) {

                    double actualDistance =
                            distances.getOrDefault(shop, 0.0);

                    cards.add(
                            shopCard(shop, actualDistance)
                    );

                    cards.add(
                            Box.createVerticalStrut(10)
                    );
                }
            }

            cards.revalidate();
            cards.repaint();
        };

        // =================================================
        // MAP CLICK
        // =================================================

        map.onPick = ShopDetailDialog::show;

        // =================================================
        // FIXED LOCATE BUTTON ACTION
        // =================================================

        locateButton.addActionListener(e -> {

            locateLaptop(
                    map,
                    locateButton,
                    location -> {

                        userLatitude[0] = location.latitude;
                        userLongitude[0] = location.longitude;

                        locationAvailable[0] = validCoordinates(
                                location.latitude,
                                location.longitude
                        );

                        fill.run();
                    }
            );

        });

        // =================================================
        // SEARCH FIELD STYLE
        // =================================================

        q.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        14
                )
        );

        q.setForeground(NAVY);
        q.setCaretColor(NAVY);
        q.setBackground(Color.WHITE);

        q.setBorder(
                new CompoundBorder(
                        new LineBorder(
                                GRADIENT_BORDER,
                                2,
                                true
                        ),
                        new EmptyBorder(6, 10, 6, 10)
                )
        );

        q.addActionListener(e -> fill.run());

        // =================================================
        // SEARCH BUTTON
        // =================================================

        GradientButton sb = new GradientButton("Search");

        sb.addActionListener(e -> fill.run());

        JPanel sr = new JPanel(new BorderLayout(10, 0));
        sr.setOpaque(false);

        sr.add(q, BorderLayout.CENTER);
        sr.add(sb, BorderLayout.EAST);

        // =================================================
        // TOP SEARCH AREA
        // =================================================

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.setOpaque(false);

        north.add(sr, BorderLayout.NORTH);

        north.setBorder(
                new EmptyBorder(0, 0, 8, 0)
        );

        // =================================================
        // MAP AREA
        // =================================================

        GradientPanel left =
                new GradientPanel(
                        new BorderLayout(0, 8),
                        GRADIENT_DARK,
                        GRADIENT_LIGHT,
                        16
                );

        left.setBorder(new EmptyBorder(8, 8, 8, 8));
        left.add(north, BorderLayout.NORTH);

        map.setBorder(
                new LineBorder(GRADIENT_BORDER, 2, true)
        );

        left.add(map, BorderLayout.CENTER);

        // =================================================
        // LOCATE BUTTON BELOW MAP
        // =================================================

        JPanel locatePanel =
                new JPanel(
                        new FlowLayout(FlowLayout.RIGHT, 0, 0)
                );

        locatePanel.setOpaque(false);
        locatePanel.add(locateButton);

        left.add(locatePanel, BorderLayout.SOUTH);

        // =================================================
        // SHOP LIST SCROLL PANE
        // =================================================

        JScrollPane sp = new JScrollPane(cards);

        sp.setBorder(
                new LineBorder(GRADIENT_BORDER, 2, true)
        );

        sp.setPreferredSize(new Dimension(320, 100));

        sp.getViewport().setBackground(GRADIENT_DARK);
        sp.getViewport().setOpaque(true);
        sp.setOpaque(false);

        sp.getVerticalScrollBar().setUnitIncrement(16);

        cards.setBorder(
                new EmptyBorder(10, 10, 10, 10)
        );

        // =================================================
        // RIGHT SIDE
        // =================================================

        GradientPanel east =
                new GradientPanel(
                        new BorderLayout(),
                        GRADIENT_DARK,
                        GRADIENT_MID,
                        16
                );

        east.setBorder(new EmptyBorder(8, 8, 8, 8));
        east.add(sp, BorderLayout.CENTER);

        // =================================================
        // ADD PANELS
        // =================================================

        body.add(left, BorderLayout.CENTER);
        body.add(east, BorderLayout.EAST);

        // =================================================
        // INITIAL DISPLAY
        // =================================================

        fill.run();

        // AUTOMATIC LOCATION DETECTION

        locateLaptop(
                map,
                locateButton,
                location -> {

                    userLatitude[0] = location.latitude;
                    userLongitude[0] = location.longitude;

                    locationAvailable[0] = validCoordinates(
                            location.latitude,
                            location.longitude
                    );

                    fill.run();
                }
        );

        return pg[0];
    }

    // =================================================
    // GET LAPTOP LOCATION
    // =================================================

    private static void locateLaptop(
            MapPanel map,
            JButton locateButton,
            java.util.function.Consumer<Location> onSuccess) {

        if (systemSafeSessionLocationDisabled()) {

            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(map),
                    "Location Sharing is turned OFF in your Preferences.\n"
                            + "Turn it ON to use location-based services.",
                    "Location Sharing Disabled",
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (locateButton != null) {
                locateButton.setEnabled(true);
            }

            return;
        }

        if (locateButton != null) {
            locateButton.setEnabled(false);
        }

        new SwingWorker<Location, Void>() {

            @Override
            protected Location doInBackground() throws Exception {
                return LocationService.getCurrentLocation();
            }

            @Override
            protected void done() {

                try {

                    Location location = get();

                    if (location == null
                            || !validCoordinates(
                                    location.latitude,
                                    location.longitude)) {

                        throw new IllegalStateException(
                                "The location returned is invalid."
                        );
                    }

                    map.setMyLocation(
                            location.latitude,
                            location.longitude
                    );

                    if (onSuccess != null) {
                        onSuccess.accept(location);
                    }

                } catch (Exception ex) {

                    Throwable cause =
                            ex.getCause() == null
                                    ? ex
                                    : ex.getCause();

                    String message = cause.getMessage();

                    JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(map),
                            "Unable to detect the laptop location.\n\n"
                                    + (message == null
                                            ? cause.getClass()
                                                    .getSimpleName()
                                            : message)
                                    + "\n\nCheck your internet connection and try again."
                                    + "\nNote: this version estimates location from the public IP address.",
                            "Location Unavailable",
                            JOptionPane.WARNING_MESSAGE
                    );

                } finally {

                    if (locateButton != null) {
                        locateButton.setEnabled(true);
                    }
                }
            }
        }.execute();
    }

    // =================================================
    // LOCATION SHARING CHECK
    // =================================================

    private static boolean systemSafeSessionLocationDisabled() {
        return Session.ME != null && !Session.ME.loc;
    }

    // =================================================
    // SHOP CARD
    // =================================================

    static JComponent shopCard(Shop shop) {
        return shopCard(shop, shop.dist);
    }

    static JComponent shopCard(
            Shop shop,
            double actualDistance) {

        GradientPanel c =
                new GradientPanel(
                        new BorderLayout(0, 8),
                        GRADIENT_DARK,
                        GRADIENT_LIGHT,
                        16
                );

        c.setBorder(
                new CompoundBorder(
                        new LineBorder(
                                GRADIENT_BORDER,
                                1,
                                true
                        ),
                        new EmptyBorder(12, 14, 12, 14)
                )
        );

        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));
        c.setPreferredSize(new Dimension(280, 125));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);

        // SHOP NAME

        JLabel name = lbl(
                shop.name == null ? "Repair Shop" : shop.name,
                15,
                true,
                Color.WHITE
        );

        name.setOpaque(false);

        // RATING AND DISTANCE

        JLabel info = lbl(
                "\u2605 "
                        + String.format("%.1f", shop.rating)
                        + "     "
                        + String.format("%.2f", actualDistance)
                        + " km",
                13,
                false,
                LIGHT_TEXT
        );

        info.setOpaque(false);

        // VIEW DETAILS

        GradientButton details =
                new GradientButton("View Details");

        details.setPreferredSize(new Dimension(140, 32));
        details.setMinimumSize(new Dimension(140, 32));

        details.addActionListener(e -> {

            try {

                ShopDetailDialog.show(shop);

            } catch (Exception ex) {

                ex.printStackTrace();

                JOptionPane.showMessageDialog(
                        c,
                        "Unable to open Shop Details:\n"
                                + ex.getMessage(),
                        "Shop Details Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        JPanel actionPanel =
                new JPanel(
                        new FlowLayout(FlowLayout.LEFT, 0, 0)
                );

        actionPanel.setOpaque(false);
        actionPanel.add(details);

        c.add(name, BorderLayout.NORTH);
        c.add(info, BorderLayout.CENTER);
        c.add(actionPanel, BorderLayout.SOUTH);

        return c;
    }

    // =================================================
    // FULL PAGE GRADIENT BACKGROUND
    // =================================================

    private static class PageGradientPanel extends JPanel {

        PageGradientPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            GradientPaint gradient =
                    new GradientPaint(
                            0,
                            0,
                            GRADIENT_DARK,
                            getWidth(),
                            getHeight(),
                            GRADIENT_LIGHT
                    );

            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.dispose();
        }
    }

    // =================================================
    // CUSTOM GRADIENT PANEL
    // =================================================

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

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            GradientPaint gradient =
                    new GradientPaint(
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
        }
    }

    // =================================================
    // CUSTOM GRADIENT BUTTON
    // =================================================

    private static class GradientButton extends JButton {

        GradientButton(String text) {

            super(text);

            setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            12
                    )
            );

            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            setPreferredSize(new Dimension(105, 36));

            setBorder(
                    new EmptyBorder(8, 16, 8, 16)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color topColor = GRADIENT_MID;
            Color bottomColor = GRADIENT_LIGHT;

            if (!isEnabled()) {

                topColor = new Color(55, 75, 100);
                bottomColor = new Color(35, 55, 80);

            } else if (getModel().isPressed()) {

                topColor = GRADIENT_DARK;
                bottomColor = GRADIENT_MID;

            } else if (getModel().isRollover()) {

                topColor = GRADIENT_LIGHT;
                bottomColor = new Color(25, 100, 165);
            }

            GradientPaint gradient =
                    new GradientPaint(
                            0,
                            0,
                            topColor,
                            0,
                            getHeight(),
                            bottomColor
                    );

            g2.setPaint(gradient);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    12,
                    12
            );

            g2.setColor(GRADIENT_BORDER);

            g2.drawRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    12,
                    12
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }
}

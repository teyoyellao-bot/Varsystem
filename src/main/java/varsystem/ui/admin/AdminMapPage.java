package varsystem.ui.admin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import varsystem.data.DB;
import varsystem.model.Shop;
import varsystem.ui.components.MapPanel;
import varsystem.ui.frame.MainFrame;

public final class AdminMapPage {

    private AdminMapPage() { }

    private static final Color NAVY = new Color(0x03, 0x19, 0x3E);
    private static final Color BLUE = new Color(0x16, 0x72, 0xFF);
    private static final Color TEXT = new Color(0x22, 0x2B, 0x3B);
    private static final Color MUTED = new Color(0x78, 0x85, 0x98);
    private static final Color BORDER = new Color(0xDF, 0xE5, 0xEE);
    private static final Color GREEN = new Color(0x16, 0xA0, 0x65);

    private static MapPanel map;
    private static JPanel shopList;
    private static JTextField searchField;

    private static List<Shop> locatedShops = new ArrayList<>();
    private static List<Shop> allShops = new ArrayList<>();

    public static JComponent build() {

        DB.loadLocations();

        allShops = DB.shops == null
                ? new ArrayList<>()
                : new ArrayList<>(DB.shops);

        locatedShops = new ArrayList<>();

        int withoutLocation = 0;

        for (Shop shop : allShops) {

            if (shop == null) continue;

            if (shop.hasLocation()) {
                locatedShops.add(shop);
            } else {
                withoutLocation++;
            }
        }

        JComponent[] page = MainFrame.page(
                "Shop Locations Map",
                "View the actual registered locations of all repair shops."
        );

        JPanel root = (JPanel) page[0];
        JPanel body = (JPanel) page[1];

        body.setLayout(new BorderLayout(14, 14));

        // STATISTICS

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);

        stats.add(statCard(
                "REGISTERED SHOPS",
                String.valueOf(allShops.size()),
                "All registered repair shops",
                BLUE
        ));

        stats.add(statCard(
                "SHOPS ON MAP",
                String.valueOf(locatedShops.size()),
                "With valid saved coordinates",
                GREEN
        ));

        stats.add(statCard(
                "NO LOCATION",
                String.valueOf(withoutLocation),
                "Need to set shop location",
                new Color(0xE0, 0x91, 0x28)
        ));

        body.add(stats, BorderLayout.NORTH);

        // MAP

        map = new MapPanel();
        map.setPreferredSize(new Dimension(650, 500));
        map.setRealLocationsOnly(true);
        map.setShops(locatedShops);

        // Clicking map marker opens large details window.
        map.onPick = shop -> showShop(shop);

        JPanel mapContainer = new JPanel(new BorderLayout());
        mapContainer.setBackground(Color.WHITE);
        mapContainer.setBorder(
                BorderFactory.createLineBorder(BORDER)
        );

        JLabel mapTitle = new JLabel("  Repair Shop Locations");
        mapTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        mapTitle.setForeground(TEXT);

        mapTitle.setBorder(
                BorderFactory.createEmptyBorder(12, 8, 12, 8)
        );

        mapContainer.add(mapTitle, BorderLayout.NORTH);
        mapContainer.add(map, BorderLayout.CENTER);

        // RIGHT PANEL

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setPreferredSize(new Dimension(320, 500));
        right.setMinimumSize(new Dimension(270, 300));
        right.setBackground(Color.WHITE);

        right.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(14, 14, 14, 14)
                )
        );

        JLabel listTitle = new JLabel("Registered Shops");
        listTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        listTitle.setForeground(TEXT);

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));

        searchField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(9, 10, 9, 10)
                )
        );

        searchField.setToolTipText("Search shop name or location");

        JPanel searchArea = new JPanel(new BorderLayout(0, 10));
        searchArea.setOpaque(false);

        searchArea.add(listTitle, BorderLayout.NORTH);
        searchArea.add(searchField, BorderLayout.CENTER);

        // SHOP LIST

        shopList = new JPanel();
        shopList.setLayout(new BoxLayout(shopList, BoxLayout.Y_AXIS));
        shopList.setBackground(Color.WHITE);
        shopList.setBorder(new EmptyBorder(3, 2, 3, 2));

        JScrollPane scroll = new JScrollPane(shopList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel listArea = new JPanel(new BorderLayout(0, 10));
        listArea.setOpaque(false);

        listArea.add(searchArea, BorderLayout.NORTH);
        listArea.add(scroll, BorderLayout.CENTER);

        right.add(listArea, BorderLayout.CENTER);

        // CENTER

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);

        center.add(mapContainer, BorderLayout.CENTER);
        center.add(right, BorderLayout.EAST);

        body.add(center, BorderLayout.CENTER);

        // SEARCH

        searchField.getDocument().addDocumentListener(
                new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterShops();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterShops();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterShops();
            }
        });

        refreshShopList();

        return root;
    }

    // STAT CARD

    private static JPanel statCard(
            String title,
            String value,
            String subtitle,
            Color accent) {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)
                )
        );

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 11));
        heading.setForeground(MUTED);

        JLabel number = new JLabel(value);
        number.setFont(new Font("SansSerif", Font.BOLD, 25));
        number.setForeground(accent);

        JLabel description = new JLabel(subtitle);
        description.setFont(new Font("SansSerif", Font.PLAIN, 11));
        description.setForeground(MUTED);

        content.add(heading);
        content.add(Box.createVerticalStrut(5));
        content.add(number);
        content.add(Box.createVerticalStrut(3));
        content.add(description);

        card.add(content, BorderLayout.CENTER);

        return card;
    }

    // SHOP LIST

    private static void refreshShopList() {

        if (shopList == null) return;

        shopList.removeAll();

        String query = searchField == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        boolean found = false;

        for (Shop shop : allShops) {

            if (shop == null) continue;

            boolean matches =
                    safe(shop.name).toLowerCase().contains(query)
                    || safe(shop.location).toLowerCase().contains(query);

            if (!matches) continue;

            found = true;

            shopList.add(shopRow(shop));
            shopList.add(Box.createVerticalStrut(7));
        }

        if (!found) {

            JLabel empty = new JLabel(
                    allShops.isEmpty()
                            ? "No registered shops."
                            : "No matching shop found."
            );

            empty.setFont(new Font("SansSerif", Font.PLAIN, 12));
            empty.setForeground(MUTED);

            empty.setBorder(
                    BorderFactory.createEmptyBorder(15, 5, 15, 5)
            );

            shopList.add(empty);
        }

        shopList.revalidate();
        shopList.repaint();
    }

    // CLICKABLE SHOP ROW

    private static JButton shopRow(Shop shop) {

        String name = shop.name == null
                ? "Unnamed Shop"
                : shop.name;

        String status = shop.status == null
                ? "Unknown"
                : shop.status;

        String location = shop.hasLocation()
                ? "Location available"
                : "No saved coordinates";

        Color statusColor = "Active".equalsIgnoreCase(shop.status)
                ? GREEN
                : MUTED;

        String html =
                "<html><div style='width:220px;'>"
                + "<b style='font-size:12px;'>"
                + escape(name)
                + "</b><br>"
                + "<span style='color:" + colorHex(statusColor) + ";'>"
                + escape(status)
                + "</span><br>"
                + "<span style='color:#788598;'>"
                + escape(location)
                + "</span>"
                + "</div></html>";

        JButton row = new JButton(html);

        row.setHorizontalAlignment(SwingConstants.LEFT);
        row.setVerticalAlignment(SwingConstants.CENTER);

        row.setFont(new Font("SansSerif", Font.PLAIN, 12));
        row.setForeground(TEXT);
        row.setBackground(new Color(0xF7, 0xF9, 0xFD));

        row.setOpaque(true);
        row.setContentAreaFilled(true);
        row.setFocusPainted(false);

        row.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(9, 10, 9, 10)
                )
        );

        row.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(270, 75));
        row.setMinimumSize(new Dimension(200, 75));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        row.addActionListener(e -> {

            if (shop.hasLocation() && map != null) {
                map.focusShop(shop);
            }

            showShop(shop);
        });

        return row;
    }

    private static void filterShops() {
        refreshShopList();
    }

    // LARGE SHOP DETAILS WINDOW

    private static void showShop(Shop shop) {

        if (shop == null) return;

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showShop(shop));
            return;
        }

        JDialog dialog = new JDialog(
                MainFrame.APP,
                "Shop Details",
                true
        );

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // HEADER

        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(NAVY);
        header.setBorder(
                new EmptyBorder(15, 22, 15, 16)
        );

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SHOP DETAILS");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
                "Complete registered repair shop information"
        );

        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0xCF, 0xE0, 0xFF));

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitle);

        JButton close = new JButton("×");
        close.setFont(new Font("SansSerif", Font.BOLD, 27));
        close.setForeground(Color.WHITE);
        close.setBackground(new Color(0xDC, 0x35, 0x45));
        close.setOpaque(true);
        close.setContentAreaFilled(true);
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setPreferredSize(new Dimension(42, 42));
        close.setToolTipText("Close");

        close.addActionListener(e -> dialog.dispose());

        header.add(headerText, BorderLayout.CENTER);
        header.add(close, BorderLayout.EAST);

        // CONTENT

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(22, 24, 24, 24));

        // SHOP NAME

        JLabel shopName = new JLabel(
                safe(shop.name)
        );

        shopName.setFont(new Font("SansSerif", Font.BOLD, 26));
        shopName.setForeground(NAVY);
        shopName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel shopSubtitle = new JLabel(
                "Registered Repair Shop"
        );

        shopSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        shopSubtitle.setForeground(MUTED);
        shopSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(shopName);
        content.add(Box.createVerticalStrut(4));
        content.add(shopSubtitle);
        content.add(Box.createVerticalStrut(20));

        // INFORMATION GRID

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 14));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        addInfoCard(
                grid,
                "users-round",
                "SHOP OWNER",
                shop.owner
        );

        addInfoCard(
                grid,
                "message-circle",
                "CONTACT NUMBER",
                shop.phone
        );

        addInfoCard(
                grid,
                "map-pin",
                "SHOP ADDRESS",
                shop.location
        );

        addInfoCard(
                grid,
                "calendar-clock",
                "OPERATING HOURS",
                shop.hours
        );

        addInfoCard(
                grid,
                "settings",
                "SHOP STATUS",
                shop.status
        );

        addInfoCard(
                grid,
                "chart-no-axes-combined",
                "SHOP RATING",
                shop.reviews > 0
                        ? String.format(
                                Locale.US,
                                "%.1f / 5  (%d reviews)",
                                shop.rating,
                                shop.reviews
                        )
                        : "No ratings yet"
        );

        addInfoCard(
                grid,
                "users-round",
                "SHOP VIEWS",
                String.valueOf(shop.views)
        );

        addInfoCard(
                grid,
                "map-pin",
                "DISTANCE",
                String.format(Locale.US, "%.2f km", shop.dist)
        );

        addInfoCard(
                grid,
                "map-pin",
                "GPS COORDINATES",
                shop.hasLocation()
                        ? String.format(
                                Locale.US,
                                "%.6f, %.6f",
                                shop.latitude,
                                shop.longitude
                        )
                        : "No saved coordinates"
        );

        content.add(grid);
        content.add(Box.createVerticalStrut(18));

        // SERVICES

        addWideSection(
                content,
                "wrench",
                "SERVICES OFFERED",
                servicesText(shop)
        );

        content.add(Box.createVerticalStrut(14));

        // ABOUT THE SHOP

        addWideSection(
                content,
                "store",
                "ABOUT THE SHOP",
                shop.about
        );

        // SCROLLABLE CONTENT

        JScrollPane scroll = new JScrollPane(content);

        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getViewport().setBackground(Color.WHITE);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);

        // ALWAYS START SCROLLBAR AT THE TOP

        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
            scroll.getViewport().setViewPosition(new Point(0, 0));
        });

        // SHOP DETAILS WINDOW SIZE AND POSITION

        dialog.setSize(900, 620);
        dialog.setMinimumSize(new Dimension(760, 520));

        // Position near the top-center of the screen
        dialog.setLocationRelativeTo(MainFrame.APP);

        Rectangle bounds = dialog.getGraphicsConfiguration()
                .getBounds();

        int x = bounds.x + (bounds.width - dialog.getWidth()) / 2;
        int y = bounds.y + 35;

        dialog.setLocation(x, y);

        dialog.setVisible(true);
    }

    // INFORMATION CARD

    private static void addInfoCard(
            JPanel parent,
            String iconName,
            String heading,
            String value) {

        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(new Color(0xF8, 0xFA, 0xFD));

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(14, 15, 14, 15)
                )
        );

        card.setPreferredSize(new Dimension(420, 100));
        card.setMinimumSize(new Dimension(250, 90));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel icon = new JLabel(
                new MainFrame.LucideIcon(
                        iconName,
                        19,
                        BLUE
                )
        );

        icon.setPreferredSize(new Dimension(24, 24));

        JLabel title = new JLabel(heading);
        title.setFont(new Font("SansSerif", Font.BOLD, 11));
        title.setForeground(MUTED);

        top.add(icon, BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);

        JTextArea valueText = new JTextArea(safe(value));
        valueText.setFont(new Font("SansSerif", Font.BOLD, 14));
        valueText.setForeground(TEXT);
        valueText.setOpaque(false);
        valueText.setEditable(false);
        valueText.setFocusable(false);
        valueText.setLineWrap(true);
        valueText.setWrapStyleWord(true);
        valueText.setBorder(null);

        card.add(top, BorderLayout.NORTH);
        card.add(valueText, BorderLayout.CENTER);

        parent.add(card);
    }

    // WIDE INFORMATION SECTION

    private static void addWideSection(
            JPanel parent,
            String iconName,
            String heading,
            String value) {

        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(16, 18, 16, 18)
                )
        );

        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        JPanel top = new JPanel(new BorderLayout(9, 0));
        top.setOpaque(false);

        JLabel icon = new JLabel(
                new MainFrame.LucideIcon(
                        iconName,
                        20,
                        BLUE
                )
        );

        icon.setPreferredSize(new Dimension(25, 25));

        JLabel title = new JLabel(heading);
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(NAVY);

        top.add(icon, BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);

        JTextArea text = new JTextArea(safe(value));
        text.setFont(new Font("SansSerif", Font.PLAIN, 14));
        text.setForeground(TEXT);
        text.setOpaque(false);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(null);
        text.setRows(3);

        card.add(top, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);

        parent.add(card);
    }

    // SERVICES TEXT

    private static String servicesText(Shop shop) {

        if (shop.services == null || shop.services.isEmpty()) {
            return "No services listed.";
        }

        String result = shop.services.stream()
                .filter(service -> service != null
                        && !service.trim().isEmpty())
                .collect(Collectors.joining("  •  "));

        return result.isEmpty()
                ? "No services listed."
                : result;
    }

    // HELPERS

    private static String safe(String value) {

        return value == null || value.trim().isEmpty()
                ? "Not provided"
                : value.trim();
    }

    private static String escape(String value) {

        if (value == null) return "";

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String colorHex(Color color) {

        return String.format(
                "#%02X%02X%02X",
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );
    }
}
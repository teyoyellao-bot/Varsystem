
package varsystem.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import varsystem.model.Shop;
import varsystem.model.Req;
import varsystem.data.Session;

import static varsystem.ui.theme.Theme.BLUE;

/** Live OpenStreetMap panel. */
public class MapPanel extends JPanel {

    public List<Shop> shops = new ArrayList<>();
    private List<Shop> allShops = new ArrayList<>();
    private boolean realLocationsOnly = false;

    public Shop sel;
    public Consumer<Shop> onPick;

    public Consumer<double[]> onLocationPick;
    private boolean pickingLocation;
    private double pickedLat = Double.NaN;
    private double pickedLon = Double.NaN;

    private List<Req> customerRequests = new ArrayList<>();
    private String authorizedShop;

    // Mechanic location
    private double myLat = Double.NaN;
    private double myLon = Double.NaN;

    // User / Vehicle Owner location
    private double userLat = Double.NaN;
    private double userLon = Double.NaN;

    // Mechanic-only Locate Requester button
    private boolean mechanicLocateEnabled = false;
    private JButton mechanicLocateButton;

    // Tracks the last located customer when multiple requests exist.
    private int locatedCustomerIndex = -1;

    private static final int LOCATE_ROW_HEIGHT = 48;

    static final double CLAT = 13.9436;
    static final double CLON = 120.7330;
    static final double SPAN = 0.06;

    static final int TS = 256;
    static final int MINZ = 3;
    static final int MAXZ = 19;

    double cLat = CLAT;
    double cLon = CLON;
    int zoom = 13;

    boolean needFit = true;
    boolean moved;
    boolean missing;
    Point drag;

    static final Map<String, Image> CACHE =
            new LinkedHashMap<String, Image>(512, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> e) {
            return size() > 400;
        }
    };

    static final Set<String> LOADING = new HashSet<>();
    static final Map<String, Long> FAILED = new HashMap<>();

    static final ExecutorService POOL =
            Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "map-tile-loader");
        t.setDaemon(true);
        return t;
    });

    final javax.swing.Timer retry =
            new javax.swing.Timer(6000, e -> {
        if (missing) repaint();
    });

    // ---------- LOCATION PRIVACY ----------

    private boolean isUserView() {
        return Session.ME != null
                && ("User".equalsIgnoreCase(Session.ME.role)
                || "Vehicle Owner".equalsIgnoreCase(Session.ME.role));
    }

    private boolean validLocation(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    // ---------- MECHANIC LOCATE BUTTON ----------

    /**
     * Enables the Locate Requester button only for the Mechanic map.
     * This button locates authorized customer requests, not the mechanic.
     */
    public void setMechanicLocateEnabled(boolean enabled) {

        mechanicLocateEnabled = enabled
                && Session.ME != null
                && "Mechanic".equalsIgnoreCase(Session.ME.role);

        if (mechanicLocateButton != null) {

            mechanicLocateButton.setVisible(mechanicLocateEnabled);

            mechanicLocateButton.setText(
                    getVisibleCustomers().isEmpty()
                            ? "No Active Request"
                            : "Locate Requester"
            );

            mechanicLocateButton.setEnabled(
                    mechanicLocateEnabled
                            && !getVisibleCustomers().isEmpty()
            );
        }

        updateMapLayout();
        revalidate();
        repaint();
    }

    private int mapHeight() {
        return mechanicLocateEnabled
                ? Math.max(1, getHeight() - LOCATE_ROW_HEIGHT)
                : getHeight();
    }

    private void updateMapLayout() {

        if (mechanicLocateButton == null) {
            return;
        }

        if (mechanicLocateEnabled) {

            mechanicLocateButton.setBounds(
                    8,
                    getHeight() - LOCATE_ROW_HEIGHT + 5,
                    Math.max(0, getWidth() - 16),
                    LOCATE_ROW_HEIGHT - 10
            );

            mechanicLocateButton.setVisible(true);

        } else {

            mechanicLocateButton.setBounds(0, 0, 0, 0);
            mechanicLocateButton.setVisible(false);
        }
    }

    /**
     * Locates the next authorized active requester.
     * If multiple customers exist, repeated clicks cycle through them.
     */
    private void locateMechanic() {

        if (!mechanicLocateEnabled) {
            return;
        }

        List<Req> customers = getVisibleCustomers();

        if (customers.isEmpty()) {

            locatedCustomerIndex = -1;

            if (mechanicLocateButton != null) {
                mechanicLocateButton.setText("No Active Request");
                mechanicLocateButton.setEnabled(false);
            }

            repaint();
            return;
        }

        locatedCustomerIndex++;

        if (locatedCustomerIndex >= customers.size()) {
            locatedCustomerIndex = 0;
        }

        Req customer = customers.get(locatedCustomerIndex);

        if (!validLocation(
                customer.userLatitude,
                customer.userLongitude)) {
            return;
        }

        // Focus the map on the selected customer.
        cLat = customer.userLatitude;
        cLon = customer.userLongitude;

        zoom = Math.max(zoom, 16);
        zoom = Math.min(zoom, MAXZ);

        sel = null;
        needFit = false;

        if (mechanicLocateButton != null) {

            String name = customer.uname == null
                    || customer.uname.isBlank()
                    ? "Customer"
                    : customer.uname;

            mechanicLocateButton.setText("Locate Requester  •  " + name);
        }

        repaint();
    }

    @Override
    public void doLayout() {
        updateMapLayout();
    }

    // ---------- GEO HELPERS ----------

    static double lat(Shop s) {
        return s.hasLocation()
                ? s.latitude
                : CLAT + (0.5 - s.y) * SPAN;
    }

    static double lon(Shop s) {
        return s.hasLocation()
                ? s.longitude
                : CLON + (s.x - 0.5) * SPAN;
    }

    static double size(int z) {
        return TS * (double) (1L << z);
    }

    static double wx(double lon, int z) {
        return (lon + 180) / 360 * size(z);
    }

    static double wy(double lat, int z) {
        double r = Math.toRadians(lat);
        return (1 - Math.log(
                Math.tan(r) + 1 / Math.cos(r)
        ) / Math.PI) / 2 * size(z);
    }

    static double lonOf(double x, int z) {
        return x / size(z) * 360 - 180;
    }

    static double latOf(double y, int z) {
        return Math.toDegrees(
                Math.atan(
                        Math.sinh(
                                Math.PI * (1 - 2 * y / size(z))
                        )
                )
        );
    }

    // ---------- SCREEN TO GEO ----------

    Point2D.Double toScreen(double la, double lo) {
        int mh = mapHeight();

        return new Point2D.Double(
                wx(lo, zoom) - wx(cLon, zoom) + getWidth() / 2.0,
                wy(la, zoom) - wy(cLat, zoom) + mh / 2.0
        );
    }

    void panBy(int dx, int dy) {
        double nx = wx(cLon, zoom) - dx;
        double ny = wy(cLat, zoom) - dy;

        cLon = Math.max(
                -179.9,
                Math.min(179.9, lonOf(nx, zoom))
        );

        cLat = Math.max(
                -84,
                Math.min(84, latOf(ny, zoom))
        );
    }

    void zoomAt(int dz, Point p) {
        int nz = Math.max(
                MINZ,
                Math.min(MAXZ, zoom + dz)
        );

        if (nz == zoom) return;

        int mh = mapHeight();

        double offX = p.x - getWidth() / 2.0;
        double offY = p.y - mh / 2.0;

        double pLon = lonOf(
                wx(cLon, zoom) + offX,
                zoom
        );

        double pLat = latOf(
                wy(cLat, zoom) + offY,
                zoom
        );

        zoom = nz;

        cLon = Math.max(
                -179.9,
                Math.min(
                        179.9,
                        lonOf(wx(pLon, zoom) - offX, zoom)
                )
        );

        cLat = Math.max(
                -84,
                Math.min(
                        84,
                        latOf(wy(pLat, zoom) - offY, zoom)
                )
        );

        repaint();
    }

    Point center() {
        return new Point(getWidth() / 2, mapHeight() / 2);
    }

    Rectangle zIn() {
        return new Rectangle(getWidth() - 46, 12, 34, 34);
    }

    Rectangle zOut() {
        return new Rectangle(getWidth() - 46, 46, 34, 34);
    }

    Rectangle loc() {
        return new Rectangle(getWidth() - 46, 92, 34, 34);
    }

    Shop shopAt(Point p) {
        Shop best = null;
        double bd = 22;

        for (Shop s : shops) {
            Point2D.Double q = toScreen(lat(s), lon(s));

            double d = Point2D.distance(
                    p.x,
                    p.y,
                    q.x,
                    q.y - 12
            );

            if (d < bd) {
                bd = d;
                best = s;
            }
        }

        return best;
    }

    // ---------- CONSTRUCTOR ----------

    public MapPanel() {
        setPreferredSize(new Dimension(560, 380));
        setBackground(new Color(0xE5, 0xE3, 0xDF));
        setLayout(null);

        mechanicLocateButton = new JButton("Locate Requester");
        mechanicLocateButton.setFont(
                new Font("SansSerif", Font.BOLD, 13)
        );
        mechanicLocateButton.setForeground(Color.WHITE);
        mechanicLocateButton.setBackground(BLUE);
        mechanicLocateButton.setFocusPainted(false);
        mechanicLocateButton.setBorderPainted(false);
        mechanicLocateButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        mechanicLocateButton.setVisible(false);
        mechanicLocateButton.setEnabled(false);
        mechanicLocateButton.addActionListener(e -> locateMechanic());
        add(mechanicLocateButton);

        MouseAdapter ma = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                drag = e.getPoint();
                moved = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (drag == null) return;

                int dx = e.getX() - drag.x;
                int dy = e.getY() - drag.y;

                if (!moved && Math.abs(dx) + Math.abs(dy) < 4) {
                    return;
                }

                moved = true;

                setCursor(Cursor.getPredefinedCursor(
                        Cursor.MOVE_CURSOR
                ));

                panBy(dx, dy);
                drag = e.getPoint();
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean hand =
                        shopAt(e.getPoint()) != null
                        || zIn().contains(e.getPoint())
                        || zOut().contains(e.getPoint())
                        || loc().contains(e.getPoint());

                setCursor(Cursor.getPredefinedCursor(
                        hand
                                ? Cursor.HAND_CURSOR
                                : Cursor.DEFAULT_CURSOR
                ));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                boolean wasDrag = moved;

                drag = null;
                moved = false;

                setCursor(Cursor.getDefaultCursor());

                if (wasDrag) return;

                Point p = e.getPoint();

                if (zIn().contains(p)) {
                    zoomAt(1, center());
                    return;
                }

                if (zOut().contains(p)) {
                    zoomAt(-1, center());
                    return;
                }

                // Center on current account's own location.
                if (loc().contains(p)) {

                    double targetLat = isUserView()
                            ? userLat
                            : myLat;

                    double targetLon = isUserView()
                            ? userLon
                            : myLon;

                    if (!validLocation(targetLat, targetLon)
                            || mechanicLocateEnabled) {
                        return;
                    }

                    cLat = targetLat;
                    cLon = targetLon;
                    zoom = Math.max(zoom, 14);
                    zoom = Math.min(zoom, MAXZ);

                    repaint();
                    return;
                }

                if (pickingLocation && onLocationPick != null) {
                    pickedLon = lonOf(
                            wx(cLon, zoom)
                                    + p.x - getWidth() / 2.0,
                            zoom
                    );

                    pickedLat = latOf(
                            wy(cLat, zoom)
                                    + p.y - mapHeight() / 2.0,
                            zoom
                    );

                    onLocationPick.accept(
                            new double[]{pickedLat, pickedLon}
                    );

                    repaint();
                    return;
                }

                Shop best = shopAt(p);

                if (best != null) {
                    sel = best;
                    repaint();

                    if (onPick != null) {
                        onPick.accept(best);
                    }
                } else if (e.getClickCount() == 2) {
                    zoomAt(1, p);
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomAt(
                        e.getWheelRotation() < 0 ? 1 : -1,
                        e.getPoint()
                );
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        retry.start();
    }

    @Override
    public void removeNotify() {
        retry.stop();
        super.removeNotify();
    }

    // ---------- SHOP FILTERING ----------

    public void setShops(List<Shop> l) {
        allShops = l == null
                ? new ArrayList<>()
                : new ArrayList<>(l);

        applyShopFilter();
    }

    public void setRealLocationsOnly(boolean enabled) {
        realLocationsOnly = enabled;
        applyShopFilter();
    }

    private void applyShopFilter() {
        shops = new ArrayList<>();

        for (Shop shop : allShops) {
            if (shop == null) continue;

            if (realLocationsOnly && !shop.hasLocation()) {
                continue;
            }

            shops.add(shop);
        }

        sel = null;
        needFit = true;
        repaint();
    }

    public void focusShop(Shop shop) {
        if (shop == null || !shop.hasLocation()) {
            return;
        }

        if (!shops.contains(shop)) {
            return;
        }

        sel = shop;
        cLat = shop.latitude;
        cLon = shop.longitude;

        zoom = Math.max(zoom, 15);
        zoom = Math.min(zoom, MAXZ);

        needFit = false;
        repaint();
    }

    // ---------- LOCATION PICKER ----------

    public void setLocationPicking(
            boolean enabled,
            Consumer<double[]> callback) {

        pickingLocation = enabled;
        onLocationPick = enabled ? callback : null;

        repaint();
    }

    // ---------- USER LOCATION ----------

    public void setUserLocation(
            double latitude,
            double longitude) {

        if (!validLocation(latitude, longitude)) {
            return;
        }

        boolean firstLocation =
                !Double.isFinite(userLat)
                || !Double.isFinite(userLon);

        userLat = latitude;
        userLon = longitude;

        if (firstLocation) {
            needFit = true;
        }

        repaint();
    }

    // ---------- MECHANIC LOCATION ----------

    public void setMyLocation(
            double latitude,
            double longitude) {

        if (!validLocation(latitude, longitude)) {
            return;
        }

        if (isUserView()) {
            setUserLocation(latitude, longitude);
            return;
        }

        boolean firstLocation =
                !Double.isFinite(myLat)
                || !Double.isFinite(myLon);

        myLat = latitude;
        myLon = longitude;

        if (firstLocation) {
            needFit = true;
        }

        repaint();
    }

    // ---------- CUSTOMER LOCATION SHARING ----------

    public void setCustomerRequests(
            List<Req> requests,
            String shopName) {

        authorizedShop = shopName;
        customerRequests = new ArrayList<>();
        locatedCustomerIndex = -1;

        if (requests != null && shopName != null) {

            for (Req r : requests) {

                if (r == null) {
                    continue;
                }

                r.enforceLocationPrivacy();

                if (!r.isActive()
                        || !r.canShareLocationWith(shopName)) {
                    continue;
                }

                if (!validLocation(
                        r.userLatitude,
                        r.userLongitude)) {
                    continue;
                }

                customerRequests.add(r);
            }
        }

        if (mechanicLocateButton != null
                && mechanicLocateEnabled) {

            boolean hasCustomers = !getVisibleCustomers().isEmpty();

            mechanicLocateButton.setEnabled(hasCustomers);

            mechanicLocateButton.setText(
                    hasCustomers
                            ? "Locate Requester"
                            : "No Active Request"
            );
        }

        needFit = true;
        repaint();
    }

    /**
     * Returns only customer requests that are allowed to be shown
     * to the currently logged-in mechanic.
     */
    private List<Req> getVisibleCustomers() {

        List<Req> visible = new ArrayList<>();

        if (isUserView()
                || authorizedShop == null
                || Session.ME == null
                || !"Mechanic".equalsIgnoreCase(Session.ME.role)) {
            return visible;
        }

        for (Req r : customerRequests) {

            if (r == null) {
                continue;
            }

            r.enforceLocationPrivacy();

            if (!r.isActive()
                    || !r.canShareLocationWith(authorizedShop)) {
                continue;
            }

            if (!validLocation(
                    r.userLatitude,
                    r.userLongitude)) {
                continue;
            }

            visible.add(r);
        }

        return visible;
    }

    // ---------- FIT MAP ----------

    void fit() {
        int w = getWidth();
        int h = mapHeight();

        if (w <= 0 || h <= 0) return;

        needFit = false;

        boolean hasPoints = false;

        double minLa = 90;
        double maxLa = -90;
        double minLo = 180;
        double maxLo = -180;

        int pointCount = 0;

        // Shops
        for (Shop s : shops) {
            if (s == null) continue;

            hasPoints = true;
            pointCount++;

            minLa = Math.min(minLa, lat(s));
            maxLa = Math.max(maxLa, lat(s));

            minLo = Math.min(minLo, lon(s));
            maxLo = Math.max(maxLo, lon(s));
        }

        // User's own location
        if (isUserView()
                && validLocation(userLat, userLon)) {

            hasPoints = true;
            pointCount++;

            minLa = Math.min(minLa, userLat);
            maxLa = Math.max(maxLa, userLat);

            minLo = Math.min(minLo, userLon);
            maxLo = Math.max(maxLo, userLon);
        }

        // Mechanic location
        if (!isUserView()
                && validLocation(myLat, myLon)) {

            hasPoints = true;
            pointCount++;

            minLa = Math.min(minLa, myLat);
            maxLa = Math.max(maxLa, myLat);

            minLo = Math.min(minLo, myLon);
            maxLo = Math.max(maxLo, myLon);
        }

        // Authorized active customer locations (Mechanic view only)
        List<Req> visibleCustomers = getVisibleCustomers();

        for (Req r : visibleCustomers) {

            hasPoints = true;
            pointCount++;

            minLa = Math.min(minLa, r.userLatitude);
            maxLa = Math.max(maxLa, r.userLatitude);

            minLo = Math.min(minLo, r.userLongitude);
            maxLo = Math.max(maxLo, r.userLongitude);
        }

        if (!hasPoints) {
            cLat = CLAT;
            cLon = CLON;
            zoom = 13;
            return;
        }

        cLat = (minLa + maxLa) / 2;
        cLon = (minLo + maxLo) / 2;

        int z = 17;

        if (pointCount > 1) {
            for (; z > MINZ; z--) {
                double pw = Math.abs(
                        wx(maxLo, z) - wx(minLo, z)
                );

                double ph = Math.abs(
                        wy(minLa, z) - wy(maxLa, z)
                );

                if (pw <= w - 140 && ph <= h - 140) {
                    break;
                }
            }
        }

        zoom = Math.min(
                z,
                pointCount == 1 ? 15 : 16
        );
    }

    // ---------- MAP TILES ----------

    boolean failedRecently(String key) {
        synchronized (CACHE) {
            Long f = FAILED.get(key);

            return f != null
                    && System.currentTimeMillis() - f < 5000;
        }
    }

    Image tile(int z, int x, int y) {
        final String key = z + "/" + x + "/" + y;

        synchronized (CACHE) {
            Image im = CACHE.get(key);

            if (im != null) return im;

            Long f = FAILED.get(key);

            if (f != null
                    && System.currentTimeMillis() - f < 5000) {
                return null;
            }

            if (!LOADING.add(key)) {
                return null;
            }
        }

        POOL.execute(() -> {
            Image img = null;

            try {
                String url =
                        "https://tile.openstreetmap.org/"
                                + key + ".png";

                HttpURLConnection c = (HttpURLConnection)
                        URI.create(url).toURL().openConnection();

                c.setRequestProperty(
                        "User-Agent",
                        "VARSystem/1.0 (Java Swing desktop app)"
                );

                c.setConnectTimeout(6000);
                c.setReadTimeout(8000);

                try (InputStream in = c.getInputStream()) {
                    img = ImageIO.read(in);
                }

            } catch (java.io.IOException
                    | IllegalArgumentException ex) {
                // Failed tiles will be retried later.
            }

            synchronized (CACHE) {
                LOADING.remove(key);

                if (img != null) {
                    CACHE.put(key, img);
                    FAILED.remove(key);
                } else {
                    FAILED.put(key, System.currentTimeMillis());
                }
            }

            SwingUtilities.invokeLater(this::repaint);
        });

        return null;
    }

    // ---------- PAINTING ----------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        int w = getWidth();
        int h = mapHeight();

        if (needFit) fit();

        g.setClip(
                new RoundRectangle2D.Double(
                        0, 0, w, h, 24, 24
                )
        );

        g.setColor(getBackground());
        g.fillRect(0, 0, w, h);

        // MAP TILES
        double left = wx(cLon, zoom) - w / 2.0;
        double top = wy(cLat, zoom) - h / 2.0;

        int n = 1 << zoom;

        int tx0 = (int) Math.floor(left / TS);
        int tx1 = (int) Math.floor((left + w) / TS);

        int ty0 = (int) Math.floor(top / TS);
        int ty1 = (int) Math.floor((top + h) / TS);

        boolean miss = false;

        for (int tx = tx0; tx <= tx1; tx++) {
            for (int ty = ty0; ty <= ty1; ty++) {

                if (ty < 0 || ty >= n) continue;

                int wxT = ((tx % n) + n) % n;

                int px = (int) Math.round(
                        tx * (double) TS - left
                );

                int py = (int) Math.round(
                        ty * (double) TS - top
                );

                Image im = tile(zoom, wxT, ty);

                if (im != null) {
                    g.drawImage(im, px, py, TS, TS, null);
                } else {
                    g.setColor(new Color(0xEC, 0xEA, 0xE6));
                    g.fillRect(px, py, TS, TS);

                    g.setColor(new Color(0xD8, 0xD5, 0xCF));
                    g.drawRect(px, py, TS, TS);

                    if (failedRecently(
                            zoom + "/" + wxT + "/" + ty
                    )) {
                        miss = true;
                    }
                }
            }
        }

        missing = miss;

        if (miss) {
            String m =
                    "Can't load map tiles. Check your internet connection.";

            g.setFont(new Font(
                    "SansSerif",
                    Font.BOLD,
                    12
            ));

            FontMetrics fm = g.getFontMetrics();
            int bw = fm.stringWidth(m) + 24;

            g.setColor(new Color(255, 255, 255, 230));

            g.fillRoundRect(
                    (w - bw) / 2,
                    h / 2 - 16,
                    bw,
                    32,
                    12,
                    12
            );

            g.setColor(new Color(0xB0, 0x2A, 0x37));

            g.drawString(
                    m,
                    (w - bw) / 2 + 12,
                    h / 2 + 5
            );
        }

        // ---------- USER LOCATION ----------

        Point2D.Double userPoint = null;

        if (isUserView()
                && validLocation(userLat, userLon)) {

            userPoint = toScreen(userLat, userLon);
        }

        // ---------- MECHANIC LOCATION ----------

        Point2D.Double mechanicPoint = null;

        if (!isUserView()
                && validLocation(myLat, myLon)) {

            mechanicPoint = toScreen(myLat, myLon);
        }

        // ---------- CUSTOMER LOCATIONS ----------

        // Customer markers are visible only in authorized Mechanic view.
        List<Req> visibleCustomers = getVisibleCustomers();

        // ---------- ROUTE LINE ----------

        if (!isUserView()
                && mechanicPoint != null
                && !visibleCustomers.isEmpty()) {

            for (Req r : visibleCustomers) {

                Point2D.Double customerPoint = toScreen(
                        r.userLatitude,
                        r.userLongitude
                );

                g.setColor(new Color(255, 255, 255, 220));
                g.setStroke(new BasicStroke(
                        7f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                ));

                g.draw(new Line2D.Double(
                        mechanicPoint,
                        customerPoint
                ));

                g.setColor(new Color(0x42, 0x85, 0xF4));
                g.setStroke(new BasicStroke(
                        4f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                ));

                g.draw(new Line2D.Double(
                        mechanicPoint,
                        customerPoint
                ));
            }
        }

        // ---------- USER BLUE DOT ----------

        if (isUserView() && userPoint != null) {

            Point2D.Double u = userPoint;

            g.setColor(new Color(0x42, 0x85, 0xF4, 65));

            g.fill(new Ellipse2D.Double(
                    u.x - 22,
                    u.y - 22,
                    44,
                    44
            ));

            g.setColor(Color.WHITE);

            g.fill(new Ellipse2D.Double(
                    u.x - 11,
                    u.y - 11,
                    22,
                    22
            ));

            g.setColor(new Color(0x42, 0x85, 0xF4));

            g.fill(new Ellipse2D.Double(
                    u.x - 7,
                    u.y - 7,
                    14,
                    14
            ));

            g.setFont(new Font(
                    "SansSerif",
                    Font.BOLD,
                    11
            ));

            halo(
                    g,
                    "You",
                    (int) u.x + 14,
                    (int) u.y + 4,
                    new Color(0x16, 0x4D, 0xA5)
            );
        }

        // ---------- MECHANIC MARKER ----------

        if (!isUserView() && mechanicPoint != null) {

            Point2D.Double u = mechanicPoint;

            g.setColor(new Color(0x42, 0x85, 0xF4, 65));

            g.fill(new Ellipse2D.Double(
                    u.x - 22,
                    u.y - 22,
                    44,
                    44
            ));

            g.setColor(Color.WHITE);

            g.fill(new Ellipse2D.Double(
                    u.x - 11,
                    u.y - 11,
                    22,
                    22
            ));

            g.setColor(new Color(0x42, 0x85, 0xF4));

            g.fill(new Ellipse2D.Double(
                    u.x - 7,
                    u.y - 7,
                    14,
                    14
            ));

            g.setFont(new Font(
                    "SansSerif",
                    Font.BOLD,
                    11
            ));

            halo(
                    g,
                    "Mechanic - You",
                    (int) u.x + 14,
                    (int) u.y + 4,
                    new Color(0x16, 0x4D, 0xA5)
            );
        }

        // ---------- CUSTOMER MARKERS ----------

        // This section runs only for authorized mechanic customer requests.
        if (!isUserView()) {

            for (Req r : visibleCustomers) {

                Point2D.Double u = toScreen(
                        r.userLatitude,
                        r.userLongitude
                );

                g.setColor(new Color(0xFF, 0x98, 0x00));

                g.fillOval(
                        (int) u.x - 9,
                        (int) u.y - 9,
                        18,
                        18
                );

                g.setColor(Color.WHITE);

                g.fillOval(
                        (int) u.x - 4,
                        (int) u.y - 4,
                        8,
                        8
                );

                g.setFont(new Font(
                        "SansSerif",
                        Font.BOLD,
                        11
                ));

                halo(
                        g,
                        (r.uname == null || r.uname.isBlank()
                                ? "Customer"
                                : r.uname),
                        (int) u.x + 12,
                        (int) u.y,
                        new Color(0xA0, 0x5C, 0x00)
                );
            }
        }

        // ---------- SELECTED LOCATION MARKER ----------

        if (Double.isFinite(pickedLat)
                && Double.isFinite(pickedLon)) {

            Point2D.Double pt = toScreen(
                    pickedLat,
                    pickedLon
            );

            g.setColor(new Color(0x15, 0x88, 0x47));

            g.fillOval(
                    (int) pt.x - 8,
                    (int) pt.y - 8,
                    16,
                    16
            );

            g.setFont(new Font(
                    "SansSerif",
                    Font.BOLD,
                    11
            ));

            halo(
                    g,
                    "Selected location",
                    (int) pt.x + 12,
                    (int) pt.y,
                    Color.BLACK
            );
        }

        // ---------- SHOP PINS ----------

        List<Shop> order = new ArrayList<>(shops);

        order.sort(
                Comparator.comparingDouble(
                        (Shop s) -> s == sel ? 1 : 0
                ).thenComparingDouble(s -> lat(s) * -1)
        );

        for (Shop s : order) {

            Point2D.Double p = toScreen(
                    lat(s),
                    lon(s)
            );

            int x = (int) p.x;
            int y = (int) p.y;

            if (x < -40 || x > w + 40
                    || y < -40 || y > h + 60) {
                continue;
            }

            boolean on = s == sel;

            g.setColor(new Color(0, 0, 0, 50));

            g.fillOval(
                    x - 6,
                    y - 3,
                    12,
                    6
            );

            g.setColor(
                    on ? BLUE : new Color(0xC6, 0x28, 0x28)
            );

            int r = on ? 11 : 8;

            g.fillOval(
                    x - r,
                    y - 24 - (on ? 4 : 0),
                    r * 2,
                    r * 2
            );

            g.fillPolygon(
                    new int[]{x - r + 2, x + r - 2, x},
                    new int[]{y - 12, y - 12, y},
                    3
            );

            g.setColor(Color.WHITE);

            g.fillOval(
                    x - 3,
                    y - 20 - (on ? 4 : 0) + (on ? 3 : 0),
                    6,
                    6
            );

            g.setFont(new Font(
                    "SansSerif",
                    Font.BOLD,
                    11
            ));

            FontMetrics fm = g.getFontMetrics();

            halo(
                    g,
                    s.name,
                    x - fm.stringWidth(s.name) / 2,
                    y + 15,
                    new Color(0x22, 0x2B, 0x3B)
            );
        }

        // ---------- SCALE BAR ----------

        double mpp =
                156543.03392
                * Math.cos(Math.toRadians(cLat))
                / (1L << zoom);

        int[] nice = {
                5, 10, 20, 50, 100, 200, 500,
                1000, 2000, 5000, 10000, 20000,
                50000, 100000, 200000, 500000, 1000000
        };

        int best = nice[0];

        for (int m : nice) {
            if (m / mpp <= 110) {
                best = m;
            }
        }

        int len = (int) (best / mpp);

        String lab = best >= 1000
                ? (best / 1000) + " km"
                : best + " m";

        g.setFont(new Font(
                "SansSerif",
                Font.PLAIN,
                10
        ));

        FontMetrics sf = g.getFontMetrics();

        g.setColor(new Color(255, 255, 255, 200));

        g.fillRoundRect(
                8,
                h - 30,
                len + sf.stringWidth(lab) + 22,
                20,
                8,
                8
        );

        g.setColor(new Color(0x33, 0x33, 0x33));
        g.setStroke(new BasicStroke(2f));

        g.drawLine(
                14,
                h - 15,
                14 + len,
                h - 15
        );

        g.drawLine(
                14,
                h - 19,
                14,
                h - 15
        );

        g.drawLine(
                14 + len,
                h - 19,
                14 + len,
                h - 15
        );

        g.drawString(
                lab,
                14 + len + 6,
                h - 16
        );

        // ---------- OPENSTREETMAP ATTRIBUTION ----------

        String at = "\u00A9 OpenStreetMap contributors";

        g.setFont(new Font(
                "SansSerif",
                Font.PLAIN,
                10
        ));

        FontMetrics af = g.getFontMetrics();
        int aw = af.stringWidth(at) + 10;

        g.setColor(new Color(255, 255, 255, 200));

        g.fillRoundRect(
                w - aw - 6,
                h - 22,
                aw,
                16,
                6,
                6
        );

        g.setColor(new Color(0x33, 0x33, 0x33));

        g.drawString(
                at,
                w - aw - 1,
                h - 10
        );

        // ---------- MAP CONTROLS ----------

        button(g, zIn(), "+");
        button(g, zOut(), "\u2212");

        // Locate control inside map remains for User/Vehicle Owner.
        // Mechanic uses the separate Locate Requester button below.
        if (!mechanicLocateEnabled) {
            Rectangle lr = loc();

            button(g, lr, "");

            g.setColor(new Color(0x42, 0x85, 0xF4));
            g.setStroke(new BasicStroke(2f));

            int cx = lr.x + lr.width / 2;
            int cy = lr.y + lr.height / 2;

            g.drawOval(
                    cx - 6,
                    cy - 6,
                    12,
                    12
            );

            g.fillOval(
                    cx - 2,
                    cy - 2,
                    4,
                    4
            );

            g.drawLine(cx, cy - 10, cx, cy - 6);
            g.drawLine(cx, cy + 6, cx, cy + 10);
            g.drawLine(cx - 10, cy, cx - 6, cy);
            g.drawLine(cx + 6, cy, cx + 10, cy);
        }

        g.setClip(null);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f));

        g.drawRoundRect(
                1,
                1,
                w - 3,
                h - 3,
                24,
                24
        );

        g.dispose();
    }

    // ---------- DRAWING HELPERS ----------

    void button(Graphics2D g, Rectangle r, String t) {
        g.setColor(new Color(0, 0, 0, 40));

        g.fillRoundRect(
                r.x,
                r.y + 1,
                r.width,
                r.height,
                8,
                8
        );

        g.setColor(Color.WHITE);

        g.fillRoundRect(
                r.x,
                r.y,
                r.width,
            r.height,
                8,
                8
        );

        g.setColor(new Color(0xCC, 0xCC, 0xCC));

        g.drawRoundRect(
                r.x,
                r.y,
                r.width,
                r.height,
                8,
                8
        );

        if (!t.isEmpty()) {
            g.setColor(new Color(0x44, 0x44, 0x44));

            g.setFont(new Font(
                    "SansSerif",
                    Font.BOLD,
                    20
            ));

            FontMetrics fm = g.getFontMetrics();

            g.drawString(
                    t,
                    r.x + (r.width - fm.stringWidth(t)) / 2,
                    r.y + (r.height + fm.getAscent()) / 2 - 3
            );
        }
    }

    void halo(
            Graphics2D g,
            String s,
            int x,
            int y,
            Color c) {

        FontMetrics fm = g.getFontMetrics();

        g.setColor(new Color(255, 255, 255, 215));

        g.fillRoundRect(
                x - 4,
                y - fm.getAscent() - 1,
                fm.stringWidth(s) + 8,
                fm.getHeight() + 2,
                8,
                8
        );

        g.setColor(c);
        g.drawString(s, x, y);
    }
}

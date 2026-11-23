
package varsystem.ui.common;

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
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import varsystem.data.DB;
import static varsystem.data.Session.ME;
import static varsystem.data.Session.MYSHOP;
import varsystem.model.Req;
import varsystem.service.PasswordSecurity;
import static varsystem.ui.components.Dialogs.err;
import static varsystem.ui.components.Dialogs.form;
import static varsystem.ui.components.Dialogs.msg;
import varsystem.ui.components.MapPanel;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.flow;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.APP;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.NAVY;
import static varsystem.util.Formats.DD;

public final class ProfilePage {

    private ProfilePage() { }

    // ==========================================
    // DARK BLUE GRADIENT COLORS
    // ==========================================

    private static final Color GRADIENT_DARK =
            new Color(0x00, 0x16, 0x38);

    private static final Color GRADIENT_MID =
            new Color(0x08, 0x2D, 0x60);

    private static final Color GRADIENT_LIGHT =
            new Color(0x10, 0x4B, 0x85);

    private static final Color LIGHT_TEXT =
            new Color(0xE2, 0xEE, 0xFF);

    private static final Color MUTED_TEXT =
            new Color(0xC1, 0xD8, 0xF0);

    private static final Color GRADIENT_BORDER =
            new Color(0x3A, 0x79, 0xB8);

    private static final Color PRICE_BLUE =
            new Color(0x65, 0xB7, 0xFF);

    // ==========================================
    // CUSTOM GRADIENT PANEL
    // ==========================================

    private static class GradientPanel extends JPanel {

        private final Color start;
        private final Color end;
        private final int arc;

        GradientPanel(
                LayoutManager layout,
                Color start,
                Color end,
                int arc) {

            super(layout);

            this.start = start;
            this.end = end;
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

            GradientPaint gradient = new GradientPaint(
                    0, 0, start,
                    getWidth(), getHeight(), end
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

    // ==========================================
    // CIRCULAR AVATAR
    // ==========================================

    private static class CircularAvatar extends JComponent {

        private Image avatarImage;
        private String initial = "?";

        CircularAvatar() {

            setPreferredSize(new Dimension(110, 110));
            setMinimumSize(new Dimension(110, 110));
            setMaximumSize(new Dimension(110, 110));

            setOpaque(false);
        }

        void setAvatarImage(Image image) {
            this.avatarImage = image;
            repaint();
        }

        void setInitial(String name) {

            if (name == null || name.trim().isEmpty()) {
                initial = "?";
            } else {
                initial = name.trim()
                        .substring(0, 1)
                        .toUpperCase();
            }

            avatarImage = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            int size = Math.min(getWidth(), getHeight());

            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            Ellipse2D circle = new Ellipse2D.Double(
                    x + 2,
                    y + 2,
                    size - 4,
                    size - 4
            );

            g2.setClip(circle);

            g2.setPaint(
                    new GradientPaint(
                            x,
                            y,
                            GRADIENT_DARK,
                            x + size,
                            y + size,
                            GRADIENT_LIGHT
                    )
            );

            g2.fill(circle);

            if (avatarImage != null) {

                int imageWidth = avatarImage.getWidth(this);
                int imageHeight = avatarImage.getHeight(this);

                if (imageWidth > 0 && imageHeight > 0) {

                    double scale = Math.max(
                            (double) size / imageWidth,
                            (double) size / imageHeight
                    );

                    int drawWidth = (int) Math.ceil(
                            imageWidth * scale
                    );

                    int drawHeight = (int) Math.ceil(
                            imageHeight * scale
                    );

                    int drawX = x + (size - drawWidth) / 2;
                    int drawY = y + (size - drawHeight) / 2;

                    g2.drawImage(
                            avatarImage,
                            drawX,
                            drawY,
                            drawWidth,
                            drawHeight,
                            this
                    );

                } else {
                    drawInitial(g2, x, y, size);
                }

            } else {
                drawInitial(g2, x, y, size);
            }

            g2.dispose();

            Graphics2D border = (Graphics2D) g.create();

            border.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            border.setColor(GRADIENT_BORDER);
            border.setStroke(new BasicStroke(3f));

            border.draw(
                    new Ellipse2D.Double(
                            x + 2,
                            y + 2,
                            size - 5,
                            size - 5
                    )
            );

            border.dispose();
        }

        private void drawInitial(
                Graphics2D g2,
                int x,
                int y,
                int size) {

            g2.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            48
                    )
            );

            g2.setColor(Color.WHITE);

            FontMetrics fm = g2.getFontMetrics();

            int textX = x + (size - fm.stringWidth(initial)) / 2;

            int textY = y + (size - fm.getHeight()) / 2
                    + fm.getAscent();

            g2.drawString(initial, textX, textY);
        }
    }

    // ==========================================
    // PROFILE PAGE
    // ==========================================

    public static JComponent build() {

        if (ME == null) {
            return new JPanel();
        }

        boolean shop =
                "Shop Manager".equalsIgnoreCase(ME.role);

        String accountName = safe(ME.name);

        JComponent[] pg = page(
                shop
                        ? MYSHOP != null
                            ? safe(MYSHOP.name)
                            : "Shop Profile"
                        : accountName,
                safe(ME.email)
                        + "   |   View and manage your information"
        );

        JPanel body = (JPanel) pg[1];

        body.setOpaque(false);
        body.setBackground(new Color(0, 0, 0, 0));

        String joinedDate = ME.joined == null
                ? "-"
                : ME.joined.format(DD);

        String[] labels = shop
                ? new String[]{
                    "Full Name",
                    "Location",
                    "Email Address",
                    "Shop Name",
                    "Phone Number",
                    "Business Hours",
                    "Member Since"
                }
                : new String[]{
                    "Full Name",
                    "Location",
                    "Email Address",
                    "Vehicle",
                    "Phone Number",
                    "License / ID",
                    "Member Since"
                };

        String[] vals = shop
                ? new String[]{
                    safe(ME.name),
                    safe(ME.location),
                    safe(ME.email),
                    safe(ME.shop),
                    safe(ME.phone),
                    MYSHOP != null
                            ? safe(MYSHOP.hours)
                            : "",
                    joinedDate
                }
                : new String[]{
                    safe(ME.name),
                    safe(ME.location),
                    safe(ME.email),
                    safe(ME.vehicle),
                    safe(ME.phone),
                    getLicenseStatus(),
                    joinedDate
                };

        JTextField[] fs = new JTextField[labels.length];

        JPanel form = new JPanel(
                new GridLayout(0, 2, 16, 8)
        );

        form.setOpaque(false);

        for (int i = 0; i < labels.length; i++) {

            JPanel c = vbox();
            c.setOpaque(false);

            JLabel fieldLabel = lbl(
                    labels[i],
                    12,
                    true,
                    LIGHT_TEXT
            );

            c.add(fieldLabel);

            fs[i] = new JTextField(vals[i]);

            fs[i].setEditable(false);
            fs[i].setFont(
                    new Font("SansSerif", Font.PLAIN, 14)
            );

            fs[i].setPreferredSize(
                    new Dimension(220, 38)
            );

            fs[i].setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 38)
            );

            fs[i].setForeground(LIGHT_TEXT);
            fs[i].setBackground(GRADIENT_MID);
            fs[i].setCaretColor(Color.WHITE);
            fs[i].setSelectionColor(
                    new Color(40, 110, 190)
            );
            fs[i].setSelectedTextColor(Color.WHITE);
            fs[i].setOpaque(true);

            fs[i].setBorder(
                    new CompoundBorder(
                            new LineBorder(
                                    GRADIENT_BORDER,
                                    1
                            ),
                            new EmptyBorder(2, 8, 2, 8)
                    )
            );

            c.add(fs[i]);
            form.add(c);
        }

        fs[2].setEditable(false);
        fs[6].setEditable(false);

        // ==========================================
        // PROFILE EDIT BUTTON
        // ==========================================

        JButton edit = btn(
                "Edit Profile",
                BLUE,
                null
        );

        JLabel licenseStatus =
                new JLabel(getLicenseStatus());

        JButton viewLicense = btn(
                "View License ID",
                NAVY,
                e -> showDriverLicense()
        );

        JButton uploadLicense = btn(
                "Upload / Change License",
                BLUE,
                e -> uploadDriverLicense(
                        licenseStatus,
                        viewLicense
                )
        );

        uploadLicense.setEnabled(false);
        viewLicense.setEnabled(isLicenseUploaded());

        edit.addActionListener(e -> {

            boolean saving =
                    edit.getText().equals("Save Changes");

            if (!saving) {

                for (int i = 0; i < fs.length; i++) {

                    if (i != 2 && i != 6
                            && (shop || i != 5)) {

                        fs[i].setEditable(true);
                    }
                }

                if (!shop) {
                    uploadLicense.setEnabled(true);
                }

                edit.setText("Save Changes");
                return;
            }

            if (fs[0].getText().trim().isEmpty()) {
                err("Name cannot be empty.");
                return;
            }

            ME.name = fs[0].getText().trim();
            ME.location = fs[1].getText().trim();
            ME.phone = fs[4].getText().trim();

            if (shop) {

                if (MYSHOP != null) {

                    String old = MYSHOP.name;

                    MYSHOP.name = fs[3].getText()
                            .trim().isEmpty()
                            ? old
                            : fs[3].getText().trim();

                    MYSHOP.hours = fs[5].getText().trim();
                    MYSHOP.phone = ME.phone;
                    MYSHOP.location = ME.location;

                    for (Req q : DB.reqs) {

                        if (q != null
                                && q.shop != null
                                && old != null
                                && q.shop.equalsIgnoreCase(old)) {

                            q.shop = MYSHOP.name;
                        }
                    }

                    for (String[] a : DB.appts) {

                        if (a != null
                                && a.length > 4
                                && a[4] != null
                                && old != null
                                && a[4].equalsIgnoreCase(old)) {

                            a[4] = MYSHOP.name;
                        }
                    }

                    ME.shop = MYSHOP.name;
                }

            } else {

                ME.vehicle = fs[3].getText().trim();
            }

            for (JTextField field : fs) {
                field.setEditable(false);
            }

            uploadLicense.setEnabled(false);
            edit.setText("Edit Profile");

            DB.log("Profile updated: " + ME.username);
            DB.saveAllData();

            msg("Profile saved.");
            refresh();
        });

        // ==========================================
        // PROFILE PHOTO
        // ==========================================

        JButton photo = btn(
                "Change Photo",
                NAVY,
                e -> {

                    JFileChooser fc = new JFileChooser();

                    fc.setFileFilter(
                            new javax.swing.filechooser.FileNameExtensionFilter(
                                    "Images",
                                    "png",
                                    "jpg",
                                    "jpeg",
                                    "gif"
                            )
                    );

                    if (fc.showOpenDialog(APP)
                            == JFileChooser.APPROVE_OPTION) {

                        ME.avatar = fc.getSelectedFile()
                                .getAbsolutePath();

                        DB.saveAllData();
                        refresh();
                    }
                }
        );

        CircularAvatar av = new CircularAvatar();

        if (ME.avatar != null && !ME.avatar.trim().isEmpty()) {

            ImageIcon ic = new ImageIcon(ME.avatar);

            if (ic.getIconWidth() > 0
                    && ic.getIconHeight() > 0) {

                av.setAvatarImage(ic.getImage());

            } else {

                av.setInitial(safe(ME.name));
            }

        } else {

            av.setInitial(safe(ME.name));
        }

        // ==========================================
        // LEFT PROFILE PANEL
        // ==========================================

        JPanel left = vbox();
        left.setOpaque(false);

        left.add(flow(av, photo));
        left.add(form);

        if (!shop) {

            left.add(Box.createVerticalStrut(14));

            JPanel licensePanel = new JPanel(
                    new BorderLayout(10, 8)
            );

            licensePanel.setOpaque(false);

            licensePanel.setBorder(
                    new EmptyBorder(10, 0, 5, 0)
            );

            JLabel licenseTitle = lbl(
                    "Driver's License",
                    15,
                    true,
                    LIGHT_TEXT
            );

            licenseStatus.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            12
                    )
            );

            updateLicenseStatusColor(licenseStatus);

            JPanel licenseInfo = vbox();
            licenseInfo.setOpaque(false);

            licenseInfo.add(licenseTitle);
            licenseInfo.add(licenseStatus);

            JPanel licenseButtons = new JPanel(
                    new FlowLayout(
                            FlowLayout.RIGHT,
                            6,
                            0
                    )
            );

            licenseButtons.setOpaque(false);

            licenseButtons.add(uploadLicense);
            licenseButtons.add(viewLicense);

            licensePanel.add(
                    licenseInfo,
                    BorderLayout.CENTER
            );

            licensePanel.add(
                    licenseButtons,
                    BorderLayout.EAST
            );

            left.add(licensePanel);
        }

        left.add(Box.createVerticalStrut(12));
        left.add(flow(edit));

        if (shop && MYSHOP != null) {

            JButton editShopLocation = btn(
                    "Edit Shop Location",
                    NAVY,
                    e -> editShopLocation()
            );

            left.add(Box.createVerticalStrut(8));
            left.add(flow(editShopLocation));
        }

        // ==========================================
        // PERSONAL INFORMATION GRADIENT CARD
        // ==========================================

        GradientPanel lc = new GradientPanel(
                new BorderLayout(0, 15),
                GRADIENT_DARK,
                GRADIENT_LIGHT,
                20
        );

        lc.setBorder(
                new EmptyBorder(16, 20, 16, 20)
        );

        lc.add(
                lbl(
                        "Personal Information",
                        20,
                        true,
                        Color.WHITE
                ),
                BorderLayout.NORTH
        );

        lc.add(left, BorderLayout.CENTER);

        // ==========================================
        // PREFERENCES
        // ==========================================

        JPanel pr = vbox();

        pr.setOpaque(false);

        pr.setBorder(
                new EmptyBorder(8, 6, 8, 6)
        );

        JToggleButton n = toggle(
                ME.notif,
                v -> ME.notif = v
        );

        JToggleButton l = toggle(
                ME.loc,
                v -> ME.loc = v
        );

        pr.add(prefRow(
                "Notification",
                "Get updates about your requests and nearby shops.",
                n
        ));

        pr.add(prefRow(
                "Location Sharing",
                "Allow us to find nearby repair shops and send location-based services.",
                l
        ));

        pr.add(prefActionRow(
                "App Setting",
                "Customize your experience",
                () -> appSettings()
        ));

        pr.add(prefActionRow(
                "Privacy & security",
                "Manage your account security",
                () -> changePassword()
        ));

        pr.add(prefActionRow(
                "Help & support",
                "Get help or contact us",
                () -> help()
        ));

        // ==========================================
        // PREFERENCES GRADIENT CARD
        // ==========================================

        GradientPanel rc = new GradientPanel(
                new BorderLayout(0, 12),
                GRADIENT_DARK,
                GRADIENT_LIGHT,
                20
        );

        rc.setBorder(
                new EmptyBorder(18, 20, 18, 20)
        );

        rc.add(
                lbl(
                        "Preferences",
                        20,
                        true,
                        Color.WHITE
                ),
                BorderLayout.NORTH
        );

        rc.add(pr, BorderLayout.CENTER);

        rc.setPreferredSize(
                new Dimension(440, 100)
        );

        // ==========================================
        // FINAL PROFILE LAYOUT
        // ==========================================

        body.add(lc, BorderLayout.CENTER);

        JPanel east = new JPanel(
                new BorderLayout()
        );

        east.setOpaque(false);

        east.setBorder(
                new EmptyBorder(0, 14, 0, 0)
        );

        east.add(rc);

        body.add(east, BorderLayout.EAST);

        return pg[0];
    }

    // ==========================================
    // SAFE STRING
    // ==========================================

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // ==========================================
    // DRIVER'S LICENSE UPLOAD
    // ==========================================

    private static void uploadDriverLicense(
            JLabel status,
            JButton viewButton) {

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle(
                "Upload Driver's License"
        );

        chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "License Image (JPG, JPEG, PNG)",
                        "jpg",
                        "jpeg",
                        "png"
                )
        );

        int result = chooser.showOpenDialog(APP);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();

        if (selectedFile == null || !selectedFile.isFile()) {
            err("Selected file is invalid.");
            return;
        }

        String fileName =
                selectedFile.getName().toLowerCase();

        if (!fileName.endsWith(".jpg")
                && !fileName.endsWith(".jpeg")
                && !fileName.endsWith(".png")) {

            err("Please select a JPG, JPEG, or PNG image.");
            return;
        }

        try {

            Path folder = Path.of(
                    System.getProperty("user.home"),
                    ".vaarl",
                    "driver-licenses"
            );

            Files.createDirectories(folder);

            String extension = fileName.substring(
                    fileName.lastIndexOf('.')
            );

            String safeUsername = safe(ME.username)
                    .replaceAll(
                            "[^a-zA-Z0-9_-]",
                            "_"
                    );

            String newFileName = safeUsername
                    + "_license_"
                    + System.currentTimeMillis()
                    + extension;

            Path destination = folder.resolve(newFileName);

            Files.copy(
                    selectedFile.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            ME.license =
                    destination.toAbsolutePath().toString();

            DB.log(
                    "Driver's License uploaded: "
                            + ME.username
            );

            DB.saveAllData();

            status.setText("Uploaded");
            updateLicenseStatusColor(status);

            viewButton.setEnabled(true);

            msg("Driver's License uploaded successfully.");

        } catch (IOException ex) {

            err(
                    "Unable to upload Driver's License: "
                            + ex.getMessage()
            );
        }
    }

    // ==========================================
    // LICENSE STATUS
    // ==========================================

    private static boolean isLicenseUploaded() {

        if (ME == null
                || ME.license == null
                || ME.license.trim().isEmpty()
                || ME.license.equalsIgnoreCase("Not added yet")) {

            return false;
        }

        File file = new File(ME.license);

        return file.isFile();
    }

    private static String getLicenseStatus() {

        return isLicenseUploaded()
                ? "Uploaded"
                : "Not Uploaded";
    }

    private static void updateLicenseStatusColor(
            JLabel status) {

        if (isLicenseUploaded()) {

            status.setForeground(
                    new Color(100, 220, 160)
            );

        } else {

            status.setForeground(
                    new Color(255, 190, 100)
            );
        }
    }

    // ==========================================
    // VIEW DRIVER'S LICENSE
    // ==========================================

    private static void showDriverLicense() {

        if (!isLicenseUploaded()) {
            err("No Driver's License uploaded yet.");
            return;
        }

        ImageIcon original = new ImageIcon(ME.license);

        if (original.getIconWidth() <= 0) {
            err("Unable to display Driver's License image.");
            return;
        }

        int width = original.getIconWidth();
        int height = original.getIconHeight();

        double scale = Math.min(
                650.0 / width,
                450.0 / height
        );

        int newWidth = Math.max(
                1,
                (int) (width * scale)
        );

        int newHeight = Math.max(
                1,
                (int) (height * scale)
        );

        ImageIcon preview = new ImageIcon(
                original.getImage().getScaledInstance(
                        newWidth,
                        newHeight,
                        Image.SCALE_SMOOTH
                )
        );

        JLabel image = new JLabel(preview);

        image.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        JScrollPane scroll = new JScrollPane(image);

        scroll.setPreferredSize(
                new Dimension(
                        Math.min(newWidth + 30, 680),
                        Math.min(newHeight + 30, 480)
                )
        );

        JOptionPane.showMessageDialog(
                APP,
                scroll,
                "Driver's License ID",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    // ==========================================
    // EDIT SHOP LOCATION
    // ==========================================

    private static void editShopLocation() {

        if (MYSHOP == null) {
            err("Shop account not found.");
            return;
        }

        MapPanel map = new MapPanel();

        map.setShops(
                Collections.singletonList(MYSHOP)
        );

        final double[][] selectedLocation = {null};

        JTextField address = new JTextField(
                safe(MYSHOP.location),
                25
        );

        JLabel coordinates = new JLabel(
                "Click the map to choose the exact shop location."
        );

        coordinates.setForeground(LIGHT_TEXT);

        map.setLocationPicking(true, point -> {

            if (point == null || point.length < 2) {
                return;
            }

            selectedLocation[0] = point.clone();

            coordinates.setText(String.format(
                    "Selected: %.6f, %.6f",
                    point[0],
                    point[1]
            ));
        });

        JPanel mapPanel = new JPanel(
                new BorderLayout(8, 8)
        );

        mapPanel.add(address, BorderLayout.NORTH);
        mapPanel.add(map, BorderLayout.CENTER);
        mapPanel.add(coordinates, BorderLayout.SOUTH);

        mapPanel.setPreferredSize(
                new Dimension(650, 480)
        );

        int result = JOptionPane.showConfirmDialog(
                APP,
                mapPanel,
                "Edit Shop Location",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        map.setLocationPicking(false, null);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String newAddress = address.getText().trim();

        if (newAddress.isEmpty()) {
            err("Please enter the shop address.");
            return;
        }

        if (selectedLocation[0] == null) {
            err("Please click the map to select the shop location.");
            return;
        }

        try {

            MYSHOP.setLocation(
                    selectedLocation[0][0],
                    selectedLocation[0][1]
            );

            MYSHOP.location = newAddress;
            ME.location = newAddress;

            DB.log(
                    "Shop location updated: " + MYSHOP.name
            );

            DB.saveAllData();

            msg("Shop location saved successfully.");

            refresh();

        } catch (IllegalArgumentException ex) {

            err("The selected map coordinates are invalid.");
        }
    }

    // ==========================================
    // ON / OFF TOGGLE - NO BORDER
    // ==========================================

    static JToggleButton toggle(
            boolean on,
            Consumer<Boolean> set) {

        JToggleButton t = new JToggleButton(
                on ? "ON" : "OFF",
                on
        ) {
            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                // Light-blue background only. No border or outline.
                Color lightBlue = new Color(135, 206, 250);

                g2.setColor(lightBlue);

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        14,
                        14
                );

                // Button text
                g2.setFont(getFont());

                g2.setColor(new Color(0, 35, 75));

                FontMetrics fm = g2.getFontMetrics();

                String text = getText();

                int textX = (getWidth() - fm.stringWidth(text)) / 2;

                int textY = (getHeight() - fm.getHeight()) / 2
                        + fm.getAscent();

                g2.drawString(text, textX, textY);

                g2.dispose();
            }
        };

        t.setPreferredSize(new Dimension(76, 34));
        t.setMinimumSize(new Dimension(76, 34));
        t.setMaximumSize(new Dimension(76, 34));

        t.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        t.setForeground(new Color(0, 35, 75));
        t.setFocusPainted(false);
        t.setOpaque(false);
        t.setContentAreaFilled(false);
        t.setBorderPainted(false);
        t.setBorder(null);
        t.setRolloverEnabled(false);

        t.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        updateToggleStyle(t, on);

        t.addActionListener(e -> {

            boolean selected = t.isSelected();

            t.setText(selected ? "ON" : "OFF");

            updateToggleStyle(t, selected);

            set.accept(selected);

            DB.saveAllData();
        });

        return t;
    }

    // ==========================================
    // TOGGLE STYLE - NO BORDER
    // ==========================================

    private static void updateToggleStyle(
            JToggleButton t,
            boolean on) {

        t.setBackground(new Color(135, 206, 250));
        t.setForeground(new Color(0, 35, 75));

        // Remove all Swing borders and focus outlines.
        t.setBorder(null);
        t.setBorderPainted(false);
        t.setFocusPainted(false);

        // Custom painting handles the background.
        t.setContentAreaFilled(false);
        t.setOpaque(false);
        t.setRolloverEnabled(false);

        t.repaint();
    }

    // ==========================================
    // PREFERENCES ROW
    // ==========================================

    static JComponent prefRow(
            String t,
            String d,
            JComponent right) {

        JPanel p = new JPanel(
                new BorderLayout(12, 0)
        );

        p.setOpaque(false);

        p.setBorder(
                new EmptyBorder(14, 12, 14, 12)
        );

        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = new JPanel();

        textPanel.setOpaque(false);

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title = lbl(
                t,
                14,
                true,
                LIGHT_TEXT
        );

        title.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(5));

        JTextArea desc = new JTextArea(d);

        desc.setEditable(false);
        desc.setFocusable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setOpaque(false);

        desc.setForeground(MUTED_TEXT);

        desc.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        desc.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        textPanel.add(desc);

        JPanel actionPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        0,
                        8
                )
        );

        actionPanel.setOpaque(false);

        actionPanel.setPreferredSize(
                new Dimension(90, 55)
        );

        actionPanel.add(right);

        p.add(textPanel, BorderLayout.CENTER);
        p.add(actionPanel, BorderLayout.EAST);

        p.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 105)
        );

        p.setPreferredSize(
                new Dimension(400, 90)
        );

        return p;
    }

    // ==========================================
    // CLICKABLE PREFERENCE ROW
    // ==========================================

    static JComponent prefActionRow(
            String t,
            String d,
            Runnable action) {

        JPanel p = new JPanel(
                new BorderLayout(12, 0)
        );

        p.setOpaque(false);
        p.setBackground(new Color(0, 0, 0, 0));

        p.setBorder(
                new EmptyBorder(14, 12, 14, 12)
        );

        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        JPanel textPanel = new JPanel();

        textPanel.setOpaque(false);

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title = lbl(
                t,
                14,
                true,
                LIGHT_TEXT
        );

        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(5));

        JTextArea desc = new JTextArea(d);

        desc.setEditable(false);
        desc.setFocusable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setOpaque(false);
        desc.setForeground(MUTED_TEXT);

        desc.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(desc);

        p.add(textPanel, BorderLayout.CENTER);

        java.awt.event.MouseAdapter clickListener =
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseClicked(
                            java.awt.event.MouseEvent e) {

                        action.run();
                    }
                };

        p.addMouseListener(clickListener);
        textPanel.addMouseListener(clickListener);
        title.addMouseListener(clickListener);
        desc.addMouseListener(clickListener);

        p.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 105)
        );

        p.setPreferredSize(
                new Dimension(380, 90)
        );

        return p;
    }

    // ==========================================
    // PRIVACY & SECURITY - CHANGE PASSWORD
    // ==========================================

    static void changePassword() {

        if (ME == null) {
            return;
        }

        Color bg = new Color(0x00, 0x16, 0x38);
        Color titleColor = LIGHT_TEXT;
        Color descriptionColor = MUTED_TEXT;
        Color borderColor = GRADIENT_BORDER;
        Color navy = GRADIENT_MID;
        Color blue = new Color(35, 105, 210);

        JDialog dialog = new JDialog(
                APP,
                "Privacy & Security",
                true
        );

        dialog.setSize(420, 330);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(APP);
        dialog.setLayout(new BorderLayout());

        JPanel main = new JPanel(
                new BorderLayout(8, 12)
        );

        main.setBorder(
                new EmptyBorder(18, 20, 18, 20)
        );

        main.setBackground(bg);

        JPanel header = new JPanel();

        header.setOpaque(false);

        header.setLayout(
                new BoxLayout(
                        header,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title = new JLabel("Change Password");

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        19
                )
        );

        title.setForeground(titleColor);

        JLabel description = new JLabel(
                "Update your account password."
        );

        description.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        description.setForeground(descriptionColor);

        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(description);

        main.add(header, BorderLayout.NORTH);

        JPanel fields = new JPanel(
                new GridLayout(0, 1, 7, 7)
        );

        fields.setOpaque(false);

        JPasswordField currentPassword =
                new JPasswordField();

        JPasswordField newPassword =
                new JPasswordField();

        JPasswordField confirmPassword =
                new JPasswordField();

        fields.add(createPasswordField(
                "Current password",
                currentPassword,
                titleColor,
                borderColor
        ));

        fields.add(createPasswordField(
                "New password",
                newPassword,
                titleColor,
                borderColor
        ));

        fields.add(createPasswordField(
                "Confirm new password",
                confirmPassword,
                titleColor,
                borderColor
        ));

        main.add(fields, BorderLayout.CENTER);

        JPanel buttons = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        8,
                        0
                )
        );

        buttons.setOpaque(false);

        JButton cancel = new JButton("Cancel");

        cancel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        cancel.setForeground(Color.WHITE);
        cancel.setBackground(navy);
        cancel.setFocusPainted(false);
        cancel.setBorderPainted(false);
        cancel.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        cancel.addActionListener(e -> dialog.dispose());

        JButton save = new JButton("Save Changes");

        save.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        save.setForeground(Color.WHITE);
        save.setBackground(blue);
        save.setFocusPainted(false);
        save.setBorderPainted(false);
        save.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        save.addActionListener(e -> {

            String current = new String(
                    currentPassword.getPassword()
            );

            String newPass = new String(
                    newPassword.getPassword()
            );

            String confirm = new String(
                    confirmPassword.getPassword()
            );

            if (!PasswordSecurity.verifyAndUpgrade(
                    ME,
                    current
            )) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Current password is incorrect.",
                        "Invalid Password",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            if (newPass.length() < DB.intS(
                    "Security.Minimum Password Length",
                    6
            )) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "New password is too short.",
                        "Invalid Password",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (!newPass.equals(confirm)) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Passwords do not match.",
                        "Invalid Password",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            if (newPass.equals(current)) {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Your new password must be different from your current password.",
                        "Invalid Password",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            ME.password = PasswordSecurity.hash(newPass);

            DB.log(
                    "Password changed: " + ME.username
            );

            DB.saveAllData();

            dialog.dispose();

            JOptionPane.showMessageDialog(
                    APP,
                    "Password updated successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        buttons.add(cancel);
        buttons.add(save);

        main.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    // ==========================================
    // PASSWORD FIELD COMPONENT
    // ==========================================

    private static JPanel createPasswordField(
            String label,
            JPasswordField field,
            Color labelColor,
            Color borderColor) {

        JPanel panel = new JPanel(
                new BorderLayout(0, 4)
        );

        panel.setOpaque(false);

        JLabel title = new JLabel(label);

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        title.setForeground(labelColor);

        field.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        field.setForeground(LIGHT_TEXT);
        field.setBackground(GRADIENT_MID);
        field.setCaretColor(Color.WHITE);

        field.setPreferredSize(
                new Dimension(350, 31)
        );

        field.setBorder(
                new CompoundBorder(
                        new LineBorder(borderColor),
                        new EmptyBorder(3, 8, 3, 8)
                )
        );

        panel.add(title, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        return panel;
    }

    // ==========================================
    // APP SETTINGS
    // ==========================================

    static void appSettings() {

        String[] r = form(
                "App Setting",
                new String[]{
                    "Distance unit",
                    "Confirm before cancelling requests"
                },
                new Object[]{
                    new String[]{
                        DB.S("App.Unit", "km"),
                        "km",
                        "mi"
                    },
                    new String[]{"Yes", "No"}
                }
        );

        if (r != null) {

            DB.settings.put("App.Unit", r[0]);

            DB.saveAllData();

            msg("Settings saved.");
        }
    }

    // ==========================================
    // HELP & SUPPORT - TWO-WAY CHAT
    // ==========================================

    static void help() {

        JDialog dialog = new JDialog(
                APP,
                "Help & Support",
                true
        );

        dialog.setSize(520, 580);
        dialog.setLocationRelativeTo(APP);

        dialog.setLayout(
                new BorderLayout(10, 10)
        );

        dialog.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE
        );

        Color navy = new Color(20, 40, 75);
        Color navyLight = new Color(35, 60, 100);
        Color lightBlue = new Color(240, 244, 250);
        Color borderBlue = new Color(200, 210, 225);

        JPanel main = new JPanel(
                new BorderLayout(10, 10)
        );

        main.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        main.setBackground(lightBlue);

        // HEADER

        JPanel header = new JPanel();

        header.setLayout(
                new BoxLayout(
                        header,
                        BoxLayout.Y_AXIS
                )
        );

        header.setBackground(navy);

        header.setBorder(
                new EmptyBorder(16, 16, 16, 16)
        );

        JLabel heading = new JLabel(
                "Support Conversation"
        );

        heading.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        20
                )
        );

        heading.setForeground(Color.WHITE);

        JLabel description = new JLabel(
                "Chat with the VAR System Admin Support Team"
        );

        description.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        description.setForeground(
                new Color(215, 225, 240)
        );

        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(heading);
        header.add(Box.createVerticalStrut(5));
        header.add(description);

        main.add(header, BorderLayout.NORTH);

        // CONVERSATION AREA

        JTextArea conversation = new JTextArea();

        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);

        conversation.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        conversation.setForeground(navy);
        conversation.setBackground(Color.WHITE);

        conversation.setBorder(
                new EmptyBorder(12, 12, 12, 12)
        );

        JScrollPane scroll = new JScrollPane(conversation);

        scroll.setBorder(
                new LineBorder(borderBlue, 1)
        );

        main.add(scroll, BorderLayout.CENTER);

        // MESSAGE INPUT

        JTextArea messageInput = new JTextArea(3, 20);

        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);

        messageInput.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        messageInput.setForeground(navy);
        messageInput.setBackground(Color.WHITE);

        messageInput.setBorder(
                new EmptyBorder(8, 8, 8, 8)
        );

        JScrollPane inputScroll =
                new JScrollPane(messageInput);

        inputScroll.setPreferredSize(
                new Dimension(350, 65)
        );

        inputScroll.setBorder(
                new LineBorder(borderBlue, 1)
        );

        // SEND BUTTON

        JButton send = new JButton("Send Message");

        send.setBackground(navy);
        send.setForeground(Color.WHITE);
        send.setFocusPainted(false);
        send.setBorderPainted(false);
        send.setOpaque(true);

        send.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        send.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        send.addMouseListener(
                new java.awt.event.MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            java.awt.event.MouseEvent e) {

                        send.setBackground(navyLight);
                    }

                    @Override
                    public void mouseExited(
                            java.awt.event.MouseEvent e) {

                        send.setBackground(navy);
                    }
                }
        );

        JPanel bottom = new JPanel(
                new BorderLayout(8, 0)
        );

        bottom.setOpaque(false);

        bottom.add(
                inputScroll,
                BorderLayout.CENTER
        );

        bottom.add(
                send,
                BorderLayout.EAST
        );

        main.add(bottom, BorderLayout.SOUTH);

        // LOAD CONVERSATION

        Runnable loadMessages = () -> {

            java.util.List<DB.SupportMessage> messages =
                    DB.getSupportMessages(
                            ME.email,
                            ME.role
                    );

            StringBuilder history = new StringBuilder();

            if (messages != null) {

                for (DB.SupportMessage m : messages) {

                    if (m == null) {
                        continue;
                    }

                    String sender = m.fromAdmin
                            ? "ADMIN"
                            : "YOU";

                    String time = m.sentAt == null
                            ? ""
                            : m.sentAt.format(
                                    DateTimeFormatter.ofPattern(
                                            "MMM dd, yyyy - hh:mm a"
                                    )
                            );

                    history.append(sender)
                            .append("  |  ")
                            .append(time)
                            .append("\n");

                    history.append(safe(m.message))
                            .append("\n\n");
                }
            }

            String newText = history.length() == 0
                    ? "No messages yet. Send a message to contact Admin Support."
                    : history.toString();

            if (!conversation.getText().equals(newText)) {

                conversation.setText(newText);

                conversation.setCaretPosition(
                        conversation.getDocument().getLength()
                );
            }

            DB.markSupportReadBySender(
                    ME.email,
                    ME.role
            );
        };

        // SEND MESSAGE

        send.addActionListener(e -> {

            String message =
                    messageInput.getText().trim();

            if (message.isEmpty()) {
                return;
            }

            boolean sent = DB.sendSupportMessage(
                    ME.name,
                    ME.email,
                    ME.role,
                    message
            );

            if (sent) {

                messageInput.setText("");

                loadMessages.run();

            } else {

                JOptionPane.showMessageDialog(
                        dialog,
                        "Unable to send your message. Please try again.",
                        "Support Message",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // AUTOMATIC CHAT REFRESH

        javax.swing.Timer refreshTimer =
                new javax.swing.Timer(
                        1500,
                        e -> loadMessages.run()
                );

        dialog.addWindowListener(
                new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosed(
                            java.awt.event.WindowEvent e) {

                        refreshTimer.stop();
                    }
                }
        );

        loadMessages.run();

        refreshTimer.start();

        dialog.setContentPane(main);
        dialog.setVisible(true);
    }
}


package varsystem.ui.user;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import varsystem.data.DB;
import varsystem.model.Feedback;
import varsystem.model.Shop;
import varsystem.model.Svc;
import varsystem.ui.frame.MainFrame;
import static varsystem.ui.frame.MainFrame.APP;

public final class ShopDetailDialog {

    private static final Color NAVY = new Color(3, 25, 62);
    private static final Color BLUE = new Color(0, 105, 255);
    private static final Color TEXT = new Color(25, 30, 38);

    private ShopDetailDialog() { }

    public static void show(Shop s) {

        if (s == null) {
            JOptionPane.showMessageDialog(
                    APP,
                    "Shop information is unavailable.",
                    "Shop Details",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(s));
            return;
        }

        DB.loadShopPhotos();
        DB.addShopView(s);

        int customersHelped = DB.getCustomersHelped(s.name);

        List<String> uploadedPhotos = DB.getShopGalleryPhotos(s);

        String selectedPhotoPath = s.photoPath;

        if (uploadedPhotos != null) {
            for (String photoPath : uploadedPhotos) {
                if (photoPath != null
                        && !photoPath.isBlank()
                        && isValidPhoto(photoPath)) {
                    selectedPhotoPath = photoPath;
                    break;
                }
            }
        }

        final String mainPhotoPath = selectedPhotoPath;

        // MAIN DIALOG

        JDialog dialog = new JDialog(APP, "Shop Details", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // TOP NAVIGATION

        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(NAVY);
        nav.setPreferredSize(new Dimension(700, 45));

        JPanel navLeft = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 0)
        );
        navLeft.setBackground(NAVY);

        JButton back = new JButton("←");
        back.setForeground(Color.WHITE);
        back.setBackground(NAVY);
        back.setFont(new Font("Arial", Font.BOLD, 27));
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.setPreferredSize(new Dimension(42, 45));
        back.addActionListener(e -> dialog.dispose());

        JLabel title = new JLabel("SHOP DETAILS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 13));

        navLeft.add(back);
        navLeft.add(title);

        JButton close = new JButton("×");
        close.setForeground(Color.WHITE);
        close.setBackground(NAVY);
        close.setFont(new Font("Arial", Font.BOLD, 27));
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setPreferredSize(new Dimension(50, 45));
        close.addActionListener(e -> dialog.dispose());

        nav.add(navLeft, BorderLayout.WEST);
        nav.add(close, BorderLayout.EAST);

        // MAIN CONTENT

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(12, 10, 10, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;

        // LEFT SIDE

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBackground(Color.WHITE);

        JPanel upper = new JPanel(new GridBagLayout());
        upper.setBackground(Color.WHITE);

        GridBagConstraints u = new GridBagConstraints();
        u.insets = new Insets(2, 2, 2, 8);
        u.fill = GridBagConstraints.BOTH;
        u.weighty = 1;

        // MAIN SHOP IMAGE

        LandscapePanel mainImage = new LandscapePanel(mainPhotoPath);
        mainImage.setPreferredSize(new Dimension(205, 180));
        mainImage.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        mainImage.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showFullPhoto(dialog, mainPhotoPath);
            }
        });

        u.gridx = 0;
        u.gridy = 0;
        u.weightx = 1.1;
        upper.add(mainImage, u);

        // SHOP INFORMATION

        JPanel info = new JPanel();
        info.setBackground(Color.WHITE);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(
                "<html><b>" + escape(s.name) + "</b></html>"
        );

        name.setFont(new Font("Arial", Font.BOLD, 17));
        name.setForeground(NAVY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(name);
        info.add(Box.createVerticalStrut(9));

        addInfoRow(
                info,
                "star",
                s.reviews > 0
                        ? String.format("%.1f  (%d reviews)", s.rating, s.reviews)
                        : "No ratings yet",
                new Color(180, 135, 0)
        );

        addInfoRow(info, "settings", safe(s.hours), TEXT);
        addInfoRow(info, "map-pin", safe(s.location), TEXT);
        addInfoRow(info, "message-circle", safe(s.phone), TEXT);

        addInfoRow(
                info,
                "wrench",
                String.format("%.2f km", s.dist),
                TEXT
        );

        addInfoRow(info, "users", s.views + " Views", TEXT);

        addInfoRow(
                info,
                "users",
                "Trusted by " + customersHelped + " customers",
                TEXT
        );

        u.gridx = 1;
        u.weightx = 1;
        upper.add(info, u);

        left.add(upper, BorderLayout.CENTER);

        // LEFT COLUMN

        JPanel leftColumn = new JPanel(new BorderLayout(0, 10));
        leftColumn.setBackground(Color.WHITE);
        leftColumn.add(left, BorderLayout.CENTER);

        // MORE SHOP PHOTOS

        JPanel galleryPanel = new JPanel(new BorderLayout(5, 8));
        galleryPanel.setBackground(Color.WHITE);

        galleryPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(220, 228, 238)
                        ),
                        new EmptyBorder(10, 10, 10, 10)
                )
        );

        JLabel galleryTitle = new JLabel("More Shop Photos");
        galleryTitle.setFont(new Font("Arial", Font.BOLD, 15));
        galleryTitle.setForeground(NAVY);

        JPanel galleryList = new JPanel(
                new GridLayout(0, 3, 10, 10)
        );

        galleryList.setBackground(Color.WHITE);
        galleryList.setBorder(new EmptyBorder(2, 2, 2, 2));

        List<String> allPhotos = new ArrayList<>();

        String[] thumbnailPaths = {
            s.photoPath2,
            s.photoPath3,
            s.photoPath4
        };

        for (String photoPath : thumbnailPaths) {
            if (photoPath != null
                    && !photoPath.isBlank()
                    && !photoPath.equals(mainPhotoPath)
                    && isValidPhoto(photoPath)
                    && !allPhotos.contains(photoPath)) {
                allPhotos.add(photoPath);
            }
        }

        if (uploadedPhotos != null) {
            for (String photoPath : uploadedPhotos) {
                if (photoPath != null
                        && !photoPath.isBlank()
                        && !photoPath.equals(mainPhotoPath)
                        && isValidPhoto(photoPath)
                        && !allPhotos.contains(photoPath)) {
                    allPhotos.add(photoPath);
                }
            }
        }

        if (allPhotos.isEmpty()) {

            JLabel empty = label(
                    "No additional photos uploaded yet.",
                    11
            );

            empty.setBorder(new EmptyBorder(8, 0, 8, 0));
            galleryList.add(empty);

        } else {

            for (String photoPath : allPhotos) {

                JPanel photoBox = new JPanel(new BorderLayout());
                photoBox.setBackground(Color.WHITE);

                photoBox.setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(
                                        new Color(215, 225, 238)
                                ),
                                new EmptyBorder(4, 4, 4, 4)
                        )
                );

                LandscapePanel galleryPhoto =
                        new LandscapePanel(photoPath);

                galleryPhoto.setPreferredSize(
                        new Dimension(90, 75)
                );

                galleryPhoto.setCursor(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                );

                galleryPhoto.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        showFullPhoto(dialog, photoPath);
                    }
                });

                photoBox.add(galleryPhoto, BorderLayout.CENTER);
                galleryList.add(photoBox);
            }
        }

        JScrollPane galleryScroll = new JScrollPane(galleryList);
        galleryScroll.setBorder(null);

        galleryScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        galleryScroll.getVerticalScrollBar().setUnitIncrement(15);
        galleryScroll.setPreferredSize(new Dimension(300, 190));

        galleryPanel.add(galleryTitle, BorderLayout.NORTH);
        galleryPanel.add(galleryScroll, BorderLayout.CENTER);

        leftColumn.add(galleryPanel, BorderLayout.SOUTH);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.1;
        c.weighty = 1;
        content.add(leftColumn, c);

        // RIGHT SIDE

        JPanel right = new RoundedCard();
        right.setLayout(new BorderLayout(5, 8));
        right.setBorder(new EmptyBorder(10, 10, 10, 10));

        // SERVICES

        JPanel servicePanel = new JPanel(
                new BorderLayout(3, 8)
        );

        servicePanel.setBackground(Color.WHITE);
        servicePanel.setPreferredSize(new Dimension(0, 165));

        JPanel serviceHeading = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 6, 0)
        );

        serviceHeading.setOpaque(false);

        serviceHeading.add(
                new JLabel(
                        new MainFrame.LucideIcon("wrench", 19, BLUE)
                )
        );

        JLabel serviceTitle = new JLabel("Services");
        serviceTitle.setFont(new Font("Arial", Font.BOLD, 19));
        serviceTitle.setForeground(NAVY);

        serviceHeading.add(serviceTitle);
        servicePanel.add(serviceHeading, BorderLayout.NORTH);

        JPanel serviceList = new JPanel(
                new GridLayout(0, 1, 6, 6)
        );

        serviceList.setBackground(Color.WHITE);

        // GET ONLY SERVICES ASSIGNED TO THIS SHOP
        // AND AVAILABLE IN SERVICE PRICES

        List<String> shopServices = s.services;
        List<Svc> matchedServices = new ArrayList<>();

        if (shopServices != null && DB.svcs != null) {

            for (Svc svc : DB.svcs) {

                if (svc == null || !svc.active
                        || svc.name == null
                        || svc.name.isBlank()) {
                    continue;
                }

                boolean assigned = false;

                for (String shopService : shopServices) {

                    if (shopService != null
                            && shopService.trim().equalsIgnoreCase(
                                    svc.name.trim()
                            )) {
                        assigned = true;
                        break;
                    }
                }

                if (assigned) {
                    matchedServices.add(svc);
                }
            }
        }

        if (matchedServices.isEmpty()) {

            JLabel emptyService = label(
                    "No available services listed.",
                    11
            );

            emptyService.setBorder(
                    new EmptyBorder(8, 5, 8, 5)
            );

            serviceList.add(emptyService);

        } else {

            for (Svc svc : matchedServices) {

                JPanel item = new JPanel(
                        new BorderLayout(8, 0)
                );

                item.setBackground(Color.WHITE);

                item.setBorder(
                        new EmptyBorder(4, 5, 4, 5)
                );

                JLabel serviceName = label(
                        safe(svc.name),
                        12
                );

                JLabel price = new JLabel(
                        String.format("₱%,d", Math.max(0, svc.base))
                );

                price.setFont(
                        new Font("Arial", Font.BOLD, 12)
                );

                price.setForeground(
                        new Color(0, 140, 85)
                );

                JLabel check = new JLabel(
                        new MainFrame.LucideIcon(
                                "check",
                                14,
                                new Color(0, 160, 100)
                        )
                );

                JPanel leftItem = new JPanel(
                        new BorderLayout(5, 0)
                );

                leftItem.setBackground(Color.WHITE);
                leftItem.add(check, BorderLayout.WEST);
                leftItem.add(serviceName, BorderLayout.CENTER);

                item.add(leftItem, BorderLayout.CENTER);
                item.add(price, BorderLayout.EAST);

                serviceList.add(item);
            }
        }

        JScrollPane serviceScroll = new JScrollPane(serviceList);
        serviceScroll.setBorder(null);

        serviceScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        serviceScroll.getVerticalScrollBar().setUnitIncrement(15);

        servicePanel.add(serviceScroll, BorderLayout.CENTER);

        // SERVICES + CUSTOMER REVIEWS

        JPanel rightContent = new JPanel(
                new BorderLayout(0, 10)
        );

        rightContent.setBackground(Color.WHITE);
        rightContent.add(servicePanel, BorderLayout.NORTH);

        JPanel reviewsPanel = createReviewsPanel(s);
        rightContent.add(reviewsPanel, BorderLayout.CENTER);

        right.add(rightContent, BorderLayout.CENTER);

        // BUTTONS

        JPanel buttons = new JPanel(
                new GridLayout(1, 2, 7, 0)
        );

        buttons.setBackground(Color.WHITE);
        buttons.setPreferredSize(new Dimension(0, 45));

        JButton contact = makeButton(
                "phone-call",
                "Contact Shop",
                BLUE,
                Color.BLACK
        );

        JButton request = makeButton(
                "map-pin",
                "Request Assistance",
                Color.BLACK,
                BLUE
        );

        request.setBorder(
                BorderFactory.createLineBorder(BLUE, 2)
        );

        contact.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    dialog,
                    "Shop: " + safe(s.name)
                            + "\nContact Number: " + safe(s.phone),
                    "Contact Shop",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        request.addActionListener(e -> {
            dialog.dispose();

            SwingUtilities.invokeLater(
                    () -> RequestAssistanceDialog.show(s, null)
            );
        });

        buttons.add(contact);
        buttons.add(request);

        right.add(buttons, BorderLayout.SOUTH);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;

        content.add(right, c);

        // DISPLAY

        dialog.add(nav, BorderLayout.NORTH);
        dialog.add(content, BorderLayout.CENTER);

        dialog.setPreferredSize(new Dimension(1000, 600));
        dialog.pack();
        dialog.setSize(1000, 600);
        dialog.setMinimumSize(new Dimension(650, 380));
        dialog.setLocationRelativeTo(APP);
        dialog.setVisible(true);
    }

    // CUSTOMER REVIEWS PANEL

    private static JPanel createReviewsPanel(Shop shop) {

        JPanel panel = new JPanel(new BorderLayout(5, 8));
        panel.setBackground(Color.WHITE);

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(220, 228, 238)
                        ),
                        new EmptyBorder(10, 10, 10, 10)
                )
        );

        // GET ACTUAL CUSTOMER FEEDBACK

        List<Feedback> reviews = new ArrayList<>(
                DB.getShopFeedback(shop.name)
        );

        // LATEST REVIEWS FIRST

        reviews.sort((a, b) -> {

            if (a.submitted == null && b.submitted == null) {
                return 0;
            }

            if (a.submitted == null) {
                return 1;
            }

            if (b.submitted == null) {
                return -1;
            }

            return b.submitted.compareTo(a.submitted);
        });

        // CALCULATE AVERAGE RATING

        double average = 0;

        if (!reviews.isEmpty()) {

            int total = 0;

            for (Feedback feedback : reviews) {
                total += feedback.rating;
            }

            average = (double) total / reviews.size();
        }

        // HEADER

        JPanel header = new JPanel();
        header.setLayout(
                new BoxLayout(header, BoxLayout.Y_AXIS)
        );

        header.setBackground(Color.WHITE);

        JLabel title = new JLabel("Customer Reviews");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel summary = new JLabel(
                reviews.isEmpty()
                        ? "No customer reviews yet"
                        : String.format(
                                "%.1f / 5.0    (%d reviews)",
                                average,
                                reviews.size()
                        )
        );

        summary.setFont(new Font("Arial", Font.BOLD, 14));
        summary.setForeground(new Color(190, 135, 0));
        if (!reviews.isEmpty()) {
            summary.setIcon(new StarRatingIcon((int) Math.round(average), 15, 3));
            summary.setIconTextGap(7);
        }
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(summary);

        panel.add(header, BorderLayout.NORTH);

        // REVIEW LIST

        JPanel reviewList = new JPanel();

        reviewList.setLayout(
                new BoxLayout(reviewList, BoxLayout.Y_AXIS)
        );

        reviewList.setBackground(Color.WHITE);

        if (reviews.isEmpty()) {

            JLabel empty = new JLabel(
                    "<html><div style='text-align:center;'>"
                    + "No feedback has been submitted for this shop yet."
                    + "</div></html>",
                    SwingConstants.CENTER
            );

            empty.setFont(new Font("Arial", Font.PLAIN, 12));
            empty.setForeground(new Color(125, 130, 140));
            empty.setBorder(new EmptyBorder(25, 5, 25, 5));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);

            reviewList.add(empty);

        } else {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("MMM dd, yyyy");

            for (Feedback feedback : reviews) {

                JPanel reviewCard = new JPanel(
                        new BorderLayout(5, 7)
                );

                reviewCard.setBackground(Color.WHITE);

                reviewCard.setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(
                                        new Color(225, 230, 238)
                                ),
                                new EmptyBorder(9, 10, 9, 10)
                        )
                );

                reviewCard.setAlignmentX(Component.LEFT_ALIGNMENT);

                // CUSTOMER NAME

                String customerName =
                        feedback.userName == null
                        || feedback.userName.isBlank()
                        ? "Customer"
                        : feedback.userName;

                JLabel customer = new JLabel(
                        escape(customerName)
                );

                customer.setFont(
                        new Font("Arial", Font.BOLD, 12)
                );

                customer.setForeground(NAVY);

                // RATING

                int rating = Math.max(
                        0,
                        Math.min(5, feedback.rating)
                );

                JLabel ratingLabel = new JLabel();
                ratingLabel.setIcon(new StarRatingIcon(rating, 12, 3));
                ratingLabel.setPreferredSize(new Dimension(72, 15));
                ratingLabel.setToolTipText(rating + " out of 5 stars");

                // DATE

                String dateText = "Date unavailable";

                if (feedback.submitted != null) {
                    dateText = feedback.submitted.format(formatter);
                }

                JLabel date = new JLabel(dateText);

                date.setFont(
                        new Font("Arial", Font.PLAIN, 10)
                );

                date.setForeground(
                        new Color(130, 135, 145)
                );

                // TOP ROW

                JPanel top = new JPanel(new BorderLayout());
                top.setBackground(Color.WHITE);

                JPanel customerInfo = new JPanel(
                        new FlowLayout(FlowLayout.LEFT, 6, 0)
                );

                customerInfo.setBackground(Color.WHITE);
                customerInfo.add(customer);
                customerInfo.add(ratingLabel);

                top.add(customerInfo, BorderLayout.WEST);
                top.add(date, BorderLayout.EAST);

                // COMMENT

                String commentText =
                        feedback.comment == null
                        || feedback.comment.isBlank()
                        ? "No written comment."
                        : feedback.comment;

                JTextArea comment = new JTextArea(commentText);

                comment.setFont(
                        new Font("Arial", Font.PLAIN, 11)
                );

                comment.setForeground(TEXT);
                comment.setLineWrap(true);
                comment.setWrapStyleWord(true);
                comment.setEditable(false);
                comment.setFocusable(true);
                comment.setOpaque(false);
                comment.setBorder(null);
                comment.setRows(3);

                // INDIVIDUAL COMMENT SCROLL

                JScrollPane commentScroll = new JScrollPane(comment);

                commentScroll.setBorder(null);

                commentScroll.setPreferredSize(
                        new Dimension(100, 65)
                );

                commentScroll.setHorizontalScrollBarPolicy(
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );

                commentScroll.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                );

                commentScroll.getVerticalScrollBar().setUnitIncrement(12);

                reviewCard.add(top, BorderLayout.NORTH);
                reviewCard.add(commentScroll, BorderLayout.CENTER);

                reviewList.add(reviewCard);
                reviewList.add(Box.createVerticalStrut(8));
            }
        }

        // WHOLE REVIEWS SCROLL

        JScrollPane reviewScroll = new JScrollPane(reviewList);

        reviewScroll.setBorder(null);

        reviewScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        reviewScroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        reviewScroll.getVerticalScrollBar().setUnitIncrement(15);

        panel.add(reviewScroll, BorderLayout.CENTER);

        return panel;
    }

    // CHECK PHOTO

    private static boolean isValidPhoto(String photoPath) {

        if (photoPath == null || photoPath.isBlank()) {
            return false;
        }

        try {
            return Files.isRegularFile(Path.of(photoPath));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    // FULL PHOTO PREVIEW

    private static void showFullPhoto(
            JDialog parent,
            String photoPath) {

        if (photoPath == null || photoPath.trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "No photo available.",
                    "Shop Photo",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        File file = new File(photoPath);

        if (!file.isFile()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Photo file not found.",
                    "Shop Photo",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        ImageIcon originalIcon = new ImageIcon(photoPath);

        if (originalIcon.getIconWidth() <= 0
                || originalIcon.getIconHeight() <= 0) {

            JOptionPane.showMessageDialog(
                    parent,
                    "Unable to open this photo.",
                    "Shop Photo",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        JDialog photoWindow = new JDialog(
                parent,
                "Shop Photo Preview",
                true
        );

        photoWindow.setSize(850, 650);
        photoWindow.setMinimumSize(new Dimension(450, 350));
        photoWindow.setLocationRelativeTo(parent);
        photoWindow.setLayout(new BorderLayout(10, 10));
        photoWindow.getContentPane().setBackground(NAVY);

        JLabel photo = new JLabel("", SwingConstants.CENTER);
        photo.setOpaque(true);
        photo.setBackground(NAVY);

        photoWindow.add(photo, BorderLayout.CENTER);

        JButton close = new JButton("× Close");
        close.setFont(new Font("Arial", Font.BOLD, 14));
        close.setForeground(Color.WHITE);
        close.setBackground(BLUE);
        close.setFocusPainted(false);
        close.setBorderPainted(false);

        close.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        close.setPreferredSize(new Dimension(110, 38));
        close.addActionListener(e -> photoWindow.dispose());

        JPanel bottom = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        bottom.setBackground(NAVY);
        bottom.setBorder(new EmptyBorder(8, 12, 8, 12));
        bottom.add(close);

        photoWindow.add(bottom, BorderLayout.SOUTH);

        Runnable resizePhoto = () -> {

            int width = photo.getWidth();
            int height = photo.getHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            int originalWidth = originalIcon.getIconWidth();
            int originalHeight = originalIcon.getIconHeight();

            double scale = Math.min(
                    (double) width / originalWidth,
                    (double) height / originalHeight
            );

            int newWidth = Math.max(
                    1,
                    (int) (originalWidth * scale)
            );

            int newHeight = Math.max(
                    1,
                    (int) (originalHeight * scale)
            );

            Image scaled = originalIcon.getImage().getScaledInstance(
                    newWidth,
                    newHeight,
                    Image.SCALE_SMOOTH
            );

            photo.setIcon(new ImageIcon(scaled));
        };

        photoWindow.addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        resizePhoto.run();
                    }
                }
        );

        photoWindow.setVisible(true);
    }

    // INFORMATION ROW

    private static void addInfoRow(
            JPanel parent,
            String iconName,
            String text,
            Color textColor) {

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color iconColor = "star".equals(iconName) ? textColor : BLUE;
        JLabel icon = new JLabel(
                new MainFrame.LucideIcon(iconName, 15, iconColor)
        );

        icon.setPreferredSize(new Dimension(20, 20));

        JLabel value = label(text, 11);
        value.setForeground(textColor);

        row.add(icon, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);

        parent.add(row);
        parent.add(Box.createVerticalStrut(6));
    }

    private static JLabel label(String text, int size) {

        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.PLAIN, size));
        l.setForeground(TEXT);

        return l;
    }

    private static JButton makeButton(
            String iconName,
            String text,
            Color bg,
            Color fg) {

        JButton b = new JButton(text);

        b.setIcon(
                new MainFrame.LucideIcon(iconName, 17, fg)
        );

        b.setIconTextGap(7);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setPreferredSize(new Dimension(0, 45));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(BLUE, 1));

        b.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        return b;
    }

    private static String safe(String value) {

        return value == null || value.trim().isEmpty()
                ? "Not available" : value;
    }

    private static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // SHOP PHOTO PANEL

    private static class LandscapePanel extends JPanel {

        private Image shopImage;

        LandscapePanel(String photoPath) {
            setOpaque(false);
            setPreferredSize(new Dimension(200, 150));
            loadImage(photoPath);
        }

        private void loadImage(String photoPath) {

            shopImage = null;

            if (photoPath == null || photoPath.isBlank()) {
                return;
            }

            try {

                Path path = Path.of(photoPath);

                if (!Files.isRegularFile(path)) {
                    System.err.println(
                            "Shop photo not found: " + photoPath
                    );
                    return;
                }

                ImageIcon icon = new ImageIcon(path.toString());

                if (icon.getIconWidth() > 0
                        && icon.getIconHeight() > 0) {
                    shopImage = icon.getImage();
                }

            } catch (RuntimeException e) {

                System.err.println(
                        "Unable to display shop photo: "
                                + e.getMessage()
                );
            }
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            int w = getWidth();
            int h = getHeight();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (shopImage != null) {

                int imageWidth = shopImage.getWidth(this);
                int imageHeight = shopImage.getHeight(this);

                if (imageWidth > 0 && imageHeight > 0) {

                    double scale = Math.max(
                            (double) w / imageWidth,
                            (double) h / imageHeight
                    );

                    int drawWidth = (int) (imageWidth * scale);
                    int drawHeight = (int) (imageHeight * scale);

                    int x = (w - drawWidth) / 2;
                    int y = (h - drawHeight) / 2;

                    g2.setClip(
                            new RoundRectangle2D.Float(
                                    0, 0, w, h, 14, 14
                            )
                    );

                    g2.drawImage(
                            shopImage,
                            x,
                            y,
                            drawWidth,
                            drawHeight,
                            this
                    );

                    g2.dispose();
                    return;
                }
            }

            // DEFAULT LANDSCAPE

            g2.setColor(new Color(205, 239, 255));
            g2.fillRoundRect(0, 0, w, h, 14, 14);

            g2.setColor(Color.WHITE);
            g2.fillOval(w / 5, h / 8, 35, 25);
            g2.fillOval(w / 5 + 15, h / 8 - 10, 35, 35);
            g2.fillOval(w / 5 + 35, h / 8, 30, 25);

            g2.fillOval(w * 2 / 3, h / 5, 30, 20);
            g2.fillOval(w * 2 / 3 + 15, h / 5 - 12, 35, 32);

            g2.setColor(new Color(190, 218, 110));

            Polygon hill = new Polygon();
            hill.addPoint(0, h * 3 / 4);
            hill.addPoint(w / 4, h * 2 / 3);
            hill.addPoint(w / 2, h * 3 / 4);
            hill.addPoint(w * 3 / 4, h * 2 / 3);
            hill.addPoint(w, h * 3 / 4);
            hill.addPoint(w, h);
            hill.addPoint(0, h);

            g2.fillPolygon(hill);

            g2.setColor(new Color(135, 165, 0));

            Polygon grass = new Polygon();
            grass.addPoint(0, h * 4 / 5);
            grass.addPoint(w / 4, h * 3 / 4);
            grass.addPoint(w / 2, h * 4 / 5);
            grass.addPoint(w * 3 / 4, h * 3 / 4);
            grass.addPoint(w, h * 4 / 5);
            grass.addPoint(w, h);
            grass.addPoint(0, h);

            g2.fillPolygon(grass);

            g2.setColor(new Color(75, 90, 105));
            g2.setFont(new Font("Arial", Font.PLAIN, 11));

            String message = "No shop photo";
            FontMetrics fm = g2.getFontMetrics();

            g2.drawString(
                    message,
                    (w - fm.stringWidth(message)) / 2,
                    h - 10
            );

            g2.dispose();
        }
    }

    // Draw stars with Java2D so display does not depend on font glyph support.
    private static class StarRatingIcon implements Icon {
        private final int rating;
        private final int starSize;
        private final int gap;
        private static final Color GOLD = new Color(230, 170, 0);

        StarRatingIcon(int rating, int starSize, int gap) {
            this.rating = Math.max(0, Math.min(5, rating));
            this.starSize = starSize;
            this.gap = gap;
        }

        @Override
        public int getIconWidth() {
            return starSize * 5 + gap * 4;
        }

        @Override
        public int getIconHeight() {
            return starSize;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < 5; i++) {
                int sx = x + i * (starSize + gap);
                Polygon star = new Polygon();
                double cx = sx + starSize / 2.0;
                double cy = y + starSize / 2.0;
                double outer = starSize / 2.0;
                double inner = outer * 0.43;
                for (int p = 0; p < 10; p++) {
                    double angle = -Math.PI / 2 + p * Math.PI / 5;
                    double radius = (p % 2 == 0) ? outer : inner;
                    star.addPoint((int) Math.round(cx + Math.cos(angle) * radius),
                            (int) Math.round(cy + Math.sin(angle) * radius));
                }
                if (i < rating) {
                    g2.setColor(GOLD);
                    g2.fillPolygon(star);
                } else {
                    g2.setColor(new Color(185, 185, 185));
                    g2.drawPolygon(star);
                }
            }
            g2.dispose();
        }
    }

    // ROUNDED RIGHT PANEL

    private static class RoundedCard extends JPanel {

        RoundedCard() {
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

            g2.setColor(Color.WHITE);

            g2.fill(new RoundRectangle2D.Float(
                    1, 1,
                    getWidth() - 3,
                    getHeight() - 3,
                    28, 28
            ));

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));

            g2.draw(new RoundRectangle2D.Float(
                    1, 1,
                    getWidth() - 3,
                    getHeight() - 3,
                    28, 28
            ));

            g2.dispose();
        }
    }
}

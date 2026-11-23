
package varsystem.ui.shop;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Shop;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;

public final class ShopSettingsPage {

    private ShopSettingsPage() { }

    private static final Color NAVY = new Color(3, 25, 62);
    private static final Color BLUE = new Color(20, 105, 220);
    private static final Color BORDER = new Color(218, 228, 241);
    private static final Color MUTED = new Color(190, 202, 220);
    private static final Color RED = new Color(210, 55, 65);

    public static JComponent build() {

        JComponent[] pg = page(
                "Shop Settings",
                "Manage your shop profile and showcase your photos."
        );

        JPanel body = (JPanel) pg[1];
        body.setOpaque(true);
        body.setBackground(NAVY);

        Shop shop = Session.MYSHOP;

        if (shop == null) {
            body.add(
                    lbl("No shop account is currently logged in.",
                            14, false, Color.WHITE),
                    BorderLayout.CENTER
            );
            return pg[0];
        }

        DB.loadShopPhotos();

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(8, 4, 12, 4));

        // SHOP INTRO

        JPanel intro = new JPanel(new BorderLayout(12, 4));
        intro.setBackground(NAVY);
        intro.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(16, 18, 16, 18)
                )
        );
        intro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel introText = new JPanel();
        introText.setOpaque(false);
        introText.setLayout(new BoxLayout(introText, BoxLayout.Y_AXIS));

        JLabel shopName = new JLabel(
                shop.name == null || shop.name.isBlank()
                        ? "Your Shop" : shop.name
        );
        shopName.setFont(new Font("Segoe UI", Font.BOLD, 19));
        shopName.setForeground(Color.WHITE);

        JLabel description = new JLabel(
                "Add clear photos of your shop, service area, or completed work."
        );
        description.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        description.setForeground(MUTED);

        introText.add(shopName);
        introText.add(Box.createVerticalStrut(5));
        introText.add(description);

        JLabel badge = new JLabel("  SHOP PHOTO GALLERY  ");
        badge.setOpaque(true);
        badge.setBackground(new Color(239, 246, 255));
        badge.setForeground(BLUE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setBorder(new EmptyBorder(8, 10, 8, 10));

        intro.add(introText, BorderLayout.CENTER);
        intro.add(badge, BorderLayout.EAST);

        content.add(intro);
        content.add(Box.createVerticalStrut(20));

        // PHOTO HEADER

        JPanel photoHeader = new JPanel(new BorderLayout(10, 5));
        photoHeader.setOpaque(false);
        photoHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        JLabel sectionTitle = new JLabel("Shop Photos");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sectionTitle.setForeground(Color.WHITE);

        JLabel sectionSubtitle = new JLabel(
                "Upload photos and manage them anytime."
        );
        sectionSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sectionSubtitle.setForeground(new Color(220, 228, 240));

        headerText.add(sectionTitle);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(sectionSubtitle);

        JButton addPhotoButton = new JButton("＋  Add Photo");
        styleButton(addPhotoButton, true);
        addPhotoButton.setPreferredSize(new Dimension(180, 44));

        photoHeader.add(headerText, BorderLayout.WEST);
        photoHeader.add(addPhotoButton, BorderLayout.EAST);

        content.add(photoHeader);
        content.add(Box.createVerticalStrut(16));

        // PHOTO COUNT

        JLabel photoCount = new JLabel();
        photoCount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        photoCount.setForeground(Color.WHITE);
        photoCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoCount.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        content.add(photoCount);
        content.add(Box.createVerticalStrut(10));

        // MAIN GALLERY BOX

        RoundedCard galleryBox = new RoundedCard(
                Color.WHITE,
                new Color(215, 226, 241),
                16
        );

        galleryBox.setLayout(new BorderLayout());
        galleryBox.setBorder(new EmptyBorder(14, 14, 14, 14));
        galleryBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        galleryBox.setPreferredSize(new Dimension(700, 190));
        galleryBox.setMinimumSize(new Dimension(300, 160));
        galleryBox.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
        );

        content.add(galleryBox);

        // SAVE BUTTON

        content.add(Box.createVerticalStrut(20));

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        savePanel.setOpaque(false);
        savePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        savePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JButton saveButton = new JButton("Save");
        styleButton(saveButton, true);
        saveButton.setPreferredSize(new Dimension(150, 44));

        savePanel.add(saveButton);
        content.add(savePanel);

        saveButton.addActionListener(e -> {

            if (DB.saveData()) {
                JOptionPane.showMessageDialog(
                        body,
                        "Shop photos saved successfully!",
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        body,
                        "Unable to save shop photos.",
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // REFRESH GALLERY

        Runnable refreshPhotos = () -> {

            refreshPhotoGallery(
                    galleryBox,
                    shop,
                    photoCount,
                    body
            );

            galleryBox.revalidate();
            galleryBox.repaint();
            content.revalidate();
            content.repaint();
        };

        refreshPhotos.run();

        // ADD PHOTO

        addPhotoButton.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Add Shop Photos");
            chooser.setMultiSelectionEnabled(true);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(
                    new FileNameExtensionFilter(
                            "Image Files",
                            "jpg", "jpeg", "png", "gif", "bmp"
                    )
            );

            if (chooser.showOpenDialog(body)
                    != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File[] files = chooser.getSelectedFiles();

            if (files == null || files.length == 0) {
                File single = chooser.getSelectedFile();

                if (single != null) {
                    files = new File[]{single};
                }
            }

            if (files == null || files.length == 0) {
                return;
            }

            addPhotoButton.setEnabled(false);

            int savedCount = 0;
            List<String> errors = new ArrayList<>();

            try {

                for (File file : files) {

                    if (file == null || !file.isFile()) {
                        errors.add("Invalid photo file.");
                        continue;
                    }

                    ImageIcon test = new ImageIcon(file.getAbsolutePath());

                    if (test.getIconWidth() <= 0
                            || test.getIconHeight() <= 0) {
                        errors.add("Invalid image: " + file.getName());
                        continue;
                    }

                    boolean saved = DB.addShopGalleryPhoto(
                            shop,
                            file.toPath()
                    );

                    if (saved) {
                        savedCount++;
                    } else {
                        errors.add("Unable to save: " + file.getName());
                    }
                }

            } finally {
                addPhotoButton.setEnabled(true);
            }

            refreshPhotos.run();

            if (savedCount > 0) {

                String message = savedCount
                        + " photo(s) added successfully.";

                if (!errors.isEmpty()) {
                    message += "\n\nSome photos could not be added:\n"
                            + String.join("\n", errors);
                }

                JOptionPane.showMessageDialog(
                        body,
                        message,
                        "Photos Added",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {

                JOptionPane.showMessageDialog(
                        body,
                        errors.isEmpty()
                                ? "No photos were added."
                                : String.join("\n", errors),
                        "Upload Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // MAIN SCROLL

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        body.add(scroll, BorderLayout.CENTER);

        return pg[0];
    }

    // BUILD PHOTO GALLERY

    private static void refreshPhotoGallery(
            RoundedCard galleryBox,
            Shop shop,
            JLabel photoCount,
            JPanel parent) {

        galleryBox.removeAll();

        List<String> photos = DB.getShopGalleryPhotos(shop);

        if (photos == null) {
            photos = new ArrayList<>();
        }

        photoCount.setText("Uploaded Photos: " + photos.size());

        if (photos.isEmpty()) {

            JLabel empty = new JLabel(
                    "No photos uploaded yet. Click Add Photo to get started.",
                    SwingConstants.CENTER
            );

            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(new Color(110, 120, 135));

            galleryBox.setPreferredSize(new Dimension(700, 150));
            galleryBox.add(empty, BorderLayout.CENTER);

        } else {

            JPanel photoGrid = new JPanel(
                    new GridLayout(0, 3, 12, 12)
            );

            photoGrid.setOpaque(false);

            for (int i = 0; i < photos.size(); i++) {

                String photoPath = photos.get(i);

                JPanel photoCard = createPhotoCard(
                        shop,
                        photoPath,
                        i,
                        galleryBox,
                        parent,
                        photoCount
                );

                photoGrid.add(photoCard);
            }

            galleryBox.setPreferredSize(new Dimension(700, 300));
            galleryBox.add(photoGrid, BorderLayout.CENTER);
        }

        galleryBox.revalidate();
        galleryBox.repaint();
    }

    // INDIVIDUAL PHOTO BOX

    private static JPanel createPhotoCard(
            Shop shop,
            String photoPath,
            int index,
            RoundedCard galleryBox,
            JPanel parent,
            JLabel photoCount) {

        RoundedCard card = new RoundedCard(
                Color.WHITE,
                new Color(210, 222, 239),
                10
        );

        card.setLayout(new BorderLayout(5, 8));
        card.setBorder(new EmptyBorder(7, 7, 8, 7));
        card.setPreferredSize(new Dimension(210, 245));

        JLabel preview = new JLabel("", SwingConstants.CENTER);
        preview.setOpaque(true);
        preview.setBackground(new Color(242, 245, 249));
        preview.setPreferredSize(new Dimension(190, 155));
        preview.setBorder(
                BorderFactory.createLineBorder(
                        new Color(230, 235, 242)
                )
        );

        showPhoto(preview, photoPath);

        JLabel fileName = new JLabel(
                new File(photoPath).getName()
        );

        fileName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fileName.setForeground(new Color(65, 75, 90));
        fileName.setToolTipText(photoPath);

        JLabel heading = new JLabel("SHOP PHOTO " + (index + 1));
        heading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        heading.setForeground(NAVY);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        info.add(heading);
        info.add(Box.createVerticalStrut(3));
        info.add(fileName);

        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        actions.setOpaque(false);

        JButton changeButton = new JButton("Change");
        styleSmallButton(changeButton, BLUE);

        JButton deleteButton = new JButton("Delete");
        styleSmallButton(deleteButton, RED);

        actions.add(changeButton);
        actions.add(deleteButton);

        JPanel bottom = new JPanel(new BorderLayout(4, 6));
        bottom.setOpaque(false);
        bottom.add(info, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);

        card.add(preview, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        // CHANGE PHOTO

        changeButton.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Change Shop Photo");
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(
                    new FileNameExtensionFilter(
                            "Image Files",
                            "jpg", "jpeg", "png", "gif", "bmp"
                    )
            );

            if (chooser.showOpenDialog(parent)
                    != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File newFile = chooser.getSelectedFile();

            if (newFile == null || !newFile.isFile()) {
                JOptionPane.showMessageDialog(
                        parent,
                        "Selected photo does not exist.",
                        "Photo Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            ImageIcon test = new ImageIcon(newFile.getAbsolutePath());

            if (test.getIconWidth() <= 0
                    || test.getIconHeight() <= 0) {
                JOptionPane.showMessageDialog(
                        parent,
                        "Please select a valid image.",
                        "Photo Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    parent,
                    "Replace this photo with the selected image?",
                    "Change Photo",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            boolean changed = replaceGalleryPhoto(
                    shop,
                    photoPath,
                    newFile.toPath()
            );

            if (changed) {

                refreshPhotoGallery(
                        galleryBox,
                        shop,
                        photoCount,
                        parent
                );

                JOptionPane.showMessageDialog(
                        parent,
                        "Photo changed successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {

                JOptionPane.showMessageDialog(
                        parent,
                        "Unable to change this photo.",
                        "Change Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        // DELETE PHOTO

        deleteButton.addActionListener(e -> {

            int confirm = JOptionPane.showConfirmDialog(
                    parent,
                    "Are you sure you want to delete this photo?",
                    "Delete Photo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            boolean deleted = deleteGalleryPhoto(shop, photoPath);

            if (deleted) {

                refreshPhotoGallery(
                        galleryBox,
                        shop,
                        photoCount,
                        parent
                );

                JOptionPane.showMessageDialog(
                        parent,
                        "Photo deleted successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } else {

                JOptionPane.showMessageDialog(
                        parent,
                        "Unable to delete this photo.",
                        "Delete Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        return card;
    }

    // REPLACE PHOTO

    private static boolean replaceGalleryPhoto(
            Shop shop,
            String oldPath,
            Path newSource) {

        if (shop == null
                || oldPath == null
                || newSource == null
                || !Files.isRegularFile(newSource)) {
            return false;
        }

        List<String> before = DB.getShopGalleryPhotos(shop);

        if (before == null || !before.contains(oldPath)) {
            return false;
        }

        boolean added = DB.addShopGalleryPhoto(shop, newSource);

        if (!added) {
            return false;
        }

        List<String> after = DB.getShopGalleryPhotos(shop);

        if (after == null) {
            return false;
        }

        String newSavedPath = null;

        for (String path : after) {
            if (!before.contains(path)) {
                newSavedPath = path;
                break;
            }
        }

        if (newSavedPath == null) {
            return false;
        }

        // Delete the old photo through DB so the stored Shop is updated.
        boolean oldDeleted = DB.deleteShopGalleryPhoto(shop, oldPath);

        if (!oldDeleted) {

            // Roll back the newly added photo if replacement fails.
            DB.deleteShopGalleryPhoto(shop, newSavedPath);

            return false;
        }

        return true;
    }

    // DELETE PHOTO

    private static boolean deleteGalleryPhoto(
            Shop shop,
            String photoPath) {

        if (shop == null
                || photoPath == null
                || photoPath.isBlank()) {
            return false;
        }

        return DB.deleteShopGalleryPhoto(shop, photoPath);
    }

    // SHOW PHOTO

    private static void showPhoto(JLabel preview, String path) {

        preview.setIcon(null);
        preview.setText("");

        if (path == null || path.isBlank()) {
            preview.setText("No photo");
            return;
        }

        try {

            File file = new File(path);

            if (!file.isFile()) {
                preview.setText("Photo not found");
                return;
            }

            ImageIcon original = new ImageIcon(path);

            if (original.getIconWidth() <= 0
                    || original.getIconHeight() <= 0) {
                preview.setText("Unable to load photo");
                return;
            }

            int boxWidth = 190;
            int boxHeight = 155;

            double scale = Math.min(
                    (double) boxWidth / original.getIconWidth(),
                    (double) boxHeight / original.getIconHeight()
            );

            int width = Math.max(
                    1,
                    (int) (original.getIconWidth() * scale)
            );

            int height = Math.max(
                    1,
                    (int) (original.getIconHeight() * scale)
            );

            Image scaled = original.getImage().getScaledInstance(
                    width,
                    height,
                    Image.SCALE_SMOOTH
            );

            preview.setIcon(new ImageIcon(scaled));

        } catch (RuntimeException ex) {
            preview.setText("Unable to load photo");
        }
    }

    // ROUNDED PANEL

    private static class RoundedCard extends JPanel {

        private final Color fillColor;
        private final Color lineColor;
        private final int radius;

        RoundedCard(Color fillColor, Color lineColor, int radius) {
            this.fillColor = fillColor;
            this.lineColor = lineColor;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(fillColor);
            g2.fillRoundRect(
                    0, 0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius, radius
            );

            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(lineColor);
            g2.drawRoundRect(
                    0, 0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius, radius
            );

            g2.dispose();
        }
    }

    // SMALL BUTTON STYLE

    private static void styleSmallButton(JButton button, Color color) {

        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(7, 5, 7, 5));
    }

    // MAIN BUTTON STYLE

    private static void styleButton(
            JButton button,
            boolean primary) {

        Color normal = primary ? BLUE : NAVY;
        Color hover = new Color(0, 75, 165);

        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBackground(normal);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(10, 18, 10, 18));

        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
            }
        });
    }
}

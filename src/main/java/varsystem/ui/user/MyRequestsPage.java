
package varsystem.ui.user;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;

import varsystem.data.DB;
import static varsystem.data.Session.ME;
import varsystem.model.Req;
import varsystem.model.Shop;
import varsystem.service.NotificationService;
import varsystem.ui.common.ChatDialog;
import static varsystem.ui.components.Dialogs.ask;
import varsystem.ui.components.MapPanel;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.flow;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.APP;
import static varsystem.ui.frame.MainFrame.go;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.RED;
import static varsystem.util.Formats.fmt;

/** Lets the user track every request. */
public final class MyRequestsPage {

    private MyRequestsPage() {
    }

    // DARK BLUE GRADIENT COLORS
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

    private static final Color ACCENT_BLUE =
        new Color(0x80, 0xC4, 0xFF);

    // MAIN BLUE GRADIENT BACKGROUND
    private static class GradientBodyPanel extends JPanel {

        GradientBodyPanel() {
            super(new BorderLayout());
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);

            Graphics2D g = (Graphics2D) g0.create();

            g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );

            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            GradientPaint gradient = new GradientPaint(
                0, 0, GRADIENT_DARK,
                0, Math.max(1, getHeight()), GRADIENT_LIGHT
            );

            g.setPaint(gradient);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.dispose();
        }
    }

    // DARK BLUE GRADIENT SECTION
    private static class GradientSection extends JPanel {

        GradientSection(String title, JComponent content) {
            super(new BorderLayout(0, 12));

            setOpaque(false);
            setBorder(new EmptyBorder(15, 16, 15, 16));

            JLabel heading = new JLabel(title);
            heading.setFont(
                new Font("SansSerif", Font.BOLD, 16)
            );
            heading.setForeground(LIGHT_TEXT);

            content.setOpaque(false);

            add(heading, BorderLayout.NORTH);
            add(content, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);

            Graphics2D g = (Graphics2D) g0.create();

            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );

            GradientPaint gradient = new GradientPaint(
                0, 0, GRADIENT_DARK,
                getWidth(), getHeight(), GRADIENT_LIGHT
            );

            g.setPaint(gradient);

            g.fillRoundRect(
                0, 0, getWidth(), getHeight(), 22, 22
            );

            g.setColor(GRADIENT_BORDER);
            g.setStroke(new BasicStroke(1f));

            g.drawRoundRect(
                0, 0,
                Math.max(0, getWidth() - 1),
                Math.max(0, getHeight() - 1),
                22, 22
            );

            g.dispose();
        }
    }

    // REQUEST SUMMARY GRADIENT CARD
    private static class GradientInfoPanel extends JPanel {

        GradientInfoPanel() {
            super(new GridLayout(1, 4, 10, 0));

            setOpaque(false);
            setBorder(new EmptyBorder(12, 18, 12, 18));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);

            Graphics2D g = (Graphics2D) g0.create();

            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            // FIXED: VALUE_RENDER_QUALITY
            g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );

            GradientPaint gradient = new GradientPaint(
                0, 0, GRADIENT_DARK,
                getWidth(), getHeight(), GRADIENT_LIGHT
            );

            g.setPaint(gradient);

            g.fillRoundRect(
                0, 0,
                getWidth(), getHeight(),
                22, 22
            );

            g.setColor(GRADIENT_BORDER);
            g.setStroke(new BasicStroke(1.2f));

            g.drawRoundRect(
                0, 0,
                Math.max(0, getWidth() - 1),
                Math.max(0, getHeight() - 1),
                22, 22
            );

            g.dispose();
        }
    }

    // MAKE HEADER BACKGROUND TRANSPARENT
    private static void makeTransparent(JComponent component) {

        component.setOpaque(false);

        for (Component child : component.getComponents()) {
            if (child instanceof JComponent) {
                makeTransparent((JComponent) child);
            }
        }
    }

    // DARK DROPDOWN RENDERER
    private static class DarkComboRenderer
            extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus
            );

            label.setOpaque(true);
            label.setForeground(LIGHT_TEXT);

            label.setBackground(
                isSelected ? GRADIENT_MID : GRADIENT_DARK
            );

            label.setBorder(
                new EmptyBorder(6, 9, 6, 9)
            );

            return label;
        }
    }

    // DARK DROPDOWN UI
    private static void styleDropdown(JComboBox<String> combo) {

        combo.setBackground(GRADIENT_DARK);
        combo.setForeground(LIGHT_TEXT);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setRenderer(new DarkComboRenderer());
        combo.setOpaque(true);

        combo.setBorder(
            BorderFactory.createLineBorder(
                GRADIENT_BORDER, 1
            )
        );

        combo.setUI(new BasicComboBoxUI() {

            @Override
            protected JButton createArrowButton() {

                JButton arrow = new JButton("\u25BE");

                arrow.setForeground(LIGHT_TEXT);
                arrow.setBackground(GRADIENT_MID);
                arrow.setFont(
                    new Font("SansSerif", Font.BOLD, 15)
                );

                arrow.setBorder(
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                );

                arrow.setFocusPainted(false);
                arrow.setOpaque(true);

                return arrow;
            }

            @Override
            public void paintCurrentValueBackground(
                    Graphics g,
                    Rectangle bounds,
                    boolean hasFocus) {
                // Keep the dark background.
            }
        });

        combo.setBackground(GRADIENT_DARK);
        combo.setForeground(LIGHT_TEXT);

        combo.setBorder(
            BorderFactory.createLineBorder(
                GRADIENT_BORDER, 1
            )
        );
    }

    static int stepOf(Req r) {
        return switch (r.status) {
            case "Pending" -> 1;
            case "Accepted" -> 2;
            case "On Going" -> 3;
            case "Completed" -> 4;
            default -> -1;
        };
    }

    public static JComponent build() {

        JComponent[] pg = page(
            "My Request",
            "Track the progress of your assistance request."
        );

        JPanel originalRoot = (JPanel) pg[0];
        JPanel oldBody = (JPanel) pg[1];

        GradientBodyPanel pageRoot = new GradientBodyPanel();

        BorderLayout layout = (BorderLayout) originalRoot.getLayout();

        for (Component component : originalRoot.getComponents()) {

            if (component == oldBody) {
                continue;
            }

            Object constraint = layout.getConstraints(component);

            originalRoot.remove(component);

            if (component instanceof JComponent) {
                makeTransparent((JComponent) component);
            }

            pageRoot.add(component, constraint);
        }

        GradientBodyPanel body = new GradientBodyPanel();

        body.setBorder(
            new EmptyBorder(16, 20, 16, 20)
        );

        List<Req> mine = DB.reqs.stream()
            .filter(r -> r != null
                && r.uemail != null
                && ME != null
                && ME.email != null
                && r.uemail.equalsIgnoreCase(ME.email))
            .sorted((a, b) -> b.submitted.compareTo(a.submitted))
            .collect(Collectors.toList());

        if (mine.isEmpty()) {

            JPanel e = vbox();
            e.setOpaque(false);
            e.setBorder(new EmptyBorder(80, 0, 0, 0));

            JLabel l = lbl(
                "You have no assistance requests yet.",
                18, true, LIGHT_TEXT
            );

            l.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton b = btn(
                "Find a Repair Shop",
                BLUE,
                x -> go("Find Shop")
            );

            b.setAlignmentX(Component.CENTER_ALIGNMENT);

            e.add(l);
            e.add(Box.createVerticalStrut(14));
            e.add(b);

            body.add(e, BorderLayout.NORTH);

            pageRoot.add(body, BorderLayout.CENTER);

            pageRoot.revalidate();
            pageRoot.repaint();

            return pageRoot;
        }

        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);

        JComboBox<String> sel = new JComboBox<>(
            mine.stream()
                .map(r -> r.id + " - " + r.problem)
                .toArray(String[]::new)
        );

        styleDropdown(sel);

        Runnable show = () -> {

            int index = sel.getSelectedIndex();

            if (index < 0 || index >= mine.size()) {
                return;
            }

            host.removeAll();
            host.add(reqView(mine.get(index)));

            host.revalidate();
            host.repaint();
        };

        sel.addActionListener(e -> show.run());

        JPanel top = flow(
            lbl("Request:", 13, true, LIGHT_TEXT),
            sel
        );

        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 8, 0));

        body.add(top, BorderLayout.NORTH);
        body.add(host, BorderLayout.CENTER);

        pageRoot.add(body, BorderLayout.CENTER);

        show.run();

        pageRoot.revalidate();
        pageRoot.repaint();

        return pageRoot;
    }

    private static JLabel leftLabel(
            String text,
            int size,
            boolean bold,
            Color color) {

        JLabel label = lbl(text, size, bold, color);

        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        label.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                label.getPreferredSize().height
            )
        );

        return label;
    }

    // SHOP MAIN PHOTO
    private static JComponent shopPhoto(Shop shop) {

        JPanel photoPanel = new JPanel(new BorderLayout());

        photoPanel.setPreferredSize(new Dimension(160, 145));
        photoPanel.setMinimumSize(new Dimension(160, 145));
        photoPanel.setMaximumSize(new Dimension(160, 145));

        photoPanel.setBackground(new Color(0xEAF0F8));

        photoPanel.setBorder(
            BorderFactory.createLineBorder(
                new Color(0xD5DFEA), 1
            )
        );

        JLabel photo = new JLabel(
            "No Shop Photo",
            SwingConstants.CENTER
        );

        photo.setForeground(new Color(0x778397));
        photo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        if (shop != null
                && shop.photoPath != null
                && !shop.photoPath.trim().isEmpty()) {

            File file = new File(shop.photoPath);

            if (file.exists() && file.isFile()) {

                ImageIcon original = new ImageIcon(shop.photoPath);

                if (original.getIconWidth() > 0
                        && original.getIconHeight() > 0) {

                    Image scaled = original.getImage()
                        .getScaledInstance(
                            158,
                            143,
                            Image.SCALE_SMOOTH
                        );

                    photo.setText("");
                    photo.setIcon(new ImageIcon(scaled));
                }
            }
        }

        photoPanel.add(photo, BorderLayout.CENTER);

        return photoPanel;
    }

    static JComponent reqView(Req r) {

        Shop sh = DB.shops.stream()
            .filter(s -> s != null
                && s.name != null
                && r.shop != null
                && s.name.equalsIgnoreCase(r.shop))
            .findFirst()
            .orElse(null);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setOpaque(false);

        // REQUEST SUMMARY - DARK BLUE GRADIENT
        GradientInfoPanel info = new GradientInfoPanel();

        String eta =
            r.status.equals("Accepted") || r.status.equals("On Going")
                ? "20 Minutes"
                : r.status.equals("Completed")
                    ? "Arrived"
                    : "Waiting for shop";

        String[][] kv = {
            {"Reference ID", r.id},
            {"Vehicle", r.vehicle},
            {"Date Submitted", fmt(r.submitted)},
            {"Estimated Arrival", eta}
        };

        for (String[] x : kv) {

            JPanel c = new JPanel();

            c.setLayout(
                new BoxLayout(c, BoxLayout.Y_AXIS)
            );

            c.setOpaque(false);

            JLabel label = lbl(
                x[0], 12, false, MUTED_TEXT
            );

            JLabel value = lbl(
                x[1], 15, true, LIGHT_TEXT
            );

            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            value.setAlignmentX(Component.LEFT_ALIGNMENT);

            c.add(label);
            c.add(Box.createVerticalStrut(3));
            c.add(value);

            info.add(c);
        }

        root.add(info, BorderLayout.NORTH);

        // REQUEST PROGRESS
        JPanel steps = new JPanel() {

            private final Color DARK = new Color(0x00, 0x16, 0x38);
            private final Color LIGHT = new Color(0x10, 0x4B, 0x85);
            private final Color ACTIVE = new Color(0x19, 0x76, 0xD2);
            private final Color INACTIVE = new Color(0x35, 0x50, 0x70);
            private final Color WHITE = new Color(0xE2, 0xEE, 0xFF);

            @Override
            protected void paintComponent(Graphics g0) {

                super.paintComponent(g0);

                Graphics2D g = (Graphics2D) g0.create();

                g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                );

                int w = getWidth();
                int h = getHeight();

                GradientPaint bg = new GradientPaint(
                    0, 0, DARK,
                    w, h, LIGHT
                );

                g.setPaint(bg);
                g.fillRoundRect(0, 0, w, h, 22, 22);

                String[] nm = {
                    "Submitted",
                    "Received",
                    "Accepted",
                    "In Progress",
                    "Completed"
                };

                String[] tm = {
                    fmt(r.submitted),
                    fmt(r.submitted.plusMinutes(1)),
                    fmt(r.accepted),
                    "",
                    ""
                };

                int cur = stepOf(r);

                if (cur < 0) {

                    g.setColor(WHITE);
                    g.setFont(new Font("SansSerif", Font.BOLD, 16));

                    g.drawString(
                        "This request was " + r.status.toLowerCase() + ".",
                        20, 60
                    );

                    g.dispose();
                    return;
                }

                int cy = 44;

                for (int i = 0; i < 5; i++) {

                    int cx = w * (2 * i + 1) / 10;

                    if (i < 4) {

                        int nextX = w * (2 * i + 3) / 10;

                        Color lineStart = i < cur ? ACTIVE : INACTIVE;
                        Color lineEnd = i < cur ? LIGHT : INACTIVE;

                        GradientPaint lineGradient = new GradientPaint(
                            cx + 23, cy, lineStart,
                            nextX - 23, cy, lineEnd
                        );

                        g.setPaint(lineGradient);

                        g.setStroke(new BasicStroke(
                            5f,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                        ));

                        g.drawLine(
                            cx + 23,
                            cy,
                            nextX - 23,
                            cy
                        );
                    }

                    Color circleColor;

                    if (i < cur) {
                        circleColor = new Color(0x19, 0x76, 0xD2);
                    } else if (i == cur) {
                        circleColor = new Color(0x42, 0xA5, 0xF5);
                    } else {
                        circleColor = new Color(0x25, 0x42, 0x65);
                    }

                    g.setPaint(
                        new GradientPaint(
                            cx - 22, cy - 22,
                            circleColor.brighter(),
                            cx + 22, cy + 22,
                            circleColor.darker()
                        )
                    );

                    g.fillOval(
                        cx - 22,
                        cy - 22,
                        44,
                        44
                    );

                    g.setColor(
                        new Color(0xB8, 0xDC, 0xFF)
                    );

                    g.setStroke(new BasicStroke(1.5f));

                    g.drawOval(
                        cx - 22,
                        cy - 22,
                        44,
                        44
                    );

                    g.setColor(Color.WHITE);
                    g.setFont(
                        new Font("SansSerif", Font.BOLD, 18)
                    );

                    String sym = i < cur
                        ? "\u2713"
                        : String.valueOf(i + 1);

                    FontMetrics fm = g.getFontMetrics();

                    g.drawString(
                        sym,
                        cx - fm.stringWidth(sym) / 2,
                        cy + 6
                    );

                    g.setColor(WHITE);
                    g.setFont(
                        new Font("SansSerif", Font.BOLD, 12)
                    );

                    fm = g.getFontMetrics();

                    g.drawString(
                        nm[i],
                        cx - fm.stringWidth(nm[i]) / 2,
                        cy + 44
                    );

                    g.setColor(
                        new Color(0xC1, 0xD8, 0xF0)
                    );

                    g.setFont(
                        new Font("SansSerif", Font.PLAIN, 10)
                    );

                    fm = g.getFontMetrics();

                    String t2 = i <= cur
                        ? tm[i]
                        : (i == cur + 1 ? "(Next Step)" : "");

                    g.drawString(
                        t2,
                        cx - fm.stringWidth(t2) / 2,
                        cy + 60
                    );
                }

                g.dispose();
            }
        };

        steps.setOpaque(false);
        steps.setPreferredSize(new Dimension(100, 130));

        // SHOP INFORMATION DETAILS
        JPanel shopDetails = new JPanel();

        shopDetails.setLayout(
            new BoxLayout(shopDetails, BoxLayout.Y_AXIS)
        );

        shopDetails.setOpaque(false);
        shopDetails.setAlignmentX(Component.LEFT_ALIGNMENT);

        shopDetails.add(leftLabel(
            r.shop, 17, true, LIGHT_TEXT
        ));

        shopDetails.add(leftLabel(
            sh == null ? "" : sh.location,
            13, false, MUTED_TEXT
        ));

        shopDetails.add(leftLabel(
            sh == null ? "" : sh.phone,
            13, false, MUTED_TEXT
        ));

        shopDetails.add(leftLabel(
            sh == null ? "" : "\u2605 " + sh.rating
                + " (" + sh.reviews + " reviews)",
            13, false, MUTED_TEXT
        ));

        shopDetails.add(Box.createVerticalStrut(10));

        // SHOP BUTTONS
        JPanel shopButtons = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 0)
        );

        shopButtons.setOpaque(false);
        shopButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

        shopButtons.add(
            btn("Contact Shop", BLUE,
                e -> ChatDialog.contact(
                    r.shop,
                    sh == null ? "N/A" : sh.phone
                )
            )
        );

        shopButtons.add(
            btn("View on Map", BLUE, e -> {

                if (sh == null) return;

                MapPanel m = new MapPanel();

                m.setShops(
                    new ArrayList<>(
                        Collections.singletonList(sh)
                    )
                );

                m.sel = sh;
                m.setPreferredSize(new Dimension(560, 360));

                JOptionPane.showMessageDialog(
                    APP,
                    m,
                    sh.name + " - " + sh.location,
                    JOptionPane.PLAIN_MESSAGE
                );
            })
        );

        shopDetails.add(shopButtons);

        // PHOTO + SHOP DETAILS
        JPanel shopInfo = new JPanel(
            new BorderLayout(14, 0)
        );

        shopInfo.setOpaque(false);
        shopInfo.setBorder(new EmptyBorder(4, 4, 4, 4));

        shopInfo.add(
            shopPhoto(sh),
            BorderLayout.WEST
        );

        shopInfo.add(
            shopDetails,
            BorderLayout.CENTER
        );

        // GIVE FEEDBACK
        JPanel feedbackPanel = vbox();
        feedbackPanel.setOpaque(false);

        boolean completed =
            "Completed".equalsIgnoreCase(r.status);

        boolean alreadyRated = DB.hasFeedback(r.id);

        JButton feedbackBtn = btn(
            "Give Feedback",
            BLUE,
            e -> {
                FeedbackDialog.show(r.id, r.shop);
                refresh();
            }
        );

        feedbackBtn.setEnabled(completed && !alreadyRated);

        feedbackPanel.add(feedbackBtn);

        if (alreadyRated) {

            feedbackPanel.add(lbl(
                "\u2713 Feedback Submitted",
                12, true, new Color(0x8C, 0xE6, 0xB0)
            ));

        } else if (!completed) {

            feedbackPanel.add(lbl(
                "Available after request completion.",
                11, false, MUTED_TEXT
            ));
        }

        // LEFT CONTENT
        JPanel left = new JPanel(new BorderLayout(0, 14));
        left.setOpaque(false); // FIXED: missing closing parenthesis

        left.add(
            new GradientSection("Request Progress", steps),
            BorderLayout.NORTH
        );

        left.add(
            new GradientSection("Repair Shop Information", shopInfo),
            BorderLayout.CENTER
        );

        left.add(
            new GradientSection("Customer Feedback", feedbackPanel),
            BorderLayout.SOUTH
        );

        root.add(left, BorderLayout.CENTER);

        // REQUEST DETAILS TEXT
        JPanel detailsText = new JPanel();

        detailsText.setLayout(
            new BoxLayout(detailsText, BoxLayout.Y_AXIS)
        );

        detailsText.setOpaque(false);
        detailsText.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsText.setBorder(
            new EmptyBorder(12, 18, 10, 18)
        );

        detailsText.add(leftLabel(
            "Vehicle Type", 11, false, MUTED_TEXT
        ));

        detailsText.add(leftLabel(
            r.vehicle, 15, true, LIGHT_TEXT
        ));

        detailsText.add(Box.createVerticalStrut(8));

        detailsText.add(leftLabel(
            "Service Request", 11, false, MUTED_TEXT
        ));

        detailsText.add(leftLabel(
            r.problem, 15, true, LIGHT_TEXT
        ));

        detailsText.add(Box.createVerticalStrut(8));

        detailsText.add(leftLabel(
            "Location", 11, false, MUTED_TEXT
        ));

        detailsText.add(leftLabel(
            r.location, 13, false, LIGHT_TEXT
        ));

        detailsText.add(Box.createVerticalStrut(10));

        detailsText.add(leftLabel(
            "Estimated Cost: PHP " + (r.fee + r.travel),
            15, true, LIGHT_TEXT
        ));

        detailsText.add(leftLabel(
            "Service Fee: " + r.fee
                + " | Mechanic Travel Fee: " + r.travel,
            12, false, MUTED_TEXT
        ));

        detailsText.add(leftLabel(
            "Payment: " + r.payment,
            12, false, MUTED_TEXT
        ));

        detailsText.add(Box.createVerticalStrut(10));

        // MECHANIC INFORMATION
        String mechanic =
            r.mechanic == null || r.mechanic.isEmpty()
                ? "Not yet assigned"
                : r.mechanic;

        String mechanicPhone = "";

        if (sh != null && sh.mechs != null
                && r.mechanic != null && !r.mechanic.isEmpty()) {

            mechanicPhone = sh.mechs.stream()
                .filter(m -> m != null
                    && m.length > 1
                    && m[0].equals(r.mechanic))
                .map(m -> m[1])
                .findFirst()
                .orElse(sh.phone);
        }

        detailsText.add(leftLabel(
            "Contact Mechanic", 12, true,
            new Color(0x80, 0xC4, 0xFF)
        ));

        detailsText.add(leftLabel(
            mechanic, 15, true, LIGHT_TEXT
        ));

        if (!mechanicPhone.isEmpty()) {

            detailsText.add(leftLabel(
                mechanicPhone, 12, false, MUTED_TEXT
            ));
        }

        // CANCEL AND UPDATE BUTTONS
        boolean pending = "Pending".equals(r.status);

        JButton cancel = btn(
            "Cancel Request",
            RED,
            e -> {

                if (ask("Cancel request " + r.id + "?")) {

                    // CANCEL REQUEST
                    r.status = "Cancelled";

                    // REVOKE CUSTOMER LOCATION
                    r.shareLocation = false;
                    r.userLatitude = Double.NaN;
                    r.userLongitude = Double.NaN;

                    // SAVE CANCELLED REQUEST
                    DB.saveData();

                    NotificationService.requestCancelled(r);

                    DB.addNotification(
                        "Request cancelled: "
                            + ME.name
                            + " cancelled "
                            + r.id + "."
                    );

                    DB.log("Request cancelled: " + r.id);

                    refresh();
                }
            }
        );

        JButton update = btn(
            "Update Request",
            BLUE,
            e -> RequestAssistanceDialog.show(sh, r)
        );

        cancel.setEnabled(
            pending || "Accepted".equals(r.status)
        );

        update.setEnabled(pending);

        Dimension buttonSize = new Dimension(137, 42);

        cancel.setPreferredSize(buttonSize);
        cancel.setMinimumSize(buttonSize);
        cancel.setMaximumSize(buttonSize);

        update.setPreferredSize(buttonSize);
        update.setMinimumSize(buttonSize);
        update.setMaximumSize(buttonSize);

        // BUTTONS AT THE BOTTOM
        JPanel actionPanel = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 10, 5)
        );

        actionPanel.setOpaque(false);
        actionPanel.add(cancel);
        actionPanel.add(update);

        actionPanel.setBorder(
            new EmptyBorder(0, 5, 18, 5)
        );

        // DETAILS CONTENT
        JPanel detailsContent = new JPanel(new BorderLayout());
        detailsContent.setOpaque(false);

        detailsContent.add(
            detailsText,
            BorderLayout.NORTH
        );

        detailsContent.add(
            actionPanel,
            BorderLayout.SOUTH
        );

        // GRADIENT REQUEST DETAILS SECTION
        GradientSection detailsCard = new GradientSection(
            "Request Details",
            detailsContent
        );

        detailsCard.setPreferredSize(new Dimension(350, 100));

        root.add(detailsCard, BorderLayout.EAST);

        return root;
    }
}

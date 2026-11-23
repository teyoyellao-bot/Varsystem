
package varsystem.ui.user;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import varsystem.data.DB;
import varsystem.model.Svc;
import static varsystem.ui.components.Dialogs.infoArea;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.link;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.go;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.NAVY;

public final class UserServicesPage {

    private UserServicesPage() { }

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

    private static final Color PRICE_BLUE =
            new Color(0x65, 0xB7, 0xFF);

    private static final Color BORDER_BLUE =
            new Color(0x3A, 0x79, 0xB8);

    // CUSTOM GRADIENT PANEL
    private static class GradientPanel extends JPanel {

        private int radius = 18;

        GradientPanel() {
            setOpaque(false);
        }

        GradientPanel(int radius) {
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

            GradientPaint gradient = new GradientPaint(
                    0, 0,
                    GRADIENT_DARK,
                    getWidth(), getHeight(),
                    GRADIENT_LIGHT
            );

            g2.setPaint(gradient);

            g2.fillRoundRect(
                    0, 0,
                    getWidth(),
                    getHeight(),
                    radius,
                    radius
            );

            g2.setColor(BORDER_BLUE);

            g2.drawRoundRect(
                    0, 0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static void addLeft(
            JPanel panel,
            JComponent component
    ) {

        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (component instanceof JLabel) {
            ((JLabel) component).setHorizontalAlignment(
                    SwingConstants.LEFT
            );
        }

        Dimension max = component.getMaximumSize();

        component.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, max.height)
        );

        panel.add(component);
    }

    private static GradientPanel gradientCard() {

        GradientPanel panel = new GradientPanel(18);

        panel.setLayout(new BorderLayout());

        panel.setBorder(
                new EmptyBorder(16, 16, 16, 16)
        );

        return panel;
    }

    public static JComponent build() {

        JComponent[] pg = page(
                "SERVICES",
                "Choose a vehicle service and request assistance from a repair shop."
        );

        JPanel body = (JPanel) pg[1];

        // TOP QUICK LINKS
        JPanel quick = new JPanel(
                new GridLayout(2, 2, 10, 6)
        );

        quick.setOpaque(false);

        quick.add(link(
                "Track Maintenance",
                e -> MaintenanceDialog.show()
        ));

        quick.add(link(
                "Get Reminders",
                e -> RemindersDialog.show()
        ));

        quick.add(link(
                "View Service History",
                e -> ServiceHistoryDialog.show()
        ));

        quick.add(link(
                "My Requests",
                e -> go("My Requests")
        ));

        // TOP BANNER
        JPanel top = new JPanel(new BorderLayout(20, 0));
        top.setOpaque(false);

        JPanel titlePanel = vbox();
        titlePanel.setOpaque(false);

        titlePanel.add(lbl(
                "Keep Your Vehicle In Top Shape",
                20, true, LIGHT_TEXT
        ));

        titlePanel.add(lbl(
                "Choose the service your vehicle needs and request assistance from a repair shop.",
                12, false, MUTED_TEXT
        ));

        titlePanel.add(lbl(
                "You can select an available mechanic when submitting your request.",
                12, false, MUTED_TEXT
        ));

        top.add(titlePanel, BorderLayout.CENTER);
        top.add(quick, BorderLayout.EAST);

        GradientPanel banner = gradientCard();
        banner.setBorder(
                new EmptyBorder(16, 20, 16, 20)
        );

        banner.add(top, BorderLayout.CENTER);

        // SERVICE CARDS
        JPanel gridp = new JPanel(
                new GridLayout(0, 3, 14, 14)
        );

        gridp.setOpaque(false);

        gridp.setBorder(
                new EmptyBorder(4, 2, 4, 2)
        );

        int serviceCount = 0;

        if (DB.svcs != null) {

            for (Svc s : DB.svcs) {

                if (s == null || !s.active) {
                    continue;
                }

                serviceCount++;

                // LATEST BASE PRICE FROM SERVICE CATALOG
                int serviceFee = Math.max(0, s.base);

                int travelFee = Math.max(
                        0,
                        DB.intS(
                                "Service Rules.Travel Fee (PHP)",
                                100
                        )
                );

                int estimatedPrice = serviceFee + travelFee;

                GradientPanel c = gradientCard();

                c.setLayout(
                        new BorderLayout(0, 12)
                );

                c.setBorder(
                        new EmptyBorder(16, 16, 16, 16)
                );

                // SERVICE DETAILS
                JPanel details = vbox();
                details.setOpaque(false);

                details.setAlignmentX(
                        Component.LEFT_ALIGNMENT
                );

                addLeft(details, lbl(
                        s.name == null
                                ? "Unnamed Service"
                                : s.name,
                        17, true, LIGHT_TEXT
                ));

                details.add(
                        Box.createVerticalStrut(10)
                );

                addLeft(details, lbl(
                        "Description",
                        12, true, MUTED_TEXT
                ));

                details.add(
                        Box.createVerticalStrut(4)
                );

                JTextArea description = infoArea(
                        s.desc == null || s.desc.trim().isEmpty()
                                ? "No description available."
                                : s.desc,
                        3, 25
                );

                description.setForeground(LIGHT_TEXT);
                description.setBackground(
                        new Color(0, 0, 0, 0)
                );
                description.setOpaque(false);
                description.setCaretColor(LIGHT_TEXT);
                description.setEditable(false);
                description.setLineWrap(true);
                description.setWrapStyleWord(true);

                addLeft(details, description);

                details.add(
                        Box.createVerticalStrut(18)
                );

                addLeft(details, lbl(
                        "Base Price",
                        12, true, MUTED_TEXT
                ));

                details.add(
                        Box.createVerticalStrut(4)
                );

                addLeft(details, lbl(
                        "PHP " + String.format(
                                "%,d",
                                serviceFee
                        ),
                        14, true, LIGHT_TEXT
                ));

                details.add(
                        Box.createVerticalStrut(8)
                );

                addLeft(details, lbl(
                        "Estimated Total",
                        12, true, MUTED_TEXT
                ));

                details.add(
                        Box.createVerticalStrut(4)
                );

                addLeft(details, lbl(
                        "PHP " + String.format(
                                "%,d",
                                estimatedPrice
                        ),
                        17, true, PRICE_BLUE
                ));

                details.add(
                        Box.createVerticalStrut(4)
                );

                addLeft(details, lbl(
                        "Includes estimated travel fee of PHP "
                                + String.format("%,d", travelFee)
                                + ".",
                        11, false, MUTED_TEXT
                ));

                c.add(details, BorderLayout.CENTER);

                // NO REQUEST BUTTON
                gridp.add(c);
            }
        }

        // NO AVAILABLE SERVICES
        if (serviceCount == 0) {

            JPanel empty = new JPanel(
                    new BorderLayout()
            );

            empty.setOpaque(false);

            empty.add(
                    lbl(
                            "No services available at the moment.",
                            14, false, MUTED_TEXT
                    ),
                    BorderLayout.NORTH
            );

            gridp.add(empty);
        }

        // AVAILABLE SERVICES SECTION
        JPanel mid = new JPanel(
                new BorderLayout(0, 8)
        );

        mid.setOpaque(false);

        JPanel heading = vbox();
        heading.setOpaque(false);

        heading.add(lbl(
                "Available Services",
                20, true, Color.BLACK
        ));

        heading.add(lbl(
                "Prices are based on the latest active service catalog.",
                12, false, Color.BLACK
        ));

        heading.setBorder(
                new EmptyBorder(10, 0, 6, 0)
        );

        mid.add(
                heading,
                BorderLayout.NORTH
        );

        JScrollPane scroll = new JScrollPane(gridp);

        scroll.setBorder(null);
        scroll.setOpaque(false);

        scroll.getViewport().setOpaque(false);

        scroll.getVerticalScrollBar().setUnitIncrement(16);

        mid.add(
                scroll,
                BorderLayout.CENTER
        );

        // BOTTOM FIND SHOP SECTION
        GradientPanel bottom = gradientCard();

        bottom.setLayout(
                new BorderLayout(15, 0)
        );

        bottom.setBorder(
                new EmptyBorder(14, 20, 14, 20)
        );

        JPanel bottomText = vbox();
        bottomText.setOpaque(false);

        bottomText.add(lbl(
                "Need to choose a repair shop first?",
                17, true, LIGHT_TEXT
        ));

        bottomText.add(lbl(
                "Browse nearby shops and view their services and available mechanics.",
                12, false, MUTED_TEXT
        ));

        bottom.add(
                bottomText,
                BorderLayout.CENTER
        );

        bottom.add(
                btn(
                        "Find Shops  >",
                        NAVY,
                        e -> go("Find Shop")
                ),
                BorderLayout.EAST
        );

        // FINAL LAYOUT
        body.add(
                banner,
                BorderLayout.NORTH
        );

        body.add(
                mid,
                BorderLayout.CENTER
        );

        body.add(
                bottom,
                BorderLayout.SOUTH
        );

        return pg[0];
    }
}

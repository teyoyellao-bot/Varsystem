
package varsystem.ui.shop;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import varsystem.model.Req;
import static varsystem.service.RequestService.shopReqs;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.*;

public final class ShopHistoryPage {

    private ShopHistoryPage() { }

    public static JComponent build() {

        JComponent[] pg = page(
                "Service History",
                "View completed customer service requests and transaction details."
        );

        JPanel body = (JPanel) pg[1];

        JPanel host = new JPanel(new BorderLayout(0, 10));
        host.setOpaque(false);

        JLabel count = lbl("", 12, false, sub());

        JTextField search = new JTextField(22);
        search.setToolTipText("Search customer, request ID, or vehicle");

        Runnable fill = () -> {

            String keyword = search.getText()
                    .toLowerCase().trim();

            List<Req> history = shopReqs().stream()
                    .filter(r -> r != null
                            && "Completed".equalsIgnoreCase(r.status))
                    .filter(r -> keyword.isEmpty()
                            || safe(r.id).toLowerCase().contains(keyword)
                            || safe(r.uname).toLowerCase().contains(keyword)
                            || safe(r.vehicle).toLowerCase().contains(keyword)
                            || safe(r.problem).toLowerCase().contains(keyword))
                    .sorted((a, b) -> {
                        if (a.submitted == null) return 1;
                        if (b.submitted == null) return -1;
                        return b.submitted.compareTo(a.submitted);
                    })
                    .collect(Collectors.toList());

            count.setText(history.size() + " completed requests");

            JPanel list = new JPanel();
            list.setOpaque(false);
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

            if (history.isEmpty()) {

                JPanel empty = vbox();
                empty.setBorder(new EmptyBorder(50, 20, 50, 20));

                JLabel message = lbl(
                        "No completed service requests yet.",
                        16, true, sub()
                );

                message.setAlignmentX(Component.CENTER_ALIGNMENT);
                empty.add(message);

                list.add(empty);

            } else {

                for (Req r : history) {
                    list.add(createHistoryCard(r));
                    list.add(Box.createVerticalStrut(12));
                }
            }

            host.removeAll();
            host.add(new JScrollPane(list), BorderLayout.CENTER);
            host.revalidate();
            host.repaint();
        };

        search.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    @Override
                    public void insertUpdate(
                            javax.swing.event.DocumentEvent e) {
                        fill.run();
                    }

                    @Override
                    public void removeUpdate(
                            javax.swing.event.DocumentEvent e) {
                        fill.run();
                    }

                    @Override
                    public void changedUpdate(
                            javax.swing.event.DocumentEvent e) {
                        fill.run();
                    }
                }
        );

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(search, BorderLayout.WEST);
        top.add(count, BorderLayout.EAST);

        body.add(section("Completed Requests", top, null),
                BorderLayout.NORTH);

        body.add(host, BorderLayout.CENTER);

        fill.run();

        return pg[0];
    }

    private static JComponent createHistoryCard(Req r) {

        JPanel content = vbox();

        content.add(lbl(
                safe(r.uname),
                17, true, txt()
        ));

        content.add(lbl(
                "Reference ID: " + safe(r.id),
                12, false, sub()
        ));

        content.add(Box.createVerticalStrut(10));

        content.add(lbl("CUSTOMER DETAILS", 12, true, BLUE));
        content.add(lbl("Name: " + safe(r.uname), 13, false, txt()));
        content.add(lbl("Email: " + safe(r.uemail), 13, false, txt()));
        content.add(lbl("Contact Number: " + safe(r.uphone), 13, false, txt()));
        content.add(lbl("Location: " + safe(r.location), 13, false, txt()));

        content.add(Box.createVerticalStrut(8));

        content.add(lbl("VEHICLE AND SERVICE", 12, true, BLUE));
        content.add(lbl("Vehicle: " + safe(r.vehicle), 13, false, txt()));
        content.add(lbl("Service: " + safe(r.problem), 13, false, txt()));
        content.add(lbl("Assigned Mechanic: " + safe(r.mechanic), 13, false, txt()));

        if (r.details != null && !r.details.isBlank()) {
            content.add(lbl(
                    "Description: " + r.details,
                    13, false, txt()
            ));
        }

        content.add(Box.createVerticalStrut(8));

        content.add(lbl("PAYMENT DETAILS", 12, true, BLUE));
        content.add(lbl(
                "Service Fee: PHP " + r.fee,
                13, false, txt()
        ));
        content.add(lbl(
                "Mechanic Travel Fee: PHP " + r.travel,
                13, false, txt()
        ));

        content.add(lbl(
                "Total Amount: PHP " + (r.fee + r.travel),
                16, true, txt()
        ));

        content.add(lbl(
                "Payment Method: " + safe(r.payment),
                13, false, txt()
        ));

        content.add(Box.createVerticalStrut(8));

        content.add(lbl("REQUEST INFORMATION", 12, true, BLUE));
        content.add(lbl(
                "Submitted: " + formatDate(r.submitted),
                13, false, txt()
        ));

        content.add(lbl(
                "Completed: " + formatDate(r.completed),
                13, false, txt()
        ));

        content.add(lbl(
                "Status: Completed",
                13, true, GREEN
        ));

        JButton details = btn(
                "View Full Details",
                BLUE,
                e -> showFullDetails(r)
        );

        content.add(Box.createVerticalStrut(10));
        content.add(details);

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 226, 235)),
                new EmptyBorder(16, 18, 16, 18)
        ));

        card.add(content, BorderLayout.CENTER);

        return card;
    }

    private static void showFullDetails(Req r) {

        JTextArea text = new JTextArea();

        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("SansSerif", Font.PLAIN, 13));

        text.setText("""
                CUSTOMER DETAILS
                Name: %s
                Email: %s
                Contact Number: %s
                Location: %s

                VEHICLE AND SERVICE DETAILS
                Vehicle: %s
                Service: %s
                Description: %s
                Assigned Mechanic: %s

                PAYMENT DETAILS
                Service Fee: PHP %s
                Travel Fee: PHP %s
                Total Amount: PHP %s
                Payment Method: %s

                REQUEST INFORMATION
                Reference ID: %s
                Submitted: %s
                Completed: %s
                Status: %s
                """.formatted(
                safe(r.uname),
                safe(r.uemail),
                safe(r.uphone),
                safe(r.location),
                safe(r.vehicle),
                safe(r.problem),
                safe(r.details),
                safe(r.mechanic),
                r.fee,
                r.travel,
                r.fee + r.travel,
                safe(r.payment),
                safe(r.id),
                formatDate(r.submitted),
                formatDate(r.completed),
                safe(r.status)
        ));

        text.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(450, 460));

        JOptionPane.showMessageDialog(
                null,
                scroll,
                "Completed Request Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static String formatDate(
            java.time.LocalDateTime date) {

        if (date == null) {
            return "N/A";
        }

        return date.format(
                DateTimeFormatter.ofPattern(
                        "MMM dd, yyyy - hh:mm a"
                )
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank()
                ? "N/A" : value;
    }
}

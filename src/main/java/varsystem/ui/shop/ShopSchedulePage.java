
package varsystem.ui.shop;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import varsystem.model.Req;
import static varsystem.service.RequestService.shopReqs;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.*;
import static varsystem.util.Formats.DD;

/** History page - displays the same requests used by Services. */
public final class ShopSchedulePage {

    private ShopSchedulePage() { }

    public static JComponent build() {

        JComponent[] pg = page(
                "History",
                "View customer requests, service details, and job status."
        );

        JPanel body = (JPanel) pg[1];

        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);

        JComboBox<String> fil = new JComboBox<>(
                new String[]{
                    "All Status",
                    "Pending",
                    "Accepted",
                    "On Going",
                    "Completed",
                    "Cancelled"
                }
        );

        JComboBox<String> sort = new JComboBox<>(
                new String[]{
                    "Newest First",
                    "Oldest First",
                    "Customer"
                }
        );

        JTextField[] q = new JTextField[1];

        JLabel count = lbl("", 12, false, sub());

        Runnable fill = () -> {

            String s = q[0] == null
                    ? ""
                    : q[0].getText().toLowerCase().trim();

            // SAME DATA SOURCE AS SHOP SERVICES
            List<Req> all = shopReqs();

            List<Req> l = all.stream()
                    .filter(r -> r != null)
                    .filter(r ->
                            fil.getSelectedIndex() == 0
                            || safe(r.status).equals(
                                    safe((String) fil.getSelectedItem())
                            )
                    )
                    .filter(r ->
                            s.isEmpty()
                            || safe(r.id).toLowerCase().contains(s)
                            || safe(r.uname).toLowerCase().contains(s)
                            || safe(r.vehicle).toLowerCase().contains(s)
                            || safe(r.problem).toLowerCase().contains(s)
                    )
                    .collect(Collectors.toList());

            // SORTING
            switch (sort.getSelectedIndex()) {

                case 0 -> l.sort(Comparator.comparing(
                        (Req r) -> r.submitted,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                ));

                case 1 -> l.sort(Comparator.comparing(
                        (Req r) -> r.submitted,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                ));

                case 2 -> l.sort(Comparator.comparing(
                        r -> safe(r.uname),
                        String.CASE_INSENSITIVE_ORDER
                ));

                default -> {
                }
            }

            count.setText(l.size() + " requests");

            // REQUEST TABLE
            List<Object[]> rows = new ArrayList<>();

            int n = 1;

            for (Req r : l) {

                String requestedDate = r.submitted == null
                        ? "N/A"
                        : r.submitted.format(DD);

                rows.add(new Object[]{
                    "" + n++,
                    safe(r.uname),
                    safe(r.vehicle),
                    safe(r.problem),
                    requestedDate,
                    safe(r.mechanic).isEmpty()
                            ? "Unassigned"
                            : r.mechanic,
                    safe(r.status),
                    btn("Details", BLUE, e -> showDetails(r))
                });
            }

            host.removeAll();

            host.add(
                    grid(
                            new String[]{
                                "#",
                                "Customer",
                                "Vehicle",
                                "Service",
                                "Requested",
                                "Mechanic",
                                "Status",
                                ""
                            },
                            new double[]{
                                .3, 2, 1.7, 2, 1.2, 1.5, 1, 1
                            },
                            rows
                    )
            );

            host.revalidate();
            host.repaint();
        };

        q[0] = search(
                "Search request ID, customer, vehicle, or service...",
                fill
        );

        fil.addActionListener(e -> fill.run());
        sort.addActionListener(e -> fill.run());

        // FILTER AND SEARCH
        JPanel tools = new JPanel(new BorderLayout(8, 0));
        tools.setOpaque(false);

        JPanel filters = flow(
                lbl("Filter:", 12, false, txt()),
                fil,
                lbl("Sort:", 12, false, txt()),
                sort
        );

        tools.add(q[0], BorderLayout.CENTER);
        tools.add(filters, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);

        top.add(tools, BorderLayout.CENTER);
        top.add(count, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);

        content.add(top, BorderLayout.NORTH);
        content.add(host, BorderLayout.CENTER);

        body.add(
                section("Customer History", content, null),
                BorderLayout.CENTER
        );

        fill.run();

        return pg[0];
    }

    // =========================================
    // CUSTOMER AND SERVICE DETAILS
    // =========================================

    private static void showDetails(Req r) {

        String details = """
                CUSTOMER INFORMATION
                ----------------------------------
                Request ID: %s
                Customer Name: %s
                Email: %s
                Vehicle: %s
                Location: %s

                SERVICE INFORMATION
                ----------------------------------
                Service Requested: %s
                Additional Details: %s
                Requested Date: %s

                JOB INFORMATION
                ----------------------------------
                Assigned Mechanic: %s
                Status: %s
                Payment: %s
                Service Fee: PHP %s
                Travel Fee: PHP %s
                """.formatted(
                safe(r.id),
                safe(r.uname),
                safe(r.uemail),
                safe(r.vehicle),
                safe(r.location),
                safe(r.problem),
                safe(r.details),
                r.submitted == null
                        ? "N/A"
                        : r.submitted.format(DD),
                safe(r.mechanic).isEmpty()
                        ? "Unassigned"
                        : r.mechanic,
                safe(r.status),
                safe(r.payment),
                r.fee,
                r.travel
        );

        JTextArea area = new JTextArea(details);

        area.setEditable(false);
        area.setFont(new Font("Arial", Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(22);
        area.setColumns(42);
        area.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        JScrollPane scroll = new JScrollPane(area);

        JOptionPane.showMessageDialog(
                null,
                scroll,
                "Customer and Service Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}


package varsystem.ui.shop;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import varsystem.data.DB;
import varsystem.model.Req;
import varsystem.service.NotificationService;
import static varsystem.service.RequestService.shopReqs;
import varsystem.ui.common.ChatDialog;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.grid;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.search;
import static varsystem.ui.components.Ui.section;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.GREEN;
import static varsystem.ui.theme.Theme.sub;
import static varsystem.util.Formats.DD;

/**
 * Accepted jobs: start a job, complete it, message the customer.
 * Updates are visible to the user and admin.
 */
public final class ShopServicesPage {

    private ShopServicesPage() {
    }

    public static JComponent build() {

        JComponent[] pg = page(
                "Services",
                "View, accepted, schedule, and manage customer service requests."
        );

        JPanel body = (JPanel) pg[1];

        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);

        JLabel count = lbl("", 12, false, sub());

        JTextField[] q = new JTextField[1];

        Runnable fill = () -> {

            String s = q[0] == null
                    ? ""
                    : q[0].getText().toLowerCase().trim();

            List<Req> l = shopReqs().stream()
                    .filter(r ->
                            (r.status.equals("Accepted")
                                    || r.status.equals("On Going")
                                    || r.status.equals("Completed"))
                                    && (s.isEmpty()
                                    || r.id.toLowerCase().contains(s)
                                    || r.uname.toLowerCase().contains(s)
                                    || r.vehicle.toLowerCase().contains(s))
                    )
                    .sorted((a, b) ->
                            b.submitted.compareTo(a.submitted))
                    .collect(Collectors.toList());

            count.setText(l.size() + " requests");

            List<Object[]> rows = new ArrayList<>();

            int n = 1;

            for (Req r : l) {

                JButton adv = btn(
                        r.status.equals("Accepted")
                                ? "Start Job"
                                : "Complete",
                        GREEN,
                        e -> {

                            // START JOB
                            if (r.status.equals("Accepted")) {

                                r.status = "On Going";

                                NotificationService.repairStarted(r);

                                DB.addNotification(
                                        "Request update: " + r.uname
                                        + ", your request " + r.id
                                        + " is now On Going."
                                );

                                DB.log(
                                        "Request " + r.id + " -> On Going"
                                );

                                DB.saveData();

                                refresh();

                                return;
                            }

                            // COMPLETE JOB
                            if (r.status.equals("On Going")) {

                                // Editable Service Fee
                                JTextField serviceField = new JTextField(
                                        String.valueOf(
                                                r.fee > 0
                                                        ? r.fee
                                                        : DB.baseFee(r.problem)
                                        )
                                );

                                // Editable Travel Fee
                                JTextField travelField = new JTextField(
                                        String.valueOf(r.travel)
                                );

                                JPanel feePanel = new JPanel(
                                        new GridLayout(0, 1, 5, 5)
                                );

                                feePanel.add(
                                        new JLabel("Service Fee (₱):")
                                );

                                feePanel.add(serviceField);

                                feePanel.add(
                                        new JLabel("Travel Fee (₱):")
                                );

                                feePanel.add(travelField);

                                int input = JOptionPane.showConfirmDialog(
                                        null,
                                        feePanel,
                                        "Enter Service and Travel Fees",
                                        JOptionPane.OK_CANCEL_OPTION,
                                        JOptionPane.PLAIN_MESSAGE
                                );

                                if (input != JOptionPane.OK_OPTION) {
                                    return;
                                }

                                int serviceAmount;
                                int travelAmount;

                                try {

                                    serviceAmount = Integer.parseInt(
                                            serviceField.getText().trim()
                                    );

                                    travelAmount = Integer.parseInt(
                                            travelField.getText().trim()
                                    );

                                    if (serviceAmount < 0
                                            || travelAmount < 0) {

                                        throw new NumberFormatException();
                                    }

                                } catch (NumberFormatException ex) {

                                    JOptionPane.showMessageDialog(
                                            null,
                                            "Please enter valid non-negative amounts.",
                                            "Invalid Amount",
                                            JOptionPane.ERROR_MESSAGE
                                    );

                                    return;
                                }

                                long totalAmount =
                                        (long) serviceAmount + travelAmount;

                                // RECEIPT PREVIEW
                                int confirm = JOptionPane.showConfirmDialog(
                                        null,
                                        """
                                        SERVICE RECEIPT

                                        Request ID: %s
                                        Customer: %s
                                        Vehicle: %s
                                        Service: %s

                                        Service Fee: ₱%s
                                        Travel Fee: ₱%s
                                        --------------------------
                                        TOTAL AMOUNT: ₱%s

                                        Confirm and send this receipt to the customer?
                                        """.formatted(
                                                r.id,
                                                r.uname,
                                                r.vehicle,
                                                r.problem,
                                                String.format("%,d", serviceAmount),
                                                String.format("%,d", travelAmount),
                                                String.format("%,d", totalAmount)
                                        ),
                                        "Confirm Service Receipt",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.INFORMATION_MESSAGE
                                );

                                if (confirm != JOptionPane.YES_OPTION) {
                                    return;
                                }

                                // SAVE BOTH FEES
                                r.fee = serviceAmount;
                                r.travel = travelAmount;

                                // COMPLETE THE REQUEST
                                r.status = "Completed";

                                // REVOKE LOCATION SHARING IMMEDIATELY
                                r.revokeLocationSharing();

                                // NOTIFY SHOP / SERVICE
                                NotificationService.serviceCompleted(r);

                                // NOTIFY CUSTOMER
                                DB.addNotification(
                                        """
                                        Your service request %s is now Completed.
                                        Service: %s
                                        Service Fee: ₱%s
                                        Travel Fee: ₱%s
                                        TOTAL AMOUNT: ₱%s
                                        Please check your service receipt.
                                        """.formatted(
                                                r.id,
                                                r.problem,
                                                String.format("%,d", serviceAmount),
                                                String.format("%,d", travelAmount),
                                                String.format("%,d", totalAmount)
                                        )
                                );

                                DB.log(
                                        "Request " + r.id
                                        + " completed. Service Fee: ₱"
                                        + serviceAmount
                                        + ", Travel Fee: ₱"
                                        + travelAmount
                                        + ", Total: ₱"
                                        + totalAmount
                                );

                                // SAVE STATUS AND CLEARED LOCATION
                                DB.saveData();

                                JOptionPane.showMessageDialog(
                                        null,
                                        """
                                        Service completed!

                                        Receipt sent to customer.

                                        Service Fee: ₱%s
                                        Travel Fee: ₱%s
                                        TOTAL: ₱%s
                                        """.formatted(
                                                String.format("%,d", serviceAmount),
                                                String.format("%,d", travelAmount),
                                                String.format("%,d", totalAmount)
                                        ),
                                        "Receipt Sent",
                                        JOptionPane.INFORMATION_MESSAGE
                                );

                                refresh();
                            }
                        }
                );

                adv.setEnabled(!r.status.equals("Completed"));

                rows.add(new Object[]{
                    "" + n++,
                    r.uname,
                    r.vehicle,
                    r.problem,
                    r.submitted.format(DD),
                    btn(
                            "Message",
                            BLUE,
                            e -> ChatDialog.open(r.uname)
                    ),
                    r.status,
                    adv
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
                                "",
                                "Status",
                                ""
                            },
                            new double[]{
                                .3,
                                2,
                                2,
                                2,
                                1.5,
                                .7,
                                1,
                                .7
                            },
                            rows
                    )
            );

            host.revalidate();
            host.repaint();
        };

        q[0] = search(
                "Search request ID, customer, or vehicle...",
                fill
        );

        JPanel t = new JPanel(new BorderLayout());

        t.setOpaque(false);
        t.add(q[0], BorderLayout.WEST);
        t.add(count, BorderLayout.EAST);

        JPanel c = new JPanel(new BorderLayout(0, 8));

        c.setOpaque(false);
        c.add(t, BorderLayout.NORTH);
        c.add(host, BorderLayout.CENTER);

        body.add(
                section("Accepted Requests", c, null),
                BorderLayout.CENTER
        );

        fill.run();

        return pg[0];
    }
}

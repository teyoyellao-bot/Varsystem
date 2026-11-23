
package varsystem.ui.shop;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Req;
import varsystem.model.User;
import varsystem.service.NotificationService;
import static varsystem.service.RequestService.acceptReq;
import static varsystem.service.RequestService.shopReqs;
import varsystem.ui.common.RequestDetailDialog;
import static varsystem.ui.components.Dialogs.ask;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.grid;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.search;
import static varsystem.ui.components.Ui.section;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.GRAY;
import static varsystem.ui.theme.Theme.RED;
import static varsystem.ui.theme.Theme.sub;
import static varsystem.util.Formats.DD;

public final class ShopRequestsPage {

    private ShopRequestsPage() {}

    public static JComponent build() {

        JComponent[] pg = page(
            "Service Requests",
            "View, accept, schedule, and manage customer service requests."
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
                .filter(r -> r.status != null
                    && r.status.equals("Pending")
                    && (s.isEmpty()
                    || (r.id != null && r.id.toLowerCase().contains(s))
                    || (r.uname != null && r.uname.toLowerCase().contains(s))
                    || (r.vehicle != null && r.vehicle.toLowerCase().contains(s))))
                .sorted((a, b) ->
                    b.submitted.compareTo(a.submitted))
                .collect(Collectors.toList());

            count.setText(l.size() + " requests");

            List<Object[]> rows = new ArrayList<>();

            int n = 1;

            for (Req r : l) {

                rows.add(new Object[] {
                    "" + n++,
                    r.uname,
                    r.vehicle,
                    r.problem,
                    r.submitted.format(DD),

                    btn("Accept / Assign", BLUE, e -> {
                        if (Session.MYSHOP != null) {
                            java.util.List<User> shopMechanics = DB.users.stream()
                                .filter(u -> u != null && "Mechanic".equalsIgnoreCase(u.role)
                                    && u.shop != null && u.shop.equalsIgnoreCase(Session.MYSHOP.name)
                                    && "Active".equalsIgnoreCase(u.status))
                                .toList();
                            if (!shopMechanics.isEmpty()) {
                                String[] names = shopMechanics.stream().map(u -> u.name).toArray(String[]::new);
                                String chosen = (String) JOptionPane.showInputDialog(null,
                                    "Assign this request to a mechanic:", "Mechanic Assignment",
                                    JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                                if (chosen == null) return;
                                r.mechanic = chosen;
                            }
                        }
                        acceptReq(r);
                        DB.saveRequests();
                        DB.saveData();
                        refresh();
                    }),

                    btn("Decline", RED, e -> {

                        if (ask("Decline request " + r.id + "?")) {

                            r.status = "Declined";

                            NotificationService.requestRejected(r);

                            DB.log("Request declined: " + r.id);

                            DB.saveRequests();
                            DB.saveData();

                            refresh();
                        }
                    }),

                    btn("Details", GRAY,
                        e -> RequestDetailDialog.show(r, false))
                });
            }

            host.removeAll();

            host.add(
                grid(
                    new String[] {
                        "#",
                        "Customer",
                        "Vehicle",
                        "Service",
                        "Requested",
                        "",
                        "",
                        ""
                    },
                    new double[] {
                        .3, 2, 2, 2, 1.5, .6, .6, .6
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
            section("Incoming Requests", c, null),
            BorderLayout.CENTER
        );

        fill.run();

        return pg[0];
    }
}

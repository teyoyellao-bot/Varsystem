
package varsystem.ui.admin;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import varsystem.data.DB;
import varsystem.model.Req;
import static varsystem.service.RequestService.cnt;
import varsystem.ui.common.RequestDetailDialog;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.*;

/** Admin monitoring of every service request in the system. */
public final class AdminRequestsPage {

    private AdminRequestsPage() { }

    public static JComponent build() {

        JComponent[] pg = page(
                "Service Requests",
                "Monitor service requests and update their status."
        );

        JPanel body = (JPanel) pg[1];

        List<Req> a = DB.reqs;

        body.add(stats(new String[][]{
            {"All Requests", "" + a.size()},
            {"Pending", "" + cnt(a, "Pending")},
            {"Accepted", "" + cnt(a, "Accepted")},
            {"Completed", "" + cnt(a, "Completed")}
        }), BorderLayout.NORTH);

        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);

        JComboBox<String> fil = new JComboBox<>(
                new String[]{
                    "All",
                    "Pending",
                    "Accepted",
                    "On Going",
                    "Completed",
                    "Declined",
                    "Cancelled"
                }
        );

        JTextField[] q = new JTextField[1];

        Runnable fill = () -> {

            String s = q[0] == null
                    ? ""
                    : q[0].getText().toLowerCase().trim();

            List<Object[]> rows = new ArrayList<>();

            DB.reqs.stream()
                    .filter(r -> r != null)
                    .filter(r ->
                            fil.getSelectedIndex() == 0
                            || (r.status != null
                            && r.status.equals(fil.getSelectedItem()))
                    )
                    .filter(r ->
                            s.isEmpty()
                            || (r.id != null
                            && r.id.toLowerCase().contains(s))
                            || (r.uname != null
                            && r.uname.toLowerCase().contains(s))
                    )
                    .sorted((x, y) -> {
                        if (x.submitted == null) return 1;
                        if (y.submitted == null) return -1;
                        return y.submitted.compareTo(x.submitted);
                    })
                    .forEach(r -> rows.add(new Object[]{
                        r.id,
                        r.uname,
                        r.vehicle,
                        r.problem,
                        r.location,
                        r.status,
                        btn("View", BLUE,
                                e -> RequestDetailDialog.show(r, true))
                    }));

            host.removeAll();

            host.add(
                    grid(
                            new String[]{
                                "Request ID",
                                "User",
                                "Vehicle",
                                "Problem",
                                "Location",
                                "Status",
                                "Action"
                            },
                            new double[]{
                                1.5, 2, 1.7, 2, 2, 1, 1
                            },
                            rows
                    ),
                    BorderLayout.CENTER
            );

            host.revalidate();
            host.repaint();
        };

        q[0] = search("Search request ID...", fill);

        fil.addActionListener(e -> fill.run());

        JPanel c = new JPanel(new BorderLayout(0, 8));
        c.setOpaque(false);

        c.add(
                flow(
                        q[0],
                        new JLabel(" "),
                        lbl("Filter:", 12, false, txt()),
                        fil
                ),
                BorderLayout.NORTH
        );

        c.add(host, BorderLayout.CENTER);

        body.add(
                section("Request Queue", c, null),
                BorderLayout.CENTER
        );

        fill.run();

        return pg[0];
    }
}

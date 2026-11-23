
package varsystem.ui.common;

import java.time.LocalDateTime;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import varsystem.data.DB;
import varsystem.model.Req;
import static varsystem.ui.components.Dialogs.ask;
import static varsystem.ui.components.Ui.flow;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.APP;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.util.Formats.fmt;

/** Request details popup. Shops see it read-only; the admin can also change status or delete. */
public final class RequestDetailDialog {

    private RequestDetailDialog() { }

    public static void show(Req r, boolean admin) {

        JPanel p = vbox();

        String[][] kv = {
            {"Request ID", r.id},
            {"Customer", r.uname},
            {"Vehicle", r.vehicle},
            {"Problem", r.problem},
            {"Location", r.location},
            {"Shop", r.shop},
            {"Mechanic", r.mechanic.isEmpty() ? "-" : r.mechanic},
            {"Submitted", fmt(r.submitted)},
            {"Cost", "PHP " + (r.fee + r.travel)},
            {"Details", r.details.isEmpty() ? "-" : r.details}
        };

        for (String[] x : kv) {
            p.add(new JLabel(x[0] + ":  " + x[1]));
        }

        JComboBox<String> st = new JComboBox<>(
            new String[]{
                "Pending",
                "Accepted",
                "On Going",
                "Completed",
                "Declined",
                "Cancelled"
            }
        );

        st.setSelectedItem(r.status);

        if (admin) {
            p.add(Box.createVerticalStrut(8));
            p.add(flow(new JLabel("Status:"), st));
        }

        Object[] o = admin
            ? new Object[]{"Update Status", "Delete Request", "Close"}
            : new Object[]{"Close"};

        int c = JOptionPane.showOptionDialog(
            APP,
            p,
            "Request " + r.id,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            o,
            o[o.length - 1]
        );

        if (!admin) return;

        if (c == 0) {

            r.status = (String) st.getSelectedItem();

            if (r.status.equals("Accepted") && r.accepted == null) {
                r.accepted = LocalDateTime.now();
            }

            DB.log("Admin set " + r.id + " -> " + r.status);
            refresh();

        } else if (c == 1 && ask("Delete request " + r.id + " permanently?")) {

            DB.reqs.remove(r);
            DB.log("Admin deleted request " + r.id);
            refresh();
        }
    }
}

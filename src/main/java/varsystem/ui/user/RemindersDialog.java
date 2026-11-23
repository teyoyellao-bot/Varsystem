package varsystem.ui.user;

import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import varsystem.data.DB;
import static varsystem.data.Session.ME;
import static varsystem.ui.components.Dialogs.infoArea;
import static varsystem.ui.frame.MainFrame.APP;

public final class RemindersDialog {
    private RemindersDialog() { }

    public static void show() {
        JCheckBox cb = new JCheckBox("Enable service reminders", ME.remind);
        StringBuilder sb = new StringBuilder();
        DB.appts.stream().filter(a -> a[0].equalsIgnoreCase(ME.email) && !a[7].equals("Completed") && !a[7].equals("Cancelled"))
                .forEach(a -> sb.append("\u2022 ").append(a[3]).append(" at ").append(a[4]).append(" - ").append(a[5]).append(" ").append(a[6]).append("\n"));
        if (sb.length() == 0) sb.append("No upcoming appointments. Use 'Schedule services' to book one.");
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(cb, BorderLayout.NORTH);
        p.add(infoArea("Upcoming:\n" + sb, 7, 38), BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(APP, p, "Get Reminders", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            ME.remind = cb.isSelected();
        }
    }
}
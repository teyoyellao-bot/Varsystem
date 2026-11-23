package varsystem.ui.user;

import java.awt.Dimension;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import varsystem.data.DB;
import static varsystem.data.Session.ME;
import static varsystem.ui.components.Dialogs.form;
import static varsystem.ui.frame.MainFrame.APP;

public final class MaintenanceDialog {
    private MaintenanceDialog() { }

    public static void show() {
        while (true) {
            List<String[]> rows = DB.maint.stream().filter(m -> m[0].equalsIgnoreCase(ME.email)).map(m -> new String[]{m[1], m[2], m[3]}).collect(Collectors.toList());
            DefaultTableModel m = new DefaultTableModel(new String[]{"Maintenance Item", "Mileage (km)", "Date"}, 0);
            for (String[] x : rows) m.addRow((Object[]) x);
            JTable t = new JTable(m);
            t.setEnabled(false);
            JScrollPane sp = new JScrollPane(t);
            sp.setPreferredSize(new Dimension(520, 240));
            Object[] o = {"Add Entry", "Clear All", "Close"};
            int c = JOptionPane.showOptionDialog(APP, sp, "Track Maintenance", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, o, o[2]);
            switch (c) {
                case 0 -> {
                    String[] f = form(
                        "New maintenance entry",
                        new String[]{"Item", "Mileage (km)", "Date (yyyy-mm-dd)"},
                        new Object[]{"Oil Change", "", LocalDate.now().toString()}
                    );
                    if (f != null && !f[0].isEmpty()) {
                        DB.maint.add(new String[]{ME.email, f[0], f[1], f[2]});
                    }
                }
                case 1 -> DB.maint.removeIf(x -> x[0].equalsIgnoreCase(ME.email));
                default -> {
                    return;
                }
            }
        }
    }
}
package varsystem.ui.user;

import java.util.ArrayList;
import java.util.List;
import varsystem.data.DB;
import varsystem.model.Req;
import static varsystem.data.Session.ME;
import static varsystem.ui.components.Dialogs.tableDialog;
import static varsystem.util.Formats.DD;

public final class ServiceHistoryDialog {
    private ServiceHistoryDialog() { }

    public static void show() {
        List<String[]> rows = new ArrayList<>();
        for (Req r : DB.reqs) if (r.uemail.equalsIgnoreCase(ME.email))
            rows.add(new String[]{r.submitted.format(DD), r.id, r.problem, r.shop, "PHP " + (r.fee + r.travel), r.status});
        for (String[] a : DB.appts) if (a[0].equalsIgnoreCase(ME.email))
            rows.add(new String[]{a[5], "Appointment", a[3], a[4], "-", a[7]});
        tableDialog("Service History", new String[]{"Date", "Reference", "Service", "Shop", "Cost", "Status"}, rows);
    }
}
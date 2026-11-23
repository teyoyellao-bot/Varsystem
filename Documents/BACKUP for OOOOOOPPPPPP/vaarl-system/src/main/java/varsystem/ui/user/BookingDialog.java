
package varsystem.ui.user;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import varsystem.data.DB;
import static varsystem.data.Session.ME;
import static varsystem.ui.components.Dialogs.err;
import static varsystem.ui.components.Dialogs.form;
import static varsystem.ui.components.Dialogs.msg;

/** Book an appointment; the shop sees it in its schedule and the admin in the logs/notifications. */
public final class BookingDialog {
    private BookingDialog() { }

    public static void show(String preset) {
        List<String> sv = DB.svcs.stream().filter(x -> x.active).map(x -> x.name).collect(Collectors.toList());
        if (preset != null) {
            sv.remove(preset);
            sv.add(0, preset);
        }

        List<String> shops = DB.shops.stream()
                .filter(x -> x.status.equals("Active"))
                .map(x -> x.name)
                .collect(Collectors.toList());

        String[] r = form("Book an appointment",
                new String[]{"Service", "Repair shop", "Date (yyyy-mm-dd)", "Time", "Vehicle"},
                new Object[]{
                    sv.toArray(String[]::new),
                    shops.toArray(String[]::new),
                    LocalDate.now().plusDays(1).toString(),
                    new String[]{"08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
                        "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM"},
                    ME.vehicle
                });

        if (r == null) return;

        try {
            if (LocalDate.parse(r[2]).isBefore(LocalDate.now())) {
                err("Please choose today or a future date.");
                return;
            }
        } catch (Exception e) {
            err("Invalid date. Use the format yyyy-mm-dd.");
            return;
        }

        DB.appts.add(new String[]{
            ME.email, ME.name, r[4], r[0], r[1], r[2], r[3], "Pending", ""
        });

        DB.addNotification("New appointment: " + ME.name
                + " booked " + r[0] + " at " + r[1] + ".");
        DB.log("Appointment booked: " + ME.username + " - " + r[0]);

        msg("Appointment booked for " + r[2] + " at " + r[3]
                + " with " + r[1] + ".\nThe shop will confirm it shortly.");
    }
}

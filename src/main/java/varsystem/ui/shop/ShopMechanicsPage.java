
package varsystem.ui.shop;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import varsystem.data.DB;
import static varsystem.data.Session.MYSHOP;
import varsystem.model.Req;
import varsystem.model.User;
import static varsystem.ui.components.Dialogs.ask;
import static varsystem.ui.components.Dialogs.form;
import varsystem.ui.components.RoundedPanel;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.card;
import static varsystem.ui.components.Ui.flow;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.GREEN;
import static varsystem.ui.theme.Theme.ORANGE;
import static varsystem.ui.theme.Theme.RED;
import static varsystem.ui.theme.Theme.sub;
import static varsystem.ui.theme.Theme.txt;

public final class ShopMechanicsPage {

    private ShopMechanicsPage() { }

    // Check if a request is still active
    private static boolean isActive(Req r) {
        return r != null && r.status != null
                && (
                    r.status.equalsIgnoreCase("Pending")
                    || r.status.equalsIgnoreCase("Accepted")
                    || r.status.equalsIgnoreCase("On Going")
                );
    }

    // Get all requests assigned to a mechanic
    private static List<Req> getJobs(String mechanic) {

        List<Req> requests = new ArrayList<>();

        if (MYSHOP == null) {
            return requests;
        }

        for (Req r : DB.reqs) {

            if (r != null
                    && r.mechanic != null
                    && r.mechanic.equalsIgnoreCase(mechanic)
                    && r.shop != null
                    && r.shop.equalsIgnoreCase(MYSHOP.name)) {

                requests.add(r);
            }
        }

        return requests;
    }

    // Find mechanic account belonging to the current shop
    private static User findMechanicAccount(String mechanicName) {

        if (MYSHOP == null || DB.users == null) {
            return null;
        }

        for (User u : DB.users) {

            if (u != null
                    && "Mechanic".equalsIgnoreCase(u.role)
                    && u.shop != null
                    && u.shop.equalsIgnoreCase(MYSHOP.name)
                    && u.name != null
                    && u.name.equalsIgnoreCase(mechanicName)) {

                return u;
            }
        }

        return null;
    }

    // Edit mechanic account
    private static void editMechanic(String[] mechanic) {

        if (MYSHOP == null || mechanic == null || mechanic.length == 0) {
            return;
        }

        String oldName = mechanic[0];
        User account = findMechanicAccount(oldName);

        if (account == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "Mechanic account not found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String oldPhone = mechanic.length > 1 && mechanic[1] != null
                ? mechanic[1] : "";

        String oldUsername = mechanic.length > 2 && mechanic[2] != null
                ? mechanic[2] : "";

        String oldEmail = mechanic.length > 3 && mechanic[3] != null
                ? mechanic[3] : "";

        String[] r = form(
                "Edit Mechanic Account",
                new String[]{"Full Name", "Phone", "Email", "Username", "New Password (leave blank to keep current)"},
                new Object[]{
                    oldName,
                    oldPhone,
                    oldEmail,
                    oldUsername,
                    ""
                }
        );

        if (r == null) {
            return;
        }

        String newName = r[0].trim();
        String newPhone = r[1].trim();
        String newEmail = r[2].trim();
        String newUsername = r[3].trim();
        String newPassword = r[4];

        // Required fields
        if (newName.isEmpty()
                || newEmail.isEmpty()
                || newUsername.isEmpty()) {

            JOptionPane.showMessageDialog(
                    null,
                    "Name, email, and username are required."
            );
            return;
        }

        // Validate email
        if (!newEmail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {

            JOptionPane.showMessageDialog(
                    null,
                    "Enter a valid email address."
            );
            return;
        }

        // Check duplicate email / username
        for (User u : DB.users) {

            if (u == null || u == account) {
                continue;
            }

            if (u.email != null
                    && u.email.equalsIgnoreCase(newEmail)) {

                JOptionPane.showMessageDialog(
                        null,
                        "Email already exists."
                );
                return;
            }

            if (u.username != null
                    && u.username.equalsIgnoreCase(newUsername)) {

                JOptionPane.showMessageDialog(
                        null,
                        "Username already exists."
                );
                return;
            }
        }

        // Update account information
        account.name = newName;
        account.phone = newPhone;
        account.email = newEmail;
        account.username = newUsername;

        if (newPassword != null && !newPassword.isBlank()) {
            account.password = newPassword;
        }

        // Update mechanic list
        mechanic[0] = newName;

        if (mechanic.length > 1) {
            mechanic[1] = newPhone;
        }

        if (mechanic.length > 2) {
            mechanic[2] = newUsername;
        }

        if (mechanic.length > 3) {
            mechanic[3] = newEmail;
        }

        // Update assigned requests using old mechanic name
        for (Req req : DB.reqs) {

            if (req != null
                    && req.mechanic != null
                    && req.mechanic.equalsIgnoreCase(oldName)
                    && req.shop != null
                    && req.shop.equalsIgnoreCase(MYSHOP.name)) {

                req.mechanic = newName;
            }
        }

        DB.saveData();

        DB.log("Mechanic account updated: "
                + oldName + " -> " + newName
                + " for " + MYSHOP.name);

        JOptionPane.showMessageDialog(
                null,
                "Mechanic account updated successfully."
        );

        refresh();
    }

    public static JComponent build() {

        JButton add = btn("+ Add Mechanic", BLUE, e -> {

            String[] r = form(
                    "Register Mechanic Account",
                    new String[]{"Full Name", "Phone", "Email", "Username", "Password"},
                    new Object[]{"", "", "", "", ""}
            );

            if (r != null && MYSHOP != null) {

                for (User existing : DB.users) {

                    if (existing != null
                            && (
                                (existing.email != null
                                    && existing.email.equalsIgnoreCase(r[2].trim()))
                                ||
                                (existing.username != null
                                    && existing.username.equalsIgnoreCase(r[3].trim()))
                            )) {

                        JOptionPane.showMessageDialog(
                                null,
                                "Email or username already exists."
                        );
                        return;
                    }
                }

                if (r[0].trim().isEmpty()
                        || r[2].trim().isEmpty()
                        || r[3].trim().isEmpty()
                        || r[4].isBlank()) {

                    JOptionPane.showMessageDialog(
                            null,
                            "Name, email, username and password are required."
                    );
                    return;
                }

                if (!r[2].trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {

                    JOptionPane.showMessageDialog(
                            null,
                            "Enter a valid email address."
                    );
                    return;
                }

                User account = DB.mkUser(
                        r[0].trim(),
                        r[2].trim(),
                        r[3].trim(),
                        r[4],
                        "Mechanic",
                        r[1].trim(),
                        MYSHOP.location,
                        "",
                        "Active",
                        0
                );

                account.shop = MYSHOP.name;

                MYSHOP.mechs.add(new String[]{
                    r[0].trim(),
                    r[1].trim(),
                    r[3].trim(),
                    r[2].trim()
                });

                DB.saveData();

                DB.log(
                        "Mechanic account registered: "
                        + r[0] + " for " + MYSHOP.name
                );

                JOptionPane.showMessageDialog(
                        null,
                        "Mechanic account created successfully. They can now log in."
                );

                refresh();
            }
        });

        JComponent[] pg = page(
                "Mechanics",
                "Manage mechanics, assignments, and availability for your shop.",
                add
        );

        JPanel body = (JPanel) pg[1];

        JPanel g = new JPanel(new GridLayout(0, 3, 14, 14));
        g.setOpaque(false);

        if (MYSHOP != null) {

            for (String[] m : new ArrayList<>(MYSHOP.mechs)) {

                if (m == null || m.length == 0 || m[0] == null) {
                    continue;
                }

                String name = m[0];

                List<Req> requests = getJobs(name);

                long total = requests.size();

                long completed = requests.stream()
                        .filter(r -> r.status != null
                                && r.status.equalsIgnoreCase("Completed"))
                        .count();

                long active = requests.stream()
                        .filter(ShopMechanicsPage::isActive)
                        .count();

                long pending = requests.stream()
                        .filter(r -> r.status != null
                                && r.status.equalsIgnoreCase("Pending"))
                        .count();

                long ongoing = requests.stream()
                        .filter(r -> r.status != null
                                && r.status.equalsIgnoreCase("On Going"))
                        .count();

                long accepted = requests.stream()
                        .filter(r -> r.status != null
                                && r.status.equalsIgnoreCase("Accepted"))
                        .count();

                RoundedPanel c = card();

                c.setLayout(new BorderLayout(0, 8));
                c.setBorder(new EmptyBorder(14, 16, 14, 16));

                JPanel t = vbox();

                t.add(lbl(name, 16, true, txt()));

                t.add(lbl(
                        m.length > 1 ? m[1] : "",
                        12, false, sub()
                ));

                // Automatic availability
                t.add(lbl(
                        active == 0 ? "Available" : "Busy",
                        12, true,
                        active == 0 ? GREEN : ORANGE
                ));

                t.add(lbl("Total Jobs: " + total, 12, false, txt()));

                t.add(lbl("Completed: " + completed, 12, true, GREEN));

                t.add(lbl("Pending: " + pending, 12, false, sub()));

                t.add(lbl("Accepted: " + accepted, 12, false, sub()));

                t.add(lbl("Ongoing: " + ongoing, 12, false, sub()));

                c.add(t, BorderLayout.CENTER);

                c.add(flow(

                        btn("View", BLUE, e ->
                                showJobs(name, getJobs(name))
                        ),

                        btn("Edit", ORANGE, e ->
                                editMechanic(m)
                        ),

                        btn("Remove", RED, e -> {

                            if (ask("Remove mechanic " + name + "?")) {

                                MYSHOP.mechs.remove(m);

                                DB.users.removeIf(u -> u != null
                                        && "Mechanic".equalsIgnoreCase(u.role)
                                        && u.shop != null
                                        && u.shop.equalsIgnoreCase(MYSHOP.name)
                                        && u.name != null
                                        && u.name.equalsIgnoreCase(name));

                                DB.saveData();

                                DB.log("Mechanic removed: " + name);

                                refresh();
                            }
                        })

                ), BorderLayout.SOUTH);

                g.add(c);
            }
        }

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(g, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(wrap);

        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);

        body.add(sp, BorderLayout.CENTER);

        return pg[0];
    }

    // View mechanic job records
    private static void showJobs(
            String mechanic, List<Req> requests) {

        StringBuilder details = new StringBuilder();

        long completed = requests.stream()
                .filter(r -> r.status != null
                        && r.status.equalsIgnoreCase("Completed"))
                .count();

        long active = requests.stream()
                .filter(ShopMechanicsPage::isActive)
                .count();

        details.append("MECHANIC: ")
                .append(mechanic).append("\n");

        details.append("STATUS: ")
                .append(active == 0 ? "Available" : "Busy")
                .append("\n");

        details.append("TOTAL JOBS: ")
                .append(requests.size()).append("\n");

        details.append("COMPLETED JOBS: ")
                .append(completed).append("\n\n");

        if (requests.isEmpty()) {

            details.append("No assigned jobs yet.");

        } else {

            int number = 1;

            for (Req r : requests) {

                details.append("JOB #")
                        .append(number++).append("\n");

                details.append("Request ID: ")
                        .append(r.id).append("\n");

                details.append("Customer: ")
                        .append(r.uname).append("\n");

                details.append("Vehicle: ")
                        .append(r.vehicle).append("\n");

                details.append("Problem: ")
                        .append(r.problem).append("\n");

                details.append("Details: ")
                        .append(r.details).append("\n");

                details.append("Location: ")
                        .append(r.location).append("\n");

                details.append("Fee: PHP ")
                        .append(r.fee).append("\n");

                details.append("Status: ")
                        .append(r.status).append("\n");

                details.append("--------------------------------\n\n");
            }
        }

        JTextArea area = new JTextArea(details.toString());

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        area.setFont(new java.awt.Font(
                "Arial", java.awt.Font.PLAIN, 13
        ));

        area.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(area);

        scroll.setPreferredSize(
                new java.awt.Dimension(550, 420)
        );

        JOptionPane.showMessageDialog(
                null,
                scroll,
                mechanic + " - Job Records",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}

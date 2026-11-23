
package varsystem.ui.admin;

import java.awt.BorderLayout;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import varsystem.data.DB;
import static varsystem.data.Session.ME;
import varsystem.model.User;
import varsystem.service.PasswordSecurity;
import static varsystem.ui.components.Dialogs.*;
import varsystem.ui.components.Dialogs.PasswordInput;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.APP;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.util.Formats.DD;

public final class AdminUsersPage {

    private AdminUsersPage() { }

    private static String generateTemporaryPassword() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder(14);
        for (int i = 0; i < 14; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }
        return result.toString();
    }

    public static JComponent build() {

        // ADD USER BUTTON
        JButton add = btn("+ Add User", BLUE, e -> {

            String[] r = form(
                "Add user",
                new String[]{
                    "Full name",
                    "Email",
                    "Username",
                    "Password",
                    "Role"
                },
                new Object[]{
                    "",
                    "",
                    "",
                    new PasswordInput(),
                    new String[]{
                        "User",
                        "Shop Manager",
                        "Admin"
                    }
                }
            );

            if (r == null) return;

            // VALIDATION
            if (r[0].trim().isEmpty()
                    || r[1].trim().isEmpty()
                    || r[2].trim().isEmpty()
                    || r[3].trim().isEmpty()) {

                err("All fields are required.");
                return;
            }

            // CHECK DUPLICATE EMAIL AND USERNAME
            for (User u : DB.users) {

                if (u == null) continue;

                if ((u.email != null
                        && u.email.equalsIgnoreCase(r[1].trim()))
                        || (u.username != null
                        && u.username.equalsIgnoreCase(r[2].trim()))) {

                    err("Email or username already exists.");
                    return;
                }
            }

            // CREATE USER
            DB.mkUser(
                r[0].trim(),
                r[1].trim(),
                r[2].trim(),
                r[3],
                r[4],
                "",
                "",
                "",
                "Active",
                0
            );

            DB.log("Admin added user " + r[2]);

            refresh();
        });

        // PAGE
        JComponent[] pg = page(
            "Manage Users",
            "View, search, and manage registered users.",
            add
        );

        JPanel body = (JPanel) pg[1];

        List<User> us = DB.users;

        // USER STATISTICS
        body.add(
            stats(new String[][]{
                {"Total Users", "" + us.size()},

                {"Active Users", "" + us.stream()
                    .filter(u -> u != null
                    && "Active".equalsIgnoreCase(u.status))
                    .count()},

                {"Pending", "" + us.stream()
                    .filter(u -> u != null
                    && "Pending".equalsIgnoreCase(u.status))
                    .count()},

                {"Suspended", "" + us.stream()
                    .filter(u -> u != null
                    && "Suspended".equalsIgnoreCase(u.status))
                    .count()}
            }),
            BorderLayout.NORTH
        );

        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);

        JTextField[] q = new JTextField[1];

        // LOAD AND SEARCH USERS
        Runnable fill = () -> {

            String s = q[0] == null
                ? ""
                : q[0].getText().toLowerCase().trim();

            List<Object[]> rows = new ArrayList<>();

            for (User u : DB.users) {

                if (u == null) continue;

                String name = safe(u.name);
                String email = safe(u.email);
                String username = safe(u.username);
                String role = safe(u.role);
                String status = safe(u.status);

                if (!s.isEmpty()
                        && !name.toLowerCase().contains(s)
                        && !email.toLowerCase().contains(s)
                        && !username.toLowerCase().contains(s)) {

                    continue;
                }

                rows.add(new Object[]{
                    name,
                    email,
                    role,
                    u.joined == null
                        ? "-"
                        : u.joined.format(DD),
                    status,
                    btn("View", BLUE, e -> userDialog(u))
                });
            }

            host.removeAll();

            host.add(
                grid(
                    new String[]{
                        "User",
                        "Email",
                        "Role",
                        "Joined",
                        "Status",
                        "Action"
                    },
                    new double[]{
                        2, 3, 1.5, 1.5, 1, 1
                    },
                    rows
                ),
                BorderLayout.CENTER
            );

            host.revalidate();
            host.repaint();
        };

        // SEARCH FIELD
        q[0] = search("Search users...", fill);

        JPanel c = new JPanel(new BorderLayout(0, 8));
        c.setOpaque(false);

        c.add(flow(q[0]), BorderLayout.NORTH);
        c.add(host, BorderLayout.CENTER);

        body.add(
            section("User Directory", c, null),
            BorderLayout.CENTER
        );

        fill.run();

        return pg[0];
    }

    // USER DETAILS AND ADMIN ACTIONS
    static void userDialog(User u) {

        if (u == null) return;

        JPanel p = vbox();

        String[][] kv = {
            {"Name", safe(u.name)},
            {"Email", safe(u.email)},
            {"Username", safe(u.username)},
            {"Role", safe(u.role)},
            {"Phone", emptyAsDash(u.phone)},
            {"Location", emptyAsDash(u.location)},
            {"Vehicle", emptyAsDash(u.vehicle)},
            {"Shop", emptyAsDash(u.shop)},
            {"Joined", u.joined == null
                ? "-"
                : u.joined.format(DD)},
            {"Status", safe(u.status)}
        };

        for (String[] x : kv) {
            p.add(new JLabel(x[0] + ":  " + x[1]));
        }

        Object[] o = {
            "Activate",
            "Suspend",
            "Reset Password",
            "Delete",
            "Close"
        };

        int c = JOptionPane.showOptionDialog(
            APP,
            p,
            "User - " + safe(u.name),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            o,
            o[4]
        );

        // ADMIN ACTIONS
        switch (c) {

            // ACTIVATE
            case 0:

                u.status = "Active";

                DB.log("Admin activated " + u.username);
                break;

            // SUSPEND
            case 1:

                if (u == ME) {
                    err("You cannot suspend your own account.");
                    return;
                }

                u.status = "Suspended";

                DB.log("Admin suspended " + u.username);
                break;

            // RESET PASSWORD
            case 2:

                String temporaryPassword = generateTemporaryPassword();
                u.password = PasswordSecurity.hash(temporaryPassword);

                msg("A temporary password was generated:\n\n"
                    + temporaryPassword
                    + "\n\nGive this password to the verified account owner "
                    + "through a trusted contact method. Ask them to change it after login.");

                DB.log(
                    "Admin reset password for " + u.username
                );
                break;

            // DELETE USER
            case 3:

                if (u == ME) {
                    err("You cannot delete your own account.");
                    return;
                }

                if (!ask("Delete user " + safe(u.name) + "?")) {
                    return;
                }

                DB.users.remove(u);

                DB.log("Admin deleted " + u.username);
                break;

            // CLOSE OR CANCEL
            case 4:
            default:
                return;
        }

        refresh();
    }

    // SAFE STRING
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // EMPTY STRING TO DASH
    private static String emptyAsDash(String value) {

        return value == null || value.trim().isEmpty()
            ? "-"
            : value;
    }
}


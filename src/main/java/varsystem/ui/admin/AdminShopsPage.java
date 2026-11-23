
package varsystem.ui.admin;

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import varsystem.data.DB;
import varsystem.model.Req;
import varsystem.model.Shop;
import varsystem.model.User;
import static varsystem.ui.components.Dialogs.ask;
import static varsystem.ui.components.Dialogs.err;
import static varsystem.ui.components.Dialogs.form;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.flow;
import static varsystem.ui.components.Ui.grid;
import static varsystem.ui.components.Ui.search;
import static varsystem.ui.components.Ui.section;
import static varsystem.ui.components.Ui.stats;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.APP;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;

public final class AdminShopsPage {

    private AdminShopsPage() {
    }

    // =========================================
    // BUILD PAGE
    // =========================================

    public static JComponent build() {

        // ADD SHOP
        JButton add = btn("+ Add Shop", BLUE, e -> {

            String[] r = form(
                    "Add repair shop",
                    new String[]{
                        "Shop name",
                        "Location",
                        "Phone",
                        "Distance from center (km)"
                    },
                    new Object[]{"", "", "", "2.0"}
            );

            if (r == null) return;

            if (r[0].trim().isEmpty()
                    || r[1].trim().isEmpty()) {

                err("Shop name and location are required.");
                return;
            }

            double d = 2;

            try {
                d = Double.parseDouble(r[3]);
            } catch (NumberFormatException ex) {
                // Keep default distance.
            }

            DB.mkShop(
                    r[0],
                    r[1],
                    0, 0, d,
                    r[2],
                    "Active",
                    0.2 + Math.random() * 0.65,
                    0.15 + Math.random() * 0.7
            );

            DB.log("Admin added shop " + r[0]);

            DB.saveData();

            refresh();
        });

        JComponent[] pg = page(
                "Manage Repair Shops",
                "Review repair shops, ratings, locations, and account status.",
                add
        );

        JPanel body = (JPanel) pg[1];

        List<Shop> ss = DB.shops;

        // =========================================
        // SHOP STATISTICS
        // =========================================

        body.add(
                stats(new String[][]{
                    {"Registered Shops", "" + ss.size()},
                    {"Active", "" + ss.stream()
                            .filter(s -> s != null
                                    && "Active".equalsIgnoreCase(s.status))
                            .count()},
                    {"Pending Review", "" + ss.stream()
                            .filter(s -> s != null
                                    && "Pending".equalsIgnoreCase(s.status))
                            .count()},
                    {"Inactive", "" + ss.stream()
                            .filter(s -> s != null
                                    && "Inactive".equalsIgnoreCase(s.status))
                            .count()}
                }),
                BorderLayout.NORTH
        );

        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);

        JTextField[] q = new JTextField[1];

        // =========================================
        // SEARCH AND DISPLAY SHOPS
        // =========================================

        Runnable fill = () -> {

            String search = q[0] == null
                    ? ""
                    : q[0].getText().toLowerCase().trim();

            List<Object[]> rows = new ArrayList<>();

            for (Shop x : DB.shops) {

                if (x == null) continue;

                String name = x.name == null ? "" : x.name;
                String location = x.location == null ? "" : x.location;

                if (!search.isEmpty()
                        && !name.toLowerCase().contains(search)
                        && !location.toLowerCase().contains(search)) {
                    continue;
                }

                int serviceCount = x.services == null
                        ? 0 : x.services.size();

                rows.add(new Object[]{
                    name,
                    location,
                    x.rating + " ★",
                    String.valueOf(serviceCount),
                    x.status == null ? "Unknown" : x.status,
                    btn("View", BLUE, e -> shopAdminDialog(x))
                });
            }

            host.removeAll();

            host.add(
                    grid(
                            new String[]{
                                "Shop Name",
                                "Location",
                                "Rating",
                                "Services",
                                "Status",
                                "Action"
                            },
                            new double[]{3, 3, 1, 1, 1, 1},
                            rows
                    ),
                    BorderLayout.CENTER
            );

            host.revalidate();
            host.repaint();
        };

        q[0] = search("Search shops...", fill);

        JPanel c = new JPanel(new BorderLayout(0, 8));
        c.setOpaque(false);

        c.add(flow(q[0]), BorderLayout.NORTH);
        c.add(host, BorderLayout.CENTER);

        body.add(
                section("Repair Shop Directory", c, null),
                BorderLayout.CENTER
        );

        fill.run();

        return pg[0];
    }

    // =========================================
    // ADMIN SHOP VERIFICATION WINDOW
    // =========================================

    static void shopAdminDialog(Shop s) {

        if (s == null) return;

        JPanel p = vbox();

        String services = s.services == null
                ? ""
                : String.join(", ", s.services);

        int mechanicCount = s.mechs == null
                ? 0 : s.mechs.size();

        String[][] kv = {
            {"Shop", safe(s.name)},
            {"Location", safe(s.location)},
            {"Phone", safe(s.phone)},
            {"Hours", safe(s.hours)},
            {"Rating", s.rating + " (" + s.reviews + " reviews)"},
            {"Services", services.isEmpty() ? "No services" : services},
            {"Mechanics", String.valueOf(mechanicCount)},
            {"Owner Email", safe(s.owner)},
            {"Status", safe(s.status)}
        };

        for (String[] x : kv) {
            p.add(new JLabel(x[0] + ":  " + x[1]));
        }

        // DISPLAY UPLOADED ID PICTURE
        p.add(Box.createVerticalStrut(10));
        p.add(new JLabel("Shop Owner ID Picture:"));

        JLabel idImage = new JLabel();
        idImage.setHorizontalAlignment(SwingConstants.CENTER);

        if (s.idPicturePath != null
                && !s.idPicturePath.isBlank()
                && new File(s.idPicturePath).isFile()) {

            ImageIcon original = new ImageIcon(s.idPicturePath);

            Image scaled = original.getImage().getScaledInstance(
                    420,
                    260,
                    Image.SCALE_SMOOTH
            );

            idImage.setIcon(new ImageIcon(scaled));

        } else {
            idImage.setText("No ID picture uploaded.");
        }

        p.add(idImage);

        JScrollPane scroll = new JScrollPane(p);
        scroll.setPreferredSize(new java.awt.Dimension(480, 430));

        Object[] options = {
            "Accept",
            "Reject",
            "Deactivate",
            "Edit",
            "Remove",
            "Close"
        };

        int c = JOptionPane.showOptionDialog(
                APP,
                scroll,
                "Repair Shop Verification",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[5]
        );

        switch (c) {

            // ACCEPT SHOP APPLICATION
            case 0 -> {

                if (s.idPicturePath == null
                        || s.idPicturePath.isBlank()
                        || !new File(s.idPicturePath).isFile()) {

                    err("Cannot accept this shop. No valid ID picture was uploaded.");
                    return;
                }

                s.status = "Active";

                updateShopManagerStatus(s, "Active");

                DB.addNotification(
                        "Shop application approved: " + s.name
                );

                varsystem.service.NotificationService
                        .shopApplicationResult(s.name, true);

                DB.log("Admin approved shop " + s.name);
            }

            // REJECT SHOP APPLICATION
            case 1 -> {

                if (!ask("Are you sure you want to reject " + s.name + "?")) {
                    return;
                }

                s.status = "Rejected";

                updateShopManagerStatus(s, "Rejected");

                DB.addNotification(
                        "Shop application rejected: " + s.name
                );

                varsystem.service.NotificationService
                        .shopApplicationResult(s.name, false);

                DB.log("Admin rejected shop " + s.name);
            }

            // DEACTIVATE SHOP
            case 2 -> {

                s.status = "Inactive";

                updateShopManagerStatus(s, "Inactive");

                DB.log("Admin deactivated shop " + s.name);
            }

            // EDIT SHOP
            case 3 -> {

                String[] r = form(
                        "Edit shop",
                        new String[]{
                            "Name",
                            "Location",
                            "Phone",
                            "Services (comma separated)"
                        },
                        new Object[]{
                            safe(s.name),
                            safe(s.location),
                            safe(s.phone),
                            services
                        }
                );

                if (r == null) return;

                String old = s.name;

                s.name = r[0].trim().isEmpty()
                        ? old : r[0].trim();

                s.location = r[1];
                s.phone = r[2];

                s.services = Arrays.stream(r[3].split(","))
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .collect(Collectors.toList());

                // UPDATE REQUEST SHOP REFERENCES
                for (Req q : DB.reqs) {

                    if (q != null
                            && q.shop != null
                            && old != null
                            && q.shop.equalsIgnoreCase(old)) {

                        q.shop = s.name;
                    }
                }

                // UPDATE USER SHOP REFERENCES
                for (User u : DB.users) {

                    if (u != null
                            && u.shop != null
                            && old != null
                            && u.shop.equalsIgnoreCase(old)) {

                        u.shop = s.name;
                    }
                }

                DB.log("Admin edited shop " + old);
            }

            // REMOVE SHOP
            case 4 -> {

                if (!ask("Remove " + s.name + "?")) {
                    return;
                }

                DB.shops.remove(s);

                DB.log("Admin removed shop " + s.name);
            }

            // CLOSE
            default -> {
                return;
            }
        }

        DB.saveData();

        refresh();
    }

    // =========================================
    // UPDATE SHOP MANAGER ACCOUNT STATUS
    // =========================================

    private static void updateShopManagerStatus(
            Shop shop,
            String status) {

        if (shop == null || shop.name == null) return;

        for (User u : DB.users) {

            if (u != null
                    && "Shop Manager".equalsIgnoreCase(u.role)
                    && u.shop != null
                    && shop.name.equalsIgnoreCase(u.shop)) {

                u.status = status;
            }
        }
    }

    // =========================================
    // SAFE STRING VALUE
    // =========================================

    private static String safe(String value) {
        return value == null ? "N/A" : value;
    }
}

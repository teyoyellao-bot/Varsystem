
package varsystem.ui.admin;

import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import varsystem.data.DB;
import varsystem.model.Req;
import varsystem.model.Svc;
import static varsystem.ui.components.Dialogs.*;
import varsystem.ui.components.Dialogs.TextAreaInput;
import varsystem.ui.components.RoundedPanel;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.*;

public final class AdminServicesPage {

    private AdminServicesPage() { }

    static Svc SEL;

    private static LocalDate selectedDate = LocalDate.now();
    private static String selectedShop = "All Shops";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    // =========================================
    // GET AVAILABLE YEARS
    // =========================================

    private static TreeSet<Integer> getYears() {

        TreeSet<Integer> years = new TreeSet<>();

        years.add(LocalDate.now().getYear());

        for (Req r : DB.reqs) {
            if (r != null && r.submitted != null) {
                years.add(r.submitted.getYear());
            }
        }

        return years;
    }

    // =========================================
    // CHECK SELECTED SHOP
    // =========================================

    private static boolean matchesSelectedShop(Req r) {

        if (r == null) return false;

        if ("All Shops".equals(selectedShop)) {
            return true;
        }

        return r.shop != null
                && r.shop.equalsIgnoreCase(selectedShop);
    }

    // =========================================
    // COUNT REQUESTS PER SERVICE
    // =========================================

    static int svcCount(Svc s) {

        if (s == null) return 0;

        return (int) DB.reqs.stream()
                .filter(r -> r != null
                        && r.submitted != null
                        && r.problem != null
                        && s.name != null
                        && r.problem.equalsIgnoreCase(s.name)
                        && r.submitted.toLocalDate().equals(selectedDate)
                        && matchesSelectedShop(r))
                .count();
    }

    // =========================================
    // COUNT TOTAL REQUESTS
    // =========================================

    private static long totalRequests() {

        return DB.reqs.stream()
                .filter(r -> r != null
                        && r.submitted != null
                        && r.submitted.toLocalDate().equals(selectedDate)
                        && matchesSelectedShop(r))
                .count();
    }

    // =========================================
    // CALENDAR DATE PICKER
    // =========================================

    private static LocalDate chooseDate(
            Component parent,
            LocalDate initialDate) {

        Window owner =
                SwingUtilities.getWindowAncestor(parent);

        final JDialog dialog = new JDialog(
                owner,
                "Select Date",
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setLayout(new BorderLayout());

        JPanel main = new JPanel(
                new BorderLayout(8, 10)
        );

        main.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        String[] months = {
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        };

        JPanel top = new JPanel(
                new FlowLayout(FlowLayout.CENTER)
        );

        JComboBox<String> monthBox =
                new JComboBox<>(months);

        TreeSet<Integer> years = getYears();

        for (int y = initialDate.getYear() - 5;
             y <= initialDate.getYear() + 5; y++) {

            years.add(y);
        }

        JComboBox<Integer> yearBox =
                new JComboBox<>(
                        years.toArray(Integer[]::new)
                );

        monthBox.setSelectedIndex(
                initialDate.getMonthValue() - 1
        );

        yearBox.setSelectedItem(
                initialDate.getYear()
        );

        top.add(new JLabel("Month:"));
        top.add(monthBox);
        top.add(new JLabel("Year:"));
        top.add(yearBox);

        main.add(top, BorderLayout.NORTH);

        JPanel calendar = new JPanel(
                new GridLayout(0, 7, 5, 5)
        );

        main.add(calendar, BorderLayout.CENTER);

        JPanel bottom = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        JButton todayButton = new JButton("Today");
        JButton cancelButton = new JButton("Cancel");

        bottom.add(todayButton);
        bottom.add(cancelButton);

        main.add(bottom, BorderLayout.SOUTH);

        final LocalDate[] result = {null};

        final LocalDate[] highlightedDate = {
            initialDate
        };

        // =====================================
        // RENDER CALENDAR
        // =====================================

        Runnable renderCalendar = () -> {

            calendar.removeAll();

            String[] weekdays = {
                "Mon", "Tue", "Wed", "Thu",
                "Fri", "Sat", "Sun"
            };

            for (String day : weekdays) {

                JLabel label = new JLabel(
                        day,
                        SwingConstants.CENTER
                );

                label.setFont(
                        new Font("SansSerif", Font.BOLD, 12)
                );

                calendar.add(label);
            }

            int month = monthBox.getSelectedIndex() + 1;

            Integer selectedYear =
                    (Integer) yearBox.getSelectedItem();

            if (selectedYear == null) return;

            int year = selectedYear;

            YearMonth ym = YearMonth.of(year, month);

            int firstDay =
                    ym.atDay(1).getDayOfWeek().getValue();

            int daysInMonth = ym.lengthOfMonth();

            for (int i = 1; i < firstDay; i++) {
                calendar.add(new JLabel(""));
            }

            for (int day = 1; day <= daysInMonth; day++) {

                final int selectedDay = day;
                final int selectedMonth = month;
                final int selectedYearValue = year;

                LocalDate buttonDate = LocalDate.of(
                        year, month, day
                );

                JButton dayButton =
                        new JButton(String.valueOf(day));

                dayButton.setUI(new BasicButtonUI());

                dayButton.setFont(
                        new Font("SansSerif", Font.BOLD, 14)
                );

                dayButton.setFocusPainted(false);
                dayButton.setFocusable(false);
                dayButton.setOpaque(true);
                dayButton.setContentAreaFilled(true);
                dayButton.setBorderPainted(true);

                dayButton.setHorizontalAlignment(
                        SwingConstants.CENTER
                );

                dayButton.setVerticalAlignment(
                        SwingConstants.CENTER
                );

                dayButton.setMargin(
                        new Insets(5, 5, 5, 5)
                );

                // SLIGHTLY DARKER LIGHT BLUE SELECTED DATE

                if (highlightedDate[0] != null
                        && highlightedDate[0].equals(buttonDate)) {

                    dayButton.setBackground(
                            new Color(205, 225, 250)
                    );

                    dayButton.setForeground(
                            new Color(35, 75, 125)
                    );

                    dayButton.setBorder(
                            BorderFactory.createLineBorder(
                                    new Color(165, 195, 235),
                                    1
                            )
                    );

                } else {

                    dayButton.setBackground(Color.WHITE);
                    dayButton.setForeground(Color.BLACK);

                    dayButton.setBorder(
                            BorderFactory.createLineBorder(
                                    new Color(220, 220, 220),
                                    1
                            )
                    );
                }

                // SELECT DATE

                dayButton.addActionListener(e -> {

                    result[0] = LocalDate.of(
                            selectedYearValue,
                            selectedMonth,
                            selectedDay
                    );

                    highlightedDate[0] = result[0];

                    dialog.dispose();
                });

                calendar.add(dayButton);
            }

            calendar.revalidate();
            calendar.repaint();
        };

        monthBox.addActionListener(
                e -> renderCalendar.run()
        );

        yearBox.addActionListener(
                e -> renderCalendar.run()
        );

        todayButton.addActionListener(e -> {

            result[0] = LocalDate.now();

            dialog.dispose();
        });

        cancelButton.addActionListener(
                e -> dialog.dispose()
        );

        renderCalendar.run();

        dialog.setContentPane(main);
        dialog.setSize(430, 370);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return result[0];
    }

    // =========================================
    // BUILD ADMIN SERVICES PAGE
    // =========================================

    public static JComponent build() {

        // ADD SERVICE BUTTON

        JButton add = btn(
                "+ Add Service",
                BLUE,
                e -> {

                    String[] r = form(
                            "Add service",
                            new String[]{
                                "Name",
                                "Category",
                                "Description"
                            },
                            new Object[]{
                                "",
                                "Roadside Assistance",
                                new TextAreaInput("")
                            }
                    );

                    if (r == null || r[0].trim().isEmpty()) {
                        return;
                    }

                    SEL = DB.mkSvc(
                            r[0], r[1], r[2], 0, true
                    );

                    DB.log("Admin added service " + r[0]);

                    refresh();
                }
        );

        JComponent[] pg = page(
                "Services",
                "Configure the services available in the locator.",
                add
        );

        JPanel body = (JPanel) pg[1];

        // SELECTED SERVICE

        if (SEL == null || !DB.svcs.contains(SEL)) {

            SEL = DB.svcs.isEmpty()
                    ? null
                    : DB.svcs.get(0);
        }

        // =========================================
        // TOP FILTER SECTION
        // =========================================

        JPanel filterPanel = new JPanel(
                new BorderLayout(10, 8)
        );

        filterPanel.setOpaque(false);

        filterPanel.setBorder(
                new EmptyBorder(0, 0, 12, 0)
        );

        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 5)
        );

        controls.setOpaque(false);

        // DATE FILTER

        JLabel dateLabel =
                lbl("Select Date:", 13, true, txt());

        JButton dateButton = btn(
                selectedDate.format(DATE_FORMAT),
                BLUE,
                e -> {

                    LocalDate picked =
                            chooseDate(pg[0], selectedDate);

                    if (picked != null) {

                        selectedDate = picked;

                        refresh();
                    }
                }
        );

        // SHOP FILTER

        JLabel shopLabel =
                lbl("Select Shop:", 13, true, txt());

        List<String> shopNames = new ArrayList<>();

        shopNames.add("All Shops");

        for (var shop : DB.shops) {

            if (shop != null
                    && shop.name != null
                    && !shopNames.contains(shop.name)) {

                shopNames.add(shop.name);
            }
        }

        JComboBox<String> shopBox =
                new JComboBox<>(
                        shopNames.toArray(String[]::new)
                );

        if (!shopNames.contains(selectedShop)) {
            selectedShop = "All Shops";
        }

        shopBox.setSelectedItem(selectedShop);

        shopBox.addActionListener(e -> {

            selectedShop =
                    (String) shopBox.getSelectedItem();

            refresh();
        });

        controls.add(dateLabel);
        controls.add(dateButton);
        controls.add(shopLabel);
        controls.add(shopBox);

        // =========================================
        // TOTAL REQUESTS CARD
        // =========================================

        JPanel totalPanel = new JPanel();

        totalPanel.setLayout(
                new BoxLayout(
                        totalPanel,
                        BoxLayout.Y_AXIS
                )
        );

        totalPanel.setOpaque(false);

        totalPanel.setBorder(
                new EmptyBorder(5, 10, 5, 10)
        );

        totalPanel.add(
                lbl("TOTAL REQUESTS", 12, true, sub())
        );

        totalPanel.add(
                lbl(
                        String.valueOf(totalRequests()),
                        24,
                        true,
                        BLUE
                )
        );

        totalPanel.add(
                lbl(
                        selectedDate.format(DATE_FORMAT)
                                + " | " + selectedShop,
                        11,
                        false,
                        sub()
                )
        );

        filterPanel.add(controls, BorderLayout.WEST);
        filterPanel.add(totalPanel, BorderLayout.EAST);

        body.add(filterPanel, BorderLayout.NORTH);

        // =========================================
        // SERVICE CATALOG
        // =========================================

        List<Object[]> rows = new ArrayList<>();

        for (Svc s : DB.svcs) {

            if (s == null) continue;

            String buttonText =
                    s == SEL ? "Selected" : "Select";

            JButton selectButton = btn(
                    buttonText,
                    BLUE,
                    e -> {

                        SEL = s;

                        refresh();
                    }
            );

            selectButton.setFocusable(false);

            rows.add(new Object[]{
                s.name,
                String.valueOf(svcCount(s)),
                s.active ? "Active" : "Inactive",
                selectButton
            });
        }

        RoundedPanel cat = section(
                "Service Catalog (" + DB.svcs.size() + " services)",

                grid(
                        new String[]{
                            "Service",
                            "Requests",
                            "Status",
                            "Action"
                        },

                        new double[]{
                            3, 1, 1, 1
                        },

                        rows
                ),

                null
        );

        // =========================================
        // SERVICE DETAILS
        // =========================================

        JPanel d = new JPanel(
                new BorderLayout(12, 15)
        );

        d.setOpaque(false);

        d.setBorder(
                new EmptyBorder(12, 12, 12, 12)
        );

        if (SEL != null) {

            Svc s = SEL;

            List<Req> serviceRequests = new ArrayList<>();

            for (Req r : DB.reqs) {

                if (r != null
                        && r.submitted != null
                        && r.problem != null
                        && s.name != null
                        && r.problem.equalsIgnoreCase(s.name)
                        && r.submitted.toLocalDate().equals(selectedDate)
                        && matchesSelectedShop(r)) {

                    serviceRequests.add(r);
                }
            }

            long tot = serviceRequests.size();

            long done = serviceRequests.stream()
                    .filter(r ->
                            "Completed".equalsIgnoreCase(r.status))
                    .count();

            int completion = tot == 0
                    ? 0
                    : (int) (done * 100 / tot);

            JPanel content = new JPanel();

            content.setLayout(
                    new BoxLayout(
                            content,
                            BoxLayout.Y_AXIS
                    )
            );

            content.setOpaque(false);

            JLabel nameLabel = lbl(
                    s.name, 22, true, txt()
            );

            nameLabel.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            content.add(nameLabel);
            content.add(Box.createVerticalStrut(15));

            // REQUEST STATISTICS

            JPanel stats = new JPanel(
                    new GridLayout(2, 1, 0, 8)
            );

            stats.setOpaque(false);

            stats.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            stats.add(
                    lbl(
                            "Total Requests: " + tot,
                            14,
                            true,
                            BLUE
                    )
            );

            stats.add(
                    lbl(
                            "Completion Rate: " + completion + "%",
                            14,
                            true,
                            BLUE
                    )
            );

            content.add(stats);
            content.add(Box.createVerticalStrut(18));

            // DESCRIPTION

            JLabel descTitle = lbl(
                    "Description", 13, true, txt()
            );

            descTitle.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            content.add(descTitle);
            content.add(Box.createVerticalStrut(6));

            JTextArea ds = new JTextArea(
                    s.desc == null ? "" : s.desc
            );

            ds.setEditable(false);
            ds.setFocusable(false);
            ds.setLineWrap(true);
            ds.setWrapStyleWord(true);
            ds.setOpaque(false);

            ds.setFont(
                    new Font("SansSerif", Font.PLAIN, 13)
            );

            ds.setForeground(txt());

            ds.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            ds.setBorder(
                    new EmptyBorder(3, 0, 3, 0)
            );

            ds.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 100)
            );

            content.add(ds);
            content.add(Box.createVerticalStrut(20));

            // CATEGORY

            JLabel categoryLabel = lbl(
                    "Category: " + s.category,
                    13,
                    true,
                    txt()
            );

            categoryLabel.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            content.add(categoryLabel);

            d.add(content, BorderLayout.CENTER);

            // =====================================
            // ACTION BUTTONS
            // =====================================

            JPanel actions = new JPanel(
                    new FlowLayout(
                            FlowLayout.LEFT, 8, 0
                    )
            );

            actions.setOpaque(false);

            // EDIT SERVICE

            actions.add(
                    btn(
                            "Edit Service",
                            BLUE,
                            e -> {

                                String[] r = form(
                                        "Edit service",
                                        new String[]{
                                            "Name",
                                            "Category",
                                            "Description"
                                        },
                                        new Object[]{
                                            s.name,
                                            s.category,
                                            new TextAreaInput(s.desc)
                                        }
                                );

                                if (r == null
                                        || r[0].trim().isEmpty()) {
                                    return;
                                }

                                String old = s.name;

                                s.name = r[0];
                                s.category = r[1];
                                s.desc = r[2];

                                for (Req q : DB.reqs) {

                                    if (q != null
                                            && q.problem != null
                                            && q.problem.equals(old)) {

                                        q.problem = s.name;
                                    }
                                }

                                DB.log(
                                        "Admin edited service " + old
                                );

                                refresh();
                            }
                    )
            );

            // ACTIVATE / DEACTIVATE

            actions.add(
                    btn(
                            s.active
                                    ? "Deactivate"
                                    : "Activate",

                            s.active ? ORANGE : GREEN,

                            e -> {

                                s.active = !s.active;

                                DB.log(
                                        "Admin toggled service " + s.name
                                );

                                refresh();
                            }
                    )
            );

            // DELETE SERVICE

            actions.add(
                    btn(
                            "Delete",
                            RED,
                            e -> {

                                if (ask(
                                        "Delete service " + s.name + "?"
                                )) {

                                    DB.svcs.remove(s);

                                    SEL = null;

                                    DB.log(
                                            "Admin deleted service " + s.name
                                    );

                                    refresh();
                                }
                            }
                    )
            );

            d.add(actions, BorderLayout.SOUTH);

        } else {

            d.add(
                    lbl(
                            "No service selected.",
                            13,
                            false,
                            sub()
                    ),
                    BorderLayout.CENTER
            );
        }

        // =========================================
        // SERVICE DETAILS PANEL
        // =========================================

        RoundedPanel det = section(
                "Service Details",
                d,
                null
        );

        // =========================================
        // FINAL LAYOUT
        // =========================================

        body.add(cat, BorderLayout.CENTER);

        JPanel east = new JPanel(
                new BorderLayout()
        );

        east.setOpaque(false);

        east.setBorder(
                new EmptyBorder(0, 14, 0, 0)
        );

        east.add(det, BorderLayout.CENTER);

        body.add(east, BorderLayout.EAST);

        return pg[0];
    }
}

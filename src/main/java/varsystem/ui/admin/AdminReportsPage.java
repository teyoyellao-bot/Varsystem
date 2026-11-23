
package varsystem.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import varsystem.data.DB;
import varsystem.model.Req;
import varsystem.model.Shop;
import static varsystem.service.RequestService.*;
import varsystem.ui.components.Chart;
import static varsystem.ui.components.Dialogs.exportCsv;
import varsystem.ui.components.RoundedPanel;
import static varsystem.ui.components.Ui.*;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.theme.Theme.*;
import static varsystem.util.Formats.DT;

public final class AdminReportsPage {

    private AdminReportsPage() { }

    public static JComponent build() {

        LocalDate[] selectedDate = {null};
        String[] selectedShop = {"All Shops"};

        // =========================================
        // EXPORT BUTTON
        // =========================================

        JButton ex = btn(
                "Export Report",
                BLUE,
                e -> { }
        );

        JComponent[] pg = page(
                "Reports & Analytics",
                "Analyze requests, service demand, and repair shop activity.",
                ex
        );

        JPanel body = (JPanel) pg[1];

        pg[0].setOpaque(true);
        pg[0].setBackground(pageBg());

        body.setOpaque(true);
        body.setBackground(pageBg());

        // =========================================
        // FILTER PANEL
        // =========================================

        JPanel filterPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 12, 8)
        );

        filterPanel.setOpaque(true);
        filterPanel.setBackground(cardBg());

        filterPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(cardLine()),
                        new EmptyBorder(5, 10, 5, 10)
                )
        );

        // DATE FILTER

        JLabel dateLabel = lbl(
                "Select Date:",
                12,
                true,
                Color.WHITE
        );

        JButton dateButton = btn(
                "Select Date",
                BLUE,
                e -> { }
        );

        dateButton.setPreferredSize(
                new Dimension(125, 34)
        );

        JButton clearDateButton = btn(
                "Clear Date",
                new Color(105, 115, 130),
                e -> { }
        );

        clearDateButton.setPreferredSize(
                new Dimension(105, 34)
        );

        // SHOP FILTER

        JLabel shopLabel = lbl(
                "Select Shop:",
                12,
                true,
                Color.WHITE
        );

        JComboBox<String> shopFilter = new JComboBox<>();

        shopFilter.addItem("All Shops");

        Set<String> shopNames = new LinkedHashSet<>();

        for (Shop shop : DB.shops) {

            if (shop != null
                    && shop.name != null
                    && !shop.name.trim().isEmpty()) {

                shopNames.add(shop.name.trim());
            }
        }

        for (String shopName : shopNames) {
            shopFilter.addItem(shopName);
        }

        shopFilter.setPreferredSize(
                new Dimension(180, 34)
        );

        shopFilter.setBackground(Color.WHITE);
        shopFilter.setForeground(Color.BLACK);

        // COMBO BOX TEXT COLORS

        DefaultListCellRenderer comboRenderer =
                new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );

                setOpaque(true);

                setBackground(
                        isSelected ? BLUE : Color.WHITE
                );

                setForeground(
                        isSelected ? Color.WHITE : Color.BLACK
                );

                return this;
            }
        };

        shopFilter.setRenderer(comboRenderer);

        // ADD FILTER COMPONENTS

        filterPanel.add(dateLabel);
        filterPanel.add(dateButton);
        filterPanel.add(clearDateButton);
        filterPanel.add(shopLabel);
        filterPanel.add(shopFilter);

        // =========================================
        // REPORT CONTENT HOLDERS
        // =========================================

        JPanel statsHolder = new JPanel(
                new BorderLayout()
        );

        statsHolder.setOpaque(false);

        JPanel monthlyHolder = new JPanel(
                new BorderLayout()
        );

        monthlyHolder.setOpaque(false);

        JPanel mostRequestedHolder = new JPanel(
                new BorderLayout()
        );

        mostRequestedHolder.setOpaque(false);

        JPanel chartPanel = new JPanel(
                new BorderLayout(14, 0)
        );

        chartPanel.setOpaque(false);

        chartPanel.add(
                monthlyHolder,
                BorderLayout.CENTER
        );

        chartPanel.add(
                mostRequestedHolder,
                BorderLayout.EAST
        );

        // =========================================
        // GET FILTERED REQUESTS
        // =========================================

        Runnable refreshReports = () -> {

            List<Req> filtered = new ArrayList<>();

            for (Req r : new ArrayList<>(DB.reqs)) {

                if (r == null || r.submitted == null) {
                    continue;
                }

                // DATE FILTER

                if (selectedDate[0] != null
                        && !r.submitted.toLocalDate()
                                .equals(selectedDate[0])) {

                    continue;
                }

                // SHOP FILTER

                if (!"All Shops".equals(selectedShop[0])) {

                    if (r.shop == null
                            || !r.shop.equalsIgnoreCase(
                                    selectedShop[0])) {

                        continue;
                    }
                }

                filtered.add(r);
            }

            // =====================================
            // STATISTICS
            // =====================================

            double avg = filtered.stream()
                    .filter(r ->
                            r.accepted != null
                                    && r.submitted != null)
                    .mapToLong(r ->
                            Duration.between(
                                    r.submitted,
                                    r.accepted
                            ).toMinutes()
                    )
                    .average()
                    .orElse(-1);

            long activeShops = DB.shops.stream()
                    .filter(s -> s != null
                            && "Active".equalsIgnoreCase(s.status))
                    .filter(s ->
                            "All Shops".equals(selectedShop[0])
                                    || (s.name != null
                                    && s.name.equalsIgnoreCase(
                                            selectedShop[0]))
                    )
                    .count();

            statsHolder.removeAll();

            statsHolder.add(
                    stats(new String[][]{
                            {
                                "Total Requests",
                                String.valueOf(filtered.size())
                            },
                            {
                                "Completion Rate",
                                (filtered.isEmpty()
                                        ? 0
                                        : cnt(filtered, "Completed")
                                        * 100 / filtered.size()) + "%"
                            },
                            {
                                "Avg. Response",
                                avg < 0
                                        ? "N/A"
                                        : (int) avg + " min"
                            },
                            {
                                "Active Shops",
                                String.valueOf(activeShops)
                            }
                    }),
                    BorderLayout.CENTER
            );

            // =====================================
            // MOST REQUESTED SERVICES
            // =====================================

            JPanel mr = vbox();
            mr.setOpaque(false);

            int i = 1;

            for (Map.Entry<String, Long> entry :
                    byProblem(filtered).entrySet()) {

                JPanel row = new JPanel(
                        new BorderLayout()
                );

                row.setOpaque(false);

                row.setBorder(
                        new EmptyBorder(8, 0, 8, 0)
                );

                row.add(
                        lbl(
                                i++ + "   " + entry.getKey(),
                                14,
                                true,
                                Color.WHITE
                        ),
                        BorderLayout.WEST
                );

                row.add(
                        lbl(
                                String.valueOf(entry.getValue()),
                                14,
                                true,
                                BLUE
                        ),
                        BorderLayout.EAST
                );

                mr.add(row);
            }

            if (filtered.isEmpty()) {

                mr.add(
                        lbl(
                                "No service requests found.",
                                12,
                                false,
                                Color.WHITE
                        )
                );
            }

            RoundedPanel mc = section(
                    "Most Requested Services",
                    mr,
                    null
            );

            mc.setPreferredSize(
                    new Dimension(360, 100)
            );

            mostRequestedHolder.removeAll();

            mostRequestedHolder.add(
                    mc,
                    BorderLayout.CENTER
            );

            // =====================================
            // MONTHLY REQUESTS
            // USE SELECTED DATE YEAR
            // OR CURRENT YEAR IF NO DATE
            // =====================================

            int chartYear = selectedDate[0] == null
                    ? LocalDate.now().getYear()
                    : selectedDate[0].getYear();

            List<Req> yearlyRequests = new ArrayList<>();

            for (Req r : new ArrayList<>(DB.reqs)) {

                if (r == null || r.submitted == null) {
                    continue;
                }

                if (r.submitted.getYear() != chartYear) {
                    continue;
                }

                if (!"All Shops".equals(selectedShop[0])) {

                    if (r.shop == null
                            || !r.shop.equalsIgnoreCase(
                                    selectedShop[0])) {

                        continue;
                    }
                }

                yearlyRequests.add(r);
            }

            double[] monthlyData = monthlyRequests(
                    yearlyRequests,
                    chartYear
            );

            monthlyHolder.removeAll();

            monthlyHolder.add(
                    section(
                            "Monthly Requests - " + chartYear,
                            new Chart(
                                    MONTHS,
                                    monthlyData,
                                    false
                            ),
                            null
                    ),
                    BorderLayout.CENTER
            );

            // =====================================
            // REFRESH UI
            // =====================================

            statsHolder.revalidate();
            statsHolder.repaint();

            mostRequestedHolder.revalidate();
            mostRequestedHolder.repaint();

            monthlyHolder.revalidate();
            monthlyHolder.repaint();
        };

        // =========================================
        // DATE SELECTION
        // =========================================

        dateButton.addActionListener(e -> {

            LocalDate initialDate =
                    selectedDate[0] == null
                            ? LocalDate.now()
                            : selectedDate[0];

            Date initial = Date.from(
                    initialDate
                            .atStartOfDay(
                                    ZoneId.systemDefault()
                            )
                            .toInstant()
            );

            SpinnerDateModel model = new SpinnerDateModel(
                    initial,
                    null,
                    null,
                    java.util.Calendar.DAY_OF_MONTH
            );

            JSpinner dateSpinner = new JSpinner(model);

            dateSpinner.setEditor(
                    new JSpinner.DateEditor(
                            dateSpinner,
                            "MMMM dd, yyyy"
                    )
            );

            int result = JOptionPane.showConfirmDialog(
                    pg[0],
                    dateSpinner,
                    "Select Report Date",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {

                Date selected = (Date) dateSpinner.getValue();

                LocalDate chosenDate = selected.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                selectedDate[0] = chosenDate;

                dateButton.setText(
                        chosenDate.format(
                                DateTimeFormatter.ofPattern(
                                        "MMM dd, yyyy"
                                )
                        )
                );

                refreshReports.run();
            }
        });

        // =========================================
        // CLEAR DATE
        // =========================================

        clearDateButton.addActionListener(e -> {

            selectedDate[0] = null;

            dateButton.setText("Select Date");

            refreshReports.run();
        });

        // =========================================
        // SHOP SELECTION
        // =========================================

        shopFilter.addActionListener(e -> {

            selectedShop[0] = String.valueOf(
                    shopFilter.getSelectedItem()
            );

            refreshReports.run();
        });

        // =========================================
        // EXPORT FILTERED REPORT
        // =========================================

        ex.addActionListener(e -> {

            List<String[]> rows = new ArrayList<>();

            for (Req r : new ArrayList<>(DB.reqs)) {

                if (r == null || r.submitted == null) {
                    continue;
                }

                // DATE FILTER

                if (selectedDate[0] != null
                        && !r.submitted.toLocalDate()
                                .equals(selectedDate[0])) {

                    continue;
                }

                // SHOP FILTER

                if (!"All Shops".equals(selectedShop[0])) {

                    if (r.shop == null
                            || !r.shop.equalsIgnoreCase(
                                    selectedShop[0])) {

                        continue;
                    }
                }

                rows.add(new String[]{
                        r.id,
                        r.uname,
                        r.vehicle,
                        r.problem,
                        r.location,
                        r.shop,
                        r.status,
                        r.submitted.format(DT)
                });
            }

            exportCsv(
                    "system_report.csv",
                    new String[]{
                            "ID",
                            "User",
                            "Vehicle",
                            "Problem",
                            "Location",
                            "Shop",
                            "Status",
                            "Submitted"
                    },
                    rows
            );
        });

        // =========================================
        // PAGE LAYOUT
        // =========================================

        JPanel topPanel = new JPanel(
                new BorderLayout(0, 12)
        );

        topPanel.setOpaque(false);

        topPanel.add(
                filterPanel,
                BorderLayout.NORTH
        );

        topPanel.add(
                statsHolder,
                BorderLayout.CENTER
        );

        JPanel content = new JPanel(
                new BorderLayout(0, 14)
        );

        content.setOpaque(false);

        content.add(
                topPanel,
                BorderLayout.NORTH
        );

        content.add(
                chartPanel,
                BorderLayout.CENTER
        );

        body.add(
                content,
                BorderLayout.CENTER
        );

        // =========================================
        // INITIAL LOADING
        // =========================================

        refreshReports.run();

        // =========================================
        // AUTOMATIC REFRESH
        // =========================================

        Timer monthlyTimer = new Timer(3000, e -> {

            if (pg[0].isShowing()) {
                refreshReports.run();
            }
        });

        monthlyTimer.start();

        return pg[0];
    }
}


package varsystem.ui.user;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Req;
import varsystem.ui.frame.MainFrame;

public final class UserHistoryPage {

    private static final Color BLUE = new Color(0, 105, 255);

    private static final Color GRADIENT_TOP =
            new Color(0x00, 0x18, 0x3F);

    private static final Color GRADIENT_MIDDLE =
            new Color(0x08, 0x2D, 0x60);

    private static final Color GRADIENT_BOTTOM =
            new Color(0x10, 0x4B, 0x85);

    private static final Color LIGHTER_BLUE =
            new Color(0xEC, 0xF4, 0xFF);

    private static final Color BORDER_BLUE =
            new Color(0x3A, 0x79, 0xB8);

    private static final Color MUTED_TEXT =
            new Color(0xC1, 0xD8, 0xF0);

    private static final Color TABLE_DARK =
            new Color(0x02, 0x20, 0x49);

    private static final Object REQUESTS_LOCK = new Object();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private UserHistoryPage() {
    }

    // ==================================================
    // BLUE GRADIENT PANEL
    // ==================================================

    private static class GradientPanel extends JPanel {

        GradientPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setPaint(new GradientPaint(
                    0, 0,
                    GRADIENT_TOP,
                    0, Math.max(1, getHeight()),
                    GRADIENT_BOTTOM
            ));

            g.fillRect(
                    0, 0,
                    getWidth(),
                    getHeight()
            );

            g.dispose();

            super.paintComponent(graphics);
        }
    }

    // ==================================================
    // ROUNDED GRADIENT SECTION
    // ==================================================

    private static class GradientSection extends JPanel {

        GradientSection(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            g.setPaint(new GradientPaint(
                    0, 0,
                    GRADIENT_TOP,
                    getWidth(), getHeight(),
                    GRADIENT_BOTTOM
            ));

            g.fillRoundRect(
                    0, 0,
                    getWidth(),
                    getHeight(),
                    22, 22
            );

            g.setColor(BORDER_BLUE);

            g.drawRoundRect(
                    0, 0,
                    Math.max(0, getWidth() - 1),
                    Math.max(0, getHeight() - 1),
                    22, 22
            );

            g.dispose();

            super.paintComponent(graphics);
        }
    }

    // ==================================================
    // TABLE CELL RENDERER
    // ==================================================

    private static class DarkTableRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );

            label.setOpaque(true);
            label.setBorder(
                    new EmptyBorder(5, 8, 5, 8)
            );

            if (isSelected) {

                label.setBackground(
                        new Color(0x19, 0x65, 0xA8)
                );

                label.setForeground(Color.WHITE);

            } else {

                label.setBackground(
                        row % 2 == 0
                                ? TABLE_DARK
                                : GRADIENT_MIDDLE
                );

                label.setForeground(LIGHTER_BLUE);
            }

            label.setHorizontalAlignment(
                    column == 1 || column == 3
                            ? SwingConstants.LEFT
                            : SwingConstants.CENTER
            );

            return label;
        }
    }

    // ==================================================
    // TABLE HEADER RENDERER
    // ==================================================

    private static class DarkHeaderRenderer
            extends DefaultTableCellRenderer {

        DarkHeaderRenderer() {

            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Arial", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setBackground(new Color(0x05, 0x38, 0x70));
            setOpaque(true);

            setBorder(
                    BorderFactory.createMatteBorder(
                            0, 0, 1, 1, BORDER_BLUE
                    )
            );
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            setText(value == null ? "" : value.toString());

            return this;
        }
    }

    // ==================================================
    // BUILD HISTORY PAGE
    // ==================================================

    public static JComponent build() {

        JComponent[] page = MainFrame.page(
                "Repair History",
                "View your previous vehicle assistance and repair requests."
        );

        JPanel body = (JPanel) page[1];

        body.setLayout(new BorderLayout(0, 15));
        body.setOpaque(true);
        body.setBackground(GRADIENT_TOP);

        List<Req> requests = getUserRequests();

        // SUMMARY CARDS

        JPanel summaries = new JPanel(
                new GridLayout(1, 3, 12, 0)
        );

        summaries.setOpaque(false);

        long total = requests.size();

        long completed = requests.stream()
                .filter(r -> isStatus(r, "Completed"))
                .count();

        long active = requests.stream()
                .filter(Req::isActive)
                .count();

        summaries.add(summaryCard(
                "Total Requests",
                String.valueOf(total)
        ));

        summaries.add(summaryCard(
                "Completed Repairs",
                String.valueOf(completed)
        ));

        summaries.add(summaryCard(
                "Active Requests",
                String.valueOf(active)
        ));

        body.add(summaries, BorderLayout.NORTH);

        // HISTORY CONTENT

        GradientSection historyPanel = new GradientSection(
                new BorderLayout(0, 12)
        );

        historyPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_BLUE,
                                1
                        ),
                        new EmptyBorder(16, 16, 16, 16)
                )
        );

        // HEADING

        JPanel heading = new JPanel(new BorderLayout());

        heading.setOpaque(false);

        JLabel title = new JLabel("My Repair History");

        title.setFont(
                new Font("Arial", Font.BOLD, 18)
        );

        title.setForeground(Color.WHITE);

        JLabel count = new JLabel(
                requests.size() + " records"
        );

        count.setFont(
                new Font("Arial", Font.PLAIN, 12)
        );

        count.setForeground(MUTED_TEXT);

        heading.add(title, BorderLayout.WEST);
        heading.add(count, BorderLayout.EAST);

        historyPanel.add(heading, BorderLayout.NORTH);

        // TABLE

        String[] columns = {
            "Request ID",
            "Repair Shop",
            "Vehicle",
            "Problem",
            "Mechanic",
            "Total Cost",
            "Status",
            "Date Submitted"
        };

        DefaultTableModel model = new DefaultTableModel(
                columns,
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Req r : requests) {

            model.addRow(new Object[]{
                safe(r.id),
                safe(r.shop),
                safe(r.vehicle),
                safe(r.problem),
                safe(r.mechanic),
                peso(r.fee + r.travel),
                safe(r.status),
                formatDate(r.submitted)
            });
        }

        JTable table = new JTable(model);

        table.setRowHeight(40);

        table.setFont(
                new Font("Arial", Font.PLAIN, 12)
        );

        table.setForeground(LIGHTER_BLUE);
        table.setBackground(TABLE_DARK);

        table.setSelectionBackground(
                new Color(0x19, 0x65, 0xA8)
        );

        table.setSelectionForeground(Color.WHITE);

        table.setGridColor(BORDER_BLUE);

        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);

        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        table.getTableHeader().setFont(
                new Font("Arial", Font.BOLD, 12)
        );

        table.getTableHeader().setBackground(
                new Color(0x05, 0x38, 0x70)
        );

        table.getTableHeader().setForeground(Color.WHITE);

        table.getTableHeader().setPreferredSize(
                new Dimension(0, 40)
        );

        table.getTableHeader().setReorderingAllowed(false);

        table.getTableHeader().setDefaultRenderer(
                new DarkHeaderRenderer()
        );

        DarkTableRenderer renderer = new DarkTableRenderer();

        for (int i = 0; i < table.getColumnCount(); i++) {

            table.getColumnModel()
                    .getColumn(i)
                    .setCellRenderer(renderer);
        }

        JScrollPane scroll = new JScrollPane(table);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER_BLUE,
                        1
                )
        );

        scroll.getViewport().setBackground(TABLE_DARK);
        scroll.setBackground(TABLE_DARK);

        scroll.getVerticalScrollBar().setUnitIncrement(15);
        scroll.getHorizontalScrollBar().setUnitIncrement(15);

        scroll.getVerticalScrollBar().setBackground(GRADIENT_TOP);
        scroll.getHorizontalScrollBar().setBackground(GRADIENT_TOP);

        historyPanel.add(scroll, BorderLayout.CENTER);

        // BOTTOM BUTTONS

        JPanel bottom = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );

        bottom.setOpaque(false);

        JButton details = new JButton("View Details");

        details.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        details.setForeground(Color.WHITE);
        details.setBackground(BLUE);
        details.setFocusPainted(false);
        details.setBorderPainted(false);

        details.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        details.setPreferredSize(
                new Dimension(135, 38)
        );

        details.addActionListener(e -> {

            int selected = table.getSelectedRow();

            if (selected < 0) {

                JOptionPane.showMessageDialog(
                        MainFrame.APP,
                        "Please select a repair request first.",
                        "Repair History",
                        JOptionPane.INFORMATION_MESSAGE
                );

                return;
            }

            int modelRow = table.convertRowIndexToModel(selected);

            showDetails(requests.get(modelRow));
        });

        JButton refresh = new JButton("Refresh");

        refresh.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        refresh.setForeground(Color.WHITE);

        refresh.setBackground(
                new Color(0x08, 0x3D, 0x72)
        );

        refresh.setFocusPainted(false);
        refresh.setBorderPainted(false);

        refresh.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        refresh.setPreferredSize(
                new Dimension(100, 38)
        );

        refresh.addActionListener(e -> MainFrame.refresh());

        bottom.add(refresh);
        bottom.add(details);

        historyPanel.add(bottom, BorderLayout.SOUTH);

        body.add(historyPanel, BorderLayout.CENTER);

        return page[0];
    }

    // ==================================================
    // GET ONLY THE LOGGED-IN USER'S REQUESTS
    // ==================================================

    private static List<Req> getUserRequests() {

        List<Req> result = new ArrayList<>();

        if (Session.ME == null
                || Session.ME.email == null) {

            return result;
        }

        String email = Session.ME.email.trim();

        synchronized (REQUESTS_LOCK) {

            for (Req r : DB.reqs) {

                if (r == null || r.uemail == null) {
                    continue;
                }

                if (r.uemail.trim().equalsIgnoreCase(email)) {
                    result.add(r);
                }
            }
        }

        result.sort(
                Comparator.comparing(
                        (Req r) -> r.submitted,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        return result;
    }

    // ==================================================
    // SUMMARY CARD
    // ==================================================

    private static JPanel summaryCard(
            String title,
            String value
    ) {

        GradientSection card = new GradientSection(
                new BorderLayout(8, 8)
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_BLUE,
                                1
                        ),
                        new EmptyBorder(14, 16, 14, 16)
                )
        );

        JLabel heading = new JLabel(title);

        heading.setFont(
                new Font("Arial", Font.BOLD, 12)
        );

        heading.setForeground(MUTED_TEXT);

        JLabel number = new JLabel(value);

        number.setFont(
                new Font("Arial", Font.BOLD, 27)
        );

        number.setForeground(Color.WHITE);

        card.add(heading, BorderLayout.NORTH);
        card.add(number, BorderLayout.CENTER);

        return card;
    }

    // ==================================================
    // REQUEST DETAILS
    // ==================================================

    private static void showDetails(Req r) {

        if (r == null) {
            return;
        }

        JDialog dialog = new JDialog(
                MainFrame.APP,
                "Repair Request Details",
                true
        );

        dialog.setContentPane(
                new GradientPanel(new BorderLayout())
        );

        // HEADER

        GradientPanel header = new GradientPanel(
                new BorderLayout()
        );

        header.setBorder(
                new EmptyBorder(16, 20, 16, 20)
        );

        JLabel heading = new JLabel("REQUEST DETAILS");

        heading.setFont(
                new Font("Arial", Font.BOLD, 17)
        );

        heading.setForeground(Color.WHITE);

        header.add(heading, BorderLayout.WEST);

        // DETAILS CONTENT

        GradientPanel content = new GradientPanel(
                new GridLayout(0, 2, 12, 12)
        );

        content.setBorder(
                new EmptyBorder(20, 22, 20, 22)
        );

        addDetail(content, "Request ID", r.id);
        addDetail(content, "Repair Shop", r.shop);
        addDetail(content, "Customer", r.uname);
        addDetail(content, "Contact Number", r.uphone);

        addDetail(content, "Vehicle", r.vehicle);
        addDetail(content, "Problem", r.problem);
        addDetail(content, "Assigned Mechanic", r.mechanic);
        addDetail(content, "Location", r.location);

        addDetail(content, "Payment Method", r.payment);
        addDetail(content, "Request Status", r.status);

        addDetail(content, "Service Fee", peso(r.fee));
        addDetail(content, "Travel Fee", peso(r.travel));
        addDetail(content, "Total Cost", peso(r.fee + r.travel));

        addDetail(
                content,
                "Date Submitted",
                formatDateTime(r.submitted)
        );

        addDetail(
                content,
                "Date Accepted",
                formatDateTime(r.accepted)
        );

        addDetail(
                content,
                "Date Completed",
                formatDateTime(r.completed)
        );

        addDetail(
                content,
                "Additional Details",
                r.details
        );

        JScrollPane scroll = new JScrollPane(content);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        BORDER_BLUE,
                        1
                )
        );

        scroll.getViewport().setBackground(GRADIENT_TOP);
        scroll.setBackground(GRADIENT_TOP);

        scroll.getVerticalScrollBar().setUnitIncrement(15);

        scroll.getVerticalScrollBar().setBackground(GRADIENT_TOP);
        scroll.getHorizontalScrollBar().setBackground(GRADIENT_TOP);

        // CLOSE BUTTON

        JButton close = new JButton("Close");

        close.setBackground(BLUE);
        close.setForeground(Color.WHITE);
        close.setFocusPainted(false);
        close.setBorderPainted(false);

        close.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        close.setPreferredSize(
                new Dimension(100, 36)
        );

        close.addActionListener(e -> dialog.dispose());

        JPanel bottom = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        bottom.setOpaque(false);

        bottom.setBorder(
                new EmptyBorder(5, 15, 12, 15)
        );

        bottom.add(close);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setSize(720, 600);

        dialog.setMinimumSize(
                new Dimension(550, 450)
        );

        dialog.setLocationRelativeTo(MainFrame.APP);
        dialog.setVisible(true);
    }

    // ==================================================
    // DETAIL ITEM
    // ==================================================

    private static void addDetail(
            JPanel panel,
            String title,
            String value
    ) {

        GradientSection item = new GradientSection(
                new BorderLayout(3, 5)
        );

        item.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_BLUE
                        ),
                        new EmptyBorder(9, 11, 9, 11)
                )
        );

        JLabel name = new JLabel(title);

        name.setFont(
                new Font("Arial", Font.BOLD, 11)
        );

        name.setForeground(MUTED_TEXT);

        JLabel data = new JLabel(
                "<html><body style='width:230px'>"
                + escape(safe(value))
                + "</body></html>"
        );

        data.setFont(
                new Font("Arial", Font.BOLD, 12)
        );

        data.setForeground(LIGHTER_BLUE);

        item.add(name, BorderLayout.NORTH);
        item.add(data, BorderLayout.CENTER);

        panel.add(item);
    }

    // ==================================================
    // HELPERS
    // ==================================================

    private static boolean isStatus(
            Req r,
            String status
    ) {

        return r != null
                && r.status != null
                && r.status.equalsIgnoreCase(status);
    }

    private static String formatDate(LocalDateTime date) {

        return date == null
                ? "Not available"
                : date.format(DATE_FORMAT);
    }

    private static String formatDateTime(LocalDateTime date) {

        return date == null
                ? "Not available"
                : date.format(DATE_TIME_FORMAT);
    }

    private static String peso(int amount) {

        return "PHP " + String.format("%,d", amount);
    }

    private static String safe(String value) {

        return value == null || value.trim().isEmpty()
                ? "Not available"
                : value.trim();
    }

    private static String escape(String value) {

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

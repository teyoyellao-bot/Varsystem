
package varsystem.ui.shop;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import varsystem.data.DB;
import varsystem.model.Svc;

public final class ShopServicePricesPage {

    private ShopServicePricesPage() {}

    // GRADIENT COLORS
    private static final Color NAVY = new Color(5, 25, 65);
    private static final Color DARK_BLUE = new Color(8, 42, 105);
    private static final Color BLUE = new Color(0, 105, 255);
    private static final Color BRIGHT_BLUE = new Color(0, 185, 255);
    private static final Color LIGHT_BLUE = new Color(30, 74, 140);
    private static final Color BACKGROUND = new Color(4, 19, 48);
    private static final Color TEXT = Color.WHITE;
    private static final Color MUTED = new Color(159, 190, 235);
    private static final Color BORDER = new Color(25, 105, 190);
    private static final Color GREEN = new Color(65, 230, 165);
    private static final Color RED = new Color(255, 105, 125);
    private static final Color SEARCH_BG = new Color(9, 43, 100);

    // SOFT DARK BLUE TABLE HEADER
    private static final Color HEADER_START = new Color(20, 91, 165);
    private static final Color HEADER_END = new Color(8, 55, 112);

    public static JComponent build() {

        GradientPanel root = new GradientPanel(
                BACKGROUND,
                DARK_BLUE,
                new BorderLayout(0, 20)
        );

        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        // HEADER
        JLabel title = new JLabel("Service Prices");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel(
                "Manage and update the details and prices of your services."
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);

        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        // SEARCH
        JTextField searchField = new JTextField();
        styleInput(searchField);
        searchField.setPreferredSize(new Dimension(280, 42));
        searchField.setToolTipText("Search service name or category");

        JLabel searchIcon = new JLabel("Search");
        searchIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchIcon.setForeground(MUTED);

        JPanel searchBox = new JPanel(new BorderLayout(10, 0));
        searchBox.setBackground(SEARCH_BG);
        searchBox.setPreferredSize(new Dimension(340, 44));
        searchBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(0, 12, 0, 12)
        ));

        searchBox.add(searchIcon, BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);

        JLabel countLabel = new JLabel("0 services");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        countLabel.setForeground(MUTED);

        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(searchBox, BorderLayout.WEST);
        searchPanel.add(countLabel, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(searchPanel, BorderLayout.SOUTH);

        // TABLE
        String[] columns = {
            "#", "Service", "Category",
            "Base Price", "Status", "Action"
        };

        JTable table = new JTable();

        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setBackground(NAVY);
        table.setRowHeight(60);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(LIGHT_BLUE);
        table.setSelectionForeground(TEXT);

        // TABLE HEADER
        JTableHeader header = table.getTableHeader();

        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(TEXT);
        header.setBackground(BLUE);
        header.setPreferredSize(new Dimension(0, 48));
        header.setReorderingAllowed(false);

        header.setDefaultRenderer((tbl, value, selected,
                focused, row, column) -> {

            JLabel label = new JLabel(
                    String.valueOf(value)
            );

            label.setOpaque(true);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Segoe UI", Font.BOLD, 13));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(
                            0, 0, 1, 1, BRIGHT_BLUE
                    ),
                    new EmptyBorder(0, 10, 0, 10)
            ));

            label.setHorizontalAlignment(
                    column == 0 ? SwingConstants.CENTER
                    : SwingConstants.LEFT
            );

            label.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
                @Override
                public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();

                    // SOFT DARK BLUE GRADIENT
                    g2.setPaint(new GradientPaint(
                            0, 0, HEADER_START,
                            c.getWidth(), c.getHeight(), HEADER_END
                    ));

                    g2.fillRect(
                            0, 0, c.getWidth(), c.getHeight()
                    );

                    g2.dispose();
                    super.paint(g, c);
                }
            });

            return label;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(NAVY);
        scroll.setBackground(NAVY);

        // BUTTONS
        JButton addButton = createGradientButton(
                "+ Add Service"
        );

        JButton refreshButton = createGradientButton(
                "Refresh"
        );

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 12, 0)
        );

        actions.setOpaque(false);
        actions.add(refreshButton);
        actions.add(addButton);

        // TABLE CARD
        GradientPanel tableCard = new GradientPanel(
                DARK_BLUE,
                NAVY,
                new BorderLayout(0, 15)
        );

        tableCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel tableTitle = new JLabel("Service List");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        tableTitle.setForeground(TEXT);

        JLabel tableSubtitle = new JLabel(
                "Click Edit in the Action column to update a service."
        );

        tableSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableSubtitle.setForeground(MUTED);

        JPanel tableHeading = new JPanel(new GridLayout(0, 1, 0, 5));
        tableHeading.setOpaque(false);
        tableHeading.add(tableTitle);
        tableHeading.add(tableSubtitle);

        JPanel tableTop = new JPanel(new BorderLayout());
        tableTop.setOpaque(false);
        tableTop.add(tableHeading, BorderLayout.WEST);

        tableCard.add(tableTop, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(actions, BorderLayout.SOUTH);

        // TABLE DATA
        Runnable fill = () -> {

            String keyword = searchField.getText()
                    .trim()
                    .toLowerCase();

            List<Svc> services = new ArrayList<>();

            if (DB.svcs != null) {

                for (Svc service : DB.svcs) {

                    if (service == null) {
                        continue;
                    }

                    boolean matches = keyword.isEmpty()
                            || safe(service.name).toLowerCase()
                                    .contains(keyword)
                            || safe(service.category).toLowerCase()
                                    .contains(keyword);

                    if (matches) {
                        services.add(service);
                    }
                }
            }

            services.sort(
                    Comparator.comparing(
                            (Svc service) ->
                                    safe(service.name).toLowerCase()
                    )
            );

            Object[][] data =
                    new Object[services.size()][columns.length];

            for (int i = 0; i < services.size(); i++) {

                Svc service = services.get(i);

                data[i][0] = i + 1;
                data[i][1] = safe(service.name);
                data[i][2] = safe(service.category);
                data[i][3] = "₱" + String.format(
                        "%,d", service.base
                );
                data[i][4] = service.active
                        ? "Active" : "Inactive";
                data[i][5] = "Edit";
            }

            DefaultTableModel model = new DefaultTableModel(
                    data, columns
            ) {
                @Override
                public boolean isCellEditable(
                        int row, int column
                ) {
                    return false;
                }
            };

            table.setModel(model);

            table.getColumnModel().getColumn(0)
                    .setPreferredWidth(55);

            table.getColumnModel().getColumn(1)
                    .setPreferredWidth(230);

            table.getColumnModel().getColumn(2)
                    .setPreferredWidth(170);

            table.getColumnModel().getColumn(3)
                    .setPreferredWidth(140);

            table.getColumnModel().getColumn(4)
                    .setPreferredWidth(110);

            table.getColumnModel().getColumn(5)
                    .setPreferredWidth(100);

            // GENERAL GRADIENT ROWS
            DefaultTableCellRenderer rowRenderer =
                    new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(
                        JTable t,
                        Object value,
                        boolean selected,
                        boolean focused,
                        int row,
                        int column
                ) {

                    JLabel label = new JLabel(
                            String.valueOf(value)
                    );

                    label.setOpaque(true);
                    label.setFont(new Font(
                            "Segoe UI",
                            Font.PLAIN,
                            13
                    ));

                    label.setForeground(TEXT);

                    label.setBackground(
                            selected ? LIGHT_BLUE : NAVY
                    );

                    label.setBorder(new EmptyBorder(0, 12, 0, 8));

                    if (column == 0) {
                        label.setHorizontalAlignment(
                                SwingConstants.CENTER
                        );
                        label.setForeground(MUTED);
                    }

                    label.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
                        @Override
                        public void paint(Graphics g, JComponent c) {

                            Graphics2D g2 =
                                    (Graphics2D) g.create();

                            if (selected) {
                                g2.setPaint(new GradientPaint(
                                        0, 0, BLUE,
                                        c.getWidth(), c.getHeight(),
                                        LIGHT_BLUE
                                ));
                            } else {
                                g2.setPaint(new GradientPaint(
                                        0, 0,
                                        row % 2 == 0
                                                ? DARK_BLUE : NAVY,
                                        c.getWidth(), c.getHeight(),
                                        NAVY
                                ));
                            }

                            g2.fillRect(
                                    0, 0,
                                    c.getWidth(), c.getHeight()
                            );

                            g2.dispose();

                            super.paint(g, c);
                        }
                    });

                    return label;
                }
            };

            for (int i = 0; i < 4; i++) {
                table.getColumnModel()
                        .getColumn(i)
                        .setCellRenderer(rowRenderer);
            }

            // STATUS
            table.getColumnModel().getColumn(4)
                    .setCellRenderer(
                    new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(
                        JTable t,
                        Object value,
                        boolean selected,
                        boolean focused,
                        int row,
                        int column
                ) {

                    JLabel label = new JLabel(
                            String.valueOf(value),
                            SwingConstants.CENTER
                    );

                    label.setOpaque(true);
                    label.setFont(new Font(
                            "Segoe UI",
                            Font.BOLD,
                            12
                    ));

                    boolean active = "Active".equals(value);

                    label.setForeground(active ? GREEN : RED);
                    label.setBackground(
                            selected ? LIGHT_BLUE : NAVY
                    );

                    return label;
                }
            });

            // EDIT BUTTON
            table.getColumnModel().getColumn(5)
                    .setCellRenderer(
                    new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(
                        JTable t,
                        Object value,
                        boolean selected,
                        boolean focused,
                        int row,
                        int column
                ) {

                    JLabel label = new JLabel(
                            "Edit",
                            SwingConstants.CENTER
                    );

                    label.setOpaque(true);
                    label.setFont(new Font(
                            "Segoe UI",
                            Font.BOLD,
                            13
                    ));

                    label.setForeground(BRIGHT_BLUE);
                    label.setBackground(
                            selected ? LIGHT_BLUE : NAVY
                    );

                    return label;
                }
            });

            countLabel.setText(
                    services.size() + " services"
            );

            table.putClientProperty(
                    "serviceList", services
            );
        };

        // ACTION COLUMN CLICK
        table.addMouseListener(
                new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(
                    java.awt.event.MouseEvent e
            ) {

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                if (row < 0 || column != 5) {
                    return;
                }

                @SuppressWarnings("unchecked")
                List<Svc> services =
                        (List<Svc>) table.getClientProperty(
                                "serviceList"
                        );

                if (services == null
                        || row >= services.size()) {
                    return;
                }

                editService(services.get(row), fill);
            }
        });

        // ADD SERVICE
        addButton.addActionListener(
                e -> addService(root, fill)
        );

        // REFRESH
        refreshButton.addActionListener(
                e -> fill.run()
        );

        // SEARCH
        searchField.getDocument().addDocumentListener(
                new DocumentListener() {

            private void update() {
                fill.run();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        root.add(top, BorderLayout.NORTH);
        root.add(tableCard, BorderLayout.CENTER);

        fill.run();

        return root;
    }

    // EDIT SERVICE
    private static void editService(
            Svc service,
            Runnable refreshList
    ) {

        if (service == null) {
            return;
        }

        JTextField nameField =
                new JTextField(safe(service.name));

        JTextField categoryField =
                new JTextField(safe(service.category));

        JTextField priceField =
                new JTextField(String.valueOf(service.base));

        JCheckBox activeCheck =
                new JCheckBox("Active");

        activeCheck.setSelected(service.active);
        activeCheck.setOpaque(false);
        activeCheck.setForeground(TEXT);
        activeCheck.setFont(new Font(
                "Segoe UI", Font.PLAIN, 13
        ));

        styleInput(nameField);
        styleInput(categoryField);
        styleInput(priceField);

        JPanel panel = new JPanel(
                new GridLayout(0, 1, 0, 8)
        );

        panel.setBackground(NAVY);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        addFormField(panel, "Service Name", nameField);
        addFormField(panel, "Category", categoryField);
        addFormField(panel, "Base Price (₱)", priceField);

        panel.add(activeCheck);

        while (true) {

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Edit Service",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();

            if (name.isEmpty() || category.isEmpty()) {

                JOptionPane.showMessageDialog(
                        null,
                        "Service Name and Category are required.",
                        "Missing Information",
                        JOptionPane.WARNING_MESSAGE
                );

                continue;
            }

            int newPrice;

            try {

                newPrice = Integer.parseInt(
                        priceField.getText().trim()
                );

                if (newPrice < 0) {
                    throw new NumberFormatException();
                }

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                        null,
                        "Please enter a valid non-negative price.",
                        "Invalid Price",
                        JOptionPane.ERROR_MESSAGE
                );

                continue;
            }

            String oldName = service.name;
            String oldCategory = service.category;
            int oldPrice = service.base;
            boolean oldActive = service.active;

            service.name = name;
            service.category = category;
            service.base = newPrice;
            service.active = activeCheck.isSelected();

            if (!DB.saveData()) {

                service.name = oldName;
                service.category = oldCategory;
                service.base = oldPrice;
                service.active = oldActive;

                JOptionPane.showMessageDialog(
                        null,
                        "Unable to save the updated service.",
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            JOptionPane.showMessageDialog(
                    null,
                    "Service updated successfully!",
                    "Updated",
                    JOptionPane.INFORMATION_MESSAGE
            );

            refreshList.run();
            return;
        }
    }

    // ADD SERVICE
    private static void addService(
            Component parent,
            Runnable refreshList
    ) {

        JTextField nameField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField descriptionField = new JTextField();
        JTextField priceField = new JTextField();

        styleInput(nameField);
        styleInput(categoryField);
        styleInput(descriptionField);
        styleInput(priceField);

        JCheckBox activeCheck = new JCheckBox("Active");

        activeCheck.setSelected(true);
        activeCheck.setOpaque(false);
        activeCheck.setForeground(TEXT);

        JPanel panel = new JPanel(
                new GridLayout(0, 1, 0, 8)
        );

        panel.setBackground(NAVY);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        addFormField(panel, "Service Name", nameField);
        addFormField(panel, "Category", categoryField);
        addFormField(panel, "Description", descriptionField);
        addFormField(panel, "Base Price (₱)", priceField);

        panel.add(activeCheck);

        while (true) {

            int result = JOptionPane.showConfirmDialog(
                    parent,
                    panel,
                    "Add New Service",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            String description = descriptionField.getText().trim();

            if (name.isEmpty()
                    || category.isEmpty()
                    || priceField.getText().trim().isEmpty()) {

                JOptionPane.showMessageDialog(
                        parent,
                        "Please fill in Service Name, Category, and Base Price.",
                        "Missing Information",
                        JOptionPane.WARNING_MESSAGE
                );

                continue;
            }

            int price;

            try {

                price = Integer.parseInt(
                        priceField.getText().trim()
                );

                if (price < 0) {
                    throw new NumberFormatException();
                }

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                        parent,
                        "Please enter a valid non-negative price.",
                        "Invalid Price",
                        JOptionPane.ERROR_MESSAGE
                );

                continue;
            }

            Svc service = new Svc();

            service.name = name;
            service.category = category;
            service.desc = description;
            service.base = price;
            service.active = activeCheck.isSelected();

            if (DB.svcs == null) {
                DB.svcs = new ArrayList<>();
            }

            DB.svcs.add(service);

            if (!DB.saveData()) {

                DB.svcs.remove(service);

                JOptionPane.showMessageDialog(
                        parent,
                        "Unable to save the new service.",
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            JOptionPane.showMessageDialog(
                    parent,
                    "New service added successfully!",
                    "Service Added",
                    JOptionPane.INFORMATION_MESSAGE
            );

            refreshList.run();
            return;
        }
    }

    // FORM FIELD
    private static void addFormField(
            JPanel panel,
            String title,
            JTextField field
    ) {

        JLabel label = new JLabel(title);
        label.setFont(new Font(
                "Segoe UI", Font.BOLD, 12
        ));
        label.setForeground(TEXT);

        panel.add(label);
        panel.add(field);
    }

    // INPUT STYLE
    private static void styleInput(JTextField field) {

        field.setFont(new Font(
                "Segoe UI", Font.PLAIN, 13
        ));

        field.setForeground(TEXT);
        field.setBackground(SEARCH_BG);
        field.setCaretColor(Color.WHITE);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(9, 11, 9, 11)
        ));
    }

    // GRADIENT BUTTON
    private static JButton createGradientButton(String text) {

        JButton button = new JButton(text) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                Color start = getModel().isPressed()
                        ? DARK_BLUE : BRIGHT_BLUE;

                Color end = getModel().isRollover()
                        ? BLUE : new Color(0, 75, 220);

                g2.setPaint(new GradientPaint(
                        0, 0, start,
                        getWidth(), getHeight(), end
                ));

                g2.fillRoundRect(
                        0, 0,
                        getWidth(), getHeight(),
                        12, 12
                );

                g2.dispose();

                super.paintComponent(g);
            }
        };

        button.setFont(new Font(
                "Segoe UI", Font.BOLD, 13
        ));

        button.setForeground(Color.WHITE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(
                Cursor.HAND_CURSOR
        ));

        button.setBorder(new EmptyBorder(
                12, 22, 12, 22
        ));

        return button;
    }

    // GRADIENT PANEL
    private static class GradientPanel extends JPanel {

        private final Color start;
        private final Color end;

        GradientPanel(
                Color start,
                Color end,
                LayoutManager layout
        ) {

            super(layout);

            this.start = start;
            this.end = end;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            g2.setPaint(new GradientPaint(
                    0, 0, start,
                    getWidth(), getHeight(), end
            ));

            g2.fillRect(
                    0, 0,
                    getWidth(), getHeight()
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

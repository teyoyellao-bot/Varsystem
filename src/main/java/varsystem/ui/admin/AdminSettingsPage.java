
package varsystem.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import varsystem.data.DB;
import static varsystem.data.Session.ME;
import static varsystem.ui.components.Dialogs.ask;
import static varsystem.ui.components.Dialogs.exportCsv;
import static varsystem.ui.components.Dialogs.msg;
import varsystem.ui.components.RoundedPanel;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.components.Ui.lbl;
import static varsystem.ui.components.Ui.section;
import static varsystem.ui.components.Ui.vbox;
import static varsystem.ui.frame.MainFrame.page;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;
import static varsystem.ui.theme.Theme.GRAY;
import static varsystem.ui.theme.Theme.RED;

public final class AdminSettingsPage {

    private AdminSettingsPage() {
    }

    static String SETSEC = "General";

    static final Map<String, String[][]> SPEC =
            new LinkedHashMap<>();

    // ==========================================
    // UI COLORS
    // ==========================================

    private static final Color DARK =
            new Color(0x05, 0x19, 0x3A);

    private static final Color NAVY =
            new Color(0x0B, 0x2D, 0x63);

    private static final Color NAVY_LIGHT =
            new Color(0x13, 0x43, 0x83);

    private static final Color LIGHT_BLUE =
            new Color(0xD8, 0xEA, 0xFF);

    private static final Color BORDER_BLUE =
            new Color(0x3C, 0x6D, 0xA8);

    private static final Color WHITE =
            new Color(0xF5, 0xF9, 0xFF);

    private static final Color FIELD_BG =
            new Color(0x0B, 0x33, 0x6E);

    // ==========================================
    // SETTINGS DATA
    // ==========================================

    static {

        SPEC.put("General", new String[][]{
            {"System Name", "t", "Vehicle Assistance and Repair Locator"},
            {"Admin Email", "t", "admin@var-system.com"},
            {"Default Request Radius", "t", "25 km"},
            {"Timezone", "c:Asia/Manila (GMT+8)|UTC|America/New_York|Europe/London", "Asia/Manila (GMT+8)"},
            {"Maintenance Mode", "c:Off|On", "Off"}
        });

        SPEC.put("Notifications", new String[][]{
            {"Email notifications", "b", "true"},
            {"New request alerts", "b", "true"},
            {"New shop application alerts", "b", "true"},
            {"Daily summary", "b", "false"}
        });

        SPEC.put("Security", new String[][]{
            {"Minimum Password Length", "t", "6"},
            {"Session Timeout", "c:15 min|30 min|60 min|Never", "30 min"},
            {"Require strong passwords", "b", "false"},
            {"Two-factor for admins", "b", "false"}
        });

        // NEARBY SHOP RADIUS
        SPEC.put("Map & Location", new String[][]{
            {"Default City", "t", "Balayan, Batangas"},
            {"Distance Unit", "c:km|mi", "km"},
            {"Nearby Shop Radius", "t", "3"},
            {"Allow location sharing", "b", "true"},
            {"Show shops on map", "b", "true"}
        });

        // TRAVEL FEE SETTINGS
        SPEC.put("Service Rules", new String[][]{
            {"Max Active Requests Per User", "t", "3"},
            {"Base Travel Fee (PHP)", "t", "55"},
            {"Rate Per Kilometer (PHP)", "t", "15"},
            {"Minimum Travel Fee (PHP)", "t", "55"},
            {"Auto-cancel after (min)", "t", "60"},
            {"Allow towing requests", "b", "true"}
        });

        SPEC.put("My Preferences", new String[][]{
            {"Personal Notifications", "b", "true"},
            {"Location Sharing", "b", "true"}
        });

        SPEC.put("System Logs", new String[][]{});
    }

    // ==========================================
    // MAIN BUILD
    // ==========================================

    public static JComponent build() {

        JComponent[] pg = page(
                "Settings",
                "Configure system preferences and administrator access."
        );

        JPanel body = (JPanel) pg[1];
        body.setOpaque(false);

        // ==========================================
        // SETTINGS MENU
        // ==========================================

        JPanel menu = vbox();
        menu.setOpaque(false);
        menu.setBorder(new EmptyBorder(8, 8, 8, 8));

        for (String k : SPEC.keySet()) {

            JButton b = btn(
                    k,
                    k.equals(SETSEC) ? BLUE : NAVY,
                    e -> {
                        SETSEC = k;
                        refresh();
                    }
            );

            b.setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 44)
            );

            b.setPreferredSize(
                    new Dimension(215, 44)
            );

            b.setAlignmentX(Component.LEFT_ALIGNMENT);

            b.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            13
                    )
            );

            b.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            menu.add(b);
            menu.add(Box.createVerticalStrut(7));
        }

        RoundedPanel mc = section(
                "Settings Menu",
                menu,
                null
        );

        mc.setPreferredSize(
                new Dimension(245, 100)
        );

        JPanel content = new JPanel(
                new BorderLayout(12, 12)
        );

        content.setOpaque(false);

        content.setBorder(
                new EmptyBorder(12, 12, 12, 12)
        );

        // ==========================================
        // SYSTEM LOGS
        // ==========================================

        if (SETSEC.equals("System Logs")) {

            JTextArea ta = new JTextArea(
                    String.join("\n", DB.logs)
            );

            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBackground(DARK);
            ta.setForeground(LIGHT_BLUE);
            ta.setCaretColor(Color.WHITE);

            ta.setFont(
                    new Font(
                            "Monospaced",
                            Font.PLAIN,
                            12
                    )
            );

            ta.setBorder(
                    new EmptyBorder(14, 14, 14, 14)
            );

            JScrollPane logScroll =
                    new JScrollPane(ta);

            logScroll.setBorder(
                    new LineBorder(BORDER_BLUE, 1)
            );

            logScroll.getViewport()
                    .setBackground(DARK);

            content.add(
                    logScroll,
                    BorderLayout.CENTER
            );

            JPanel logButtons = new JPanel(
                    new FlowLayout(
                            FlowLayout.RIGHT,
                            8,
                            8
                    )
            );

            logButtons.setOpaque(false);

            JButton clearButton = btn(
                    "Clear Logs",
                    RED,
                    e -> {

                        if (ask("Clear all logs?")) {

                            DB.logs.clear();

                            DB.log(
                                    "Logs cleared by admin"
                            );

                            DB.saveAllData();

                            refresh();
                        }
                    }
            );

            JButton exportButton = btn(
                    "Export Logs",
                    BLUE,
                    e -> exportCsv(
                            "system_logs.csv",
                            new String[]{"Log"},
                            DB.logs.stream()
                                    .map(x -> new String[]{x})
                                    .collect(Collectors.toList())
                    )
            );

            clearButton.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            exportButton.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            logButtons.add(clearButton);
            logButtons.add(exportButton);

            content.add(
                    logButtons,
                    BorderLayout.SOUTH
            );

        } else {

            // ==========================================
            // SETTINGS FORM
            // ==========================================

            String[][] spec = SPEC.get(SETSEC);

            JPanel f = new JPanel(
                    new GridBagLayout()
            );

            f.setOpaque(false);

            f.setBorder(
                    new EmptyBorder(12, 12, 12, 12)
            );

            GridBagConstraints c =
                    new GridBagConstraints();

            c.insets = new Insets(12, 10, 12, 10);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            List<Runnable> savers =
                    new ArrayList<>();

            List<Runnable> resetters =
                    new ArrayList<>();

            boolean personalPreferences =
                    "My Preferences".equals(SETSEC);

            // NEARBY RADIUS REFERENCE
            JTextField[] nearbyRadiusField = {null};

            // TRAVEL FEE FIELD REFERENCES
            Map<String, JTextField> travelFeeFields =
                    new LinkedHashMap<>();

            for (int i = 0; i < spec.length; i++) {

                String[] s = spec[i];

                String key =
                        SETSEC + "." + s[0];

                // ==========================================
                // LABEL
                // ==========================================

                c.gridy = i;
                c.gridx = 0;
                c.weightx = 0.35;

                JLabel fieldLabel = lbl(
                        s[0],
                        13,
                        true,
                        LIGHT_BLUE
                );

                f.add(fieldLabel, c);

                // ==========================================
                // INPUT
                // ==========================================

                c.gridx = 1;
                c.weightx = 0.65;

                String cur;

                if (personalPreferences && ME != null) {

                    if ("Personal Notifications".equals(s[0])) {

                        cur = String.valueOf(ME.notif);

                    } else if ("Location Sharing".equals(s[0])) {

                        cur = String.valueOf(ME.loc);

                    } else {

                        cur = s[2];
                    }

                } else {

                    cur = DB.S(key, s[2]);
                }

                switch (s[1]) {

                    // ==========================================
                    // TEXT FIELD
                    // ==========================================

                    case "t" -> {

                        JTextField t =
                                new JTextField(cur, 24);

                        styleTextField(t);

                        f.add(t, c);

                        if ("Map & Location".equals(SETSEC)
                                && "Nearby Shop Radius".equals(s[0])) {

                            nearbyRadiusField[0] = t;
                        }

                        // STORE TRAVEL FEE FIELD REFERENCES
                        if ("Service Rules".equals(SETSEC)
                                && (s[0].equals("Base Travel Fee (PHP)")
                                || s[0].equals("Rate Per Kilometer (PHP)")
                                || s[0].equals("Minimum Travel Fee (PHP)"))) {

                            travelFeeFields.put(s[0], t);
                        }

                        savers.add(() ->
                                DB.settings.put(
                                        key,
                                        t.getText().trim()
                                )
                        );

                        resetters.add(() ->
                                t.setText(s[2])
                        );
                    }

                    // ==========================================
                    // CHECKBOX
                    // ==========================================

                    case "b" -> {

                        JCheckBox cb = new JCheckBox(
                                "Enabled",
                                cur.equalsIgnoreCase("true")
                        );

                        styleCheckBox(cb);

                        f.add(cb, c);

                        savers.add(() -> {

                            boolean selected =
                                    cb.isSelected();

                            if (personalPreferences
                                    && ME != null) {

                                if ("Personal Notifications"
                                        .equals(s[0])) {

                                    ME.notif = selected;
                                }

                                if ("Location Sharing"
                                        .equals(s[0])) {

                                    ME.loc = selected;
                                }

                            } else {

                                DB.settings.put(
                                        key,
                                        String.valueOf(selected)
                                );
                            }
                        });

                        resetters.add(() ->
                                cb.setSelected(
                                        Boolean.parseBoolean(s[2])
                                )
                        );
                    }

                    // ==========================================
                    // COMBO BOX
                    // ==========================================

                    default -> {

                        JComboBox<String> cbx =
                                new JComboBox<>(
                                        s[1].substring(2)
                                                .split("\\|")
                                );

                        cbx.setSelectedItem(cur);

                        styleComboBox(cbx);

                        f.add(cbx, c);

                        savers.add(() ->
                                DB.settings.put(
                                        key,
                                        (String) cbx.getSelectedItem()
                                )
                        );

                        resetters.add(() ->
                                cbx.setSelectedItem(s[2])
                        );
                    }
                }
            }

            // ==========================================
            // FORM SCROLL
            // ==========================================

            JPanel formWrap = new JPanel(
                    new BorderLayout()
            );

            formWrap.setOpaque(false);

            formWrap.add(
                    f,
                    BorderLayout.NORTH
            );

            JScrollPane formScroll =
                    new JScrollPane(formWrap);

            formScroll.setBorder(null);
            formScroll.setOpaque(false);

            formScroll.getViewport()
                    .setOpaque(false);

            formScroll.getVerticalScrollBar()
                    .setUnitIncrement(16);

            content.add(
                    formScroll,
                    BorderLayout.CENTER
            );

            // ==========================================
            // BUTTONS
            // ==========================================

            JPanel buttons = new JPanel(
                    new FlowLayout(
                            FlowLayout.RIGHT,
                            10,
                            8
                    )
            );

            buttons.setOpaque(false);

            JButton reset = btn(
                    "Reset to Defaults",
                    GRAY,
                    e -> {

                        if (ask(
                                "Reset " + SETSEC
                                        + " to defaults?"
                        )) {

                            if (personalPreferences
                                    && ME != null) {

                                ME.notif = true;
                                ME.loc = true;

                            } else {

                                for (String[] setting : spec) {

                                    DB.settings.remove(
                                            SETSEC
                                                    + "."
                                                    + setting[0]
                                    );
                                }
                            }

                            resetters.forEach(
                                    Runnable::run
                            );

                            DB.saveAllData();

                            DB.log(
                                    "Settings reset: "
                                            + SETSEC
                            );

                            msg(
                                    SETSEC
                                            + " restored to defaults."
                            );

                            refresh();
                        }
                    }
            );

            JButton save = btn(
                    "Save Changes",
                    BLUE,
                    e -> {

                        // ======================================
                        // VALIDATE NEARBY RADIUS
                        // ======================================

                        if ("Map & Location".equals(SETSEC)
                                && nearbyRadiusField[0] != null) {

                            String value =
                                    nearbyRadiusField[0]
                                            .getText()
                                            .trim();

                            int radiusValue;

                            try {

                                radiusValue =
                                        Integer.parseInt(value);

                            } catch (NumberFormatException ex) {

                                msg(
                                        "Nearby Shop Radius must be "
                                                + "a whole number from "
                                                + "1 to 50 km."
                                );

                                nearbyRadiusField[0]
                                        .requestFocus();

                                return;
                            }

                            if (radiusValue < 1
                                    || radiusValue > 50) {

                                msg(
                                        "Nearby Shop Radius must be "
                                                + "between 1 and 50 km."
                                );

                                nearbyRadiusField[0]
                                        .requestFocus();

                                return;
                            }
                        }

                        // ======================================
                        // VALIDATE TRAVEL FEE SETTINGS
                        // ======================================

                        if ("Service Rules".equals(SETSEC)) {

                            String[] feeSettingNames = {
                                "Base Travel Fee (PHP)",
                                "Rate Per Kilometer (PHP)",
                                "Minimum Travel Fee (PHP)"
                            };

                            for (String settingName
                                    : feeSettingNames) {

                                JTextField field =
                                        travelFeeFields.get(
                                                settingName
                                        );

                                if (field == null) {
                                    continue;
                                }

                                String value =
                                        field.getText().trim();

                                int amount;

                                try {

                                    amount =
                                            Integer.parseInt(value);

                                } catch (NumberFormatException ex) {

                                    msg(
                                            settingName
                                                    + " must be a valid "
                                                    + "whole number."
                                    );

                                    field.requestFocus();

                                    return;
                                }

                                if (amount < 0) {

                                    msg(
                                            settingName
                                                    + " cannot be negative."
                                    );

                                    field.requestFocus();

                                    return;
                                }
                            }
                        }

                        // ======================================
                        // SAVE SETTINGS
                        // ======================================

                        savers.forEach(
                                Runnable::run
                        );

                        DB.saveAllData();

                        DB.log(
                                "Settings saved: "
                                        + SETSEC
                        );

                        msg(
                                SETSEC
                                        + " settings saved."
                        );

                        refresh();
                    }
            );

            reset.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            save.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );

            buttons.add(reset);
            buttons.add(save);

            content.add(
                    buttons,
                    BorderLayout.SOUTH
            );
        }

        // ==========================================
        // MAIN LAYOUT
        // ==========================================

        body.add(
                mc,
                BorderLayout.WEST
        );

        JPanel east = new JPanel(
                new BorderLayout()
        );

        east.setOpaque(false);

        east.setBorder(
                new EmptyBorder(0, 14, 0, 0)
        );

        east.add(
                section(
                        SETSEC.equals("System Logs")
                                ? "System Logs"
                                : SETSEC + " Settings",
                        content,
                        null
                )
        );

        body.add(
                east,
                BorderLayout.CENTER
        );

        return pg[0];
    }

    // ==========================================
    // TEXT FIELD STYLE
    // ==========================================

    private static void styleTextField(
            JTextField t) {

        t.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        t.setForeground(WHITE);
        t.setBackground(FIELD_BG);
        t.setCaretColor(Color.WHITE);
        t.setSelectionColor(BLUE);
        t.setSelectedTextColor(Color.WHITE);

        t.setPreferredSize(
                new Dimension(260, 40)
        );

        t.setBorder(
                new CompoundBorder(
                        new LineBorder(
                                BORDER_BLUE,
                                1
                        ),
                        new EmptyBorder(
                                7,
                                10,
                                7,
                                10
                        )
                )
        );
    }

    // ==========================================
    // COMBO BOX STYLE
    // ==========================================

    private static void styleComboBox(
            JComboBox<String> cb) {

        cb.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        cb.setForeground(WHITE);
        cb.setBackground(FIELD_BG);

        cb.setPreferredSize(
                new Dimension(260, 40)
        );

        cb.setBorder(
                new LineBorder(BORDER_BLUE, 1)
        );

        cb.setFocusable(false);

        cb.setRenderer(
                (list, value, index, selected, focus) -> {

                    JLabel label = new JLabel(
                            value == null ? "" : value
                    );

                    label.setOpaque(true);

                    label.setBorder(
                            new EmptyBorder(
                                    8,
                                    10,
                                    8,
                                    10
                            )
                    );

                    label.setFont(
                            new Font(
                                    "SansSerif",
                                    Font.PLAIN,
                                    13
                            )
                    );

                    if (selected) {

                        label.setBackground(NAVY_LIGHT);
                        label.setForeground(Color.WHITE);

                    } else {

                        label.setBackground(FIELD_BG);
                        label.setForeground(LIGHT_BLUE);
                    }

                    return label;
                }
        );
    }

    // ==========================================
    // CHECKBOX STYLE
    // ==========================================

    private static void styleCheckBox(
            JCheckBox cb) {

        cb.setOpaque(false);
        cb.setForeground(LIGHT_BLUE);

        cb.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        13
                )
        );

        cb.setFocusPainted(false);

        cb.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        cb.setBorder(
                new EmptyBorder(
                        5,
                        8,
                        5,
                        8
                )
        );
    }
}

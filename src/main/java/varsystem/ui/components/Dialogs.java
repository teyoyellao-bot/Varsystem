
package varsystem.ui.components;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableModel;
import varsystem.data.DB;
import static varsystem.ui.frame.MainFrame.APP;

public final class Dialogs {

    private Dialogs() { }

    public static class TextAreaInput {
        public String t;

        public TextAreaInput(String t) {
            this.t = t;
        }
    }

    public static class PasswordInput { }

    /** Date picker input; future dates are disabled for birthday entry. */
    public static class DateInput extends JPanel {

        private final JSpinner spinner;

        public DateInput() {
            super(new BorderLayout());

            Date today = new Date();

            SpinnerDateModel model = new SpinnerDateModel(
                today, null, today, java.util.Calendar.DAY_OF_MONTH
            );

            spinner = new JSpinner(model);

            spinner.setEditor(
                new JSpinner.DateEditor(spinner, "yyyy-MM-dd")
            );

            add(spinner, BorderLayout.CENTER);
        }

        public String getValue() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd")
                .format(spinner.getValue());
        }
    }

    public static class FileInput extends JPanel {

        private String selectedPath = "";

        private final JLabel previewLabel;
        private final JButton uploadButton;
        private final String dialogTitle;

        public FileInput() {
            this("Upload Image");
        }

        public FileInput(String dialogTitle) {

            this.dialogTitle = dialogTitle;

            setLayout(new BorderLayout(5, 5));
            setPreferredSize(new Dimension(235, 140));

            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(211, 222, 238)
                ),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));

            previewLabel = new JLabel(
                "No image uploaded",
                SwingConstants.CENTER
            );

            previewLabel.setPreferredSize(
                new Dimension(215, 95)
            );

            previewLabel.setOpaque(true);
            previewLabel.setBackground(new Color(245, 245, 245));
            previewLabel.setForeground(Color.GRAY);
            previewLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 11)
            );

            previewLabel.setBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
            );

            uploadButton = new JButton(dialogTitle);

            uploadButton.setPreferredSize(
                new Dimension(215, 29)
            );

            uploadButton.setFont(
                new Font("Segoe UI", Font.BOLD, 11)
            );

            uploadButton.setForeground(Color.BLACK);
            uploadButton.setBackground(new Color(22, 114, 255));

            uploadButton.setBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            );

            uploadButton.setOpaque(true);
            uploadButton.setFocusPainted(false);

            uploadButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            uploadButton.addActionListener(e -> uploadImage());

            add(previewLabel, BorderLayout.CENTER);
            add(uploadButton, BorderLayout.SOUTH);
        }

        private void uploadImage() {

            JFileChooser chooser = new JFileChooser();

            chooser.setDialogTitle(dialogTitle);

            chooser.setFileFilter(
                new FileNameExtensionFilter(
                    "Image Files (JPG, JPEG, PNG)",
                    "jpg", "jpeg", "png"
                )
            );

            int result = chooser.showOpenDialog(this);

            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedFile = chooser.getSelectedFile();

            String fileName = selectedFile.getName().toLowerCase();

            if (!(fileName.endsWith(".jpg")
                    || fileName.endsWith(".jpeg")
                    || fileName.endsWith(".png"))) {

                err("Please select a JPG, JPEG, or PNG image.");
                return;
            }

            ImageIcon originalImage =
                new ImageIcon(selectedFile.getAbsolutePath());

            if (originalImage.getIconWidth() <= 0) {
                err("The selected image cannot be opened.");
                return;
            }

            selectedPath = selectedFile.getAbsolutePath();

            Image scaledImage = originalImage.getImage()
                .getScaledInstance(
                    210,
                    95,
                    Image.SCALE_SMOOTH
                );

            previewLabel.setIcon(new ImageIcon(scaledImage));
            previewLabel.setText("");
            previewLabel.setToolTipText(selectedFile.getName());

            uploadButton.setText("Change Image");

            revalidate();
            repaint();
        }

        public String getPath() {
            return selectedPath;
        }
    }

    public static void msg(String m) {
        JOptionPane.showMessageDialog(
            APP,
            m,
            "Vehicle Assistance and Repair Locator",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void err(String m) {
        JOptionPane.showMessageDialog(
            APP,
            m,
            "Notice",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public static boolean ask(String m) {
        return JOptionPane.showConfirmDialog(
            APP,
            m,
            "Confirm",
            JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION;
    }

    public static String[] form(
            String title,
            String[] labels,
            Object[] init) {

        return formValidated(title, labels, init, null);
    }

    public static String[] formValidated(
            String title,
            String[] labels,
            Object[] init,
            Function<String[], String> validator) {

        final Color NAVY = new Color(3, 25, 62);
        final Color BLUE = new Color(22, 114, 255);
        final Color TEXT = new Color(28, 39, 58);
        final Color FIELD = new Color(247, 250, 255);
        final Color BORDER = new Color(211, 222, 238);

        JDialog dialog = new JDialog(
            APP,
            title == null ? "VAARL" : title,
            Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setDefaultCloseOperation(
            WindowConstants.DISPOSE_ON_CLOSE
        );

        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // HEADER
        JPanel header = new JPanel(
            new BorderLayout(6, 3)
        );

        header.setBackground(NAVY);

        header.setBorder(
            BorderFactory.createEmptyBorder(
                14, 22, 13, 22
            )
        );

        JLabel heading = new JLabel(
            title == null || title.isBlank()
                ? "VAARL"
                : title
        );

        heading.setForeground(Color.WHITE);
        heading.setFont(
            new Font("Segoe UI", Font.BOLD, 19)
        );

        JLabel subtitle = new JLabel(
            "Vehicle Assistance and Repair Locator"
        );

        subtitle.setForeground(
            new Color(205, 221, 247)
        );

        subtitle.setFont(
            new Font("Segoe UI", Font.PLAIN, 11)
        );

        JPanel headingBox = new JPanel();

        headingBox.setOpaque(false);

        headingBox.setLayout(
            new BoxLayout(
                headingBox,
                BoxLayout.Y_AXIS
            )
        );

        headingBox.add(heading);
        headingBox.add(Box.createVerticalStrut(3));
        headingBox.add(subtitle);

        header.add(headingBox, BorderLayout.CENTER);

        // FORM PANEL
        JPanel p = new JPanel(
            new GridBagLayout()
        );

        p.setBackground(Color.WHITE);

        p.setBorder(
            BorderFactory.createEmptyBorder(
                13, 19, 13, 19
            )
        );

        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(4, 4, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JComponent[] fs = new JComponent[labels.length];
        JLabel[] fieldLabels = new JLabel[labels.length];

        for (int i = 0; i < labels.length; i++) {

            c.gridx = 0;
            c.gridy = i;
            c.weightx = 0;
            c.gridwidth = 1;

            fieldLabels[i] = new JLabel(labels[i]);

            fieldLabels[i].setFont(
                new Font("Segoe UI", Font.BOLD, 11)
            );

            fieldLabels[i].setForeground(TEXT);

            fieldLabels[i].setPreferredSize(
                new Dimension(150, 24)
            );

            p.add(fieldLabels[i], c);

            Object o = init[i];
            JComponent f;

            if (o instanceof String[] options) {

                JComboBox<String> combo =
                    new JComboBox<>(options);

                combo.setFont(
                    new Font("Segoe UI", Font.PLAIN, 12)
                );

                combo.setBackground(Color.WHITE);

                combo.setPreferredSize(
                    new Dimension(320, 34)
                );

                f = combo;

            } else if (o instanceof TextAreaInput areaInput) {

                JTextArea a = new JTextArea(
                    areaInput.t,
                    3,
                    20
                );

                a.setLineWrap(true);
                a.setWrapStyleWord(true);

                a.setFont(
                    new Font("Segoe UI", Font.PLAIN, 12)
                );

                a.setForeground(TEXT);
                a.setBackground(FIELD);

                a.setBorder(
                    BorderFactory.createEmptyBorder(
                        7, 9, 7, 9
                    )
                );

                JScrollPane areaScroll =
                    new JScrollPane(a);

                areaScroll.setBorder(
                    BorderFactory.createLineBorder(BORDER)
                );

                f = areaScroll;

            } else if (o instanceof PasswordInput) {

                JPasswordField password =
                    new JPasswordField(20);

                styleTextField(
                    password,
                    FIELD,
                    BORDER,
                    TEXT
                );

                f = password;

            } else if (o instanceof FileInput fileInput) {

                f = fileInput;
                fileInput.setBackground(Color.WHITE);

            } else if (o instanceof DateInput dateInput) {

                f = dateInput;
                dateInput.setBackground(Color.WHITE);

                dateInput.setPreferredSize(
                    new Dimension(320, 34)
                );

            } else {

                JTextField field = new JTextField(
                    o == null ? "" : String.valueOf(o),
                    20
                );

                styleTextField(
                    field,
                    FIELD,
                    BORDER,
                    TEXT
                );

                f = field;
            }

            fs[i] = f;

            c.gridx = 1;
            c.weightx = 1;

            c.insets = new Insets(4, 4, 4, 4);

            p.add(f, c);

            c.insets = new Insets(4, 4, 4, 8);
        }

        // REGISTRATION FORM DYNAMIC FIELDS
        boolean isRegistrationForm =
            labels.length >= 13
            && "Account type".equals(labels[0])
            && "Shop name (repair shops)".equals(labels[6])
            && "Shop photo".equals(labels[7])
            && "Shop owner ID picture".equals(labels[8])
            && labels[9].startsWith("Driver's license")
            && fs[0] instanceof JComboBox;

        if (isRegistrationForm) {

            JComboBox<?> accountType =
                (JComboBox<?>) fs[0];

            Runnable updateRegistrationFields = () -> {

                String selectedType =
                    String.valueOf(accountType.getSelectedItem());

                boolean isShop =
                    "Repair Shop".equals(selectedType);

                boolean isVehicleOwner =
                    "Vehicle Owner".equals(selectedType);

                for (int index = 6; index <= 8; index++) {

                    fieldLabels[index].setVisible(isShop);
                    fs[index].setVisible(isShop);
                }

                fieldLabels[9].setVisible(isVehicleOwner);
                fs[9].setVisible(isVehicleOwner);

                p.revalidate();
                p.repaint();
            };

            accountType.addActionListener(
                e -> updateRegistrationFields.run()
            );

            updateRegistrationFields.run();
        }

        // SCROLLABLE FORM
        JScrollPane formScroll = new JScrollPane(p);

        formScroll.setBorder(
            BorderFactory.createEmptyBorder()
        );

        formScroll.getViewport().setBackground(Color.WHITE);

        formScroll.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        formScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        Dimension screenSize =
            Toolkit.getDefaultToolkit().getScreenSize();

        // DIALOG SIZE
        // Only Forgot Password-related dialogs are compact.
        boolean isForgotPasswordForm =
            title != null
            && (
                title.toLowerCase().contains("forgot password")
                || title.toLowerCase().contains("otp verification")
                || title.toLowerCase().contains("new password")
                || title.toLowerCase().contains("reset password")
            );

        int width;
        int height;

        if (isForgotPasswordForm) {

            width = 520;
            height = 430;

        } else {

            width = Math.min(
                680,
                Math.max(520, screenSize.width - 160)
            );

            height = Math.min(
                580,
                Math.max(400, screenSize.height - 160)
            );
        }

        formScroll.setPreferredSize(
            new Dimension(width - 25, height - 135)
        );

        // FOOTER
        JPanel footer = new JPanel(
            new FlowLayout(
                FlowLayout.RIGHT,
                10,
                10
            )
        );

        footer.setBackground(Color.WHITE);

        footer.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                    1, 0, 0, 0, BORDER
                ),
                BorderFactory.createEmptyBorder(
                    0, 15, 0, 18
                )
            )
        );

        // CANCEL BUTTON
        RoundedButton cancelButton =
            new RoundedButton("Cancel");

        styleButton(
            cancelButton,
            Color.WHITE,
            new Color(205, 215, 230)
        );

        // SUBMIT BUTTON
        RoundedButton submitButton =
            new RoundedButton(
                isRegistrationForm
                    ? "Create"
                    : "Submit"
            );

        styleButton(
            submitButton,
            BLUE,
            new Color(10, 91, 220)
        );

        final String[][] result = new String[1][];
        final boolean[] completed = { false };

        cancelButton.addActionListener(
            e -> dialog.dispose()
        );

        submitButton.addActionListener(e -> {

            String[] values = new String[labels.length];

            for (int i = 0; i < values.length; i++) {

                JComponent field = fs[i];

                values[i] = switch (field) {

                    case JComboBox<?> combo ->
                        String.valueOf(combo.getSelectedItem());

                    case JPasswordField password ->
                        new String(password.getPassword());

                    case FileInput fileInput ->
                        fileInput.getPath();

                    case DateInput dateInput ->
                        dateInput.getValue();

                    case JScrollPane scrollPane -> {

                        Component view =
                            scrollPane.getViewport().getView();

                        yield view instanceof JTextArea area
                            ? area.getText().trim()
                            : "";
                    }

                    case JTextField textField ->
                        textField.getText().trim();

                    default -> "";
                };
            }

            if (validator != null) {

                String error = validator.apply(values);

                if (error != null && !error.isEmpty()) {

                    JOptionPane.showMessageDialog(
                        dialog,
                        error,
                        "Please check your information",
                        JOptionPane.WARNING_MESSAGE
                    );

                    return;
                }
            }

            result[0] = values;
            completed[0] = true;

            dialog.dispose();
        });

        footer.add(cancelButton);
        footer.add(submitButton);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(formScroll, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setSize(width, height);

        dialog.setMinimumSize(
            isForgotPasswordForm
                ? new Dimension(500, 400)
                : new Dimension(500, 380)
        );

        dialog.setLocationRelativeTo(APP);
        dialog.setVisible(true);

        return completed[0] ? result[0] : null;
    }

    // TEXT FIELD STYLE
    private static void styleTextField(
            JTextField field,
            Color background,
            Color border,
            Color foreground) {

        field.setFont(
            new Font("Segoe UI", Font.PLAIN, 12)
        );

        field.setForeground(foreground);
        field.setBackground(background);

        field.setCaretColor(
            new Color(22, 114, 255)
        );

        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(
                    6, 9, 6, 9
                )
            )
        );

        field.setPreferredSize(
            new Dimension(320, 34)
        );
    }

    // CUSTOM ROUNDED BUTTON
    private static class RoundedButton extends JButton {

        private static final int RADIUS = 10;

        public RoundedButton(String text) {
            super(text);

            setUI(new BasicButtonUI());

            setFont(
                new Font("Segoe UI", Font.BOLD, 12)
            );

            setForeground(Color.BLACK);
            setBackground(Color.WHITE);

            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);

            setFocusPainted(false);
            setBorderPainted(true);
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusable(false);
            setRolloverEnabled(false);

            setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            setPreferredSize(new Dimension(105, 36));
            setMinimumSize(new Dimension(105, 36));
            setMaximumSize(new Dimension(105, 36));
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(getBackground());

            g2.fillRoundRect(
                0,
                0,
                getWidth() - 1,
                getHeight() - 1,
                RADIUS,
                RADIUS
            );

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    // BUTTON STYLE
    private static void styleButton(
            RoundedButton button,
            Color background,
            Color borderColor) {

        button.setBackground(background);
        button.setForeground(Color.BLACK);

        button.setBorder(
            new RoundedButtonBorder(
                borderColor,
                10
            )
        );

        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusable(false);
        button.setRolloverEnabled(false);

        button.setPreferredSize(
            new Dimension(105, 36)
        );

        button.setMinimumSize(
            new Dimension(105, 36)
        );

        button.setMaximumSize(
            new Dimension(105, 36)
        );
    }

    // ROUNDED BORDER ONLY
    private static class RoundedButtonBorder
            extends AbstractBorder {

        private final Color borderColor;
        private final int radius;

        public RoundedButtonBorder(
                Color borderColor,
                int radius) {

            this.borderColor = borderColor;
            this.radius = radius;
        }

        @Override
        public void paintBorder(
                Component component,
                Graphics graphics,
                int x,
                int y,
                int width,
                int height) {

            Graphics2D g2 =
                (Graphics2D) graphics.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(borderColor);

            g2.setStroke(
                new BasicStroke(1.0f)
            );

            g2.drawRoundRect(
                x,
                y,
                width - 1,
                height - 1,
                radius,
                radius
            );

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(6, 15, 6, 15);
        }

        @Override
        public Insets getBorderInsets(
                Component component,
                Insets insets) {

            insets.set(6, 15, 6, 15);

            return insets;
        }
    }

    public static void tableDialog(
            String title,
            String[] cols,
            List<String[]> rows) {

        DefaultTableModel m =
            new DefaultTableModel(cols, 0) {

                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };

        for (String[] r : rows) {
            m.addRow((Object[]) r);
        }

        JTable t = new JTable(m);
        t.setRowHeight(24);

        JScrollPane sp = new JScrollPane(t);

        sp.setPreferredSize(
            new Dimension(680, 300)
        );

        JOptionPane.showMessageDialog(
            APP,
            sp,
            title,
            JOptionPane.PLAIN_MESSAGE
        );
    }

    public static JTextArea infoArea(
            String text,
            int rows,
            int cols) {

        JTextArea a = new JTextArea(
            text,
            rows,
            cols
        );

        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setOpaque(false);

        return a;
    }

    public static void exportCsv(
            String name,
            String[] heads,
            List<String[]> rows) {

        JFileChooser fc = new JFileChooser();

        fc.setSelectedFile(new File(name));

        if (fc.showSaveDialog(APP)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (PrintWriter w = new PrintWriter(
                new FileWriter(fc.getSelectedFile()))) {

            w.println(
                Arrays.stream(heads)
                    .map(Dialogs::csvValue)
                    .collect(Collectors.joining(","))
            );

            for (String[] r : rows) {

                w.println(
                    Arrays.stream(r)
                        .map(Dialogs::csvValue)
                        .collect(Collectors.joining(","))
                );
            }

            msg(
                "Report exported to:\n"
                + fc.getSelectedFile().getAbsolutePath()
            );

            DB.log(
                "Report exported: "
                + fc.getSelectedFile().getName()
            );

        } catch (IOException ex) {

            err(
                "Could not save the file: "
                + ex.getMessage()
            );
        }
    }

    private static String csvValue(String value) {

        String safe = value == null ? "" : value;

        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}

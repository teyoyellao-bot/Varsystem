
package varsystem.ui.auth;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class IconField extends JPanel {

    public enum Glyph {
        USER, LOCK
    }

    private final JTextField field;

    public IconField(String placeholder, Glyph glyph, boolean password) {
        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 14, 0, 14));

        setPreferredSize(new Dimension(310, 44));
        setMinimumSize(new Dimension(180, 44));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Left icon
        JLabel icon = new JLabel(glyph == Glyph.USER ? "\uD83D\uDC64" : "\uD83D\uDD12");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
        icon.setPreferredSize(new Dimension(24, 32));
        add(icon, BorderLayout.WEST);

        if (password) {
            JPasswordField pf = new JPasswordField() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    drawPlaceholder(g, this, placeholder);
                }
            };

            char hiddenChar = pf.getEchoChar();

            // Real eye icon drawn using Java Graphics
            JToggleButton eye = new JToggleButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;

                    g2.setColor(new Color(70, 80, 100));
                    g2.setStroke(new BasicStroke(
                        2f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                    ));

                    // Eye outline
                    Path2D eyeShape = new Path2D.Double();
                    eyeShape.moveTo(cx - 11, cy);
                    eyeShape.quadTo(cx, cy - 12, cx + 11, cy);
                    eyeShape.quadTo(cx, cy + 12, cx - 11, cy);
                    g2.draw(eyeShape);

                    // Eye pupil
                    g2.fillOval(cx - 3, cy - 3, 6, 6);

                    // Slash when password is hidden
                    if (!isSelected()) {
                        g2.drawLine(
                            cx - 12, cy + 10,
                            cx + 12, cy - 10
                        );
                    }

                    g2.dispose();
                }
            };

            eye.setPreferredSize(new Dimension(34, 34));
            eye.setToolTipText("Show password");
            eye.setContentAreaFilled(false);
            eye.setBorderPainted(false);
            eye.setFocusPainted(false);
            eye.setOpaque(false);
            eye.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            );

            eye.addActionListener(e -> {
                if (eye.isSelected()) {
                    pf.setEchoChar((char) 0);
                    eye.setToolTipText("Hide password");
                } else {
                    pf.setEchoChar(hiddenChar);
                    eye.setToolTipText("Show password");
                }

                eye.repaint();
            });

            add(eye, BorderLayout.EAST);
            field = pf;

        } else {
            field = new JTextField() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    drawPlaceholder(g, this, placeholder);
                }
            };
        }

        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(new Color(35, 40, 50));
        field.setOpaque(false);
        field.setBorder(null);

        add(field, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(
            0, 0,
            getWidth(), getHeight(),
            14, 14
        );

        g2.dispose();
    }

    private static void drawPlaceholder(
            Graphics g,
            JTextField field,
            String placeholder) {

        if (!field.getText().isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setColor(new Color(125, 130, 140));
        g2.setFont(field.getFont());

        FontMetrics fm = g2.getFontMetrics();

        int y = (field.getHeight() - fm.getHeight()) / 2
                + fm.getAscent();

        g2.drawString(placeholder, 2, y);
        g2.dispose();
    }

    public String text() {
        if (field instanceof JPasswordField pf) {
            return new String(pf.getPassword());
        }

        return field.getText();
    }

    public void clear() {
        field.setText("");
        field.repaint();
    }

    public void focusInput() {
        field.requestFocusInWindow();
    }

    public void addActionListener(ActionListener listener) {
        field.addActionListener(listener);
    }
}

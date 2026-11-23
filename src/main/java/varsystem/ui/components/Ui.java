
package varsystem.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import static varsystem.ui.theme.Theme.*;

/** Reusable Swing helper widgets (labels, buttons, cards, tables, badges...). */
public final class Ui {
    private Ui() { }

    public static JLabel lbl(String t, int size, boolean bold, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(c);
        return l;
    }

    public static JButton btn(String t, Color bg, ActionListener a) {
        JButton b = new JButton(t) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getBackground();

                if (!isEnabled()) c = GRAY;
                else if (getModel().isPressed()) c = c.darker();
                else if (getModel().isRollover()) c = c.brighter();

                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(7, 14, 7, 14));

        if (a != null) b.addActionListener(a);

        return b;
    }

    public static JButton link(String t, ActionListener a) {
        JButton b = new JButton(t);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(DARK ? new Color(0x4D, 0xA3, 0xFF) : BLUE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(a);
        return b;
    }

    public static RoundedPanel card() {
        return new RoundedPanel(cardBg(), cardLine());
    }

    public static RoundedPanel section(String title, JComponent content, JComponent right) {
        RoundedPanel p = card();
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.add(lbl(title, 15, true, txt()), BorderLayout.WEST);

        if (right != null) {
            h.add(right, BorderLayout.EAST);
        }

        p.add(h, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);

        return p;
    }

    public static JPanel flow(Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);

        for (Component c : cs) {
            p.add(c);
        }

        return p;
    }

    public static JPanel vbox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    public static Color statusColor(String s) {
        if (s == null) return null;

        return switch (s) {
            case "Pending", "Today" -> ORANGE;
            case "Accepted", "Confirmed" -> BLUE;
            case "Completed", "Active", "Approved" -> GREEN;
            case "On Going", "In Service" -> PURPLE;
            case "Suspended", "Declined", "Cancelled" -> RED;
            case "Inactive" -> GRAY;
            default -> null;
        };
    }

    public static JComponent badge(String s, Color c) {
        JLabel l = new JLabel(s, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        l.setOpaque(false);
        l.setBackground(c);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setBorder(new EmptyBorder(3, 10, 3, 10));

        return l;
    }

    public static JComponent cell(String s) {
        Color c = statusColor(s);
        return flow(c != null ? badge(s, c) : lbl(s, 12, false, txt()));
    }

    /** Generic list/table. Cells may be Strings (status words become badges) or any Component. */
    public static JComponent grid(String[] heads, double[] weights, List<Object[]> rows) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 6, 7, 6);

        for (int i = 0; i < heads.length; i++) {
            c.gridx = i;
            c.gridy = 0;
            c.weightx = weights[i];
            p.add(lbl(heads[i].toUpperCase(), 11, true, sub()), c);
        }

        int r = 1;

        for (Object[] row : rows) {
            for (int i = 0; i < heads.length; i++) {
                c.gridx = i;
                c.gridy = r;
                c.weightx = weights[i];

                Object o = i < row.length ? row[i] : "";

                JComponent comp = o instanceof JComponent
                        ? (o instanceof JButton ? flow((JButton) o) : (JComponent) o)
                        : cell(String.valueOf(o));

                p.add(comp, c);
            }

            r++;
        }

        c.gridx = 0;
        c.gridy = r;
        c.weighty = 1;
        c.gridwidth = heads.length;
        c.fill = GridBagConstraints.BOTH;

        p.add(Box.createVerticalGlue(), c);

        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        return sp;
    }

    public static JTextField search(String hint, Runnable onChange) {
        JTextField t = new JTextField();
        t.setToolTipText(hint);
        t.setPreferredSize(new Dimension(260, 32));
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setBorder(new CompoundBorder(
                new LineBorder(cardLine()),
                new EmptyBorder(2, 8, 2, 8)
        ));
        t.setBackground(DARK ? new Color(0x0B, 0x33, 0x6E) : Color.WHITE);
        t.setForeground(txt());
        t.setCaretColor(txt());

        t.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (onChange != null) onChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (onChange != null) onChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (onChange != null) onChange.run();
            }
        });

        return t;
    }

    public static JComponent stats(String[][] it) {
        JPanel p = new JPanel(new GridLayout(1, it.length, 12, 0));
        p.setOpaque(false);

        for (String[] s : it) {
            RoundedPanel c = card();
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            c.setBorder(new EmptyBorder(12, 16, 12, 16));

            c.add(lbl(s[0].toUpperCase(), 11, true, sub()));
            c.add(lbl(s[1], 26, true, txt()));

            if (s.length > 2) {
                c.add(lbl(s[2], 11, false, sub()));
            }

            p.add(c);
        }

        p.setBorder(new EmptyBorder(0, 0, 14, 0));

        return p;
    }
}

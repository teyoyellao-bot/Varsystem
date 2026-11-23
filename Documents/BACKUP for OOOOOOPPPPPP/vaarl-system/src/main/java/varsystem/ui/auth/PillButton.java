
package varsystem.ui.auth;

import java.awt.*;
import javax.swing.*;

public class PillButton extends JButton {

    private final Color fill;

    public PillButton(String text, boolean primary) {
        super(text);

        fill = primary
            ? new Color(0x0878FF)
            : new Color(0xC62828);

        setFont(
            new Font("SansSerif", Font.BOLD, 15)
        );

        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);

        setCursor(
            new Cursor(Cursor.HAND_CURSOR)
        );

        setPreferredSize(
            new Dimension(230, 42)
        );
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setColor(fill);

        g2.fillRoundRect(
            0, 0,
            getWidth() - 1,
            getHeight() - 1,
            getHeight(),
            getHeight()
        );

        if (getModel().isRollover()) {
            g2.setColor(
                new Color(255, 255, 255, 30)
            );

            g2.fillRoundRect(
                0, 0,
                getWidth() - 1,
                getHeight() - 1,
                getHeight(),
                getHeight()
            );
        }

        g2.dispose();

        super.paintComponent(g);
    }
}

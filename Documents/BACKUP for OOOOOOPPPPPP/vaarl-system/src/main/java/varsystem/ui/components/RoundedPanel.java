package varsystem.ui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class RoundedPanel extends JPanel {
    public Color fill, line;
    public int arc = 20;

    public RoundedPanel(Color fill, Color line) {
        this.fill = fill;
        this.line = line;
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (line != null) { g2.setColor(line); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); }
        g2.dispose();
    }
}
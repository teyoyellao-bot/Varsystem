package varsystem.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

import javax.swing.JPanel;

import static varsystem.ui.theme.Theme.cardBg;
import static varsystem.ui.theme.Theme.cardLine;

/** Donut chart without center text or number. */
public class Pie extends JPanel {

    private final double[] v;
    private final Color[] c;
    private final String center, small;

    public Pie(double[] v, Color[] c, String center, String small) {
        this.v = v;
        this.c = c;
        this.center = center;
        this.small = small;

        setOpaque(false);
        setPreferredSize(new Dimension(150, 150));
    }

    @Override
    protected void paintComponent(Graphics g0) {

        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0.create();

        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        int d = Math.min(getWidth(), getHeight()) - 10;
        int x = (getWidth() - d) / 2;
        int y = (getHeight() - d) / 2;

        double tot = 0;

        for (double a : v) {
            tot += a;
        }

        // =========================================
        // PIE CHART
        // =========================================

        if (tot == 0) {

            g.setColor(cardLine());
            g.fillOval(x, y, d, d);

        } else {

            double st = 90;

            for (int i = 0; i < v.length; i++) {

                double ext = -360.0 * v[i] / tot;

                g.setColor(c[i]);

                g.fill(
                    new Arc2D.Double(
                        x,
                        y,
                        d,
                        d,
                        st,
                        ext,
                        Arc2D.PIE
                    )
                );

                st += ext;
            }
        }

        // =========================================
        // DONUT HOLE
        // =========================================

        int hd = d * 55 / 100;

        int hx = x + (d - hd) / 2;
        int hy = y + (d - hd) / 2;

        g.setColor(cardBg());

        g.fillOval(
            hx,
            hy,
            hd,
            hd
        );

        // =========================================
        // NO CENTER TEXT OR NUMBER
        // =========================================

        g.dispose();
    }
}
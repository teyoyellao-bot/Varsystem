
package varsystem.ui.components;

import java.awt.*;
import javax.swing.JPanel;

import static varsystem.ui.theme.Theme.*;

/** Simple line / bar chart. */
public class Chart extends JPanel {

    private String[] lb;
    private double[] v;
    private final boolean bars;

    public Chart(String[] lb, double[] v, boolean bars) {
        this(lb, v, bars, 300);
    }

    public Chart(String[] lb, double[] v, boolean bars, int width) {
        this.lb = lb == null ? new String[0] : lb.clone();
        this.v = v == null ? new double[0] : v.clone();
        this.bars = bars;

        setOpaque(false);
        setPreferredSize(new Dimension(width, 190));
    }

    public void setData(String[] labels, double[] values) {
        this.lb = labels == null ? new String[0] : labels.clone();
        this.v = values == null ? new double[0] : values.clone();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0.create();

        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        int w = getWidth();
        int h = getHeight();

        int l = 35, r = 30, t = 26, b = 32;
        int n = Math.min(lb.length, v.length);

        if (n == 0 || w <= l + r || h <= t + b) {
            g.dispose();
            return;
        }

        double max = 1;

        for (int i = 0; i < n; i++) {
            max = Math.max(max, v[i]);
        }

        max *= 1.15;

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));

        // Grid lines
        g.setColor(cardLine());

        for (int i = 0; i <= 4; i++) {
            int y = t + (h - t - b) * i / 4;
            g.drawLine(l, y, w - r, y);
        }

        int[] xs = new int[n];
        int[] ys = new int[n];

        for (int i = 0; i < n; i++) {

            xs[i] = bars
                ? l + (int)((w - l - r) * (i + 0.5) / n)
                : l + (n == 1
                    ? (w - l - r) / 2
                    : (w - l - r) * i / (n - 1));

            ys[i] = h - b - (int)(
                (h - t - b) * v[i] / max
            );
        }

        // Bar chart
        if (bars) {

            int bw = Math.max(
                8,
                (int)((w - l - r) / (double)n * 0.55)
            );

            g.setColor(BLUE);

            for (int i = 0; i < n; i++) {
                g.fillRoundRect(
                    xs[i] - bw / 2,
                    ys[i],
                    bw,
                    Math.max(0, h - b - ys[i]),
                    8,
                    8
                );
            }

        } else {

            // Line chart
            g.setColor(BLUE);
            g.setStroke(new BasicStroke(
                2.5f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            ));

            for (int i = 0; i < n - 1; i++) {
                g.drawLine(
                    xs[i], ys[i],
                    xs[i + 1], ys[i + 1]
                );
            }

            // Data point circles
            for (int i = 0; i < n; i++) {
                g.fillOval(
                    xs[i] - 4,
                    ys[i] - 4,
                    8,
                    8
                );
            }
        }

        // Values and labels
        FontMetrics fm = g.getFontMetrics();

        for (int i = 0; i < n; i++) {

            String value = String.valueOf((int)v[i]);
            String label = lb[i] == null ? "" : lb[i];

            g.setColor(txt());
            g.drawString(
                value,
                xs[i] - fm.stringWidth(value) / 2,
                ys[i] - 9
            );

            g.setColor(sub());
            g.drawString(
                label,
                xs[i] - fm.stringWidth(label) / 2,
                h - 7
            );
        }

        g.dispose();
    }
}

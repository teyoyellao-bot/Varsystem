
package varsystem.ui.auth;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;

/** Left side of the login screen: title, tagline and the three feature icons, placed like the reference image. */
public class LoginHero extends JPanel {
    private static final String[] TITLE = {"Philosopher", "Marcellus", "Candara", "Trebuchet MS"};
    private static final String[] BODY = {"Segoe UI", "Helvetica Neue", "Arial"};
    private static Set<String> families;

    public LoginHero() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, 500));
        setMinimumSize(new Dimension(260, 300));
    }

    private static Font font(int style, int size, String... names) {
        if (families == null) {
            families = new HashSet<>();
            java.util.Collections.addAll(
                families,
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
            );
        }
        for (String n : names) if (families.contains(n)) return new Font(n, style, Math.max(8, size));
        return new Font("SansSerif", style, Math.max(8, size));
    }

    private static Font fit(Graphics2D g, String text, int style, int size, int maxW, String... names) {
        Font f = font(style, size, names);
        while (size > 12 && g.getFontMetrics(f).stringWidth(text) > maxW) {
            size--;
            f = font(style, size, names);
        }
        return f;
    }

    private static void centered(Graphics2D g, String text, int cx, int y) {
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, y);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        Container par = getParent();
        int ww = par != null ? par.getWidth() : w;      // full window width
        int top = par != null ? par.getY() : 0;          // height of the Exit bar above
        int H = h + top;                                 // approximate full window height
        double s = Math.max(.6, Math.min(1.6, Math.min(H / 600.0, ww / 1000.0)));

        int left = (int) (ww * .057);
        int maxW = Math.max(100, w - left - 10);

        // title line 1 (white) and line 2 (blue, bigger)
        String l1 = "VEHICLE ASSISTANCE AND", l2 = "REPAIR LOCATOR";
        g.setFont(fit(g, l1, Font.BOLD, (int) (44 * s), maxW, TITLE));
        g.setColor(Color.WHITE);
        g.drawString(l1, left + (int) (4 * s), (int) (.180 * H) - top);
        g.setFont(fit(g, l2, Font.BOLD, (int) (60 * s), maxW, TITLE));
        g.setColor(new Color(0x16, 0x72, 0xFF));
        g.drawString(l2, left, (int) (.292 * H) - top);

        // tagline
        g.setFont(font(Font.PLAIN, (int) (19 * s), BODY));
        g.setColor(Color.WHITE);
        g.drawString("Find nearby vehicle repair assistance", left + (int) (4 * s), (int) (.362 * H) - top);
        g.drawString("quickly and easily", left + (int) (4 * s), (int) (.402 * H) - top);

        // features
        String[][] feats = {{"Find Nearby", "Repair Shop"}, {"Get Help", "Faster"}, {"Stay Safe", "on the road"}};
        double[] cxs = {.135, .270, .411};
        int iconY = (int) (.792 * H) - top, size = (int) (46 * s);
        for (int i = 0; i < 3; i++) {
            int cx = Math.min((int) (ww * cxs[i]), w - 40);
            drawIcon(g, i, cx, iconY, size, s);
            g.setColor(Color.WHITE);
            g.setFont(font(Font.BOLD, (int) (15 * s), BODY));
            centered(g, feats[i][0], cx, (int) (.887 * H) - top);
            g.setFont(font(Font.PLAIN, (int) (15 * s), BODY));
            centered(g, feats[i][1], cx, (int) (.925 * H) - top);
        }
        g.dispose();
    }

    private void drawIcon(Graphics2D g, int which, int cx, int cy, int size, double s) {
        Stroke old = g.getStroke();

        switch (which) {
            case 0 -> { // Modern outline location pin
                g.setColor(new Color(0xFF, 0x45, 0x45));
                g.setStroke(new BasicStroke(
                    (float) (3.5 * s),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
                ));

                Path2D pin = new Path2D.Double();

                pin.moveTo(cx, cy + size * 0.48);

                pin.curveTo(
                    cx - size * 0.15, cy + size * 0.18,
                    cx - size * 0.38, cy - size * 0.08,
                    cx - size * 0.38, cy - size * 0.20
                );

                pin.curveTo(
                    cx - size * 0.38, cy - size * 0.65,
                    cx + size * 0.38, cy - size * 0.65,
                    cx + size * 0.38, cy - size * 0.20
                );

                pin.curveTo(
                    cx + size * 0.38, cy - size * 0.08,
                    cx + size * 0.15, cy + size * 0.18,
                    cx, cy + size * 0.48
                );

                g.draw(pin);

                double r = size * 0.105;

                g.draw(new Ellipse2D.Double(
                    cx - r,
                    cy - size * 0.28 - r,
                    r * 2,
                    r * 2
                ));
            }
            case 1 -> { // clock
                g.setColor(new Color(0x5A, 0xA0, 0xFF));
                g.setStroke(new BasicStroke((float) (3.5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double r = size * .46;
                g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                g.drawLine(cx, cy, cx, cy - (int) (r * .6));
                g.drawLine(cx, cy, cx + (int) (r * .45), cy);
            }
            default -> { // check mark
                g.setColor(new Color(0x9B, 0xFF, 0x1A));
                g.setStroke(new BasicStroke((float) (7 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(cx - size * .38, cy + size * .02);
                p.lineTo(cx - size * .10, cy + size * .30);
                p.lineTo(cx + size * .42, cy - size * .32);
                g.draw(p);
            }
        }

        g.setStroke(old);
    }
}

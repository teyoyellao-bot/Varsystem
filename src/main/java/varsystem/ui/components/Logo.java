
package varsystem.ui.components;

import java.awt.*;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import static varsystem.ui.theme.Theme.BLUE;

public class Logo extends JComponent {

    private final int d;
    private final Image logoImage;

    public Logo(int d) {
        this.d = d;

        setPreferredSize(new Dimension(d, d));
        setMaximumSize(new Dimension(d, d));

        URL imageURL = getClass().getResource(
            "/varsystem/ui/auth/images/901498895_1118892907399230_927000478317574789_n.png"
        );

        if (imageURL != null) {
            logoImage = new ImageIcon(imageURL).getImage();
        } else {
            logoImage = null;
            System.out.println("Logo image not found! Using default VAR logo.");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        if (logoImage != null) {
            int imgW = logoImage.getWidth(this);
            int imgH = logoImage.getHeight(this);

            if (imgW > 0 && imgH > 0) {
                double scale = Math.min(
                    (double) d / imgW,
                    (double) d / imgH
                );

                int w = (int) (imgW * scale);
                int h = (int) (imgH * scale);

                int x = (d - w) / 2;
                int y = (d - h) / 2;

                g2.drawImage(logoImage, x, y, w, h, this);
            }
        } else {
            // Default VAR logo kapag walang PNG image
            g2.setColor(Color.WHITE);
            g2.fillOval(0, 0, d - 1, d - 1);

            g2.setColor(BLUE);
            g2.setFont(new Font(
                "SansSerif",
                Font.BOLD | Font.ITALIC,
                d / 3
            ));

            FontMetrics fm = g2.getFontMetrics();

            g2.drawString(
                "VAR",
                (d - fm.stringWidth("VAR")) / 2,
                d / 2 + fm.getAscent() / 3
            );

            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(6, d / 2, d - 12, d / 3, 200, 140);
        }

        g2.dispose();
    }
}


package varsystem.ui.auth;

import java.awt.*;
import java.net.URL;
import javax.swing.*;

public class LoginBackground extends JPanel {

    private Image background;

    public LoginBackground() {
        setLayout(new BorderLayout());

        URL imageURL = getClass().getResource("images/login-bg.jpg");

        if (imageURL != null) {
            background = new ImageIcon(imageURL).getImage();
        } else {
            System.err.println("Background image not found: images/login-bg.jpg");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (background != null) {
            // Fill the entire login window
            g.drawImage(
                background,
                0, 0,
                getWidth(), getHeight(),
                this
            );
        } else {
            // Default background if image is missing
            g.setColor(new Color(3, 20, 55));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}

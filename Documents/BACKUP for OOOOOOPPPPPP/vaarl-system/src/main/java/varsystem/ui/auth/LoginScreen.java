
package varsystem.ui.auth;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import varsystem.data.Session;
import varsystem.model.User;
import varsystem.service.AuthService;
import static varsystem.ui.components.Dialogs.ask;
import static varsystem.ui.components.Ui.*;
import varsystem.ui.frame.MainFrame;
import varsystem.ui.theme.Theme;

public final class LoginScreen {

    private LoginScreen() {}

    static final Color CARD_FILL = new Color(0x021A4E);
    static final Color ERROR = new Color(0xFF7A85);

    public static void show() {
        Session.clear();
        Theme.DARK = true;
        MainFrame.setRoot(panel());
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JLabel msgLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setPreferredSize(new Dimension(300, 22));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return left(l);
    }

    private static void say(JLabel l, String text) {
        l.setForeground(ERROR);
        String safe = text.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;");
        l.setText("<html>" + safe + "</html>");
    }

    private static JButton smallLink(String text, ActionListener a) {
        JButton b = link(text, a);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setMargin(new Insets(0, 0, 0, 0));
        return left(b);
    }

    private static void exitApp() {
        if (ask("Are you sure you want to exit?")) {
            MainFrame.APP.dispose();
            System.exit(0);
        }
    }

    private static final class Card extends JPanel {

        Card() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(CARD_FILL);
            g2.fillRoundRect(
                1, 1, getWidth() - 3, getHeight() - 3, 55, 55
            );

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(
                1, 1, getWidth() - 3, getHeight() - 3, 55, 55
            );

            g2.dispose();
        }
    }

    static JComponent panel() {

        LoginBackground root = new LoginBackground();

        IconField user = new IconField(
            "Username or Email", IconField.Glyph.USER, false
        );

        IconField pass = new IconField(
            "Password", IconField.Glyph.LOCK, true
        );

        JLabel msg = msgLabel();

        PillButton loginBtn = new PillButton("Login", true);
        PillButton exitBtn = new PillButton("Exit", false);

        JPanel v = new JPanel();
        v.setLayout(new BoxLayout(v, BoxLayout.Y_AXIS));
        v.setOpaque(false);

        // Compact padding
        v.setBorder(new EmptyBorder(18, 30, 18, 30));

        // Register
        JPanel top = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 4, 0)
        );
        top.setOpaque(false);

        top.add(lbl(
            "Don't have an account?",
            12, false, Color.WHITE
        ));

        top.add(link(
            "Register",
            e -> RegisterDialog.show()
        ));

        top.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 28)
        );

        v.add(left(top));
        v.add(Box.createVerticalStrut(7));

        // Welcome
        v.add(left(lbl(
            "Welcome!", 30, true, Color.WHITE
        )));

        v.add(Box.createVerticalStrut(4));

        v.add(left(lbl(
            "Log in to your account",
            14, false, Color.WHITE
        )));

        v.add(Box.createVerticalStrut(18));

        // Username
        v.add(left(user));
        v.add(Box.createVerticalStrut(12));

        // Password
        v.add(left(pass));
        v.add(Box.createVerticalStrut(4));

        // Validation messages
        v.add(msg);
        v.add(Box.createVerticalStrut(5));

        // Login
        loginBtn.addActionListener(
            e -> doLogin(user, pass, msg)
        );

        JPanel loginRow = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 0, 0)
        );
        loginRow.setOpaque(false);
        loginRow.add(loginBtn);

        loginRow.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 48)
        );

        v.add(left(loginRow));
        v.add(Box.createVerticalStrut(8));

        // Exit
        exitBtn.addActionListener(e -> exitApp());

        JPanel exitRow = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 0, 0)
        );
        exitRow.setOpaque(false);
        exitRow.add(exitBtn);

        exitRow.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 48)
        );

        v.add(left(exitRow));
        v.add(Box.createVerticalStrut(10));

        // Forgot Password
        v.add(left(smallLink(
            "Forgot password?",
            e -> ForgotPasswordDialog.show()
        )));

        v.add(Box.createVerticalStrut(14));

        // Demo accounts
        v.add(left(lbl(
            "Demo: macmac/user123 • shop/shop123 • admin/admin123",
            10,
            false,
            new Color(0x9FB5D5)
        )));

        // Compact login card
        Card card = new Card();
        card.setPreferredSize(new Dimension(450, 510));
        card.setMinimumSize(new Dimension(380, 480));
        card.add(v, BorderLayout.CENTER);

        // Left hero and right login card
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        center.add(new LoginHero(), c);

        c.gridx = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 10, 20, 120);

        center.add(card, c);

        root.add(center, BorderLayout.CENTER);

        // Responsive card size
        root.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {

                int availableWidth = Math.max(
                    320, root.getWidth() - 40
                );

                int w = Math.max(
                    340,
                    Math.min(
                        450,
                        Math.min(
                            availableWidth,
                            (int) (root.getWidth() * 0.40)
                        )
                    )
                );

                int h = root.getHeight() < 560
                    ? Math.max(
                        480,
                        Math.min(480, root.getHeight() - 30)
                    )
                    : 455;

                card.setPreferredSize(new Dimension(w, h));
                center.revalidate();
            }
        });

        // Keyboard controls
        user.addActionListener(
            e -> pass.focusInput()
        );

        pass.addActionListener(
            e -> doLogin(user, pass, msg)
        );

        SwingUtilities.invokeLater(user::focusInput);

        return root;
    }

    static void doLogin(
        IconField user,
        IconField pass,
        JLabel msg
    ) {

        String id = user.text().trim();
        String pw = pass.text();

        if (id.isEmpty() && pw.isEmpty()) {
            say(msg,
                "Please enter your username/email and password."
            );
            user.focusInput();
            return;
        }

        if (id.isEmpty()) {
            say(msg,
                "Please enter your username or email."
            );
            user.focusInput();
            return;
        }

        if (pw.isEmpty()) {
            say(msg, "Please enter your password.");
            pass.focusInput();
            return;
        }

        User f = AuthService.authenticate(id, pw);

        if (f == null) {
            say(msg,
                "Incorrect username/email or password."
            );
            pass.clear();
            pass.focusInput();
            return;
        }

        enter(f, msg, pass);
    }

    static void enter(
        User f,
        JLabel msg,
        IconField pass
    ) {

        String blocked = AuthService.loginBlocker(f);

        if (blocked != null) {
            say(msg, blocked);

            if (pass != null) {
                pass.clear();
            }

            return;
        }

        AuthService.signIn(f);
        MainFrame.showMain();
    }
}
